package com.linglong.project.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * LLM厂商配置实体类
 */
@Data
@Entity
@Table(name = "llm_providers")
public class LlmProvider {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "provider", nullable = false)
    private String provider;
    
    @Column(name = "provider_name")
    private String providerName;
    
    @Column(name = "model", nullable = false)
    private String model;
    
    @Column(name = "api_key", nullable = false)
    private String apiKey;
    
    @Column(name = "base_url")
    private String baseUrl;
    
    @Column(name = "temperature")
    private Double temperature = 0.7;
    
    @Column(name = "max_tokens")
    private Integer maxTokens = 4096;
    
    @Column(name = "is_default")
    private Boolean isDefault = false;
    
    @Column(name = "enabled")
    private Boolean enabled = true;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
