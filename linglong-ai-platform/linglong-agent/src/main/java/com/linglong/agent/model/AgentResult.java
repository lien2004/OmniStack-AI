package com.linglong.agent.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Agent执行结果
 */
public class AgentResult {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 结果数据
     */
    private Object data;

    /**
     * 输出内容(文本)
     */
    private String output;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 执行耗时(毫秒)
     */
    private Long executionTime;

    /**
     * Token消耗
     */
    private TokenUsage tokenUsage;

    /**
     * 额外元数据
     */
    private Map<String, Object> metadata = new HashMap<>();

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(Long executionTime) {
        this.executionTime = executionTime;
    }

    public TokenUsage getTokenUsage() {
        return tokenUsage;
    }

    public void setTokenUsage(TokenUsage tokenUsage) {
        this.tokenUsage = tokenUsage;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    /**
     * 创建成功结果
     */
    public static AgentResult success(String output) {
        AgentResult result = new AgentResult();
        result.setSuccess(true);
        result.setOutput(output);
        return result;
    }

    /**
     * 创建成功结果(带数据)
     */
    public static AgentResult success(String output, Object data) {
        AgentResult result = new AgentResult();
        result.setSuccess(true);
        result.setOutput(output);
        result.setData(data);
        return result;
    }

    /**
     * 创建失败结果
     */
    public static AgentResult error(String errorMessage) {
        AgentResult result = new AgentResult();
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        return result;
    }

    public static class TokenUsage {
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;

        public Integer getPromptTokens() {
            return promptTokens;
        }

        public void setPromptTokens(Integer promptTokens) {
            this.promptTokens = promptTokens;
        }

        public Integer getCompletionTokens() {
            return completionTokens;
        }

        public void setCompletionTokens(Integer completionTokens) {
            this.completionTokens = completionTokens;
        }

        public Integer getTotalTokens() {
            return totalTokens;
        }

        public void setTotalTokens(Integer totalTokens) {
            this.totalTokens = totalTokens;
        }
    }
}
