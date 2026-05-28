import React, { useState } from 'react'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { Layout as AntLayout, Avatar, Dropdown, Button, Badge, Input, Breadcrumb } from 'antd'
import type { MenuProps } from 'antd'
import {
  RobotOutlined,
  ApiOutlined,
  UserOutlined,
  MessageOutlined,
  AppstoreOutlined,
  BellOutlined,
  DatabaseOutlined,
  SearchOutlined,
  SettingOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  ThunderboltOutlined,
  CodeOutlined,
  CrownOutlined,
  TeamOutlined,
} from '@ant-design/icons'
import { useUserStore } from '../../stores/userStore'
import logoImg from '../../img/重工AI.png'
import './style.css'

const { Header, Sider, Content } = AntLayout

// ── 导航数据 ──────────────────────────────────────────────────────────────

interface NavItem {
  key: string
  icon: React.ElementType
  iconColor: string
  iconBg: string
  title: string
  subtitle: string
  tag?: string
  tagColor?: string
}

interface NavGroup {
  label: string
  items: NavItem[]
}

const NAV_GROUPS: NavGroup[] = [
  {
    label: '核心能力',
    items: [
      {
        key: '/',
        icon: AppstoreOutlined,
        iconColor: '#60a5fa',
        iconBg: 'rgba(96,165,250,0.15)',
        title: '应用广场',
        subtitle: '平台概况与核心指标',
      },
      {
        key: '/chat',
        icon: MessageOutlined,
        iconColor: '#34d399',
        iconBg: 'rgba(52,211,153,0.15)',
        title: 'AI 对话助手',
        subtitle: '智能对话与知识问答',
        tag: 'AI',
        tagColor: '#34d399',
      },
      {
        key: '/knowledge',
        icon: DatabaseOutlined,
        iconColor: '#a78bfa',
        iconBg: 'rgba(167,139,250,0.15)',
        title: '知识库管理',
        subtitle: '向量知识检索与管理',
      },
    ],
  },
  {
    label: '智能集成',
    items: [
      {
        key: '/codeflow',
        icon: ThunderboltOutlined,
        iconColor: '#f97316',
        iconBg: 'rgba(249,115,22,0.15)',
        title: 'Agent智能开发',
        subtitle: '一站式AI驱动软件开发工作流',
        tag: 'New',
        tagColor: '#f97316',
      },
      {
        key: '/agents',
        icon: RobotOutlined,
        iconColor: '#fbbf24',
        iconBg: 'rgba(251,191,36,0.15)',
        title: '智能体中心',
        subtitle: '创建与集成 MCP 协议工具',
        tag: 'Beta',
        tagColor: '#fbbf24',
      },
      {
        key: '/mcp',
        icon: ThunderboltOutlined,
        iconColor: '#f87171',
        iconBg: 'rgba(248,113,113,0.15)',
        title: 'MCP Hub',
        subtitle: 'MCP 工具集成与管理',
      },
      {
        key: '/projects',
        icon: CodeOutlined,
        iconColor: '#38bdf8',
        iconBg: 'rgba(56,189,248,0.15)',
        title: '项目中心',
        subtitle: '微服务版本与路由管理',
      },
    ],
  },
  {
    label: '系统配置',
    items: [
      {
        key: '/settings',
        icon: SettingOutlined,
        iconColor: '#94a3b8',
        iconBg: 'rgba(148,163,184,0.15)',
        title: '模型管理',
        subtitle: '管理 LLM 供应商与参数',
      },
      {
        key: '/admin/users',
        icon: CrownOutlined,
        iconColor: '#f59e0b',
        iconBg: 'rgba(245,158,11,0.15)',
        title: '用户管理',
        subtitle: '管理所有账号与角色',
        tag: 'Admin',
        tagColor: '#f59e0b',
      },
    ],
  },
]

const routeNameMap: Record<string, string> = {
  '/': '应用广场',
  '/chat': 'AI 对话助手',
  '/knowledge': '知识库管理',
  '/agents': '智能体中心',
  '/codeflow': 'Agent智能开发',
  '/mcp': 'MCP Hub',
  '/projects': '项目中心',
  '/settings': '模型管理',
  '/profile': '个人中心',
  '/admin/users': '用户管理',
}

// ── 自定义侧边栏导航 ──────────────────────────────────────────────────────

interface SideNavProps {
  currentPath: string
  collapsed: boolean
  isAdmin: boolean
}

