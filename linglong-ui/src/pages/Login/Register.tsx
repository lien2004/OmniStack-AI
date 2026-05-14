import { useState } from 'react'
import { Form, Input, Button, message } from 'antd'
import { UserOutlined, LockOutlined, MobileOutlined, SafetyCertificateOutlined, EyeOutlined, EyeInvisibleOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { register, sendSmsCode } from '../../services/api'
import './index.css'

const Register = () => {
  const [loading, setLoading] = useState(false)
  const [codeLoading, setCodeLoading] = useState(false)
  const [countdown, setCountdown] = useState(0)
  const [selectedRole, setSelectedRole] = useState<number>(0)
  const navigate = useNavigate()
  const [form] = Form.useForm()

  const handleSendCode = async () => {
    const mobile = form.getFieldValue('mobile')
    if (!mobile) {
      message.warning('请输入手机号')
      return
    }
    if (!/^1[3-9]\d{9}$/.test(mobile)) {
      message.warning('手机号格式不正确')
      return
    }

    setCodeLoading(true)
    try {
      const res = await sendSmsCode({ mobile })
      const code = res?.data
      if (code) {
        form.setFieldsValue({ code })
        message.success(`验证码已获取：${code}（已自动填入）`, 5)
      } else {
        message.success('验证码发送成功，请查收')
      }
      let second = 60
      setCountdown(second)
      const timer = setInterval(() => {
        second--
        setCountdown(second)
        if (second <= 0) {
          clearInterval(timer)
          setCodeLoading(false)
        }
      }, 1000)
    } catch (err) {
      message.error('验证码发送失败')
      setCodeLoading(false)
    }
  }

  const onFinish = async (values: any) => {
    const { username, mobile, password, code } = values

    setLoading(true)
    try {
      const result = await register({ username, password, mobile, verifyCode: code, role: selectedRole })
      if (result.success) {
        message.success('注册成功，请登录')
        navigate('/login')
      } else {
        message.error(result.message || '注册失败')
      }
    } catch (error: any) {
      message.error(error.message || '注册失败，请稍后重试')
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
              <img src="src/img/重工AI.png" alt="灵龙AI Logo" />
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

      {/* 右侧注册表单 */}
      <div className="login-right">
        <div className="login-form-container">
          <div className="form-header">
            <h2>创建账号</h2>
            <p>注册灵龙AI+智能开发平台</p>
          </div>

          {/* 角色选择（与登录页面一致的样式） */}
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

          {/* 注册表单标题 */}
          <div className="login-tab-header">
            <span className="login-tab-title active">账号注册</span>
          </div>

          <Form form={form} onFinish={onFinish} size="large" className="login-form">
            <Form.Item
              name="username"
              rules={[{ required: true, message: '请输入用户名' }]}
            >
              <Input
                prefix={<UserOutlined />}
                placeholder="用户名"
                size="large"
                className="login-input"
              />
            </Form.Item>

            <Form.Item
              name="mobile"
              rules={[
                { required: true, message: '请输入手机号' },
                { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' },
              ]}
            >
              <Input
                prefix={<MobileOutlined />}
                placeholder="手机号"
                size="large"
                className="login-input"
              />
            </Form.Item>

            <Form.Item
              name="password"
              rules={[
                { required: true, message: '请输入密码' },
                { min: 6, message: '密码至少6位' },
              ]}
            >
              <Input.Password
                prefix={<LockOutlined />}
                placeholder="密码（至少6位）"
                size="large"
                className="login-input"
                iconRender={(visible) => (visible ? <EyeOutlined /> : <EyeInvisibleOutlined />)}
              />
            </Form.Item>

            <Form.Item
              name="code"
              rules={[{ required: true, message: '请输入验证码' }]}
            >
              <div className="code-input-wrapper">
                <Input
                  prefix={<SafetyCertificateOutlined />}
                  placeholder="验证码"
                  size="large"
                  className="login-input"
                />
                <Button
                  size="large"
                  onClick={handleSendCode}
                  disabled={codeLoading || countdown > 0}
                  className="code-button"
                >
                  {countdown > 0 ? `${countdown}秒后重发` : '获取验证码'}
                </Button>
              </div>
            </Form.Item>

            <Form.Item>
              <Button
                type="primary"
                htmlType="submit"
                loading={loading}
                block
                size="large"
                className="login-button"
              >
                立即注册
              </Button>
            </Form.Item>
          </Form>

          <div className="register-link">
            已有账号？
            <a onClick={() => navigate('/login')} className="register-link-highlight">立即登录</a>
          </div>
        </div>
      </div>
    </div>
  )
}

export default Register
