package com.linglong.agent.service;

import com.linglong.agent.model.Project;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 项目服务层
 * 处理项目的业务逻辑，包括CRUD、与工作流的联动
 */
@Service
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    private final ProjectRepository projectRepository;
    private final ObjectMapper objectMapper;

    public ProjectService(ProjectRepository projectRepository, ObjectMapper objectMapper) {
        this.projectRepository = projectRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 创建项目
     */
    public Project createProject(String name, String description, String techStack,
                                  String architectureType, String model, String requirement,
                                  String userId) {
        Project project = Project.builder()
                .id(UUID.randomUUID().toString())
                .name(name)
                .description(description)
                .status("init")
                .techStack(techStack)
                .architectureType(architectureType)
                .model(model)
                .requirement(requirement)
                .userId(userId)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        return projectRepository.save(project);
    }

    /**
     * 从工作流创建项目（CodeFlow完成后自动保存）
     */
    public Project createFromWorkflow(String workflowId, String name, String requirement,
                                       String model, String userId,
                                       List<Map<String, String>> codeFiles,
                                       Map<String, Object> workflowOutput) {
        // 检查是否已存在关联该工作流的项目
        Optional<Project> existing = projectRepository.findByWorkflowId(workflowId);
        if (existing.isPresent()) {
            // 更新已有项目
            Project project = existing.get();
            project.setStatus("completed");
            project.setCodeFiles(toJson(codeFiles));
            project.setWorkflowOutput(toJson(workflowOutput));
            project.setUpdateTime(LocalDateTime.now());
            return projectRepository.save(project);
        }

        // 从需求描述中提取项目名称（如果未提供）
        if (name == null || name.isBlank()) {
            name = extractProjectName(requirement);
        }

        Project project = Project.builder()
                .id(UUID.randomUUID().toString())
                .name(name)
                .description(requirement != null && requirement.length() > 200
                        ? requirement.substring(0, 200) + "..." : requirement)
                .status("completed")
                .model(model)
                .requirement(requirement)
                .workflowId(workflowId)
                .userId(userId)
                .codeFiles(toJson(codeFiles))
                .workflowOutput(toJson(workflowOutput))
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        return projectRepository.save(project);
    }

    /**
     * 绑定工作流到项目（在项目中心新建项目后启动AI开发时使用）
     */
    public Project bindWorkflow(String projectId, String workflowId) {
        Optional<Project> opt = projectRepository.findById(projectId);
        if (opt.isEmpty()) {
            throw new RuntimeException("项目不存在: " + projectId);
        }
        Project project = opt.get();
        project.setWorkflowId(workflowId);
        project.setStatus("analyzing");
        project.setUpdateTime(LocalDateTime.now());
        return projectRepository.save(project);
    }

    /**
     * 更新项目状态
     */
    public void updateStatus(String projectId, String status) {
        projectRepository.updateStatus(projectId, status);
    }

    /**
     * 更新项目代码文件
     */
    public void updateCodeFiles(String projectId, List<Map<String, String>> codeFiles) {
        projectRepository.updateCodeFiles(projectId, toJson(codeFiles));
    }

    /**
     * 获取项目详情
     */
    public Optional<Project> getProject(String id) {
        return projectRepository.findById(id);
    }

    /**
     * 获取项目列表
     */
    public List<Project> listProjects(String userId, String status, String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            return projectRepository.search(keyword);
        }
        if (status != null && !status.isBlank()) {
            return projectRepository.findByStatus(status);
        }
        if (userId != null && !userId.isBlank()) {
            return projectRepository.findByUserId(userId);
        }
        return projectRepository.findAll();
    }

    /**
     * 删除项目
     */
    public void deleteProject(String id) {
        projectRepository.deleteById(id);
    }

    /**
     * 更新项目信息
     */
    public Project updateProject(String id, Map<String, Object> updates) {
        Optional<Project> opt = projectRepository.findById(id);
        if (opt.isEmpty()) {
            throw new RuntimeException("项目不存在: " + id);
        }
        Project project = opt.get();
        
        if (updates.containsKey("name")) {
            project.setName((String) updates.get("name"));
        }
        if (updates.containsKey("description")) {
            project.setDescription((String) updates.get("description"));
        }
        if (updates.containsKey("status")) {
            project.setStatus((String) updates.get("status"));
        }
        if (updates.containsKey("techStack")) {
            project.setTechStack((String) updates.get("techStack"));
        }
        if (updates.containsKey("architectureType")) {
            project.setArchitectureType((String) updates.get("architectureType"));
        }
        if (updates.containsKey("model")) {
            project.setModel((String) updates.get("model"));
        }
        if (updates.containsKey("requirement")) {
            project.setRequirement((String) updates.get("requirement"));
        }
        
        project.setUpdateTime(LocalDateTime.now());
        return projectRepository.save(project);
    }

    /**
     * 统计项目数量
     */
    public long countProjects() {
        return projectRepository.count();
    }

    // ── 工具方法 ──────────────────────────────────────────────────────────────

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("JSON序列化失败", e);
            return null;
        }
    }

    /**
     * 从需求描述中提取项目名称
     */
    private String extractProjectName(String requirement) {
        if (requirement == null || requirement.isBlank()) {
            return "未命名项目";
        }
        // 取第一句话作为名称（最多30字）
        String firstLine = requirement.split("[。！？\\n]")[0].trim();
        if (firstLine.length() > 30) {
            firstLine = firstLine.substring(0, 30) + "...";
        }
        return firstLine.isEmpty() ? "AI生成项目" : firstLine;
    }
}
