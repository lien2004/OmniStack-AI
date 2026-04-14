package com.linglong.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linglong.agent.model.AgentConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Agent配置服务
 * 管理Agent配置的存储和读取
 */
@Service
public class AgentConfigService {

    private static final Logger log = LoggerFactory.getLogger(AgentConfigService.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    // 配置过期时间(永久)
    private static final long CONFIG_EXPIRE_DAYS = 3650; // 10年

    public AgentConfigService(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 获取Agent配置
     *
     * @param agentName Agent名称
     * @return Agent配置，如果不存在返回null
     */
    public AgentConfig getConfig(String agentName) {
        try {
            String key = buildConfigKey(agentName);
            Object value = redisTemplate.opsForValue().get(key);

            if (value instanceof AgentConfig) {
                return (AgentConfig) value;
            }

            // 尝试反序列化
            if (value instanceof Map) {
                return objectMapper.convertValue(value, AgentConfig.class);
            }

            return null;
        } catch (Exception e) {
            log.error("获取Agent配置失败: {}", agentName, e);
            return null;
        }
    }

    /**
     * 保存Agent配置
     *
     * @param config Agent配置
     */
    public void saveConfig(AgentConfig config) {
        try {
            if (config == null || config.getAgentName() == null) {
                log.warn("Agent配置或名称不能为空");
                return;
            }

            String key = buildConfigKey(config.getAgentName());
            redisTemplate.opsForValue().set(key, config, CONFIG_EXPIRE_DAYS, TimeUnit.DAYS);
            log.info("Agent配置已保存: {}", config.getAgentName());
        } catch (Exception e) {
            log.error("保存Agent配置失败: {}", config != null ? config.getAgentName() : "null", e);
        }
    }

    /**
     * 更新Agent配置的部分字段
     *
     * @param agentName Agent名称
     * @param updates   更新的字段
     */
    public void updateConfig(String agentName, Map<String, Object> updates) {
        try {
            AgentConfig config = getConfig(agentName);
            if (config == null) {
                config = new AgentConfig();
                config.setAgentName(agentName);
            }

            // 应用更新
            if (updates.containsKey("displayName")) {
                config.setDisplayName((String) updates.get("displayName"));
            }
            if (updates.containsKey("description")) {
                config.setDescription((String) updates.get("description"));
            }
            if (updates.containsKey("systemPrompt")) {
                config.setSystemPrompt((String) updates.get("systemPrompt"));
            }
            if (updates.containsKey("parameters")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> params = (Map<String, Object>) updates.get("parameters");
                config.setParameters(params);
            }
            if (updates.containsKey("enabled")) {
                config.setEnabled(Boolean.parseBoolean(updates.get("enabled").toString()));
            }

            saveConfig(config);
        } catch (Exception e) {
            log.error("更新Agent配置失败: {}", agentName, e);
        }
    }

    /**
     * 删除Agent配置
     *
     * @param agentName Agent名称
     */
    public void deleteConfig(String agentName) {
        try {
            String key = buildConfigKey(agentName);
            redisTemplate.delete(key);
            log.info("Agent配置已删除: {}", agentName);
        } catch (Exception e) {
            log.error("删除Agent配置失败: {}", agentName, e);
        }
    }

    /**
     * 检查Agent配置是否存在
     *
     * @param agentName Agent名称
     * @return 是否存在
     */
    public boolean configExists(String agentName) {
        try {
            String key = buildConfigKey(agentName);
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("检查Agent配置存在性失败: {}", agentName, e);
            return false;
        }
    }

    // ===== 私有方法 =====

    private String buildConfigKey(String agentName) {
        return "linglong:agent:config:" + agentName;
    }
}

