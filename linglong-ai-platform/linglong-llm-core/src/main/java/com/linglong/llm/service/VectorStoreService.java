package com.linglong.llm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 向量存储服务
 * 提供文档嵌入、语义检索等功能
 */
@Service
public class VectorStoreService {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreService.class);

    private final VectorStore vectorStore;
    private final EmbeddingClient embeddingClient;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public VectorStoreService(VectorStore vectorStore, EmbeddingClient embeddingClient,
                              @Qualifier("pgJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.vectorStore = vectorStore;
        this.embeddingClient = embeddingClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 添加文档到向量存储
     *
     * @param content  文档内容
     * @param metadata 文档元数据
     */
    public void addDocument(String content, Map<String, Object> metadata) {
        Document document = new Document(content, metadata);
        vectorStore.add(List.of(document));
        log.info("文档已添加到向量存储, metadata: {}", metadata);
    }

    /**
     * 批量添加文档
     *
     * @param documents 文档列表
     */
    public void addDocuments(List<Document> documents) {
        vectorStore.add(documents);
        log.info("批量添加 {} 个文档到向量存储", documents.size());
    }

    /**
     * 添加带ID的文档
     *
     * @param id       文档ID
     * @param content  文档内容
     * @param metadata 文档元数据
     */
    public void addDocument(String id, String content, Map<String, Object> metadata) {
        Document document = new Document(id, content, metadata);
        vectorStore.add(List.of(document));
        log.info("文档已添加, id: {}", id);
    }

    /**
     * 语义相似度搜索
     *
     * @param query  查询文本
     * @param topK   返回结果数量
     * @return 相似文档列表（包含 distance 元数据）
     */
    public List<Document> similaritySearch(String query, int topK) {
        SearchRequest request = SearchRequest.query(query)
                .withTopK(topK);
        List<Document> results = vectorStore.similaritySearch(request);

        // PgVectorStore 不自动返回 distance，需要手动查询获取
        // 使用 cosine_similarity_search 函数获取距离信息
        results = enrichWithDistance(query, results);

        log.info("语义搜索完成, 查询: {}, 返回 {} 条结果", query, results.size());
        return results;
    }

    /**
     * 使用 JDBC 查询获取文档的 distance 信息并 enrich 到 metadata
     * 使用 PostgreSQL 的 <=> 操作符计算余弦距离
     */
    private List<Document> enrichWithDistance(String query, List<Document> documents) {
        if (documents.isEmpty() || jdbcTemplate == null) {
            return documents;
        }

        try {
            // 获取查询文本的 embedding
            List<List<Double>> embeddings = embeddingClient.embed(List.of(query));
            if (embeddings.isEmpty()) {
                return documents;
            }
            List<Double> queryEmbedding = embeddings.get(0);

            // 构建向量字符串
            StringBuilder vectorStr = new StringBuilder("[");
            for (int i = 0; i < queryEmbedding.size(); i++) {
                if (i > 0) vectorStr.append(",");
                vectorStr.append(queryEmbedding.get(i));
            }
            vectorStr.append("]");

            // 查询每个文档的 distance
            for (Document doc : documents) {
                try {
                    String docId = doc.getId();
                    // 使用 SQL 查询计算余弦距离
                    String sql = "SELECT embedding <=> ?::vector as distance FROM vector_store WHERE id = ?::uuid";
                    Double distance = jdbcTemplate.queryForObject(sql, Double.class, vectorStr.toString(), docId);
                    if (distance != null) {
                        doc.getMetadata().put("distance", distance);
                        log.debug("文档 {} 的 distance: {}", docId, distance);
                    }
                } catch (Exception e) {
                    log.warn("获取文档 {} 的 distance 失败: {}", doc.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("获取 distance 信息失败: {}", e.getMessage());
        }
        return documents;
    }

    /**
     * 带阈值过滤的语义搜索
     *
     * @param query      查询文本
     * @param topK       返回结果数量
     * @param threshold  相似度阈值 (0-1)
     * @return 相似文档列表
     */
    public List<Document> similaritySearchWithThreshold(String query, int topK, double threshold) {
        SearchRequest request = SearchRequest.query(query)
                .withTopK(topK)
                .withSimilarityThreshold(threshold);
        List<Document> results = vectorStore.similaritySearch(request);
        log.info("带阈值语义搜索完成, 查询: {}, 阈值: {}, 返回 {} 条结果", query, threshold, results.size());
        return results;
    }

    /**
     * 带过滤条件的语义搜索
     *
     * @param query          查询文本
     * @param topK           返回结果数量
     * @param filterExpression 过滤表达式
     * @return 相似文档列表
     */
    public List<Document> similaritySearchWithFilter(String query, int topK, String filterExpression) {
        SearchRequest request = SearchRequest.query(query)
                .withTopK(topK)
                .withFilterExpression(filterExpression);
        List<Document> results = vectorStore.similaritySearch(request);
        log.info("带过滤条件语义搜索完成, 查询: {}, 过滤: {}, 返回 {} 条结果", query, filterExpression, results.size());
        return results;
    }

    /**
     * 删除指定ID的文档
     *
     * @param documentIds 文档ID列表
     */
    public void deleteDocuments(List<String> documentIds) {
        vectorStore.delete(documentIds);
        log.info("已删除 {} 个文档", documentIds.size());
    }

    /**
     * 创建代码片段文档
     *
     * @param codeContent 代码内容
     * @param fileName    文件名
     * @param projectId   项目ID
     * @return 文档对象
     */
    public Document createCodeDocument(String codeContent, String fileName, String projectId) {
        return new Document(
                codeContent,
                Map.of(
                        "type", "code",
                        "fileName", fileName,
                        "projectId", projectId
                )
        );
    }

    /**
     * 创建需求文档
     *
     * @param requirement 需求内容
     * @param title       需求标题
     * @param projectId   项目ID
     * @return 文档对象
     */
    public Document createRequirementDocument(String requirement, String title, String projectId) {
        return new Document(
                requirement,
                Map.of(
                        "type", "requirement",
                        "title", title,
                        "projectId", projectId
                )
        );
    }

    /**
     * 创建架构文档
     *
     * @param architecture 架构描述
     * @param diagramName  架构图名称
     * @param projectId    项目ID
     * @return 文档对象
     */
    public Document createArchitectureDocument(String architecture, String diagramName, String projectId) {
        return new Document(
                architecture,
                Map.of(
                        "type", "architecture",
                        "diagramName", diagramName,
                        "projectId", projectId
                )
        );
    }

    /**
     * 按项目ID搜索代码片段
     *
     * @param query    查询内容
     * @param projectId 项目ID
     * @param topK     返回数量
     * @return 相似文档列表
     */
    public List<Document> searchCodeByProject(String query, String projectId, int topK) {
        return similaritySearchWithFilter(
                query,
                topK,
                String.format("projectId == '%s' && type == 'code'", projectId)
        );
    }

    /**
     * 搜索需求文档
     *
     * @param query    查询内容
     * @param projectId 项目ID
     * @param topK     返回数量
     * @return 相似文档列表
     */
    public List<Document> searchRequirements(String query, String projectId, int topK) {
        return similaritySearchWithFilter(
                query,
                topK,
                String.format("projectId == '%s' && type == 'requirement'", projectId)
        );
    }
}
