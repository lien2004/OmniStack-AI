package com.linglong.llm.controller;

import com.linglong.llm.service.RagService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RAG智能问答控制器
 * 基于向量数据库知识进行增强生成
 */
@RestController
@RequestMapping("/rag")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    /**
     * RAG问答
     * GET /rag/chat?question=如何登录系统&topK=3
     */
    @GetMapping("/chat")
    public Map<String, Object> chat(
            @RequestParam(value = "question") String question,
            @RequestParam(value = "topK", defaultValue = "3") int topK) {

        String answer = ragService.chat(question, topK);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("question", question);
        result.put("answer", answer);
        return result;
    }

    /**
     * 按项目RAG问答
     * GET /rag/chat/project?question=如何登录系统&projectId=1&topK=3
     */
    @GetMapping("/chat/project")
    public Map<String, Object> chatByProject(
            @RequestParam(value = "question") String question,
            @RequestParam(value = "projectId") String projectId,
            @RequestParam(value = "topK", defaultValue = "3") int topK) {

        String answer = ragService.chatByProject(question, projectId, topK);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("question", question);
        result.put("projectId", projectId);
        result.put("answer", answer);
        return result;
    }

    /**
     * RAG问答（带引用来源）
     * GET /rag/chat/sources?question=如何登录系统&topK=3
     */
    @GetMapping("/chat/sources")
    public Map<String, Object> chatWithSources(
            @RequestParam(value = "question") String question,
            @RequestParam(value = "topK", defaultValue = "3") int topK) {

        RagService.RagResult ragResult = ragService.chatWithSources(question, topK);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("question", question);
        result.put("answer", ragResult.getAnswer());
        result.put("sources", ragResult.getSources().stream()
                .map(s -> {
                    Map<String, Object> source = new LinkedHashMap<>();
                    source.put("id", s.getId());
                    source.put("content", s.getContent());
                    source.put("type", s.getType());
                    source.put("distance", s.getDistance());
                    return source;
                })
                .collect(Collectors.toList()));
        return result;
    }
}
