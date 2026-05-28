package com.linglong.llm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linglong.llm.model.KnowledgeDocument;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class KnowledgeDocumentService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeDocumentService.class);
    private static final Tika tika = new Tika();
    private static final int CHUNK_SIZE = 800;
    private static final int CHUNK_OVERLAP = 100;

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    @Qualifier("chatJdbcTemplate")
    private JdbcTemplate mysqlJdbc;

    @Autowired
    @Qualifier("pgJdbcTemplate")
    private JdbcTemplate pgJdbc;

    @Autowired
    private VectorStoreService vectorStoreService;

    private final RowMapper<KnowledgeDocument> docMapper = (rs, rowNum) -> KnowledgeDocument.builder()
            .id(rs.getString("id"))
            .kbId(rs.getString("kb_id"))
            .name(rs.getString("name"))
            .filename(rs.getString("filename"))
            .category(rs.getString("category"))
            .mimeType(rs.getString("mime_type"))
            .sizeBytes(rs.getLong("size_bytes"))
            .chunkCount(rs.getInt("chunk_count"))
            .status(rs.getString("status"))
            .enabled(rs.getInt("enabled") == 1)
            .errorMessage(rs.getString("error_message"))
            .uploader(rs.getString("uploader"))
            .createdAt(toLdt(rs.getTimestamp("created_at")))
            .updatedAt(toLdt(rs.getTimestamp("updated_at")))
            .build();

    public Map<String, Object> list(String kbId, int page, int size, String keyword, String status) {
        StringBuilder where = new StringBuilder("WHERE kb_id = ?");
        List<Object> args = new ArrayList<>();
        args.add(kbId);
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (name LIKE ? OR filename LIKE ?)");
            args.add("%" + keyword + "%");
            args.add("%" + keyword + "%");
        }
        if (status != null && !status.isBlank()) {
            where.append(" AND status = ?");
            args.add(status);
        }

        Long total = mysqlJdbc.queryForObject(
                "SELECT count(*) FROM knowledge_document " + where, Long.class, args.toArray());

        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add(size);
        queryArgs.add(page * size);
        List<KnowledgeDocument> rows = mysqlJdbc.query(
                "SELECT * FROM knowledge_document " + where + " ORDER BY created_at DESC LIMIT ? OFFSET ?",
                docMapper, queryArgs.toArray());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total == null ? 0 : total);
        result.put("page", page);
        result.put("size", size);
        result.put("items", rows);
        return result;
    }

    public KnowledgeDocument get(String docId) {
        List<KnowledgeDocument> rows = mysqlJdbc.query(
                "SELECT * FROM knowledge_document WHERE id = ?", docMapper, docId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public KnowledgeDocument upload(String kbId, MultipartFile file, String name, String category) throws Exception {
        // 1. 校验知识库存在
        Long kbExists = mysqlJdbc.queryForObject(
                "SELECT count(*) FROM knowledge_base WHERE id = ?", Long.class, kbId);
        if (kbExists == null || kbExists == 0) {
            throw new IllegalArgumentException("知识库不存在: " + kbId);
        }

        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document";
        String docName = (name == null || name.isBlank()) ? filename : name;
        String cat = (category == null || category.isBlank()) ? "document" : category;
        String mimeType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
        long sizeBytes = file.getSize();
        String docId = "doc-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        // 2. 先写入文档记录（状态：parsing）
        mysqlJdbc.update("""
                INSERT INTO knowledge_document
                  (id, kb_id, name, filename, category, mime_type, size_bytes, chunk_count, status, enabled, uploader)
                VALUES (?, ?, ?, ?, ?, ?, ?, 0, 'parsing', 1, ?)
                """,
                docId, kbId, docName, filename, cat, mimeType, sizeBytes, "system");

        Path tmp = null;
        try {
            tmp = Files.createTempFile("kbdoc_", "_" + filename);
            file.transferTo(tmp.toFile());
            String text = extractText(tmp.toFile(), filename);
            if (text.isBlank()) {
                markFailed(docId, "文档内容为空，无法建索");
                throw new IllegalStateException("文档内容为空");
            }

            // 3. 分块 + 写入向量库
            List<String> chunks = splitIntoChunks(text);
            List<Document> docs = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                Map<String, Object> meta = new LinkedHashMap<>();
                meta.put("type", "knowledge");
                meta.put("kbId", kbId);
                meta.put("documentId", docId);
                meta.put("category", cat);
                meta.put("filename", filename);
                meta.put("title", docName);
                meta.put("chunkIndex", i);
                meta.put("totalChunks", chunks.size());
                meta.put("enabled", true);
                docs.add(new Document(chunks.get(i), meta));
            }
            vectorStoreService.addDocuments(docs);

            // 4. 更新状态为 ready + chunkCount
            mysqlJdbc.update(
                    "UPDATE knowledge_document SET status = 'ready', chunk_count = ?, error_message = NULL WHERE id = ?",
                    chunks.size(), docId);

            return get(docId);
        } catch (Exception e) {
            markFailed(docId, e.getMessage());
            throw e;
        } finally {
            if (tmp != null) try { Files.deleteIfExists(tmp); } catch (Exception ignore) {}
        }
    }

    public KnowledgeDocument setEnabled(String docId, boolean enabled) {
        mysqlJdbc.update(
                "UPDATE knowledge_document SET enabled = ? WHERE id = ?",
                enabled ? 1 : 0, docId);
        // 联动更新 vector_store 中所有 chunk 的 metadata.enabled
        try {
            pgJdbc.update("""
                    UPDATE vector_store
                    SET metadata = jsonb_set(metadata, '{enabled}', to_jsonb(?::boolean), true)
                    WHERE metadata->>'documentId' = ?
                    """, enabled, docId);
        } catch (DataAccessException e) {
            log.warn("更新文档 {} 的 chunk enabled 失败: {}", docId, e.getMessage());
        }
        return get(docId);
    }

    public void delete(String docId) {
        try {
            pgJdbc.update("DELETE FROM vector_store WHERE metadata->>'documentId' = ?", docId);
        } catch (DataAccessException e) {
            log.warn("删除文档 {} 的向量失败: {}", docId, e.getMessage());
        }
        mysqlJdbc.update("DELETE FROM knowledge_document WHERE id = ?", docId);
    }

    /**
     * 重建向量：删除原有 chunk，重新读取文档内容（从 vector_store 已有的内容拼回）
     * 注意：实际生产环境应该保留原始文件并基于原文重建。这里基于已有 chunk 内容重建以演示。
     */
    public KnowledgeDocument rebuild(String docId) {
        KnowledgeDocument doc = get(docId);
        if (doc == null) {
            throw new IllegalArgumentException("文档不存在: " + docId);
        }
        // 取出原始 chunk 内容并按 chunkIndex 排序
        List<Map<String, Object>> rows;
        try {
            rows = pgJdbc.queryForList("""
                    SELECT content, metadata
                    FROM vector_store
                    WHERE metadata->>'documentId' = ?
                    ORDER BY (metadata->>'chunkIndex')::int
                    """, docId);
        } catch (DataAccessException e) {
            throw new IllegalStateException("读取原始 chunk 失败: " + e.getMessage());
        }
        if (rows.isEmpty()) {
            throw new IllegalStateException("文档没有可重建的内容");
        }

        StringBuilder full = new StringBuilder();
        for (Map<String, Object> r : rows) {
            full.append(r.get("content")).append("\n\n");
        }

        mysqlJdbc.update(
                "UPDATE knowledge_document SET status = 'parsing' WHERE id = ?", docId);

        try {
            pgJdbc.update("DELETE FROM vector_store WHERE metadata->>'documentId' = ?", docId);
            List<String> chunks = splitIntoChunks(full.toString());
            List<Document> docs = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                Map<String, Object> meta = new LinkedHashMap<>();
                meta.put("type", "knowledge");
                meta.put("kbId", doc.getKbId());
                meta.put("documentId", docId);
                meta.put("category", doc.getCategory());
                meta.put("filename", doc.getFilename());
                meta.put("title", doc.getName());
                meta.put("chunkIndex", i);
                meta.put("totalChunks", chunks.size());
                meta.put("enabled", doc.getEnabled() != null && doc.getEnabled());
                docs.add(new Document(chunks.get(i), meta));
            }
            vectorStoreService.addDocuments(docs);
            mysqlJdbc.update(
                    "UPDATE knowledge_document SET status = 'ready', chunk_count = ?, error_message = NULL WHERE id = ?",
                    chunks.size(), docId);
            return get(docId);
        } catch (Exception e) {
            markFailed(docId, e.getMessage());
            throw new RuntimeException("重建失败: " + e.getMessage(), e);
        }
    }

    private void markFailed(String docId, String error) {
        mysqlJdbc.update(
                "UPDATE knowledge_document SET status = 'failed', error_message = ? WHERE id = ?",
                error == null ? "未知错误" : (error.length() > 900 ? error.substring(0, 900) : error), docId);
    }

    // ---------- 文本提取 / 分块 ----------

    private String extractText(File file, String filename) throws Exception {
        String mimeType = tika.detect(file);
        if (mimeType != null && (
                mimeType.contains("pdf") ||
                mimeType.contains("word") ||
                mimeType.contains("officedocument") ||
                mimeType.contains("opendocument") ||
                mimeType.contains("powerpoint") ||
                mimeType.contains("excel") ||
                mimeType.contains("spreadsheet") ||
                mimeType.contains("presentation") ||
                mimeType.contains("rtf") ||
                mimeType.contains("msword"))) {
            return parseWithTika(file);
        }
        try {
            return new String(Files.readAllBytes(file.toPath()), java.nio.charset.StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            return parseWithTika(file);
        }
    }

    private String parseWithTika(File file) throws Exception {
        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler(-1);
        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();
        try (InputStream stream = new FileInputStream(file)) {
            parser.parse(stream, handler, metadata, context);
        }
        return handler.toString().trim();
    }

    private List<String> splitIntoChunks(String text) {
        List<String> chunks = new ArrayList<>();
        String[] paragraphs = text.split("\\n\\n+");
        StringBuilder current = new StringBuilder();
        for (String para : paragraphs) {
            para = para.trim();
            if (para.isEmpty()) continue;
            if (current.length() + para.length() > CHUNK_SIZE && current.length() > 0) {
                chunks.add(current.toString().trim());
                String overlap = current.length() > CHUNK_OVERLAP
                        ? current.substring(current.length() - CHUNK_OVERLAP) : current.toString();
                current = new StringBuilder(overlap);
            }
            if (para.length() > CHUNK_SIZE) {
                String[] lines = para.split("\\n");
                for (String line : lines) {
                    if (current.length() + line.length() > CHUNK_SIZE && current.length() > 0) {
                        chunks.add(current.toString().trim());
                        String overlap = current.length() > CHUNK_OVERLAP
                                ? current.substring(current.length() - CHUNK_OVERLAP) : current.toString();
                        current = new StringBuilder(overlap);
                    }
                    current.append(line).append("\n");
                }
            } else {
                if (current.length() > 0) current.append("\n\n");
                current.append(para);
            }
        }
        if (current.length() > 0) chunks.add(current.toString().trim());
        return chunks;
    }

    private java.time.LocalDateTime toLdt(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }
}
