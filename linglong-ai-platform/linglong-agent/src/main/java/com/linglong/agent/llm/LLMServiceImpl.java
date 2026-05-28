package com.linglong.agent.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.output.Response;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LLM服务实现 - 基于LangChain4j + 原生 HTTP SSE
 * 默认模型走LangChain4j, 非标模型名(如gpt-5.4-mini/gpt-5.5等)
 * 通过原生 HTTP SSE 直连 CodeFlow API, 避免 jtokkit 不识别的问题。
 */
@Service
public class LLMServiceImpl implements LLMService {

    private static final Logger log = LoggerFactory.getLogger(LLMServiceImpl.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    @Value("${bailian.base-url:https://codeflow.asia/v1}")
    private String baseUrl;

    @Value("${bailian.api-key}")
    private String apiKey;

    @Value("${bailian.model:gpt-5.4-mini}")
    private String model;

    @Value("${bailian.temperature:0.7}")
    private double temperature;

    @Value("${bailian.max-tokens:4096}")
    private int maxTokens;

    private ChatLanguageModel chatModel;
    private StreamingChatLanguageModel streamingChatModel;

    @PostConstruct
    public void init() {
        log.info("初始化LLM: baseUrl={}, model={}", baseUrl, model);

        this.chatModel = OpenAiChatModel.builder()
                .baseUrl(baseUrl).apiKey(apiKey)
                .modelName(model).temperature(temperature).maxTokens(maxTokens)
                .timeout(Duration.ofSeconds(120))
                .build();

        this.streamingChatModel = OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl).apiKey(apiKey)
                .modelName(model).temperature(temperature).maxTokens(maxTokens)
                .build();

        log.info("LLM初始化完成");
    }

    // ==================== 同步 ====================

    @Override
    public String generate(String prompt) {
        try {
            UserMessage m = UserMessage.from(prompt);
            return chatModel.generate(m).content().text();
        } catch (Exception e) {
            log.error("LLM生成失败", e);
            throw new RuntimeException("AI生成失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String generate(String sys, String user) {
        return generate(sys, user, model);
    }

    @Override
    public String generate(String sys, String user, String model) {
        try {
            SystemMessage sm = SystemMessage.from(sys);
            UserMessage um = UserMessage.from(user);
            ChatLanguageModel target = model.equals(this.model) ? chatModel
                    : OpenAiChatModel.builder()
                        .baseUrl(baseUrl).apiKey(apiKey)
                        .modelName(model).temperature(temperature).maxTokens(maxTokens)
                        .timeout(Duration.ofSeconds(120)).build();
            return target.generate(List.of(sm, um)).content().text();
        } catch (Exception e) {
            log.error("LLM生成失败", e);
            throw new RuntimeException("AI生成失败: " + e.getMessage(), e);
        }
    }

    // ==================== 流式 ====================

    @Override
    public Flux<String> generateStream(String prompt) {
        return doStream(model, null, prompt);
    }

    @Override
    public Flux<String> generateStream(String sys, String user) {
        return doStream(model, sys, user);
    }

    @Override
    public Flux<String> generateStream(String sys, String user, String model) {
        return doStream(model, sys, user);
    }

    // ---- 核心 ----

    private Flux<String> doStream(String modelName, String systemPrompt, String userPrompt) {
        return Flux.create(sink -> {
            Thread thread = new Thread(() -> {
                try {
                    rawSSEStream(modelName, systemPrompt, userPrompt,
                            token -> sink.next(token),
                            () -> sink.complete(),
                            err -> sink.error(new RuntimeException(err)));
                } catch (Exception e) {
                    sink.error(e);
                }
            }, "llm-sse-" + modelName);
            thread.setDaemon(true);
            thread.start();
        });
    }

    /**
     * 原生 HTTP SSE 连接 CodeFlow API（无 jtokkit 依赖）
     */
    private void rawSSEStream(String modelName, String systemPrompt, String userPrompt,
                               java.util.function.Consumer<String> onToken,
                               Runnable onDone,
                               java.util.function.Consumer<String> onError) {
        HttpURLConnection conn = null;
        try {
            String url = baseUrl + "/chat/completions";
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", modelName);
            body.put("temperature", temperature);
            body.put("max_tokens", maxTokens);
            body.put("stream", true);

            var messages = new java.util.ArrayList<Map<String, String>>();
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                messages.add(Map.of("role", "system", "content", systemPrompt));
            }
            messages.add(Map.of("role", "user", "content", userPrompt));
            body.put("messages", messages);

            String json = mapper.writeValueAsString(body);

            conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Accept", "text/event-stream");
            conn.setDoOutput(true);
            conn.setConnectTimeout(30_000);
            conn.setReadTimeout(300_000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int status = conn.getResponseCode();
            if (status != 200) {
                try (var is = conn.getErrorStream()) {
                    String errBody = is != null ? new String(is.readAllBytes(), StandardCharsets.UTF_8) : "HTTP " + status;
                    onError.accept(errBody);
                }
                return;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6).trim();
                        if ("[DONE]".equals(data)) {
                            onDone.run();
                            return;
                        }
                        try {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> chunk = mapper.readValue(data, Map.class);
                            @SuppressWarnings("unchecked")
                            var choices = (List<Map<String, Object>>) chunk.get("choices");
                            if (choices != null && !choices.isEmpty()) {
                                Map<String, Object> delta = (Map<String, Object>) choices.get(0).get("delta");
                                if (delta != null) {
                                    Object content = delta.get("content");
                                    if (content != null && !content.toString().isEmpty()) {
                                        onToken.accept(content.toString());
                                    }
                                }
                            }
                        } catch (Exception ignore) {
                            // skip unparseable chunks
                        }
                    }
                }
            }
            onDone.run();
        } catch (Exception e) {
            log.error("原生SSE流式调用失败: {}", e.getMessage());
            onError.accept(e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
