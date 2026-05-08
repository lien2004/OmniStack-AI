package com.linglong.llm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * LLM 供应商配置管理服务
 * 支持商用、开源、自建模型的配置持久化（MySQL）及 API 连接测试
 */
@Service
public class LlmProviderService {

    private static final Logger log = LoggerFactory.getLogger(LlmProviderService.class);
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public LlmProviderService(@Qualifier("chatDataSource") DataSource chatDataSource,
                               ObjectMapper objectMapper) {
        this.jdbc = new JdbcTemplate(chatDataSource);
        this.objectMapper = objectMapper;
    }

    /** 启动时自动建表（幂等） */
    @PostConstruct
    public void initTable() {
        String ddl = """
                CREATE TABLE IF NOT EXISTS llm_providers (
                    id            VARCHAR(36)  NOT NULL PRIMARY KEY,
                    name          VARCHAR(100) NOT NULL COMMENT '供应商显示名称',
                    vendor        VARCHAR(50)  NOT NULL COMMENT '厂商标识(openai/zhipu/deepseek/custom...)',
                    provider_type VARCHAR(20)  NOT NULL DEFAULT 'commercial' COMMENT 'commercial/open_source/self_hosted',
                    base_url      VARCHAR(500) COMMENT 'API 基础地址',
                    api_key       VARCHAR(1000) COMMENT 'API 密钥（加密存储）',
                    default_model VARCHAR(200) COMMENT '默认调用模型',
                    custom_models TEXT         COMMENT '自定义模型列表(JSON数组)',
                    routing_keywords VARCHAR(500) COMMENT '路由关键词前缀，逗号分隔',
                    temperature   DECIMAL(4,2) DEFAULT 0.70,
                    max_tokens    INT          DEFAULT 4000,
                    top_p         DECIMAL(4,2) DEFAULT 1.00,
                    context_length INT         DEFAULT 32000,
                    stream_enabled TINYINT(1)  DEFAULT 1,
                    timeout_seconds INT        DEFAULT 120,
                    is_default    TINYINT(1)  DEFAULT 0,
                    enabled       TINYINT(1)  DEFAULT 1,
                    remark        TEXT,
                    created_at    DATETIME     DEFAULT CURRENT_TIMESTAMP,
                    updated_at    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LLM供应商配置表';
                """;
        try {
            jdbc.execute(ddl);
            log.info("llm_providers 表初始化成功");
        } catch (Exception e) {
            log.warn("llm_providers 表初始化失败（可能已存在）: {}", e.getMessage());
        }
    }

