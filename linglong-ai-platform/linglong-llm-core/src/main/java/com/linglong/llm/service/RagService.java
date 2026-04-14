package com.linglong.llm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG服务（检索增强生成）
 * 将向量数据库中的知识与大模型结合，实现基于项目知识的智能问答
 */
@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    @Value("${zhipu.model:glm-4.6}")
    private String modelName;

    private final VectorStoreService vectorStoreService;
    private final ZhipuChatService zhipuChatService;

    public RagService(VectorStoreService vectorStoreService, ZhipuChatService zhipuChatService) {
        this.vectorStoreService = vectorStoreService;
        this.zhipuChatService = zhipuChatService;
    }

    /**
     * RAG问答：基于向量库知识回答用户问题
     *
     * @param question 用户问题
     * @param topK     检索的相关文档数量
     * @return 大模型生成的回答
     */
    public String chat(String question, int topK) {
        // 1. 从向量库检索相关文档
        List<Document> relevantDocs = vectorStoreService.similaritySearch(question, topK);

        if (relevantDocs.isEmpty()) {
            log.info("未找到相关文档，直接调用大模型回答");
            return zhipuChatService.chat(question);
        }

        // 2. 拼装上下文
        String context = relevantDocs.stream()
                .map(Document::getContent)
                .collect(Collectors.joining("\n\n"));

        // 3. 构造带知识的 Prompt
        String prompt = buildRagPrompt(context, question);
        log.info("RAG检索到 {} 条相关文档，生成增强prompt", relevantDocs.size());

        // 4. 调用智谱AI大模型生成回答
        return zhipuChatService.chat(prompt);
    }

    /**
     * 按项目进行RAG问答
     *
     * @param question  用户问题
     * @param projectId 项目ID
     * @param topK      检索的相关文档数量
     * @return 大模型生成的回答
     */
    public String chatByProject(String question, String projectId, int topK) {
        // 1. 从向量库检索该项目的相关文档
        List<Document> codeDocs = vectorStoreService.searchCodeByProject(question, projectId, topK);
        List<Document> reqDocs = vectorStoreService.searchRequirements(question, projectId, topK);

        // 2. 合并检索结果
        StringBuilder contextBuilder = new StringBuilder();

        if (!reqDocs.isEmpty()) {
            contextBuilder.append("【相关需求文档】\n");
            for (Document doc : reqDocs) {
                contextBuilder.append("- ").append(doc.getContent()).append("\n");
            }
            contextBuilder.append("\n");
        }

        if (!codeDocs.isEmpty()) {
            contextBuilder.append("【相关代码片段】\n");
            for (Document doc : codeDocs) {
                String fileName = (String) doc.getMetadata().getOrDefault("fileName", "未知文件");
                contextBuilder.append("文件: ").append(fileName).append("\n");
                contextBuilder.append(doc.getContent()).append("\n\n");
            }
        }

        String context = contextBuilder.toString();

        if (context.isBlank()) {
            log.info("项目 {} 未找到相关文档，直接调用大模型", projectId);
            return zhipuChatService.chat(question);
        }

        String prompt = buildRagPrompt(context, question);
        log.info("RAG检索到项目 {} 的相关文档，生成增强prompt", projectId);

        return zhipuChatService.chat(prompt);
    }

    /**
     * RAG问答（返回详细结果，包含引用来源）
     *
     * @param question 用户问题
     * @param topK     检索数量
     * @return RAG结果，包含回答和引用来源
     */
    public RagResult chatWithSources(String question, int topK) {
        List<Document> relevantDocs = vectorStoreService.similaritySearch(question, topK);

        String context = relevantDocs.stream()
                .map(Document::getContent)
                .collect(Collectors.joining("\n\n"));

        String answer;
        if (context.isBlank()) {
            answer = zhipuChatService.chat(question);
        } else {
            answer = zhipuChatService.chat(buildRagPrompt(context, question));
        }

        List<Source> sources = relevantDocs.stream()
                .map(doc -> new Source(
                        doc.getId(),
                        doc.getContent().length() > 100
                                ? doc.getContent().substring(0, 100) + "..."
                                : doc.getContent(),
                        (String) doc.getMetadata().getOrDefault("type", "unknown"),
                        doc.getMetadata().containsKey("distance")
                                ? ((Number) doc.getMetadata().get("distance")).doubleValue()
                                : 0.0
                ))
                .collect(Collectors.toList());

        return new RagResult(answer, sources);
    }

    /**
     * 构造RAG Prompt
     */
    private String buildRagPrompt(String context, String question) {
        return String.format("""
                你是灵龙AI平台的智能助手，当前使用的大模型是 %s。
                请根据以下参考资料回答用户的问题。
                如果参考资料中没有相关信息，请基于你的知识回答，并说明这不是来自项目文档。
                如果用户询问你是什么模型，请直接回答：我是 %s 模型。
                
                【参考资料】
                %s
                
                【用户问题】
                %s
                
                请用中文回答，简洁准确：""", modelName, modelName, context, question);
    }

    /**
     * RAG结果
     */
    public static class RagResult {
        private final String answer;
        private final List<Source> sources;

        public RagResult(String answer, List<Source> sources) {
            this.answer = answer;
            this.sources = sources;
        }

        public String getAnswer() { return answer; }
        public List<Source> getSources() { return sources; }
    }

    /**
     * 引用来源
     */
    public static class Source {
        private final String id;
        private final String content;
        private final String type;
        private final double distance;

        public Source(String id, String content, String type, double distance) {
            this.id = id;
            this.content = content;
            this.type = type;
            this.distance = distance;
        }

        public String getId() { return id; }
        public String getContent() { return content; }
        public String getType() { return type; }
        public double getDistance() { return distance; }
    }
}
