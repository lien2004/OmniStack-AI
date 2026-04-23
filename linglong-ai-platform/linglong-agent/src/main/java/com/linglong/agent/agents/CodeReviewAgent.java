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
 * 代码审查Agent
 * 审查代码质量和规范
 */
@Component
public class CodeReviewAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(CodeReviewAgent.class);

    private final LLMService llmService;

    public CodeReviewAgent(LLMService llmService) {
        this.llmService = llmService;
    }

    @Override
    public String getName() {
        return "CodeReviewAgent";
    }

    @Override
    public String getDescription() {
        return "代码审查Agent - 审查代码质量和规范";
    }

    @Override
    public String getSystemPrompt() {
        return """
                你是一位资深的代码审查专家，精通Java编码规范和代码质量分析。
                
                你的任务是：
                1. 检查代码是否符合阿里巴巴Java开发手册
                2. 识别潜在的代码缺陷和Bug
                3. 检查代码的可读性和可维护性
                4. 识别性能瓶颈和优化机会
                5. 检查安全漏洞(SQL注入、XSS等)
                6. 评估代码的测试覆盖率
                7. 提供改进建议和重构方案
                
                审查维度：
                - 命名规范(类名、方法名、变量名)
                - 代码格式(缩进、空格、换行)
                - OOP规范(封装、继承、多态)
                - 并发处理(线程安全、锁使用)
                - 异常处理(try-catch、异常类型)
                - 日志规范(日志级别、内容)
                - 数据库规范(SQL编写、索引使用)
                - 安全规范(输入验证、敏感数据处理)
                
                输出格式要求：
                - 总体评分(1-10分)
                - 问题清单(按严重程度分类)
                - 具体代码片段和改进建议
                - 正面评价(做得好的地方)
                - 优先级排序的改进建议
                """;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("[{}] 开始执行代码审查任务", getName());

            String input = buildPromptFromContext(context);
            String result = llmService.generate(getSystemPrompt(), input);

            // 将结果存入共享上下文
            context.addToShared("codeReview", result);

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("[{}] 代码审查完成，耗时: {}ms", getName(), executionTime);

            AgentResult agentResult = new AgentResult();
            agentResult.setSuccess(true);
            agentResult.setOutput(result);
            agentResult.setExecutionTime(executionTime);
            return agentResult;
        } catch (Exception e) {
            log.error("[{}] 代码审查失败", getName(), e);
            return AgentResult.error("代码审查失败: " + e.getMessage());
        }
    }

    @Override
    public Flux<String> executeStream(AgentContext context) {
        log.info("[{}] 开始流式执行代码审查任务", getName());
        String input = buildPromptFromContext(context);
        return llmService.generateStream(getSystemPrompt(), input);
    }

    @Override
    public AgentType getType() {
        return AgentType.CODE_REVIEW;
    }

    private String buildPromptFromContext(AgentContext context) {
        StringBuilder prompt = new StringBuilder();

        // 添加生成的代码
        String generatedCode = context.getFromShared("generatedCode");
        if (generatedCode != null) {
            prompt.append("## 待审查代码\n\n").append(generatedCode).append("\n\n");
        }

        // 添加需求分析结果
        String requirementAnalysis = context.getFromShared("requirementAnalysis");
        if (requirementAnalysis != null) {
            prompt.append("## 需求分析结果\n\n").append(requirementAnalysis).append("\n\n");
        }

        // 添加用户输入
        if (context.getInput() != null) {
            prompt.append("## 额外要求\n\n").append(context.getInput());
        }

        return prompt.toString();
    }
}
