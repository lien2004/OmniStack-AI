import { Row, Col, Card, Button, Tag, Input, Tabs } from 'antd'
import {
  ThunderboltOutlined,
  SearchOutlined,
  StarOutlined,
  MoreOutlined,
  RobotOutlined,
  ApiOutlined,
  MessageOutlined,
  DatabaseOutlined,
  ArrowUpOutlined,
} from '@ant-design/icons'
import './style.css'

const quickStartItems = [
  { title: 'AI 对话助手', desc: '快速开始智能对话', color: '#1677ff', icon: <MessageOutlined /> },
  { title: '创建智能体', desc: '配置专属 AI Agent', color: '#52c41a', icon: <RobotOutlined /> },
]

const resourceItems = [
  { title: 'MCP 工具集', icon: <ApiOutlined />, color: '#faad14' },
  { title: '知识库', icon: <DatabaseOutlined />, color: '#1677ff' },
]

const platformStats = [
  { title: '活跃智能体', value: 12, icon: <RobotOutlined />, color: '#1677ff', trend: '+3' },
  { title: 'API 调用次数', value: '1.2k', icon: <ApiOutlined />, color: '#52c41a', trend: '+12%' },
  { title: 'MCP 工具集成', value: 45, icon: <ThunderboltOutlined />, color: '#faad14', trend: '+5' },
  { title: '平均响应时间', value: '120ms', icon: <MessageOutlined />, color: '#722ed1', trend: '-8%' },
]

const appCategories = [
  { key: 'all', label: '全部应用' },
  { key: 'chat', label: 'AI 对话' },
  { key: 'agent', label: '智能体' },
  { key: 'rag', label: 'RAG 应用' },
  { key: 'tool', label: 'MCP 工具' },
]

const appList = [
  { id: 1, name: '灵龙对话助手', desc: '基于 GLM-4.6 + 向量缓存的智能对话', category: 'chat', icon: '🤖', color: '#1677ff' },
  { id: 2, name: 'RAG 知识问答', desc: '向量检索增强生成，项目知识问答', category: 'rag', icon: '📚', color: '#52c41a' },
  { id: 3, name: 'MCP Hub', desc: 'Model Context Protocol 工具集成中心', category: 'tool', icon: '⚡', color: '#faad14' },
  { id: 4, name: 'DDDUP Agent', desc: '域驱动设计统一过程多智能体协作', category: 'agent', icon: '🌐', color: '#722ed1' },
]

