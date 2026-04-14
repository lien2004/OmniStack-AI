package com.linglong.mcp.tool;

import com.linglong.mcp.annotation.MCPTool;
import com.linglong.mcp.model.ToolExecutionRequest;
import com.linglong.mcp.model.ToolExecutionResult;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * 文件读取工具
 * 支持读取PDF、Docx、Markdown、TXT等文档内容
 */
@Component
public class ReadFileTool {

    private static final Logger log = LoggerFactory.getLogger(ReadFileTool.class);
    private static final Tika tika = new Tika();

    /**
     * 读取文件内容
     *
     * @param request 工具执行请求，包含filePath和maxLength参数
     * @return 文件内容
     */
    @MCPTool(
            name = "read_file",
            description = "读取指定路径的文件内容，支持PDF、Word、Markdown、TXT等格式",
            category = "document"
    )
    public ToolExecutionResult readFile(ToolExecutionRequest request) {
        Map<String, Object> params = request.getParameters();
        String filePath = (String) params.get("filePath");

        if (filePath == null || filePath.isEmpty()) {
            return ToolExecutionResult.error("filePath参数不能为空");
        }

        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return ToolExecutionResult.error("文件不存在: " + filePath);
            }

            File file = path.toFile();
            String content;
            String mimeType = tika.detect(file);

            log.info("读取文件: {}, MIME类型: {}", filePath, mimeType);

            if (mimeType != null && (
                    mimeType.contains("pdf") ||
                            mimeType.contains("word") ||
                            mimeType.contains("officedocument") ||
                            mimeType.contains("opendocument"))) {
                // 使用Apache Tika解析复杂文档
                content = parseWithTika(file);
            } else {
                // 直接读取文本文件
                content = Files.readString(path, StandardCharsets.UTF_8);
            }

            // 限制返回内容长度
            int maxLength = params.containsKey("maxLength") ?
                    (Integer) params.get("maxLength") : 10000;

            if (content.length() > maxLength) {
                content = content.substring(0, maxLength) + "\n... (内容已截断)";
            }

            return ToolExecutionResult.success(content, Map.of(
                    "filePath", filePath,
                    "mimeType", mimeType,
                    "fileSize", file.length()
            ));

        } catch (Exception e) {
            log.error("读取文件失败: {}", filePath, e);
            return ToolExecutionResult.error("读取文件失败: " + e.getMessage());
        }
    }

    /**
     * 批量读取多个文件
     *
     * @param request 工具执行请求，包含filePaths参数
     * @return 文件内容列表
     */
    @MCPTool(
            name = "read_files_batch",
            description = "批量读取多个文件内容",
            category = "document"
    )
    public ToolExecutionResult readFilesBatch(ToolExecutionRequest request) {
        Map<String, Object> params = request.getParameters();
        @SuppressWarnings("unchecked")
        List<String> filePaths = (List<String>) params.get("filePaths");

        if (filePaths == null || filePaths.isEmpty()) {
            return ToolExecutionResult.error("filePaths参数不能为空");
        }

        StringBuilder result = new StringBuilder();
        int successCount = 0;
        int failCount = 0;

        for (String filePath : filePaths) {
            try {
                Path path = Paths.get(filePath);
                if (!Files.exists(path)) {
                    result.append("\n=== ").append(filePath).append(" ===\n");
                    result.append("[错误: 文件不存在]\n");
                    failCount++;
                    continue;
                }

                String content = Files.readString(path, StandardCharsets.UTF_8);
                result.append("\n=== ").append(filePath).append(" ===\n");
                result.append(content).append("\n");
                successCount++;

            } catch (Exception e) {
                result.append("\n=== ").append(filePath).append(" ===\n");
                result.append("[错误: ").append(e.getMessage()).append("]\n");
                failCount++;
            }
        }

        return ToolExecutionResult.success(result.toString(), Map.of(
                "successCount", successCount,
                "failCount", failCount,
                "totalCount", filePaths.size()
        ));
    }

    /**
     * 读取目录下的所有文件
     *
     * @param request 工具执行请求，包含directoryPath和extensions参数
     * @return 文件内容列表
     */
    @MCPTool(
            name = "read_directory",
            description = "读取目录下所有符合条件的文件内容",
            category = "document"
    )
    public ToolExecutionResult readDirectory(ToolExecutionRequest request) {
        Map<String, Object> params = request.getParameters();
        String directoryPath = (String) params.get("directoryPath");
        @SuppressWarnings("unchecked")
        List<String> extensions = (List<String>) params.get("extensions");

        if (directoryPath == null || directoryPath.isEmpty()) {
            return ToolExecutionResult.error("directoryPath参数不能为空");
        }

        try {
            Path dirPath = Paths.get(directoryPath);
            if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
                return ToolExecutionResult.error("目录不存在: " + directoryPath);
            }

            StringBuilder result = new StringBuilder();
            int fileCount = 0;

            for (Path path : Files.walk(dirPath)
                    .filter(Files::isRegularFile)
                    .toList()) {

                // 过滤扩展名
                if (extensions != null && !extensions.isEmpty()) {
                    String fileName = path.getFileName().toString();
                    boolean matches = extensions.stream()
                            .anyMatch(ext -> fileName.toLowerCase().endsWith(ext.toLowerCase()));
                    if (!matches) {
                        continue;
                    }
                }

                try {
                    String content = Files.readString(path, StandardCharsets.UTF_8);
                    result.append("\n=== ").append(path.toString()).append(" ===\n");
                    result.append(content).append("\n");
                    fileCount++;
                } catch (Exception e) {
                    log.warn("读取文件失败: {}", path, e);
                }
            }

            return ToolExecutionResult.success(result.toString(), Map.of(
                    "directory", directoryPath,
                    "fileCount", fileCount
            ));

        } catch (Exception e) {
            log.error("读取目录失败: {}", directoryPath, e);
            return ToolExecutionResult.error("读取目录失败: " + e.getMessage());
        }
    }

    /**
     * 使用Apache Tika解析文档
     */
    private String parseWithTika(File file) throws Exception {
        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler(-1); // -1表示无限制
        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();

        try (InputStream stream = new FileInputStream(file)) {
            parser.parse(stream, handler, metadata, context);
        }

        return handler.toString();
    }
}
