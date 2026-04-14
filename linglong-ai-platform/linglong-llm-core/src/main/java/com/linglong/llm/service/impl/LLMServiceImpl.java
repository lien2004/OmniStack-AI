package com.linglong.llm.service.impl;

import com.linglong.llm.service.LLMService;
import com.linglong.llm.service.ZhipuChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * LLM服务实现
 * 使用智谱AI服务
 */
@Service
public class LLMServiceImpl implements LLMService {

    private static final Logger log = LoggerFactory.getLogger(LLMServiceImpl.class);

    @Resource
    private ZhipuChatService zhipuChatService;

    @Override
    public String generate(String prompt) {
        try {
            log.debug("LLM生成请求: {}", prompt.substring(0, Math.min(prompt.length(), 100)));
            return zhipuChatService.chat(prompt);
        } catch (Exception e) {
            log.error("LLM生成失败", e);
            throw new RuntimeException("AI生成失败: " + e.getMessage());
        }
    }

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        try {
            log.debug("LLM生成请求(带systemPrompt): system={}, user={}", 
                systemPrompt.substring(0, Math.min(systemPrompt.length(), 50)),
                userPrompt.substring(0, Math.min(userPrompt.length(), 50)));
            // 使用 chatWithContext 方法，将 systemPrompt 作为 system 角色，userPrompt 作为 user 角色
            return zhipuChatService.chatWithContext(systemPrompt, userPrompt, "glm-4.6", 0.7, 4000);
        } catch (Exception e) {
            log.error("LLM生成失败", e);
            throw new RuntimeException("AI生成失败: " + e.getMessage());
        }
    }

    @Override
    public Flux<String> generateStream(String prompt) {
        // 智谱AI暂不支持流式，返回完整响应模拟流式
        return Mono.fromCallable(() -> generate(prompt))
                .flatMapMany(result -> Flux.just(result));
    }

    @Override
    public Flux<String> generateStream(String systemPrompt, String userPrompt) {
        return generateStream(systemPrompt + "\n" + userPrompt);
    }

    @Override
    public String chat(List<Message> messages) {
        // 将消息列表转换为字符串
        StringBuilder sb = new StringBuilder();
        for (Message msg : messages) {
            sb.append(msg.getMessageType().getValue()).append(": ")
              .append(msg.getContent()).append("\n");
        }
        return generate(sb.toString());
    }

    @Override
    public Flux<String> chatStream(List<Message> messages) {
        return Mono.fromCallable(() -> chat(messages))
                .flatMapMany(result -> Flux.just(result));
    }

    @Override
    public String chat(Prompt prompt) {
        return zhipuChatService.chat(prompt);
    }

    @Override
    public Flux<String> chatStream(Prompt prompt) {
        return Mono.fromCallable(() -> chat(prompt))
                .flatMapMany(result -> Flux.just(result));
    }
}
