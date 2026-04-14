-- 插入默认平台设置
INSERT INTO platform_settings (setting_key, setting_value, setting_type, description) VALUES
('platform.name', '灵龙AI智能平台', 'string', '平台名称'),
('platform.notification', 'true', 'boolean', '系统通知开关'),
('platform.autoSave', 'true', 'boolean', '自动保存开关'),
('security.jwt.enabled', 'false', 'boolean', 'JWT认证开关'),
('security.jwt.expireHours', '24', 'integer', 'Token过期时间(小时)'),
('security.operationLog.enabled', 'false', 'boolean', '操作日志开关')
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value);
