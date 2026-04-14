package com.linglong.llm.controller;

import com.linglong.llm.service.ChatHistoryService;
import com.linglong.llm.service.MultiModelChatService;
import com.linglong.llm.service.VectorStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能对话控制器
 * 提供支持 RAG、多模型路由、流式输出、MySQL 持久化历史的对话接口
 *
 * <pre>
 * POST /ai/conversation/chat          同步对话（含 RAG + 历史）
 * POST /ai/conversation/stream        SSE 流式对话（含 RAG + 历史）
 * GET  /ai/conversation/history/{id}  获取会话历史
 * GET  /ai/conversation/sessions      获取所有会话列表
 * DELETE /ai/conversation/sessions/{id}  删除会话
 * </pre>
 */
@RestController
@RequestMapping("/ai/conversation")
@ConditionalOnProperty(name = "chat.datasource.url")  // 仅当配置了 MySQL 时激活
public class ConversationController {

    private static final Logger log = LoggerFactory.getLogger(ConversationController.class);

    @Autowired
    private MultiModelChatService multiModelChatService;

    @Autowired
    private ChatHistoryService chatHistoryService;

    @Autowired(required = false)
    private VectorStoreService vectorStoreService;

    /** 对话向量缓存相似度阈値（默认 0.85） */
    @Value("${conversation.cache.similarity-threshold:0.85}")
    private double cacheThreshold;

    // =====================================================================
    // 同步对话
    // =====================================================================

    /**
     * 同步多轮对话（非流式）
     * 请求体示例：
     * <pre>
     * {
     *   "chatId": "abc-123",
     *   "message": "你好",
     *   "model": "glm-4.6",
     *   "useRag": true,
     *   "temperature": 0.7,
     *   "maxTokens": 4000
     * }
     * </pre>
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody ConversationRequest req) {
        log.info("同步对话请求 chatId={} model={} useRag={}", req.getChatId(), req.getModel(), req.isUseRag());

        String chatId  = req.getChatId();
        String message = req.getMessage();
        String model   = req.getModel() != null ? req.getModel() : "glm-4.6";

        // 1. 开启 RAG 时先检查对话向量缓存
        if (req.isUseRag()) {
            Optional<String> cached = tryCacheHit(message);
            if (cached.isPresent()) {
                log.info("对话向量缓存命中，直接返回缓存答案");
                chatHistoryService.saveMessage(chatId, "user", message, model, null);
                chatHistoryService.saveMessage(chatId, "assistant", cached.get(), model, null);
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("chatId", chatId);
                result.put("answer", cached.get());
                result.put("model", model);
                result.put("fromCache", true);
                return ResponseEntity.ok(result);
            }
        }

        // 2. 构建对话上下文（历史 + RAG 知识库文档）
        List<Map<String, Object>> messages = buildMessages(chatId, message, req.isUseRag());

        // 3. 保存用户消息
        chatHistoryService.saveMessage(chatId, "user", message, model, null);

        // 4. 调用 LLM
        String answer = multiModelChatService.chat(messages, model, req.getTemperature(), req.getMaxTokens());

        // 5. 保存 AI 回复
        chatHistoryService.saveMessage(chatId, "assistant", answer, model, null);

        // 6. 开启 RAG 时异步将问答对写入向量缓存
        if (req.isUseRag()) {
            saveToCacheAsync(message, answer, model);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("chatId", chatId);
        result.put("answer", answer);
        result.put("model", model);
        return ResponseEntity.ok(result);
    }

    // =====================================================================
    // SSE 流式对话
    // =====================================================================

    /**
     * SSE 流式对话
     * 前端使用 fetch + ReadableStream 消费 text/event-stream
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestBody ConversationRequest req) {
        log.info("流式对话请求 chatId={} model={} useRag={}", req.getChatId(), req.getModel(), req.isUseRag());

        String chatId  = req.getChatId();
        String message = req.getMessage();
        String model   = req.getModel() != null ? req.getModel() : "glm-4.6";
        SseEmitter emitter = new SseEmitter(180_000L);

        // 1. 开启 RAG 时先检查对话向量缓存
        if (req.isUseRag()) {
            Optional<String> cached = tryCacheHit(message);
            if (cached.isPresent()) {
                log.info("对话向量缓存命中，流式返回缓存答案");
                chatHistoryService.saveMessage(chatId, "user", message, model, null);
                chatHistoryService.saveMessage(chatId, "assistant", cached.get(), model, null);
                final String cachedAnswer = cached.get();
                new Thread(() -> {
                    try {
                        // 分块流式输出缓存答案（每50字一块）
                        int chunkSize = 50;
                        for (int i = 0; i < cachedAnswer.length(); i += chunkSize) {
                            String chunk = cachedAnswer.substring(i, Math.min(i + chunkSize, cachedAnswer.length()));
                            emitter.send(SseEmitter.event().data(chunk));
                        }
                        emitter.send(SseEmitter.event().data("[DONE]"));
                        emitter.complete();
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                }).start();
                return emitter;
            }
        }

        // 2. 构建对话上下文（历史 + RAG 知识库文档）
        List<Map<String, Object>> messages = buildMessages(chatId, message, req.isUseRag());

        // 3. 保存用户消息
        chatHistoryService.saveMessage(chatId, "user", message, model, null);

        // 4. 流式调用 LLM，完成后保存回复并写入向量缓存
        final boolean saveCache = req.isUseRag();
        multiModelChatService.streamChat(
                messages, model,
                req.getTemperature(), req.getMaxTokens(),
                emitter,
                fullContent -> {
                    chatHistoryService.saveMessage(chatId, "assistant", fullContent, model, null);
                    if (saveCache) {
                        saveToCacheAsync(message, fullContent, model);
                    }
                }
        );

        return emitter;
    }

    // =====================================================================
    // 历史记录管理
    // =====================================================================

    /** 获取指定会话的完整历史记录 */
    @GetMapping("/history/{chatId}")
    public ResponseEntity<List<Map<String, Object>>> getHistory(@PathVariable String chatId) {
        return ResponseEntity.ok(chatHistoryService.getHistory(chatId));
    }

