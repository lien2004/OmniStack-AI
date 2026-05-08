package com.linglong.mcp.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 外部MCP工具配置实体
 * 用于持久化用户添加的外部工具配置
 */
@Entity
@Table(name = "external_tool_config")
public class ExternalToolConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 工具名称（唯一标识）
     */
    @Column(name = "tool_name", nullable = false, unique = true, length = 100)
    private String toolName;

    /**
     * 工具显示名称
     */
    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    /**
     * 工具描述
     */
    @Column(name = "description", length = 1000)
    private String description;

    /**
     * 工具分类
     */
    @Column(name = "category", nullable = false, length = 50)
    private String category;

    /**
     * 工具来源：external-外部集成, local-本地内置
     */
    @Column(name = "source", nullable = false, length = 20)
    private String source = "external";

    /**
     * 外部API URL
     */
    @Column(name = "api_url", length = 2000)
    private String apiUrl;

    /**
     * HTTP请求方法：GET, POST, PUT, DELETE
     */
    @Column(name = "http_method", length = 10)
    private String httpMethod = "GET";

    /**
     * 请求头配置（JSON格式）
     */
    @Column(name = "headers", length = 2000)
    private String headers;

    /**
     * 参数定义（JSON格式，与ToolDefinition.ParameterDefinition一致）
     */
    @Column(name = "parameters", length = 4000)
    private String parameters;

    /**
     * 响应提取表达式（JSONPath或字段映射）
     */
    @Column(name = "response_extractor", length = 1000)
    private String responseExtractor;

    /**
     * 是否启用
     */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    /**
     * 创建者ID
     */
    @Column(name = "created_by")
    private Long createdBy;

    /**
     * 图标或颜色标识
     */
    @Column(name = "icon", length = 100)
    private String icon;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getHeaders() {
        return headers;
    }

    public void setHeaders(String headers) {
        this.headers = headers;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public String getResponseExtractor() {
        return responseExtractor;
    }

    public void setResponseExtractor(String responseExtractor) {
        this.responseExtractor = responseExtractor;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
