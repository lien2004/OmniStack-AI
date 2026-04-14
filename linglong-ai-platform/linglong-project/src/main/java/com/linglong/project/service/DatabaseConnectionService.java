package com.linglong.project.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据库连接服务
 */
@Slf4j
@Service
public class DatabaseConnectionService {

    /**
     * 测试数据库连接
     */
    public Map<String, Object> testConnection(String dbType, String host, Integer port, 
                                               String database, String username, String password) {
        Map<String, Object> result = new HashMap<>();
        String jdbcUrl = buildJdbcUrl(dbType, host, port, database);
        
        try {
            // 加载驱动
            loadDriver(dbType);
            
            // 测试连接
            try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
                DatabaseMetaData metaData = conn.getMetaData();
                
                result.put("success", true);
                result.put("message", "连接成功");
                result.put("databaseProductName", metaData.getDatabaseProductName());
                result.put("databaseProductVersion", metaData.getDatabaseProductVersion());
                result.put("driverName", metaData.getDriverName());
                result.put("driverVersion", metaData.getDriverVersion());
                
                log.info("数据库连接测试成功: {} @ {}", dbType, host);
            }
        } catch (ClassNotFoundException e) {
            result.put("success", false);
            result.put("message", "数据库驱动未找到: " + e.getMessage());
            log.error("数据库驱动未找到: {}", dbType, e);
        } catch (SQLException e) {
            result.put("success", false);
            result.put("message", "连接失败: " + e.getMessage());
            log.error("数据库连接失败: {} @ {}", dbType, host, e);
        }
        
        return result;
    }

    /**
     * 获取数据库信息
     */
    public Map<String, Object> getDatabaseInfo(String dbType, String host, Integer port,
                                                String database, String username, String password) {
        Map<String, Object> result = new HashMap<>();
        String jdbcUrl = buildJdbcUrl(dbType, host, port, database);
        
        try {
            loadDriver(dbType);
            
            try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
                DatabaseMetaData metaData = conn.getMetaData();
                
                result.put("productName", metaData.getDatabaseProductName());
                result.put("productVersion", metaData.getDatabaseProductVersion());
                result.put("driverName", metaData.getDriverName());
                result.put("driverVersion", metaData.getDriverVersion());
                
                // 获取表列表
                List<String> tables = new ArrayList<>();
                try (ResultSet rs = metaData.getTables(database, null, "%", new String[]{"TABLE"})) {
                    while (rs.next()) {
                        tables.add(rs.getString("TABLE_NAME"));
                    }
                }
                result.put("tables", tables);
                result.put("tableCount", tables.size());
                
                log.info("获取数据库信息成功: {} 表数量: {}", database, tables.size());
            }
        } catch (Exception e) {
            result.put("error", e.getMessage());
            log.error("获取数据库信息失败", e);
        }
        
        return result;
    }

    /**
     * 构建JDBC URL
     */
    private String buildJdbcUrl(String dbType, String host, Integer port, String database) {
        switch (dbType.toLowerCase()) {
            case "mysql":
                return String.format("jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false", 
                    host, port, database);
            case "postgresql":
                return String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
            case "mariadb":
                return String.format("jdbc:mariadb://%s:%d/%s", host, port, database);
            case "sqlserver":
                return String.format("jdbc:sqlserver://%s:%d;databaseName=%s", host, port, database);
            case "oracle":
                return String.format("jdbc:oracle:thin:@%s:%d:%s", host, port, database);
            case "sqlite":
                return String.format("jdbc:sqlite:%s", database);
            case "clickhouse":
                return String.format("jdbc:clickhouse://%s:%d/%s", host, port, database);
            default:
                throw new IllegalArgumentException("不支持的数据库类型: " + dbType);
        }
    }

    /**
     * 加载数据库驱动
     */
    private void loadDriver(String dbType) throws ClassNotFoundException {
        switch (dbType.toLowerCase()) {
            case "mysql":
                Class.forName("com.mysql.cj.jdbc.Driver");
                break;
            case "postgresql":
                Class.forName("org.postgresql.Driver");
                break;
            case "mariadb":
                Class.forName("org.mariadb.jdbc.Driver");
                break;
            case "sqlserver":
                Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
                break;
            case "oracle":
                Class.forName("oracle.jdbc.OracleDriver");
                break;
            case "sqlite":
                Class.forName("org.sqlite.JDBC");
                break;
            case "clickhouse":
                Class.forName("com.clickhouse.jdbc.ClickHouseDriver");
                break;
            default:
                throw new ClassNotFoundException("未知的数据库类型: " + dbType);
        }
    }
}