const SideNav: React.FC<SideNavProps> = ({ currentPath, collapsed, isAdmin }) => {
  const navigate = useNavigate()

  return (
    <nav className="side-nav">
      {NAV_GROUPS.map((group) => (
        <div key={group.label} className="nav-group">
          {!collapsed && (
            <div className="nav-group-label">
              <span>{group.label}</span>
            </div>
          )}
          {group.items
            .filter((item) => item.key !== '/admin/users' || isAdmin)
            .map((item) => {
            const isActive = currentPath === item.key
            const Icon = item.icon
            return (
              <div
                key={item.key}
                className={`nav-item${isActive ? ' is-active' : ''}`}
                onClick={() => navigate(item.key)}
                title={collapsed ? item.title : undefined}
              >
                {isActive && <span className="nav-active-bar" />}
                <div
                  className="nav-item-icon"
                  style={{
                    background: isActive ? item.iconBg.replace('0.15', '0.25') : item.iconBg,
                  }}
                >
                  <Icon style={{ color: item.iconColor, fontSize: 14 }} />
                </div>
                {!collapsed && (
                  <div className="nav-item-body">
                    <div className="nav-item-top">
                      <span className="nav-item-title">{item.title}</span>
                      {item.tag && (
                        <span
                          className="nav-item-tag"
                          style={{
                            color: item.tagColor,
                            borderColor: item.tagColor,
                            background: item.iconBg,
                          }}
                        >
                          {item.tag}
                        </span>
                      )}
                    </div>
                  </div>
                )}
              </div>
            )
          })}
        </div>
      ))}
    </nav>
  )
}

// ── 主布局 ────────────────────────────────────────────────────────────────

function Layout() {
  const navigate = useNavigate()
  const location = useLocation()
  const [collapsed, setCollapsed] = useState(false)
  const { user, logout, isAdmin } = useUserStore()

  const currentPageName = routeNameMap[location.pathname] || '首页'

  const userMenuItems: MenuProps['items'] = [
    { key: 'profile', label: '个人中心' },
    ...(isAdmin() ? [{ key: 'admin-users', label: '用户管理' }] : []),
    { type: 'divider' as const },
    { key: 'logout', label: <span style={{ color: '#ef4444' }}>退出登录</span> },
  ]

  const handleMenuClick = ({ key }: { key: string }) => {
    if (key === 'profile') {
      navigate('/profile')
    } else if (key === 'admin-users') {
      navigate('/admin/users')
    } else if (key === 'logout') {
      logout()
      navigate('/login')
    }
  }

  return (
    <AntLayout className="layout">
      <Sider
        trigger={null}
        collapsible
        collapsed={collapsed}
        theme="light"
        className="sider"
        width={220}
        collapsedWidth={64}
      >
        {/* Logo */}
        <div className={`logo${collapsed ? ' logo-collapsed' : ''}`}>
          <div className="logo-icon-box">
            <img src={logoImg} alt="灵龙AI Logo" className="logo-img" />
          </div>
          {!collapsed && <div className="logo-text">灵龙<span className="logo-text-ai">AI</span></div>}
        </div>

        {/* 导航 */}
        <div className="menu-section">
          <SideNav currentPath={location.pathname} collapsed={collapsed} isAdmin={isAdmin()} />
        </div>

        {/* 底部区域 */}
        <div className={`sider-footer${collapsed ? ' sider-footer-collapsed' : ''}`}>
          {!collapsed && (
            <div className="sider-version">
              <div className="version-badge">
                <ApiOutlined style={{ fontSize: 10 }} />
                <span>v1.0.0 · 专业版</span>
              </div>
              <span className="version-env">灵龙AI Platform</span>
            </div>
          )}
          <button
            className="collapse-btn"
            onClick={() => setCollapsed(!collapsed)}
            title={collapsed ? '展开侧边栏' : '收起侧边栏'}
          >
            {collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
          </button>
        </div>
      </Sider>

      <AntLayout>
        <Header className="header">
          <div className="header-left">
            <Breadcrumb
              className="header-breadcrumb"
              items={[
                { title: <span className="breadcrumb-home" onClick={() => navigate('/')}>主页</span> },
                { title: currentPageName },
              ]}
            />
          </div>
          <div className="header-right">
            <Input
              prefix={<SearchOutlined className="search-icon" />}
              placeholder="搜索资源、文档、智能体..."
              className="header-search"
            />
            <Badge count={3} size="small">
              <Button type="text" shape="circle" icon={<BellOutlined />} className="header-icon-btn" />
            </Badge>
            <Dropdown menu={{ items: userMenuItems, onClick: handleMenuClick }} placement="bottomRight">
              <div className="user-info">
                <Avatar style={{ background: '#1677ff' }} icon={<UserOutlined />} />
                <div className="user-detail">
                  <span className="username">{user?.username || '未登录'}</span>
                  <span className="user-role">
                    {user?.role === 1 ? (
                      <><CrownOutlined style={{ marginRight: 3, color: '#f59e0b' }} />系统管理员</>
                    ) : (
                      <><TeamOutlined style={{ marginRight: 3 }} />普通用户</>
                    )}
                  </span>
                </div>
              </div>
            </Dropdown>
          </div>
        </Header>
        <Content className="content">
          <Outlet />
        </Content>
      </AntLayout>
    </AntLayout>
  )
}

export default Layout
