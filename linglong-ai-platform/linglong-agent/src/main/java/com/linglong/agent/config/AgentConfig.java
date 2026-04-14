package com.linglong.agent.config;

import com.linglong.agent.core.Agent;
import com.linglong.agent.orchestrator.AgentOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * Agent配置类
 * 自动注册所有Agent到Orchestrator
 */
@Configuration
public class AgentConfig {

    private static final Logger log = LoggerFactory.getLogger(AgentConfig.class);

    private final AgentOrchestrator orchestrator;
    private final List<Agent> agents;

    public AgentConfig(AgentOrchestrator orchestrator, List<Agent> agents) {
        this.orchestrator = orchestrator;
        this.agents = agents;
    }

    @PostConstruct
    public void init() {
        log.info("开始注册Agent，共 {} 个", agents.size());
        
        for (Agent agent : agents) {
            orchestrator.registerAgent(agent);
            log.info("Agent已注册: {} [{}]", agent.getName(), agent.getType());
        }
        
        log.info("所有Agent注册完成");
    }
}
