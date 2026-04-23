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
 * 代码生成Agent
 * 负责根据设计文档生成高质量的代码
 */
@Component
public class CodeGenerationAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(CodeGenerationAgent.class);

    private final LLMService llmService;

    public CodeGenerationAgent(LLMService llmService) {
        this.llmService = llmService;
    }

    @Override
    public String getName() {
        return "CodeGenerationAgent";
    }

    @Override
    public String getDescription() {
        return "代码生成Agent - 根据设计文档生成高质量的Java代码";
    }

    @Override
    public String getSystemPrompt() {
        return """
                你是一位资深的Java架构师和开发工程师，精通Spring Boot、DDD领域驱动设计和微服务架构。
                
                你的任务是：
                1. 根据提供的需求分析和领域模型，生成高质量的Java代码
                2. 遵循阿里巴巴Java开发手册的编码规范
                3. 使用DDD分层架构：Domain、Application、Infrastructure、Interface
                4. 生成完整的代码，包括实体类、值对象、领域服务、应用服务、仓储接口等
                5. 添加必要的注释和JavaDoc
                6. 考虑异常处理、日志记录和性能优化
                
                代码规范要求：
                - 使用Java 17+ 特性（如Record、Pattern Matching等）
                - 遵循SOLID原则
                - 使用Lombok简化代码
                - 使用Spring Boot 3.x 最佳实践
                - 包含单元测试代码
                
                输出格式：
                - 每个文件使用 ```java 代码块包裹
                - 文件顶部使用注释标明文件路径
                - 提供文件清单说明
                """;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("[{}] 开始执行代码生成任务", getName());
            
            String input = buildPromptFromContext(context);
            String result = llmService.generate(getSystemPrompt(), input);
            
            long executionTime = System.currentTimeMillis() - startTime;
            log.info("[{}] 代码生成完成，耗时: {}ms", getName(), executionTime);
            
            AgentResult agentResult = new AgentResult();
            agentResult.setSuccess(true);
            agentResult.setOutput(result);
            agentResult.setExecutionTime(executionTime);
            return agentResult;
        } catch (Exception e) {
            log.error("[{}] 代码生成失败", getName(), e);
            return AgentResult.error("代码生成失败: " + e.getMessage());
        }
    }

    @Override
    public Flux<String> executeStream(AgentContext context) {
        log.info("[{}] 开始流式执行代码生成任务", getName());
        String input = buildPromptFromContext(context);
        return llmService.generateStream(getSystemPrompt(), input);
    }

    @Override
    public AgentType getType() {
        return AgentType.CODE_GENERATION;
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
        
        // 添加架构设计
        String architecture = context.getFromShared("architecture");
        if (architecture != null) {
            prompt.append("## 架构设计\n\n").append(architecture).append("\n\n");
        }
        
        // 添加用户输入
        prompt.append("## 额外要求\n\n").append(context.getInput());
        
        return prompt.toString();
    }
}
