package com.linglong.mcp.model;

import java.util.HashMap;
import java.util.Map;

/**
 * 工具执行请求
 */
public class ToolExecutionRequest {

    /**
     * 工具名称
     */
    private String toolName;

    /**
     * 执行参数
     */
    private Map<String, Object> parameters = new HashMap<>();

    /**
     * 请求ID
     */
    private String requestId;

    /**
     * 超时时间(毫秒)
     */
    private Long timeout;

    /**
     * 执行上下文
     */
    private Map<String, Object> context = new HashMap<>();

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Long getTimeout() {
        return timeout;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }

    /**
     * 获取参数值
     */
    @SuppressWarnings("unchecked")
    public <T> T getParameter(String key) {
        return (T) parameters.get(key);
    }

    /**
     * 添加参数
     */
    public void addParameter(String key, Object value) {
        this.parameters.put(key, value);
    }
}
