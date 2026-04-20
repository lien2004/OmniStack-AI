package com.linglong.mcp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * MCP Server 模块 OpenAPI/Swagger 配置
 * 访问地址：http://localhost:8083/swagger-ui.html
 */
@Configuration("mcpOpenApiConfig")
public class OpenApiConfig {

    @Bean
    @ConditionalOnMissingBean(OpenAPI.class)
    public OpenAPI mcpOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("灵龙AI — MCP 工具服务 API")
                        .description("提供 MCP 工具注册、查询、按分类检索及工具执行等能力")
                        .version("1.0.0-SNAPSHOT")
                        .contact(new Contact()
                                .name("灵龙AI团队")
                                .email("support@linglong.ai"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("http://localhost:8083").description("MCP Server（本地）")
                ));
    }
}
