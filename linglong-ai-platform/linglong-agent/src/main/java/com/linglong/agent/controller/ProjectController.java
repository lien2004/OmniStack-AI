package com.linglong.agent.controller;

import com.linglong.agent.common.Result;
import com.linglong.agent.model.Project;
import com.linglong.agent.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 项目中心控制器
 * 提供项目管理相关的 REST API
 */
@Tag(name = "项目中心", description = "项目CRUD、工作流关联、代码管理")
@RestController
@RequestMapping("/api/agent/projects")
public class ProjectController {

    private static final Logger log = LoggerFactory.getLogger(ProjectController.class);

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    /**
     * 创建项目
     */
    @Operation(summary = "创建项目")
    @PostMapping
    public Result<Project> createProject(@RequestBody Map<String, Object> request) {
        log.info("创建项目: {}", request.get("name"));

        String name = (String) request.get("name");
        String description = (String) request.get("description");
        String techStack = (String) request.get("techStack");
        String architectureType = (String) request.get("architectureType");
        String model = (String) request.get("model");
        String requirement = (String) request.get("requirement");
        String userId = (String) request.get("userId");

        if (name == null || name.isBlank()) {
            return Result.error("项目名称不能为空");
        }

        try {
            Project project = projectService.createProject(name, description, techStack,
                    architectureType, model, requirement, userId);
            return Result.success(project);
        } catch (Exception e) {
            log.error("创建项目失败", e);
            return Result.error("创建项目失败: " + e.getMessage());
        }
    }

    /**
     * 从工作流保存项目（CodeFlow完成后自动调用）
     */
    @Operation(summary = "从工作流保存项目")
    @PostMapping("/from-workflow")
    @SuppressWarnings("unchecked")
    public Result<Project> saveFromWorkflow(@RequestBody Map<String, Object> request) {
        log.info("从工作流保存项目, workflowId: {}", request.get("workflowId"));

        String workflowId = (String) request.get("workflowId");
        String name = (String) request.get("name");
        String requirement = (String) request.get("requirement");
        String model = (String) request.get("model");
        String userId = (String) request.get("userId");
        List<Map<String, String>> codeFiles = (List<Map<String, String>>) request.get("codeFiles");
        Map<String, Object> workflowOutput = (Map<String, Object>) request.get("workflowOutput");

        if (workflowId == null || workflowId.isBlank()) {
            return Result.error("workflowId不能为空");
        }

        try {
            Project project = projectService.createFromWorkflow(workflowId, name, requirement,
                    model, userId, codeFiles, workflowOutput);
            return Result.success(project);
        } catch (Exception e) {
            log.error("从工作流保存项目失败", e);
            return Result.error("保存项目失败: " + e.getMessage());
        }
    }

    /**
     * 绑定工作流到项目（项目中心启动AI开发时使用）
     */
    @Operation(summary = "绑定工作流到项目")
    @PostMapping("/{projectId}/bind-workflow")
    public Result<Project> bindWorkflow(@PathVariable String projectId,
                                         @RequestBody Map<String, Object> request) {
        String workflowId = (String) request.get("workflowId");
        if (workflowId == null || workflowId.isBlank()) {
            return Result.error("workflowId不能为空");
        }

        try {
            Project project = projectService.bindWorkflow(projectId, workflowId);
            return Result.success(project);
        } catch (Exception e) {
            log.error("绑定工作流失败", e);
            return Result.error("绑定工作流失败: " + e.getMessage());
        }
    }

    /**
     * 获取项目列表
     */
    @Operation(summary = "获取项目列表")
    @GetMapping
    public Result<List<Project>> listProjects(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        try {
            List<Project> projects = projectService.listProjects(userId, status, keyword);
            return Result.success(projects);
        } catch (Exception e) {
            log.error("获取项目列表失败", e);
            return Result.error("获取项目列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取项目详情
     */
    @Operation(summary = "获取项目详情")
    @GetMapping("/{id}")
    public Result<Project> getProject(@PathVariable String id) {
        Optional<Project> project = projectService.getProject(id);
        if (project.isEmpty()) {
            return Result.error("项目不存在: " + id);
        }
        return Result.success(project.get());
    }

    /**
     * 更新项目
     */
    @Operation(summary = "更新项目")
    @PutMapping("/{id}")
    public Result<Project> updateProject(@PathVariable String id,
                                          @RequestBody Map<String, Object> updates) {
        try {
            Project project = projectService.updateProject(id, updates);
            return Result.success(project);
        } catch (Exception e) {
            log.error("更新项目失败", e);
            return Result.error("更新项目失败: " + e.getMessage());
        }
    }

    /**
     * 更新项目状态
     */
    @Operation(summary = "更新项目状态")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable String id,
                                      @RequestBody Map<String, String> request) {
        String status = request.get("status");
        if (status == null || status.isBlank()) {
            return Result.error("status不能为空");
        }
        try {
            projectService.updateStatus(id, status);
            return Result.success();
        } catch (Exception e) {
            log.error("更新项目状态失败", e);
            return Result.error("更新状态失败: " + e.getMessage());
        }
    }

    /**
     * 删除项目
     */
    @Operation(summary = "删除项目")
    @DeleteMapping("/{id}")
    public Result<Void> deleteProject(@PathVariable String id) {
        try {
            projectService.deleteProject(id);
            return Result.success();
        } catch (Exception e) {
            log.error("删除项目失败", e);
            return Result.error("删除项目失败: " + e.getMessage());
        }
    }

    /**
     * 统计项目数量
     */
    @Operation(summary = "获取项目统计")
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        long count = projectService.countProjects();
        return Result.success(Map.of("total", count));
    }
}
