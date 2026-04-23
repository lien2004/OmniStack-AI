package com.linglong.agent.agents;

import com.linglong.agent.core.Agent;
import com.linglong.agent.model.AgentContext;
import com.linglong.agent.model.AgentResult;
import com.linglong.agent.llm.LLMService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 部署Agent
 * 生成部署配置和脚本
 */
@Component
public class DeploymentAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(DeploymentAgent.class);

    private final LLMService llmService;

    public DeploymentAgent(LLMService llmService) {
        this.llmService = llmService;
    }

    @Override
    public String getName() {
        return "DeploymentAgent";
    }

    @Override
    public String getDescription() {
        return "部署Agent - 生成Dockerfile、K8s配置、CI/CD脚本";
    }

    @Override
    public String getSystemPrompt() {
        return """
                你是一位资深的DevOps工程师，精通Docker、Kubernetes和CI/CD流程。
                
                你的任务是：
                1. 生成Dockerfile和.dockerignore
                2. 生成Docker Compose配置
                3. 生成Kubernetes部署清单
                4. 生成CI/CD流水线配置(GitHub Actions/GitLab CI)
                5. 生成环境配置文件
                6. 编写部署脚本和运维脚本
                
                技术栈：
                - Docker：多阶段构建、安全最佳实践
                - Kubernetes：Deployment、Service、ConfigMap、Secret
                - Helm：Chart模板和Values配置
                - CI/CD：自动化构建、测试、部署
                
                输出格式要求：
                - 每个配置文件使用代码块包裹
                - 文件顶部标明文件路径
                - 包含详细的注释说明
                - 提供部署步骤说明
                
                最佳实践：
                - 使用非root用户运行容器
                - 优化镜像大小(多阶段构建)
                - 配置健康检查和资源限制
                - 使用ConfigMap和Secret管理配置
                """;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("[{}] 开始执行部署配置生成任务", getName());

            String input = buildPromptFromContext(context);
            String result = llmService.generate(getSystemPrompt(), input);

            // 将结果存入共享上下文
            context.addToShared("deploymentConfig", result);

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("[{}] 部署配置生成完成，耗时: {}ms", getName(), executionTime);

            AgentResult agentResult = new AgentResult();
            agentResult.setSuccess(true);
            agentResult.setOutput(result);
            agentResult.setExecutionTime(executionTime);
            return agentResult;
        } catch (Exception e) {
            log.error("[{}] 部署配置生成失败", getName(), e);
            return AgentResult.error("部署配置生成失败: " + e.getMessage());
        }
    }

    @Override
    public Flux<String> executeStream(AgentContext context) {
        log.info("[{}] 开始流式执行部署配置生成任务", getName());
        String input = buildPromptFromContext(context);
        return llmService.generateStream(getSystemPrompt(), input);
    }

    @Override
    public AgentType getType() {
        return AgentType.DEPLOYMENT;
    }

    private String buildPromptFromContext(AgentContext context) {
        StringBuilder prompt = new StringBuilder();

        // 添加生成的代码
        String generatedCode = context.getFromShared("generatedCode");
        if (generatedCode != null) {
            prompt.append("## 项目代码\n\n").append(generatedCode).append("\n\n");
        }

        // 添加架构设计
        String architecture = context.getFromShared("architecture");
        if (architecture != null) {
            prompt.append("## 架构设计\n\n").append(architecture).append("\n\n");
        }

        // 添加用户输入
        if (context.getInput() != null) {
            prompt.append("## 额外要求\n\n").append(context.getInput());
        }

        return prompt.toString();
    }
}
