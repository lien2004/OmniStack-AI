package com.linglong.engine.agent;

import java.util.HashMap;
import java.util.Map;

public class AgentContext {
    private String taskId;
    private String projectId;
    private String userId;
    private Object input;
    private Map<String, Object> history = new HashMap<>();
    private Map<String, Object> sharedContext = new HashMap<>();
    private Map<String, Object> config = new HashMap<>();

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public Object getInput() { return input; }
    public void setInput(Object input) { this.input = input; }
    public Map<String, Object> getHistory() { return history; }
    public void setHistory(Map<String, Object> history) { this.history = history; }
    public Map<String, Object> getSharedContext() { return sharedContext; }
    public void setSharedContext(Map<String, Object> sharedContext) { this.sharedContext = sharedContext; }
    public Map<String, Object> getConfig() { return config; }
    public void setConfig(Map<String, Object> config) { this.config = config; }

    @SuppressWarnings("unchecked")
    public <T> T getFromShared(String key) { return (T) sharedContext.get(key); }
    public void addToShared(String key, Object value) { this.sharedContext.put(key, value); }
    @SuppressWarnings("unchecked")
    public <T> T getFromHistory(String key) { return (T) history.get(key); }
    public void addToHistory(String key, Object value) { this.history.put(key, value); }
}
