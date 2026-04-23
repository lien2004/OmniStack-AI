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
 * 需求分析Agent
 * 负责分析用户需求，提取业务规则和功能点
 */
@Component
public class RequirementAnalysisAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(RequirementAnalysisAgent.class);

    private final LLMService llmService;

    public RequirementAnalysisAgent(LLMService llmService) {
        this.llmService = llmService;
    }

    @Override
    public String getName() {
        return "RequirementAnalysisAgent";
    }

    @Override
    public String getDescription() {
        return "需求分析Agent - 分析用户需求，提取业务规则和功能点";
    }

    @Override
    public String getSystemPrompt() {
        return """
                你是一位资深的产品经理和业务分析师，擅长从各种需求文档中提取核心业务规则和功能需求。
                
                你的任务是：
                1. 仔细阅读用户提供的需求描述或文档
                2. 提取关键业务实体和它们之间的关系
                3. 识别核心业务流程和规则
                4. 列出功能需求清单(功能性需求和非功能性需求)
                5. 识别潜在的风险和约束条件
                
                输出格式要求：
                - 使用结构化的方式输出分析结果
                - 包含业务实体、业务规则、功能列表、非功能需求
                - 如果有不明确的地方，列出需要澄清的问题
                
                注意：保持专业、简洁，专注于业务层面而非技术实现。
                """;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("[{}] 开始执行需求分析任务", getName());
            
            String input = (String) context.getInput();
            String result = llmService.generate(getSystemPrompt(), input);
            
            long executionTime = System.currentTimeMillis() - startTime;
            log.info("[{}] 需求分析完成，耗时: {}ms", getName(), executionTime);
            
            AgentResult agentResult = new AgentResult();
            agentResult.setSuccess(true);
            agentResult.setOutput(result);
            agentResult.setExecutionTime(executionTime);
            return agentResult;
        } catch (Exception e) {
            log.error("[{}] 需求分析失败", getName(), e);
            return AgentResult.error("需求分析失败: " + e.getMessage());
        }
    }

    @Override
    public Flux<String> executeStream(AgentContext context) {
        log.info("[{}] 开始流式执行需求分析任务", getName());
        String input = (String) context.getInput();
        return llmService.generateStream(getSystemPrompt(), input);
    }

    @Override
    public AgentType getType() {
        return AgentType.REQUIREMENT_ANALYSIS;
    }
}
