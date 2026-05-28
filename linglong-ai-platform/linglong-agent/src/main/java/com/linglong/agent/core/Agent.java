package com.linglong.agent.core;

import com.linglong.agent.model.AgentContext;
import com.linglong.agent.model.AgentResult;
import reactor.core.publisher.Flux;

/**
 * Agent接口 - 所有Agent的基类
 */
public interface Agent {

    /**
     * 获取Agent名称
     *
     * @return Agent名称
     */
    String getName();

    /**
     * 获取Agent描述
     *
     * @return Agent描述
     */
    String getDescription();

    /**
     * 获取Agent角色定义(System Prompt)
     *
     * @return System Prompt
     */
    String getSystemPrompt();

    /**
     * 执行Agent任务(同步)
     *
     * @param context 执行上下文
     * @return 执行结果
     */
    AgentResult execute(AgentContext context);

    /**
     * 执行Agent任务(流式)
     *
     * @param context 执行上下文
     * @return 流式执行结果
     */
    Flux<String> executeStream(AgentContext context);

    /**
     * 验证输入参数
     *
     * @param context 执行上下文
     * @return 是否验证通过
     */
    default boolean validate(AgentContext context) {
        return context != null && context.getInput() != null;
    }

    /**
     * 获取Agent类型
     *
     * @return Agent类型
     */
    AgentType getType();

    /**
     * Agent类型枚举
     */
    enum AgentType {
        REQUIREMENT_ANALYSIS,   // 需求分析Agent
        DOMAIN_MODELING,        // 领域建模Agent
        ARCHITECTURE_DESIGN,    // 架构设计Agent
        CODE_GENERATION,        // 代码生成Agent
        CODE_REVIEW,            // 代码审查Agent
        TEST_GENERATION,        // 测试生成Agent
        DOCUMENTATION,          // 文档生成Agent
        DEPLOYMENT,             // 部署Agent
        DEBUGGING,              // 调试Agent
        OPTIMIZATION,           // 优化Agent
        RESUME,                 // 简历生成Agent
        PPT                     // PPT生成Agent
    }
}
