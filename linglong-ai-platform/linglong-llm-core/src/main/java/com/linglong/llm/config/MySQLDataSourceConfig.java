package com.linglong.llm.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

/**
 * MySQL 数据源配置 - 用于对话历史持久化
 * 独立于 PostgreSQL（向量存储），避免主数据源冲突
 */
@Configuration
@ConditionalOnProperty(name = "chat.datasource.url")  // 仅当明确配置了 MySQL 数据源时才激活
public class MySQLDataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(MySQLDataSourceConfig.class);

    @Value("${chat.datasource.url:jdbc:mysql://8.137.117.129:3306/linglong_chat?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false}")
    private String url;

    @Value("${chat.datasource.username:root}")
    private String username;

    @Value("${chat.datasource.password:123456}")
    private String password;

    @Value("${chat.datasource.driver-class-name:com.mysql.cj.jdbc.Driver}")
    private String driverClassName;

    /**
     * MySQL 数据源 Bean（命名为 chatDataSource，不影响主 PG 数据源）
     */
    @Bean(name = "chatDataSource")
    public DataSource chatDataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName(driverClassName);
        ds.setUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        return ds;
    }

    /**
     * 专用于对话历史的 JdbcTemplate，使用 MySQL 数据源
     */
    @Bean(name = "chatJdbcTemplate")
    public JdbcTemplate chatJdbcTemplate(@Qualifier("chatDataSource") DataSource ds) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(ds);
        initChatSchema(jdbcTemplate);
        return jdbcTemplate;
    }

    /**
     * 初始化对话历史表（幂等，仅首次创建）
     */
    private void initChatSchema(JdbcTemplate jdbcTemplate) {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS chat_message (
                        id           BIGINT AUTO_INCREMENT PRIMARY KEY,
                        chat_id      VARCHAR(64)   NOT NULL COMMENT '会话唯一ID（每次进入页面生成）',
                        role         VARCHAR(20)   NOT NULL COMMENT 'user / assistant',
                        content      LONGTEXT      NOT NULL COMMENT '消息正文',
                        model        VARCHAR(100)           COMMENT '使用的模型名称',
                        file_attachment VARCHAR(500)        COMMENT '附件文件名（文档对话时使用）',
                        created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        INDEX idx_chat_id (chat_id),
                        INDEX idx_created_at (created_at)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                      COMMENT='智能对话历史记录'
                    """);
            log.info("chat_message 表初始化成功（MySQL）");
        } catch (Exception e) {
            log.warn("chat_message 表初始化警告（可能已存在）: {}", e.getMessage());
        }
    }
}
