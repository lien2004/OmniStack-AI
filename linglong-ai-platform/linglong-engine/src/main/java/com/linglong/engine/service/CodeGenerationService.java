package com.linglong.engine.service;

import com.linglong.engine.agent.AgentContext;
import com.linglong.engine.agent.AgentResult;
import com.linglong.engine.llm.LLMService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * 代码生成服务
 * 结合模板引擎和LLM生成代码
 */
@Service
public class CodeGenerationService {

    private static final Logger log = LoggerFactory.getLogger(CodeGenerationService.class);

    private final LLMService llmService;
    private final Configuration freemarkerConfig;

    public CodeGenerationService(LLMService llmService) {
        this.llmService = llmService;
        this.freemarkerConfig = new Configuration(Configuration.VERSION_2_3_32);
        this.freemarkerConfig.setDefaultEncoding("UTF-8");
    }

    /**
     * 使用模板生成代码
     *
     * @param templateName 模板名称
     * @param dataModel    数据模型
     * @return 生成的代码
     */
    public String generateFromTemplate(String templateName, Map<String, Object> dataModel) {
        try {
            Template template = freemarkerConfig.getTemplate(templateName);
            StringWriter writer = new StringWriter();
            template.process(dataModel, writer);
            return writer.toString();
        } catch (IOException | TemplateException e) {
            log.error("模板渲染失败: {}", templateName, e);
            throw new RuntimeException("模板渲染失败: " + e.getMessage());
        }
    }

    /**
     * 使用字符串模板生成代码
     *
     * @param templateContent 模板内容
     * @param dataModel       数据模型
     * @return 生成的代码
     */
    public String generateFromStringTemplate(String templateContent, Map<String, Object> dataModel) {
        try {
            Template template = new Template("stringTemplate", new StringReader(templateContent), freemarkerConfig);
            StringWriter writer = new StringWriter();
            template.process(dataModel, writer);
            return writer.toString();
        } catch (IOException | TemplateException e) {
            log.error("字符串模板渲染失败", e);
            throw new RuntimeException("模板渲染失败: " + e.getMessage());
        }
    }

    /**
     * 使用LLM生成代码
     *
     * @param prompt 提示词
     * @return 生成的代码
     */
    public String generateFromLLM(String prompt) {
        String systemPrompt = """
                你是一位资深的Java开发工程师，精通Spring Boot和DDD领域驱动设计。
                
                请根据提供的需求生成高质量的Java代码，要求：
                1. 遵循阿里巴巴Java开发手册
                2. 使用Java 17+特性
                3. 遵循DDD分层架构
                4. 包含完整的注释和JavaDoc
                5. 考虑异常处理和日志记录
                
                输出格式：
                - 每个文件使用 ```java 代码块包裹
                - 文件顶部使用注释标明文件路径
                """;

        return llmService.generate(systemPrompt, prompt);
    }

    /**
     * 生成实体类代码
     *
     * @param className  类名
     * @param packageName 包名
     * @param fields     字段列表
     * @return 生成的代码
     */
    public String generateEntityClass(String className, String packageName, java.util.List<FieldDefinition> fields) {
        String template = """
                package ${packageName};
                
                import jakarta.persistence.*;
                import lombok.Data;
                import lombok.NoArgsConstructor;
                import lombok.AllArgsConstructor;
                import lombok.Builder;
                
                import java.time.LocalDateTime;
                
                /**
                 * ${className}实体类
                 */
                @Data
                @NoArgsConstructor
                @AllArgsConstructor
                @Builder
                @Entity
                @Table(name = "${tableName}")
                public class ${className} {
                
                    @Id
                    @GeneratedValue(strategy = GenerationType.IDENTITY)
                    private Long id;
                
                <#list fields as field>
                    /**
                     * ${field.comment}
                     */
                    <#if field.columnName??>
                    @Column(name = "${field.columnName}")
                    </#if>
                    private ${field.type} ${field.name};
                
                </#list>
                    /**
                     * 创建时间
                     */
                    @Column(name = "created_at", updatable = false)
                    private LocalDateTime createdAt;
                
                    /**
                     * 更新时间
                     */
                    @Column(name = "updated_at")
                    private LocalDateTime updatedAt;
                
                    @PrePersist
                    protected void onCreate() {
                        createdAt = LocalDateTime.now();
                        updatedAt = LocalDateTime.now();
                    }
                
                    @PreUpdate
                    protected void onUpdate() {
                        updatedAt = LocalDateTime.now();
                    }
                }
                """;

        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("packageName", packageName);
        dataModel.put("className", className);
        dataModel.put("tableName", camelToUnderscore(className));
        dataModel.put("fields", fields);

        return generateFromStringTemplate(template, dataModel);
    }

