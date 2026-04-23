package com.linglong.agent.controller;

import com.linglong.agent.model.AgentConfig;
import com.linglong.agent.model.AgentResult;
import com.linglong.agent.service.AgentConfigService;
import com.linglong.agent.service.AgentService;
import com.linglong.agent.service.AgentStatsService;
import com.linglong.agent.service.FileStorageService;
import com.linglong.agent.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Agent控制器
 * 提供 Agent 相关的 REST API
 */
@Tag(name = "Agent 服务", description = "智能 Agent 执行（同步/流式/文件上传）、工作流管理、Agent 配置与统计")
@RestController("agentModuleController")
@RequestMapping("/api/agent")
public class AgentController {

    private static final Logger log = LoggerFactory.getLogger(AgentController.class);

    private final AgentService agentService;
    private final AgentStatsService statsService;
    private final AgentConfigService configService;
    private final FileStorageService fileStorageService;

    public AgentController(AgentService agentService, AgentStatsService statsService,
                          AgentConfigService configService, FileStorageService fileStorageService) {
        this.agentService = agentService;
        this.statsService = statsService;
        this.configService = configService;
        this.fileStorageService = fileStorageService;
    }

    /**
     * 获取所有Agent列表
     */
    @Operation(summary = "获取 Agent 列表")
    @GetMapping("/list")
    public Result<List<AgentService.AgentInfo>> listAgents() {
        return Result.success(agentService.getAllAgents());
    }

    /**
     * 获取所有Agent详细信息（包含统计和配置）
     */
    @Operation(summary = "获取 Agent 详细列表（含统计和配置）")
    @GetMapping("/list/detail")
    public Result<List<AgentService.AgentDetailInfo>> listAgentDetails() {
        return Result.success(agentService.getAllAgentDetails());
    }

    /**
     * 获取单个Agent详情
     */
    @Operation(summary = "获取单个 Agent 详情")
    @GetMapping("/{agentName}")
    public Result<AgentService.AgentInfo> getAgent(@PathVariable String agentName) {
        AgentService.AgentInfo info = agentService.getAgentInfo(agentName);
        if (info == null) {
            return Result.error("Agent不存在: " + agentName);
        }
        return Result.success(info);
    }

    /**
     * 执行单个Agent（同步）
     */
    @Operation(summary = "同步执行 Agent", description = "同步调用指定 Agent，等待执行完成后返回结果")
    @PostMapping("/{agentName}/execute")
    public Result<AgentResult> executeAgent(
            @PathVariable String agentName,
            @RequestBody Map<String, Object> request) {
        
        log.info("收到Agent执行请求: {}", agentName);
        
        String taskId = getString(request, "taskId", UUID.randomUUID().toString());
        String projectId = getString(request, "projectId", null);
        String userId = getString(request, "userId", null);
        Object input = request.get("input");
        String outputPath = getString(request, "outputPath", null);
        Boolean autoSave = getBoolean(request, "autoSave", false);

        if (input == null) {
            return Result.error("input参数不能为空");
        }

        try {
            AgentResult result = agentService.executeAgent(agentName, taskId, projectId, userId, input, 
                    outputPath, autoSave, null);
            
            // 如果启用了自动保存，将结果保存到文件
            if (autoSave && result.isSuccess()) {
                String savedPath = fileStorageService.saveResultToFile(result, outputPath, agentName, taskId);
                if (savedPath != null) {
                    result.setData(Map.of("savedPath", savedPath));
                }
            }
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("Agent执行失败: {}", agentName, e);
            return Result.error("Agent执行失败: " + e.getMessage());
        }
    }

