package com.linglong.agent.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Agent统计服务
 * 管理Agent使用次数、状态等统计信息
 */
@Service
public class AgentStatsService {

    private static final Logger log = LoggerFactory.getLogger(AgentStatsService.class);

    private final RedisTemplate<String, Object> redisTemplate;

    // 统计信息过期时间(30天)
    private static final long STATS_EXPIRE_DAYS = 30;

    public AgentStatsService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 增加Agent使用次数
     *
     * @param agentName Agent名称
     */
    public void incrementUsageCount(String agentName) {
        try {
            String key = buildUsageCountKey(agentName);
            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, STATS_EXPIRE_DAYS, TimeUnit.DAYS);
            log.debug("Agent使用次数已增加: {}", agentName);
        } catch (Exception e) {
            log.error("增加Agent使用次数失败: {}", agentName, e);
        }
    }

    /**
     * 获取Agent使用次数
     *
     * @param agentName Agent名称
     * @return 使用次数
     */
    public long getUsageCount(String agentName) {
        try {
            String key = buildUsageCountKey(agentName);
            Object value = redisTemplate.opsForValue().get(key);
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            return 0L;
        } catch (Exception e) {
            log.error("获取Agent使用次数失败: {}", agentName, e);
            return 0L;
        }
    }

    /**
     * 设置Agent状态
     *
     * @param agentName Agent名称
     * @param enabled   是否启用
     */
    public void setAgentEnabled(String agentName, boolean enabled) {
        try {
            String key = buildAgentStatusKey(agentName);
            redisTemplate.opsForValue().set(key, enabled ? "1" : "0", STATS_EXPIRE_DAYS, TimeUnit.DAYS);
            log.info("Agent状态已更新: {} -> {}", agentName, enabled ? "启用" : "禁用");
        } catch (Exception e) {
            log.error("设置Agent状态失败: {}", agentName, e);
        }
    }

    /**
     * 检查Agent是否启用
     *
     * @param agentName Agent名称
     * @return 是否启用(默认启用)
     */
    public boolean isAgentEnabled(String agentName) {
        try {
            String key = buildAgentStatusKey(agentName);
            Object value = redisTemplate.opsForValue().get(key);
            // 默认为启用状态
            if (value == null) {
                return true;
            }
            return "1".equals(value.toString());
        } catch (Exception e) {
            log.error("获取Agent状态失败: {}", agentName, e);
            return true; // 默认启用
        }
    }

    /**
     * 记录Agent执行时间
     *
     * @param agentName Agent名称
     */
    public void recordExecution(String agentName) {
        try {
            String key = buildLastExecutionKey(agentName);
            String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
            redisTemplate.opsForValue().set(key, timestamp, STATS_EXPIRE_DAYS, TimeUnit.DAYS);
        } catch (Exception e) {
            log.error("记录Agent执行时间失败: {}", agentName, e);
        }
    }

    /**
     * 获取Agent最后执行时间
     *
     * @param agentName Agent名称
     * @return 最后执行时间(ISO格式)
     */
    public String getLastExecutionTime(String agentName) {
        try {
            String key = buildLastExecutionKey(agentName);
            Object value = redisTemplate.opsForValue().get(key);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            log.error("获取Agent最后执行时间失败: {}", agentName, e);
            return null;
        }
    }

    /**
     * 获取Agent完整统计信息
     *
     * @param agentName Agent名称
     * @return 统计信息Map
     */
    public Map<String, Object> getAgentStats(String agentName) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("usageCount", getUsageCount(agentName));
        stats.put("enabled", isAgentEnabled(agentName));
        stats.put("lastExecutionTime", getLastExecutionTime(agentName));
        return stats;
    }

    /**
     * 获取所有Agent的统计信息
     *
     * @return 所有Agent统计信息
     */
    public Map<String, Map<String, Object>> getAllAgentStats() {
        Map<String, Map<String, Object>> allStats = new HashMap<>();
        // 通过扫描Redis键获取所有Agent名称
        try {
            String pattern = buildUsageCountKey("*");
            var keys = redisTemplate.keys(pattern);
            if (keys != null) {
                for (String key : keys) {
                    String agentName = extractAgentNameFromKey(key);
                    if (agentName != null) {
                        allStats.put(agentName, getAgentStats(agentName));
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取所有Agent统计信息失败", e);
        }
        return allStats;
    }

    // ===== 私有方法 =====

    private String buildUsageCountKey(String agentName) {
        return "linglong:agent:usage:" + agentName;
    }

    private String buildAgentStatusKey(String agentName) {
        return "linglong:agent:status:" + agentName;
    }

    private String buildLastExecutionKey(String agentName) {
        return "linglong:agent:lastexec:" + agentName;
    }

    private String extractAgentNameFromKey(String key) {
        // 从 "linglong:agent:usage:AgentName" 提取 AgentName
        if (key != null && key.startsWith("linglong:agent:usage:")) {
            return key.substring("linglong:agent:usage:".length());
        }
        return null;
    }
}