    /**
     * 生成Repository接口代码
     *
     * @param entityName  实体名
     * @param packageName 包名
     * @return 生成的代码
     */
    public String generateRepository(String entityName, String packageName) {
        String template = """
                package ${packageName};
                
                import ${entityPackage}.${entityName};
                import org.springframework.data.jpa.repository.JpaRepository;
                import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
                import org.springframework.stereotype.Repository;
                
                /**
                 * ${entityName}仓储接口
                 */
                @Repository
                public interface ${entityName}Repository extends JpaRepository<${entityName}, Long>, 
                        JpaSpecificationExecutor<${entityName}> {
                }
                """;

        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("entityName", entityName);
        dataModel.put("packageName", packageName);
        dataModel.put("entityPackage", packageName.replace(".repository", ".entity"));

        return generateFromStringTemplate(template, dataModel);
    }

    /**
     * 生成Service接口代码
     *
     * @param entityName  实体名
     * @param packageName 包名
     * @return 生成的代码
     */
    public String generateServiceInterface(String entityName, String packageName) {
        String template = """
                package ${packageName};
                
                import ${entityPackage}.${entityName};
                import ${entityPackage}.${entityName}DTO;
                import org.springframework.data.domain.Page;
                import org.springframework.data.domain.Pageable;
                
                import java.util.Optional;
                
                /**
                 * ${entityName}服务接口
                 */
                public interface ${entityName}Service {
                
                    /**
                     * 根据ID查询
                     */
                    Optional<${entityName}DTO> findById(Long id);
                
                    /**
                     * 分页查询
                     */
                    Page<${entityName}DTO> findAll(Pageable pageable);
                
                    /**
                     * 创建
                     */
                    ${entityName}DTO create(${entityName}DTO dto);
                
                    /**
                     * 更新
                     */
                    ${entityName}DTO update(Long id, ${entityName}DTO dto);
                
                    /**
                     * 删除
                     */
                    void delete(Long id);
                }
                """;

        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("entityName", entityName);
        dataModel.put("packageName", packageName);
        dataModel.put("entityPackage", packageName.replace(".service", ".entity"));

        return generateFromStringTemplate(template, dataModel);
    }

    /**
     * 将代码写入文件
     *
     * @param filePath    文件路径
     * @param codeContent 代码内容
     */
    public void writeCodeToFile(String filePath, String codeContent) {
        try {
            Path path = Paths.get(filePath);
            Path parent = path.getParent();

            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            Files.writeString(path, codeContent, StandardCharsets.UTF_8);
            log.info("代码已写入文件: {}", filePath);

        } catch (IOException e) {
            log.error("写入代码文件失败: {}", filePath, e);
            throw new RuntimeException("写入代码文件失败: " + e.getMessage());
        }
    }

    /**
     * 驼峰命名转下划线命名
     */
    private String camelToUnderscore(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    /**
     * 字段定义
     */
    public static class FieldDefinition {
        private String name;
        private String type;
        private String comment;
        private String columnName;

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
        public String getColumnName() { return columnName; }
        public void setColumnName(String columnName) { this.columnName = columnName; }
    }
}
