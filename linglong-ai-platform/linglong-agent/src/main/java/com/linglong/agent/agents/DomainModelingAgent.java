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
 * 领域建模Agent
 * 基于需求分析结果构建领域模型
 */
@Component
public class DomainModelingAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(DomainModelingAgent.class);

    private final LLMService llmService;

    public DomainModelingAgent(LLMService llmService) {
        this.llmService = llmService;
    }

    @Override
    public String getName() {
        return "DomainModelingAgent";
    }

    @Override
    public String getDescription() {
        return "领域建模Agent - 基于需求分析结果构建领域模型";
    }

    @Override
    public String getSystemPrompt() {
        return """
                你是一位资深的领域驱动设计(DDD)专家，擅长从业务需求中提取领域模型。
                
                你的任务是：
                1. 分析需求分析结果，识别核心业务领域
                2. 识别领域实体(Entity)、值对象(Value Object)和聚合根(Aggregate Root)
                3. 定义实体之间的关系(一对一、一对多、多对多)
                4. 识别领域事件(Domain Event)
                5. 划分限界上下文(Bounded Context)
                6. 定义领域服务(Domain Service)和应用服务(Application Service)
                
                输出格式要求：
                - 领域模型概述
                - 实体清单(包含属性、行为)
                - 值对象清单
                - 聚合根定义
                - 实体关系图(使用Mermaid语法)
                - 领域事件清单
                - 限界上下文划分
                - 领域服务定义
                
                设计原则：
                - 遵循DDD战术设计和战略设计原则
                - 确保模型的高内聚、低耦合
                - 识别业务不变量(Invariants)
                - 考虑未来的可扩展性
                """;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("[{}] 开始执行领域建模任务", getName());

            String input = buildPromptFromContext(context);
            String result = llmService.generate(getSystemPrompt(), input);

            // 将结果存入共享上下文
            context.addToShared("domainModel", result);

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("[{}] 领域建模完成，耗时: {}ms", getName(), executionTime);

            AgentResult agentResult = new AgentResult();
            agentResult.setSuccess(true);
            agentResult.setOutput(result);
            agentResult.setExecutionTime(executionTime);
            return agentResult;
        } catch (Exception e) {
            log.error("[{}] 领域建模失败", getName(), e);
            return AgentResult.error("领域建模失败: " + e.getMessage());
        }
    }

    @Override
    public Flux<String> executeStream(AgentContext context) {
        log.info("[{}] 开始流式执行领域建模任务", getName());
        String input = buildPromptFromContext(context);
        return llmService.generateStream(getSystemPrompt(), input);
    }

    @Override
    public AgentType getType() {
        return AgentType.DOMAIN_MODELING;
    }

    private String buildPromptFromContext(AgentContext context) {
        StringBuilder prompt = new StringBuilder();

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
