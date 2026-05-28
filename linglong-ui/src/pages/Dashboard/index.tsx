import { useEffect, useRef, useState } from 'react'
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
  PictureOutlined,
  FileTextOutlined,
  LeftOutlined,
  RightOutlined,
} from '@ant-design/icons'
import './style.css'

const featuredApps = [
  {
    id: 1,
    title: 'AI 对话助手',
    desc: '基于 GLM-4.6 + 向量缓存，智能问答让 AI 更懂你！',
    icon: <MessageOutlined />,
    iconBg: '#eff6ff',
    iconColor: '#1677ff',
    path: '/chat',
    tags: ['GLM-4.6', 'RAG增强', '上下文记忆'],
  },
  {
    id: 2,
    title: 'RAG 知识问答',
    desc: '向量检索增强生成，基于知识库精准回答专业问题。',
    icon: <DatabaseOutlined />,
    iconBg: '#f0fdf4',
    iconColor: '#16a34a',
    path: '/knowledge',
    tags: ['向量检索', 'PGVector', '文档解析'],
  },
  {
    id: 3,
    title: 'MCP Hub',
    desc: 'Model Context Protocol 工具集成中心，扩展 AI 能力边界。',
    icon: <ApiOutlined />,
    iconBg: '#fff7ed',
    iconColor: '#ea580c',
    path: '/mcp',
    tags: ['MCP协议', '工具调用', '能力扩展'],
  },
  {
    id: 4,
    title: 'AI 图片生成',
    desc: '基于 GLM-image,支持文生图与图生图创作。',
    icon: <PictureOutlined />,
    iconBg: '#faf5ff',
    iconColor: '#9333ea',
    path: '/image-generation',
    tags: ['Gemini', '文生图', '图生图'],
  },
  {
    id: 5,
    title: 'AI 简历助手',
    desc: '基于 gpt-5.5,智能生成简历、专业测评分析、一键优化完善。',
    icon: <FileTextOutlined />,
    iconBg: '#f0fdf4',
    iconColor: '#16a34a',
    path: '/resume',
    tags: ['gpt-5.5', '简历生成', '测评优化'],
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
  {
    id: 7,
    name: 'AI 图片生成',
    nameEn: 'AI Image Generation',
    desc: '基于 GLM-image 模型，支持文生图与图生图创作。',
    category: 'agent',
    icon: <PictureOutlined />,
    iconBg: '#a855f71a',
    iconColor: '#a855f7',
    path: '/image-generation',
  },
  {
    id: 8,
    name: 'AI 简历助手',
    nameEn: 'AI Resume Builder',
    desc: '基于 gpt-5.5 模型，智能生成简历、专业测评分析、一键优化完善。',
    category: 'agent',
    icon: <FileTextOutlined />,
    iconBg: '#52c41a1a',
    iconColor: '#52c41a',
    path: '/resume',
  },
]

function Dashboard() {
  const navigate = useNavigate()
  const [activeTab, setActiveTab] = useState('all')
  const [activeCategory, setActiveCategory] = useState('featured')
  const [searchVal, setSearchVal] = useState('')

  // 精选卡片轮播
  const [cardsPerView, setCardsPerView] = useState(3)
  const totalCards = featuredApps.length
  const extendedApps = [...featuredApps, ...featuredApps]
  const [carouselIndex, setCarouselIndex] = useState(0)
  const [transitionOn, setTransitionOn] = useState(true)
  const [isPaused, setIsPaused] = useState(false)
  const timerRef = useRef<number | null>(null)

  // 根据视口宽度调整每屏卡片数
  useEffect(() => {
    const computeCardsPerView = () => {
      const w = window.innerWidth
      if (w <= 720) return 1
      if (w <= 1100) return 2
      return 3
    }
    const handle = () => setCardsPerView(computeCardsPerView())
    handle()
    window.addEventListener('resize', handle)
    return () => window.removeEventListener('resize', handle)
  }, [])

  // 自动滚动
  useEffect(() => {
    if (isPaused) return
    timerRef.current = window.setInterval(() => {
      setCarouselIndex((i) => i + 1)
    }, 3500)
    return () => {
      if (timerRef.current) window.clearInterval(timerRef.current)
    }
  }, [isPaused])

  // 到达克隆段末尾时，无动画跳回首段
  useEffect(() => {
    if (carouselIndex < totalCards) return
    const t = window.setTimeout(() => {
      setTransitionOn(false)
      setCarouselIndex(carouselIndex - totalCards)
      // 下一帧恢复 transition
      requestAnimationFrame(() => {
        requestAnimationFrame(() => setTransitionOn(true))
      })
    }, 600)
    return () => window.clearTimeout(t)
  }, [carouselIndex, totalCards])

  const handlePrev = () => {
    if (carouselIndex <= 0) {
      // 反向：先无动画跳到 totalCards，再向左滚一张
      setTransitionOn(false)
      setCarouselIndex(totalCards)
      requestAnimationFrame(() => {
        requestAnimationFrame(() => {
          setTransitionOn(true)
          setCarouselIndex(totalCards - 1)
        })
      })
    } else {
      setCarouselIndex((i) => i - 1)
    }
  }

  const handleNext = () => {
    setCarouselIndex((i) => i + 1)
  }

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

      {/* 精选应用卡片区（横向轮播） */}
      <div className="featured-section">
        <div
          className="featured-carousel"
          onMouseEnter={() => setIsPaused(true)}
          onMouseLeave={() => setIsPaused(false)}
        >
          <button
            type="button"
            className="featured-arrow featured-arrow-left"
            onClick={handlePrev}
            aria-label="上一组"
          >
            <LeftOutlined />
          </button>

          <div className="featured-viewport">
            <div
              className="featured-track"
              style={{
                transform: `translateX(calc(${-carouselIndex} * (100% / ${cardsPerView})))`,
                transition: transitionOn ? 'transform 0.6s cubic-bezier(0.22, 0.61, 0.36, 1)' : 'none',
              }}
            >
              {extendedApps.map((app, idx) => (
                <div
                  key={`${app.id}-${idx}`}
                  className="featured-card"
                  onClick={() => navigate(app.path)}
                >
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
                    <div
                      className="featured-card-icon"
                      style={{ background: app.iconBg, color: app.iconColor }}
                    >
                      {app.icon}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <button
            type="button"
            className="featured-arrow featured-arrow-right"
            onClick={handleNext}
            aria-label="下一组"
          >
            <RightOutlined />
          </button>
        </div>

        <div className="featured-dots">
          {featuredApps.map((_, i) => (
            <span
              key={i}
              className={`featured-dot ${i === carouselIndex % totalCards ? 'active' : ''}`}
              onClick={() => setCarouselIndex(i)}
            />
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
