#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成灵龙AI智能平台项目进展汇报Word文档"""

from docx import Document
from docx.shared import Pt, RGBColor, Cm, Inches
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.enum.style import WD_STYLE_TYPE
from docx.oxml.ns import qn
from docx.oxml import OxmlElement

def set_cell_shading(cell, color):
    """设置单元格背景色"""
    shading = OxmlElement('w:shd')
    shading.set(qn('w:fill'), color)
    cell._tc.get_or_add_tcPr().append(shading)

def set_cell_border(cell, **kwargs):
    """设置单元格边框"""
    tc = cell._tc
    tcPr = tc.get_or_add_tcPr()
    tcBorders = OxmlElement('w:tcBorders')
    for edge in ('top', 'left', 'bottom', 'right'):
        edge_data = kwargs.get(edge)
        if edge_data:
            tag = f'w:{edge}'
            element = OxmlElement(tag)
            element.set(qn('w:val'), edge_data.get('val'))
            element.set(qn('w:sz'), edge_data.get('sz'))
            element.set(qn('w:color'), edge_data.get('color'))
            tcBorders.append(element)
    tcPr.append(tcBorders)

def create_heading_style(doc, style_name, font_size, font_name='黑体', bold=True, color=None):
    """创建标题样式"""
    style = doc.styles.add_style(style_name, WD_STYLE_TYPE.PARAGRAPH)
    style.font.size = Pt(font_size)
    style.font.name = font_name
    style.font.bold = bold
    style.element.rPr.rFonts.set(qn('w:eastAsia'), font_name)
    if color:
        style.font.color.rgb = color
    return style

