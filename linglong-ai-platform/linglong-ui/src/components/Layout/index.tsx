import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { Layout as AntLayout, Menu, Avatar, Dropdown, Button, Badge } from 'antd'
import {
  ProjectOutlined,
  RobotOutlined,
  ApiOutlined,
  ToolOutlined,
  UserOutlined,
  PlusOutlined,
  MessageOutlined,
  AppstoreOutlined,
  BellOutlined,
  ThunderboltOutlined,
  PartitionOutlined,
  DatabaseOutlined,
} from '@ant-design/icons'
import type { MenuProps } from 'antd'
import './style.css'

const { Header, Sider, Content } = AntLayout

const mainMenuItems: MenuProps['items'] = [
  {
    key: '/',
    icon: <AppstoreOutlined />,
    label: '应用广场',
  },
  {
    key: '/chat',
    icon: <MessageOutlined />,
    label: 'AI 对话助手',
  },
  {
    key: '/knowledge',
    icon: <DatabaseOutlined />,
    label: '知识库管理',
  },
  {
    key: '/projects',
    icon: <ProjectOutlined />,
    label: '项目中心',
  },
]

const personalMenuItems: MenuProps['items'] = [
  {
    key: '/agents',
    icon: <RobotOutlined />,
    label: '智能体管理',
  },
  {
    key: '/mcp',
    icon: <ApiOutlined />,
    label: 'MCP Hub',
  },
  {
    key: '/settings',
    icon: <ToolOutlined />,
    label: '平台工具',
  },
]

const userMenuItems: MenuProps['items'] = [
  {
    key: 'profile',
    label: '个人中心',
  },
  {
    key: 'settings',
    label: '账号设置',
  },
  {
    type: 'divider',
  },
  {
    key: 'logout',
    label: '退出登录',
  },
]

function Layout() {
  const navigate = useNavigate()
  const location = useLocation()

  const handleMenuClick: MenuProps['onClick'] = (e) => {
    navigate(e.key)
  }

  return (
    <AntLayout className="layout">
      <Sider trigger={null} collapsible theme="light" className="sider">
        <div className="logo">
          <div className="logo-icon">🐉</div>
          <div className="logo-text">灵龙AI智能平台</div>
        </div>
        <div className="create-btn-wrapper">
          <Button type="primary" icon={<PlusOutlined />} block size="large">
            创建
          </Button>
        </div>
        <div className="menu-section">
          <Menu
            theme="light"
            mode="inline"
            selectedKeys={[location.pathname]}
            items={mainMenuItems}
            onClick={handleMenuClick}
            className="main-menu"
          />
        </div>
        <div className="menu-section personal-section">
          <div className="menu-label">个人工作区</div>
          <Menu
            theme="light"
            mode="inline"
            selectedKeys={[location.pathname]}
            items={personalMenuItems}
            onClick={handleMenuClick}
            className="personal-menu"
          />
        </div>
      </Sider>
      <AntLayout>
        <Header className="header">
          <div className="header-left">
            <div className="header-title">
              <ThunderboltOutlined className="header-title-icon" />
              <span>灵龙AI智能平台</span>
            </div>
          </div>
          <div className="header-right">
            <div className="header-stats">
              <span className="stat-item"><PartitionOutlined /> <span className="stat-label">智能体</span><span className="stat-value">12</span></span>
              <span className="stat-divider" />
              <span className="stat-item"><ApiOutlined /> <span className="stat-label">MCP工具</span><span className="stat-value">45</span></span>
            </div>
            <Badge count={3} size="small">
              <Button type="text" shape="circle" icon={<BellOutlined />} className="header-icon-btn" />
            </Badge>
            <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
              <div className="user-info">
                <Avatar style={{ background: 'linear-gradient(135deg, #667eea, #764ba2)' }} icon={<UserOutlined />} />
                <span className="username">管理员</span>
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
