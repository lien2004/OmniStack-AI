package com.linglong.agent.service;

import com.linglong.agent.core.Agent;
import com.linglong.agent.model.AgentConfig;
import com.linglong.agent.model.AgentContext;
import com.linglong.agent.model.AgentResult;
import com.linglong.agent.orchestrator.AgentOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Agent服务层
 * 封装Agent调用逻辑，提供统一的API
 */
@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    private final AgentOrchestrator orchestrator;
    private final ContextManager contextManager;
    private final AgentStatsService statsService;
    private final AgentConfigService configService;

    public AgentService(AgentOrchestrator orchestrator, ContextManager contextManager,
                        AgentStatsService statsService, AgentConfigService configService) {
        this.orchestrator = orchestrator;
        this.contextManager = contextManager;
        this.statsService = statsService;
        this.configService = configService;
    }

    /**
     * 获取所有Agent信息
     */
    public List<AgentInfo> getAllAgents() {
        return orchestrator.getAllAgents().stream()
                .map(this::toAgentInfo)
                .sorted(Comparator.comparing(a -> a.type.name()))
                .collect(Collectors.toList());
    }

    /**
     * 获取所有Agent详细信息（包含统计和配置）
     */
    public List<AgentDetailInfo> getAllAgentDetails() {
        return orchestrator.getAllAgents().stream()
                .map(this::toAgentDetailInfo)
                .sorted(Comparator.comparing(a -> a.type.name()))
                .collect(Collectors.toList());
    }

    /**
     * 获取单个Agent信息
     */
    public AgentInfo getAgentInfo(String agentName) {
        Agent agent = orchestrator.getAgent(agentName);
        if (agent == null) {
            return null;
        }
        return toAgentInfo(agent);
    }

    /**
     * 执行单个Agent
     *
     * @param agentName Agent名称
     * @param taskId    任务ID
     * @param projectId 项目ID（可选）
     * @param userId    用户ID（可选）
     * @param input     输入内容
     * @return 执行结果
     */
    public AgentResult executeAgent(String agentName, String taskId, String projectId, 
                                    String userId, Object input) {
        return executeAgent(agentName, taskId, projectId, userId, input, null, false, null);
    }

    /**
     * 执行单个Agent（完整参数）
     *
     * @param agentName     Agent名称
     * @param taskId        任务ID
     * @param projectId     项目ID（可选）
     * @param userId        用户ID（可选）
     * @param input         输入内容
     * @param outputPath    输出保存路径（可选）
     * @param autoSave      是否自动保存结果
     * @param uploadedFiles 上传的文件列表（可选）
     * @return 执行结果
     */
    public AgentResult executeAgent(String agentName, String taskId, String projectId, 
                                    String userId, Object input, String outputPath, 
                                    boolean autoSave, List<AgentContext.UploadedFile> uploadedFiles) {
        log.info("执行Agent: {}, taskId: {}, outputPath: {}, autoSave: {}", agentName, taskId, outputPath, autoSave);
        
        // 检查Agent是否启用
        if (!statsService.isAgentEnabled(agentName)) {
            return AgentResult.error("Agent已被禁用: " + agentName);
        }
        
        // 构建上下文
        AgentContext context = new AgentContext();
        context.setTaskId(taskId);
        context.setProjectId(projectId);
        context.setUserId(userId);
        context.setInput(input);
        context.setOutputPath(outputPath);
        context.setAutoSaveOutput(autoSave);
        context.setUploadedFiles(uploadedFiles);

        // 尝试从Redis恢复历史上下文
        AgentContext savedContext = contextManager.getContext(taskId);
        if (savedContext != null) {
            context.setSharedContext(savedContext.getSharedContext());
            context.setHistory(savedContext.getHistory());
        }

        // 执行Agent
        AgentResult result = orchestrator.executeAgent(agentName, context);

        // 保存上下文
        if (result.isSuccess()) {
            contextManager.saveContext(taskId, context);
        }

        // 更新统计信息
        statsService.incrementUsageCount(agentName);
        statsService.recordExecution(agentName);

        return result;
    }

    /**
     * 流式执行Agent
     */
    public Flux<String> executeAgentStream(String agentName, String taskId, 
                                           String projectId, String userId, Object input) {
        log.info("流式执行Agent: {}, taskId: {}", agentName, taskId);
        
        AgentContext context = new AgentContext();
        context.setTaskId(taskId);
        context.setProjectId(projectId);
        context.setUserId(userId);
        context.setInput(input);

        // 尝试从Redis恢复历史上下文
        AgentContext savedContext = contextManager.getContext(taskId);
        if (savedContext != null) {
            context.setSharedContext(savedContext.getSharedContext());
            context.setHistory(savedContext.getHistory());
        }

        return orchestrator.executeAgentStream(agentName, context);
    }

    /**
     * 启动标准工作流
     */
    public WorkflowInfo startStandardWorkflow(String projectId, String userId, Object input) {
        log.info("启动标准工作流, projectId: {}, userId: {}", projectId, userId);
        
        AgentContext context = new AgentContext();
        context.setTaskId(UUID.randomUUID().toString());
        context.setProjectId(projectId);
        context.setUserId(userId);
        context.setInput(input);

        var instance = orchestrator.startStandardWorkflow(context);
        
        return toWorkflowInfo(instance);
    }

    /**
     * 获取工作流状态
     */
    public WorkflowInfo getWorkflowStatus(String workflowId) {
        var instance = orchestrator.getWorkflowInstance(workflowId);
        if (instance == null) {
            return null;
        }
        return toWorkflowInfo(instance);
    }

    /**
     * 转换Agent信息
     */
    private AgentInfo toAgentInfo(Agent agent) {
        AgentInfo info = new AgentInfo();
        info.name = agent.getName();
        info.description = agent.getDescription();
        info.type = agent.getType();
        info.systemPrompt = agent.getSystemPrompt();
        return info;
    }

    /**
     * 转换Agent详细信息（包含统计）
     */
    private AgentDetailInfo toAgentDetailInfo(Agent agent) {
        AgentDetailInfo info = new AgentDetailInfo();
        info.name = agent.getName();
        info.description = agent.getDescription();
        info.type = agent.getType();
        info.systemPrompt = agent.getSystemPrompt();
        
        // 获取统计信息
        info.usageCount = statsService.getUsageCount(agent.getName());
        info.enabled = statsService.isAgentEnabled(agent.getName());
        info.lastExecutionTime = statsService.getLastExecutionTime(agent.getName());
        
        // 获取自定义配置
        AgentConfig config = configService.getConfig(agent.getName());
        if (config != null) {
            if (config.getDisplayName() != null) {
                info.displayName = config.getDisplayName();
            }
            if (config.getDescription() != null) {
                info.description = config.getDescription();
            }
            if (config.getSystemPrompt() != null) {
                info.systemPrompt = config.getSystemPrompt();
            }
            info.parameters = config.getParameters();
        }
        
        return info;
    }

    /**
     * 转换工作流信息
     */
    private WorkflowInfo toWorkflowInfo(AgentOrchestrator.WorkflowInstance instance) {
        WorkflowInfo info = new WorkflowInfo();
        info.id = instance.getId();
        info.type = instance.getType();
        info.status = instance.getStatus().name();
        info.currentStepIndex = instance.getCurrentStepIndex();
        info.totalSteps = instance.getSteps().size();
        
        info.steps = instance.getSteps().stream()
                .map(step -> {
                    StepInfo stepInfo = new StepInfo();
                    stepInfo.agentName = step.getAgentName();
                    stepInfo.name = step.getName();
                    stepInfo.status = step.getStatus().name();
                    if (step.getResult() != null) {
                        stepInfo.output = step.getResult().getOutput();
                        stepInfo.success = step.getResult().isSuccess();
                        stepInfo.errorMessage = step.getResult().getErrorMessage();
                    }
                    return stepInfo;
                })
                .collect(Collectors.toList());
        
        return info;
    }

    // ===== DTO 类 =====

    public static class AgentInfo {
        public String name;
        public String description;
        public Agent.AgentType type;
        public String systemPrompt;
    }

    public static class AgentDetailInfo extends AgentInfo {
        public String displayName;
        public long usageCount;
        public boolean enabled;
        public String lastExecutionTime;
        public Map<String, Object> parameters;
    }

    public static class WorkflowInfo {
        public String id;
        public String type;
        public String status;
        public int currentStepIndex;
        public int totalSteps;
        public List<StepInfo> steps;
    }

    public static class StepInfo {
        public String agentName;
        public String name;
        public String status;
        public String output;
        public boolean success;
        public String errorMessage;
    }
}
