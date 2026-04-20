package com.linglong.llm.controller;

import com.linglong.llm.service.ConversationCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI对话控制器
 * 所有请求经由 ConversationCacheService 路由：
 * 先检索向量数据库缓存，命中则返回历史回答，未命中则调用大模型并写入向量库。
 */
@Tag(name = "AI 对话（简单版）", description = "单轮对话接口，优先走向量缓存")
@RestController
@RequestMapping("/ai")
public class ChatbigmodelController {

    @Resource
    private ConversationCacheService conversationCacheService;

    /**
     * 生成AI回复（优先走向量缓存）
     * @param message 用户输入消息
     * @return AI生成的回复
     */
    @Operation(summary = "生成 AI 回复（简单版）", description = "单轮对话，优先检索向量缓存，未命中则调用 LLM")
    @GetMapping("/generate")
    public String generate(@Parameter(description = "用户输入消息", example = "hello") @RequestParam(value = "message", defaultValue = "hello") String message) {
        return conversationCacheService.chat(message);
    }

    /**
     * 指定模型生成AI回复（优先走向量缓存）
     * @param message 用户输入消息
     * @param model 模型名称
     * @return AI生成的回复
     */
    @Operation(summary = "生成 AI 回复（指定模型）", description = "指定模型的对话，优先走向量缓存")
    @GetMapping("/generate/advanced")
    public String generateAdvanced(
            @Parameter(description = "用户输入消息", required = true) @RequestParam(value = "message") String message,
            @Parameter(description = "模型名称，如 glm-4.6 / gpt-5.4-mini", example = "glm-4.6") @RequestParam(value = "model", defaultValue = "glm-4.6") String model) {
        return conversationCacheService.chat(message, model, 0.7, 2000);
    }
}
