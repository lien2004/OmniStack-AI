package com.linglong.llm.controller;

import com.linglong.llm.service.LlmProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LLM 供应商管理接口
 *
 * <pre>
 * GET    /ai/llm/providers          列出全部供应商
 * GET    /ai/llm/providers/{id}     获取单个供应商
 * POST   /ai/llm/providers          新增供应商
 * PUT    /ai/llm/providers/{id}     更新供应商
 * DELETE /ai/llm/providers/{id}     删除供应商
 * POST   /ai/llm/providers/{id}/enable   启用供应商
 * POST   /ai/llm/providers/{id}/disable  禁用供应商
 * POST   /ai/llm/providers/{id}/default  设为默认
 * POST   /ai/llm/providers/test     测试连接（不保存）
 * </pre>
 */
@Tag(name = "LLM 供应商管理", description = "支持商用/开源/自建大语言模型的配置与管理")
@RestController
@RequestMapping("/ai/llm/providers")
@ConditionalOnBean(LlmProviderService.class)
public class LlmProviderController {

    @Autowired(required = false)
    private LlmProviderService llmProviderService;

    // ── 查询全部 ────────────────────────────────────────────────────────────

    @Operation(summary = "获取所有LLM供应商配置列表")
    @GetMapping
    public ResponseEntity<Map<String, Object>> listAll() {
        if (llmProviderService == null) return serviceUnavailable();
        List<Map<String, Object>> providers = llmProviderService.findAll();
        return ok(providers);
    }

    // ── 查询单个 ────────────────────────────────────────────────────────────

    @Operation(summary = "获取单个LLM供应商配置")
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getOne(@PathVariable String id) {
        if (llmProviderService == null) return serviceUnavailable();
        Map<String, Object> provider = llmProviderService.findById(id);
        if (provider == null) return notFound("供应商不存在: " + id);
        return ok(provider);
    }

    // ── 新增 ────────────────────────────────────────────────────────────────

    @Operation(summary = "新增LLM供应商配置")
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        if (llmProviderService == null) return serviceUnavailable();
        try {
            Map<String, Object> saved = llmProviderService.create(body);
            return ok(saved, "供应商创建成功");
        } catch (Exception e) {
            return error("创建失败: " + e.getMessage());
        }
    }

    // ── 更新 ────────────────────────────────────────────────────────────────

    @Operation(summary = "更新LLM供应商配置")
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable String id,
                                                       @RequestBody Map<String, Object> body) {
        if (llmProviderService == null) return serviceUnavailable();
        try {
            Map<String, Object> saved = llmProviderService.update(id, body);
            if (saved == null) return notFound("供应商不存在: " + id);
            return ok(saved, "供应商更新成功");
        } catch (Exception e) {
            return error("更新失败: " + e.getMessage());
        }
    }

    // ── 删除 ────────────────────────────────────────────────────────────────

    @Operation(summary = "删除LLM供应商配置")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable String id) {
        if (llmProviderService == null) return serviceUnavailable();
        try {
            llmProviderService.delete(id);
            return ok(null, "删除成功");
        } catch (Exception e) {
            return error("删除失败: " + e.getMessage());
        }
    }

    // ── 启用 / 禁用 ─────────────────────────────────────────────────────────

    @Operation(summary = "启用供应商")
    @PostMapping("/{id}/enable")
    public ResponseEntity<Map<String, Object>> enable(@PathVariable String id) {
        if (llmProviderService == null) return serviceUnavailable();
        llmProviderService.toggleEnabled(id, true);
        return ok(null, "已启用");
    }

    @Operation(summary = "禁用供应商")
    @PostMapping("/{id}/disable")
    public ResponseEntity<Map<String, Object>> disable(@PathVariable String id) {
        if (llmProviderService == null) return serviceUnavailable();
        llmProviderService.toggleEnabled(id, false);
        return ok(null, "已禁用");
    }

    // ── 设为默认 ────────────────────────────────────────────────────────────

    @Operation(summary = "设置为默认供应商")
    @PostMapping("/{id}/default")
    public ResponseEntity<Map<String, Object>> setDefault(@PathVariable String id) {
        if (llmProviderService == null) return serviceUnavailable();
        llmProviderService.setDefault(id);
        return ok(null, "已设为默认供应商");
    }

    // ── 连接测试 ────────────────────────────────────────────────────────────

    @Operation(summary = "测试LLM API连接（不保存）")
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testConnection(@RequestBody Map<String, Object> body) {
        if (llmProviderService == null) return serviceUnavailable();
        Map<String, Object> result = llmProviderService.testConnection(body);
        return ok(result);
    }

    // ── 工具方法 ────────────────────────────────────────────────────────────

    private ResponseEntity<Map<String, Object>> ok(Object data) {
        return ok(data, null);
    }

    private ResponseEntity<Map<String, Object>> ok(Object data, String message) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("code", 200);
        resp.put("success", true);
        if (message != null) resp.put("message", message);
        resp.put("data", data);
        resp.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(resp);
    }

    private ResponseEntity<Map<String, Object>> error(String message) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("code", 500);
        resp.put("success", false);
        resp.put("message", message);
        resp.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.status(500).body(resp);
    }

    private ResponseEntity<Map<String, Object>> notFound(String message) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("code", 404);
        resp.put("success", false);
        resp.put("message", message);
        resp.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.status(404).body(resp);
    }

    private ResponseEntity<Map<String, Object>> serviceUnavailable() {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("code", 503);
        resp.put("success", false);
        resp.put("message", "数据库服务未就绪，请检查 chat.datasource 配置");
        resp.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.status(503).body(resp);
    }
}
