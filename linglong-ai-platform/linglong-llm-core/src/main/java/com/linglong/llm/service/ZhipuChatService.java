package com.linglong.llm.service;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智谱AI Chat服务
 * 封装了与智谱AI API的交互
 */
@Service
public class ZhipuChatService {

    @Resource
    private com.linglong.llm.config.LLMConfig llmConfig;

    private final RestTemplate restTemplate = new RestTemplate();
    private final String apiUrl = "https://open.bigmodel.cn/api/paas/v4/chat/completions";

    /**
     * 发送消息并获取AI回复
     * @param message 用户消息
     * @return AI回复内容
     */
    public String chat(String message) {
        return chat(message, "glm-4.6", 0.7, 2000);
    }

    /**
     * 发送消息并获取AI回复（带参数）
     * @param message 用户消息
     * @param model 模型名称
     * @param temperature 温度参数
     * @param maxTokens 最大token数
     * @return AI回复内容
     */
    public String chat(String message, String model, Double temperature, Integer maxTokens) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + llmConfig.getApiKey());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", List.of(
            Map.of("role", "user", "content", message)
        ));
        
        if (temperature != null) {
            requestBody.put("temperature", temperature);
        }
        if (maxTokens != null) {
            requestBody.put("max_tokens", maxTokens);
        }

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        Map<String, Object> response = restTemplate.postForObject(apiUrl, request, Map.class);
        
        return extractContent(response);
    }

    /**
     * 带系统上下文的对话（system + user 双角色）
     * 用于文档分析等需要系统提示词的场景
     *
     * @param systemContent 系统提示词（包含文档内容、分析指令等）
     * @param userMessage   用户消息
     * @param model         模型名称
     * @param temperature   温度参数
     * @param maxTokens     最大 Token 数
     * @return AI 回复内容
     */
    public String chatWithContext(String systemContent, String userMessage,
                                  String model, Double temperature, Integer maxTokens) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + llmConfig.getApiKey());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", List.of(
            Map.of("role", "system", "content", systemContent),
            Map.of("role", "user",   "content", userMessage)
        ));
        if (temperature != null) requestBody.put("temperature", temperature);
        if (maxTokens   != null) requestBody.put("max_tokens",  maxTokens);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        Map<String, Object> response = restTemplate.postForObject(apiUrl, request, Map.class);
        return extractContent(response);
    }

    /**
     * 使用Spring AI的Prompt对象进行对话
     * @param prompt Spring AI Prompt对象
     * @return AI回复内容
     */
    public String chat(Prompt prompt) {
        // 从Prompt中提取消息内容
        String message = prompt.getInstructions().stream()
            .filter(msg -> msg instanceof UserMessage)
            .findFirst()
            .map(msg -> msg.getContent())
            .orElse("");
        
        return chat(message);
    }

    /**
     * 从响应中提取内容
     */
    private String extractContent(Map<String, Object> response) {
        if (response == null || !response.containsKey("choices")) {
            throw new RuntimeException("Invalid response from Zhipu API");
        }

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices.isEmpty()) {
            throw new RuntimeException("Empty choices in response");
        }

        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }

    /**
     * 获取Token使用量
     */
    public Map<String, Integer> getUsage(String message) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + llmConfig.getApiKey());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "glm-4");
        requestBody.put("messages", List.of(
            Map.of("role", "user", "content", message)
        ));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        Map<String, Object> response = restTemplate.postForObject(apiUrl, request, Map.class);

        Map<String, Object> usage = (Map<String, Object>) response.get("usage");
        Map<String, Integer> result = new HashMap<>();
        result.put("promptTokens", (Integer) usage.get("prompt_tokens"));
        result.put("completionTokens", (Integer) usage.get("completion_tokens"));
        result.put("totalTokens", (Integer) usage.get("total_tokens"));
        
        return result;
    }
}
