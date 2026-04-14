package com.linglong.mcp.tool;

import com.linglong.mcp.annotation.MCPTool;
import com.linglong.mcp.model.ToolExecutionRequest;
import com.linglong.mcp.model.ToolExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;

/**
 * 代码写入工具
 * 将生成的代码写入文件系统
 */
@Component
public class WriteCodeTool {

    private static final Logger log = LoggerFactory.getLogger(WriteCodeTool.class);

    /**
     * 写入代码文件
     *
     * @param request 工具执行请求，包含filePath、content和append参数
     * @return 写入结果
     */
    @MCPTool(
            name = "write_code",
            description = "将代码内容写入指定文件路径，支持自动创建目录",
            category = "code"
    )
    public ToolExecutionResult writeCode(ToolExecutionRequest request) {
        Map<String, Object> params = request.getParameters();
        String filePath = (String) params.get("filePath");
        String content = (String) params.get("content");
        boolean append = params.containsKey("append") && (Boolean) params.get("append");

        if (filePath == null || filePath.isEmpty()) {
            return ToolExecutionResult.error("filePath参数不能为空");
        }

        if (content == null) {
            return ToolExecutionResult.error("content参数不能为空");
        }

        try {
            Path path = Paths.get(filePath);

            // 创建父目录
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
                log.info("创建目录: {}", parent);
            }

            // 写入文件
            StandardOpenOption[] options = append ?
                    new StandardOpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.APPEND} :
                    new StandardOpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE};

            Files.writeString(path, content, StandardCharsets.UTF_8, options);

            log.info("写入文件成功: {}, 大小: {} bytes", filePath, content.getBytes(StandardCharsets.UTF_8).length);

            return ToolExecutionResult.success("文件写入成功", Map.of(
                    "filePath", filePath,
                    "fileSize", content.getBytes(StandardCharsets.UTF_8).length,
                    "append", append
            ));

        } catch (IOException e) {
            log.error("写入文件失败: {}", filePath, e);
            return ToolExecutionResult.error("写入文件失败: " + e.getMessage());
        }
    }

    /**
     * 批量写入多个代码文件
     *
     * @param request 工具执行请求，包含files参数
     * @return 批量写入结果
     */
    @MCPTool(
            name = "write_code_batch",
            description = "批量写入多个代码文件",
            category = "code"
    )
    public ToolExecutionResult writeCodeBatch(ToolExecutionRequest request) {
        Map<String, Object> params = request.getParameters();
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> files = (java.util.List<Map<String, Object>>) params.get("files");

        if (files == null || files.isEmpty()) {
            return ToolExecutionResult.error("files参数不能为空");
        }

        int successCount = 0;
        int failCount = 0;
        StringBuilder errorMessages = new StringBuilder();

        for (Map<String, Object> file : files) {
            String filePath = (String) file.get("filePath");
            String content = (String) file.get("content");

            if (filePath == null || content == null) {
                failCount++;
                errorMessages.append("跳过无效文件项\n");
                continue;
            }

            try {
                Path path = Paths.get(filePath);
                Path parent = path.getParent();
                if (parent != null && !Files.exists(parent)) {
                    Files.createDirectories(parent);
                }

                Files.writeString(path, content, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                successCount++;
                log.info("批量写入文件成功: {}", filePath);

            } catch (IOException e) {
                failCount++;
                errorMessages.append("写入失败 [").append(filePath).append("]: ")
                        .append(e.getMessage()).append("\n");
                log.error("批量写入文件失败: {}", filePath, e);
            }
        }

        if (failCount == 0) {
            return ToolExecutionResult.success(
                    String.format("成功写入 %d 个文件", successCount),
                    Map.of("successCount", successCount, "failCount", failCount)
            );
        } else {
            return ToolExecutionResult.error(String.format(
                    "写入完成: %d 成功, %d 失败\n%s", successCount, failCount, errorMessages));
        }
    }

    /**
     * 创建项目目录结构
     *
     * @param request 工具执行请求，包含basePath、projectName和packageName参数
     * @return 创建结果
     */
    @MCPTool(
            name = "create_project_structure",
            description = "创建标准的Java项目目录结构",
            category = "code"
    )
    public ToolExecutionResult createProjectStructure(ToolExecutionRequest request) {
        Map<String, Object> params = request.getParameters();
        String basePath = (String) params.get("basePath");
        String projectName = (String) params.get("projectName");
        String packageName = (String) params.get("packageName");

        if (basePath == null || basePath.isEmpty()) {
            return ToolExecutionResult.error("basePath参数不能为空");
        }

        if (projectName == null || projectName.isEmpty()) {
            projectName = "my-project";
        }

        try {
            Path projectPath = Paths.get(basePath, projectName);

            // 创建标准Maven/Gradle项目结构
            String[] dirs = {
                    "src/main/java",
                    "src/main/resources",
                    "src/test/java",
                    "src/test/resources"
            };

            // 如果提供了包名，创建包目录
            if (packageName != null && !packageName.isEmpty()) {
                String packagePath = packageName.replace(".", "/");
                dirs[0] = "src/main/java/" + packagePath;
                dirs[2] = "src/test/java/" + packagePath;
            }

            for (String dir : dirs) {
                Path dirPath = projectPath.resolve(dir);
                Files.createDirectories(dirPath);
            }

            log.info("创建项目结构成功: {}", projectPath);

            return ToolExecutionResult.success("项目结构创建成功", Map.of(
                    "projectPath", projectPath.toString(),
                    "projectName", projectName
            ));

        } catch (IOException e) {
            log.error("创建项目结构失败", e);
            return ToolExecutionResult.error("创建项目结构失败: " + e.getMessage());
        }
    }

    /**
     * 检查文件是否存在
     *
     * @param request 工具执行请求，包含filePath参数
     * @return 检查结果
     */
    @MCPTool(
            name = "file_exists",
            description = "检查指定路径的文件是否存在",
            category = "code"
    )
    public ToolExecutionResult fileExists(ToolExecutionRequest request) {
        Map<String, Object> params = request.getParameters();
        String filePath = (String) params.get("filePath");

        if (filePath == null || filePath.isEmpty()) {
            return ToolExecutionResult.error("filePath参数不能为空");
        }

        Path path = Paths.get(filePath);
        boolean exists = Files.exists(path);
        boolean isDirectory = Files.isDirectory(path);

        return ToolExecutionResult.success(exists ? "文件存在" : "文件不存在", Map.of(
                "exists", exists,
                "isDirectory", isDirectory,
                "filePath", filePath
        ));
    }
}
