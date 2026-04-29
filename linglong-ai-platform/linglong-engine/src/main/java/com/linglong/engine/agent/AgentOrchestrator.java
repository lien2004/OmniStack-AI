package com.linglong.engine.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AgentOrchestrator {
    private static final Logger log = LoggerFactory.getLogger(AgentOrchestrator.class);
    private final Map<String, Agent> agents = new ConcurrentHashMap<>();
    private final Map<String, WorkflowInstance> workflowInstances = new ConcurrentHashMap<>();
    public void registerAgent(Agent agent) { agents.put(agent.getName(), agent); }
    public Agent getAgent(String name) { return agents.get(name); }
    public Collection<Agent> getAllAgents() { return agents.values(); }
    public AgentResult executeAgent(String name, AgentContext ctx) {
        Agent a = agents.get(name);
        if (a == null) return AgentResult.error("Agent not found: " + name);
        if (!a.validate(ctx)) return AgentResult.error("Validation failed");
        return a.execute(ctx);
    }
    public Flux<String> executeAgentStream(String name, AgentContext ctx) {
        Agent a = agents.get(name);
        if (a == null) return Flux.error(new RuntimeException("Agent not found: " + name));
        return a.executeStream(ctx);
    }
    public WorkflowInstance startStandardWorkflow(AgentContext c) {
        String id = UUID.randomUUID().toString();
        WorkflowInstance inst = new WorkflowInstance(id, "standard", c);
        inst.addStep(new WorkflowStep("RequirementAnalysisAgent", "需求分析"));
        inst.addStep(new WorkflowStep("DomainModelingAgent", "领域建模"));
        inst.addStep(new WorkflowStep("ArchitectureDesignAgent", "架构设计"));
        inst.addStep(new WorkflowStep("CodeGenerationAgent", "代码生成"));
        inst.addStep(new WorkflowStep("CodeReviewAgent", "代码审查"));
        inst.addStep(new WorkflowStep("TestGenerationAgent", "测试生成"));
        workflowInstances.put(id, inst);
        CompletableFuture.runAsync(() -> execWf(inst));
        return inst;
    }
    private void execWf(WorkflowInstance inst) {
        for (int i = 0; i < inst.getSteps().size(); i++) {
            WorkflowStep s = inst.getSteps().get(i);
            inst.setCurrentStepIndex(i);
            inst.setStatus(WorkflowStatus.RUNNING);
            try {
                AgentResult r = executeAgent(s.getAgentName(), inst.getContext());
                if (r.isSuccess()) { s.setStatus(StepStatus.COMPLETED); s.setResult(r); }
                else { s.setStatus(StepStatus.FAILED); s.setErrorMessage(r.getErrorMessage()); inst.setStatus(WorkflowStatus.FAILED); return; }
            } catch (Exception e) { s.setStatus(StepStatus.FAILED); s.setErrorMessage(e.getMessage()); inst.setStatus(WorkflowStatus.FAILED); return; }
        }
        inst.setStatus(WorkflowStatus.COMPLETED);
    }
    public WorkflowInstance getWorkflowInstance(String id) { return workflowInstances.get(id); }
    public static class WorkflowInstance {
        private final String id; private final String type; private final AgentContext context;
        private final List<WorkflowStep> steps = new ArrayList<>();
        private volatile WorkflowStatus status = WorkflowStatus.PENDING;
        private volatile int currentStepIndex = -1;
        public WorkflowInstance(String id, String type, AgentContext c) { this.id = id; this.type = type; this.context = c; }
        public void addStep(WorkflowStep s) { steps.add(s); }
        public String getId() { return id; } public String getType() { return type; }
        public AgentContext getContext() { return context; } public List<WorkflowStep> getSteps() { return steps; }
        public WorkflowStatus getStatus() { return status; } public void setStatus(WorkflowStatus s) { this.status = s; }
        public int getCurrentStepIndex() { return currentStepIndex; } public void setCurrentStepIndex(int i) { this.currentStepIndex = i; }
    }
    public static class WorkflowStep {
        private final String agentName; private final String name;
        private volatile StepStatus status = StepStatus.PENDING;
        private AgentResult result; private String errorMessage;
        public WorkflowStep(String a, String n) { this.agentName = a; this.name = n; }
        public String getAgentName() { return agentName; } public String getName() { return name; }
        public StepStatus getStatus() { return status; } public void setStatus(StepStatus s) { this.status = s; }
        public AgentResult getResult() { return result; } public void setResult(AgentResult r) { this.result = r; }
        public String getErrorMessage() { return errorMessage; } public void setErrorMessage(String e) { this.errorMessage = e; }
    }
    public enum WorkflowStatus { PENDING, RUNNING, COMPLETED, FAILED, PAUSED }
    public enum StepStatus { PENDING, RUNNING, COMPLETED, FAILED, SKIPPED }
}
