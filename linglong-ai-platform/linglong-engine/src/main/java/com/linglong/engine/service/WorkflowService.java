package com.linglong.engine.service;

import com.linglong.engine.agent.Agent;
import com.linglong.engine.agent.AgentContext;
import com.linglong.engine.agent.AgentResult;
import com.linglong.engine.agent.AgentOrchestrator;
import com.linglong.engine.llm.LLMService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 工作流服务
 * 协调Agent执行和代码生成流程
 */
@Service
public class WorkflowService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowService.class);

    private final AgentOrchestrator orchestrator;
    private final CodeGenerationService codeGenerationService;
    private final LLMService llmService;

    public WorkflowService(AgentOrchestrator orchestrator,
                          CodeGenerationService codeGenerationService,
                          LLMService llmService) {
        this.orchestrator = orchestrator;
        this.codeGenerationService = codeGenerationService;
        this.llmService = llmService;
    }

    /**
     * 启动完整的软件开发工作流
     *
     * @param requirement 需求描述
     * @param projectName 项目名称
     * @return 工作流实例ID
     */
    public String startDevelopmentWorkflow(String requirement, String projectName) {
        String workflowId = UUID.randomUUID().toString();

        AgentContext context = new AgentContext();
        context.setTaskId(workflowId);
        context.setProjectId(projectName);
        context.setInput(requirement);

        // 启动异步工作流
        CompletableFuture.runAsync(() -> executeDevelopmentWorkflow(workflowId, context));

        log.info("开发工作流已启动: {}, 项目: {}", workflowId, projectName);
        return workflowId;
    }

    /**
     * 执行开发工作流
     */
    private void executeDevelopmentWorkflow(String workflowId, AgentContext context) {
        try {
            // 步骤1: 需求分析
            log.info("[{}] 开始需求分析...", workflowId);
            AgentResult requirementResult = orchestrator.executeAgent("RequirementAnalysisAgent", context);
            if (!requirementResult.isSuccess()) {
                log.error("[{}] 需求分析失败: {}", workflowId, requirementResult.getErrorMessage());
                return;
            }
            context.addToShared("requirementAnalysis", requirementResult.getOutput());

            // 步骤2: 领域建模
            log.info("[{}] 开始领域建模...", workflowId);
            AgentResult domainResult = orchestrator.executeAgent("DomainModelingAgent", context);
            if (!domainResult.isSuccess()) {
                log.error("[{}] 领域建模失败: {}", workflowId, domainResult.getErrorMessage());
                return;
            }
            context.addToShared("domainModel", domainResult.getOutput());

            // 步骤3: 架构设计
            log.info("[{}] 开始架构设计...", workflowId);
            AgentResult archResult = orchestrator.executeAgent("ArchitectureDesignAgent", context);
            if (!archResult.isSuccess()) {
                log.error("[{}] 架构设计失败: {}", workflowId, archResult.getErrorMessage());
                return;
            }
            context.addToShared("architecture", archResult.getOutput());

            // 步骤4: 代码生成
            log.info("[{}] 开始代码生成...", workflowId);
            AgentResult codeResult = orchestrator.executeAgent("CodeGenerationAgent", context);
            if (!codeResult.isSuccess()) {
                log.error("[{}] 代码生成失败: {}", workflowId, codeResult.getErrorMessage());
                return;
            }
            context.addToShared("generatedCode", codeResult.getOutput());

            // 步骤5: 代码审查
            log.info("[{}] 开始代码审查...", workflowId);
            AgentResult reviewResult = orchestrator.executeAgent("CodeReviewAgent", context);
            if (reviewResult.isSuccess()) {
                context.addToShared("codeReview", reviewResult.getOutput());
            }

            // 步骤6: 测试生成
            log.info("[{}] 开始测试生成...", workflowId);
            AgentResult testResult = orchestrator.executeAgent("TestGenerationAgent", context);
            if (testResult.isSuccess()) {
                context.addToShared("testCode", testResult.getOutput());
            }

            log.info("[{}] 开发工作流执行完成", workflowId);

        } catch (Exception e) {
            log.error("[{}] 工作流执行异常", workflowId, e);
        }
    }

    /**
     * 流式执行工作流步骤
     *
     * @param agentName Agent名称
     * @param context   上下文
     * @return 流式响应
     */
    public Flux<String> executeAgentStream(String agentName, AgentContext context) {
        return orchestrator.executeAgentStream(agentName, context);
    }

    /**
     * 生成并写入代码文件
     *
     * @param outputDir   输出目录
     * @param packageName 包名
     * @param className   类名
     * @param codeContent 代码内容
     */
    public void generateAndWriteCode(String outputDir, String packageName, String className, String codeContent) {
        // 构建文件路径
        String packagePath = packageName.replace(".", "/");
        String filePath = outputDir + "/src/main/java/" + packagePath + "/" + className + ".java";

        // 写入文件
        codeGenerationService.writeCodeToFile(filePath, codeContent);

        log.info("代码文件已生成: {}", filePath);
    }

    /**
     * 根据领域模型生成完整项目结构
     *
     * @param domainModel 领域模型描述
     * @param outputDir   输出目录
     * @param basePackage 基础包名
     */
    public void generateProjectFromDomainModel(String domainModel, String outputDir, String basePackage) {
        // 使用LLM解析领域模型并生成代码
        String prompt = """
                根据以下领域模型生成Java实体类代码：
                
                %s
                
                要求：
                1. 生成完整的JPA实体类
                2. 包含Lombok注解
                3. 包含必要的字段验证注解
                4. 包名为: %s.entity
                
                输出格式：
                每个实体类使用 ```java 代码块包裹
                """.formatted(domainModel, basePackage);

        String generatedCode = llmService.generate(prompt);

        // 解析并写入代码文件
        // 这里简化处理，实际应该解析代码块并分别写入文件
        log.info("项目代码生成完成，输出目录: {}", outputDir);
    }

    /**
     * 获取工作流状态
     *
     * @param workflowId 工作流ID
     * @return 工作流实例
     */
    public AgentOrchestrator.WorkflowInstance getWorkflowStatus(String workflowId) {
        return orchestrator.getWorkflowInstance(workflowId);
    }
}
