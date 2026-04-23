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
 * 文档生成Agent
 * 生成API文档、README等
 */
@Component
public class DocumentationAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(DocumentationAgent.class);

    private final LLMService llmService;

    public DocumentationAgent(LLMService llmService) {
        this.llmService = llmService;
    }

    @Override
    public String getName() {
        return "DocumentationAgent";
    }

    @Override
    public String getDescription() {
        return "文档生成Agent - 生成API文档、README、技术文档等";
    }

    @Override
    public String getSystemPrompt() {
        return """
                你是一位资深的技术文档工程师，擅长编写清晰、专业的技术文档。
                
                你的任务是：
                1. 生成API接口文档(OpenAPI/Swagger格式)
                2. 编写项目README文档
                3. 生成架构设计文档
                4. 编写部署和运维文档
                5. 生成开发指南和贡献指南
                
                文档规范：
                - 使用Markdown格式
                - 包含清晰的目录结构
                - 提供代码示例
                - 使用表格展示参数和返回值
                - 包含完整的错误码说明
                
                输出格式要求：
                - README.md：项目概述、快速开始、技术栈
                - API.md：接口列表、请求参数、响应示例
                - ARCHITECTURE.md：架构设计、模块说明
                - DEPLOYMENT.md：部署步骤、环境配置
                
                文档质量要求：
                - 语言简洁明了
                - 结构清晰，易于导航
                - 示例代码可运行
                - 涵盖常见问题解答
                """;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("[{}] 开始执行文档生成任务", getName());

            String input = buildPromptFromContext(context);
            String result = llmService.generate(getSystemPrompt(), input);

            // 将结果存入共享上下文
            context.addToShared("documentation", result);

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("[{}] 文档生成完成，耗时: {}ms", getName(), executionTime);

            AgentResult agentResult = new AgentResult();
            agentResult.setSuccess(true);
            agentResult.setOutput(result);
            agentResult.setExecutionTime(executionTime);
            return agentResult;
        } catch (Exception e) {
            log.error("[{}] 文档生成失败", getName(), e);
            return AgentResult.error("文档生成失败: " + e.getMessage());
        }
    }

    @Override
    public Flux<String> executeStream(AgentContext context) {
        log.info("[{}] 开始流式执行文档生成任务", getName());
        String input = buildPromptFromContext(context);
        return llmService.generateStream(getSystemPrompt(), input);
    }

    @Override
    public AgentType getType() {
        return AgentType.DOCUMENTATION;
    }

    private String buildPromptFromContext(AgentContext context) {
        StringBuilder prompt = new StringBuilder();

        // 添加生成的代码
        String generatedCode = context.getFromShared("generatedCode");
        if (generatedCode != null) {
            prompt.append("## 项目代码\n\n").append(generatedCode).append("\n\n");
        }

        // 添加需求分析结果
        String requirementAnalysis = context.getFromShared("requirementAnalysis");
        if (requirementAnalysis != null) {
            prompt.append("## 需求分析结果\n\n").append(requirementAnalysis).append("\n\n");
        }

        // 添加架构设计
        String architecture = context.getFromShared("architecture");
        if (architecture != null) {
            prompt.append("## 架构设计\n\n").append(architecture).append("\n\n");
        }

        // 添加用户输入
        if (context.getInput() != null) {
            prompt.append("## 额外要求\n\n").append(context.getInput());
        }

        return prompt.toString();
    }
}
