package com.linglong.engine.agent;

import java.util.HashMap;
import java.util.Map;

public class AgentResult {
    private boolean success;
    private Object data;
    private String output;
    private String errorMessage;
    private Long executionTime;
    private Map<String, Object> metadata = new HashMap<>();

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Long getExecutionTime() { return executionTime; }
    public void setExecutionTime(Long executionTime) { this.executionTime = executionTime; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public static AgentResult success(String output) {
        AgentResult r = new AgentResult();
        r.setSuccess(true);
        r.setOutput(output);
        return r;
    }

    public static AgentResult error(String errorMessage) {
        AgentResult r = new AgentResult();
        r.setSuccess(false);
        r.setErrorMessage(errorMessage);
        return r;
    }
}
