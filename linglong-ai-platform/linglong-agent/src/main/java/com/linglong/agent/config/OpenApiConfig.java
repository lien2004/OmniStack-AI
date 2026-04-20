package com.linglong.agent.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

/**
 * Agent 模块 OpenAPI/Swagger 配置
 * 访问地址：http://localhost:8085/swagger-ui.html
 */
@Configuration("agentOpenApiConfig")
public class OpenApiConfig {

    @Bean
    @Primary
    public OpenAPI agentOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("灵龙AI — Agent 服务 API")
                        .description("提供智能 Agent 执行（同步/流式/文件上传）、工作流编排、Agent 配置与统计管理等能力")
                        .version("1.0.0-SNAPSHOT")
                        .contact(new Contact()
                                .name("灵龙AI团队")
                                .email("support@linglong.ai"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("http://localhost:8085").description("Agent 服务（本地）")
                ));
    }
}
