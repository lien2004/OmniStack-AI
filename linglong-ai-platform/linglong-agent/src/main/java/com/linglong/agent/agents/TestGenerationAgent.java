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
 * 测试生成Agent
 * 生成单元测试和集成测试
 */
@Component
public class TestGenerationAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(TestGenerationAgent.class);

    private final LLMService llmService;

    public TestGenerationAgent(LLMService llmService) {
        this.llmService = llmService;
    }

    @Override
    public String getName() {
        return "TestGenerationAgent";
    }

    @Override
    public String getDescription() {
        return "测试生成Agent - 生成单元测试和集成测试";
    }

    @Override
    public String getSystemPrompt() {
        return """
                你是一位资深的测试工程师，精通JUnit 5、Mockito和Spring Boot测试。
                
                你的任务是：
                1. 根据代码生成全面的单元测试
                2. 生成集成测试
                3. 设计边界条件和异常场景测试
                4. 生成测试数据和Mock对象
                5. 确保测试覆盖率达到80%以上
                6. 遵循Given-When-Then测试结构
                
                测试类型：
                - 单元测试：测试单个方法，使用Mockito隔离依赖
                - 集成测试：测试组件间交互，使用@SpringBootTest
                - 参数化测试：使用@ParameterizedTest测试多种输入
                - 异常测试：测试异常情况处理
                - 性能测试：测试方法性能指标
                
                测试原则：
                - 一个测试方法只测试一个概念
                - 测试方法名要清晰描述测试场景
                - 使用AssertJ进行断言
                - 避免测试间的相互依赖
                - 测试代码要易于维护
                
                输出格式要求：
                - 每个测试类使用 ```java 代码块包裹
                - 文件顶部使用注释标明文件路径
                - 包含测试覆盖率报告
                - 提供测试执行说明
                """;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("[{}] 开始执行测试生成任务", getName());

            String input = buildPromptFromContext(context);
            String result = llmService.generate(getSystemPrompt(), input);

            // 将结果存入共享上下文
            context.addToShared("testCode", result);

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("[{}] 测试生成完成，耗时: {}ms", getName(), executionTime);

            AgentResult agentResult = new AgentResult();
            agentResult.setSuccess(true);
            agentResult.setOutput(result);
            agentResult.setExecutionTime(executionTime);
            return agentResult;
        } catch (Exception e) {
            log.error("[{}] 测试生成失败", getName(), e);
            return AgentResult.error("测试生成失败: " + e.getMessage());
        }
    }

    @Override
    public Flux<String> executeStream(AgentContext context) {
        log.info("[{}] 开始流式执行测试生成任务", getName());
        String input = buildPromptFromContext(context);
        return llmService.generateStream(getSystemPrompt(), input);
    }

    @Override
    public AgentType getType() {
        return AgentType.TEST_GENERATION;
    }

    private String buildPromptFromContext(AgentContext context) {
        StringBuilder prompt = new StringBuilder();

        // 添加生成的代码
        String generatedCode = context.getFromShared("generatedCode");
        if (generatedCode != null) {
            prompt.append("## 待测试代码\n\n").append(generatedCode).append("\n\n");
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
