package com.linglong.agent.llm;

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

import java.time.Duration;
import java.util.List;

/**
 * LLM服务实现 - 基于LangChain4j OpenAI兼容客户端
 * 使用CodeFlow/灵龙AI的OpenAI兼容接口
 */
@Service
public class LLMServiceImpl implements LLMService {

    private static final Logger log = LoggerFactory.getLogger(LLMServiceImpl.class);

    @Value("${bailian.base-url:https://codeflow.asia/v1}")
    private String baseUrl;

    @Value("${bailian.api-key}")
    private String apiKey;

    @Value("${bailian.model:gpt-5.5}")
    private String model;

    @Value("${bailian.temperature:0.7}")
    private double temperature;

    @Value("${bailian.max-tokens:4096}")
    private int maxTokens;

    private ChatLanguageModel chatModel;
    private StreamingChatLanguageModel streamingChatModel;

    @PostConstruct
    public void init() {
        log.info("初始化LangChain4j OpenAI兼容客户端: baseUrl={}, model={}", baseUrl, model);

        this.chatModel = OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(model)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .timeout(Duration.ofSeconds(120))
                .build();

        this.streamingChatModel = OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(model)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();

        log.info("LangChain4j OpenAI兼容客户端初始化完成");
    }

    @Override
    public String generate(String prompt) {
        try {
            log.debug("LLM生成请求: {}", prompt.substring(0, Math.min(prompt.length(), 100)));
            UserMessage userMessage = UserMessage.from(prompt);
            Response<AiMessage> response = chatModel.generate(userMessage);
            String result = response.content().text();
            log.debug("LLM生成完成, 长度: {}", result.length());
            return result;
        } catch (Exception e) {
            log.error("LLM生成失败", e);
            throw new RuntimeException("AI生成失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        return generate(systemPrompt, userPrompt, model);
    }

    @Override
    public String generate(String systemPrompt, String userPrompt, String model) {
        try {
            log.debug("LLM生成请求(带systemPrompt): model={}, system={}, user={}",
                    model,
                    systemPrompt.substring(0, Math.min(systemPrompt.length(), 50)),
                    userPrompt.substring(0, Math.min(userPrompt.length(), 50)));
            SystemMessage systemMessage = SystemMessage.from(systemPrompt);
            UserMessage userMessage = UserMessage.from(userPrompt);

            final ChatLanguageModel targetModel;
            if (!model.equals(this.model)) {
                targetModel = OpenAiChatModel.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .modelName(model)
                        .temperature(temperature)
                        .maxTokens(maxTokens)
                        .timeout(Duration.ofSeconds(120))
                        .build();
            } else {
                targetModel = this.chatModel;
            }

            Response<AiMessage> response = targetModel.generate(List.of(systemMessage, userMessage));
            String result = response.content().text();
            log.debug("LLM生成完成, 长度: {}", result.length());
            return result;
        } catch (Exception e) {
            log.error("LLM生成失败", e);
            throw new RuntimeException("AI生成失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Flux<String> generateStream(String prompt) {
        try {
            log.debug("LLM流式生成请求: {}", prompt.substring(0, Math.min(prompt.length(), 100)));
            UserMessage userMessage = UserMessage.from(prompt);
            return Flux.create(sink -> {
                streamingChatModel.generate(List.of(userMessage), new StreamingResponseHandler<AiMessage>() {
                    @Override
                    public void onNext(String token) {
                        sink.next(token);
                    }

                    @Override
                    public void onComplete(Response<AiMessage> response) {
                        sink.complete();
                    }

                    @Override
                    public void onError(Throwable error) {
                        log.error("LLM流式生成失败", error);
                        sink.error(error);
                    }
                });
            });
        } catch (Exception e) {
            log.error("LLM流式生成启动失败", e);
            return Flux.error(e);
        }
    }

    @Override
    public Flux<String> generateStream(String systemPrompt, String userPrompt) {
        return generateStream(systemPrompt, userPrompt, model);
    }

    @Override
    public Flux<String> generateStream(String systemPrompt, String userPrompt, String model) {
        try {
            log.debug("LLM流式生成请求(带systemPrompt): model={}, system={}, user={}",
                    model,
                    systemPrompt.substring(0, Math.min(systemPrompt.length(), 50)),
                    userPrompt.substring(0, Math.min(userPrompt.length(), 50)));
            SystemMessage systemMessage = SystemMessage.from(systemPrompt);
            UserMessage userMessage = UserMessage.from(userPrompt);

            final StreamingChatLanguageModel targetModel;
            if (!model.equals(this.model)) {
                targetModel = OpenAiStreamingChatModel.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .modelName(model)
                        .temperature(temperature)
                        .maxTokens(maxTokens)
                        .build();
            } else {
                targetModel = this.streamingChatModel;
            }

            return Flux.create(sink -> {
                targetModel.generate(List.of(systemMessage, userMessage), new StreamingResponseHandler<AiMessage>() {
                    @Override
                    public void onNext(String token) {
                        sink.next(token);
                    }

                    @Override
                    public void onComplete(Response<AiMessage> response) {
                        sink.complete();
                    }

                    @Override
                    public void onError(Throwable error) {
                        log.error("LLM流式生成失败", error);
                        sink.error(error);
                    }
                });
            });
        } catch (Exception e) {
            log.error("LLM流式生成启动失败", e);
            return Flux.error(e);
        }
    }
}
