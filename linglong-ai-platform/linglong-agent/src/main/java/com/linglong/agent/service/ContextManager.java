package com.linglong.agent.service;

import com.linglong.agent.model.AgentContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 上下文管理服务
 * 使用Redis存储和共享Agent上下文
 */
@Service
public class ContextManager {

    private static final Logger log = LoggerFactory.getLogger(ContextManager.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    // 上下文过期时间(24小时)
    private static final long CONTEXT_EXPIRE_HOURS = 24;

    public ContextManager(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 保存上下文到Redis
     *
     * @param contextId 上下文ID
     * @param context   Agent上下文
     */
    public void saveContext(String contextId, AgentContext context) {
        try {
            String key = buildContextKey(contextId);
            redisTemplate.opsForValue().set(key, context, CONTEXT_EXPIRE_HOURS, TimeUnit.HOURS);
            log.debug("上下文已保存到Redis: {}", contextId);
        } catch (Exception e) {
            log.error("保存上下文失败: {}", contextId, e);
        }
    }

    /**
     * 从Redis获取上下文
     *
     * @param contextId 上下文ID
     * @return Agent上下文
     */
    public AgentContext getContext(String contextId) {
        try {
            String key = buildContextKey(contextId);
            Object value = redisTemplate.opsForValue().get(key);

            if (value instanceof AgentContext) {
                return (AgentContext) value;
            }

            // 尝试反序列化
            if (value instanceof Map) {
                return objectMapper.convertValue(value, AgentContext.class);
            }

            return null;
        } catch (Exception e) {
            log.error("获取上下文失败: {}", contextId, e);
            return null;
        }
    }

    /**
     * 更新上下文中的共享数据
     *
     * @param contextId 上下文ID
     * @param key       数据键
     * @param value     数据值
     */
    public void updateSharedData(String contextId, String key, Object value) {
        try {
            AgentContext context = getContext(contextId);
            if (context == null) {
                context = new AgentContext();
                context.setTaskId(contextId);
            }

            context.addToShared(key, value);
            saveContext(contextId, context);

            log.debug("上下文共享数据已更新: {} -> {}", contextId, key);
        } catch (Exception e) {
            log.error("更新共享数据失败: {} -> {}", contextId, key, e);
        }
    }

    /**
     * 获取上下文中的共享数据
     *
     * @param contextId 上下文ID
     * @param key       数据键
     * @return 数据值
     */
    @SuppressWarnings("unchecked")
    public <T> T getSharedData(String contextId, String key) {
        try {
            AgentContext context = getContext(contextId);
            if (context == null) {
                return null;
            }

            return (T) context.getFromShared(key);
        } catch (Exception e) {
            log.error("获取共享数据失败: {} -> {}", contextId, key, e);
            return null;
        }
    }

    /**
     * 删除上下文
     *
     * @param contextId 上下文ID
     */
    public void deleteContext(String contextId) {
        try {
            String key = buildContextKey(contextId);
            redisTemplate.delete(key);
            log.debug("上下文已删除: {}", contextId);
        } catch (Exception e) {
            log.error("删除上下文失败: {}", contextId, e);
        }
    }

    /**
     * 延长上下文过期时间
     *
     * @param contextId 上下文ID
     */
    public void extendContextTTL(String contextId) {
        try {
            String key = buildContextKey(contextId);
            redisTemplate.expire(key, CONTEXT_EXPIRE_HOURS, TimeUnit.HOURS);
            log.debug("上下文过期时间已延长: {}", contextId);
        } catch (Exception e) {
            log.error("延长上下文过期时间失败: {}", contextId, e);
        }
    }

    /**
     * 保存聊天历史
     *
     * @param sessionId 会话ID
     * @param role      角色(user/assistant)
     * @param content   内容
     */
    public void saveChatHistory(String sessionId, String role, String content) {
        try {
            String key = buildChatHistoryKey(sessionId);
            Map<String, String> message = Map.of(
                    "role", role,
                    "content", content,
                    "timestamp", String.valueOf(System.currentTimeMillis())
            );

            redisTemplate.opsForList().rightPush(key, message);
            redisTemplate.expire(key, CONTEXT_EXPIRE_HOURS, TimeUnit.HOURS);

            log.debug("聊天历史已保存: {}", sessionId);
        } catch (Exception e) {
            log.error("保存聊天历史失败: {}", sessionId, e);
        }
    }

    /**
     * 获取聊天历史
     *
     * @param sessionId 会话ID
     * @param limit     限制条数
     * @return 聊天历史列表
     */
    @SuppressWarnings("unchecked")
    public java.util.List<Map<String, String>> getChatHistory(String sessionId, int limit) {
        try {
            String key = buildChatHistoryKey(sessionId);
            Long size = redisTemplate.opsForList().size(key);

            if (size == null || size == 0) {
                return java.util.Collections.emptyList();
            }

            int start = (int) Math.max(0, size - limit);
            int end = size.intValue() - 1;

            return (java.util.List<Map<String, String>>) (Object) redisTemplate.opsForList()
                    .range(key, start, end);

        } catch (Exception e) {
            log.error("获取聊天历史失败: {}", sessionId, e);
            return java.util.Collections.emptyList();
        }
    }

    /**
     * 清除聊天历史
     *
     * @param sessionId 会话ID
     */
    public void clearChatHistory(String sessionId) {
        try {
            String key = buildChatHistoryKey(sessionId);
            redisTemplate.delete(key);
            log.debug("聊天历史已清除: {}", sessionId);
        } catch (Exception e) {
            log.error("清除聊天历史失败: {}", sessionId, e);
        }
    }

    /**
     * 构建上下文Key
     */
    private String buildContextKey(String contextId) {
        return "linglong:context:" + contextId;
    }

    /**
     * 构建聊天历史Key
     */
    private String buildChatHistoryKey(String sessionId) {
        return "linglong:chat:" + sessionId;
    }
}

