package com.linglong.llm.controller;

import com.linglong.llm.service.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RAG智能问答控制器
 * 基于向量数据库知识进行增强生成
 */
@Tag(name = "RAG 问答", description = "基于向量知识库的增强检索生成问答，支持按项目检索和引用来源")
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
    @Operation(summary = "RAG 问答（基础）", description = "检索知识库并出回答")
    @GetMapping("/chat")
    public Map<String, Object> chat(
            @Parameter(description = "用户问题", required = true) @RequestParam(value = "question") String question,
            @Parameter(description = "检索知识库条数") @RequestParam(value = "topK", defaultValue = "3") int topK) {

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
    @Operation(summary = "按项目 RAG 问答", description = "限定项目范围内的知识检索问答")
    @GetMapping("/chat/project")
    public Map<String, Object> chatByProject(
            @Parameter(description = "用户问题", required = true) @RequestParam(value = "question") String question,
            @Parameter(description = "项目 ID", required = true) @RequestParam(value = "projectId") String projectId,
            @Parameter(description = "检索条数") @RequestParam(value = "topK", defaultValue = "3") int topK) {

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
    @Operation(summary = "RAG 问答（含引用来源）", description = "返回回答的同时附上匹配的知识库来源条目")
    @GetMapping("/chat/sources")
    public Map<String, Object> chatWithSources(
            @Parameter(description = "用户问题", required = true) @RequestParam(value = "question") String question,
            @Parameter(description = "检索条数") @RequestParam(value = "topK", defaultValue = "3") int topK) {

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
