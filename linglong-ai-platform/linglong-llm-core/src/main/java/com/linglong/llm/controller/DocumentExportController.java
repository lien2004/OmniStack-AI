package com.linglong.llm.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.poi.xwpf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 对话导出控制器
 * 将对话消息列表导出为格式化的 .docx 文档，支持 Markdown 内容渲染。
 */
@Tag(name = "对话导出", description = "将对话记录导出为 DOCX 文件，支持 Markdown 格式渲染")
@RestController
@RequestMapping("/ai/document")
public class DocumentExportController {

    private static final Logger log = LoggerFactory.getLogger(DocumentExportController.class);

    /** 内联 Markdown 匹配：**bold** | *italic* | `code` */
    private static final Pattern INLINE_MD = Pattern.compile(
            "\\*\\*(.+?)\\*\\*|(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)|`([^`]+?)`");

    // ===== 请求 DTO =====

    public static class ExportRequest {
        private String title;
        private List<MessageItem> messages;

        public String getTitle() { return title; }
        public void setTitle(String t) { this.title = t; }
        public List<MessageItem> getMessages() { return messages; }
        public void setMessages(List<MessageItem> m) { this.messages = m; }

        public static class MessageItem {
            private String role;
            private String content;
            private String timestamp;
            private String model;
            private String fileAttachment;
            private Boolean loading;

            public String getRole() { return role; }
            public void setRole(String r) { this.role = r; }
            public String getContent() { return content; }
            public void setContent(String c) { this.content = c; }
            public String getTimestamp() { return timestamp; }
            public void setTimestamp(String t) { this.timestamp = t; }
            public String getModel() { return model; }
            public void setModel(String m) { this.model = m; }
            public String getFileAttachment() { return fa; }
            public void setFileAttachment(String fa) { this.fa = fa; }
            private String fa;
            public Boolean getLoading() { return loading; }
            public void setLoading(Boolean l) { this.loading = l; }
        }
    }

    // ===== 接口 =====

    @Operation(summary = "导出对话为 DOCX", description = "传入对话消息列表，返回 .docx 文件下载流")
    @PostMapping("/export")
    public ResponseEntity<byte[]> exportConversation(
            @RequestBody ExportRequest request) throws Exception {

        log.info("导出对话 DOCX | 标题: {} | 消息数: {}",
                request.getTitle(),
                request.getMessages() != null ? request.getMessages().size() : 0);

        try (XWPFDocument doc = buildDocument(request);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            doc.write(out);
            byte[] bytes = out.toByteArray();

            String safeTitle = (request.getTitle() != null ? request.getTitle() : "对话记录")
                    .replaceAll("[\\\\/:*?\"<>|]", "_");
            String filename = URLEncoder.encode(safeTitle + ".docx", StandardCharsets.UTF_8)
                    .replace("+", "%20");

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename*=UTF-8''" + filename)
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                    .body(bytes);
        }
    }

    // ===== 文档构建 =====

    private XWPFDocument buildDocument(ExportRequest request) {
        XWPFDocument doc = new XWPFDocument();

        // 文档标题
        styledPara(doc, "灵龙AI  对话记录", 22, "1677FF", true, ParagraphAlignment.CENTER, 0, 80);

        // 会话标题
        String convTitle = request.getTitle() != null ? request.getTitle().trim() : "";
        if (!convTitle.isBlank()) {
            styledPara(doc, convTitle, 13, "595959", false, ParagraphAlignment.CENTER, 0, 80);
        }

        // 导出时间
        String exportTime = "导出时间：" + LocalDateTime.now(ZoneId.of("Asia/Shanghai"))
                .format(DateTimeFormatter.ofPattern("yyyy年MM月dd日  HH:mm"));
        styledPara(doc, exportTime, 10, "8C8C8C", false, ParagraphAlignment.CENTER, 0, 160);

        // 分隔线
        styledPara(doc, "─".repeat(56), 8, "D9D9D9", false, ParagraphAlignment.CENTER, 0, 200);

        // 消息列表
        if (request.getMessages() != null) {
            for (ExportRequest.MessageItem msg : request.getMessages()) {
                if (Boolean.TRUE.equals(msg.getLoading())) continue;
                if (msg.getContent() == null || msg.getContent().isBlank()) continue;
                addMessageBlock(doc, msg);
            }
        }

        // 页脚
        styledPara(doc, "─".repeat(56), 8, "D9D9D9", false, ParagraphAlignment.CENTER, 200, 80);
        styledPara(doc, "由 灵龙AI 智能平台 生成", 9, "BFBFBF", false, ParagraphAlignment.CENTER, 0, 0);

        return doc;
    }

    private void addMessageBlock(XWPFDocument doc, ExportRequest.MessageItem msg) {
        boolean isUser = "user".equals(msg.getRole());
        ParagraphAlignment align = isUser ? ParagraphAlignment.RIGHT : ParagraphAlignment.LEFT;
        String roleColor = isUser ? "1677FF" : "13C2C2";

        // 角色 + 时间行
        String timeStr = fmtTime(msg.getTimestamp());
        String roleLabel = (isUser ? "▶  我" : "◆  灵龙AI")
                + (msg.getModel() != null && !isUser ? "  [" + msg.getModel() + "]" : "")
                + (timeStr.isBlank() ? "" : "  " + timeStr);
        styledPara(doc, roleLabel, 10, roleColor, true, align, 240, 60);

        // 文件附件标识
        String fa = msg.getFileAttachment();
        if (fa != null && !fa.isBlank()) {
            XWPFParagraph p = doc.createParagraph();
            p.setAlignment(align);
            p.setSpacingAfter(40);
            XWPFRun r = p.createRun();
            r.setText("📎  " + fa);
            r.setFontSize(10);
            r.setColor("1677FF");
            r.setItalic(true);
        }

        // 内容
        if (isUser) {
            XWPFParagraph p = doc.createParagraph();
            p.setAlignment(ParagraphAlignment.RIGHT);
            p.setSpacingAfter(240);
            XWPFRun r = p.createRun();
            r.setText(msg.getContent());
            r.setFontSize(11);
            r.setColor("1A1A1A");
        } else {
            renderMarkdown(doc, msg.getContent());
            styledPara(doc, "", 6, "FFFFFF", false, ParagraphAlignment.LEFT, 0, 160);
        }
    }

