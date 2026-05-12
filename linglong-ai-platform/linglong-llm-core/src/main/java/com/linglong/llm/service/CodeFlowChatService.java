package com.linglong.llm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * CodeFlow对话服务（OpenAI兼容接口）
 * 调用 CodeFlow 平台灵龙AI模型进行对话生成
 */
@Service
public class CodeFlowChatService {

    private static final Logger log = LoggerFactory.getLogger(CodeFlowChatService.class);

    @Value("${bailian.base-url:https://codeflow.asia/v1}")
    private String baseUrl;

    @Value("${bailian.api-key}")
    private String apiKey;

    @Value("${bailian.model:gpt-5.5}")
    private String chatModel;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 发送消息并获取AI回复
     *
     * @param message 用户消息
     * @return AI回复内容
     */
    public String chat(String message) {
        return chat(message, chatModel);
    }

    /**
     * 指定模型进行对话
     *
     * @param message 用户消息
     * @param model   模型名称
     * @return AI回复内容
     */
    @SuppressWarnings("unchecked")
    public String chat(String message, String model) {
        String url = baseUrl + "/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "user", "content", message)
                ),
                "stream", false
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        log.debug("调用CodeFlow对话: model={}, message长度={}", model, message.length());

        Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

        if (response == null || !response.containsKey("choices")) {
            throw new RuntimeException("CodeFlow API返回无效响应: " + response);
        }

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("CodeFlow API返回空choices");
        }

        Map<String, Object> responseMessage = (Map<String, Object>) choices.get(0).get("message");
        if (responseMessage == null) {
            throw new RuntimeException("CodeFlow API返回的message为null，原始响应: " + response);
        }

        // 兼容推理模型：content 可能为 null，回复在 reasoning_content 字段
        String content = (String) responseMessage.get("content");
        if (content == null) {
            content = (String) responseMessage.get("reasoning_content");
        }
        if (content == null) {
            log.warn("CodeFlow API响应message结构: {}", responseMessage);
            throw new RuntimeException("CodeFlow API返回的content和reasoning_content均为null");
        }

        log.debug("CodeFlow回复长度: {}", content.length());
        return content;
    }
}
