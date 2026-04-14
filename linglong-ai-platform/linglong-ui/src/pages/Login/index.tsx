import { useState } from 'react'
import { Form, Input, Button, Card, message, Checkbox } from 'antd'
import { UserOutlined, LockOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { login } from '../../services/api'
import { useUserStore } from '../../stores/userStore'
import './index.css'

interface LoginForm {
  username: string
  password: string
  remember: boolean
}

const Login = () => {
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  const { setUser } = useUserStore()

  const onFinish = async (values: LoginForm) => {
    setLoading(true)
    try {
      const result = await login(values.username, values.password)
      if (result.success) {
        message.success('登录成功')
        setUser(result.data)
        navigate('/chat')
      } else {
        message.error(result.message || '登录失败')
      }
    } catch (error: any) {
      message.error(error.message || '登录失败，请稍后重试')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-container">
      <div className="login-background">
        <div className="login-card-wrapper">
          <Card className="login-card" bordered={false}>
            <div className="login-header">
              <div className="logo">🐉</div>
              <h1>玲珑AI平台</h1>
              <p>AI驱动的智能开发平台</p>
            </div>
            <Form
              name="login"
              initialValues={{ remember: true }}
              onFinish={onFinish}
              size="large"
            >
              <Form.Item
                name="username"
                rules={[{ required: true, message: '请输入用户名' }]}
              >
                <Input
                  prefix={<UserOutlined />}
                  placeholder="用户名"
                  autoComplete="username"
                />
              </Form.Item>
              <Form.Item
                name="password"
                rules={[{ required: true, message: '请输入密码' }]}
              >
                <Input.Password
                  prefix={<LockOutlined />}
                  placeholder="密码"
                  autoComplete="current-password"
                />
              </Form.Item>
              <Form.Item>
                <Form.Item name="remember" valuePropName="checked" noStyle>
                  <Checkbox>记住我</Checkbox>
                </Form.Item>
                <a className="login-form-forgot" href="#">
                  忘记密码
                </a>
              </Form.Item>
              <Form.Item>
                <Button type="primary" htmlType="submit" loading={loading} block>
                  登录
                </Button>
              </Form.Item>
            </Form>
            <div className="login-footer">
              <p>默认账号: admin / admin123</p>
            </div>
          </Card>
        </div>
      </div>
    </div>
  )
}

export default Login
