package com.linglong.engine.model;

import com.linglong.engine.agent.AgentContext;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 工作流实例
 */
public class WorkflowInstance {

    /**
     * 实例ID
     */
    private String instanceId;

    /**
     * 工作流定义ID
     */
    private String workflowId;

    /**
     * 当前步骤ID
     */
    private String currentStepId;

    /**
     * 实例状态
     */
    private InstanceStatus status;

    /**
     * 执行上下文
     */
    private AgentContext context;

    /**
     * 步骤执行结果
     */
    private Map<String, Object> stepResults;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 完成时间
     */
    private LocalDateTime completeTime;

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public String getCurrentStepId() {
        return currentStepId;
    }

    public void setCurrentStepId(String currentStepId) {
        this.currentStepId = currentStepId;
    }

    public InstanceStatus getStatus() {
        return status;
    }

    public void setStatus(InstanceStatus status) {
        this.status = status;
    }

    public AgentContext getContext() {
        return context;
    }

    public void setContext(AgentContext context) {
        this.context = context;
    }

    public Map<String, Object> getStepResults() {
        return stepResults;
    }

    public void setStepResults(Map<String, Object> stepResults) {
        this.stepResults = stepResults;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public LocalDateTime getCompleteTime() {
        return completeTime;
    }

    public void setCompleteTime(LocalDateTime completeTime) {
        this.completeTime = completeTime;
    }

    /**
     * 实例状态枚举
     */
    public enum InstanceStatus {
        CREATED,      // 已创建
        RUNNING,      // 运行中
        PAUSED,       // 已暂停
        COMPLETED,    // 已完成
        FAILED,       // 已失败
        STOPPED       // 已停止
    }
}
