package com.linglong.mcp.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linglong.mcp.annotation.MCPParam;
import com.linglong.mcp.annotation.MCPTool;
import com.linglong.mcp.model.ToolExecutionRequest;
import com.linglong.mcp.model.ToolExecutionResult;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * 图表生成工具
 * 支持Mermaid、PlantUML和draw.io图表生成
 * 新增智能图表生成：通过自然语言描述或上传文件，AI自动生成图表
 */
@Component
public class DiagramTool {

    private static final Logger log = LoggerFactory.getLogger(DiagramTool.class);
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Tika tika = new Tika();

    @Value("${llm.api-key:}")
    private String llmApiKey;

    @Value("${llm.api-url:https://open.bigmodel.cn/api/paas/v4/chat/completions}")
    private String llmApiUrl;

    @Value("${llm.model:glm-4.6}")
    private String llmModel;

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

    // ==================== 智能图表生成 ====================

    /**
     * 智能图表生成
     * 根据用户自然语言描述或上传文件，AI自动生成图表代码并渲染
     *
     * @param request 工具执行请求，包含prompt、filePath、format、outputPath参数
     * @return 生成结果，包含SVG内容或draw.io链接
     */
    @MCPTool(
            name = "generate_smart_diagram",
            description = "智能图表生成：输入自然语言描述或上传文件，AI自动生成架构图、流程图、类图等，支持Mermaid/PlantUML/draw.io格式",
            category = "diagram"
    )
    @MCPParam(name = "prompt", description = "图表描述需求，如：生成一个电商系统的微服务架构图", type = "string", required = true)
    @MCPParam(name = "filePath", description = "上传的文件路径（可选），AI会读取文件内容辅助生成图表", type = "string", required = false)
    @MCPParam(name = "format", description = "图表格式：mermaid（默认，可渲染SVG预览）、plantuml（可渲染SVG预览）、drawio（可编辑，返回draw.io打开链接）", type = "string", required = false, defaultValue = "mermaid", enumValues = "mermaid,plantuml,drawio")
    @MCPParam(name = "outputPath", description = "输出文件保存路径（可选）", type = "string", required = false)
    public ToolExecutionResult generateSmartDiagram(ToolExecutionRequest request) {
        Map<String, Object> params = request.getParameters();
        String prompt = (String) params.get("prompt");
        String filePath = (String) params.get("filePath");
        String format = (String) params.getOrDefault("format", "mermaid");
        String outputPath = (String) params.get("outputPath");

        if (prompt == null || prompt.isEmpty()) {
            return ToolExecutionResult.error("prompt参数不能为空，请描述您想要生成的图表");
        }

        try {
            // 1. 读取文件内容（如果有）
            String fileContent = "";
            if (filePath != null && !filePath.isEmpty()) {
                fileContent = readFileContent(filePath);
                log.info("已读取文件内容: {}, 长度: {} 字符", filePath, fileContent.length());
            }

            // 2. 调用LLM生成图表代码
            String diagramCode = callLlmForDiagram(prompt, fileContent, format);
            if (diagramCode == null || diagramCode.isEmpty()) {
                return ToolExecutionResult.error("AI生成图表代码失败，请重试或调整描述");
            }

            // 清理代码（去除markdown代码块标记）
            diagramCode = cleanDiagramCode(diagramCode, format);

            log.info("AI生成{}图表代码成功, 长度: {} 字符", format, diagramCode.length());

            // 3. 根据格式处理
            return switch (format.toLowerCase()) {
                case "plantuml" -> handlePlantUmlResult(diagramCode, outputPath);
                case "drawio" -> handleDrawioResult(diagramCode, outputPath);
                default -> handleMermaidResult(diagramCode, outputPath);
            };

        } catch (Exception e) {
            log.error("智能图表生成失败", e);
            return ToolExecutionResult.error("图表生成失败: " + e.getMessage());
        }
    }

