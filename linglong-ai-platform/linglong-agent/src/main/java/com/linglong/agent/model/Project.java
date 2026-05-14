package com.linglong.agent.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 项目实体类
 * 用于项目中心管理，记录AI全智能开发的项目信息和生成代码
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project {

    /** 项目ID */
    private String id;

    /** 项目名称 */
    private String name;

    /** 项目描述 */
    private String description;

    /** 项目状态: init, analyzing, designing, coding, testing, completed, failed */
    private String status;

    /** 技术栈 */
    private String techStack;

    /** 架构类型 */
    private String architectureType;

    /** 使用的AI模型 */
    private String model;

    /** 需求文档（原始需求描述） */
    private String requirement;

    /** 关联的工作流ID */
    private String workflowId;

    /** 创建者用户ID */
    private String userId;

    /** 生成的代码文件（JSON格式存储） */
    private String codeFiles;

    /** 工作流执行输出（JSON格式存储各步骤输出） */
    private String workflowOutput;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
