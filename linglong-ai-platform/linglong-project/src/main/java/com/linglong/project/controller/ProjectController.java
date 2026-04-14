package com.linglong.project.controller;

import com.linglong.common.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 项目管理控制器
 */
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private static final Logger log = LoggerFactory.getLogger(ProjectController.class);

    // 模拟项目存储
    private final Map<String, Project> projects = new HashMap<>();

    /**
     * 创建项目
     */
    @PostMapping
    public Result<Project> createProject(@RequestBody CreateProjectRequest request) {
        String projectId = UUID.randomUUID().toString();

        Project project = new Project();
        project.setId(projectId);
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setTechStack(request.getTechStack());
        project.setArchitectureType(request.getArchitectureType());
        project.setStatus("init");
        project.setCreateTime(new Date());
        project.setUpdateTime(new Date());

        projects.put(projectId, project);

        log.info("项目创建成功: {}", projectId);
        return Result.success(project);
    }

    /**
     * 获取项目列表
     */
    @GetMapping
    public Result<List<Project>> getProjects(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        List<Project> list = projects.values().stream()
                .filter(p -> status == null || status.equals(p.getStatus()))
                .sorted(Comparator.comparing(Project::getCreateTime).reversed())
                .toList();

        return Result.success(list);
    }

    /**
     * 获取项目详情
     */
    @GetMapping("/{projectId}")
    public Result<Project> getProject(@PathVariable String projectId) {
        Project project = projects.get(projectId);
        if (project == null) {
            return Result.error("项目不存在: " + projectId);
        }
        return Result.success(project);
    }

    /**
     * 更新项目
     */
    @PutMapping("/{projectId}")
    public Result<Project> updateProject(
            @PathVariable String projectId,
            @RequestBody UpdateProjectRequest request) {

        Project project = projects.get(projectId);
        if (project == null) {
            return Result.error("项目不存在: " + projectId);
        }

        if (request.getName() != null) {
            project.setName(request.getName());
        }
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            project.setStatus(request.getStatus());
        }
        project.setUpdateTime(new Date());

        return Result.success(project);
    }

    /**
     * 删除项目
     */
    @DeleteMapping("/{projectId}")
    public Result<Void> deleteProject(@PathVariable String projectId) {
        projects.remove(projectId);
        log.info("项目已删除: {}", projectId);
        return Result.success();
    }

    /**
     * 项目实体
     */
    public static class Project {
        private String id;
        private String name;
        private String description;
        private String status;
        private String techStack;
        private String architectureType;
        private Date createTime;
        private Date updateTime;

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getTechStack() { return techStack; }
        public void setTechStack(String techStack) { this.techStack = techStack; }
        public String getArchitectureType() { return architectureType; }
        public void setArchitectureType(String architectureType) { this.architectureType = architectureType; }
        public Date getCreateTime() { return createTime; }
        public void setCreateTime(Date createTime) { this.createTime = createTime; }
        public Date getUpdateTime() { return updateTime; }
        public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
    }

    /**
     * 创建项目请求
     */
    public static class CreateProjectRequest {
        private String name;
        private String description;
        private String techStack;
        private String architectureType;

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getTechStack() { return techStack; }
        public void setTechStack(String techStack) { this.techStack = techStack; }
        public String getArchitectureType() { return architectureType; }
        public void setArchitectureType(String architectureType) { this.architectureType = architectureType; }
    }

    /**
     * 更新项目请求
     */
    public static class UpdateProjectRequest {
        private String name;
        private String description;
        private String status;

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}