    /** 查询所有供应商 */
    public List<Map<String, Object>> findAll() {
        String sql = "SELECT * FROM llm_providers ORDER BY is_default DESC, created_at ASC";
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(sql);
            rows.forEach(this::convertRow);
            return rows;
        } catch (Exception e) {
            log.error("查询LLM供应商列表失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /** 根据 ID 查询 */
    public Map<String, Object> findById(String id) {
        String sql = "SELECT * FROM llm_providers WHERE id = ?";
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(sql, id);
            if (rows.isEmpty()) return null;
            convertRow(rows.get(0));
            return rows.get(0);
        } catch (Exception e) {
            log.error("查询LLM供应商失败: {}", e.getMessage());
            return null;
        }
    }

    /** 创建供应商 */
    public Map<String, Object> create(Map<String, Object> data) {
        String id = UUID.randomUUID().toString();
        String now = LocalDateTime.now().format(DTF);
        String sql = """
                INSERT INTO llm_providers
                  (id, name, vendor, provider_type, base_url, api_key, default_model,
                   custom_models, routing_keywords, temperature, max_tokens, top_p,
                   context_length, stream_enabled, timeout_seconds, is_default, enabled,
                   remark, created_at, updated_at)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """;
        jdbc.update(sql,
                id,
                getString(data, "name"),
                getString(data, "vendor", "custom"),
                getString(data, "providerType", "commercial"),
                getString(data, "baseUrl"),
                getString(data, "apiKey"),
                getString(data, "defaultModel"),
                toJsonArray(data.get("customModels")),
                getString(data, "routingKeywords"),
                getDouble(data, "temperature", 0.7),
                getInt(data, "maxTokens", 4000),
                getDouble(data, "topP", 1.0),
                getInt(data, "contextLength", 32000),
                getBool(data, "streamEnabled", true),
                getInt(data, "timeoutSeconds", 120),
                getBool(data, "isDefault", false),
                getBool(data, "enabled", true),
                getString(data, "remark"),
                now, now
        );
        // 如果设为默认，取消其他默认
        if (getBool(data, "isDefault", false)) {
            jdbc.update("UPDATE llm_providers SET is_default=0 WHERE id != ?", id);
        }
        return findById(id);
    }

    /** 更新供应商 */
    public Map<String, Object> update(String id, Map<String, Object> data) {
        String now = LocalDateTime.now().format(DTF);
        String sql = """
                UPDATE llm_providers SET
                  name=?, vendor=?, provider_type=?, base_url=?, api_key=?,
                  default_model=?, custom_models=?, routing_keywords=?,
                  temperature=?, max_tokens=?, top_p=?, context_length=?,
                  stream_enabled=?, timeout_seconds=?, is_default=?, enabled=?,
                  remark=?, updated_at=?
                WHERE id=?
                """;
        jdbc.update(sql,
                getString(data, "name"),
                getString(data, "vendor", "custom"),
                getString(data, "providerType", "commercial"),
                getString(data, "baseUrl"),
                getString(data, "apiKey"),
                getString(data, "defaultModel"),
                toJsonArray(data.get("customModels")),
                getString(data, "routingKeywords"),
                getDouble(data, "temperature", 0.7),
                getInt(data, "maxTokens", 4000),
                getDouble(data, "topP", 1.0),
                getInt(data, "contextLength", 32000),
                getBool(data, "streamEnabled", true),
                getInt(data, "timeoutSeconds", 120),
                getBool(data, "isDefault", false),
                getBool(data, "enabled", true),
                getString(data, "remark"),
                now, id
        );
        if (getBool(data, "isDefault", false)) {
            jdbc.update("UPDATE llm_providers SET is_default=0 WHERE id != ?", id);
        }
        return findById(id);
    }

    /** 删除供应商 */
    public void delete(String id) {
        jdbc.update("DELETE FROM llm_providers WHERE id = ?", id);
    }

    /** 切换启用状态 */
    public void toggleEnabled(String id, boolean enabled) {
        jdbc.update("UPDATE llm_providers SET enabled=?, updated_at=? WHERE id=?",
                enabled ? 1 : 0, LocalDateTime.now().format(DTF), id);
    }

    /** 设置为默认供应商 */
    public void setDefault(String id) {
        jdbc.update("UPDATE llm_providers SET is_default=0");
        jdbc.update("UPDATE llm_providers SET is_default=1, updated_at=? WHERE id=?",
                LocalDateTime.now().format(DTF), id);
    }

    /** 测试 LLM 连接：发送一条最简消息，验证 API Key 和 Base URL 是否有效 */
    public Map<String, Object> testConnection(Map<String, Object> data) {
        String baseUrl = getString(data, "baseUrl");
        String apiKey  = getString(data, "apiKey");
        String model   = getString(data, "defaultModel", "gpt-3.5-turbo");

        if (baseUrl == null || baseUrl.isBlank()) {
            // 根据 vendor 自动推断 baseUrl
            baseUrl = resolveDefaultBaseUrl(getString(data, "vendor", ""));
        }
        if (apiKey == null || apiKey.isBlank()) {
            return Map.of("success", false, "message", "API Key 不能为空");
        }

        String url = baseUrl.endsWith("/") ? baseUrl + "chat/completions"
                                           : baseUrl + "/chat/completions";
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(15))
                    .build();

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("messages", List.of(Map.of("role", "user", "content", "hi")));
            body.put("max_tokens", 5);
            body.put("stream", false);

            String bodyStr = objectMapper.writeValueAsString(body);
            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(java.time.Duration.ofSeconds(30))
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(bodyStr))
                    .build();

