package com.linglong.engine.llm;

import reactor.core.publisher.Flux;

public interface LLMService {
    String generate(String prompt);
    String generate(String systemPrompt, String userPrompt);
    Flux<String> generateStream(String prompt);
    Flux<String> generateStream(String systemPrompt, String userPrompt);
}
