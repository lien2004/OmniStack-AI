package com.linglong.agent.service;

import com.linglong.agent.model.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 项目数据访问层
 * 使用JdbcTemplate操作PostgreSQL
 */
@Repository
public class ProjectRepository {

    private static final Logger log = LoggerFactory.getLogger(ProjectRepository.class);

    private final JdbcTemplate jdbcTemplate;

    public ProjectRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 自动建表（启动时执行）
     */
    @PostConstruct
    public void initTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS projects (
                id VARCHAR(64) PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                description TEXT,
                status VARCHAR(32) DEFAULT 'init',
                tech_stack VARCHAR(255),
                architecture_type VARCHAR(128),
                model VARCHAR(128),
                requirement TEXT,
                workflow_id VARCHAR(64),
                user_id VARCHAR(64),
                code_files TEXT,
                workflow_output TEXT,
                create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;
        try {
            jdbcTemplate.execute(sql);
            log.info("项目表初始化完成");
        } catch (Exception e) {
            log.error("项目表初始化失败", e);
        }
    }

    private final RowMapper<Project> rowMapper = (rs, rowNum) -> Project.builder()
            .id(rs.getString("id"))
            .name(rs.getString("name"))
            .description(rs.getString("description"))
            .status(rs.getString("status"))
            .techStack(rs.getString("tech_stack"))
            .architectureType(rs.getString("architecture_type"))
            .model(rs.getString("model"))
            .requirement(rs.getString("requirement"))
            .workflowId(rs.getString("workflow_id"))
            .userId(rs.getString("user_id"))
            .codeFiles(rs.getString("code_files"))
            .workflowOutput(rs.getString("workflow_output"))
            .createTime(rs.getTimestamp("create_time") != null ? rs.getTimestamp("create_time").toLocalDateTime() : null)
            .updateTime(rs.getTimestamp("update_time") != null ? rs.getTimestamp("update_time").toLocalDateTime() : null)
            .build();

    /**
     * 保存项目
     */
    public Project save(Project project) {
        String sql = """
            INSERT INTO projects (id, name, description, status, tech_stack, architecture_type, 
                model, requirement, workflow_id, user_id, code_files, workflow_output, create_time, update_time)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                name = EXCLUDED.name,
                description = EXCLUDED.description,
                status = EXCLUDED.status,
                tech_stack = EXCLUDED.tech_stack,
                architecture_type = EXCLUDED.architecture_type,
                model = EXCLUDED.model,
                requirement = EXCLUDED.requirement,
                workflow_id = EXCLUDED.workflow_id,
                code_files = EXCLUDED.code_files,
                workflow_output = EXCLUDED.workflow_output,
                update_time = EXCLUDED.update_time
            """;
        
        LocalDateTime now = LocalDateTime.now();
        if (project.getCreateTime() == null) {
            project.setCreateTime(now);
        }
        project.setUpdateTime(now);

        jdbcTemplate.update(sql,
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getStatus(),
                project.getTechStack(),
                project.getArchitectureType(),
                project.getModel(),
                project.getRequirement(),
                project.getWorkflowId(),
                project.getUserId(),
                project.getCodeFiles(),
                project.getWorkflowOutput(),
                project.getCreateTime(),
                project.getUpdateTime()
        );
        return project;
    }

    /**
     * 根据ID查询项目
     */
    public Optional<Project> findById(String id) {
        String sql = "SELECT * FROM projects WHERE id = ?";
        List<Project> list = jdbcTemplate.query(sql, rowMapper, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /**
     * 查询所有项目（按更新时间降序）
     */
    public List<Project> findAll() {
        String sql = "SELECT * FROM projects ORDER BY update_time DESC";
        return jdbcTemplate.query(sql, rowMapper);
    }

    /**
     * 按用户ID查询项目
     */
    public List<Project> findByUserId(String userId) {
        String sql = "SELECT * FROM projects WHERE user_id = ? ORDER BY update_time DESC";
        return jdbcTemplate.query(sql, rowMapper, userId);
    }

    /**
     * 按状态查询项目
     */
    public List<Project> findByStatus(String status) {
        String sql = "SELECT * FROM projects WHERE status = ? ORDER BY update_time DESC";
        return jdbcTemplate.query(sql, rowMapper, status);
    }

    /**
     * 搜索项目（按名称或描述模糊搜索）
     */
    public List<Project> search(String keyword) {
        String sql = "SELECT * FROM projects WHERE name ILIKE ? OR description ILIKE ? ORDER BY update_time DESC";
        String pattern = "%" + keyword + "%";
        return jdbcTemplate.query(sql, rowMapper, pattern, pattern);
    }

    /**
     * 根据工作流ID查询项目
     */
    public Optional<Project> findByWorkflowId(String workflowId) {
        String sql = "SELECT * FROM projects WHERE workflow_id = ?";
        List<Project> list = jdbcTemplate.query(sql, rowMapper, workflowId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /**
     * 更新项目状态
     */
    public void updateStatus(String id, String status) {
        String sql = "UPDATE projects SET status = ?, update_time = ? WHERE id = ?";
        jdbcTemplate.update(sql, status, LocalDateTime.now(), id);
    }

    /**
     * 更新项目代码文件
     */
    public void updateCodeFiles(String id, String codeFiles) {
        String sql = "UPDATE projects SET code_files = ?, update_time = ? WHERE id = ?";
        jdbcTemplate.update(sql, codeFiles, LocalDateTime.now(), id);
    }

    /**
     * 更新项目工作流输出
     */
    public void updateWorkflowOutput(String id, String workflowOutput) {
        String sql = "UPDATE projects SET workflow_output = ?, update_time = ? WHERE id = ?";
        jdbcTemplate.update(sql, workflowOutput, LocalDateTime.now(), id);
    }

    /**
     * 删除项目
     */
    public void deleteById(String id) {
        String sql = "DELETE FROM projects WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    /**
     * 统计项目数量
     */
    public long count() {
        String sql = "SELECT COUNT(*) FROM projects";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0;
    }
}
