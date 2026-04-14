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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 多模型对话服务
 * 支持模型路由：
 *   - GLM 系列      → 智谱AI  (https://open.bigmodel.cn)
 *   - gpt/linglong  → CodeFlow/灵龙AI (OpenAI 兼容接口)
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
    public String chat(List<Map<String, Object>> messages, String model,
                       Double temperature, Integer maxTokens) {
        String url    = resolveUrl(model);
        String apiKey = resolveApiKey(model);
        String effectiveModel = resolveModel(model);

        Map<String, Object> body = buildRequestBody(messages, effectiveModel, temperature, maxTokens, false);
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
    public void streamChat(List<Map<String, Object>> messages, String model,
                           Double temperature, Integer maxTokens,
                           SseEmitter emitter, java.util.function.Consumer<String> onComplete) {
        String url    = resolveUrl(model);
        String apiKey = resolveApiKey(model);
        String effectiveModel = resolveModel(model);

        streamExecutor.submit(() -> {
            StringBuilder fullContent = new StringBuilder();
            try {
                Map<String, Object> body = buildRequestBody(messages, effectiveModel, temperature, maxTokens, true);
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
                                    String content = (String) delta.get("content");
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
        return isZhipuModel(model) ? ZHIPU_URL : (bailianBaseUrl + "/chat/completions");
    }

    private String resolveApiKey(String model) {
        return isZhipuModel(model) ? zhipuApiKey : bailianApiKey;
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

    private Map<String, Object> buildRequestBody(List<Map<String, Object>> messages,
                                                  String model, Double temperature,
                                                  Integer maxTokens, boolean stream) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("stream", stream);
        if (temperature != null) body.put("temperature", temperature);
        if (maxTokens != null)   body.put("max_tokens", maxTokens);
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
                if (msg != null) return (String) msg.get("content");
            }
            throw new RuntimeException("响应格式异常: " + responseBody);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("解析 LLM 响应失败", e);
        }
    }
}
