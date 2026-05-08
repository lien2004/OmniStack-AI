package com.linglong.llm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * LLM配置类
 */
@Configuration
public class LLMConfig {

    @Value("${zhipu.api-key:3fdfff7268f548058f57baa43dce3e5e.KAUNLL8CTCzXj5uv}")
    private String apiKey;

    public String getApiKey() {
        return apiKey;
    }
}