package com.linglong.project.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI配置
 */
@Configuration("projectOpenApiConfig")
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("玲珑AI平台 API")
                        .description("AI驱动的软件开发平台 - 提供Agent协作、代码生成、MCP工具管理等功能")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("玲珑AI团队")
                                .email("support@linglong.ai"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("http://8.137.117.129:8080").description("项目服务"),
                        new Server().url("http://8.137.117.129:8088").description("网关服务")
                ));
    }
}
