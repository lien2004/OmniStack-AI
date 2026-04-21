package com.linglong.llm.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * 向量存储配置类
 * 使用自定义 ZhipuEmbeddingClient 直接调用智谱AI v4 接口
 * 绕过 Spring AI 0.8.x 硬编码 /v1/embeddings 路径的限制
 */
@Configuration
public class VectorStoreConfig {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreConfig.class);

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
     * 安全 JdbcTemplate：拦截 CREATE EXTENSION 语句，
     * 若扩展在系统中不可用（如 uuid-ossp），静默跳过而非抛出异常。
     * 我们的表使用 gen_random_uuid()（PG内置），无需 uuid-ossp。
     */
    /**
     * 安全 JdbcTemplate：
     * 1. 跳过系统未安装的 uuid-ossp 扩展创建
     * 2. 将所有 SQL 中的 uuid_generate_v4() 替换为 gen_random_uuid()（PG内置，无需扩展）
     */
    private JdbcTemplate safeJdbcTemplate() {
        DriverManagerDataSource pgDataSource = new DriverManagerDataSource();
        pgDataSource.setDriverClassName("org.postgresql.Driver");
        pgDataSource.setUrl(pgUrl);
        pgDataSource.setUsername(pgUsername);
        pgDataSource.setPassword(pgPassword);
        return new JdbcTemplate(pgDataSource) {
            @Override
            public void execute(String sql) throws DataAccessException {
                if (sql == null) {
                    super.execute((String) null);
                    return;
                }
                // 1. 替换 uuid_generate_v4() 为 PG 内置函数，避免依赖 uuid-ossp
                String fixedSql = sql
                        .replace("uuid_generate_v4 ()", "gen_random_uuid()")
                        .replace("uuid_generate_v4()", "gen_random_uuid()");
                if (!fixedSql.equals(sql)) {
                    log.info("[VectorStore] uuid_generate_v4() 已替换为 gen_random_uuid()");
                }
                // 2. 如果是 CREATE EXTENSION 语句，失败时静默跳过
                if (fixedSql.toUpperCase().contains("CREATE EXTENSION")) {
                    try {
                        super.execute(fixedSql);
                    } catch (DataAccessException e) {
                        log.warn("[VectorStore] 扩展创建跳过（系统未安装）: {} | 原因: {}", fixedSql.trim(), e.getMessage());
                    }
                } else {
                    super.execute(fixedSql);
                }
            }
        };
    }

    /**
     * 创建 PgVectorStore Bean
     * 使用 safeJdbcTemplate 屏蔽 Spring AI 硬编码的 uuid-ossp 扩展创建失败
     * 我们的 vector_store 表用 gen_random_uuid()，不依赖 uuid-ossp
     */
    @Bean
    public VectorStore vectorStore(EmbeddingClient embeddingClient) {
        return new PgVectorStore(
                safeJdbcTemplate(),
                embeddingClient,
                dimensions,
                PgVectorStore.PgDistanceType.COSINE_DISTANCE,
                initializeSchema,
                PgVectorStore.PgIndexType.NONE
        );
    }
}
