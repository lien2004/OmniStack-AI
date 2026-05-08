package com.linglong.llm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 多模型对话服务
 * 支持模型路由：
 *   - GLM 系列         → 智谱AI  (https://open.bigmodel.cn)
 *   - gpt/linglong/claude → CodeFlow/灵龙AI (OpenAI 兼容接口)
 *   - deepseek 系列    → DeepSeek (https://api.deepseek.com, 支持思考模式)
 * 同时支持同步和 SSE 流式输出
 */
@Service
public class MultiModelChatService {

    private static final Logger log = LoggerFactory.getLogger(MultiModelChatService.class);

    // --- 智谱AI ---
    @Value("${zhipu.api-key:}")
    private String zhipuApiKey;
    private static final String ZHIPU_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions";

    // --- CodeFlow/灵龙AI ---
    @Value("${bailian.base-url:https://codeflow.asia/v1}")
    private String bailianBaseUrl;
    @Value("${bailian.api-key:}")
    private String bailianApiKey;
    @Value("${bailian.model:gpt-5.4-mini}")
    private String bailianDefaultModel;

    // --- DeepSeek ---
    @Value("${deepseek.base-url:https://api.deepseek.com}")
    private String deepseekBaseUrl;
    @Value("${deepseek.api-key:}")
    private String deepseekApiKey;
    @Value("${deepseek.thinking-enabled:false}")
    private boolean deepseekThinkingEnabled;
    @Value("${deepseek.reasoning-effort:high}")
    private String deepseekReasoningEffort;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    /** 用于异步流式推送的线程池 */
    private final ExecutorService streamExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "sse-stream");
        t.setDaemon(true);
        return t;
    });

    public MultiModelChatService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    // =====================================================================
    // 同步对话（非流式）
    // =====================================================================

    /**
     * 同步多轮对话
     *
     * @param messages    消息列表 [{role, content}, ...]
     * @param model       模型名称
     * @param temperature 温度参数
     * @param maxTokens   最大 Token 数
     * @return AI 回复内容
     */
    /**
     * 同步多轮对话（向后兼容重载，思考/搜索默认关闭）
     */
    public String chat(List<Map<String, Object>> messages, String model,
                       Double temperature, Integer maxTokens) {
        return chat(messages, model, temperature, maxTokens, false, false);
    }

    /**
     * 同步多轮对话（支持深度思考和联网搜索）
     *
     * @param messages          消息列表 [{role, content}, ...]
     * @param model             模型名称
     * @param temperature       温度参数
     * @param maxTokens         最大 Token 数
     * @param enableThinking    是否开启深度思考模式
     * @param enableWebSearch   是否开启联网搜索
     * @return AI 回复内容
     */
    public String chat(List<Map<String, Object>> messages, String model,
                       Double temperature, Integer maxTokens,
                       boolean enableThinking, boolean enableWebSearch) {
        String url    = resolveUrl(model);
        String apiKey = resolveApiKey(model);
        String effectiveModel = resolveModel(model);

        Map<String, Object> body = buildRequestBody(messages, effectiveModel, temperature, maxTokens, false, enableThinking, enableWebSearch);
        try {
            String bodyStr = objectMapper.writeValueAsString(body);
            HttpRequest req = buildHttpRequest(url, apiKey, bodyStr);
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 200) {
                log.error("LLM 调用失败 status={}, body={}", resp.statusCode(), resp.body());
                throw new RuntimeException("LLM API 返回错误: " + resp.statusCode());
            }
            return extractContent(resp.body());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("LLM 调用异常: " + e.getMessage(), e);
        }
    }

    // =====================================================================
    // SSE 流式对话
    // =====================================================================

    /**
     * 异步流式对话，通过 SseEmitter 向前端推送内容块
     *
     * @param messages    消息列表
     * @param model       模型名称
     * @param temperature 温度参数
     * @param maxTokens   最大 Token 数
     * @param emitter     SseEmitter 实例
     * @param onComplete  流式完成后的回调（携带完整内容）
     */
    /**
     * 异步流式对话（向后兼容重载，思考/搜索默认关闭）
     */
    public void streamChat(List<Map<String, Object>> messages, String model,
                           Double temperature, Integer maxTokens,
                           SseEmitter emitter, java.util.function.Consumer<String> onComplete) {
        streamChat(messages, model, temperature, maxTokens, emitter, onComplete, false, false);
    }

    /**
     * 异步流式对话（支持深度思考和联网搜索），通过 SseEmitter 向前端推送内容块
     *
     * @param messages          消息列表
     * @param model             模型名称
     * @param temperature       温度参数
     * @param maxTokens         最大 Token 数
     * @param emitter           SseEmitter 实例
     * @param onComplete        流式完成后的回调（携带完整内容）
     * @param enableThinking    是否开启深度思考模式
     * @param enableWebSearch   是否开启联网搜索
     */
    public void streamChat(List<Map<String, Object>> messages, String model,
                           Double temperature, Integer maxTokens,
                           SseEmitter emitter, java.util.function.Consumer<String> onComplete,
                           boolean enableThinking, boolean enableWebSearch) {
        String url    = resolveUrl(model);
        String apiKey = resolveApiKey(model);
        String effectiveModel = resolveModel(model);

        streamExecutor.submit(() -> {
            StringBuilder fullContent = new StringBuilder();
            try {
                Map<String, Object> body = buildRequestBody(messages, effectiveModel, temperature, maxTokens, true, enableThinking, enableWebSearch);
                String bodyStr = objectMapper.writeValueAsString(body);
                HttpRequest req = buildHttpRequest(url, apiKey, bodyStr);

                HttpResponse<java.util.stream.Stream<String>> resp =
                        httpClient.send(req, HttpResponse.BodyHandlers.ofLines());

                resp.body().forEach(line -> {
                    if (line.startsWith("data:")) {
                        String data = line.substring(5).trim();
                        if ("[DONE]".equals(data)) return;
                        try {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> chunk = objectMapper.readValue(data, Map.class);
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> choices = (List<Map<String, Object>>) chunk.get("choices");
                            if (choices != null && !choices.isEmpty()) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> delta = (Map<String, Object>) choices.get(0).get("delta");
                                if (delta != null) {
                                    // 优先取 content 字段
                                    String content = (String) delta.get("content");
                                    // 深度思考模式：content 可能为空，取 reasoning_content
                                    if (content == null || content.isEmpty()) {
                                        content = (String) delta.get("reasoning_content");
                                    }
                                    if (content != null && !content.isEmpty()) {
                                        fullContent.append(content);
                                        emitter.send(SseEmitter.event().data(content));
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.debug("解析流式 chunk 忽略: {}", e.getMessage());
                        }
                    }
                });

                // 发送结束标记
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();
                if (onComplete != null) onComplete.accept(fullContent.toString());

            } catch (Exception e) {
                log.error("流式对话异常: {}", e.getMessage(), e);
                try {
                    emitter.send(SseEmitter.event().name("error").data("⚠️ 流式响应出错：" + e.getMessage()));
                    emitter.complete();
                } catch (Exception ignore) { }
                if (onComplete != null) onComplete.accept(fullContent.toString());
            }
        });
    }

    // =====================================================================
    // 工具方法
    // =====================================================================

    private String resolveUrl(String model) {
        if (isZhipuModel(model)) return ZHIPU_URL;
        if (isDeepSeekModel(model)) return deepseekBaseUrl + "/chat/completions";
        return bailianBaseUrl + "/chat/completions";
    }

    private String resolveApiKey(String model) {
        if (isZhipuModel(model)) return zhipuApiKey;
        if (isDeepSeekModel(model)) return deepseekApiKey;
        return bailianApiKey;
    }

    private String resolveModel(String model) {
        if (model == null || model.isBlank()) return "glm-4.6";
        // 如果是 'linglong' 这个别名，映射到实际模型
        if ("linglong".equalsIgnoreCase(model)) return bailianDefaultModel;
        return model;
    }

    public boolean isZhipuModel(String model) {
        if (model == null || model.isBlank()) return true;
        String lower = model.toLowerCase();
        return lower.startsWith("glm");
    }

    public boolean isDeepSeekModel(String model) {
        if (model == null || model.isBlank()) return false;
        return model.toLowerCase().startsWith("deepseek");
    }

    private Map<String, Object> buildRequestBody(List<Map<String, Object>> messages,
                                                  String model, Double temperature,
                                                  Integer maxTokens, boolean stream,
                                                  boolean enableThinking, boolean enableWebSearch) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("stream", stream);
        if (temperature != null) body.put("temperature", temperature);
        if (maxTokens != null)   body.put("max_tokens", maxTokens);

        // ── 深度思考模式 ──────────────────────────────────────────────────
        if (enableThinking) {
            if (isZhipuModel(model)) {
                // 智谱AI GLM-4.5+/GLM-5 系列支持深度思考
                Map<String, Object> thinking = new HashMap<>();
                thinking.put("type", "enabled");
                body.put("thinking", thinking);
                log.debug("智谱AI 深度思考已开启，模型={}", model);
            } else if (isDeepSeekModel(model) && model.toLowerCase().contains("pro")) {
                // DeepSeek V4-Pro 支持思考模式
                Map<String, Object> thinking = new HashMap<>();
                thinking.put("type", "enabled");
                body.put("thinking", thinking);
                body.put("reasoning_effort", deepseekReasoningEffort);
                log.debug("DeepSeek 深度思考已开启: reasoning_effort={}", deepseekReasoningEffort);
            }
        } else if (isDeepSeekModel(model) && deepseekThinkingEnabled) {
            // 应用服务端默认开启配置（如果前端未主动控制）
            Map<String, Object> thinking = new HashMap<>();
            thinking.put("type", "enabled");
            body.put("thinking", thinking);
            body.put("reasoning_effort", deepseekReasoningEffort);
        }

        // ── 联网搜索 ──────────────────────────────────────────────────────
        if (enableWebSearch && isZhipuModel(model)) {
            Map<String, Object> webSearch = new HashMap<>();
            webSearch.put("enable", true);
            webSearch.put("search_result", true);
            Map<String, Object> tool = new HashMap<>();
            tool.put("type", "web_search");
            tool.put("web_search", webSearch);
            List<Map<String, Object>> tools = new ArrayList<>();
            tools.add(tool);
            body.put("tools", tools);
            body.put("tool_choice", "auto");
            log.debug("智谱AI 联网搜索已开启，模型={}", model);
        }

        return body;
    }

    private HttpRequest buildHttpRequest(String url, String apiKey, String bodyStr) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(120))
                .POST(HttpRequest.BodyPublishers.ofString(bodyStr))
                .build();
    }

    @SuppressWarnings("unchecked")
    private String extractContent(String responseBody) {
        try {
            Map<String, Object> resp = objectMapper.readValue(responseBody, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) resp.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
                if (msg != null) {
                    String content = (String) msg.get("content");
                    if (content != null) return content;
                    // 兼容 DeepSeek 思考模式：content 可能为 null，回复在 reasoning_content 字段
                    String reasoningContent = (String) msg.get("reasoning_content");
                    if (reasoningContent != null) return reasoningContent;
                }
            }
            throw new RuntimeException("响应格式异常: " + responseBody);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("解析 LLM 响应失败", e);
        }
    }
}
