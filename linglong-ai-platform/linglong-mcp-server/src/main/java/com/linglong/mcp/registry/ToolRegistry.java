package com.linglong.mcp.registry;

import com.linglong.mcp.model.ToolDefinition;
import com.linglong.mcp.model.ToolExecutionRequest;
import com.linglong.mcp.model.ToolExecutionResult;

import java.util.List;

/**
 * 工具注册中心接口
 */
public interface ToolRegistry {

    /**
     * 注册工具
     *
     * @param definition 工具定义
     * @param handler    工具执行处理器
     */
    void register(ToolDefinition definition, ToolHandler handler);

    /**
     * 注销工具
     *
     * @param toolName 工具名称
     */
    void unregister(String toolName);

    /**
     * 获取工具定义
     *
     * @param toolName 工具名称
     * @return 工具定义
     */
    ToolDefinition getToolDefinition(String toolName);

    /**
     * 获取所有工具定义
     *
     * @return 工具定义列表
     */
    List<ToolDefinition> getAllToolDefinitions();

    /**
     * 按分类获取工具定义
     *
     * @param category 分类
     * @return 工具定义列表
     */
    List<ToolDefinition> getToolDefinitionsByCategory(String category);

    /**
     * 执行工具
     *
     * @param request 执行请求
     * @return 执行结果
     */
    ToolExecutionResult execute(ToolExecutionRequest request);

    /**
     * 检查工具是否存在
     *
     * @param toolName 工具名称
     * @return 是否存在
     */
    boolean hasTool(String toolName);

    /**
     * 工具执行处理器接口
     */
    @FunctionalInterface
    interface ToolHandler {
        ToolExecutionResult execute(ToolExecutionRequest request);
    }
}