            java.net.http.HttpResponse<String> resp =
                    client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());

            int status = resp.statusCode();
            if (status == 200 || status == 201) {
                result.put("success", true);
                result.put("message", "连接测试成功，API 响应正常");
                result.put("statusCode", status);
            } else if (status == 401) {
                result.put("success", false);
                result.put("message", "API Key 无效（401 Unauthorized）");
            } else if (status == 404) {
                result.put("success", false);
                result.put("message", "接口地址不存在（404），请检查 Base URL");
            } else {
                // 非标准状态码也可能是成功（如代理返回 4xx 但包含合法响应）
                result.put("success", status < 500);
                result.put("message", "HTTP " + status + (status < 500 ? "（可能正常）" : "（服务端错误）"));
                result.put("statusCode", status);
            }
        } catch (java.net.ConnectException e) {
            result.put("success", false);
            result.put("message", "连接失败：无法连接到 " + url + "，请检查地址和网络");
        } catch (java.net.http.HttpTimeoutException e) {
            result.put("success", false);
            result.put("message", "连接超时：服务器响应时间过长");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "测试异常：" + e.getMessage());
        }
        return result;
    }

    // ── 私有工具方法 ────────────────────────────────────────────────────────

    private String resolveDefaultBaseUrl(String vendor) {
        return switch (vendor.toLowerCase()) {
            case "openai"   -> "https://api.openai.com/v1";
            case "zhipu"    -> "https://open.bigmodel.cn/api/paas/v4";
            case "deepseek" -> "https://api.deepseek.com/v1";
            case "anthropic"-> "https://api.anthropic.com/v1";
            case "moonshot" -> "https://api.moonshot.cn/v1";
            case "baidu"    -> "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat";
            case "aliyun"   -> "https://dashscope.aliyuncs.com/compatible-mode/v1";
            case "doubao"   -> "https://ark.cn-beijing.volces.com/api/v3";
            case "ollama"   -> "http://localhost:11434/v1";
            default         -> "";
        };
    }

    @SuppressWarnings("unchecked")
    private void convertRow(Map<String, Object> row) {
        // 将数据库下划线字段名转为驼峰
        renameKey(row, "provider_type", "providerType");
        renameKey(row, "base_url", "baseUrl");
        renameKey(row, "api_key", "apiKey");
        renameKey(row, "default_model", "defaultModel");
        renameKey(row, "custom_models", "customModels");
        renameKey(row, "routing_keywords", "routingKeywords");
        renameKey(row, "max_tokens", "maxTokens");
        renameKey(row, "top_p", "topP");
        renameKey(row, "context_length", "contextLength");
        renameKey(row, "stream_enabled", "streamEnabled");
        renameKey(row, "timeout_seconds", "timeoutSeconds");
        renameKey(row, "is_default", "isDefault");
        renameKey(row, "created_at", "createdAt");
        renameKey(row, "updated_at", "updatedAt");

        // 解析 customModels JSON
        Object cmRaw = row.get("customModels");
        if (cmRaw instanceof String s && !s.isBlank()) {
            try {
                row.put("customModels", objectMapper.readValue(s, List.class));
            } catch (Exception ignored) {
                row.put("customModels", List.of(s));
            }
        } else if (cmRaw == null) {
            row.put("customModels", List.of());
        }

        // API key 脱敏
        Object keyRaw = row.get("apiKey");
        if (keyRaw instanceof String k && k.length() > 8) {
            row.put("apiKeyMasked", "••••••••" + k.substring(k.length() - 4));
        }

        // Boolean 字段归一化
        row.put("isDefault", toBoolVal(row.get("isDefault")));
        row.put("enabled", toBoolVal(row.get("enabled")));
        row.put("streamEnabled", toBoolVal(row.get("streamEnabled")));
    }

    private void renameKey(Map<String, Object> map, String oldKey, String newKey) {
        if (map.containsKey(oldKey)) {
            map.put(newKey, map.remove(oldKey));
        }
    }

    private boolean toBoolVal(Object v) {
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n) return n.intValue() != 0;
        return false;
    }

    private String getString(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? null : v.toString();
    }

    private String getString(Map<String, Object> m, String k, String def) {
        String v = getString(m, k);
        return (v == null || v.isBlank()) ? def : v;
    }

    private double getDouble(Map<String, Object> m, String k, double def) {
        Object v = m.get(k);
        if (v == null) return def;
        try { return Double.parseDouble(v.toString()); } catch (Exception e) { return def; }
    }

    private int getInt(Map<String, Object> m, String k, int def) {
        Object v = m.get(k);
        if (v == null) return def;
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return def; }
    }

    private boolean getBool(Map<String, Object> m, String k, boolean def) {
        Object v = m.get(k);
        if (v == null) return def;
        if (v instanceof Boolean b) return b;
        return "true".equalsIgnoreCase(v.toString()) || "1".equals(v.toString());
    }

    @SuppressWarnings("unchecked")
    private String toJsonArray(Object v) {
        if (v == null) return "[]";
        if (v instanceof List) {
            try { return objectMapper.writeValueAsString(v); } catch (Exception e) { return "[]"; }
        }
        if (v instanceof String s) {
            s = s.trim();
            if (s.startsWith("[")) return s;
            // 如果是逗号分隔字符串，转换为数组
            String[] parts = s.split(",");
            List<String> list = new ArrayList<>();
            for (String p : parts) {
                String trimmed = p.trim();
                if (!trimmed.isBlank()) list.add(trimmed);
            }
            try { return objectMapper.writeValueAsString(list); } catch (Exception e) { return "[]"; }
        }
        return "[]";
    }
}
