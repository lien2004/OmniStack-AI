import { useState } from 'react'
import { Form, Input, Button, message, Checkbox } from 'antd'
import { UserOutlined, LockOutlined, EyeInvisibleOutlined, EyeOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { login } from '../../services/api'
import { useUserStore } from '../../stores/userStore'
import logoImg from '../../img/重工AI.png'
import './index.css'

const Login = () => {
  const [loading, setLoading] = useState(false)
  const [selectedRole, setSelectedRole] = useState<number>(0)
  const navigate = useNavigate()
  const { setUser, setToken } = useUserStore()

  const onFinish = async (values: any) => {
    setLoading(true)
    try {
      const result = await login({ username: values.username, password: values.password })
      if (result.success) {
        const userData = result.data?.user || result.data
        const token = result.data?.token

        if (userData.role !== selectedRole) {
          message.error(
            selectedRole === 1
              ? '该账号不是系统管理员，请切换角色后重试'
              : '该账号为系统管理员，请选择管理员身份登录'
          )
          return
        }

        setUser(userData)
        if (token) {
          setToken(token)
          localStorage.setItem('token', token)
        }
        message.success('登录成功')
        navigate('/chat')
      } else {
        message.error(result.message || '登录失败')
      }
    } catch (error: any) {
      message.error(error.message || '登录失败')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-container-split">
      {/* 左侧：品牌区 */}
      <div className="login-left">
        {/* AI 科技流动背景 */}
        <div className="bg-grid" />
        <div className="floating-orb orb-1" />
        <div className="floating-orb orb-2" />
        <div className="floating-orb orb-3" />
        <div className="floating-orb orb-4" />
        <div className="floating-orb orb-5" />
        <div className="floating-line line-1" />
        <div className="floating-line line-2" />
        <div className="floating-line line-3" />
        <div className="brand-content">
          <div className="brand-logo">
            <span className="brand-logo-icon">
              <img src={logoImg} alt="灵龙AI Logo" />
            </span>
            <div className="brand-title-wrapper">
              <span className="brand-title-main">灵龙AI</span>
              <span className="brand-title-sub">LINGLONG AI</span>
            </div>
          </div>

          <div className="brand-description">
            <p className="brand-description-title">灵动架构<br />龙腾智能</p>
            <p className="brand-description-content">基于LLM的智能Agent平台</p>
            <p className="brand-description-contentmain">
              集成代码开发、智能对话、智能体中心与应用广场的下一代数字化引擎
            </p>
          </div>

          <div className="features-list">
            <div className="feature-item">
              <div className="feature-icon">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#79a3e2" strokeWidth="2">
                  <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z" />
                </svg>
              </div>
              <div className="feature-text">
                <h4 className="feature-title">AI 对话助手</h4>
                <p className="feature-desc">基于大模型的智能问答、多轮上下文记忆与知识库检索</p>
              </div>
            </div>

            <div className="feature-item">
              <div className="feature-icon">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#79a3e2" strokeWidth="2">
                  <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" />
                </svg>
              </div>
              <div className="feature-text">
                <h4 className="feature-title">智能体中心</h4>
                <p className="feature-desc">MCP 协议工具集成，打造全生命周期自动化的能力边界</p>
              </div>
            </div>

            <div className="feature-item">
              <div className="feature-icon">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#79a3e2" strokeWidth="2">
                  <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
                </svg>
              </div>
              <div className="feature-text">
                <h4 className="feature-title">项目工作台</h4>
                <p className="feature-desc">微服务版本管理与路由配置，一站式项目生命周期管理</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* 右侧：登录表单 */}
      <div className="login-right">
        <div className="login-form-container">
          <div className="form-header">
            <h2>欢迎回来</h2>
            <p>登录灵龙AI智能平台</p>
          </div>

          {/* 角色选择（与注册页面一致的样式） */}
          <div className="role-tabs">
            <div
              className={`role-tab ${selectedRole === 0 ? 'active' : ''}`}
              onClick={() => setSelectedRole(0)}
            >
              <UserOutlined style={{ marginRight: 6 }} />
              普通用户
            </div>
            <div
              className={`role-tab ${selectedRole === 1 ? 'active' : ''}`}
              onClick={() => setSelectedRole(1)}
            >
              <LockOutlined style={{ marginRight: 6 }} />
              系统管理员
            </div>
          </div>

          {/* 登录表单标题 */}
          <div className="login-tab-header">
            <span className="login-tab-title active">账号密码登录</span>
          </div>

          <Form onFinish={onFinish} size="large" className="login-form">
            <Form.Item
              name="username"
              rules={[{ required: true, message: '请输入账号' }]}
            >
              <Input
                prefix={<UserOutlined />}
                placeholder="账号/手机号"
                size="large"
                className="login-input"
              />
            </Form.Item>
            <Form.Item
              name="password"
              rules={[{ required: true, message: '请输入密码' }]}
            >
              <Input.Password
                prefix={<LockOutlined />}
                placeholder="密码"
                size="large"
                className="login-input"
                iconRender={(visible) => (visible ? <EyeOutlined /> : <EyeInvisibleOutlined />)}
              />
            </Form.Item>

            <div className="form-options">
              <Form.Item name="remember" valuePropName="checked" noStyle>
                <Checkbox className="remember-checkbox">记住我</Checkbox>
              </Form.Item>
              <a onClick={() => navigate('/forget-password')} className="forgot-password">忘记密码？</a>
            </div>

            <Form.Item>
              <Button
                type="primary"
                htmlType="submit"
                loading={loading}
                block
                size="large"
                className="login-button"
              >
                立即登录
              </Button>
            </Form.Item>
          </Form>

          <div className="register-link">
            还没有账号？
            <a onClick={() => navigate('/register')} className="register-link-highlight">立即注册</a>
          </div>
        </div>
      </div>
    </div>
  )
}

export default Login