    /**
     * 执行单个Agent（带文件上传）
     */
    @Operation(summary = "带文件上传执行 Agent", description = "文件内容会自动附加到 input 中一并提交给 Agent")
    @PostMapping(value = "/{agentName}/execute-with-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<AgentResult> executeAgentWithFiles(
            @PathVariable String agentName,
            @RequestParam("input") String input,
            @RequestParam(value = "taskId", required = false) String taskId,
            @RequestParam(value = "projectId", required = false) String projectId,
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam(value = "outputPath", required = false) String outputPath,
            @RequestParam(value = "autoSave", required = false, defaultValue = "false") Boolean autoSave,
            @RequestParam(value = "files", required = false) MultipartFile[] files) {
        
        log.info("收到Agent执行请求(带文件上传): {}, 文件数: {}", agentName, files != null ? files.length : 0);
        
        if (taskId == null || taskId.isEmpty()) {
            taskId = UUID.randomUUID().toString();
        }

        try {
            // 保存上传的文件
            var uploadedFiles = fileStorageService.saveUploadedFiles(files, taskId);
            
            // 构建文件内容描述
            StringBuilder fileContext = new StringBuilder();
            if (uploadedFiles != null && !uploadedFiles.isEmpty()) {
                fileContext.append("\n\n--- 上传的文件 ---\n");
                for (var file : uploadedFiles) {
                    fileContext.append("文件: ").append(file.getFileName()).append("\n");
                    // 尝试读取文本文件内容
                    String content = fileStorageService.readFileContent(file);
                    log.info("文件 {} 读取内容长度: {}", file.getFileName(), content != null ? content.length() : 0);
                    if (content != null && !content.isEmpty()) {
                        if (content.startsWith("[文件读取失败:")) {
                            log.warn("文件读取失败: {}", content);
                            fileContext.append("内容:\n```\n[无法读取此文件内容]\n```\n\n");
                        } else {
                            fileContext.append("内容:\n```\n").append(content).append("\n```\n\n");
                        }
                    } else {
                        fileContext.append("内容:\n```\n[文件内容为空]\n```\n\n");
                    }
                }
            }
            
            // 合并输入内容和文件内容
            String combinedInput = input + fileContext.toString();
            log.info("合并后的输入内容长度: {} 字符, 原始输入长度: {} 字符, 文件内容长度: {} 字符", 
                combinedInput.length(), input.length(), fileContext.length());
            // 打印前500字符用于调试
            if (combinedInput.length() > 0) {
                log.debug("合并后的输入内容前500字符: {}", combinedInput.substring(0, Math.min(500, combinedInput.length())));
            }
            
            AgentResult result = agentService.executeAgent(agentName, taskId, projectId, userId, combinedInput, 
                    outputPath, autoSave, uploadedFiles);
            
            // 如果启用了自动保存，将结果保存到文件
            if (autoSave && result.isSuccess()) {
                String savedPath = fileStorageService.saveResultToFile(result, outputPath, agentName, taskId);
                if (savedPath != null) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("savedPath", savedPath);
                    data.put("uploadedFiles", uploadedFiles);
                    result.setData(data);
                }
            }
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("Agent执行失败: {}", agentName, e);
            return Result.error("Agent执行失败: " + e.getMessage());
        }
    }

    /**
     * 流式执行Agent（SSE）
     */
    @Operation(summary = "SSE 流式执行 Agent", description = "服务端推送事件流，实时输出 Agent 执行结果")
    @PostMapping(value = "/{agentName}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> executeAgentStream(
            @PathVariable String agentName,
            @RequestBody Map<String, Object> request) {
        
        log.info("收到Agent流式执行请求: {}", agentName);
        
        String taskId = getString(request, "taskId", UUID.randomUUID().toString());
        String projectId = getString(request, "projectId", null);
        String userId = getString(request, "userId", null);
        Object input = request.get("input");

        if (input == null) {
            return Flux.just("data: {\"error\": \"input参数不能为空\"}\n\n");
        }

        try {
            return agentService.executeAgentStream(agentName, taskId, projectId, userId, input)
                    .map(chunk -> "data: " + escapeJson(chunk) + "\n\n")
                    .concatWith(Flux.just("data: [DONE]\n\n"))
                    .onErrorResume(e -> {
                        log.error("Agent流式执行失败: {}", agentName, e);
                        return Flux.just("data: {\"error\": \"" + escapeJson(e.getMessage()) + "\"}\n\n");
                    });
        } catch (Exception e) {
            log.error("Agent流式执行失败: {}", agentName, e);
            return Flux.just("data: {\"error\": \"" + escapeJson(e.getMessage()) + "\"}\n\n");
        }
    }

    /**
     * 启动标准工作流
     */
    @Operation(summary = "启动标准工作流")
    @PostMapping("/workflow/start")
    public Result<AgentService.WorkflowInfo> startWorkflow(@RequestBody Map<String, Object> request) {
        log.info("收到工作流启动请求");
        
        String projectId = getString(request, "projectId", null);
        String userId = getString(request, "userId", null);
        Object input = request.get("input");

        if (input == null) {
            return Result.error("input参数不能为空");
        }

        try {
            AgentService.WorkflowInfo info = agentService.startStandardWorkflow(projectId, userId, input);
            return Result.success(info);
        } catch (Exception e) {
            log.error("启动工作流失败", e);
            return Result.error("启动工作流失败: " + e.getMessage());
        }
    }

    /**
     * 获取工作流状态
     */
    @Operation(summary = "获取工作流状态")
    @GetMapping("/workflow/{workflowId}")
    public Result<AgentService.WorkflowInfo> getWorkflowStatus(@PathVariable String workflowId) {
        AgentService.WorkflowInfo info = agentService.getWorkflowStatus(workflowId);
        if (info == null) {
            return Result.error("工作流不存在: " + workflowId);
        }
        return Result.success(info);
    }

    // ===== Agent统计和配置管理 API =====

    /**
     * 获取Agent统计信息
     */
    @Operation(summary = "获取 Agent 统计信息")
    @GetMapping("/{agentName}/stats")
    public Result<Map<String, Object>> getAgentStats(@PathVariable String agentName) {
        Map<String, Object> stats = statsService.getAgentStats(agentName);
        return Result.success(stats);
    }

    /**
     * 启用Agent
     */
    @Operation(summary = "启用 Agent")
    @PostMapping("/{agentName}/enable")
    public Result<Void> enableAgent(@PathVariable String agentName) {
        statsService.setAgentEnabled(agentName, true);
        return Result.success();
    }

    /**
     * 禁用Agent
     */
    @Operation(summary = "禁用 Agent")
    @PostMapping("/{agentName}/disable")
    public Result<Void> disableAgent(@PathVariable String agentName) {
        statsService.setAgentEnabled(agentName, false);
        return Result.success();
    }

    /**
     * 获取Agent配置
     */
    @Operation(summary = "获取 Agent 配置")
    @GetMapping("/{agentName}/config")
    public Result<AgentConfig> getAgentConfig(@PathVariable String agentName) {
        AgentConfig config = configService.getConfig(agentName);
        if (config == null) {
            // 返回默认配置
            config = new AgentConfig();
            config.setAgentName(agentName);
        }
        return Result.success(config);
    }

    /**
     * 保存Agent配置
     */
    @Operation(summary = "保存 Agent 配置")
    @PostMapping("/{agentName}/config")
    public Result<Void> saveAgentConfig(@PathVariable String agentName, @RequestBody AgentConfig config) {
        config.setAgentName(agentName);
        configService.saveConfig(config);
        return Result.success();
    }

    /**
     * 更新Agent配置
     */
    @Operation(summary = "更新 Agent 配置（部分更新）")
    @PutMapping("/{agentName}/config")
    public Result<Void> updateAgentConfig(@PathVariable String agentName, @RequestBody Map<String, Object> updates) {
        configService.updateConfig(agentName, updates);
        return Result.success();
    }

    // ===== 辅助方法 =====

    private String getString(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value.toString();
    }

    private Boolean getBoolean(Map<String, Object> map, String key, Boolean defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString());
    }

    private String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
