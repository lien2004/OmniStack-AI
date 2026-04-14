package com.linglong.engine.core;

import com.linglong.agent.core.Agent;
import com.linglong.agent.model.AgentContext;
import com.linglong.agent.model.AgentResult;
import com.linglong.engine.model.WorkflowDefinition;
import com.linglong.engine.model.WorkflowInstance;
import reactor.core.publisher.Flux;

/**
 * 工作流引擎接口
 */
public interface WorkflowEngine {

    /**
     * 注册工作流定义
     *
     * @param definition 工作流定义
     */
    void registerWorkflow(WorkflowDefinition definition);

    /**
     * 启动工作流实例
     *
     * @param workflowId 工作流ID
     * @param context    初始上下文
     * @return 工作流实例
     */
    WorkflowInstance startWorkflow(String workflowId, AgentContext context);

    /**
     * 执行工作流步骤
     *
     * @param instanceId 实例ID
     * @param agent      当前步骤Agent
     * @return 执行结果
     */
    AgentResult executeStep(String instanceId, Agent agent);

    /**
     * 流式执行工作流步骤
     *
     * @param instanceId 实例ID
     * @param agent      当前步骤Agent
     * @return 流式执行结果
     */
    Flux<String> executeStepStream(String instanceId, Agent agent);

    /**
     * 获取工作流实例状态
     *
     * @param instanceId 实例ID
     * @return 工作流实例
     */
    WorkflowInstance getInstance(String instanceId);

    /**
     * 暂停工作流
     *
     * @param instanceId 实例ID
     */
    void pauseWorkflow(String instanceId);

    /**
     * 恢复工作流
     *
     * @param instanceId 实例ID
     */
    void resumeWorkflow(String instanceId);

    /**
     * 停止工作流
     *
     * @param instanceId 实例ID
     */
    void stopWorkflow(String instanceId);
}