    /** 获取所有会话列表（用于侧边栏展示） */
    @GetMapping("/sessions")
    public ResponseEntity<List<Map<String, Object>>> getSessions() {
        return ResponseEntity.ok(chatHistoryService.getAllSessions());
    }

    /** 删除指定会话 */
    @DeleteMapping("/sessions/{chatId}")
    public ResponseEntity<Void> deleteSession(@PathVariable String chatId) {
        chatHistoryService.deleteSession(chatId);
        return ResponseEntity.ok().build();
    }

    /** 清空所有历史 */
    @DeleteMapping("/sessions")
    public ResponseEntity<Void> clearAll() {
        chatHistoryService.clearAll();
        return ResponseEntity.ok().build();
    }

    // =====================================================================
    // 私有方法
    // =====================================================================

    /**
     * 构建发送给 LLM 的 messages 列表：
     * [system(RAG知识库上下文)] + [历史消息...] + [当前用户消息]
     * 注：RAG 只检索 knowledge/document 类型文档，不包含 conversation 缓存记录
     */
    private List<Map<String, Object>> buildMessages(String chatId, String userMessage, boolean useRag) {
        List<Map<String, Object>> messages = new ArrayList<>();

        // (1) RAG 检索增强（只取 knowledge/document 类型，过滤掉 conversation 缓存）
        if (useRag && vectorStoreService != null) {
            try {
                List<Document> docs = vectorStoreService.similaritySearch(userMessage, 6);
                String ragContext = docs.stream()
                        .filter(doc -> !"conversation".equals(doc.getMetadata().get("type")))
                        .map(Document::getContent)
                        .collect(Collectors.joining("\n\n"));
                if (!ragContext.isBlank()) {
                    String systemPrompt = """
                            你是灵龙AI智能助手，请优先根据以下参考资料回答用户的问题。
                            如果参考资料中没有相关信息，请基于自身知识回答。
                            
                            【参考资料（来自知识库）】
                            %s
                            """.formatted(ragContext);
                    messages.add(Map.of("role", "system", "content", systemPrompt));
                    log.info("RAG 检索到 {} 条知识文档，已注入系统提示", docs.stream()
                            .filter(d -> !"conversation".equals(d.getMetadata().get("type"))).count());
                }
            } catch (Exception e) {
                log.warn("RAG 检索失败，将直接对话: {}", e.getMessage());
            }
        }

        // (2) 对话历史
        if (chatId != null && !chatId.isBlank()) {
            List<Map<String, Object>> history = chatHistoryService.buildContextMessages(chatId);
            messages.addAll(history);
        }

        // (3) 当前用户消息
        messages.add(Map.of("role", "user", "content", userMessage));
        return messages;
    }

    /**
     * 检查对话向量缓存中是否有足够相似的问答记录
     */
    private Optional<String> tryCacheHit(String message) {
        if (vectorStoreService == null) return Optional.empty();
        try {
            List<Document> candidates = vectorStoreService.similaritySearch(message, 5);
            return candidates.stream()
                    .filter(doc -> "conversation".equals(doc.getMetadata().get("type")))
                    .filter(doc -> {
                        Object distObj = doc.getMetadata().get("distance");
                        double dist = distObj instanceof Number ? ((Number) distObj).doubleValue() : 0.0;
                        double similarity = 1.0 - dist;
                        log.debug("对话缓存检查 | 相似度: {} | 阈値: {}", similarity, cacheThreshold);
                        return similarity >= cacheThreshold;
                    })
                    .map(doc -> (String) doc.getMetadata().get("answer"))
                    .filter(a -> a != null && !a.isBlank())
                    .findFirst();
        } catch (Exception e) {
            log.warn("对话向量缓存检查失败: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 异步将问答对写入向量缓存
     */
    private void saveToCacheAsync(String message, String answer, String model) {
        if (vectorStoreService == null) return;
        new Thread(() -> {
            try {
                Map<String, Object> meta = new HashMap<>();
                meta.put("type", "conversation");
                meta.put("question", message);
                meta.put("answer", answer);
                meta.put("model", model);
                meta.put("createdAt", String.valueOf(System.currentTimeMillis()));
                vectorStoreService.addDocument(message, meta);
                log.info("问答对已异步写入向量缓存");
            } catch (Exception e) {
                log.warn("问答对写入向量缓存失败: {}", e.getMessage());
            }
        }, "cache-writer").start();
    }

    // =====================================================================
    // 请求体 DTO
    // =====================================================================

    public static class ConversationRequest {
        private String chatId;
        private String message;
        private String model;
        private boolean useRag = false;
        private Double temperature = 0.7;
        private Integer maxTokens = 4000;

        public String getChatId() { return chatId; }
        public void setChatId(String chatId) { this.chatId = chatId; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public boolean isUseRag() { return useRag; }
        public void setUseRag(boolean useRag) { this.useRag = useRag; }
        public Double getTemperature() { return temperature; }
        public void setTemperature(Double temperature) { this.temperature = temperature; }
        public Integer getMaxTokens() { return maxTokens; }
        public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
    }
}
