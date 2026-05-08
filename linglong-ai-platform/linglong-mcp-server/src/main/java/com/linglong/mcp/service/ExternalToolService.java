package com.linglong.mcp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.linglong.mcp.model.ExternalToolConfig;
import com.linglong.mcp.model.ToolDefinition;
import com.linglong.mcp.model.ToolExecutionRequest;
import com.linglong.mcp.model.ToolExecutionResult;
import com.linglong.mcp.registry.ToolRegistry;
import com.linglong.mcp.repository.ExternalToolConfigRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 外部MCP工具服务
 * 管理外部工具的CRUD，并支持将外部HTTP API动态注册为MCP工具
 */
@Service
public class ExternalToolService {

    private static final Logger log = LoggerFactory.getLogger(ExternalToolService.class);

    private final ExternalToolConfigRepository repository;
    private final ToolRegistry toolRegistry;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public ExternalToolService(ExternalToolConfigRepository repository,
                               ToolRegistry toolRegistry,
                               ObjectMapper objectMapper) {
        this.repository = repository;
        this.toolRegistry = toolRegistry;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    /**
     * 启动时加载所有已启用的外部工具并注册到注册中心
     */
    @PostConstruct
    public void init() {
        log.info("开始加载外部工具配置...");
        List<ExternalToolConfig> configs = repository.findAllByEnabledTrue();
        for (ExternalToolConfig config : configs) {
            try {
                registerExternalTool(config);
                log.info("外部工具已加载: {}", config.getToolName());
            } catch (Exception e) {
                log.error("加载外部工具失败: {}", config.getToolName(), e);
            }
        }
        log.info("外部工具加载完成，共 {} 个", configs.size());
    }

    /**
     * 创建并注册外部工具
     */
    public ExternalToolConfig createExternalTool(ExternalToolConfig config) {
        if (repository.existsByToolName(config.getToolName())) {
            throw new IllegalArgumentException("工具名称已存在: " + config.getToolName());
        }
        config.setSource("external");
        ExternalToolConfig saved = repository.save(config);
        if (Boolean.TRUE.equals(saved.getEnabled())) {
            registerExternalTool(saved);
        }
        return saved;
    }

    /**
     * 更新外部工具配置
     */
    public ExternalToolConfig updateExternalTool(Long id, ExternalToolConfig config) {
        ExternalToolConfig existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("工具不存在: " + id));

        // 如果工具名称变了，需要重新注册
        boolean nameChanged = !existing.getToolName().equals(config.getToolName());
        if (nameChanged && repository.existsByToolName(config.getToolName())) {
            throw new IllegalArgumentException("工具名称已存在: " + config.getToolName());
        }

        // 注销旧的
        toolRegistry.unregister(existing.getToolName());

        existing.setToolName(config.getToolName());
        existing.setDisplayName(config.getDisplayName());
        existing.setDescription(config.getDescription());
        existing.setCategory(config.getCategory());
        existing.setApiUrl(config.getApiUrl());
        existing.setHttpMethod(config.getHttpMethod());
        existing.setHeaders(config.getHeaders());
        existing.setParameters(config.getParameters());
        existing.setResponseExtractor(config.getResponseExtractor());
        existing.setEnabled(config.getEnabled());
        existing.setIcon(config.getIcon());

        ExternalToolConfig saved = repository.save(existing);

        // 重新注册
        if (Boolean.TRUE.equals(saved.getEnabled())) {
            registerExternalTool(saved);
        }
        return saved;
    }

