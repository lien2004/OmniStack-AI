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
 * 调试Agent
 * 分析错误日志，提供调试建议
 */
@Component
public class DebuggingAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(DebuggingAgent.class);

    private final LLMService llmService;

    public DebuggingAgent(LLMService llmService) {
        this.llmService = llmService;
    }

    @Override
    public String getName() {
        return "DebuggingAgent";
    }

    @Override
    public String getDescription() {
        return "调试Agent - 分析错误日志，提供调试建议和修复方案";
    }

    @Override
    public String getSystemPrompt() {
        return """
                你是一位资深的调试专家，擅长分析错误日志和定位问题根因。
                
                你的任务是：
                1. 分析错误日志和异常堆栈
                2. 定位问题根因
                3. 提供修复建议和代码补丁
                4. 分析性能瓶颈
                5. 提供调试技巧和工具建议
                
                分析维度：
                - 异常类型和错误信息
                - 堆栈跟踪分析
                - 代码上下文分析
                - 环境配置检查
                - 依赖版本兼容性
                - 并发和线程问题
                - 内存泄漏分析
                
                输出格式要求：
                - 问题概述
                - 根因分析
                - 修复方案(多种选择)
                - 代码补丁示例
                - 预防措施建议
                - 调试工具推荐
                
                调试原则：
                - 从现象到本质，层层深入
                - 提供可验证的修复方案
                - 考虑修复的副作用
                - 提供回归测试建议
                """;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("[{}] 开始执行调试分析任务", getName());

            String input = buildPromptFromContext(context);
            String result = llmService.generate(getSystemPrompt(), input);

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("[{}] 调试分析完成，耗时: {}ms", getName(), executionTime);

            AgentResult agentResult = new AgentResult();
            agentResult.setSuccess(true);
            agentResult.setOutput(result);
            agentResult.setExecutionTime(executionTime);
            return agentResult;
        } catch (Exception e) {
            log.error("[{}] 调试分析失败", getName(), e);
            return AgentResult.error("调试分析失败: " + e.getMessage());
        }
    }

    @Override
    public Flux<String> executeStream(AgentContext context) {
        log.info("[{}] 开始流式执行调试分析任务", getName());
        String input = buildPromptFromContext(context);
        return llmService.generateStream(getSystemPrompt(), input);
    }

    @Override
    public AgentType getType() {
        return AgentType.DEBUGGING;
    }

    private String buildPromptFromContext(AgentContext context) {
        StringBuilder prompt = new StringBuilder();

        // 添加错误日志
        String errorLog = context.getFromShared("errorLog");
        if (errorLog != null) {
            prompt.append("## 错误日志\n\n").append(errorLog).append("\n\n");
        }

        // 添加相关代码
        String code = context.getFromShared("code");
        if (code != null) {
            prompt.append("## 相关代码\n\n").append(code).append("\n\n");
        }

        // 添加用户输入
        if (context.getInput() != null) {
            prompt.append("## 问题描述\n\n").append(context.getInput());
        }

        return prompt.toString();
    }
}
