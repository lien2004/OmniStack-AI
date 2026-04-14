package com.linglong.agent.orchestrator;

import com.linglong.agent.core.Agent;
import com.linglong.agent.model.AgentContext;
import com.linglong.agent.model.AgentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent编排器
 * 负责协调多个Agent的执行流程
 */
@Component
public class AgentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AgentOrchestrator.class);

    private final Map<String, Agent> agents = new ConcurrentHashMap<>();
    private final Map<String, WorkflowInstance> workflowInstances = new ConcurrentHashMap<>();

    /**
     * 注册Agent
     */
    public void registerAgent(Agent agent) {
        agents.put(agent.getName(), agent);
        log.info("Agent已注册: {} - {}", agent.getName(), agent.getDescription());
    }

    /**
     * 获取Agent
     */
    public Agent getAgent(String name) {
        return agents.get(name);
    }

    /**
     * 获取所有Agent
     */
    public Collection<Agent> getAllAgents() {
        return agents.values();
    }

    /**
     * 执行单个Agent
     */
    public AgentResult executeAgent(String agentName, AgentContext context) {
        Agent agent = agents.get(agentName);
        if (agent == null) {
            return AgentResult.error("Agent不存在: " + agentName);
        }

        if (!agent.validate(context)) {
            return AgentResult.error("Agent输入验证失败");
        }

        return agent.execute(context);
    }

    /**
     * 流式执行单个Agent
     */
    public Flux<String> executeAgentStream(String agentName, AgentContext context) {
        Agent agent = agents.get(agentName);
        if (agent == null) {
            return Flux.error(new RuntimeException("Agent不存在: " + agentName));
        }

        if (!agent.validate(context)) {
            return Flux.error(new RuntimeException("Agent输入验证失败"));
        }

        return agent.executeStream(context);
    }

    /**
     * 执行标准软件开发工作流
     * 流程：需求分析 -> 领域建模 -> 架构设计 -> 代码生成 -> 代码审查 -> 测试生成
     */
    public WorkflowInstance startStandardWorkflow(AgentContext initialContext) {
        String workflowId = UUID.randomUUID().toString();
        WorkflowInstance instance = new WorkflowInstance(workflowId, "standard", initialContext);

        // 定义标准工作流步骤
        instance.addStep(new WorkflowStep("RequirementAnalysisAgent", "需求分析"));
        instance.addStep(new WorkflowStep("DomainModelingAgent", "领域建模"));
        instance.addStep(new WorkflowStep("ArchitectureDesignAgent", "架构设计"));
        instance.addStep(new WorkflowStep("CodeGenerationAgent", "代码生成"));
        instance.addStep(new WorkflowStep("CodeReviewAgent", "代码审查"));
        instance.addStep(new WorkflowStep("TestGenerationAgent", "测试生成"));

        workflowInstances.put(workflowId, instance);

        // 异步执行工作流
        CompletableFuture.runAsync(() -> executeWorkflow(instance));

        return instance;
    }

    /**
     * 执行工作流
     */
    private void executeWorkflow(WorkflowInstance instance) {
        List<WorkflowStep> steps = instance.getSteps();
        AgentContext context = instance.getContext();

        for (int i = 0; i < steps.size(); i++) {
            WorkflowStep step = steps.get(i);
            instance.setCurrentStepIndex(i);
            instance.setStatus(WorkflowStatus.RUNNING);

            log.info("工作流[{}] 执行步骤 {}/{}: {}",
                    instance.getId(), i + 1, steps.size(), step.getName());

            try {
                AgentResult result = executeAgent(step.getAgentName(), context);

                if (result.isSuccess()) {
                    step.setStatus(StepStatus.COMPLETED);
                    step.setResult(result);
                    log.info("步骤完成: {}", step.getName());
                } else {
                    step.setStatus(StepStatus.FAILED);
                    step.setErrorMessage(result.getErrorMessage());
                    instance.setStatus(WorkflowStatus.FAILED);
                    log.error("步骤失败: {} - {}", step.getName(), result.getErrorMessage());
                    return;
                }

            } catch (Exception e) {
                step.setStatus(StepStatus.FAILED);
                step.setErrorMessage(e.getMessage());
                instance.setStatus(WorkflowStatus.FAILED);
                log.error("步骤执行异常: {}", step.getName(), e);
                return;
            }
        }

        instance.setStatus(WorkflowStatus.COMPLETED);
        log.info("工作流[{}] 执行完成", instance.getId());
    }

    /**
     * 获取工作流实例
     */
    public WorkflowInstance getWorkflowInstance(String workflowId) {
        return workflowInstances.get(workflowId);
    }

    /**
     * 工作流实例
     */
    public static class WorkflowInstance {
        private final String id;
        private final String type;
        private final AgentContext context;
        private final List<WorkflowStep> steps = new ArrayList<>();
        private volatile WorkflowStatus status = WorkflowStatus.PENDING;
        private volatile int currentStepIndex = -1;

        public WorkflowInstance(String id, String type, AgentContext context) {
            this.id = id;
            this.type = type;
            this.context = context;
        }

        public void addStep(WorkflowStep step) {
            steps.add(step);
        }

        // Getters
        public String getId() { return id; }
        public String getType() { return type; }
        public AgentContext getContext() { return context; }
        public List<WorkflowStep> getSteps() { return steps; }
        public WorkflowStatus getStatus() { return status; }
        public void setStatus(WorkflowStatus status) { this.status = status; }
        public int getCurrentStepIndex() { return currentStepIndex; }
        public void setCurrentStepIndex(int index) { this.currentStepIndex = index; }
    }

    /**
     * 工作流步骤
     */
    public static class WorkflowStep {
        private final String agentName;
        private final String name;
        private volatile StepStatus status = StepStatus.PENDING;
        private AgentResult result;
        private String errorMessage;

        public WorkflowStep(String agentName, String name) {
            this.agentName = agentName;
            this.name = name;
        }

        // Getters and Setters
        public String getAgentName() { return agentName; }
        public String getName() { return name; }
        public StepStatus getStatus() { return status; }
        public void setStatus(StepStatus status) { this.status = status; }
        public AgentResult getResult() { return result; }
        public void setResult(AgentResult result) { this.result = result; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }

    /**
     * 工作流状态
     */
    public enum WorkflowStatus {
        PENDING, RUNNING, COMPLETED, FAILED, PAUSED
    }

    /**
     * 步骤状态
     */
    public enum StepStatus {
        PENDING, RUNNING, COMPLETED, FAILED, SKIPPED
    }
}
