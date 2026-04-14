package com.linglong.llm.controller;

import com.linglong.llm.service.ConversationCacheService;
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
    @GetMapping("/generate")
    public String generate(@RequestParam(value = "message", defaultValue = "hello") String message) {
        return conversationCacheService.chat(message);
    }

    /**
     * 指定模型生成AI回复（优先走向量缓存）
     * @param message 用户输入消息
     * @param model 模型名称
     * @return AI生成的回复
     */
    @GetMapping("/generate/advanced")
    public String generateAdvanced(
            @RequestParam(value = "message") String message,
            @RequestParam(value = "model", defaultValue = "glm-4.6") String model) {
        return conversationCacheService.chat(message, model, 0.7, 2000);
    }
}
