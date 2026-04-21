import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Input, Tabs } from 'antd'
import {
  PlusOutlined,
  SearchOutlined,
  MessageOutlined,
  RobotOutlined,
  ApiOutlined,
  DatabaseOutlined,
  ThunderboltOutlined,
  AppstoreOutlined,
  ArrowRightOutlined,
} from '@ant-design/icons'
import './style.css'

const featuredApps = [
  {
    id: 1,
    title: 'AI 对话助手',
    desc: '基于 GLM-4.6 + 向量缓存，智能问答让 AI 更懂你！',
    icon: <MessageOutlined />,
    iconBg: 'linear-gradient(135deg, #1677ff, #69b1ff)',
    path: '/chat',
    tags: ['GLM-4.6', 'RAG增强', '上下文记忆'],
  },
  {
    id: 2,
    title: 'RAG 知识问答',
    desc: '向量检索增强生成，基于知识库精准回答专业问题。',
    icon: <DatabaseOutlined />,
    iconBg: 'linear-gradient(135deg, #52c41a, #95de64)',
    path: '/knowledge',
    tags: ['向量检索', 'PGVector', '文档解析'],
  },
  {
    id: 3,
    title: 'MCP Hub',
    desc: 'Model Context Protocol 工具集成中心，扩展 AI 能力边界。',
    icon: <ApiOutlined />,
    iconBg: 'linear-gradient(135deg, #fa8c16, #ffd591)',
    path: '/mcp',
    tags: ['MCP协议', '工具调用', '能力扩展'],
  },
]

const categories = [
  { key: 'all', label: '全部' },
  { key: 'chat', label: 'AI 对话' },
  { key: 'agent', label: '智能体' },
  { key: 'rag', label: 'RAG 应用' },
  { key: 'tool', label: 'MCP 工具' },
]

const leftCategories = [
  { key: 'featured', label: '精选' },
  { key: 'all', label: '全部' },
  { key: 'chat', label: 'AI 对话' },
  { key: 'agent', label: '智能体' },
  { key: 'rag', label: 'RAG 应用' },
  { key: 'tool', label: 'MCP 工具' },
]

const appList = [
  {
    id: 1,
    name: '灵龙对话助手',
    nameEn: 'LingLong Chat',
    desc: '基于 GLM-4.6 + 向量缓存的智能对话，支持多轮问答与知识库检索。',
    category: 'chat',
    icon: <MessageOutlined />,
    iconBg: '#1677ff1a',
    iconColor: '#1677ff',
    path: '/chat',
  },
  {
    id: 2,
    name: 'DDDUP Agent',
    nameEn: 'Domain Driven Agent',
    desc: '域驱动设计统一过程多智能体协作，全 AI 智能软件开发。',
    category: 'agent',
    icon: <RobotOutlined />,
    iconBg: '#722ed11a',
    iconColor: '#722ed1',
    path: '/agents',
  },
  {
    id: 3,
    name: 'RAG 知识问答',
    nameEn: 'RAG Knowledge QA',
    desc: '向量检索增强生成，项目知识精准问答，支持多格式文档解析。',
    category: 'rag',
    icon: <DatabaseOutlined />,
    iconBg: '#52c41a1a',
    iconColor: '#52c41a',
    path: '/knowledge',
  },
  {
    id: 4,
    name: 'MCP Hub',
    nameEn: 'MCP Tool Center',
    desc: 'Model Context Protocol 工具集成中心，扩展大模型能力边界。',
    category: 'tool',
    icon: <ThunderboltOutlined />,
    iconBg: '#fa8c161a',
    iconColor: '#fa8c16',
    path: '/mcp',
  },
  {
    id: 5,
    name: '项目工作台',
    nameEn: 'Project Workbench',
    desc: '微服务项目版本管理与路由配置，一站式项目生命周期管理平台。',
    category: 'agent',
    icon: <AppstoreOutlined />,
    iconBg: '#eb2f961a',
    iconColor: '#eb2f96',
    path: '/projects',
  },
  {
    id: 6,
    name: '模型管理中心',
    nameEn: 'Model Management',
    desc: '管理 LLM 供应商配置与参数，支持多模型切换与性能对比。',
    category: 'tool',
    icon: <ApiOutlined />,
    iconBg: '#13c2c21a',
    iconColor: '#13c2c2',
    path: '/settings',
  },
]