def main():
    doc = Document()
    
    # 设置全局字体
    doc.styles['Normal'].font.name = '微软雅黑'
    doc.styles['Normal'].element.rPr.rFonts.set(qn('w:eastAsia'), '微软雅黑')
    doc.styles['Normal'].font.size = Pt(10.5)
    
    # 标题
    title = doc.add_heading('灵龙AI智能开发平台 — 项目进展汇报', level=0)
    title.alignment = WD_ALIGN_PARAGRAPH.CENTER
    title_run = title.runs[0]
    title_run.font.size = Pt(22)
    title_run.font.bold = True
    title_run.font.name = '黑体'
    title_run.element.rPr.rFonts.set(qn('w:eastAsia'), '黑体')
    title_run.font.color.rgb = RGBColor(0x1F, 0x4E, 0x79)
    
    # 基本信息
    doc.add_paragraph()
    info_para = doc.add_paragraph()
    info_para.alignment = WD_ALIGN_PARAGRAPH.CENTER
    info_para.add_run('汇报人：鲁鸿（组长）  |  汇报日期：2026年5月6日').font.size = Pt(11)
    info_para.add_run('\n项目周期：2026年3月30日 — 2026年6月19日（当前处于开发攻坚阶段）').font.size = Pt(11)
    
    # 一、项目整体概况
    h1 = doc.add_heading('一、项目整体概况', level=1)
    h1.runs[0].font.color.rgb = RGBColor(0x1F, 0x4E, 0x79)
    
    doc.add_paragraph('项目名称：灵龙AI智能开发平台（LingLong AI Platform）')
    doc.add_paragraph('项目定位：基于 AI + DDD 的智能软件开发平台，支持多智能体协作、MCP 协议集成及全生命周期软件研发辅助。')
    doc.add_paragraph('线上地址：http://8.137.117.129（已部署至阿里云生产环境）')
    doc.add_paragraph('团队规模：10人（前端5人 + 后端3人 + 测试2人）')
    
    # 技术架构
    doc.add_paragraph('技术架构：')
    tech_items = [
        '后端：Spring Boot 3.2 + Spring Cloud + Spring AI + LangChain4j + MyBatis-Plus + JWT',
        '前端：React 18 + Vite + TypeScript + Ant Design',
        '数据层：MySQL（业务数据）+ PostgreSQL/pgvector（向量知识库）+ Redis（缓存/会话）',
        'AI 层：智谱 AI GLM 系列 / 灵龙AI CodeFlow 系列 / GLM-Image 多模型集成'
    ]
    for item in tech_items:
        p = doc.add_paragraph(item, style='List Bullet')
    
    # 二、整体开发进度
    doc.add_heading('二、整体开发进度', level=1)
    
    # 进度表
    table = doc.add_table(rows=8, cols=4)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    headers = ['阶段', '计划周期', '实际进度', '状态']
    for i, header in enumerate(headers):
        cell = table.rows[0].cells[i]
        cell.text = header
        cell.paragraphs[0].alignment = WD_ALIGN_PARAGRAPH.CENTER
        cell.paragraphs[0].runs[0].font.bold = True
        cell.paragraphs[0].runs[0].font.color.rgb = RGBColor(255, 255, 255)
        set_cell_shading(cell, '1F4E79')
    
    data = [
        ['需求分析与功能模块设计', '3/30 — 4/3', '100%', '✅ 已完成'],
        ['AI 相关技术储备', '3/30 — 4/8', '100%', '✅ 已完成'],
        ['前后端技术选型', '4/9 — 4/10', '100%', '✅ 已完成'],
        ['UI 设计与业务逻辑设计', '4/13 — 4/17', '100%', '✅ 已完成'],
        ['后端核心功能开发', '4/20 — 6/12', '~75%', '🟡 进行中'],
        ['前端核心功能开发', '4/21 — 6/12', '~70%', '🟡 进行中'],
        ['系统测试与回归', '6/1 — 6/19', '0%', '⚪ 待启动'],
    ]
    
    for row_idx, row_data in enumerate(data, 1):
        for col_idx, cell_text in enumerate(row_data):
            cell = table.rows[row_idx].cells[col_idx]
            cell.text = cell_text
            cell.paragraphs[0].alignment = WD_ALIGN_PARAGRAPH.CENTER
            # 斑马纹
            if row_idx % 2 == 0:
                set_cell_shading(cell, 'F2F2F2')
    
    # 三、各岗位工作进展汇报
    doc.add_heading('三、各岗位工作进展汇报', level=1)
    
    # 3.1 前端开发组
    doc.add_heading('3.1 前端开发组（5人）', level=2)
    doc.add_paragraph('前端组已完成 React + Vite + TypeScript 工程体系搭建，累计输出 TS/TSX 源码 30+ 个页面/组件文件，约 6,000+ 行有效代码，已实现与后端 50+ 接口的全量联调。')
    
    frontend_members = [
        ('黄雨 — 前端工程负责人', [
            '负责前端工程化架构搭建（Vite + React 18 + TypeScript），配置 ESLint、路径别名、环境变量',
            '设计封装平台全局 Layout 布局组件、路由守卫、基于 Zustand 的全局状态管理',
            '搭建 Axios 请求封装层（含请求拦截、Token 注入、全局错误处理、Loading 状态管理）',
            '实现前端路由体系（含懒加载、权限控制、动态导航）'
        ]),
        ('周钞钞 — 用户系统前端开发', [
            '独立完成登录/注册/找回密码三套页面的 UI 开发与表单校验逻辑',
            '负责个人中心（Profile）页面开发，实现用户信息展示与资料维护',
            '负责管理员用户管理（AdminUsers）页面，实现用户列表查询、角色分配、分页管理'
        ]),
        ('王继辉 — AI 核心交互前端开发', [
            '负责 Chat AI 对话页（平台核心页面）：多模型选择器组件、SSE 流式消息实时渲染、Markdown 语法高亮、代码块复制、对话历史侧边栏、新建/删除/清空会话、对话导出 DOCX 功能',
            '负责 Dashboard 应用广场页面：应用卡片网格布局、分类筛选（全部/对话/工具/RAG/智能体）、搜索功能、精选应用 Banner、快捷创建智能体入口'
        ]),
        ('陈晴玥 — 知识库与工具前端开发', [
            '负责 KnowledgeBase 知识库页面：文档上传组件（支持拖拽）、知识库文档列表、语义搜索交互、文档内容预览',
            '负责 MCPHub MCP 工具中心页面：工具分类列表、工具详情卡片、工具执行参数表单、执行结果展示',
            '负责 AgentCenter 智能体中心页面：10 类智能体（需求分析/架构设计/领域建模/代码开发/测试/部署等）的展示与调用入口'
        ]),
        ('杨素萍 — 项目与设置前端开发', [
            '负责项目工作台：ProjectList 项目列表、CreateProject 项目创建表单、ProjectDetail 项目详情页，实现项目全生命周期管理界面',
            '负责 Settings 模型管理中心：LLM 供应商配置表单、API Key 管理、数据库连接配置（含连接测试按钮）、平台基础设置',
            '统筹前端与后端 5 个微服务模块的接口联调工作，跟进并修复联调过程中的字段映射与跨域问题'
        ])
    ]
    
    for name, tasks in frontend_members:
        p = doc.add_paragraph()
        p.add_run(name).bold = True
        for task in tasks:
            doc.add_paragraph(task, style='List Bullet')
    
    # 3.2 后端开发组
    doc.add_heading('3.2 后端开发组（3人）', level=2)
    doc.add_paragraph('后端组已完成 7个微服务模块 的架构设计与核心编码，累计输出 Java 源码 115+ 个文件，约 12,600+ 行有效代码，构建 RESTful API 接口 50+ 个。')
    
    backend_members = [
        ('鲁鸿 — 组长 / 后端技术负责人', [
            '架构与基础设施：主导设计 7 模块微服务架构（gateway/user/llm-core/mcp-server/agent/engine/infra）；完成阿里云服务器（8.137.117.129）PostgreSQL、MySQL、Redis 生产环境部署；配置各模块独立 Swagger 文档与网关路由',
            'llm-core 大模型核心服务：实现多轮上下文对话（同步+SSE 流式）、多模型动态切换（智谱 GLM 系列/灵龙AI 系列）；实现 RAG 知识库检索增强生成、按项目维度隔离的 RAG 问答；实现文档对话（支持 PDF/Word/Excel/PPT/Markdown/TXT/HTML/JSON/CSV/代码文件，单文件上限 100MB，含同步/流式两种模式）；实现对话导出 DOCX；集成 GLM-Image 图片生成服务；实现 AI 简历助手（生成/评估/优化三模式）',
            'agent 智能体服务：设计 DDDUP 统一过程多智能体协作框架，实现 10 类专业 Agent（需求分析、业务架构、应用架构、API 设计、领域建模、代码开发、单元测试、自动化测试、设计方案评审、自动化部署）',
            '系统集成：统筹前后端联调、跨模块接口对接、生产环境部署与线上问题排查'
        ]),
        ('张鲜 — 业务系统后端开发', [
            'user 服务：实现用户注册/登录/退出/密码找回全流程；基于 JWT + BCrypt 的安全认证体系与角色权限控制（ADMIN/USER）；实现管理员用户管理接口',
            '项目管理模块：实现项目 CRUD 全生命周期接口（创建、列表分页、详情、更新、删除）',
            '平台设置模块：实现 LLM 厂商配置管理（增删改查、默认模型切换）、数据库连接配置（含在线连通性测试）、平台基础设置、安全设置（Token 过期时间、操作日志开关）',
            '数据库设计：负责 MySQL 业务库表结构设计（account 用户表、项目表等）、数据库初始化脚本编写'
        ]),
        ('孙春燕 — AI 基础设施后端开发', [
            'mcp-server MCP 工具服务：实现 MCP 协议工具的注册、查询、按分类检索及远程执行能力；设计工具注解体系与工具注册中心',
            '向量知识库（llm-core）：基于 PostgreSQL pgvector 实现向量知识库管理；实现文档批量入库、代码片段入库、需求文档入库；实现语义搜索、带相似度阈值的搜索、按项目隔离的代码/需求搜索；编写 pgvector HNSW 高性能索引与混合搜索 SQL 函数',
            'infra 基础设施：负责 Redis 缓存配置模块、Redis 分布式缓存工具类封装',
            'gateway 网关：协助配置 API 网关路由转发、跨域处理、负载均衡策略'
        ])
    ]
    
    for name, tasks in backend_members:
        p = doc.add_paragraph()
        p.add_run(name).bold = True
        for task in tasks:
            doc.add_paragraph(task, style='List Bullet')
    
    # 3.3 测试组
    doc.add_heading('3.3 测试组（2人）', level=2)
    doc.add_paragraph('测试组前期深度参与 UI 设计，目前已进入测试准备阶段，计划 6 月 1 日正式启动系统测试。')
    
    test_members = [
        ('孟佳鑫 — UI 设计 & 测试计划负责人', [
            'UI 设计阶段（4/13 — 4/17）：独立完成平台整体科技蓝视觉风格设计；输出登录页、应用广场、对话页、知识库、设置中心等全部核心页面的高保真 UI 设计稿与交互说明文档；绘制业务功能逻辑流程图',
            '测试准备：编写《灵龙AI平台系统测试计划》；设计冒烟测试用例（覆盖登录、对话、知识库、项目创建等核心路径）；组织并主持测试用例评审会议'
        ]),
        ('刘一新 — 测试执行 & 缺陷管理负责人', [
            '测试准备：搭建测试环境，准备测试数据（多角色测试账号、模拟项目数据、多格式测试文档）',
            '待执行工作（6月启动）：负责全量功能测试执行、缺陷记录与跟踪管理、缺陷评审会议组织、回归测试执行与测试报告输出'
        ])
    ]
    
    for name, tasks in test_members:
        p = doc.add_paragraph()
        p.add_run(name).bold = True
        for task in tasks:
            doc.add_paragraph(task, style='List Bullet')
    
    # 四、核心功能模块完成情况汇总
    doc.add_heading('四、核心功能模块完成情况汇总', level=1)
    
    table2 = doc.add_table(rows=13, cols=4)
    table2.alignment = WD_TABLE_ALIGNMENT.CENTER
    headers2 = ['功能模块', '负责人/主要负责组', '完成度', '是否上线']
    for i, header in enumerate(headers2):
        cell = table2.rows[0].cells[i]
        cell.text = header
        cell.paragraphs[0].alignment = WD_ALIGN_PARAGRAPH.CENTER
        cell.paragraphs[0].runs[0].font.bold = True
        cell.paragraphs[0].runs[0].font.color.rgb = RGBColor(255, 255, 255)
        set_cell_shading(cell, '1F4E79')
    
    data2 = [
        ['用户认证与权限管理', '张鲜 + 周钞钞', '100%', '✅ 已上线'],
        ['AI 大模型智能对话', '鲁鸿 + 王继辉', '95%', '✅ 已上线'],
        ['RAG 知识库问答', '鲁鸿 + 孙春燕 + 陈晴玥', '90%', '✅ 已上线'],
        ['文档对话与解析', '鲁鸿 + 陈晴玥', '90%', '✅ 已上线'],
        ['向量知识库管理', '孙春燕 + 陈晴玥', '90%', '✅ 已上线'],
        ['MCP Hub 工具集成', '孙春燕 + 陈晴玥', '85%', '✅ 已上线'],
        ['Agent 智能体中心', '鲁鸿 + 王继辉', '85%', '✅ 已上线'],
        ['模型管理中心', '张鲜 + 杨素萍', '90%', '✅ 已上线'],
        ['项目管理', '张鲜 + 杨素萍', '90%', '✅ 已上线'],
        ['AI 简历助手', '鲁鸿 + 王继辉', '100%', '✅ 已上线'],
        ['AI 图片生成', '鲁鸿 + 王继辉', '100%', '✅ 已上线'],
        ['系统测试与缺陷修复', '孟佳鑫 + 刘一新', '0%', '⏳ 待 6 月启动'],
    ]
    
    for row_idx, row_data in enumerate(data2, 1):
        for col_idx, cell_text in enumerate(row_data):
            cell = table2.rows[row_idx].cells[col_idx]
            cell.text = cell_text
            cell.paragraphs[0].alignment = WD_ALIGN_PARAGRAPH.CENTER
            if row_idx % 2 == 0:
                set_cell_shading(cell, 'F2F2F2')
    
    # 五、工作量与产出统计
    doc.add_heading('五、工作量与产出统计（数据化体现）', level=1)
    
    table3 = doc.add_table(rows=11, cols=3)
    table3.alignment = WD_TABLE_ALIGNMENT.CENTER
    headers3 = ['指标', '数量', '说明']
    for i, header in enumerate(headers3):
        cell = table3.rows[0].cells[i]
        cell.text = header
        cell.paragraphs[0].alignment = WD_ALIGN_PARAGRAPH.CENTER
        cell.paragraphs[0].runs[0].font.bold = True
        cell.paragraphs[0].runs[0].font.color.rgb = RGBColor(255, 255, 255)
        set_cell_shading(cell, '1F4E79')
    
    data3 = [
        ['团队人数', '10人', '前端5人 + 后端3人 + 测试2人'],
        ['后端 Java 文件', '115+ 个', '核心业务代码，7个微服务模块'],
        ['后端有效代码行', '~12,600+ 行', '由鲁鸿、张鲜、孙春燕共同完成'],
        ['前端 TS/TSX 文件', '30+ 个', '页面级组件与核心逻辑'],
        ['前端有效代码行', '~6,000+ 行', '由黄雨、周钞钞、王继辉、陈晴玥、杨素萍共同完成'],
        ['数据库设计', '3 套', 'MySQL 业务库、PostgreSQL 向量库、平台配置库'],
        ['RESTful API 接口', '50+ 个', '10 大模块，全部配备 Swagger 文档'],
        ['部署环境', '1 套', '阿里云 ECS（8.137.117.129），含 PG + MySQL + Redis'],
        ['UI 设计稿', '10+ 页', '孟佳鑫独立完成全套高保真设计'],
        ['AI 模型集成', '10+ 个', '智谱 GLM 系列 + 灵龙AI 系列 + GLM-Image'],
    ]
    
    for row_idx, row_data in enumerate(data3, 1):
        for col_idx, cell_text in enumerate(row_data):
            cell = table3.rows[row_idx].cells[col_idx]
            cell.text = cell_text
            cell.paragraphs[0].alignment = WD_ALIGN_PARAGRAPH.CENTER
            if row_idx % 2 == 0:
                set_cell_shading(cell, 'F2F2F2')
    
    # 六、下阶段工作计划
    doc.add_heading('六、下阶段工作计划（5月中旬 — 6月下旬）', level=1)
    
    plans = [
        '5月中旬：鲁鸿完成后端性能调优与 Agent/MCP 深度优化；杨素萍统筹前端与图片生成/简历助手模块的联调；全组进入 BUG 修复冲刺',
        '5月下旬：前后端联调收尾；黄雨组织接口压测与前端性能优化；各组完成自测',
        '6月上旬（测试组主导）：孟佳鑫、刘一新启动系统测试（冒烟测试 → 功能测试 → 缺陷修复）',
        '6月中旬：测试评审、最终回归、产品验收、准备交付',
    ]
    for plan in plans:
        doc.add_paragraph(plan, style='List Number')
    
    # 七、总结
    doc.add_heading('七、总结', level=1)
    
    summary = doc.add_paragraph()
    summary.add_run('目前团队整体开发进度符合预期，核心功能模块已基本开发完毕并提前部署上线，').font.size = Pt(10.5)
    summary.add_run('平台已形成"AI 智能对话 + RAG 知识库 + MCP 工具集成 + Agent 智能体协作 + 多模型管理 + 项目管理"的完整能力闭环。').font.size = Pt(10.5)
    
    doc.add_paragraph('后端组在鲁鸿带领下完成了微服务架构搭建、50+ 接口开发及 AI 算法集成；')
    doc.add_paragraph('前端组 5人分工明确，已完成 10+ 核心页面开发与全量接口联调；')
    doc.add_paragraph('测试组已完成 UI 设计与测试准备工作，6 月将进入全面测试阶段。')
    
    conclusion = doc.add_paragraph()
    conclusion.add_run('预计可按计划于 6 月中下旬完成系统测试并交付。').font.size = Pt(11)
    conclusion.add_run('\n').font.size = Pt(11)
    conclusion.add_run('\n汇报完毕，感谢领导审阅！').font.size = Pt(11)
    
    # 保存文档
    doc.save('灵龙AI智能平台_项目进展汇报_20260506.docx')
    print('Word文档已生成：灵龙AI智能平台_项目进展汇报_20260506.docx')

if __name__ == '__main__':
    main()
