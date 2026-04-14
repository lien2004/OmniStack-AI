package com.linglong.mcp.annotation;

import java.lang.annotation.*;

/**
 * MCP工具注解
 * 用于将Java方法暴露为MCP工具
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MCPTool {

    /**
     * 工具名称
     */
    String name();

    /**
     * 工具描述
     */
    String description();

    /**
     * 工具分类
     */
    String category() default "general";

    /**
     * 是否异步执行
     */
    boolean async() default false;
}
