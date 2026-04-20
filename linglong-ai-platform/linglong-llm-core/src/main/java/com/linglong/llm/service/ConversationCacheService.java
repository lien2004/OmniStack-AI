package com.linglong.llm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 对话向量缓存服务
 * 优先从向量数据库检索历史问答缓存，命中则直接返回；
 * 未命中时调用大模型，并将问答对存入向量数据库供后续复用。
 */
@Service
public class ConversationCacheService {

    private static final Logger log = LoggerFactory.getLogger(ConversationCacheService.class);

    /** 相似度阈值，超过此值视为命中缓存 */
    @Value("${conversation.cache.similarity-threshold:0.85}")
    private double similarityThreshold;

    /** 默认模型 */
    @Value("${zhipu.model:glm-4.6}")
    private String defaultModel;

    private final VectorStoreService vectorStoreService;
    private final ZhipuChatService zhipuChatService;

    public ConversationCacheService(VectorStoreService vectorStoreService,
                                    ZhipuChatService zhipuChatService) {
        this.vectorStoreService = vectorStoreService;
        this.zhipuChatService = zhipuChatService;
    }

    /**
     * 带向量缓存的对话（使用默认模型）
     *
     * @param message 用户输入
     * @return AI回复
     */
    public String chat(String message) {
        return chat(message, defaultModel, 0.7, 2000);
    }

    /**
     * 带向量缓存的对话（可指定模型参数）
     *
     * <ol>
     *   <li>先以相似度阈值检索向量数据库，查找历史相似问答</li>
     *   <li>命中 → 直接返回缓存的回答，不消耗 Token</li>
     *   <li>未命中 → 调用大模型，将问答对写入向量数据库后返回</li>
     * </ol>
     *
     * @param message     用户输入
     * @param model       模型名称
     * @param temperature 温度参数
     * @param maxTokens   最大 Token 数
     * @return AI回复
     */
    public String chat(String message, String model, Double temperature, Integer maxTokens) {
        // 1. 尝试从向量库检索相似问答（embedding 服务不可用时自动降级）
        try {
            // 先获取topK结果，然后手动计算相似度进行过滤
            // PgVectorStore使用余弦距离，需要转换为相似度：similarity = 1 - distance
            List<Document> candidates = vectorStoreService.similaritySearch(message, 5);

            // 2. 筛选 type=conversation 且相似度达标的缓存记录
            log.debug("对话缓存服务 | 检索到 {} 条候选记录", candidates.size());

            Optional<Document> hit = candidates.stream()
                    .filter(doc -> {
                        String type = (String) doc.getMetadata().get("type");
                        boolean isConversation = "conversation".equals(type);
                        if (!isConversation) {
                            log.debug("对话缓存服务 | 过滤非对话类型: type={}", type);
                        }
                        return isConversation;
                    })
                    .filter(doc -> {
                        // 从metadata中获取distance，转换为相似度
                        Object distanceObj = doc.getMetadata().get("distance");
                        double distance = distanceObj instanceof Number ? ((Number) distanceObj).doubleValue() : 1.0;
                        // 余弦相似度 = 1 - 余弦距离
                        double similarity = 1.0 - distance;
                        boolean isHit = similarity >= similarityThreshold;
                        log.info("对话缓存服务 | 问题: {} | 相似度: {:.4f} | 阈值: {} | 命中: {}",
                                doc.getMetadata().get("question"), similarity, similarityThreshold, isHit);
                        return isHit;
                    })
                    .findFirst();

            if (hit.isPresent()) {
                String cachedAnswer = (String) hit.get().getMetadata().get("answer");
                if (cachedAnswer != null && !cachedAnswer.isBlank()) {
                    log.info("向量缓存命中，直接返回，问题: {}", message);
                    return cachedAnswer;
                }
            }
        } catch (Exception e) {
            log.warn("向量缓存检索失败（embedding 服务不可用），降级直接调用大模型。原因: {}", e.getMessage());
        }

        // 3. 缓存未命中，调用大模型
        log.info("向量缓存未命中，调用大模型，问题: {}", message);
        String answer = zhipuChatService.chat(message, model, temperature, maxTokens);

        // 4. 尝试将问答对写入向量数据库（embedding 服务不可用时跳过，不影响响应）
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("type", "conversation");
            metadata.put("question", message);
            metadata.put("answer", answer);
            metadata.put("model", model);
            metadata.put("createdAt", String.valueOf(System.currentTimeMillis()));
            // 将问题和答案组合存储，便于向量检索时匹配
            String content = "问题：" + message + "\n\n答案：" + answer;
            vectorStoreService.addDocument(content, metadata);
            log.info("问答对已写入向量数据库，问题: {}", message);
        } catch (Exception e) {
            log.warn("问答对写入向量数据库失败（embedding 服务不可用），已跳过。原因: {}", e.getMessage());
        }

        return answer;
    }
}