function Dashboard() {
  const navigate = useNavigate()
  const [activeTab, setActiveTab] = useState('all')
  const [activeCategory, setActiveCategory] = useState('featured')
  const [searchVal, setSearchVal] = useState('')

  const filteredApps = appList.filter((app) => {
    const matchTab = activeTab === 'all' || app.category === activeTab
    const matchCat =
      activeCategory === 'featured' || activeCategory === 'all' || app.category === activeCategory
    const matchSearch =
      !searchVal ||
      app.name.toLowerCase().includes(searchVal.toLowerCase()) ||
      app.desc.toLowerCase().includes(searchVal.toLowerCase())
    return matchTab && matchCat && matchSearch
  })

  return (
    <div className="dashboard">
      {/* 页面标题区 */}
      <div className="page-header">
        <div className="page-header-left">
          <h1 className="page-title">应用广场</h1>
          <p className="page-subtitle">探索、发现并集成最先进的 AI 应用与智能体</p>
        </div>
        <Button
          type="primary"
          size="large"
          icon={<PlusOutlined />}
          className="create-btn"
          onClick={() => navigate('/agents')}
        >
          创建智能体
        </Button>
      </div>

      {/* 精选应用卡片区 */}
      <div className="featured-section">
        <div className="featured-cards">
          {featuredApps.map((app) => (
            <div key={app.id} className="featured-card" onClick={() => navigate(app.path)}>
              <div className="featured-card-body">
                <div className="featured-card-left">
                  <h3 className="featured-card-title">{app.title}</h3>
                  <p className="featured-card-desc">{app.desc}</p>
                  <div className="featured-card-footer">
                    <Button
                      type="primary"
                      size="small"
                      className="featured-card-btn"
                      onClick={(e) => {
                        e.stopPropagation()
                        navigate(app.path)
                      }}
                    >
                      点击查看
                    </Button>
                    <div className="featured-card-tags">
                      {app.tags.map((tag) => (
                        <span key={tag} className="featured-tag">
                          {tag}
                        </span>
                      ))}
                    </div>
                  </div>
                </div>
                <div className="featured-card-icon" style={{ background: app.iconBg }}>
                  {app.icon}
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* 分类标签栏 */}
      <div className="filter-bar">
        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          items={categories.map((c) => ({ key: c.key, label: c.label }))}
          className="filter-tabs"
        />
        <div className="filter-actions">
          <Input
            prefix={<SearchOutlined style={{ color: '#9ca3af' }} />}
            placeholder="请输入"
            value={searchVal}
            onChange={(e) => setSearchVal(e.target.value)}
            className="filter-search"
          />
        </div>
      </div>

      {/* 内容区：左侧分类 + 右侧应用列表 */}
      <div className="content-area">
        {/* 左侧分类列表 */}
        <div className="category-sidebar">
          <div className="category-label">分类</div>
          {leftCategories.map((cat) => (
            <div
              key={cat.key}
              className={`category-item ${activeCategory === cat.key ? 'active' : ''}`}
              onClick={() => setActiveCategory(cat.key)}
            >
              {cat.label}
            </div>
          ))}
        </div>

        {/* 右侧应用卡片网格 */}
        <div className="app-grid">
          {filteredApps.length === 0 ? (
            <div className="empty-state">暂无匹配的应用</div>
          ) : (
            filteredApps.map((app) => (
              <div key={app.id} className="app-card" onClick={() => navigate(app.path)}>
                <div className="app-card-icon" style={{ background: app.iconBg, color: app.iconColor }}>
                  {app.icon}
                </div>
                <div className="app-card-info">
                  <div className="app-card-name">{app.name}</div>
                  <div className="app-card-name-en">{app.nameEn}</div>
                  <div className="app-card-desc">{app.desc}</div>
                </div>
                <div className="app-card-action">
                  <ArrowRightOutlined className="app-card-arrow" />
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  )
}

export default Dashboard
