package com.linglong.llm.controller;

import com.linglong.llm.service.VectorStoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 向量存储控制器
 * 提供文档存储、文件上传向量化、语义检索的 REST API
 */
@Tag(name = "向量知识库", description = "文档/代码/需求向量存储、文件上传建索、语义检索和知识库管理")
@RestController
@RequestMapping("/vector")
public class VectorStoreController {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreController.class);
    private static final Tika tika = new Tika();

    /** 单块最大字符数 */
    private static final int CHUNK_SIZE = 800;
    /** 块间重叠字符数 */
    private static final int CHUNK_OVERLAP = 100;

    private final VectorStoreService vectorStoreService;

    @Autowired
    @Qualifier("pgJdbcTemplate")
    private JdbcTemplate pgJdbcTemplate;

    public VectorStoreController(VectorStoreService vectorStoreService) {
        this.vectorStoreService = vectorStoreService;
    }

    // ==================== 存储接口 ====================

    /**
     * 添加单个文档
     * POST /vector/doc
     * Body: { "content": "文档内容", "metadata": { "type": "code", "projectId": "1" } }
     */
    @Operation(summary = "添加单个文档", description = "将文档内容向量化并存入知识库")
    @PostMapping("/doc")
    public Map<String, Object> addDocument(@RequestBody Map<String, Object> request) {
        String content = (String) request.get("content");
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) request.getOrDefault("metadata", Map.of());

        vectorStoreService.addDocument(content, metadata);
        return Map.of("success", true, "message", "文档已添加");
    }

    /**
     * 批量添加文档
     * POST /vector/docs
     * Body: [ { "content": "内容1", "metadata": {} }, { "content": "内容2", "metadata": {} } ]
     */
    @Operation(summary = "批量添加文档")
    @PostMapping("/docs")
    public Map<String, Object> addDocuments(@RequestBody List<Map<String, Object>> items) {
        List<Document> documents = items.stream()
                .map(item -> {
                    String content = (String) item.get("content");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> metadata = (Map<String, Object>) item.getOrDefault("metadata", new HashMap<>());
                    // 确保 type 字段存在，默认为 knowledge（知识库文档）
                    if (!metadata.containsKey("type")) {
                        metadata.put("type", "knowledge");
                    }
                    return new Document(content, metadata);
                })
                .collect(Collectors.toList());

        vectorStoreService.addDocuments(documents);
        return Map.of("success", true, "message", "已添加 " + documents.size() + " 个文档");
    }

    /**
     * 添加代码片段
     * POST /vector/code
     * Body: { "content": "代码内容", "fileName": "UserService.java", "projectId": "1" }
     */
    @Operation(summary = "添加代码片段")
    @PostMapping("/code")
    public Map<String, Object> addCode(@RequestBody Map<String, String> request) {
        String content = request.get("content");
        String fileName = request.get("fileName");
        String projectId = request.get("projectId");

        Document doc = vectorStoreService.createCodeDocument(content, fileName, projectId);
        vectorStoreService.addDocuments(List.of(doc));
        return Map.of("success", true, "message", "代码片段已添加", "fileName", fileName);
    }

    /**
     * 添加需求文档
     * POST /vector/requirement
     * Body: { "content": "需求内容", "title": "用户登录", "projectId": "1" }
     */
    @Operation(summary = "添加需求文档")
    @PostMapping("/requirement")
    public Map<String, Object> addRequirement(@RequestBody Map<String, String> request) {
        String content = request.get("content");
        String title = request.get("title");
        String projectId = request.get("projectId");

        Document doc = vectorStoreService.createRequirementDocument(content, title, projectId);
        vectorStoreService.addDocuments(List.of(doc));
        return Map.of("success", true, "message", "需求文档已添加", "title", title);
    }

    // ==================== 搜索接口 ====================

    /**
     * 语义搜索
     * GET /vector/search?query=用户登录&topK=5
     */
    @Operation(summary = "语义搜索", description = "基于向量相似度的语义搜索")
    @GetMapping("/search")
    public List<Map<String, Object>> search(
            @Parameter(description = "搜索关键词/语义", required = true) @RequestParam(value = "query") String query,
            @Parameter(description = "返回结果数") @RequestParam(value = "topK", defaultValue = "5") int topK) {

        List<Document> results = vectorStoreService.similaritySearch(query, topK);
        return toResultList(results);
    }

    /**
     * 带相似度阈値的搜索
     * GET /vector/search/threshold?query=用户登录&topK=5&threshold=0.7
     */
    @Operation(summary = "带相似度阈値的搜索")
    @GetMapping("/search/threshold")
    public List<Map<String, Object>> searchWithThreshold(
            @Parameter(description = "搜索语义", required = true) @RequestParam(value = "query") String query,
            @Parameter(description = "最多返回数") @RequestParam(value = "topK", defaultValue = "5") int topK,
            @Parameter(description = "相似度阈値（0-1）") @RequestParam(value = "threshold", defaultValue = "0.7") double threshold) {

        List<Document> results = vectorStoreService.similaritySearchWithThreshold(query, topK, threshold);
        return toResultList(results);
    }

    /**
     * 按项目搜索代码
     * GET /vector/search/code?query=用户登录&projectId=1&topK=5
     */
    @Operation(summary = "按项目搜索代码")
    @GetMapping("/search/code")
    public List<Map<String, Object>> searchCode(
            @Parameter(description = "搜索内容", required = true) @RequestParam(value = "query") String query,
            @Parameter(description = "项目 ID", required = true) @RequestParam(value = "projectId") String projectId,
            @Parameter(description = "返回数量") @RequestParam(value = "topK", defaultValue = "5") int topK) {

        List<Document> results = vectorStoreService.searchCodeByProject(query, projectId, topK);
        return toResultList(results);
    }

    /**
     * 按项目搜索需求
     * GET /vector/search/requirement?query=用户登录&projectId=1&topK=5
     */
    @Operation(summary = "按项目搜索需求")
    @GetMapping("/search/requirement")
    public List<Map<String, Object>> searchRequirement(
            @Parameter(description = "搜索内容", required = true) @RequestParam(value = "query") String query,
            @Parameter(description = "项目 ID", required = true) @RequestParam(value = "projectId") String projectId,
            @Parameter(description = "返回数量") @RequestParam(value = "topK", defaultValue = "5") int topK) {

        List<Document> results = vectorStoreService.searchRequirements(query, projectId, topK);
        return toResultList(results);
    }

    // ==================== 文件上传向量化 ====================

    /**
     * 上传文件并向量化存入知识库
     * POST /vector/upload
     * 参数：file(文件), title(标题可选), category(分类可选)
     */
    @Operation(summary = "上传文件到知识库", description = "上传文件后自动解析、分块并向量化存入知识库")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadToKnowledge(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", defaultValue = "") String title,
            @RequestParam(value = "category", defaultValue = "document") String category
    ) {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document";
        if (title.isBlank()) title = filename;
        log.info("开始上传文件到知识库: {} | 分类: {}", filename, category);

        Path tmp = null;
        try {
            // 1. 保存临时文件
            tmp = Files.createTempFile("kb_upload_", "_" + filename);
            file.transferTo(tmp.toFile());

            // 2. Tika 解析文本
            String text = extractText(tmp.toFile(), filename);
            if (text.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "文档内容为空，无法建索"));
            }

            // 3. 分块
            List<String> chunks = splitIntoChunks(text);
            log.info("文件 {} 解析完成，共 {} 块", filename, chunks.size());

            // 4. 构建 Document 列表并存入向量库
            final String docTitle = title;
            List<Document> docs = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                Map<String, Object> meta = new LinkedHashMap<>();
                meta.put("type", "knowledge");
                meta.put("category", category);
                meta.put("filename", filename);
                meta.put("title", docTitle);
                meta.put("chunkIndex", i);
                meta.put("totalChunks", chunks.size());
                docs.add(new Document(chunks.get(i), meta));
            }
            vectorStoreService.addDocuments(docs);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "文档已成功建索",
                    "filename", filename,
                    "chunks", chunks.size()
            ));
        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "上传失败：" + e.getMessage()));
        } finally {
            if (tmp != null) try { Files.deleteIfExists(tmp); } catch (Exception ignore) {}
        }
    }

    // ==================== 文档列表 ====================

    /**
     * 获取知识库文档列表（不返回向量）
     * GET /vector/list?page=0&size=20&category=document
     */
    @Operation(summary = "获取知识库文档列表", description = "分页获取知识库文档，不返回向量字段")
    @GetMapping("/list")
    public Map<String, Object> listDocuments(
            @Parameter(description = "页码（从0起）") @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "每页数量") @RequestParam(value = "size", defaultValue = "20") int size,
            @Parameter(description = "文档分类过滤") @RequestParam(value = "category", defaultValue = "") String category
    ) {
        String countSql;
        String querySql;
        Object[] params;

        // 只查询知识库文档 (type = 'knowledge')，排除对话缓存 (type = 'conversation')
        if (category.isBlank()) {
            countSql = "SELECT count(*) FROM vector_store WHERE metadata->>'type' = 'knowledge'";
            querySql = "SELECT id, content, metadata FROM vector_store WHERE metadata->>'type' = 'knowledge' ORDER BY id LIMIT ? OFFSET ?";
            params = new Object[]{ size, page * size };
        } else {
            countSql = "SELECT count(*) FROM vector_store WHERE metadata->>'type' = 'knowledge' AND metadata->>'category' = ?";
            querySql = "SELECT id, content, metadata FROM vector_store WHERE metadata->>'type' = 'knowledge' AND metadata->>'category' = ? ORDER BY id LIMIT ? OFFSET ?";
            params = new Object[]{ category, size, page * size };
        }

        if (!isVectorStoreAvailable()) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("total", 0);
            empty.put("page", page);
            empty.put("size", size);
            empty.put("items", Collections.emptyList());
            return empty;
        }

        long total = category.isBlank()
                ? pgJdbcTemplate.queryForObject(countSql, Long.class)
                : pgJdbcTemplate.queryForObject(countSql, Long.class, category);

        List<Map<String, Object>> rows = pgJdbcTemplate.queryForList(querySql, params);
        List<Map<String, Object>> items = rows.stream().map(row -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.get("id"));
            String content = String.valueOf(row.get("content"));
            item.put("contentPreview", content.length() > 120 ? content.substring(0, 120) + "..." : content);
            item.put("contentLength", content.length());
            item.put("metadata", row.get("metadata"));
            return item;
        }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("items", items);
        return result;
    }

    // ==================== 详情接口 ====================

    /**
     * 获取单条向量文档完整内容
     * GET /vector/doc/{id}
     */
    @Operation(summary = "获取单条文档完整内容")
    @GetMapping("/doc/{id}")
    public ResponseEntity<Map<String, Object>> getDocument(@PathVariable("id") String id) {
        if (!isVectorStoreAvailable()) {
            return ResponseEntity.notFound().build();
        }
        List<Map<String, Object>> rows = pgJdbcTemplate.queryForList(
                "SELECT id, content, metadata FROM vector_store WHERE id = ?::uuid", id);
        if (rows.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> row = rows.get(0);
        String content = String.valueOf(row.get("content"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", row.get("id"));
        result.put("content", content);
        result.put("contentLength", content.length());
        result.put("metadata", row.get("metadata"));
        return ResponseEntity.ok(result);
    }

    // ==================== 删除接口 ====================

    /**
     * 删除文档
     * DELETE /vector/docs
     * Body: ["id1", "id2"]
     */
    @Operation(summary = "删除文档", description = "按 ID 列表批量删除向量文档")
    @DeleteMapping("/docs")
    public Map<String, Object> deleteDocuments(@RequestBody List<String> ids) {
        vectorStoreService.deleteDocuments(ids);
        return Map.of("success", true, "message", "已删除 " + ids.size() + " 个文档");
    }

    // ==================== 工具方法 ====================

    /** 检查 vector_store 表是否存在 */
    private boolean isVectorStoreAvailable() {
        try {
            Integer count = pgJdbcTemplate.queryForObject(
                    "SELECT count(*) FROM information_schema.tables WHERE table_name = 'vector_store'", Integer.class);
            return count != null && count > 0;
        } catch (Exception e) {
            log.warn("[VectorStore] 检查 vector_store 表失败: {}", e.getMessage());
            return false;
        }
    }

    /** 使用 Apache Tika 提取文件文本 */
    private String extractText(File file, String filename) throws Exception {
        String mimeType = tika.detect(file);
        if (mimeType != null && (
                mimeType.contains("pdf")            ||
                mimeType.contains("word")           ||
                mimeType.contains("officedocument") ||
                mimeType.contains("opendocument")   ||
                mimeType.contains("powerpoint")     ||
                mimeType.contains("excel")          ||
                mimeType.contains("spreadsheet")    ||
                mimeType.contains("presentation")   ||
                mimeType.contains("rtf")            ||
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

    /**
     * 按段落分块，单块超过 CHUNK_SIZE 时切分
     */
    private List<String> splitIntoChunks(String text) {
        List<String> chunks = new ArrayList<>();
        // 先按双换行分段落
        String[] paragraphs = text.split("\\n\\n+");
        StringBuilder current = new StringBuilder();

        for (String para : paragraphs) {
            para = para.trim();
            if (para.isEmpty()) continue;

            if (current.length() + para.length() > CHUNK_SIZE && current.length() > 0) {
                chunks.add(current.toString().trim());
                // 保留重叠部分
                String overlap = current.length() > CHUNK_OVERLAP
                        ? current.substring(current.length() - CHUNK_OVERLAP)
                        : current.toString();
                current = new StringBuilder(overlap);
            }

            if (para.length() > CHUNK_SIZE) {
                // 超长段落按段切分
                String[] lines = para.split("\\n");
                for (String line : lines) {
                    if (current.length() + line.length() > CHUNK_SIZE && current.length() > 0) {
                        chunks.add(current.toString().trim());
                        String overlap = current.length() > CHUNK_OVERLAP
                                ? current.substring(current.length() - CHUNK_OVERLAP)
                                : current.toString();
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

    private List<Map<String, Object>> toResultList(List<Document> documents) {
        return documents.stream()
                .map(doc -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("id", doc.getId());
                    result.put("content", doc.getContent());
                    result.put("metadata", doc.getMetadata());
                    return result;
                })
                .collect(Collectors.toList());
    }
}
