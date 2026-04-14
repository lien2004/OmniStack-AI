import { 
  LayoutDashboard, 
  Cpu, 
  MessageSquare, 
  Box, 
  Database, 
  Workflow
} from 'lucide-react';

export const MODULES = [
  {
    id: 'dashboard',
    name: '控制台',
    icon: LayoutDashboard,
    description: '平台概览与核心指标'
  },
  {
    id: 'llm',
    name: '模型供应商',
    icon: Cpu,
    description: '管理 LLM 供应商与参数配置'
  },
  {
    id: 'mcp',
    name: 'MCP Hub',
    icon: Box,
    description: '创建与集成 MCP 协议工具'
  },
  {
    id: 'agents',
    name: '智能体协作',
    icon: Workflow,
    description: 'DDDUP 统一过程多智能体协作'
  },
  {
    id: 'api',
    name: 'API 管理中心',
    icon: Database,
    description: '微服务 API 版本与路由管理'
  },
  {
    id: 'chat',
    name: 'LUI 对话',
    icon: MessageSquare,
    description: '自然语言交互辅助开发'
  }
];

export const AGENTS = [
  { name: '需求分析 Agent', role: 'Requirement Analysis' },
  { name: '业务架构 Agent', role: 'Business Architecture' },
  { name: '应用架构 Agent', role: 'Application Architecture' },
  { name: 'API 设计 Agent', role: 'API Design' },
  { name: '领域建模 Agent', role: 'Domain Modeling' },
  { name: '代码开发 Agent', role: 'Code Development' },
  { name: '单元测试 Agent', role: 'Unit Testing' },
  { name: '自动化测试 Agent', role: 'Automation Testing' },
  { name: '设计方案评审 Agent', role: 'Design Review' },
  { name: '自动化部署 Agent', role: 'Auto Deployment' }
];
