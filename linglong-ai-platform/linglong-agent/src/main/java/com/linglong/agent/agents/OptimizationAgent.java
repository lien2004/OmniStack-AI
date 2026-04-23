package com.linglong.agent.agents;

import com.linglong.agent.core.Agent;
import com.linglong.agent.model.AgentContext;
import com.linglong.agent.model.AgentResult;
import com.linglong.agent.llm.LLMService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 优化Agent
 * 代码性能优化和重构建议
 */
@Component
public class OptimizationAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(OptimizationAgent.class);

    private final LLMService llmService;

    public OptimizationAgent(LLMService llmService) {
        this.llmService = llmService;
    }

    @Override
    public String getName() {
        return "OptimizationAgent";
    }

    @Override
    public String getDescription() {
        return "优化Agent - 提供代码性能优化和重构建议";
    }

    @Override
    public String getSystemPrompt() {
        return """
                你是一位资深的性能优化专家，精通Java性能调优和代码重构。
                
                你的任务是：
                1. 分析代码性能瓶颈
                2. 识别内存泄漏风险
                3. 优化算法和数据结构
                4. 优化数据库查询
                5. 优化并发处理
                6. 提供重构建议
                
                优化维度：
                - 时间复杂度优化
                - 空间复杂度优化
                - 数据库查询优化
                - 缓存策略优化
                - 并发性能优化
                - JVM参数调优
                - 垃圾回收优化
                
                输出格式要求：
                - 性能分析报告
                - 优化前后的对比
                - 具体的优化代码
                - 性能测试建议
                - 重构优先级排序
                
                优化原则：
                - 先测量，后优化
                - 避免过早优化
                - 保持代码可读性
                - 考虑维护成本
                - 提供基准测试数据
                """;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("[{}] 开始执行性能优化分析任务", getName());

            String input = buildPromptFromContext(context);
            String result = llmService.generate(getSystemPrompt(), input);

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("[{}] 性能优化分析完成，耗时: {}ms", getName(), executionTime);

            AgentResult agentResult = new AgentResult();
            agentResult.setSuccess(true);
            agentResult.setOutput(result);
            agentResult.setExecutionTime(executionTime);
            return agentResult;
        } catch (Exception e) {
            log.error("[{}] 性能优化分析失败", getName(), e);
            return AgentResult.error("性能优化分析失败: " + e.getMessage());
        }
    }

    @Override
    public Flux<String> executeStream(AgentContext context) {
        log.info("[{}] 开始流式执行性能优化分析任务", getName());
        String input = buildPromptFromContext(context);
        return llmService.generateStream(getSystemPrompt(), input);
    }

    @Override
    public AgentType getType() {
        return AgentType.OPTIMIZATION;
    }

    private String buildPromptFromContext(AgentContext context) {
        StringBuilder prompt = new StringBuilder();

        // 添加待优化代码
        String code = context.getFromShared("code");
        if (code != null) {
            prompt.append("## 待优化代码\n\n").append(code).append("\n\n");
        }

        // 添加性能数据
        String performanceData = context.getFromShared("performanceData");
        if (performanceData != null) {
            prompt.append("## 性能数据\n\n").append(performanceData).append("\n\n");
        }

        // 添加用户输入
        if (context.getInput() != null) {
            prompt.append("## 优化目标\n\n").append(context.getInput());
        }

        return prompt.toString();
    }
}
