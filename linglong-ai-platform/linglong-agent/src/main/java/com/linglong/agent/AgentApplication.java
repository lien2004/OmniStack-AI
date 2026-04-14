package com.linglong.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Agent 服务启动类
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.linglong.agent", "com.linglong.llm", "com.linglong.mcp"})
public class AgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentApplication.class, args);
    }
}
