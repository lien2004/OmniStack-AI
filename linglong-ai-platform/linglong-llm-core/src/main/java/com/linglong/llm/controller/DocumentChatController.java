package com.linglong.llm.controller;

import com.linglong.llm.service.ChatHistoryService;
import com.linglong.llm.service.MultiModelChatService;
import com.linglong.llm.service.VectorStoreService;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文档对话控制器（增强版）
 *
 * <p>支持：
 * <ul>
 *   <li>多格式文档：PDF、Word(DOCX/DOC)、Excel(XLSX/XLS)、PPT(PPTX/PPT)、RTF、
 *       Markdown、TXT、XML、HTML、JSON、CSV 等（Apache Tika 自动识别）</li>
 *   <li>两种输入方式：文件流上传 / 服务器本地文件路径</li>
 *   <li>RAG 增强：从向量数据库检索相关文档片段作为上下文</li>
 *   <li>多模型路由：GLM 系列 → 智谱AI；gpt/linglong → CodeFlow 灵龙AI</li>
 *   <li>流式输出：SSE 实时推送，提升用户体验</li>
 *   <li>对话历史：基于 chatId 维护多轮上下文，MySQL 持久化</li>
 * </ul>
 *
 * <pre>
 * POST /ai/document/chat         文件流上传对话（同步）
 * POST /ai/document/stream       文件流上传对话（SSE 流式）
 * POST /ai/document/chat/path    本地路径文件对话（同步）
 * POST /ai/document/stream/path  本地路径文件对话（SSE 流式）
 * POST /ai/document/upload/temp  上传临时文件，返回路径（供 MCP 工具使用）
 * </pre>
 */
@RestController
@RequestMapping("/ai/document")
public class DocumentChatController {

    private static final Logger log = LoggerFactory.getLogger(DocumentChatController.class);

    /**
     * 支持的文件扩展名（Apache Tika 可解析的全部格式）
     */
    private static final String[] SUPPORTED_EXTENSIONS = {
        ".pdf", ".docx", ".doc", ".rtf",
        ".xlsx", ".xls", ".xlsm",
        ".pptx", ".ppt", ".pps",
        ".txt", ".md", ".markdown",
        ".html", ".htm",
        ".json", ".yaml", ".yml", ".xml",
        ".csv",
        ".java", ".py", ".js", ".ts", ".go", ".rs", ".cpp", ".c", ".sh"
    };

    private static final Tika tika = new Tika();

    @Autowired
    private MultiModelChatService multiModelChatService;

    @Autowired(required = false)
    private ChatHistoryService chatHistoryService;

    @Autowired(required = false)
    private VectorStoreService vectorStoreService;

    // =====================================================================
    // 文件流上传 - 同步
    // =====================================================================

