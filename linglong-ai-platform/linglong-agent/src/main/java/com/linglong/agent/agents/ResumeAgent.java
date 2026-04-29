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
 * AI简历生成Agent
 * 支持：简历生成、简历测评、一键优化
 * 使用gpt-5.5模型
 */
@Component
public class ResumeAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(ResumeAgent.class);
    private static final String MODEL = "glm-4.6";

    private final LLMService llmService;

    public ResumeAgent(LLMService llmService) {
        this.llmService = llmService;
    }

    @Override
    public String getName() {
        return "ResumeAgent";
    }

    @Override
    public String getDescription() {
        return "AI简历助手 - 智能生成简历、专业测评分析、一键优化完善";
    }

    @Override
    public String getSystemPrompt() {
        return """
                你是一位资深的人力资源专家和职业规划顾问，拥有20年简历筛选与优化经验。
                你熟悉互联网、金融、制造、咨询等各行业的招聘标准，精通STAR法则和量化表达技巧。

                你的核心能力：
                1. 简历生成：根据用户提供的基本信息，生成结构清晰、重点突出、数据量化的专业简历
                2. 简历测评：从HR视角深度分析简历的优缺点，指出与目标岗位的匹配度
                3. 简历优化：针对测评发现的问题，提供具体、可执行的修改建议并直接输出优化后的简历

                输出规范：
                - 使用Markdown格式输出
                - 中文为主，专有名词可保留英文
                - 简历内容必须包含：个人信息、求职意向、教育背景、工作经历、项目经验、专业技能、自我评价
                - 工作经历必须使用STAR法则，尽量量化成果（提升XX%、降低XX成本、服务XX用户等）
                - 测评报告需分维度评分（内容充实度、结构清晰度、数据量化度、岗位匹配度、亮点突出度）
                """;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("[{}] 开始执行简历任务", getName());

            // 从config获取操作模式，默认为generate
            Map<String, Object> config = context.getConfig();
            String mode = config != null && config.get("mode") != null
                    ? config.get("mode").toString()
                    : "generate";

            String input = (String) context.getInput();
            String result;

            switch (mode) {
                case "evaluate":
                    result = evaluateResume(input);
                    break;
                case "optimize":
                    // 从config获取测评反馈用于优化
                    String feedback = config != null && config.get("feedback") != null
                            ? config.get("feedback").toString()
                            : "";
                    result = optimizeResume(input, feedback);
                    break;
                case "generate":
                default:
                    result = generateResume(input);
                    break;
            }

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("[{}] 简历任务完成，模式: {}, 耗时: {}ms", getName(), mode, executionTime);

            AgentResult agentResult = new AgentResult();
            agentResult.setSuccess(true);
            agentResult.setOutput(result);
            agentResult.setExecutionTime(executionTime);
            return agentResult;
        } catch (Exception e) {
            log.error("[{}] 简历任务失败", getName(), e);
            return AgentResult.error("简历任务失败: " + e.getMessage());
        }
    }

    @Override
    public Flux<String> executeStream(AgentContext context) {
        log.info("[{}] 开始流式执行简历任务", getName());

        Map<String, Object> config = context.getConfig();
        String mode = config != null && config.get("mode") != null
                ? config.get("mode").toString()
                : "generate";
        String input = (String) context.getInput();

        String systemPrompt = getSystemPrompt();
        String userPrompt;

        switch (mode) {
            case "evaluate":
                userPrompt = buildEvaluatePrompt(input);
                break;
            case "optimize":
                String feedback = config != null && config.get("feedback") != null
                        ? config.get("feedback").toString()
                        : "";
                userPrompt = buildOptimizePrompt(input, feedback);
                break;
            case "generate":
            default:
                userPrompt = buildGeneratePrompt(input);
                break;
        }

        return llmService.generateStream(systemPrompt, userPrompt, MODEL);
    }

    private String generateResume(String input) {
        String userPrompt = buildGeneratePrompt(input);
        return llmService.generate(getSystemPrompt(), userPrompt, MODEL);
    }

    private String evaluateResume(String input) {
        String userPrompt = buildEvaluatePrompt(input);
        return llmService.generate(getSystemPrompt(), userPrompt, MODEL);
    }

    private String optimizeResume(String resume, String feedback) {
        String userPrompt = buildOptimizePrompt(resume, feedback);
        return llmService.generate(getSystemPrompt(), userPrompt, MODEL);
    }

    private String buildGeneratePrompt(String input) {
        return """
                【任务：生成专业简历】

                请根据以下用户信息，生成一份完整、专业、有竞争力的简历。
                简历必须结构清晰、重点突出、数据量化，符合现代HR的阅读习惯。

                用户信息：
                %s

                请直接输出Markdown格式的完整简历内容，包含以下板块：
                1. 个人信息（姓名、联系方式、邮箱、意向岗位、意向城市）
                2. 求职意向
                3. 教育背景（学校、专业、学历、时间，如有GPA或荣誉可添加）
                4. 工作经历（按时间倒序，每段使用STAR法则，必须有量化数据）
                5. 项目经验（项目名称、角色、技术栈、成果量化）
                6. 专业技能（分类列出，如编程语言、框架工具、软技能等）
                7. 自我评价（3-4句话，突出核心竞争力）

                如果用户信息不完整，请基于合理推测补充完善，并在简历末尾标注"【AI补充说明】"列出推测内容。
                """.formatted(input);
    }

    private String buildEvaluatePrompt(String input) {
        return """
                【任务：专业简历测评】

                请对以下简历进行深度专业测评，从HR和用人部门双视角分析。

                待测评简历：
                %s

                测评要求：
                1. 总体评分（百分制）
                2. 分维度评分（每项满分10分）：
                   - 内容充实度
                   - 结构清晰度
                   - 数据量化度
                   - 岗位匹配度
                   - 亮点突出度
                3. 优势分析（列出3-5个核心亮点）
                4. 不足分析（列出3-5个关键问题，按严重程度排序）
                5. 具体修改建议（每个问题给出1-2条可执行的修改方案）
                6. 求职竞争力评估（简述该简历在目标岗位中的竞争力水平）

                输出格式使用Markdown，层次分明，便于阅读。
                """.formatted(input);
    }

    private String buildOptimizePrompt(String resume, String feedback) {
        return """
                【任务：简历一键优化】

                请根据以下简历和测评反馈，输出优化后的完整简历。

                原始简历：
                %s

                测评反馈与修改建议：
                %s

                优化要求：
                1. 针对测评反馈中的所有问题进行修改
                2. 保持简历原有真实信息不变，只优化表达方式和结构
                3. 增强数据量化表达，使用STAR法则完善经历描述
                4. 突出与目标岗位最匹配的技能和经验
                5. 优化排版结构，使HR能在10秒内抓住核心信息
                6. 直接输出完整的优化后简历（Markdown格式）
                7. 在末尾添加"【优化说明】"简要列出本次优化的核心改进点

                请输出完整的优化后简历，不要省略任何板块。
                """.formatted(resume, feedback.isEmpty() ? "请基于专业标准进行全面优化" : feedback);
    }

    @Override
    public AgentType getType() {
        return AgentType.RESUME;
    }
}
