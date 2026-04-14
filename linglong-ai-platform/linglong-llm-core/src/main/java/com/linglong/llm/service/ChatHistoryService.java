package com.linglong.llm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 对话历史服务 - 基于 MySQL 持久化存储
 * 每个 chatId 对应一个独立会话，每次进入页面生成新的 chatId
 */
@Service
@ConditionalOnProperty(name = "chat.datasource.url")  // 仅当配置了 MySQL 时激活
public class ChatHistoryService {

    private static final Logger log = LoggerFactory.getLogger(ChatHistoryService.class);

    @Value("${conversation.history.max-messages:20}")
    private int maxMessages;

    private final JdbcTemplate chatJdbcTemplate;

    public ChatHistoryService(@Qualifier("chatJdbcTemplate") JdbcTemplate chatJdbcTemplate) {
        this.chatJdbcTemplate = chatJdbcTemplate;
    }

    /**
     * 保存一条消息（user 或 assistant）
     */
    public void saveMessage(String chatId, String role, String content,
                            String model, String fileAttachment) {
        try {
            chatJdbcTemplate.update(
                    "INSERT INTO chat_message (chat_id, role, content, model, file_attachment) VALUES (?, ?, ?, ?, ?)",
                    chatId, role, content, model, fileAttachment
            );
        } catch (Exception e) {
            log.error("保存消息失败 chatId={}: {}", chatId, e.getMessage(), e);
        }
    }

    /**
     * 获取指定会话的完整历史记录（按时间正序）
     */
    public List<Map<String, Object>> getHistory(String chatId) {
        try {
            return chatJdbcTemplate.queryForList(
                    "SELECT role, content, model, file_attachment, created_at " +
                    "FROM chat_message WHERE chat_id = ? ORDER BY created_at ASC",
                    chatId
            );
        } catch (Exception e) {
            log.error("获取历史记录失败 chatId={}: {}", chatId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 获取最近 N 条消息（用于传入 LLM 的上下文，按时间正序返回）
     */
    public List<Map<String, Object>> getLastNMessages(String chatId, int n) {
        try {
            // 先取最新 N 条（倒序），再反转为正序
            List<Map<String, Object>> rows = chatJdbcTemplate.queryForList(
                    "SELECT role, content, model FROM chat_message " +
                    "WHERE chat_id = ? ORDER BY created_at DESC LIMIT ?",
                    chatId, n
            );
            Collections.reverse(rows);
            return rows;
        } catch (Exception e) {
            log.error("获取最近消息失败 chatId={}: {}", chatId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 获取所有会话列表（按最新消息时间倒序）
     * 每个会话取第一条 user 消息作为标题
     */
    public List<Map<String, Object>> getAllSessions() {
        try {
            return chatJdbcTemplate.queryForList(
                    "SELECT t.chat_id, t.title, t.last_time, t.message_count " +
                    "FROM ( " +
                    "    SELECT chat_id, " +
                    "           (SELECT content FROM chat_message m2 " +
                    "            WHERE m2.chat_id = m1.chat_id AND m2.role = 'user' " +
                    "            ORDER BY created_at ASC LIMIT 1) AS title, " +
                    "           MAX(created_at) AS last_time, " +
                    "           COUNT(*) AS message_count " +
                    "    FROM chat_message m1 " +
                    "    GROUP BY chat_id " +
                    ") t ORDER BY t.last_time DESC LIMIT 100"
            );
        } catch (Exception e) {
            log.error("获取会话列表失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 删除指定会话的所有消息
     */
    public void deleteSession(String chatId) {
        try {
            int rows = chatJdbcTemplate.update(
                    "DELETE FROM chat_message WHERE chat_id = ?", chatId);
            log.info("已删除会话 chatId={}, 共删除 {} 条消息", chatId, rows);
        } catch (Exception e) {
            log.error("删除会话失败 chatId={}: {}", chatId, e.getMessage(), e);
        }
    }

    /**
     * 清空所有会话历史
     */
    public void clearAll() {
        try {
            chatJdbcTemplate.update("DELETE FROM chat_message");
            log.info("已清空所有对话历史");
        } catch (Exception e) {
            log.error("清空历史失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 将历史记录转换为 LLM messages 格式（用于多轮对话上下文）
     */
    public List<Map<String, Object>> buildContextMessages(String chatId) {
        List<Map<String, Object>> history = getLastNMessages(chatId, maxMessages);
        List<Map<String, Object>> messages = new ArrayList<>();
        for (Map<String, Object> row : history) {
            messages.add(Map.of(
                    "role", String.valueOf(row.get("role")),
                    "content", String.valueOf(row.get("content"))
            ));
        }
        return messages;
    }
}
