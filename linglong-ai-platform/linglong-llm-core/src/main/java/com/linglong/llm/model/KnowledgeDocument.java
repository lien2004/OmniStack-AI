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
public class KnowledgeDocument {
    private String id;
    private String kbId;
    private String name;
    private String filename;
    private String category;
    private String mimeType;
    private Long sizeBytes;
    private Integer chunkCount;
    private String status;
    private Boolean enabled;
    private String errorMessage;
    private String uploader;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
