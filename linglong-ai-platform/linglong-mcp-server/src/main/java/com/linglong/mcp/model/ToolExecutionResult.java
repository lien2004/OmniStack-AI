package com.linglong.mcp.model;

import java.util.HashMap;
import java.util.Map;

/**
 * 工具执行结果
 */
public class ToolExecutionResult {

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

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    /**
     * 创建成功结果
     */
    public static ToolExecutionResult success(String output) {
        ToolExecutionResult result = new ToolExecutionResult();
        result.setSuccess(true);
        result.setOutput(output);
        return result;
    }

    /**
     * 创建成功结果(带数据)
     */
    public static ToolExecutionResult success(String output, Object data) {
        ToolExecutionResult result = new ToolExecutionResult();
        result.setSuccess(true);
        result.setOutput(output);
        result.setData(data);
        return result;
    }

    /**
     * 创建成功结果(带元数据)
     */
    public static ToolExecutionResult success(String output, Map<String, Object> metadata) {
        ToolExecutionResult result = new ToolExecutionResult();
        result.setSuccess(true);
        result.setOutput(output);
        result.setMetadata(metadata);
        return result;
    }

    /**
     * 创建失败结果
     */
    public static ToolExecutionResult error(String errorMessage) {
        ToolExecutionResult result = new ToolExecutionResult();
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        return result;
    }
}
