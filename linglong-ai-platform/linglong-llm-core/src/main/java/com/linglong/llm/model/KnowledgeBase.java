package com.linglong.llm.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBase {
    private String id;
    private String name;
    private String description;
    private String collectionName;
    private String embeddingModel;
    private Integer dimensions;
    private String owner;
    private Integer documentCount;
    private Integer chunkCount;
    private Long sizeBytes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
