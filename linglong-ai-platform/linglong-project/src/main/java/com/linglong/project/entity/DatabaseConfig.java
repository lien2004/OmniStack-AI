package com.linglong.project.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 数据库配置实体类
 */
@Data
@Entity
@Table(name = "database_configs")
public class DatabaseConfig {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "db_type", nullable = false)
    private String dbType;
    
    @Column(name = "host", nullable = false)
    private String host;
    
    @Column(name = "port", nullable = false)
    private Integer port;
    
    @Column(name = "database", nullable = false)
    private String database;
    
    @Column(name = "username", nullable = false)
    private String username;
    
    @Column(name = "password", nullable = false)
    private String password;
    
    @Column(name = "status")
    private String status = "untested";
    
    @Column(name = "enabled")
    private Boolean enabled = true;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
