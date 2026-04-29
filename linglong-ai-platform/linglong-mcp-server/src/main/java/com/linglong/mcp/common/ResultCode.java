package com.linglong.mcp.common;

/**
 * 响应状态码枚举
 */
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    ERROR(500, "操作失败"),

    // 系统级别错误码
    SYSTEM_ERROR(1000, "系统错误"),
    PARAM_ERROR(1001, "参数错误"),
    UNAUTHORIZED(1002, "未授权"),
    FORBIDDEN(1003, "禁止访问"),
    NOT_FOUND(1004, "资源不存在"),

    // 业务级别错误码 - AI相关
    AI_GENERATE_ERROR(2000, "AI生成失败"),
    AI_STREAM_ERROR(2001, "AI流式输出失败"),
    LLM_NOT_AVAILABLE(2002, "LLM服务不可用"),

    // 业务级别错误码 - Agent相关
    AGENT_EXECUTE_ERROR(3000, "Agent执行失败"),
    AGENT_TIMEOUT(3001, "Agent执行超时"),
    AGENT_NOT_FOUND(3002, "Agent不存在"),

    // 业务级别错误码 - MCP相关
    MCP_TOOL_ERROR(4000, "MCP工具调用失败"),
    MCP_SERVER_ERROR(4001, "MCP服务错误"),

    // 业务级别错误码 - 项目相关
    PROJECT_NOT_FOUND(5000, "项目不存在"),
    PROJECT_CREATE_ERROR(5001, "项目创建失败"),

    // 业务级别错误码 - 代码生成
    CODE_GENERATE_ERROR(6000, "代码生成失败"),
    CODE_COMPILE_ERROR(6001, "代码编译失败"),
    CODE_TEST_ERROR(6002, "代码测试失败");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
