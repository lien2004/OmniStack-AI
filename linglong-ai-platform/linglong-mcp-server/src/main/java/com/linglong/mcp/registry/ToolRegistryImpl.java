package com.linglong.mcp.registry;

import com.linglong.mcp.model.ToolDefinition;
import com.linglong.mcp.model.ToolExecutionRequest;
import com.linglong.mcp.model.ToolExecutionResult;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具注册中心实现
 */
@Component
public class ToolRegistryImpl implements ToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(ToolRegistryImpl.class);

    private final Map<String, ToolDefinition> toolDefinitions = new ConcurrentHashMap<>();
    private final Map<String, ToolHandler> toolHandlers = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("工具注册中心初始化完成");
    }

    @Override
    public void register(ToolDefinition definition, ToolHandler handler) {
        String toolName = definition.getName();
        toolDefinitions.put(toolName, definition);
        toolHandlers.put(toolName, handler);
        log.info("工具已注册: {}", toolName);
    }

    @Override
    public void unregister(String toolName) {
        toolDefinitions.remove(toolName);
        toolHandlers.remove(toolName);
        log.info("工具已注销: {}", toolName);
    }

    @Override
    public ToolDefinition getToolDefinition(String toolName) {
        return toolDefinitions.get(toolName);
    }

    @Override
    public List<ToolDefinition> getAllToolDefinitions() {
        return new ArrayList<>(toolDefinitions.values());
    }

    @Override
    public List<ToolDefinition> getToolDefinitionsByCategory(String category) {
        return toolDefinitions.values().stream()
                .filter(def -> category.equals(def.getCategory()))
                .toList();
    }

    @Override
    public ToolExecutionResult execute(ToolExecutionRequest request) {
        String toolName = request.getToolName();
        ToolHandler handler = toolHandlers.get(toolName);

        if (handler == null) {
            log.warn("工具不存在: {}", toolName);
            return ToolExecutionResult.error("工具不存在: " + toolName);
        }

        long startTime = System.currentTimeMillis();
        try {
            log.info("开始执行工具: {}", toolName);
            ToolExecutionResult result = handler.execute(request);
            long executionTime = System.currentTimeMillis() - startTime;
            result.setExecutionTime(executionTime);
            log.info("工具执行完成: {}, 耗时: {}ms", toolName, executionTime);
            return result;
        } catch (Exception e) {
            log.error("工具执行失败: {}", toolName, e);
            return ToolExecutionResult.error("工具执行失败: " + e.getMessage());
        }
    }

    @Override
    public boolean hasTool(String toolName) {
        return toolHandlers.containsKey(toolName);
    }
}