    /**
     * 删除外部工具
     */
    public void deleteExternalTool(Long id) {
        ExternalToolConfig config = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("工具不存在: " + id));
        toolRegistry.unregister(config.getToolName());
        repository.deleteById(id);
        log.info("外部工具已删除: {}", config.getToolName());
    }

    /**
     * 获取所有外部工具配置
     */
    public List<ExternalToolConfig> getAllExternalTools() {
        return repository.findAll();
    }

    /**
     * 根据ID获取外部工具配置
     */
    public Optional<ExternalToolConfig> getExternalToolById(Long id) {
        return repository.findById(id);
    }

    /**
     * 将外部工具配置注册到 ToolRegistry
     */
    public void registerExternalTool(ExternalToolConfig config) {
        ToolDefinition definition = buildToolDefinition(config);

        ToolRegistry.ToolHandler handler = request -> {
            try {
                return executeExternalApi(config, request);
            } catch (Exception e) {
                log.error("外部工具执行失败: {}", config.getToolName(), e);
                return ToolExecutionResult.error("外部工具执行失败: " + e.getMessage());
            }
        };

        toolRegistry.register(definition, handler);
        log.info("外部工具已注册: {} -> {}", config.getToolName(), config.getApiUrl());
    }

    /**
     * 构建 ToolDefinition
     */
    private ToolDefinition buildToolDefinition(ExternalToolConfig config) {
        ToolDefinition definition = new ToolDefinition();
        definition.setName(config.getToolName());
        definition.setDescription(config.getDescription());
        definition.setCategory(config.getCategory());
        definition.setReturnType("Object");
        definition.setAsync(false);

        Map<String, Object> meta = new HashMap<>();
        meta.put("groupName", config.getDisplayName());
        meta.put("source", config.getSource());
        meta.put("apiUrl", config.getApiUrl());
        meta.put("httpMethod", config.getHttpMethod());
        meta.put("icon", config.getIcon());
        definition.setMetadata(meta);

        // 解析参数定义
        List<ToolDefinition.ParameterDefinition> params = parseParameters(config.getParameters());
        definition.setParameters(params);

        return definition;
    }

    /**
     * 解析参数JSON
     */
    private List<ToolDefinition.ParameterDefinition> parseParameters(String parametersJson) {
        if (parametersJson == null || parametersJson.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(parametersJson, new TypeReference<List<ToolDefinition.ParameterDefinition>>() {});
        } catch (Exception e) {
            log.warn("解析参数定义失败: {}", parametersJson, e);
            return new ArrayList<>();
        }
    }

    /**
     * 执行外部API调用
     */
    private ToolExecutionResult executeExternalApi(ExternalToolConfig config, ToolExecutionRequest request) {
        String apiUrl = config.getApiUrl();
        String httpMethod = config.getHttpMethod();
        Map<String, Object> params = request.getParameters();

        // 构建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (config.getHeaders() != null && !config.getHeaders().isEmpty()) {
            Map<String, String> headerMap = parseHeaders(config.getHeaders());
            headerMap.forEach(headers::set);
        }

        // 替换URL中的模板变量
        apiUrl = replaceTemplateVariables(apiUrl, params);

        HttpEntity<Object> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response;
        try {
            if ("GET".equalsIgnoreCase(httpMethod)) {
                UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(apiUrl);
                // 将参数添加到URL查询串
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    if (entry.getValue() != null) {
                        builder.queryParam(entry.getKey(), entry.getValue());
                    }
                }
                URI uri = builder.build().encode(StandardCharsets.UTF_8).toUri();
                response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
            } else {
                // POST / PUT / DELETE
                Object body = buildRequestBody(params, config.getParameters());
                entity = new HttpEntity<>(body, headers);
                HttpMethod method = switch (httpMethod.toUpperCase()) {
                    case "POST" -> HttpMethod.POST;
                    case "PUT" -> HttpMethod.PUT;
                    case "DELETE" -> HttpMethod.DELETE;
                    case "PATCH" -> HttpMethod.PATCH;
                    default -> HttpMethod.POST;
                };
                response = restTemplate.exchange(apiUrl, method, entity, String.class);
            }

            if (response.getStatusCode().is2xxSuccessful()) {
                String body = response.getBody();
                Object extracted = extractResponse(body, config.getResponseExtractor());
                return ToolExecutionResult.success("外部API调用成功", extracted != null ? extracted : body);
            } else {
                return ToolExecutionResult.error("外部API调用失败: HTTP " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("调用外部API失败: {}", apiUrl, e);
            return ToolExecutionResult.error("调用外部API失败: " + e.getMessage());
        }
    }

    /**
     * 替换URL模板变量，如 {city} -> 实际值
     */
    private String replaceTemplateVariables(String url, Map<String, Object> params) {
        String result = url;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            if (result.contains(placeholder)) {
                result = result.replace(placeholder, String.valueOf(entry.getValue()));
            }
        }
        return result;
    }

    /**
     * 构建请求体
     */
    private Object buildRequestBody(Map<String, Object> params, String parametersDefJson) {
        if (parametersDefJson != null && !parametersDefJson.isEmpty()) {
            try {
                List<ToolDefinition.ParameterDefinition> defs = parseParameters(parametersDefJson);
                ObjectNode bodyNode = objectMapper.createObjectNode();
                for (ToolDefinition.ParameterDefinition def : defs) {
                    Object value = params.get(def.getName());
                    if (value != null) {
                        bodyNode.set(def.getName(), objectMapper.valueToTree(value));
                    }
                }
                return bodyNode;
            } catch (Exception e) {
                log.warn("构建请求体失败，使用原始参数", e);
            }
        }
        return params;
    }

    /**
     * 解析请求头
     */
    private Map<String, String> parseHeaders(String headersJson) {
        if (headersJson == null || headersJson.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(headersJson, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            log.warn("解析请求头失败: {}", headersJson, e);
            return new HashMap<>();
        }
    }

    /**
     * 提取响应数据
     */
    private Object extractResponse(String responseBody, String extractor) {
        if (extractor == null || extractor.isEmpty() || responseBody == null) {
            try {
                // 尝试解析为JSON
                return objectMapper.readValue(responseBody, Object.class);
            } catch (Exception e) {
                return responseBody;
            }
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            // 支持简单的字段路径，如 "data.result"
            String[] paths = extractor.split("\\.");
            JsonNode current = root;
            for (String path : paths) {
                if (current != null && current.isObject()) {
                    current = current.get(path);
                } else {
                    current = null;
                    break;
                }
            }
            if (current != null) {
                return objectMapper.treeToValue(current, Object.class);
            }
            return objectMapper.readValue(responseBody, Object.class);
        } catch (Exception e) {
            log.warn("响应提取失败，返回原始数据");
            return responseBody;
        }
    }
}

