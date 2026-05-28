package com.linglong.llm.config;

import com.linglong.llm.service.KnowledgeMigrationService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class KnowledgeBaseSchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseSchemaInitializer.class);

    @Autowired
    @Qualifier("chatJdbcTemplate")
    private JdbcTemplate mysqlJdbc;

    @Autowired
    private KnowledgeMigrationService migrationService;

    @PostConstruct
    public void init() {
        try {
            mysqlJdbc.execute("""
                    CREATE TABLE IF NOT EXISTS knowledge_base (
                        id              VARCHAR(64)  NOT NULL PRIMARY KEY,
                        name            VARCHAR(200) NOT NULL,
                        description     VARCHAR(1000),
                        collection_name VARCHAR(200) NOT NULL,
                        embedding_model VARCHAR(100) NOT NULL DEFAULT 'embedding-3',
                        dimensions      INT          NOT NULL DEFAULT 1024,
                        owner           VARCHAR(100),
                        created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        UNIQUE KEY uk_collection (collection_name)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                      COMMENT='知识库'
                    """);
            log.info("knowledge_base 表初始化成功");

            mysqlJdbc.execute("""
                    CREATE TABLE IF NOT EXISTS knowledge_document (
                        id            VARCHAR(64)  NOT NULL PRIMARY KEY,
                        kb_id         VARCHAR(64)  NOT NULL,
                        name          VARCHAR(500) NOT NULL,
                        filename      VARCHAR(500),
                        category      VARCHAR(100),
                        mime_type     VARCHAR(200),
                        size_bytes    BIGINT       DEFAULT 0,
                        chunk_count   INT          DEFAULT 0,
                        status        VARCHAR(32)  NOT NULL DEFAULT 'ready',
                        enabled       TINYINT(1)   NOT NULL DEFAULT 1,
                        error_message VARCHAR(1000),
                        uploader      VARCHAR(100),
                        created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        INDEX idx_kb (kb_id),
                        INDEX idx_status (status)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                      COMMENT='知识库文档'
                    """);
            log.info("knowledge_document 表初始化成功");

            migrationService.ensureDefaultKnowledgeBase();
            migrationService.migrateLegacyChunks();
        } catch (Exception e) {
            log.warn("知识库表初始化警告: {}", e.getMessage());
        }
    }
}
