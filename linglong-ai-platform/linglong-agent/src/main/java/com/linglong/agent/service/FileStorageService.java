package com.linglong.agent.service;

import com.linglong.agent.model.AgentContext;
import com.linglong.agent.model.AgentResult;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 文件存储服务
 * 处理Agent文件上传和结果保存
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    /** Tika 实例（线程安全） */
    private static final Tika tika = new Tika();

    @Value("${agent.upload.path:./uploads}")
    private String defaultUploadPath;

    @Value("${agent.output.path:./outputs}")
    private String defaultOutputPath;

    /**
     * 保存上传的文件
     *
     * @param files 上传的文件列表
     * @param taskId 任务ID
     * @return 保存后的文件信息列表
     */
    public List<AgentContext.UploadedFile> saveUploadedFiles(MultipartFile[] files, String taskId) {
        if (files == null || files.length == 0) {
            return null;
        }

        List<AgentContext.UploadedFile> uploadedFiles = new ArrayList<>();
        Path uploadDir = Paths.get(defaultUploadPath, taskId);

        try {
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    continue;
                }

                String originalFilename = file.getOriginalFilename();
                String fileName = UUID.randomUUID().toString().substring(0, 8) + "_" + originalFilename;
                Path filePath = uploadDir.resolve(fileName);

                // 保存文件
                Files.write(filePath, file.getBytes());

                AgentContext.UploadedFile uploadedFile = new AgentContext.UploadedFile();
                uploadedFile.setFileName(originalFilename);
                uploadedFile.setFileType(file.getContentType());
                uploadedFile.setFilePath(filePath.toString());
                uploadedFile.setFileSize(file.getSize());
                uploadedFiles.add(uploadedFile);

                log.info("文件上传成功: {} -> {}", originalFilename, filePath);
            }

            return uploadedFiles;
        } catch (IOException e) {
            log.error("保存上传文件失败", e);
            throw new RuntimeException("保存上传文件失败: " + e.getMessage());
        }
    }

    /**
     * 保存Agent执行结果到文件
     *
     * @param result 执行结果
     * @param outputPath 输出路径（可选，为空则使用默认路径）
     * @param agentName Agent名称
     * @param taskId 任务ID
     * @return 保存的文件路径
     */
    public String saveResultToFile(AgentResult result, String outputPath, String agentName, String taskId) {
        if (result == null || result.getOutput() == null || result.getOutput().isEmpty()) {
            return null;
        }

        try {
            // 确定输出目录
            Path outputDir;
            if (outputPath != null && !outputPath.isEmpty()) {
                outputDir = Paths.get(outputPath);
            } else {
                outputDir = Paths.get(defaultOutputPath, agentName, taskId);
            }

            // 创建目录
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }

            // 生成文件名
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = agentName + "_" + timestamp + ".md";
            Path filePath = outputDir.resolve(fileName);

            // 写入文件内容
            StringBuilder content = new StringBuilder();
            content.append("# ").append(agentName).append(" 执行结果\n\n");
            content.append("**任务ID**: ").append(taskId).append("\n\n");
            content.append("**执行时间**: ").append(LocalDateTime.now()).append("\n\n");
            content.append("**执行耗时**: ").append(result.getExecutionTime()).append("ms\n\n");
            content.append("**执行状态**: ").append(result.isSuccess() ? "成功" : "失败").append("\n\n");
            
            if (result.getErrorMessage() != null) {
                content.append("**错误信息**: ").append(result.getErrorMessage()).append("\n\n");
            }

            content.append("---\n\n");
            content.append(result.getOutput());

            Files.writeString(filePath, content.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            log.info("结果已保存到文件: {}", filePath);
            return filePath.toString();

        } catch (IOException e) {
            log.error("保存结果到文件失败", e);
            throw new RuntimeException("保存结果到文件失败: " + e.getMessage());
        }
    }

    /**
     * 保存代码文件
     *
     * @param codeContent 代码内容
     * @param fileName 文件名
     * @param outputPath 输出路径
     * @return 保存的文件路径
     */
    public String saveCodeFile(String codeContent, String fileName, String outputPath) {
        if (codeContent == null || codeContent.isEmpty()) {
            return null;
        }

        try {
            Path outputDir = Paths.get(outputPath);
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }

            Path filePath = outputDir.resolve(fileName);
            Files.writeString(filePath, codeContent, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            log.info("代码文件已保存: {}", filePath);
            return filePath.toString();

        } catch (IOException e) {
            log.error("保存代码文件失败", e);
            throw new RuntimeException("保存代码文件失败: " + e.getMessage());
        }
    }

    /**
     * 从上传的文件中读取文本内容
     * 支持普通文本文件和复杂文档格式（PDF/DOCX/DOC/PPTX等）
     *
     * @param uploadedFile 上传的文件
     * @return 文件内容
     */
    public String readFileContent(AgentContext.UploadedFile uploadedFile) {
        if (uploadedFile == null || uploadedFile.getFilePath() == null) {
            log.warn("文件信息不完整: {}", uploadedFile);
            return null;
        }

        try {
            Path filePath = Paths.get(uploadedFile.getFilePath());
            if (!Files.exists(filePath)) {
                log.warn("文件不存在: {}", filePath);
                return null;
            }

            String fileName = uploadedFile.getFileName().toLowerCase();
            log.info("开始读取文件: {}, 类型判断: isDocument={}, isText={}", 
                fileName, isDocumentFile(fileName), isTextFile(fileName));
            
            String content;
            // 对于复杂文档格式（PDF/Word/Office）用 Tika 解析
            if (isDocumentFile(fileName)) {
                content = extractTextWithTika(filePath.toFile(), uploadedFile.getFileName());
            } else if (isTextFile(fileName)) {
                // 对于纯文本文件直接读取
                content = Files.readString(filePath, StandardCharsets.UTF_8);
            } else {
                // 其他类型尝试用 Tika 解析
                content = extractTextWithTika(filePath.toFile(), uploadedFile.getFileName());
            }
            
            log.info("文件 {} 读取完成, 内容长度: {}", fileName, content != null ? content.length() : 0);
            return content;
            
        } catch (Exception e) {
            log.error("读取文件内容失败: {}", uploadedFile.getFilePath(), e);
            return "[文件读取失败: " + uploadedFile.getFileName() + ", 错误: " + e.getMessage() + "]";
        }
    }

    /**
     * 使用 Apache Tika 提取文件文本
     * 自动识别文件类型，支持 PDF/DOCX/DOC/PPTX/TXT/HTML/XML/JSON 等
     */
    private String extractTextWithTika(File file, String originalFilename) throws Exception {
        String mimeType = tika.detect(file);
        log.info("文件类型识别: {} -> {}", originalFilename, mimeType);

        // 对于复杂文档格式（PDF/Word/Office）用 AutoDetectParser 深度解析
        if (mimeType != null && (
                mimeType.contains("pdf") ||
                mimeType.contains("word") ||
                mimeType.contains("officedocument") ||
                mimeType.contains("opendocument") ||
                mimeType.contains("powerpoint") ||
                mimeType.contains("excel") ||
                mimeType.contains("spreadsheet") ||
                mimeType.contains("presentation"))) {
            return parseWithTikaParser(file);
        }

        // 对于纯文本文件直接读取
        try {
            return Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 如果 UTF-8 读取失败，降级使用 Tika
            log.warn("直接读取失败，改用 Tika 解析: {}", e.getMessage());
            return parseWithTikaParser(file);
        }
    }

    /** 使用 Apache Tika AutoDetectParser 解析文档 */
    private String parseWithTikaParser(File file) throws Exception {
        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler(-1); // -1 无内容长度限制
        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();

        try (InputStream stream = new FileInputStream(file)) {
            parser.parse(stream, handler, metadata, context);
        }
        return handler.toString().trim();
    }

    /**
     * 判断是否为文档文件（PDF/Word/PPT/Excel等）
     */
    private boolean isDocumentFile(String fileName) {
        String[] docExtensions = {
            ".pdf", ".doc", ".docx", ".ppt", ".pptx", ".xls", ".xlsx",
            ".odt", ".ods", ".odp", ".rtf"
        };

        String lowerFileName = fileName.toLowerCase();
        for (String ext : docExtensions) {
            if (lowerFileName.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否为文本文件
     */
    private boolean isTextFile(String fileName) {
        String[] textExtensions = {
            ".txt", ".md", ".markdown", ".java", ".py", ".js", ".ts", ".jsx", ".tsx",
            ".html", ".htm", ".css", ".scss", ".less", ".xml", ".json", ".yaml", ".yml",
            ".properties", ".sql", ".sh", ".bash", ".zsh", ".ps1", ".bat", ".cmd",
            ".c", ".cpp", ".h", ".hpp", ".cs", ".go", ".rs", ".rb", ".php", ".swift",
            ".kt", ".kts", ".scala", ".groovy", ".gradle", ".dockerfile", ".gitignore"
        };

        String lowerFileName = fileName.toLowerCase();
        for (String ext : textExtensions) {
            if (lowerFileName.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取默认输出路径
     */
    public String getDefaultOutputPath() {
        return defaultOutputPath;
    }

    /**
     * 获取默认上传路径
     */
    public String getDefaultUploadPath() {
        return defaultUploadPath;
    }
}
