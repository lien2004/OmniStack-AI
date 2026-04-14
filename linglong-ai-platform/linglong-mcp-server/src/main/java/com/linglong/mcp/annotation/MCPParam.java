package com.linglong.mcp.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * MCP工具参数注解
 * 用于定义工具方法的参数信息
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(MCPParams.class)
public @interface MCPParam {
    /**
     * 参数名
     */
    String name();

    /**
     * 参数描述
     */
    String description() default "";

    /**
     * 参数类型
     */
    String type() default "string";

    /**
     * 是否必填
     */
    boolean required() default true;

    /**
     * 默认值
     */
    String defaultValue() default "";

    /**
     * 枚举值（逗号分隔）
     */
    String enumValues() default "";
}
