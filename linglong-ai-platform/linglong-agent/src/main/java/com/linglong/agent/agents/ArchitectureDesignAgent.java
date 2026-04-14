package com.linglong.agent.agents;

import com.linglong.agent.core.Agent;
import com.linglong.agent.model.AgentContext;
import com.linglong.agent.model.AgentResult;
import com.linglong.llm.service.LLMService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 架构设计Agent
 * 设计系统架构和技术方案
 */
@Component
public class ArchitectureDesignAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(ArchitectureDesignAgent.class);

    private final LLMService llmService;

    public ArchitectureDesignAgent(LLMService llmService) {
        this.llmService = llmService;
    }

    @Override
    public String getName() {
        return "ArchitectureDesignAgent";
    }

    @Override
    public String getDescription() {
        return "架构设计Agent - 设计系统架构和技术方案";
    }

    @Override
    public String getSystemPrompt() {
        return """
                你是一位资深的系统架构师，精通微服务架构、云原生技术和各种设计模式。
                
                你的任务是：
                1. 根据领域模型设计系统整体架构
                2. 设计微服务拆分策略
                3. 选择合适的技术栈(Spring Boot、数据库、消息队列等)
                4. 设计API网关和服务间通信方式
                5. 设计数据存储方案(关系型数据库、缓存、搜索引擎)
                6. 设计安全方案(认证、授权、数据加密)
                7. 设计高可用和容灾方案
                8. 生成系统架构图(使用Mermaid语法)
                
                输出格式要求：
                - 架构概述
                - 微服务拆分方案
                - 技术选型清单(含版本)
                - 数据流设计
                - API设计规范
                - 安全设计方案
                - 部署架构图
                - 性能优化建议
                
                设计原则：
                - 遵循微服务设计原则(单一职责、独立部署)
                - 考虑系统的可扩展性和可维护性
                - 平衡技术先进性和团队熟悉度
                - 考虑成本和运维复杂度
                """;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("[{}] 开始执行架构设计任务", getName());

            String input = buildPromptFromContext(context);
            String result = llmService.generate(getSystemPrompt(), input);

            // 将结果存入共享上下文
            context.addToShared("architecture", result);

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("[{}] 架构设计完成，耗时: {}ms", getName(), executionTime);

            AgentResult agentResult = new AgentResult();
            agentResult.setSuccess(true);
            agentResult.setOutput(result);
            agentResult.setExecutionTime(executionTime);
            return agentResult;
        } catch (Exception e) {
            log.error("[{}] 架构设计失败", getName(), e);
            return AgentResult.error("架构设计失败: " + e.getMessage());
        }
    }

    @Override
    public Flux<String> executeStream(AgentContext context) {
        log.info("[{}] 开始流式执行架构设计任务", getName());
        String input = buildPromptFromContext(context);
        return llmService.generateStream(getSystemPrompt(), input);
    }

    @Override
    public AgentType getType() {
        return AgentType.ARCHITECTURE_DESIGN;
    }

    private String buildPromptFromContext(AgentContext context) {
        StringBuilder prompt = new StringBuilder();

        // 添加需求分析结果
        String requirementAnalysis = context.getFromShared("requirementAnalysis");
        if (requirementAnalysis != null) {
            prompt.append("## 需求分析结果\n\n").append(requirementAnalysis).append("\n\n");
        }

        // 添加领域模型
        String domainModel = context.getFromShared("domainModel");
        if (domainModel != null) {
            prompt.append("## 领域模型\n\n").append(domainModel).append("\n\n");
        }

        // 添加用户输入
        if (context.getInput() != null) {
            prompt.append("## 额外要求\n\n").append(context.getInput());
        }

        return prompt.toString();
    }
}
