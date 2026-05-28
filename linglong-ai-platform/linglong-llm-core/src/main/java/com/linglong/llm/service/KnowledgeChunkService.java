package com.linglong.llm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class KnowledgeChunkService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeChunkService.class);

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    @Qualifier("pgJdbcTemplate")
    private JdbcTemplate pgJdbc;

    @Autowired
    private EmbeddingClient embeddingClient;

    @Autowired
    private VectorStoreService vectorStoreService;

    public Map<String, Object> list(String docId, int page, int size, String keyword) {
        if (!isReady()) {
            return emptyResult(page, size);
        }
        StringBuilder where = new StringBuilder("WHERE metadata->>'documentId' = ?");
        List<Object> args = new java.util.ArrayList<>();
        args.add(docId);
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND content ILIKE ?");
            args.add("%" + keyword + "%");
        }

        Long total = pgJdbc.queryForObject(
                "SELECT count(*) FROM vector_store " + where, Long.class, args.toArray());

        List<Object> queryArgs = new java.util.ArrayList<>(args);
        queryArgs.add(size);
        queryArgs.add(page * size);
        List<Map<String, Object>> rows = pgJdbc.queryForList(
                "SELECT id, content, metadata FROM vector_store " + where +
                        " ORDER BY (metadata->>'chunkIndex')::int LIMIT ? OFFSET ?",
                queryArgs.toArray());

        List<Map<String, Object>> items = rows.stream().map(this::toItem).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total == null ? 0 : total);
        result.put("page", page);
        result.put("size", size);
        result.put("items", items);
        return result;
    }

    public Map<String, Object> get(String chunkId) {
        if (!isReady()) return null;
        List<Map<String, Object>> rows = pgJdbc.queryForList(
                "SELECT id, content, metadata FROM vector_store WHERE id = ?::uuid", chunkId);
        return rows.isEmpty() ? null : toItem(rows.get(0));
    }

    /**
     * 编辑分块内容：删除旧 chunk + 用相同 metadata 重新写入，触发重新 embedding
     */
    public Map<String, Object> updateContent(String chunkId, String newContent) {
        Map<String, Object> existing = get(chunkId);
        if (existing == null) {
            throw new IllegalArgumentException("分块不存在: " + chunkId);
        }
        Map<String, Object> metadata = parseMetadata(existing.get("metadata"));
        // 删除旧的
        pgJdbc.update("DELETE FROM vector_store WHERE id = ?::uuid", chunkId);
        // 写入新的
        Document doc = new Document(newContent, metadata);
        vectorStoreService.addDocuments(List.of(doc));
        return Map.of("success", true, "message", "分块已更新");
    }

    public void setEnabled(List<String> chunkIds, boolean enabled) {
        if (chunkIds == null || chunkIds.isEmpty()) return;
        for (String id : chunkIds) {
            try {
                pgJdbc.update("""
                        UPDATE vector_store
                        SET metadata = jsonb_set(metadata, '{enabled}', to_jsonb(?::boolean), true)
                        WHERE id = ?::uuid
                        """, enabled, id);
            } catch (DataAccessException e) {
                log.warn("更新 chunk {} 启停失败: {}", id, e.getMessage());
            }
        }
    }

    public void delete(List<String> chunkIds) {
        if (chunkIds == null || chunkIds.isEmpty()) return;
        vectorStoreService.deleteDocuments(chunkIds);
    }

    /** 重新计算 embedding（其实就是删了重写） */
    public void rebuild(List<String> chunkIds) {
        if (chunkIds == null || chunkIds.isEmpty()) return;
        for (String id : chunkIds) {
            Map<String, Object> existing = get(id);
            if (existing == null) continue;
            String content = (String) existing.get("content");
            Map<String, Object> meta = parseMetadata(existing.get("metadata"));
            pgJdbc.update("DELETE FROM vector_store WHERE id = ?::uuid", id);
            Document doc = new Document(content, meta);
            vectorStoreService.addDocuments(List.of(doc));
        }
    }

    // ---------- helpers ----------

    private Map<String, Object> toItem(Map<String, Object> row) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", row.get("id"));
        String content = String.valueOf(row.get("content"));
        item.put("content", content);
        item.put("contentLength", content.length());
        Map<String, Object> meta = parseMetadata(row.get("metadata"));
        item.put("metadata", meta);
        item.put("chunkIndex", meta.get("chunkIndex"));
        item.put("totalChunks", meta.get("totalChunks"));
        item.put("enabled", meta.get("enabled") == null ? true : meta.get("enabled"));
        return item;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseMetadata(Object metaObj) {
        if (metaObj == null) return new HashMap<>();
        try {
            return mapper.readValue(metaObj.toString(), Map.class);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private boolean isReady() {
        try {
            Integer cnt = pgJdbc.queryForObject(
                    "SELECT count(*) FROM information_schema.tables WHERE table_name='vector_store'", Integer.class);
            return cnt != null && cnt > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private Map<String, Object> emptyResult(int page, int size) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", 0);
        r.put("page", page);
        r.put("size", size);
        r.put("items", List.of());
        return r;
    }
}