    // ===== Markdown 渲染 =====

    private void renderMarkdown(XWPFDocument doc, String content) {
        String[] lines = content.split("\n");
        boolean inCode = false;
        StringBuilder codeBuf = new StringBuilder();

        for (String line : lines) {
            if (line.startsWith("```")) {
                if (inCode) {
                    addCodeBlock(doc, codeBuf.toString().trim());
                    codeBuf.setLength(0);
                    inCode = false;
                } else {
                    inCode = true;
                }
                continue;
            }
            if (inCode) { codeBuf.append(line).append("\n"); continue; }

            if (line.startsWith("### "))      addHeadingPara(doc, line.substring(4), 13);
            else if (line.startsWith("## "))  addHeadingPara(doc, line.substring(3), 15);
            else if (line.startsWith("# "))   addHeadingPara(doc, line.substring(2), 17);
            else if (line.startsWith("- ") || line.startsWith("* "))
                addBulletPara(doc, "•  " + line.substring(2));
            else if (line.matches("^\\d+\\.\\s.*"))
                addBulletPara(doc, line);
            else if (line.startsWith("> "))
                addQuotePara(doc, line.substring(2));
            else if (!line.isBlank())
                addNormalPara(doc, line);
        }
        if (inCode && codeBuf.length() > 0) addCodeBlock(doc, codeBuf.toString().trim());
    }

    private void addHeadingPara(XWPFDocument doc, String text, int size) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(120); p.setSpacingAfter(60);
        inlineRuns(p, text, size, "1A1A1A", true);
    }

    private void addBulletPara(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setIndentationLeft(360); p.setSpacingAfter(40);
        inlineRuns(p, text, 11, "1A1A1A", false);
    }

    private void addQuotePara(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setIndentationLeft(360); p.setSpacingAfter(60);
        XWPFRun r = p.createRun();
        r.setText("| " + stripMd(text));
        r.setFontSize(11); r.setColor("595959"); r.setItalic(true);
    }

    private void addCodeBlock(XWPFDocument doc, String code) {
        for (String line : code.split("\n")) {
            XWPFParagraph p = doc.createParagraph();
            p.setIndentationLeft(360); p.setSpacingAfter(0);
            XWPFRun r = p.createRun();
            r.setText(line.isEmpty() ? " " : line);
            r.setFontFamily("Courier New");
            r.setFontSize(10);
            r.setColor("7C3AED");
        }
        // Code block bottom spacing
        XWPFParagraph spacer = doc.createParagraph();
        spacer.setSpacingAfter(80);
        spacer.createRun().setText("");
    }

    private void addNormalPara(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingAfter(60);
        inlineRuns(p, text, 11, "1A1A1A", false);
    }

    // ===== 内联 Markdown 处理 =====

    private void inlineRuns(XWPFParagraph para, String text, int size, String color, boolean bold) {
        Matcher m = INLINE_MD.matcher(text);
        int last = 0;
        while (m.find()) {
            if (m.start() > last) run(para, text.substring(last, m.start()), size, color, bold, false, null);
            if (m.group(1) != null)      run(para, m.group(1), size, color, true,  false, null);
            else if (m.group(2) != null) run(para, m.group(2), size, color, bold,  true,  null);
            else if (m.group(3) != null) run(para, m.group(3), Math.max(size - 1, 9), "7C3AED", false, false, "Courier New");
            last = m.end();
        }
        if (last < text.length()) run(para, text.substring(last), size, color, bold, false, null);
    }

    private void run(XWPFParagraph para, String text, int size,
                     String color, boolean bold, boolean italic, String font) {
        XWPFRun r = para.createRun();
        r.setText(text);
        r.setFontSize(size);
        r.setColor(color);
        if (bold)   r.setBold(true);
        if (italic) r.setItalic(true);
        if (font != null) r.setFontFamily(font);
    }

    // ===== 工具方法 =====

    private void styledPara(XWPFDocument doc, String text, int size, String color,
                             boolean bold, ParagraphAlignment align, int before, int after) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(align);
        if (before > 0) p.setSpacingBefore(before);
        if (after  > 0) p.setSpacingAfter(after);
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setFontSize(size);
        r.setColor(color);
        if (bold) r.setBold(true);
    }

    private String stripMd(String text) {
        return text.replaceAll("\\*\\*(.+?)\\*\\*", "$1")
                   .replaceAll("\\*(.+?)\\*", "$1")
                   .replaceAll("`(.+?)`", "$1")
                   .replaceAll("~~(.+?)~~", "$1")
                   .replaceAll("\\[(.+?)]\\(.+?\\)", "$1");
    }

    private String fmtTime(String iso) {
        if (iso == null) return "";
        try {
            return DateTimeFormatter.ofPattern("HH:mm")
                    .withZone(ZoneId.of("Asia/Shanghai"))
                    .format(Instant.parse(iso));
        } catch (Exception e) { return ""; }
    }
}
