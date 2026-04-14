package com.linglong.common.constants;

/**
 * 通用常量
 */
public interface CommonConstants {

    // 请求头
    String HEADER_TOKEN = "Authorization";
    String HEADER_USER_ID = "X-User-Id";
    String HEADER_TENANT_ID = "X-Tenant-Id";
    
    // Token前缀
    String TOKEN_PREFIX = "Bearer ";
    
    // 默认分页参数
    Integer DEFAULT_PAGE_NUM = 1;
    Integer DEFAULT_PAGE_SIZE = 10;
    Integer MAX_PAGE_SIZE = 100;
    
    // 缓存前缀
    String CACHE_PREFIX = "linglong:";
    String CACHE_USER_PREFIX = CACHE_PREFIX + "user:";
    String CACHE_PROJECT_PREFIX = CACHE_PREFIX + "project:";
    String CACHE_AGENT_PREFIX = CACHE_PREFIX + "agent:";
    
    // AI相关常量
    Integer AI_MAX_TOKENS = 4096;
    Double AI_TEMPERATURE = 0.7;
    Double AI_TOP_P = 0.95;
    
    // Agent状态
    String AGENT_STATUS_PENDING = "PENDING";
    String AGENT_STATUS_RUNNING = "RUNNING";
    String AGENT_STATUS_SUCCESS = "SUCCESS";
    String AGENT_STATUS_FAILED = "FAILED";
    
    // 项目状态
    String PROJECT_STATUS_INIT = "INIT";
    String PROJECT_STATUS_ANALYZING = "ANALYZING";
    String PROJECT_STATUS_DESIGNING = "DESIGNING";
    String PROJECT_STATUS_CODING = "CODING";
    String PROJECT_STATUS_TESTING = "TESTING";
    String PROJECT_STATUS_COMPLETED = "COMPLETED";
    
    // 文件类型
    String FILE_TYPE_JAVA = "java";
    String FILE_TYPE_XML = "xml";
    String FILE_TYPE_YAML = "yaml";
    String FILE_TYPE_JSON = "json";
    String FILE_TYPE_MARKDOWN = "md";
    String FILE_TYPE_SQL = "sql";
}
