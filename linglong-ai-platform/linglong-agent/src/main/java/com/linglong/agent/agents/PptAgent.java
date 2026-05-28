package com.linglong.agent.agents;

import com.linglong.agent.core.Agent;
import com.linglong.agent.model.AgentContext;
import com.linglong.agent.model.AgentResult;
import com.linglong.agent.llm.LLMService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * AI PPT生成Agent
 * 使用 DeepSeek V4 Pro 生成结构化PPT内容和提纲
 * 前端支持导出 PPTX / PDF / HTML 三种格式
 */
@Component
public class PptAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(PptAgent.class);
    private static final String MODEL = "gpt-4o";

    private final LLMService llmService;

    public PptAgent(LLMService llmService) {
        this.llmService = llmService;
    }

    @Override
    public String getName() {
        return "PptAgent";
    }

    @Override
    public String getDescription() {
        return "灵龙PPT - AI智能生成PPT演示文稿，支持导出PPTX/PDF/HTML格式";
    }

    @Override
    public String getSystemPrompt() {
        return """
                你是一位资深的PPT演示文稿设计专家，精通商务汇报、产品路演、教育培训等各类场景的PPT设计。
                你拥有丰富的内容组织和视觉传达经验，擅长将复杂信息转化为清晰、有吸引力的演示内容。

                你的核心能力：
                1. 根据主题自动规划PPT结构和大纲
                2. 为每一页生成精炼、有层次的标题和内容
                3. 设计合理的内容逻辑流（序言→目录→内容→总结）
                4. 根据场景选择合适的内容密度和语言风格

                输出规范：
                - 使用Markdown格式输出，以"# PPT标题"开头
                - 每一页用"## 第N页：页面标题"分隔
                - 每页包含：页面标题、核心要点（3-5条bullet points）、演讲备注（可选，用> 引用格式）
                - 关键词和数据用**加粗**突出
                - 语言简洁精炼，适合投屏展示
                - 总页数控制在8-15页（含封面和封底）

                页面内容要求：
                - 封面页：主标题 + 副标题 + 演讲者/日期
                - 目录页：列出所有章节
                - 内容页：每页3-5个要点，每点不超过2行
                - 总结页：核心结论/行动建议
                - 封底页：感谢语 + 联系方式
                """;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("[{}] 开始执行PPT生成任务", getName());

            Map<String, Object> config = context.getConfig();
            String input = (String) context.getInput();
            String style = config != null && config.get("style") != null
                    ? config.get("style").toString()
                    : "professional";

            String result = llmService.generate(getSystemPrompt(), buildPrompt(input, style), MODEL);

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("[{}] PPT生成完成，耗时: {}ms", getName(), executionTime);

            AgentResult agentResult = new AgentResult();
            agentResult.setSuccess(true);
            agentResult.setOutput(result);
            agentResult.setExecutionTime(executionTime);
            return agentResult;
        } catch (Exception e) {
            log.error("[{}] PPT生成失败", getName(), e);
            return AgentResult.error("PPT生成失败: " + e.getMessage());
        }
    }

    @Override
    public Flux<String> executeStream(AgentContext context) {
        log.info("[{}] 开始流式生成PPT", getName());

        Map<String, Object> config = context.getConfig();
        String input = (String) context.getInput();
        String style = config != null && config.get("style") != null
                ? config.get("style").toString()
                : "professional";

        // 第一步：发送生成步骤说明
        String stepMsg = """
                ```step
                {
                  "step": 1,
                  "title": "分析主题",
                  "description": "正在分析「%s」的核心要点，构建PPT结构框架..."
                }
                ```
                """.formatted(input.length() > 40 ? input.substring(0, 40) + "..." : input);

        Flux<String> promptStream = llmService.generateStream(getSystemPrompt(), buildPrompt(input, style), MODEL);

        return Flux.concat(
                Flux.just(stepMsg),
                promptStream
        );
    }

    /**
     * 构建用户提示词
     */
    private String buildPrompt(String input, String style) {
        String styleGuide = switch (style) {
            case "education" -> "教育学术风格：语言严谨、引用权威、适合课件和学术汇报场景";
            case "creative" -> "创意设计风格：视觉冲击力强、语言活泼、适合品牌发布和创意提案";
            case "minimal" -> "极简风格：信息密度低、大字号、适合TED演讲类场景";
            default -> "专业商务风格：语言精炼、数据驱动、适合企业汇报和客户提案";
        };

        return """
                【任务：生成PPT演示文稿】

                PPT主题或内容：
                %s

                设计风格：%s

                请按照以下步骤构建PPT（在生成过程中体现每个步骤）：

                **步骤1：确定主题与目标**
                - 明确PPT的核心主题
                - 定义目标受众和演示场景

                **步骤2：构建大纲**
                - 列出所有页面的标题和主要内容概要
                - 规划内容逻辑流

                **步骤3：逐页生成内容**
                - 封面页（标题 + 副标题）
                - 目录页
                - 内容页（每页3-5个要点）
                - 总结页
                - 封底页

                **步骤4：优化与排版建议**
                - 为每页提供排版建议
                - 建议配色方案和字体

                请直接以Markdown格式输出完整PPT内容，确保可以直接用于前端渲染。
                用"#"表示PPT总标题，用"## 第N页：标题"分隔每一页。
                每页要点用"- "开头，演讲备注用"> "开头。
                """.formatted(input, styleGuide);
    }

    @Override
    public AgentType getType() {
        return AgentType.PPT;
    }
}