function Dashboard() {
  return (
    <div className="dashboard">
      {/* 统计卡片区域 */}
      <div className="stats-section">
        <Row gutter={16}>
          {platformStats.map((stat, i) => (
            <Col span={6} key={i}>
              <Card className="stat-card" bordered={false}>
                <div className="stat-card-inner">
                  <div className="stat-icon" style={{ color: stat.color, background: `${stat.color}15` }}>
                    {stat.icon}
                  </div>
                  <div className="stat-info">
                    <div className="stat-card-value">{stat.value}</div>
                    <div className="stat-card-title">{stat.title}</div>
                  </div>
                  <div className="stat-trend" style={{ color: stat.trend.startsWith('-') ? '#52c41a' : '#1677ff' }}>
                    <ArrowUpOutlined style={{ transform: stat.trend.startsWith('-') ? 'rotate(180deg)' : 'none' }} />
                    {stat.trend}
                  </div>
                </div>
              </Card>
            </Col>
          ))}
        </Row>
      </div>

      {/* Banner 区域 */}
      <div className="banner-section">
        <Row gutter={24}>
          <Col span={16}>
            <div className="banner-main">
              <div className="banner-content">
                <div className="banner-badge">灵龙AI智能平台 v2.0</div>
                <h1 className="banner-title">灵龙AI智能平台</h1>
                <p className="banner-subtitle">基于大模型 + 向量数据库 + MCP 工具链的一站式 AI 开发平台</p>
                <div className="banner-tags">
                  <Tag color="blue">GLM-4.6 大模型</Tag>
                  <Tag color="cyan">RAG 向量检索</Tag>
                  <Tag color="purple">MCP 工具集成</Tag>
                  <Tag color="geekblue">DDDUP Agent</Tag>
                </div>
                <div className="banner-image">
                  <div className="platform-preview">
                    <div className="preview-code">
                      <div className="code-line"><span className="code-comment">// 灵龙AI智能平台 - 山水相逢</span></div>
                      <div className="code-line"><span className="code-keyword">import</span> <span className="code-variable">LingLongAI</span> <span className="code-keyword">from</span> <span className="code-string">'@linglong/core'</span></div>
                      <div className="code-line"><span className="code-keyword">const</span> <span className="code-variable">ai</span> = <span className="code-keyword">new</span> <span className="code-function">LingLongAI</span>({`{`} model: <span className="code-string">'glm-4.6'</span> {`}`})</div>
                      <div className="code-line"><span className="code-keyword">const</span> <span className="code-variable">answer</span> = <span className="code-keyword">await</span> <span className="code-variable">ai</span>.<span className="code-function">chat</span>(<span className="code-string">'你好，灵龙!'</span>)</div>
                      <div className="code-line"><span className="code-comment">// 向量缓存命中，节省 Token</span></div>
                      <div className="code-line"><span className="code-keyword">const</span> <span className="code-variable">cached</span> = <span className="code-keyword">await</span> <span className="code-variable">ai</span>.<span className="code-function">rag</span>(<span className="code-variable">answer</span>)</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </Col>
          <Col span={8}>
            <div className="banner-sidebar">
              <Card className="quick-start-card" bordered={false}>
                <div className="card-header">
                  <h3>快速开始</h3>
                </div>
                <div className="quick-start-list">
                  {quickStartItems.map((item, index) => (
                    <div key={index} className="quick-start-item" style={{ borderLeftColor: item.color }}>
                      <div className="quick-start-icon" style={{ color: item.color }}>{item.icon}</div>
                      <div className="quick-start-info">
                        <div className="quick-start-title">{item.title}</div>
                        <div className="quick-start-desc">{item.desc}</div>
                      </div>
                    </div>
                  ))}
                </div>
              </Card>
              <Row gutter={12} className="resource-row">
                {resourceItems.map((item, index) => (
                  <Col span={12} key={index}>
                    <Card className="resource-card" bordered={false}>
                      <div className="resource-icon" style={{ color: item.color }}>{item.icon}</div>
                      <div className="resource-title">{item.title}</div>
                    </Card>
                  </Col>
                ))}
              </Row>
            </div>
          </Col>
        </Row>
      </div>

      {/* 应用列表区域 */}
      <div className="app-section">
        <div className="section-header">
          <Tabs items={appCategories} className="app-tabs" />
          <div className="section-actions">
            <Input
              prefix={<SearchOutlined />}
              placeholder="搜索应用"
              className="search-input"
            />
            <Button type="link">更多</Button>
          </div>
        </div>
        <Row gutter={[16, 16]} className="app-grid">
          {appList.map((app) => (
            <Col span={6} key={app.id}>
              <Card className="app-card" bordered={false} hoverable>
                <div className="app-card-header">
                  <div className="app-icon" style={{ backgroundColor: `${app.color}15`, color: app.color }}>
                    {app.icon}
                  </div>
                  <div className="app-actions">
                    <Button type="text" size="small" icon={<StarOutlined />} />
                    <Button type="text" size="small" icon={<MoreOutlined />} />
                  </div>
                </div>
                <div className="app-info">
                  <h4 className="app-name">{app.name}</h4>
                  <p className="app-desc">{app.desc}</p>
                </div>
                <div className="app-meta">
                  <Tag color={app.color === '#1677ff' ? 'blue' : app.color === '#52c41a' ? 'green' : app.color === '#faad14' ? 'gold' : 'purple'}>
                    {appCategories.find(c => c.key === app.category)?.label}
                  </Tag>
                  <Button type="primary" size="small" style={{ background: app.color, borderColor: app.color }}>进入</Button>
                </div>
              </Card>
            </Col>
          ))}
        </Row>
      </div>
    </div>
  )
}

export default Dashboard
