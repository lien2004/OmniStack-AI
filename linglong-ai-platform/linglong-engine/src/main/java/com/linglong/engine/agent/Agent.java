package com.linglong.engine.agent;

import reactor.core.publisher.Flux;

/**
 * Agent接口（Engine模块独立副本）
 */
public interface Agent {

    String getName();
    String getDescription();
    String getSystemPrompt();
    AgentResult execute(AgentContext context);
    Flux<String> executeStream(AgentContext context);

    default boolean validate(AgentContext context) {
        return context != null && context.getInput() != null;
    }

    AgentType getType();

    enum AgentType {
        REQUIREMENT_ANALYSIS, DOMAIN_MODELING, ARCHITECTURE_DESIGN,
        CODE_GENERATION, CODE_REVIEW, TEST_GENERATION,
        DOCUMENTATION, DEPLOYMENT, DEBUGGING, OPTIMIZATION
    }
}