    /**
     * 文件流上传对话（同步）
     * 接收 multipart/form-data：file + message + model + chatId + useRag
     */
    @PostMapping(value = "/chat", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> documentChat(
            @RequestParam("file") MultipartFile file,
            @RequestParam("message") String message,
            @RequestParam(value = "model", defaultValue = "glm-4.6") String model,
            @RequestParam(value = "chatId", defaultValue = "") String chatId,
            @RequestParam(value = "useRag", defaultValue = "false") boolean useRag
    ) throws Exception {

        String filename = normalizeFilename(file.getOriginalFilename());
        log.info("文档对话（同步）| 文件: {} | 大小: {} bytes | 模型: {}", filename, file.getSize(), model);

        validateFileType(filename);
        Path tmp = saveToTemp(file, filename);
        try {
            String content;
            try {
                content = extractText(tmp.toFile(), filename);
            } catch (Exception e) {
                log.error("文档解析失败: {}", e.getMessage(), e);
                return ResponseEntity.internalServerError()
                        .body("文档解析失败：" + e.getMessage());
            }
            String answer  = executeChat(chatId, message, model, content, filename, useRag, false, null);
            return ResponseEntity.ok(answer);
        } finally {
            deleteSilently(tmp);
        }
    }

    // =====================================================================
    // 文件流上传 - SSE 流式
    // =====================================================================

    /**
     * 文件流上传对话（SSE 流式）
     */
    @PostMapping(value = "/stream", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
                 produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter documentStream(
            @RequestParam("file") MultipartFile file,
            @RequestParam("message") String message,
            @RequestParam(value = "model", defaultValue = "glm-4.6") String model,
            @RequestParam(value = "chatId", defaultValue = "") String chatId,
            @RequestParam(value = "useRag", defaultValue = "false") boolean useRag
    ) throws Exception {

        String filename = normalizeFilename(file.getOriginalFilename());
        log.info("文档对话（流式）| 文件: {} | 大小: {} bytes | 模型: {}", filename, file.getSize(), model);

        validateFileType(filename);
        Path tmp = saveToTemp(file, filename);

        SseEmitter emitter = new SseEmitter(180_000L);
        String content;
        try {
            content = extractText(tmp.toFile(), filename);
        } catch (Exception e) {
            deleteSilently(tmp);
            emitter.completeWithError(e);
            return emitter;
        }

        final Path tmpRef = tmp;
        executeChat(chatId, message, model, content, filename, useRag, true, emitter);
        emitter.onCompletion(() -> deleteSilently(tmpRef));
        emitter.onTimeout(() -> deleteSilently(tmpRef));
        return emitter;
    }

    // =====================================================================
    // 本地文件路径 - 同步
    // =====================================================================

    /**
     * 本地文件路径对话（同步）
     * 请求体：{ filePath, message, model, chatId, useRag }
     */
    @PostMapping("/chat/path")
    public ResponseEntity<String> documentChatByPath(@RequestBody PathChatRequest req) throws Exception {
        String filePath = req.getFilePath();
        log.info("文档对话（路径同步）| 路径: {} | 模型: {}", filePath, req.getModel());

        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            return ResponseEntity.badRequest().body("文件不存在：" + filePath);
        }
        String filename = path.getFileName().toString();
        validateFileType(filename);

        String content = extractText(path.toFile(), filename);
        String answer  = executeChat(req.getChatId(), req.getMessage(),
                req.getModel() != null ? req.getModel() : "glm-4.6",
                content, filename, req.isUseRag(), false, null);
        return ResponseEntity.ok(answer);
    }

    // =====================================================================
    // 本地文件路径 - SSE 流式
    // =====================================================================

    /**
     * 本地文件路径对话（SSE 流式）
     */
    @PostMapping(value = "/stream/path", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter documentStreamByPath(@RequestBody PathChatRequest req) throws Exception {
        String filePath = req.getFilePath();
        log.info("文档对话（路径流式）| 路径: {} | 模型: {}", filePath, req.getModel());

        Path path = Paths.get(filePath);
        SseEmitter emitter = new SseEmitter(180_000L);
        if (!Files.exists(path)) {
            try {
                emitter.send(SseEmitter.event().name("error").data("文件不存在：" + filePath));
                emitter.complete();
            } catch (Exception ignore) { }
            return emitter;
        }

        String filename = path.getFileName().toString();
        validateFileType(filename);
        String content = extractText(path.toFile(), filename);
        executeChat(req.getChatId(), req.getMessage(),
                req.getModel() != null ? req.getModel() : "glm-4.6",
                content, filename, req.isUseRag(), true, emitter);
        return emitter;
    }

    // =====================================================================
    // 临时文件上传接口（供 MCP Hub 文件上传使用）
    // =====================================================================

    /**
     * 上传文件到服务器临时目录，返回文件绝对路径
     * MCP 工具（read_file 等）可直接使用该路径
     */
    @PostMapping(value = "/upload/temp", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadTemp(
            @RequestParam("file") MultipartFile file) throws IOException {
        String filename = normalizeFilename(file.getOriginalFilename());
        Path tmp = Files.createTempFile("linglong_upload_", "_" + filename);
        file.transferTo(tmp.toFile());
        log.info("临时文件已上传: {}", tmp.toAbsolutePath());
        return ResponseEntity.ok(Map.of("path", tmp.toAbsolutePath().toString(), "filename", filename));
    }

    // =====================================================================
    // 核心处理逻辑
    // =====================================================================

    /**
     * 统一的文档对话处理（同步 / 流式均调用此方法）
     *
     * @param chatId       会话 ID（为空则不记录历史）
     * @param userMessage  用户问题
     * @param model        模型名称
     * @param docContent   从文档提取的文本
     * @param filename     文件名（展示用）
     * @param useRag       是否从向量库检索补充上下文
     * @param streaming    是否流式输出
     * @param emitter      流式时的 SseEmitter（同步时传 null）
     * @return 同步时返回回答文本，流式时返回 null
     */
    private String executeChat(String chatId, String userMessage, String model,
                                String docContent, String filename, boolean useRag,
                                boolean streaming, SseEmitter emitter) {
        // 1. 截取文档内容（防超出 LLM 上下文限制）
        String truncated = docContent;
        if (docContent.length() > 50_000) {
            truncated = docContent.substring(0, 50_000) + "\n\n[...文档内容过长，已截取前 50000 字符...]";
        }

        // 2. 构建系统提示词（文档内容 + RAG 上下文）
        String systemPrompt = buildSystemPrompt(truncated, filename, useRag ? retrieveRagContext(userMessage) : "");

        // 3. 构建 messages（system + 历史 + 当前 user）
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        if (chatId != null && !chatId.isBlank()) {
            messages.addAll(chatHistoryService != null
                    ? chatHistoryService.buildContextMessages(chatId)
                    : java.util.Collections.emptyList());
        }
        messages.add(Map.of("role", "user", "content", userMessage));

        // 4. 保存用户消息
        if (chatId != null && !chatId.isBlank() && chatHistoryService != null) {
            chatHistoryService.saveMessage(chatId, "user", userMessage, model, filename);
        }

        if (streaming) {
            // 5a. 流式调用
            multiModelChatService.streamChat(messages, model, 0.7, 4000, emitter,
                    fullContent -> {
                        if (chatId != null && !chatId.isBlank() && chatHistoryService != null) {
                            chatHistoryService.saveMessage(chatId, "assistant", fullContent, model, null);
                        }
                    });
            return null;
        } else {
            // 5b. 同步调用
            String answer = multiModelChatService.chat(messages, model, 0.7, 4000);
            if (chatId != null && !chatId.isBlank() && chatHistoryService != null) {
                chatHistoryService.saveMessage(chatId, "assistant", answer, model, null);
            }
            return answer;
        }
    }

    /**
     * 从向量数据库检索与问题相关的文档片段
     */
    private String retrieveRagContext(String question) {
        if (vectorStoreService == null) return "";
        try {
            List<Document> docs = vectorStoreService.similaritySearch(question, 3);
            if (docs.isEmpty()) return "";
            return "\n\n【知识库参考资料】\n" + docs.stream()
                    .map(Document::getContent)
                    .collect(Collectors.joining("\n\n"));
        } catch (Exception e) {
            log.warn("RAG 检索失败: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt(String docContent, String fileName, String ragContext) {
        return """
                你是一个专业的文档分析助手，当前分析的文件名为：**%s**

                以下是文档的完整内容：

                ---文档开始---
                %s
                ---文档结束---
                %s
                请严格基于以上文档内容回答用户的问题，并以规范的 **Markdown 格式** 输出：
                - 使用标题（`#` `##` `###`）组织层次结构
                - 使用有序或无序列表（`1.` `-`）呈现要点
                - 代码或配置使用代码块（` ``` `）包裹并标注语言
                - 重要内容使用 `**加粗**` 或 `*斜体*` 强调
                - 如有表格数据，使用 Markdown 表格格式
                - 如文档中没有相关信息，请如实告知用户
                """.formatted(fileName, docContent,
                ragContext.isBlank() ? "" : "\n" + ragContext + "\n");
    }

    // =====================================================================
    // 文档解析工具方法
    // =====================================================================

    /**
     * 使用 Apache Tika 提取文件文本内容
     */
    private String extractText(File file, String filename) throws Exception {
        String mimeType = tika.detect(file);
        log.info("文件类型识别: {} -> {}", filename, mimeType);

        // 复杂二进制格式（Office/PDF）使用 AutoDetectParser 深度解析
        if (mimeType != null && (
                mimeType.contains("pdf")          ||
                mimeType.contains("word")         ||
                mimeType.contains("officedocument") ||
                mimeType.contains("opendocument") ||
                mimeType.contains("powerpoint")   ||
                mimeType.contains("excel")        ||
                mimeType.contains("spreadsheet")  ||
                mimeType.contains("presentation") ||
                mimeType.contains("rtf")          ||
                mimeType.contains("msword"))) {
            return parseWithTikaParser(file);
        }

        // 纯文本格式直接读取
        try {
            return Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("UTF-8 读取失败，降级使用 Tika 解析: {}", e.getMessage());
            return parseWithTikaParser(file);
        }
    }

    /** 使用 Apache Tika AutoDetectParser 解析文档 */
    private String parseWithTikaParser(File file) throws Exception {
        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler(-1);
        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();
        try (InputStream stream = new FileInputStream(file)) {
            parser.parse(stream, handler, metadata, context);
        }
        String text = handler.toString().trim();
        if (text.isBlank()) {
            throw new RuntimeException("无法提取文档内容，请检查文件格式是否受支持。");
        }
        return text;
    }

    /** 校验文件扩展名 */
    private void validateFileType(String filename) {
        String lower = filename.toLowerCase();
        for (String ext : SUPPORTED_EXTENSIONS) {
            if (lower.endsWith(ext)) return;
        }
        throw new IllegalArgumentException(
                "不支持的文件类型：" + getExtension(filename) +
                "。支持：PDF、Word、Excel、PPT、RTF、Markdown、TXT、XML、HTML、JSON、CSV 等");
    }

    /** 保存上传文件到临时目录 */
    private Path saveToTemp(MultipartFile file, String filename) throws IOException {
        Path tmp = Files.createTempFile("linglong_doc_", getExtension(filename));
        file.transferTo(tmp.toFile());
        log.debug("临时文件已保存: {}", tmp);
        return tmp;
    }

    /** 静默删除临时文件 */
    private void deleteSilently(Path path) {
        if (path == null) return;
        try { Files.deleteIfExists(path); } catch (Exception ignore) { }
    }

    private String normalizeFilename(String original) {
        return original != null ? original : "document";
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : ".tmp";
    }

    // =====================================================================
    // DTO
    // =====================================================================

    public static class PathChatRequest {
        private String filePath;
        private String message;
        private String model;
        private String chatId;
        private boolean useRag = false;

        public String getFilePath()  { return filePath; }
        public void setFilePath(String v)  { filePath = v; }
        public String getMessage()   { return message; }
        public void setMessage(String v)   { message = v; }
        public String getModel()     { return model; }
        public void setModel(String v)     { model = v; }
        public String getChatId()    { return chatId; }
        public void setChatId(String v)    { chatId = v; }
        public boolean isUseRag()    { return useRag; }
        public void setUseRag(boolean v)   { useRag = v; }
    }
}
