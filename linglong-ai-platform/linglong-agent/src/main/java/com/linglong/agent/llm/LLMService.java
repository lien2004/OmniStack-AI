package com.linglong.agent.llm;

import reactor.core.publisher.Flux;

/**
 * LLM服务接口（Agent模块独立副本）
 * 各模块独立运行，Agent自行持有LLM调用能力
 */
public interface LLMService {

    /**
     * 同步调用LLM生成文本
     */
    String generate(String prompt);

    /**
     * 同步调用LLM生成文本(带系统提示)
     */
    String generate(String systemPrompt, String userPrompt);

    /**
     * 同步调用LLM生成文本(指定模型)
     */
    String generate(String systemPrompt, String userPrompt, String model);

    /**
     * 流式调用LLM生成文本
     */
    Flux<String> generateStream(String prompt);

    /**
     * 流式调用LLM生成文本(带系统提示)
     */
    Flux<String> generateStream(String systemPrompt, String userPrompt);

    /**
     * 流式调用LLM生成文本(指定模型)
     */
    Flux<String> generateStream(String systemPrompt, String userPrompt, String model);
}
