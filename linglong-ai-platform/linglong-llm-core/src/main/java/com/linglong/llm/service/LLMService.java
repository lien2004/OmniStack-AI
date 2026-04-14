package com.linglong.llm.service;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * LLM服务接口
 */
public interface LLMService {

    /**
     * 同步调用LLM生成文本
     *
     * @param prompt 提示词
     * @return 生成的文本
     */
    String generate(String prompt);

    /**
     * 同步调用LLM生成文本(带系统提示)
     *
     * @param systemPrompt 系统提示
     * @param userPrompt   用户提示
     * @return 生成的文本
     */
    String generate(String systemPrompt, String userPrompt);

    /**
     * 流式调用LLM生成文本
     *
     * @param prompt 提示词
     * @return 流式响应
     */
    Flux<String> generateStream(String prompt);

    /**
     * 流式调用LLM生成文本(带系统提示)
     *
     * @param systemPrompt 系统提示
     * @param userPrompt   用户提示
     * @return 流式响应
     */
    Flux<String> generateStream(String systemPrompt, String userPrompt);

    /**
     * 使用消息列表调用LLM
     *
     * @param messages 消息列表
     * @return 生成的文本
     */
    String chat(List<Message> messages);

    /**
     * 使用消息列表流式调用LLM
     *
     * @param messages 消息列表
     * @return 流式响应
     */
    Flux<String> chatStream(List<Message> messages);

    /**
     * 使用Prompt对象调用LLM
     *
     * @param prompt Prompt对象
     * @return 生成的文本
     */
    String chat(Prompt prompt);

    /**
     * 使用Prompt对象流式调用LLM
     *
     * @param prompt Prompt对象
     * @return 流式响应
     */
    Flux<String> chatStream(Prompt prompt);
}
