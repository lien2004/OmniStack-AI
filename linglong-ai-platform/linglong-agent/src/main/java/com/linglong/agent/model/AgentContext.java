package com.linglong.agent.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent执行上下文
 */
public class AgentContext {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 项目ID
     */
    private String projectId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 输入数据
     */
    private Object input;

    /**
     * 历史消息/上下文
     */
    private Map<String, Object> history = new HashMap<>();

    /**
     * 共享上下文(跨Agent共享)
     */
    private Map<String, Object> sharedContext = new HashMap<>();

    /**
     * 配置参数
     */
    private Map<String, Object> config = new HashMap<>();

    /**
     * 上传的文件列表
     */
    private List<UploadedFile> uploadedFiles;

    /**
     * 输出保存路径
     */
    private String outputPath;

    /**
     * 是否自动保存结果到文件
     */
    private boolean autoSaveOutput = false;

    /**
     * 上传的文件信息
     */
    public static class UploadedFile {
        private String fileName;
        private String fileType;
        private String filePath;
        private byte[] content;
        private long fileSize;

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getFileType() {
            return fileType;
        }

        public void setFileType(String fileType) {
            this.fileType = fileType;
        }

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }

        public byte[] getContent() {
            return content;
        }

        public void setContent(byte[] content) {
            this.content = content;
        }

        public long getFileSize() {
            return fileSize;
        }

        public void setFileSize(long fileSize) {
            this.fileSize = fileSize;
        }
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Object getInput() {
        return input;
    }

    public void setInput(Object input) {
        this.input = input;
    }

    public Map<String, Object> getHistory() {
        return history;
    }

    public void setHistory(Map<String, Object> history) {
        this.history = history;
    }

    public Map<String, Object> getSharedContext() {
        return sharedContext;
    }

    public void setSharedContext(Map<String, Object> sharedContext) {
        this.sharedContext = sharedContext;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }

    public List<UploadedFile> getUploadedFiles() {
        return uploadedFiles;
    }

    public void setUploadedFiles(List<UploadedFile> uploadedFiles) {
        this.uploadedFiles = uploadedFiles;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public boolean isAutoSaveOutput() {
        return autoSaveOutput;
    }

    public void setAutoSaveOutput(boolean autoSaveOutput) {
        this.autoSaveOutput = autoSaveOutput;
    }

    /**
     * 获取上下文中的值
     */
    @SuppressWarnings("unchecked")
    public <T> T getFromHistory(String key) {
        return (T) history.get(key);
    }

    /**
     * 获取共享上下文中的值
     */
    @SuppressWarnings("unchecked")
    public <T> T getFromShared(String key) {
        return (T) sharedContext.get(key);
    }

    /**
     * 添加到历史
     */
    public void addToHistory(String key, Object value) {
        this.history.put(key, value);
    }

    /**
     * 添加到共享上下文
     */
    public void addToShared(String key, Object value) {
        this.sharedContext.put(key, value);
    }
}
