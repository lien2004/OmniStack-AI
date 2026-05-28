package com.linglong.llm.service;

import com.linglong.llm.model.KnowledgeBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class KnowledgeBaseService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseService.class);

    @Autowired
    @Qualifier("chatJdbcTemplate")
    private JdbcTemplate mysqlJdbc;

    @Autowired
    @Qualifier("pgJdbcTemplate")
    private JdbcTemplate pgJdbc;

    private final RowMapper<KnowledgeBase> kbMapper = (rs, rowNum) -> KnowledgeBase.builder()
            .id(rs.getString("id"))
            .name(rs.getString("name"))
            .description(rs.getString("description"))
            .collectionName(rs.getString("collection_name"))
            .embeddingModel(rs.getString("embedding_model"))
            .dimensions(rs.getInt("dimensions"))
            .owner(rs.getString("owner"))
            .createdAt(toLdt(rs.getTimestamp("created_at")))
            .updatedAt(toLdt(rs.getTimestamp("updated_at")))
            .build();

    public List<KnowledgeBase> list() {
        List<KnowledgeBase> bases = mysqlJdbc.query(
                "SELECT * FROM knowledge_base ORDER BY created_at DESC", kbMapper);
        Map<String, int[]> stats = aggregateStats();
        Map<String, Long> sizes = aggregateSizes();
        for (KnowledgeBase kb : bases) {
            int[] s = stats.getOrDefault(kb.getId(), new int[]{0, 0});
            kb.setDocumentCount(s[0]);
            kb.setChunkCount(s[1]);
            kb.setSizeBytes(sizes.getOrDefault(kb.getId(), 0L));
        }
        return bases;
    }

    public KnowledgeBase get(String id) {
        List<KnowledgeBase> rows = mysqlJdbc.query(
                "SELECT * FROM knowledge_base WHERE id = ?", kbMapper, id);
        if (rows.isEmpty()) {
            return null;
        }
        KnowledgeBase kb = rows.get(0);
        Map<String, int[]> stats = aggregateStats();
        Map<String, Long> sizes = aggregateSizes();
        int[] s = stats.getOrDefault(kb.getId(), new int[]{0, 0});
        kb.setDocumentCount(s[0]);
        kb.setChunkCount(s[1]);
        kb.setSizeBytes(sizes.getOrDefault(kb.getId(), 0L));
        return kb;
    }

    public KnowledgeBase create(KnowledgeBase kb) {
        if (kb.getId() == null || kb.getId().isBlank()) {
            kb.setId("kb-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        }
        if (kb.getCollectionName() == null || kb.getCollectionName().isBlank()) {
            kb.setCollectionName(kb.getId());
        }
        if (kb.getEmbeddingModel() == null) kb.setEmbeddingModel("embedding-3");
        if (kb.getDimensions() == null) kb.setDimensions(1024);
        mysqlJdbc.update("""
                INSERT INTO knowledge_base (id, name, description, collection_name, embedding_model, dimensions, owner)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                kb.getId(), kb.getName(), kb.getDescription(),
                kb.getCollectionName(), kb.getEmbeddingModel(), kb.getDimensions(),
                kb.getOwner() == null ? "system" : kb.getOwner());
        return get(kb.getId());
    }

    public KnowledgeBase update(String id, KnowledgeBase kb) {
        mysqlJdbc.update("""
                UPDATE knowledge_base
                SET name = COALESCE(?, name),
                    description = COALESCE(?, description),
                    embedding_model = COALESCE(?, embedding_model),
                    dimensions = COALESCE(?, dimensions),
                    owner = COALESCE(?, owner)
                WHERE id = ?
                """,
                kb.getName(), kb.getDescription(),
                kb.getEmbeddingModel(), kb.getDimensions(), kb.getOwner(), id);
        return get(id);
    }

    public void delete(String id) {
        // 先删 vector_store 中的 chunk
        try {
            pgJdbc.update("DELETE FROM vector_store WHERE metadata->>'kbId' = ?", id);
        } catch (DataAccessException e) {
            log.warn("删除 kb {} 的向量数据失败: {}", id, e.getMessage());
        }
        mysqlJdbc.update("DELETE FROM knowledge_document WHERE kb_id = ?", id);
        mysqlJdbc.update("DELETE FROM knowledge_base WHERE id = ?", id);
    }

    public Map<String, Object> overviewStats() {
        Map<String, Object> result = new LinkedHashMap<>();
        Long kbCount = mysqlJdbc.queryForObject(
                "SELECT count(*) FROM knowledge_base", Long.class);
        Long docCount = mysqlJdbc.queryForObject(
                "SELECT count(*) FROM knowledge_document", Long.class);
        Long chunkCount = 0L;
        Long sizeBytes = 0L;
        try {
            chunkCount = pgJdbc.queryForObject(
                    "SELECT count(*) FROM vector_store WHERE metadata->>'type'='knowledge'", Long.class);
        } catch (Exception ignored) {
        }
        Long mysqlSize = mysqlJdbc.queryForObject(
                "SELECT COALESCE(SUM(size_bytes),0) FROM knowledge_document", Long.class);
        if (mysqlSize != null) sizeBytes = mysqlSize;
        result.put("knowledgeBaseCount", kbCount == null ? 0 : kbCount);
        result.put("documentCount", docCount == null ? 0 : docCount);
        result.put("chunkCount", chunkCount == null ? 0 : chunkCount);
        result.put("sizeBytes", sizeBytes);
        return result;
    }

    /** 返回 kbId -> [documentCount, chunkCount] */
    private Map<String, int[]> aggregateStats() {
        Map<String, int[]> stats = new HashMap<>();
        List<Map<String, Object>> docRows = mysqlJdbc.queryForList(
                "SELECT kb_id, count(*) cnt, COALESCE(SUM(chunk_count),0) chunks FROM knowledge_document GROUP BY kb_id");
        for (Map<String, Object> r : docRows) {
            String kbId = (String) r.get("kb_id");
            int docCount = ((Number) r.get("cnt")).intValue();
            int chunks = ((Number) r.get("chunks")).intValue();
            stats.put(kbId, new int[]{docCount, chunks});
        }
        return stats;
    }

    private Map<String, Long> aggregateSizes() {
        Map<String, Long> map = new HashMap<>();
        List<Map<String, Object>> rows = mysqlJdbc.queryForList(
                "SELECT kb_id, COALESCE(SUM(size_bytes),0) total FROM knowledge_document GROUP BY kb_id");
        for (Map<String, Object> r : rows) {
            map.put((String) r.get("kb_id"), ((Number) r.get("total")).longValue());
        }
        return map;
    }

    private java.time.LocalDateTime toLdt(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }
}
