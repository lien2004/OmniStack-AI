package com.linglong.llm.config;

import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * 向量存储配置类
 * 使用自定义 ZhipuEmbeddingClient 直接调用智谱AI v4 接口
 * 绕过 Spring AI 0.8.x 硬编码 /v1/embeddings 路径的限制
 */
@Configuration
public class VectorStoreConfig {

    @Value("${vector.store.dimensions:1024}")
    private int dimensions;

    @Value("${vector.store.initialize-schema:false}")
    private boolean initializeSchema;

    @Value("${zhipu.api-key}")
    private String zhipuApiKey;

    @Value("${zhipu.embedding.model:embedding-3}")
    private String embeddingModel;

    /**
     * 智谱AI Embedding 客户端
     * 直接 POST https://open.bigmodel.cn/api/paas/v4/embeddings
     */
    @Bean
    @Primary
    public EmbeddingClient embeddingClient() {
        return new ZhipuEmbeddingClient(zhipuApiKey, embeddingModel);
    }

    @Value("${spring.datasource.url}")
    private String pgUrl;

    @Value("${spring.datasource.username}")
    private String pgUsername;

    @Value("${spring.datasource.password}")
    private String pgPassword;

    /**
     * 暴露 PostgreSQL JdbcTemplate Bean，供知识库查询等使用
     */
    @Bean("pgJdbcTemplate")
    public JdbcTemplate pgJdbcTemplate() {
        DriverManagerDataSource pgDataSource = new DriverManagerDataSource();
        pgDataSource.setDriverClassName("org.postgresql.Driver");
        pgDataSource.setUrl(pgUrl);
        pgDataSource.setUsername(pgUsername);
        pgDataSource.setPassword(pgPassword);
        return new JdbcTemplate(pgDataSource);
    }

    /**
     * 创建 PgVectorStore Bean
     * 显式使用 PostgreSQL 数据源，避免多数据源环境下注入错误
     */
    @Bean
    public VectorStore vectorStore(EmbeddingClient embeddingClient) {
        // 注意：vector_store 是视图，指向 linglong 物理表
        // 索引已在 linglong 表上创建（见 init-pgvector.sql）
        // 因此 initializeSchema=false 且 indexType=NONE，避免在视图上创建索引
        return new PgVectorStore(
                pgJdbcTemplate(),
                embeddingClient,
                dimensions,
                PgVectorStore.PgDistanceType.COSINE_DISTANCE,
                initializeSchema,
                PgVectorStore.PgIndexType.NONE
        );
    }
}
