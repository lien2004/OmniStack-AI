package com.linglong.mcp.tool;

import com.linglong.mcp.annotation.MCPTool;
import com.linglong.mcp.model.ToolExecutionRequest;
import com.linglong.mcp.model.ToolExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

/**
 * 图表生成工具
 * 支持Mermaid和PlantUML图表生成
 */
@Component
public class DiagramTool {

    private static final Logger log = LoggerFactory.getLogger(DiagramTool.class);
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    /**
     * 生成Mermaid图表（使用在线API）
     *
     * @param request 工具执行请求，包含mermaidCode、outputPath、format参数
     * @return 生成结果，包含SVG内容
     */
    @MCPTool(
            name = "generate_mermaid_diagram",
            description = "根据Mermaid代码生成图表(SVG)，返回SVG内容",
            category = "diagram"
    )
    public ToolExecutionResult generateMermaidDiagram(ToolExecutionRequest request) {
        Map<String, Object> params = request.getParameters();
        String mermaidCode = (String) params.get("mermaidCode");
        String outputPath = (String) params.get("outputPath");

        if (mermaidCode == null || mermaidCode.isEmpty()) {
            return ToolExecutionResult.error("mermaidCode参数不能为空");
        }

        try {
            // 使用 mermaid.ink API 生成 SVG
            String encodedCode = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(mermaidCode.getBytes(StandardCharsets.UTF_8));
            
            String apiUrl = "https://mermaid.ink/svg/" + encodedCode;
            
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(60))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String svgContent = response.body();
                
                // 如果指定了输出路径，保存文件
                if (outputPath != null && !outputPath.isEmpty()) {
                    try {
                        Path outputFilePath = Paths.get(outputPath);
                        Path parent = outputFilePath.getParent();
                        if (parent != null && !Files.exists(parent)) {
                            Files.createDirectories(parent);
                        }
                        Files.writeString(outputFilePath, svgContent, StandardCharsets.UTF_8);
                        log.info("Mermaid图表已保存: {}", outputPath);
                    } catch (Exception e) {
                        log.warn("保存文件失败，但SVG已生成: {}", e.getMessage());
                    }
                }

                log.info("Mermaid图表生成成功，SVG大小: {} bytes", svgContent.length());
                return ToolExecutionResult.success("图表生成成功", Map.of(
                        "svgContent", svgContent,
                        "outputPath", outputPath != null ? outputPath : "",
                        "fileSize", svgContent.length()
                ));
            } else {
                return ToolExecutionResult.error("图表生成失败: HTTP " + response.statusCode());
            }

        } catch (Exception e) {
            log.error("Mermaid图表生成失败", e);
            return ToolExecutionResult.error("图表生成失败: " + e.getMessage());
        }
    }

    /**
     * 生成PlantUML图表（使用在线API）
     *
     * @param request 工具执行请求，包含plantUmlCode、outputPath参数
     * @return 生成结果，包含SVG内容
     */
    @MCPTool(
            name = "generate_plantuml_diagram",
            description = "根据PlantUML代码生成图表(SVG)，返回SVG内容",
            category = "diagram"
    )
    public ToolExecutionResult generatePlantUmlDiagram(ToolExecutionRequest request) {
        Map<String, Object> params = request.getParameters();
        String plantUmlCode = (String) params.get("plantUmlCode");
        String outputPath = (String) params.get("outputPath");

        if (plantUmlCode == null || plantUmlCode.isEmpty()) {
            return ToolExecutionResult.error("plantUmlCode参数不能为空");
        }

        try {
            // 使用 PlantUML 在线服务生成 SVG
            // 首先压缩PlantUML代码
            String compressed = compressPlantUml(plantUmlCode);
            String apiUrl = "https://www.plantuml.com/plantuml/svg/" + compressed;
            
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(60))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String svgContent = response.body();
                
                // 如果指定了输出路径，保存文件
                if (outputPath != null && !outputPath.isEmpty()) {
                    try {
                        Path outputFilePath = Paths.get(outputPath);
                        Path parent = outputFilePath.getParent();
                        if (parent != null && !Files.exists(parent)) {
                            Files.createDirectories(parent);
                        }
                        Files.writeString(outputFilePath, svgContent, StandardCharsets.UTF_8);
                        log.info("PlantUML图表已保存: {}", outputPath);
                    } catch (Exception e) {
                        log.warn("保存文件失败，但SVG已生成: {}", e.getMessage());
                    }
                }

                log.info("PlantUML图表生成成功，SVG大小: {} bytes", svgContent.length());
                return ToolExecutionResult.success("图表生成成功", Map.of(
                        "svgContent", svgContent,
                        "outputPath", outputPath != null ? outputPath : "",
                        "fileSize", svgContent.length()
                ));
            } else {
                return ToolExecutionResult.error("图表生成失败: HTTP " + response.statusCode());
            }

        } catch (Exception e) {
            log.error("PlantUML图表生成失败", e);
            return ToolExecutionResult.error("图表生成失败: " + e.getMessage());
        }
    }

    /**
     * 压缩PlantUML代码为URL编码格式
     * 使用PlantUML的标准压缩算法
     */
    private String compressPlantUml(String text) throws Exception {
        // 使用 deflate 压缩
        java.util.zip.Deflater deflater = new java.util.zip.Deflater(9);
        deflater.setInput(text.getBytes(StandardCharsets.UTF_8));
        deflater.finish();
        byte[] buffer = new byte[8192];
        int compressedLength = deflater.deflate(buffer);
        deflater.end();
        
        // 转换为 PlantUML 的 base64 编码
        byte[] compressed = new byte[compressedLength];
        System.arraycopy(buffer, 0, compressed, 0, compressedLength);
        
        return encodePlantUmlBase64(compressed);
    }

    /**
     * PlantUML 特殊的 Base64 编码
     */
    private String encodePlantUmlBase64(byte[] data) {
        final char[] PLANTUML_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-_".toCharArray();
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < data.length; i += 3) {
            int b1 = data[i] & 0xFF;
            int b2 = (i + 1 < data.length) ? (data[i + 1] & 0xFF) : 0;
            int b3 = (i + 2 < data.length) ? (data[i + 2] & 0xFF) : 0;
            
            result.append(PLANTUML_CHARS[b1 >> 2]);
            result.append(PLANTUML_CHARS[((b1 & 0x3) << 4) | (b2 >> 4)]);
            if (i + 1 < data.length) {
                result.append(PLANTUML_CHARS[((b2 & 0xF) << 2) | (b3 >> 6)]);
            }
            if (i + 2 < data.length) {
                result.append(PLANTUML_CHARS[b3 & 0x3F]);
            }
        }
        return result.toString();
    }

    /**
     * 验证Mermaid代码语法
     *
     * @param request 工具执行请求，包含mermaidCode参数
     * @return 验证结果
     */
    @MCPTool(
            name = "validate_mermaid",
            description = "验证Mermaid代码语法",
            category = "diagram"
    )
    public ToolExecutionResult validateMermaid(ToolExecutionRequest request) {
        Map<String, Object> params = request.getParameters();
        String mermaidCode = (String) params.get("mermaidCode");

        if (mermaidCode == null || mermaidCode.isEmpty()) {
            return ToolExecutionResult.error("mermaidCode参数不能为空");
        }

        // 基本语法检查
        boolean isValid = true;
        StringBuilder errors = new StringBuilder();

        // 检查是否包含图表类型声明
        String[] diagramTypes = {"graph", "flowchart", "sequenceDiagram", "classDiagram",
                "stateDiagram", "erDiagram", "gantt", "pie", "journey"};

        boolean hasType = false;
        for (String type : diagramTypes) {
            if (mermaidCode.contains(type)) {
                hasType = true;
                break;
            }
        }

        if (!hasType) {
            isValid = false;
            errors.append("未找到有效的图表类型声明\n");
        }

        // 检查括号匹配
        int openBrackets = 0;
        int closeBrackets = 0;
        for (char c : mermaidCode.toCharArray()) {
            if (c == '[') openBrackets++;
            if (c == ']') closeBrackets++;
            if (c == '(') openBrackets++;
            if (c == ')') closeBrackets++;
            if (c == '{') openBrackets++;
            if (c == '}') closeBrackets++;
        }

        if (openBrackets != closeBrackets) {
            isValid = false;
            errors.append("括号不匹配\n");
        }

        if (isValid) {
            return ToolExecutionResult.success("Mermaid代码语法验证通过");
        } else {
            return ToolExecutionResult.error("语法错误:\n" + errors);
        }
    }

    /**
     * 生成系统架构图代码
     *
     * @param request 工具执行请求，包含components、relations、title参数
     * @return Mermaid代码
     */
    @MCPTool(
            name = "generate_architecture_code",
            description = "生成系统架构图的Mermaid代码",
            category = "diagram"
    )
    public ToolExecutionResult generateArchitectureCode(ToolExecutionRequest request) {
        Map<String, Object> params = request.getParameters();
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, String>> components =
                (java.util.List<Map<String, String>>) params.get("components");
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, String>> relations =
                (java.util.List<Map<String, String>>) params.get("relations");
        String title = (String) params.getOrDefault("title", "系统架构图");

        if (components == null || components.isEmpty()) {
            return ToolExecutionResult.error("components参数不能为空");
        }

        StringBuilder mermaidCode = new StringBuilder();
        mermaidCode.append("graph TB\n");
        mermaidCode.append("    subgraph \"").append(title).append("\"\n");

        // 添加组件
        for (Map<String, String> component : components) {
            String id = component.get("id");
            String name = component.get("name");
            String type = component.getOrDefault("type", "service");

            if (id == null || name == null) {
                continue;
            }

            String shape = switch (type.toLowerCase()) {
                case "database", "db" -> "[(" + name + ")]";
                case "user", "actor" -> "((" + name + "))";
                case "external" -> ">" + name + "]";
                default -> "[" + name + "]";
            };

            mermaidCode.append("        ").append(id).append(shape).append("\n");
        }

        // 添加关系
        if (relations != null) {
            for (Map<String, String> relation : relations) {
                String from = relation.get("from");
                String to = relation.get("to");
                String label = relation.get("label");

                if (from == null || to == null) {
                    continue;
                }

                mermaidCode.append("        ").append(from);
                if (label != null && !label.isEmpty()) {
                    mermaidCode.append(" -->|\"").append(label).append("\"| ").append(to);
                } else {
                    mermaidCode.append(" --> ").append(to);
                }
                mermaidCode.append("\n");
            }
        }

        mermaidCode.append("    end\n");

        return ToolExecutionResult.success("架构图代码生成成功", Map.of(
                "mermaidCode", mermaidCode.toString()
        ));
    }

    /**
     * 生成类图代码
     *
     * @param request 工具执行请求，包含classes、relations参数
     * @return Mermaid代码
     */
    @MCPTool(
            name = "generate_class_diagram_code",
            description = "生成类图的Mermaid代码",
            category = "diagram"
    )
    public ToolExecutionResult generateClassDiagramCode(ToolExecutionRequest request) {
        Map<String, Object> params = request.getParameters();
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> classes =
                (java.util.List<Map<String, Object>>) params.get("classes");
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, String>> relations =
                (java.util.List<Map<String, String>>) params.get("relations");

        if (classes == null || classes.isEmpty()) {
            return ToolExecutionResult.error("classes参数不能为空");
        }

        StringBuilder mermaidCode = new StringBuilder();
        mermaidCode.append("classDiagram\n");

        // 添加类定义
        for (Map<String, Object> cls : classes) {
            String name = (String) cls.get("name");
            @SuppressWarnings("unchecked")
            java.util.List<String> attributes = (java.util.List<String>) cls.get("attributes");
            @SuppressWarnings("unchecked")
            java.util.List<String> methods = (java.util.List<String>) cls.get("methods");

            if (name == null) {
                continue;
            }

            mermaidCode.append("    class ").append(name).append(" {\n");

            if (attributes != null) {
                for (String attr : attributes) {
                    mermaidCode.append("        ").append(attr).append("\n");
                }
            }

            if (methods != null) {
                for (String method : methods) {
                    mermaidCode.append("        ").append(method).append("\n");
                }
            }

            mermaidCode.append("    }\n");
        }

        // 添加关系
        if (relations != null) {
            for (Map<String, String> relation : relations) {
                String from = relation.get("from");
                String to = relation.get("to");
                String type = relation.getOrDefault("type", "association");

                if (from == null || to == null) {
                    continue;
                }

                String arrow = switch (type.toLowerCase()) {
                    case "inheritance", "extends" -> " <|-- ";
                    case "implementation", "implements" -> " <|.. ";
                    case "composition" -> " *-- ";
                    case "aggregation" -> " o-- ";
                    case "dependency" -> " ..> ";
                    default -> " --> ";
                };

                mermaidCode.append("    ").append(from).append(arrow).append(to).append("\n");
            }
        }

        return ToolExecutionResult.success("类图代码生成成功", Map.of(
                "mermaidCode", mermaidCode.toString()
        ));
    }
}
