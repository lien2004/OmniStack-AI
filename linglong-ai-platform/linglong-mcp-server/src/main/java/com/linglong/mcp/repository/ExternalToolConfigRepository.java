package com.linglong.mcp.repository;

import com.linglong.mcp.model.ExternalToolConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 外部工具配置仓库
 */
@Repository
public interface ExternalToolConfigRepository extends JpaRepository<ExternalToolConfig, Long> {

    /**
     * 根据工具名称查找
     */
    Optional<ExternalToolConfig> findByToolName(String toolName);

    /**
     * 查找所有启用的工具
     */
    List<ExternalToolConfig> findAllByEnabledTrue();

    /**
     * 根据分类查找
     */
    List<ExternalToolConfig> findAllByCategoryAndEnabledTrue(String category);

    /**
     * 检查工具名称是否已存在
     */
    boolean existsByToolName(String toolName);

    /**
     * 根据工具名称删除
     */
    void deleteByToolName(String toolName);
}