    /**
     * 读取文件内容
     */
    private String readFileContent(String filePath) throws Exception {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new RuntimeException("文件不存在: " + filePath);
        }
        File file = path.toFile();
        String content = tika.parseToString(file);
        // 限制长度，避免超出LLM上下文
        if (content.length() > 15000) {
            content = content.substring(0, 15000) + "\n...（文件内容已截断）";
        }
        return content;
    }

    /**
     * 调用LLM生成图表代码
     */
    private String callLlmForDiagram(String prompt, String fileContent, String format) throws Exception {
        String systemPrompt = buildSystemPrompt(format);
        String userPrompt = buildUserPrompt(prompt, fileContent);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userPrompt));

        Map<String, Object> requestBody = new java.util.HashMap<>();
        requestBody.put("model", llmModel);
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.3);
        requestBody.put("max_tokens", 4096);

        String bodyJson = objectMapper.writeValueAsString(requestBody);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(llmApiUrl))
                .timeout(Duration.ofSeconds(120))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + llmApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(bodyJson, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("LLM API调用失败: HTTP {} - {}", response.statusCode(), response.body());
            throw new RuntimeException("LLM API调用失败: HTTP " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode choices = root.path("choices");
        if (choices.isArray() && choices.size() > 0) {
            JsonNode message = choices.get(0).path("message");
            return message.path("content").asText("");
        }
        return "";
    }

    /**
     * 构建System Prompt
     */
    private String buildSystemPrompt(String format) {
        return switch (format.toLowerCase()) {
            case "plantuml" -> """
                你是一位专业的UML图表设计专家和PlantUML代码大师。
                
                任务：根据用户的需求，生成标准、正确、美观的 PlantUML 代码。
                
                要求：
                1. 只输出纯 PlantUML 代码，不要有任何解释、markdown代码块标记（如 ```plantuml）或其他额外内容
                2. 代码必须以 @startuml 开头，以 @enduml 结尾
                3. 支持的图表类型：时序图(sequenceDiagram)、类图(classDiagram)、用例图(usecase)、活动图(activity)、组件图(component)、部署图(deployment)、状态图(stateDiagram)、ER图(erDiagram)等
                4. 使用合适的颜色、样式让图表美观易读
                5. 确保语法正确，节点命名清晰有意义
                6. 如果需求复杂，合理分层和分组，使用 package/subgraph 组织
                """;
            case "drawio" -> """
                你是一位专业的图表设计专家和draw.io(mxGraph)XML代码大师。
                
                任务：根据用户的需求，生成标准、正确、美观的 draw.io XML 格式图表代码。
                
                要求：
                1. 只输出纯 XML 代码，不要有任何解释、markdown代码块标记（如 ```xml）或其他额外内容
                2. XML根元素必须是 <mxfile>，包含 <diagram> 子元素
                3. <mxGraphModel> 必须包含 <root>，root内第一个是 <mxCell id="0"/>，第二个是 <mxCell id="1" parent="0"/>
                4. 节点使用 <mxCell id="唯一ID" value="显示文本" style="样式" vertex="1" parent="1">，内部包含 <mxGeometry x="坐标" y="坐标" width="宽度" height="高度" as="geometry"/>
                5. 连线使用 <mxCell id="唯一ID" value="标签文本" style="edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;" edge="1" parent="1" source="源节点ID" target="目标节点ID">，内部包含 <mxGeometry relative="1" as="geometry"/>
                6. 常用节点样式：
                   - 矩形：rounded=1;whiteSpace=wrap;html=1;fillColor=#dae8fc;strokeColor=#6c8ebf;
                   - 圆角矩形：rounded=1;whiteSpace=wrap;html=1;fillColor=#d5e8d4;strokeColor=#82b366;
                   - 数据库：shape=cylinder3;whiteSpace=wrap;html=1;boundedLbl=1;backgroundOutline=1;fillColor=#fff2cc;strokeColor=#d6b656;
                   - 用户/角色：shape=umlActor;verticalLabelPosition=bottom;verticalAlign=top;html=1;fillColor=#f8cecc;strokeColor=#b85450;
                   - 云服务：ellipse;shape=cloud;whiteSpace=wrap;html=1;fillColor=#e1d5e7;strokeColor=#9673a6;
                7. 确保节点布局合理，避免重叠，坐标建议从 x=80,y=40 开始，每个节点间隔 width+40 或 height+40
                8. 图表要有清晰的层次结构，相关节点可以使用 <mxCell> 作为 group（vertex="1"）进行分组
                """;
            default -> """
                你是一位专业的图表设计专家和Mermaid代码大师。
                
                任务：根据用户的需求，生成标准、正确、美观的 Mermaid 图表代码。
                
                要求：
                1. 只输出纯 Mermaid 代码，不要有任何解释、markdown代码块标记（如 ```mermaid）或其他额外内容
                2. 支持的图表类型：graph TD/LR（流程图）、sequenceDiagram（时序图）、classDiagram（类图）、erDiagram（ER图）、stateDiagram（状态图）、gantt（甘特图）、pie（饼图）、journey（用户旅程图）、mindmap（思维导图）等
                3. 选择最适合表达需求的图表类型
                4. 使用合适的样式、颜色让图表美观易读（如 classDef、style、linkStyle）
                5. 确保语法正确，节点命名清晰有意义
                6. 如果需求复杂，合理使用 subgraph 进行分组
                7. 对于架构图，推荐使用 graph TB 或 graph LR，并区分不同组件类型（数据库用 [()]，用户用 (()) 等）
                """;
        };
    }

    /**
     * 构建User Prompt
     */
    private String buildUserPrompt(String prompt, String fileContent) {
        StringBuilder sb = new StringBuilder();
        sb.append("请根据以下需求生成图表代码：\n\n");
        sb.append("【用户需求】\n").append(prompt).append("\n\n");
        if (!fileContent.isEmpty()) {
            sb.append("【参考文件内容】\n").append(fileContent).append("\n\n");
            sb.append("请结合用户需求和参考文件内容生成图表。文件内容可能包含需求文档、代码、设计稿等。\n");
        }
        sb.append("请直接输出图表代码，不要添加任何解释。");
        return sb.toString();
    }

    /**
     * 清理图表代码（去除markdown代码块标记）
     */
    private String cleanDiagramCode(String code, String format) {
        String cleaned = code.trim();
        String blockMarker = "```" + format.toLowerCase();
        if (cleaned.startsWith(blockMarker)) {
            cleaned = cleaned.substring(blockMarker.length()).trim();
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3).trim();
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
        }
        // 去除可能的语言标记前缀（如 xml, plantuml 等）
        if (cleaned.startsWith("xml")) {
            cleaned = cleaned.substring(3).trim();
        }
        if (cleaned.startsWith("plantuml")) {
            cleaned = cleaned.substring(8).trim();
        }
        if (cleaned.startsWith("mermaid")) {
            cleaned = cleaned.substring(7).trim();
        }
        return cleaned;
    }

    /**
     * 处理Mermaid结果
     */
    private ToolExecutionResult handleMermaidResult(String mermaidCode, String outputPath) throws Exception {
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
            String savedPath = saveOutput(outputPath, mermaidCode, "diagram.mmd");
            String svgPath = saveOutput(outputPath != null ? outputPath.replace(".mmd", ".svg").replace(".txt", ".svg") : null, svgContent, "diagram.svg");

            return ToolExecutionResult.success("Mermaid图表生成成功", Map.of(
                    "mermaidCode", mermaidCode,
                    "svgContent", svgContent,
                    "savedPath", savedPath != null ? savedPath : "",
                    "svgPath", svgPath != null ? svgPath : "",
                    "previewUrl", "https://mermaid.live/edit#pako:" + encodedCode,
                    "format", "mermaid"
            ));
        } else {
            // 渲染失败，但至少返回代码
            String savedPath = saveOutput(outputPath, mermaidCode, "diagram.mmd");
            return ToolExecutionResult.success("Mermaid代码生成成功（SVG渲染失败，请检查语法）", Map.of(
                    "mermaidCode", mermaidCode,
                    "svgContent", "",
                    "savedPath", savedPath != null ? savedPath : "",
                    "previewUrl", "https://mermaid.live/edit#pako:" + encodedCode,
                    "format", "mermaid",
                    "error", "SVG渲染失败: HTTP " + response.statusCode()
            ));
        }
    }

    /**
     * 处理PlantUML结果
     */
    private ToolExecutionResult handlePlantUmlResult(String plantUmlCode, String outputPath) throws Exception {
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
            String savedPath = saveOutput(outputPath, plantUmlCode, "diagram.puml");
            String svgPath = saveOutput(outputPath != null ? outputPath.replace(".puml", ".svg").replace(".txt", ".svg") : null, svgContent, "diagram.svg");

            return ToolExecutionResult.success("PlantUML图表生成成功", Map.of(
                    "plantUmlCode", plantUmlCode,
                    "svgContent", svgContent,
                    "savedPath", savedPath != null ? savedPath : "",
                    "svgPath", svgPath != null ? svgPath : "",
                    "previewUrl", "https://www.plantuml.com/plantuml/uml/" + compressed,
                    "format", "plantuml"
            ));
        } else {
            String savedPath = saveOutput(outputPath, plantUmlCode, "diagram.puml");
            return ToolExecutionResult.success("PlantUML代码生成成功（SVG渲染失败，请检查语法）", Map.of(
                    "plantUmlCode", plantUmlCode,
                    "svgContent", "",
                    "savedPath", savedPath != null ? savedPath : "",
                    "previewUrl", "https://www.plantuml.com/plantuml/uml/" + compressed,
                    "format", "plantuml",
                    "error", "SVG渲染失败: HTTP " + response.statusCode()
            ));
        }
    }

    /**
     * 处理draw.io结果
     */
    private ToolExecutionResult handleDrawioResult(String drawioXml, String outputPath) throws Exception {
        // 确保XML格式正确
        if (!drawioXml.contains("<mxfile")) {
            // 尝试包装为mxfile格式
            drawioXml = """
                    <mxfile host="app.diagrams.net">
                      <diagram name="Page-1">
                    """ + drawioXml + """
                      </diagram>
                    </mxfile>
                    """;
        }

        String savedPath = saveOutput(outputPath, drawioXml, "diagram.drawio");

        // 生成draw.io打开链接
        String drawioUrl = generateDrawioUrl(drawioXml);

        return ToolExecutionResult.success("draw.io图表生成成功", Map.of(
                "drawioXml", drawioXml,
                "savedPath", savedPath != null ? savedPath : "",
                "drawioUrl", drawioUrl,
                "format", "drawio",
                "tip", "点击 drawioUrl 可在 draw.io 在线编辑器中打开并编辑此图表"
        ));
    }

    /**
     * 生成draw.io打开链接
     */
    private String generateDrawioUrl(String drawioXml) {
        try {
            String base64 = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(drawioXml.getBytes(StandardCharsets.UTF_8));
            return "https://app.diagrams.net/?create=data:application/xml;base64," + base64;
        } catch (Exception e) {
            log.warn("生成draw.io链接失败", e);
            return "https://app.diagrams.net";
        }
    }

    /**
     * 保存输出文件
     */
    private String saveOutput(String outputPath, String content, String defaultName) {
        if (outputPath == null || outputPath.isEmpty()) {
            try {
                Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "linglong-diagrams");
                if (!Files.exists(tempDir)) {
                    Files.createDirectories(tempDir);
                }
                outputPath = tempDir.resolve(defaultName + "_" + System.currentTimeMillis()).toString();
            } catch (Exception e) {
                log.warn("创建临时目录失败", e);
                return null;
            }
        }

        try {
            Path path = Paths.get(outputPath);
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, content, StandardCharsets.UTF_8);
            log.info("文件已保存: {}", outputPath);
            return outputPath;
        } catch (Exception e) {
            log.warn("保存文件失败: {}", e.getMessage());
            return null;
        }
    }
}
