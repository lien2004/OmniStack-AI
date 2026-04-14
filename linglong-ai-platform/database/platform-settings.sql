-- 平台设置相关表

-- 平台基础设置表
CREATE TABLE IF NOT EXISTS platform_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value TEXT,
    setting_type VARCHAR(50),
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_setting_key (setting_key)
);

-- LLM厂商配置表
CREATE TABLE IF NOT EXISTS llm_providers (
    id VARCHAR(36) PRIMARY KEY,
    provider VARCHAR(50) NOT NULL,
    provider_name VARCHAR(100),
    model VARCHAR(100) NOT NULL,
    api_key VARCHAR(500) NOT NULL,
    base_url VARCHAR(500),
    temperature DOUBLE DEFAULT 0.7,
    max_tokens INT DEFAULT 4096,
    is_default BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_provider (provider),
    INDEX idx_is_default (is_default),
    INDEX idx_is_active (is_active)
);

-- 数据库配置表
CREATE TABLE IF NOT EXISTS database_configs (
    id VARCHAR(36) PRIMARY KEY,
    db_type VARCHAR(50) NOT NULL,
    host VARCHAR(255) NOT NULL,
    port INT NOT NULL,
    database_name VARCHAR(100) NOT NULL,
    username VARCHAR(100) NOT NULL,
    password VARCHAR(500) NOT NULL,
    status VARCHAR(50),
    connection_params TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_db_type (db_type)
);

-- 插入默认平台设置
INSERT INTO platform_settings (setting_key, setting_value, setting_type, description) VALUES
('platform.name', '玲珑AI平台', 'string', '平台名称'),
('platform.notification', 'true', 'boolean', '系统通知开关'),
('platform.autoSave', 'true', 'boolean', '自动保存开关'),
('security.jwt.enabled', 'false', 'boolean', 'JWT认证开关'),
('security.jwt.expireHours', '24', 'integer', 'Token过期时间(小时)'),
('security.operationLog.enabled', 'false', 'boolean', '操作日志开关')
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value);
