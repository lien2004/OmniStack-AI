package com.linglong.llm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * LLM配置类
 */
@Configuration
public class LLMConfig {

    @Value("${zhipu.api-key:397f408166f94e93a63e9f33cd3cd875.0T9IB0n1QU0dMbIy}")
    private String apiKey;

    public String getApiKey() {
        return apiKey;
    }
}
