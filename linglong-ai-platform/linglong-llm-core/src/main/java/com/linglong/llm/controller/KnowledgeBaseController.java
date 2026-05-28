package com.linglong.llm.controller;

import com.linglong.llm.model.KnowledgeBase;
import com.linglong.llm.model.KnowledgeDocument;
import com.linglong.llm.service.KnowledgeBaseService;
import com.linglong.llm.service.KnowledgeChunkService;
import com.linglong.llm.service.KnowledgeDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 知识库 / 文档 / 分块 三级管理 API
 */
@Tag(name = "知识库管理", description = "知识库、文档、分块的三级管理接口")
@RestController
@RequestMapping("/ai/kb")
public class KnowledgeBaseController {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseController.class);

    @Autowired
    private KnowledgeBaseService kbService;

    @Autowired
    private KnowledgeDocumentService documentService;

    @Autowired
    private KnowledgeChunkService chunkService;

    // ============ 知识库 ============

    @Operation(summary = "知识库列表")
    @GetMapping
    public List<KnowledgeBase> listKb() {
        return kbService.list();
    }

    @Operation(summary = "知识库统计概览（顶部 4 个卡片）")
    @GetMapping("/stats")
    public Map<String, Object> overview() {
        return kbService.overviewStats();
    }

    @Operation(summary = "知识库详情")
    @GetMapping("/{id}")
    public ResponseEntity<KnowledgeBase> getKb(@PathVariable("id") String id) {
        KnowledgeBase kb = kbService.get(id);
        return kb == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(kb);
    }

    @Operation(summary = "创建知识库")
    @PostMapping
    public KnowledgeBase createKb(@RequestBody KnowledgeBase kb) {
        return kbService.create(kb);
    }

    @Operation(summary = "更新知识库")
    @PutMapping("/{id}")
    public KnowledgeBase updateKb(@PathVariable("id") String id, @RequestBody KnowledgeBase kb) {
        return kbService.update(id, kb);
    }

    @Operation(summary = "删除知识库（连同其下文档与向量）")
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteKb(@PathVariable("id") String id) {
        kbService.delete(id);
        return Map.of("success", true);
    }

    // ============ 文档 ============

    @Operation(summary = "文档列表")
    @GetMapping("/{kbId}/documents")
    public Map<String, Object> listDocuments(
            @PathVariable("kbId") String kbId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status) {
        return documentService.list(kbId, page, size, keyword, status);
    }

    @Operation(summary = "文档详情")
    @GetMapping("/{kbId}/documents/{docId}")
    public ResponseEntity<KnowledgeDocument> getDocument(
            @PathVariable("kbId") String kbId,
            @PathVariable("docId") String docId) {
        KnowledgeDocument doc = documentService.get(docId);
        return doc == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(doc);
    }

    @Operation(summary = "上传文档到知识库")
    @PostMapping(value = "/{kbId}/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @PathVariable("kbId") String kbId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "category", required = false) String category) {
        try {
            KnowledgeDocument doc = documentService.upload(kbId, file, name, category);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "上传成功",
                    "document", doc));
        } catch (Exception e) {
            log.error("文档上传失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage() == null ? "上传失败" : e.getMessage()));
        }
    }

    @Operation(summary = "启用/禁用文档")
    @PostMapping("/{kbId}/documents/{docId}/enabled")
    public KnowledgeDocument setDocumentEnabled(
            @PathVariable("kbId") String kbId,
            @PathVariable("docId") String docId,
            @RequestBody Map<String, Boolean> body) {
        boolean enabled = Boolean.TRUE.equals(body.get("enabled"));
        return documentService.setEnabled(docId, enabled);
    }

    @Operation(summary = "重建文档向量")
    @PostMapping("/{kbId}/documents/{docId}/rebuild")
    public KnowledgeDocument rebuildDocument(
            @PathVariable("kbId") String kbId,
            @PathVariable("docId") String docId) {
        return documentService.rebuild(docId);
    }

    @Operation(summary = "删除文档")
    @DeleteMapping("/{kbId}/documents/{docId}")
    public Map<String, Object> deleteDocument(
            @PathVariable("kbId") String kbId,
            @PathVariable("docId") String docId) {
        documentService.delete(docId);
        return Map.of("success", true);
    }

    // ============ 分块 ============

    @Operation(summary = "分块列表")
    @GetMapping("/{kbId}/documents/{docId}/chunks")
    public Map<String, Object> listChunks(
            @PathVariable("kbId") String kbId,
            @PathVariable("docId") String docId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return chunkService.list(docId, page, size, keyword);
    }

    @Operation(summary = "分块详情")
    @GetMapping("/chunks/{chunkId}")
    public ResponseEntity<Map<String, Object>> getChunk(@PathVariable("chunkId") String chunkId) {
        Map<String, Object> chunk = chunkService.get(chunkId);
        return chunk == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(chunk);
    }

    @Operation(summary = "编辑分块内容（自动重新 embedding）")
    @PutMapping("/chunks/{chunkId}")
    public Map<String, Object> updateChunk(
            @PathVariable("chunkId") String chunkId,
            @RequestBody Map<String, String> body) {
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            return Map.of("success", false, "message", "内容不能为空");
        }
        return chunkService.updateContent(chunkId, content);
    }

    @Operation(summary = "批量启停分块")
    @PostMapping("/chunks/enabled")
    public Map<String, Object> batchSetEnabled(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> ids = (List<String>) body.get("ids");
        boolean enabled = Boolean.TRUE.equals(body.get("enabled"));
        chunkService.setEnabled(ids, enabled);
        return Map.of("success", true, "count", ids == null ? 0 : ids.size());
    }

    @Operation(summary = "批量删除分块")
    @PostMapping("/chunks/delete")
    public Map<String, Object> batchDelete(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> ids = (List<String>) body.get("ids");
        chunkService.delete(ids);
        return Map.of("success", true, "count", ids == null ? 0 : ids.size());
    }

    @Operation(summary = "批量重建分块向量")
    @PostMapping("/chunks/rebuild")
    public Map<String, Object> batchRebuild(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> ids = (List<String>) body.get("ids");
        chunkService.rebuild(ids);
        return Map.of("success", true, "count", ids == null ? 0 : ids.size());
    }
}
