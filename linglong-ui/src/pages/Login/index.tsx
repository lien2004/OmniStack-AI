import { useState } from 'react'
import { Form, Input, Button, message, Checkbox, Tabs, Radio } from 'antd'
import { UserOutlined, LockOutlined, CrownOutlined, TeamOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { login } from '../../services/api'
import { useUserStore } from '../../stores/userStore'
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

        // 验证角色是否匹配
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
      {/* 左侧 - 品牌展示区 */}
      <div className="login-left">
        <div className="brand-content">
          <div className="brand-logo">
            <span className="brand-logo-icon">🐉</span>
            <div className="brand-title-wrapper">
              <span className="brand-title-main">灵龙AI+</span>
              <span className="brand-title-sub">智能开发平台</span>
            </div>
          </div>
          <div className="brand-description">
            <p className="brand-description-title">基于AI+DDD的智能开发平台</p>
            <p className="brand-description-content">
              通过大语言模型（LLM）+MCP协议+多智能体（Agent）
              <br />
              协作，实现业务代码全流程智能研发
            </p>
          </div>
          <div className="features-list">
            <div className="feature">🤖 多智能体全流程研发自动化</div>
            <div className="feature">📐 DDD驱动架构设计自动生成文档</div>
            <div className="feature">🏢 企业级多租户SaaS平台</div>
          </div>
        </div>
      </div>

      {/* 右侧 - 登录表单区 */}
      <div className="login-right">
        <div className="login-form-container">
          <div className="form-header">
            <h2>欢迎回来</h2>
            <p>登录灵龙AI+智能开发平台</p>
          </div>

          {/* 角色选择 */}
          <div style={{ marginBottom: 24 }}>
            <div style={{ marginBottom: 8, color: '#666', fontSize: 13 }}>请选择登录身份</div>
            <Radio.Group
              value={selectedRole}
              onChange={(e) => setSelectedRole(e.target.value)}
              style={{ width: '100%' }}
            >
              <Radio.Button
                value={0}
                style={{ width: '50%', textAlign: 'center' }}
              >
                <TeamOutlined style={{ marginRight: 6 }} />
                普通用户
              </Radio.Button>
              <Radio.Button
                value={1}
                style={{ width: '50%', textAlign: 'center' }}
              >
                <CrownOutlined style={{ marginRight: 6 }} />
                系统管理员
              </Radio.Button>
            </Radio.Group>
          </div>

          <Tabs
            activeKey="account"
            items={[
              {
                key: 'account',
                label: '账号密码登录',
                children: (
                  <Form onFinish={onFinish} size="large">
                    <Form.Item
                      name="username"
                      rules={[{ required: true, message: '请输入账号' }]}
                    >
                      <Input
                        prefix={<UserOutlined />}
                        placeholder="账号/手机号"
                        size="large"
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
                      />
                    </Form.Item>
                    <Form.Item>
                      <div className="form-options">
                        <Form.Item name="remember" valuePropName="checked" noStyle>
                          <Checkbox>记住我</Checkbox>
                        </Form.Item>
                        <a href="#">忘记密码？</a>
                      </div>
                    </Form.Item>
                    <Form.Item>
                      <Button type="primary" htmlType="submit" loading={loading} block size="large">
                        立即登录
                      </Button>
                    </Form.Item>
                  </Form>
                ),
              },
            ]}
          />

          <div className="register-link">
            还没有账号？<a onClick={() => navigate('/register')}>立即注册</a>
          </div>
        </div>
      </div>
    </div>
  )
}

export default Login
