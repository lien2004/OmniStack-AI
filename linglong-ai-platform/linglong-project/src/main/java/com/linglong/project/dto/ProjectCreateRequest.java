package com.linglong.project.dto;

/**
 * 项目创建请求
 */
public class ProjectCreateRequest {

    /**
     * 项目名称
     */
    private String name;

    /**
     * 项目描述
     */
    private String description;

    /**
     * 需求文档内容
     */
    private String requirementDoc;

    /**
     * 技术栈选择
     */
    private String techStack;

    /**
     * 目标架构类型
     */
    private String architectureType;

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

    public String getRequirementDoc() {
        return requirementDoc;
    }

    public void setRequirementDoc(String requirementDoc) {
        this.requirementDoc = requirementDoc;
    }

    public String getTechStack() {
        return techStack;
    }

    public void setTechStack(String techStack) {
        this.techStack = techStack;
    }

    public String getArchitectureType() {
        return architectureType;
    }

    public void setArchitectureType(String architectureType) {
        this.architectureType = architectureType;
    }
}
