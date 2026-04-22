package com.lingnong.user.config;

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
@Configuration("userOpenApiConfig")
public class OpenApiConfig {

    @Bean("userCustomOpenAPI")
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("玲珑AI平台 - 用户模块 API")
                        .description("用户管理模块 - 提供用户注册、登录、信息管理等功能")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("玲珑AI团队")
                                .email("support@linglong.ai"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("http://localhost:8081").description("用户服务"),
                        new Server().url("http://localhost:8088").description("网关服务")
                ));
    }
}
