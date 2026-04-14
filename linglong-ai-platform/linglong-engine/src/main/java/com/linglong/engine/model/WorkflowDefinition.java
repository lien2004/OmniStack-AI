package com.linglong.engine.model;

import java.util.List;
import java.util.Map;

/**
 * 工作流定义
 */
public class WorkflowDefinition {

    /**
     * 工作流ID
     */
    private String id;

    /**
     * 工作流名称
     */
    private String name;

    /**
     * 工作流描述
     */
    private String description;

    /**
     * 步骤列表
     */
    private List<Step> steps;

    /**
     * 全局配置
     */
    private Map<String, Object> config;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Step> getSteps() {
        return steps;
    }

    public void setSteps(List<Step> steps) {
        this.steps = steps;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }

    public static class Step {
        /**
         * 步骤ID
         */
        private String id;

        /**
         * 步骤名称
         */
        private String name;

        /**
         * Agent类型
         */
        private String agentType;

        /**
         * 步骤描述
         */
        private String description;

        /**
         * 输入映射
         */
        private Map<String, String> inputMapping;

        /**
         * 输出映射
         */
        private Map<String, String> outputMapping;

        /**
         * 下一步骤ID
         */
        private String nextStepId;

        /**
         * 条件分支
         */
        private List<ConditionBranch> branches;

        /**
         * 超时时间(毫秒)
         */
        private Long timeout;

        /**
         * 重试次数
         */
        private Integer retryCount;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAgentType() {
            return agentType;
        }

        public void setAgentType(String agentType) {
            this.agentType = agentType;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Map<String, String> getInputMapping() {
            return inputMapping;
        }

        public void setInputMapping(Map<String, String> inputMapping) {
            this.inputMapping = inputMapping;
        }

        public Map<String, String> getOutputMapping() {
            return outputMapping;
        }

        public void setOutputMapping(Map<String, String> outputMapping) {
            this.outputMapping = outputMapping;
        }

        public String getNextStepId() {
            return nextStepId;
        }

        public void setNextStepId(String nextStepId) {
            this.nextStepId = nextStepId;
        }

        public List<ConditionBranch> getBranches() {
            return branches;
        }

        public void setBranches(List<ConditionBranch> branches) {
            this.branches = branches;
        }

        public Long getTimeout() {
            return timeout;
        }

        public void setTimeout(Long timeout) {
            this.timeout = timeout;
        }

        public Integer getRetryCount() {
            return retryCount;
        }

        public void setRetryCount(Integer retryCount) {
            this.retryCount = retryCount;
        }
    }

    public static class ConditionBranch {
        /**
         * 条件表达式
         */
        private String condition;

        /**
         * 目标步骤ID
         */
        private String targetStepId;

        public String getCondition() {
            return condition;
        }

        public void setCondition(String condition) {
            this.condition = condition;
        }

        public String getTargetStepId() {
            return targetStepId;
        }

        public void setTargetStepId(String targetStepId) {
            this.targetStepId = targetStepId;
        }
    }
}
