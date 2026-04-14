package com.linglong.mcp.config;

import com.linglong.mcp.annotation.MCPParam;
import com.linglong.mcp.annotation.MCPTool;
import com.linglong.mcp.model.ToolDefinition;
import com.linglong.mcp.model.ToolExecutionRequest;
import com.linglong.mcp.model.ToolExecutionResult;
import com.linglong.mcp.registry.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP配置类
 * 自动扫描并注册带有@MCPTool注解的工具方法
 * 使用SmartLifecycle确保在所有Bean创建完成后才扫描
 */
@Configuration
public class MCPConfig implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(MCPConfig.class);

    private final ApplicationContext applicationContext;
    private final ToolRegistry toolRegistry;
    private boolean running = false;

    public MCPConfig(ApplicationContext applicationContext, ToolRegistry toolRegistry) {
        this.applicationContext = applicationContext;
        this.toolRegistry = toolRegistry;
    }

    @Override
    public void start() {
        log.info("开始扫描MCP工具...");
        scanAndRegisterTools();
        this.running = true;
    }

    @Override
    public void stop() {
        this.running = false;
    }

    @Override
    public boolean isRunning() {
        return this.running;
    }

    @Override
    public int getPhase() {
        // 在较后的阶段执行，确保所有Bean都已创建
        return Integer.MAX_VALUE - 100;
    }

    /**
     * 扫描并注册所有带有@MCPTool注解的工具
     */
    private void scanAndRegisterTools() {
        // 获取所有Spring Bean
        String[] beanNames = applicationContext.getBeanDefinitionNames();

        for (String beanName : beanNames) {
            try {
                Object bean = applicationContext.getBean(beanName);
                Class<?> clazz = bean.getClass();

                // 遍历所有方法
                for (Method method : clazz.getDeclaredMethods()) {
                    MCPTool annotation = method.getAnnotation(MCPTool.class);
                    if (annotation != null) {
                        registerTool(bean, method, annotation);
                    }
                }
            } catch (Exception e) {
                log.warn("扫描Bean失败: {}", beanName, e);
            }
        }

        log.info("MCP工具扫描完成，共注册 {} 个工具", toolRegistry.getAllToolDefinitions().size());
    }

    /**
     * 注册单个工具
     */
    private void registerTool(Object bean, Method method, MCPTool annotation) {
        String toolName = annotation.name();

        // 创建工具定义
        ToolDefinition definition = new ToolDefinition();
        definition.setName(toolName);
        definition.setDescription(annotation.description());
        definition.setCategory(annotation.category());
        definition.setAsync(annotation.async());
        definition.setReturnType(method.getReturnType().getSimpleName());

        // 元数据：groupName = 工具类名，用于前端分组展示
        Map<String, Object> meta = new HashMap<>();
        meta.put("groupName", bean.getClass().getSimpleName());
        definition.setMetadata(meta);

        // 解析参数 - 优先从@MCPParam注解获取
        List<ToolDefinition.ParameterDefinition> parameters = new ArrayList<>();

        // 检查方法上的@MCPParam注解
        MCPParam[] paramAnnotations = method.getAnnotationsByType(MCPParam.class);
        if (paramAnnotations.length > 0) {
            for (MCPParam paramAnn : paramAnnotations) {
                ToolDefinition.ParameterDefinition paramDef = new ToolDefinition.ParameterDefinition();
                paramDef.setName(paramAnn.name());
                paramDef.setDescription(paramAnn.description());
                paramDef.setType(paramAnn.type());
                paramDef.setRequired(paramAnn.required());
                if (!paramAnn.defaultValue().isEmpty()) {
                    paramDef.setDefaultValue(paramAnn.defaultValue());
                }
                if (!paramAnn.enumValues().isEmpty()) {
                    paramDef.setEnumValues(Arrays.asList(paramAnn.enumValues().split(",")));
                }
                parameters.add(paramDef);
            }
        } else {
            // 兼容旧逻辑：从方法参数中解析
            for (Parameter param : method.getParameters()) {
                // 跳过ToolExecutionRequest类型的参数
                if (param.getType() == ToolExecutionRequest.class) {
                    continue;
                }

                ToolDefinition.ParameterDefinition paramDef = new ToolDefinition.ParameterDefinition();
                paramDef.setName(param.getName());
                paramDef.setType(param.getType().getSimpleName());
                paramDef.setRequired(true);
                parameters.add(paramDef);
            }
        }
        definition.setParameters(parameters);

        // 创建工具处理器
        ToolRegistry.ToolHandler handler = request -> {
            try {
                // 准备参数
                Object[] args = new Object[method.getParameterCount()];
                int argIndex = 0;

                for (Parameter param : method.getParameters()) {
                    if (param.getType() == ToolExecutionRequest.class) {
                        args[argIndex++] = request;
                    } else {
                        // 从请求参数中获取值
                        Object value = request.getParameters().get(param.getName());
                        args[argIndex++] = value;
                    }
                }

                // 调用方法
                Object result = method.invoke(bean, args);

                // 如果返回类型已经是ToolExecutionResult，直接返回
                if (result instanceof ToolExecutionResult) {
                    return (ToolExecutionResult) result;
                }

                // 否则包装成ToolExecutionResult
                return ToolExecutionResult.success("执行成功", result);

            } catch (Exception e) {
                log.error("工具执行失败: {}", toolName, e);
                return ToolExecutionResult.error("工具执行失败: " + e.getCause().getMessage());
            }
        };

        // 注册工具
        toolRegistry.register(definition, handler);
        log.info("工具已注册: {} - {}", toolName, annotation.description());
    }
}
