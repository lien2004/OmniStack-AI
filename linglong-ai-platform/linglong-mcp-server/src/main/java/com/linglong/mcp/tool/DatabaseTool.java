package com.linglong.mcp.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linglong.mcp.annotation.MCPTool;
import com.linglong.mcp.model.ToolExecutionRequest;
import com.linglong.mcp.model.ToolExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.*;

/**
 * 数据库操作工具
 * 支持执行SQL查询、更新，以及查看表结构等操作
 */
@Component
public class DatabaseTool {

    private static final Logger log = LoggerFactory.getLogger(DatabaseTool.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${db.default.url:jdbc:postgresql://8.137.117.129:5432/linglong}")
    private String defaultJdbcUrl;

    @Value("${db.default.username:postgres}")
    private String defaultUsername;

    @Value("${db.default.password:20041212}")
    private String defaultPassword;

    /**
     * 执行SQL查询（SELECT），返回JSON格式结果
     */
    @MCPTool(
            name = "db_query",
            description = "执行SQL查询语句（SELECT），返回结果集（JSON格式）",
            category = "database"
    )
    public ToolExecutionResult dbQuery(ToolExecutionRequest request) {
        Map<String, Object> params = request.getParameters();
        String sql = (String) params.get("sql");
        if (sql == null || sql.isBlank()) {
            return ToolExecutionResult.error("sql参数不能为空");
        }

        String jdbcUrl  = getOrDefault(params, "jdbcUrl",   defaultJdbcUrl);
        String username = getOrDefault(params, "username",  defaultUsername);
        String password = getOrDefault(params, "password",  defaultPassword);

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             Statement stmt = conn.createStatement()) {

            stmt.setMaxRows(200);
            ResultSet rs = stmt.executeQuery(sql);
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();

            List<Map<String, Object>> rows = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= colCount; i++) {
                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                }
                rows.add(row);
            }

            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rows);
            log.info("db_query 执行成功, SQL: {}, 返回 {} 行", sql, rows.size());

            return ToolExecutionResult.success(json, Map.of(
                    "rowCount", rows.size(),
                    "sql", sql
            ));

        } catch (Exception e) {
            log.error("db_query 执行失败: {}", sql, e);
            return ToolExecutionResult.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 执行SQL更新/插入/删除语句，返回影响行数
     */
    @MCPTool(
            name = "db_execute",
            description = "执行SQL更新/插入/删除语句（INSERT/UPDATE/DELETE/DDL），返回影响行数",
            category = "database"
    )
    public ToolExecutionResult dbExecute(ToolExecutionRequest request) {
        Map<String, Object> params = request.getParameters();
        String sql = (String) params.get("sql");
        if (sql == null || sql.isBlank()) {
            return ToolExecutionResult.error("sql参数不能为空");
        }

        String jdbcUrl  = getOrDefault(params, "jdbcUrl",   defaultJdbcUrl);
        String username = getOrDefault(params, "username",  defaultUsername);
        String password = getOrDefault(params, "password",  defaultPassword);

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             Statement stmt = conn.createStatement()) {

            int affected = stmt.executeUpdate(sql);
            log.info("db_execute 执行成功, SQL: {}, 影响 {} 行", sql, affected);

            return ToolExecutionResult.success(
                    "执行成功，影响 " + affected + " 行",
                    Map.of("affectedRows", affected, "sql", sql)
            );

        } catch (Exception e) {
            log.error("db_execute 执行失败: {}", sql, e);
            return ToolExecutionResult.error("执行失败: " + e.getMessage());
        }
    }

    /**
     * 列出数据库中所有表
     */
    @MCPTool(
            name = "db_list_tables",
            description = "列出数据库（Schema）中所有表名及注释",
            category = "database"
    )
    public ToolExecutionResult dbListTables(ToolExecutionRequest request) {
        Map<String, Object> params = request.getParameters();
        String schema    = getOrDefault(params, "schema",   "public");
        String jdbcUrl   = getOrDefault(params, "jdbcUrl",  defaultJdbcUrl);
        String username  = getOrDefault(params, "username", defaultUsername);
        String password  = getOrDefault(params, "password", defaultPassword);

        String sql = """
                SELECT table_name, table_type
                FROM information_schema.tables
                WHERE table_schema = ?
                ORDER BY table_name
                """;

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, schema);
            ResultSet rs = ps.executeQuery();

            List<Map<String, Object>> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(Map.of(
                        "tableName", rs.getString("table_name"),
                        "tableType", rs.getString("table_type")
                ));
            }

            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rows);
            log.info("db_list_tables 完成，schema: {}, 找到 {} 张表", schema, rows.size());

            return ToolExecutionResult.success(json, Map.of("tableCount", rows.size(), "schema", schema));

        } catch (Exception e) {
            log.error("db_list_tables 失败", e);
            return ToolExecutionResult.error("获取表列表失败: " + e.getMessage());
        }
    }

    /**
     * 描述表结构（列名、类型、是否可空等）
     */
    @MCPTool(
            name = "db_describe_table",
            description = "查看指定表的字段结构（列名、数据类型、是否可空、默认值）",
            category = "database"
    )
    public ToolExecutionResult dbDescribeTable(ToolExecutionRequest request) {
        Map<String, Object> params = request.getParameters();
        String tableName = (String) params.get("tableName");
        if (tableName == null || tableName.isBlank()) {
            return ToolExecutionResult.error("tableName参数不能为空");
        }

        String schema   = getOrDefault(params, "schema",   "public");
        String jdbcUrl  = getOrDefault(params, "jdbcUrl",  defaultJdbcUrl);
        String username = getOrDefault(params, "username", defaultUsername);
        String password = getOrDefault(params, "password", defaultPassword);

        String sql = """
                SELECT column_name, data_type, character_maximum_length,
                       is_nullable, column_default, ordinal_position
                FROM information_schema.columns
                WHERE table_schema = ? AND table_name = ?
                ORDER BY ordinal_position
                """;

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, schema);
            ps.setString(2, tableName);
            ResultSet rs = ps.executeQuery();

            List<Map<String, Object>> columns = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> col = new LinkedHashMap<>();
                col.put("column",     rs.getString("column_name"));
                col.put("type",       rs.getString("data_type"));
                col.put("maxLength",  rs.getObject("character_maximum_length"));
                col.put("nullable",   rs.getString("is_nullable"));
                col.put("default",    rs.getObject("column_default"));
                columns.add(col);
            }

            if (columns.isEmpty()) {
                return ToolExecutionResult.error("表不存在或无权访问: " + tableName);
            }

            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(columns);
            log.info("db_describe_table 完成，表: {}, 共 {} 列", tableName, columns.size());

            return ToolExecutionResult.success(json, Map.of(
                    "tableName",   tableName,
                    "columnCount", columns.size()
            ));

        } catch (Exception e) {
            log.error("db_describe_table 失败: {}", tableName, e);
            return ToolExecutionResult.error("获取表结构失败: " + e.getMessage());
        }
    }

    // -------- 工具方法 --------

    private String getOrDefault(Map<String, Object> params, String key, String defaultValue) {
        Object val = params.get(key);
        return (val instanceof String s && !s.isBlank()) ? s : defaultValue;
    }
}
