package com.linglong.llm.config;

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
 * LLM Core 模块 OpenAPI/Swagger 配置
 * 访问地址：http://8.137.117.129:8084/swagger-ui.html
 */
@Configuration("llmCoreOpenApiConfig")
public class OpenApiConfig {

    @Bean
    @ConditionalOnMissingBean(OpenAPI.class)
    public OpenAPI llmCoreOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("灵龙AI — LLM Core 服务 API")
                        .description("提供 AI 对话（多轮/RAG）、文档对话、文档导出、向量知识库管理等核心 LLM 能力")
                        .version("1.0.0-SNAPSHOT")
                        .contact(new Contact()
                                .name("灵龙AI团队")
                                .email("support@linglong.ai"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("http://8.137.117.129:8084").description("LLM Core 服务")
                ));
    }
}
