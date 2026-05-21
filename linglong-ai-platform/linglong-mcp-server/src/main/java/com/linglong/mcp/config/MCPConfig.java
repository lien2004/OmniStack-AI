package com.linglong.mcp.config;

import com.linglong.mcp.annotation.MCPParam;
import com.linglong.mcp.annotation.MCPTool;
import com.linglong.mcp.model.ToolDefinition;
import com.linglong.mcp.model.ToolExecutionRequest;
import com.linglong.mcp.model.ToolExecutionResult;
import com.linglong.mcp.registry.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
     * 支持 CGLIB 代理、JDK 动态代理等多种代理场景
     */
    private void scanAndRegisterTools() {
        // 获取所有Spring Bean
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        int totalRegistered = 0;

        for (String beanName : beanNames) {
            try {
                Object bean = applicationContext.getBean(beanName);
                // 多层代理解析：优先 AopUtils（处理AOP代理），回退到 ClassUtils（处理CGLIB）
                Class<?> clazz = resolveTargetClass(bean);

                // 收集所有方法，去重
                Set<String> processedMethods = new HashSet<>();
                // 使用 ReflectionUtils 遍历所有方法（包括继承链），确保代理场景下不遗漏
                ReflectionUtils.doWithMethods(clazz, method -> {
                    try {
                        MCPTool annotation = method.getAnnotation(MCPTool.class);
                        if (annotation != null && processedMethods.add(annotation.name())) {
                            ReflectionUtils.makeAccessible(method);
                            registerTool(bean, method, annotation);
                            log.info("成功注册工具: {} (来自 {}#{})", annotation.name(), clazz.getSimpleName(), method.getName());
                        }
                    } catch (Exception e) {
                        log.error("注册工具方法失败: {}#{} - {}", clazz.getSimpleName(), method.getName(), e.getMessage(), e);
                    }
                }, method -> method.isAnnotationPresent(MCPTool.class));

                totalRegistered += processedMethods.size();
            } catch (Exception e) {
                // 只在debug级别记录非工具Bean的失败，避免日志噪音
                if (log.isDebugEnabled()) {
                    log.debug("扫描Bean失败: {}", beanName, e);
                }
            }
        }

        log.info("MCP工具扫描完成，共注册 {} 个工具", totalRegistered);
        // 列出所有已注册的工具
        toolRegistry.getAllToolDefinitions().forEach(def ->
            log.info("  已注册: {} [{}] - {}", def.getName(), def.getCategory(), def.getDescription())
        );
    }

    /**
     * 解析Bean的真实目标类（穿透所有代理层）
     * 优先使用 AopUtils 处理 AOP 代理，回退到 ClassUtils 处理 CGLIB
     */
    private Class<?> resolveTargetClass(Object bean) {
        try {
            // AopUtils.getTargetClass 能正确识别 JDK/CGLIB 代理并返回目标类
            if (AopUtils.isAopProxy(bean)) {
                return AopUtils.getTargetClass(bean);
            }
        } catch (Exception e) {
            log.debug("AopUtils解析失败，回退到ClassUtils: {}", e.getMessage());
        }
        // 通用回退：ClassUtils.getUserClass 处理 CGLIB 代理类名中的 $$ 标识
        return ClassUtils.getUserClass(bean.getClass());
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

        // 元数据：groupName = 工具类名（使用真实类名，跳过代理类），用于前端分组展示
        Map<String, Object> meta = new HashMap<>();
        meta.put("groupName", ClassUtils.getUserClass(bean.getClass()).getSimpleName());
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

        // 创建工具处理器（捕获 method 和 bean 引用，支持代理场景下的反射调用）
        ToolRegistry.ToolHandler handler = request -> {
            try {
                // 准备参数
                Object[] args = new Object[method.getParameterCount()];
                int argIndex = 0;

                for (Parameter param : method.getParameters()) {
                    if (param.getType() == ToolExecutionRequest.class) {
                        args[argIndex++] = request;
                    } else {
                        // 从请求参数中获取值（兼容 @MCPParam 注解定义的参数名）
                        String paramName = param.getName();
                        Object value = request.getParameters().get(paramName);
                        // 尝试从 @MCPParam 注解名匹配（防止编译时未启用 -parameters）
                        if (value == null && paramAnnotations.length > 0) {
                            int pIdx = argIndex - (method.getParameters()[0].getType() == ToolExecutionRequest.class ? 1 : 0);
                            if (pIdx >= 0 && pIdx < paramAnnotations.length) {
                                value = request.getParameters().get(paramAnnotations[pIdx].name());
                            }
                        }
                        args[argIndex++] = value;
                    }
                }

                // 通过反射调用方法（对代理对象同样有效，CGLIB代理会走拦截链）
                Object result = method.invoke(bean, args);

                // 如果返回类型已经是ToolExecutionResult，直接返回
                if (result instanceof ToolExecutionResult) {
                    return (ToolExecutionResult) result;
                }

                // 否则包装成ToolExecutionResult
                return ToolExecutionResult.success("执行成功", result);

            } catch (Exception e) {
                log.error("工具执行失败: {}", toolName, e);
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                return ToolExecutionResult.error("工具执行失败: " + cause.getMessage());
            }
        };

        // 注册工具
        toolRegistry.register(definition, handler);
        log.info("工具已注册: {} - {}", toolName, annotation.description());
    }
}
