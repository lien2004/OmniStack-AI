package com.linglong.llm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 历史数据迁移：把存量 vector_store 中 type=knowledge 的 chunk 归到「默认知识库」下
 */
@Service
public class KnowledgeMigrationService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeMigrationService.class);
    private static final String DEFAULT_KB_ID = "kb-default";
    private static final String DEFAULT_KB_NAME = "默认知识库";
    private static final String DEFAULT_KB_COLLECTION = "default";

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    @Qualifier("chatJdbcTemplate")
    private JdbcTemplate mysqlJdbc;

    @Autowired
    @Qualifier("pgJdbcTemplate")
    private JdbcTemplate pgJdbc;

    public void ensureDefaultKnowledgeBase() {
        Long count = mysqlJdbc.queryForObject(
                "SELECT count(*) FROM knowledge_base WHERE id = ?", Long.class, DEFAULT_KB_ID);
        if (count != null && count > 0) {
            return;
        }
        mysqlJdbc.update("""
                INSERT INTO knowledge_base (id, name, description, collection_name, embedding_model, dimensions, owner)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                DEFAULT_KB_ID, DEFAULT_KB_NAME, "系统初始化的默认知识库",
                DEFAULT_KB_COLLECTION, "embedding-3", 1024, "system");
        log.info("已创建默认知识库: {}", DEFAULT_KB_ID);
    }

    /**
     * 把 vector_store 中没有 kbId 的 knowledge 类型 chunk 归并为默认知识库的文档
     */
    public void migrateLegacyChunks() {
        if (!isVectorStoreReady()) {
            log.info("vector_store 未就绪，跳过迁移");
            return;
        }
        List<Map<String, Object>> rows;
        try {
            rows = pgJdbc.queryForList("""
                    SELECT id, content, metadata
                    FROM vector_store
                    WHERE metadata->>'type' = 'knowledge'
                      AND (metadata->>'kbId' IS NULL OR metadata->>'kbId' = '')
                    """);
        } catch (DataAccessException e) {
            log.warn("查询存量 chunk 失败，跳过迁移: {}", e.getMessage());
            return;
        }
        if (rows.isEmpty()) {
            return;
        }
        log.info("开始迁移 {} 条存量 chunk 到默认知识库", rows.size());

        // 按 filename + title 聚合成「文档」
        Map<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> meta = parseMetadata(row.get("metadata"));
            String filename = (String) meta.getOrDefault("filename", "未命名");
            String title = (String) meta.getOrDefault("title", filename);
            String groupKey = filename + "::" + title;
            grouped.computeIfAbsent(groupKey, k -> new java.util.ArrayList<>()).add(row);
        }

        for (Map.Entry<String, List<Map<String, Object>>> entry : grouped.entrySet()) {
            List<Map<String, Object>> chunks = entry.getValue();
            Map<String, Object> firstMeta = parseMetadata(chunks.get(0).get("metadata"));
            String filename = (String) firstMeta.getOrDefault("filename", "未命名");
            String title = (String) firstMeta.getOrDefault("title", filename);
            String category = (String) firstMeta.getOrDefault("category", "document");

            String docId = "doc-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            long totalLen = chunks.stream()
                    .mapToLong(c -> String.valueOf(c.get("content")).length())
                    .sum();

            try {
                mysqlJdbc.update("""
                        INSERT INTO knowledge_document
                          (id, kb_id, name, filename, category, mime_type, size_bytes, chunk_count, status, enabled, uploader)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'ready', 1, 'system')
                        """,
                        docId, DEFAULT_KB_ID, title, filename, category,
                        "application/octet-stream", totalLen, chunks.size());
            } catch (DataAccessException e) {
                log.warn("迁移文档 {} 失败: {}", title, e.getMessage());
                continue;
            }

            // 更新每条 chunk 的 metadata：补 kbId/documentId/enabled
            for (Map<String, Object> row : chunks) {
                String chunkId = String.valueOf(row.get("id"));
                Map<String, Object> meta = parseMetadata(row.get("metadata"));
                meta.put("kbId", DEFAULT_KB_ID);
                meta.put("documentId", docId);
                meta.putIfAbsent("enabled", true);
                try {
                    String json = mapper.writeValueAsString(meta);
                    pgJdbc.update(
                            "UPDATE vector_store SET metadata = ?::jsonb WHERE id = ?::uuid",
                            json, chunkId);
                } catch (Exception e) {
                    log.warn("更新 chunk {} metadata 失败: {}", chunkId, e.getMessage());
                }
            }
        }
        log.info("存量 chunk 迁移完成");
    }

    private boolean isVectorStoreReady() {
        try {
            Integer cnt = pgJdbc.queryForObject(
                    "SELECT count(*) FROM information_schema.tables WHERE table_name='vector_store'",
                    Integer.class);
            return cnt != null && cnt > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseMetadata(Object metaObj) {
        if (metaObj == null) {
            return new HashMap<>();
        }
        try {
            String s = metaObj.toString();
            return mapper.readValue(s, Map.class);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}
