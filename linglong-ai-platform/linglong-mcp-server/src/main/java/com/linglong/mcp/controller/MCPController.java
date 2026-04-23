package com.linglong.mcp.controller;

import com.linglong.mcp.common.Result;
import com.linglong.mcp.model.ToolDefinition;
import com.linglong.mcp.model.ToolExecutionRequest;
import com.linglong.mcp.model.ToolExecutionResult;
import com.linglong.mcp.registry.ToolRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * MCP工具控制器
 */
@Tag(name = "MCP 工具", description = "MCP 工具注册、查询、按分类检索及工具执行")
@RestController
@RequestMapping("/api/mcp")
public class MCPController {

    private static final Logger log = LoggerFactory.getLogger(MCPController.class);

    private final ToolRegistry toolRegistry;

    public MCPController(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    /**
     * 获取所有工具定义
     */
    @Operation(summary = "获取所有工具列表")
    @GetMapping("/tools")
    public Result<List<ToolDefinition>> getAllTools() {
        return Result.success(toolRegistry.getAllToolDefinitions());
    }

    /**
     * 按分类获取工具
     */
    @Operation(summary = "按分类获取工具")
    @GetMapping("/tools/category/{category}")
    public Result<List<ToolDefinition>> getToolsByCategory(@Parameter(description = "工具分类，如 filesystem、database") @PathVariable String category) {
        return Result.success(toolRegistry.getToolDefinitionsByCategory(category));
    }

    /**
     * 获取单个工具定义
     */
    @Operation(summary = "获取单个工具定义")
    @GetMapping("/tools/{toolName}")
    public Result<ToolDefinition> getTool(@Parameter(description = "工具名称") @PathVariable String toolName) {
        ToolDefinition definition = toolRegistry.getToolDefinition(toolName);
        if (definition == null) {
            return Result.error("工具不存在: " + toolName);
        }
        return Result.success(definition);
    }

    /**
     * 执行工具
     */
    @Operation(summary = "执行工具", description = "按工具名称执行对应工具，请求体为工具所需参数")
    @PostMapping("/tools/{toolName}/execute")
    public Result<ToolExecutionResult> executeTool(
            @Parameter(description = "工具名称") @PathVariable String toolName,
            @RequestBody Map<String, Object> parameters) {

        log.info("执行工具: {}, 参数: {}", toolName, parameters);

        if (!toolRegistry.hasTool(toolName)) {
            return Result.error("工具不存在: " + toolName);
        }

        ToolExecutionRequest request = new ToolExecutionRequest();
        request.setToolName(toolName);
        request.setParameters(parameters);

        ToolExecutionResult result = toolRegistry.execute(request);

        // 始终返回完整的 ToolExecutionResult 对象，让前端统一处理
        return Result.success(result);
    }

    /**
     * 检查工具是否存在
     */
    @Operation(summary = "检查工具是否存在")
    @GetMapping("/tools/{toolName}/exists")
    public Result<Boolean> checkToolExists(@Parameter(description = "工具名称") @PathVariable String toolName) {
        return Result.success(toolRegistry.hasTool(toolName));
    }
}
