import { useState } from 'react'
import { Form, Input, Button, message, Tabs, Radio } from 'antd'
import { UserOutlined, LockOutlined, MobileOutlined, SafetyCertificateOutlined, CrownOutlined, TeamOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { register, sendSmsCode } from '../../services/api'
import './index.css'

const Register = () => {
  const [loading, setLoading] = useState(false)
  const [codeLoading, setCodeLoading] = useState(false)
  const [countdown, setCountdown] = useState(0)
  const navigate = useNavigate()
  const [form] = Form.useForm()

  // 获取验证码
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
      // 倒计时 60 秒
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

  // 注册提交
  const onFinish = async (values: any) => {
    const { username, mobile, password, code } = values

    setLoading(true)
    try {
      // 传给后端：用户名、密码、手机号、验证码
      const result = await register({ username, password, mobile, verifyCode: code, role: values.role ?? 0 })
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
      {/* 左侧品牌区 */}
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

      {/* 右侧注册表单 */}
      <div className="login-right">
        <div className="login-form-container">
          <div className="form-header">
            <h2>创建账号</h2>
            <p>注册灵龙AI+智能开发平台</p>
          </div>

          <Tabs activeKey="register" items={[{
            key: 'register',
            label: '账号注册',
            children: (
              <Form form={form} onFinish={onFinish} size="large">
                {/* 角色选择 */}
                <Form.Item name="role" initialValue={0}>
                  <Radio.Group style={{ width: '100%' }}>
                    <Radio.Button value={0} style={{ width: '50%', textAlign: 'center' }}>
                      <TeamOutlined style={{ marginRight: 6 }} />
                      普通用户
                    </Radio.Button>
                    <Radio.Button value={1} style={{ width: '50%', textAlign: 'center' }}>
                      <CrownOutlined style={{ marginRight: 6 }} />
                      系统管理员
                    </Radio.Button>
                  </Radio.Group>
                </Form.Item>

                <Form.Item
                  name="username"
                  rules={[{ required: true, message: '请输入用户名' }]}
                >
                  <Input prefix={<UserOutlined />} placeholder="用户名" size="large" />
                </Form.Item>

                <Form.Item
                  name="mobile"
                  rules={[
                    { required: true, message: '请输入手机号' },
                    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' },
                  ]}
                >
                  <Input prefix={<MobileOutlined />} placeholder="手机号" size="large" />
                </Form.Item>

                <Form.Item
                  name="password"
                  rules={[{ required: true, message: '请输入密码' }]}
                >
                  <Input.Password prefix={<LockOutlined />} placeholder="密码（至少6位）" size="large" />
                </Form.Item>

                {/* 验证码 */}
                <Form.Item
                  name="code"
                  rules={[{ required: true, message: '请输入验证码' }]}
                >
                  <div style={{ display: 'flex', gap: 10 }}>
                    <Input
                      prefix={<SafetyCertificateOutlined />}
                      placeholder="验证码"
                      size="large"
                      style={{ flex: 1 }}
                    />
                    <Button
                      size="large"
                      onClick={handleSendCode}
                      disabled={codeLoading || countdown > 0}
                      style={{ width: 130 }}
                    >
                      {countdown > 0 ? `${countdown}秒后重发` : '获取验证码'}
                    </Button>
                  </div>
                </Form.Item>

                <Form.Item>
                  <Button type="primary" htmlType="submit" loading={loading} block size="large">
                    立即注册
                  </Button>
                </Form.Item>
              </Form>
            ),
          }]} />

          <div className="register-link">
            已有账号？<a onClick={() => navigate('/login')}>立即登录</a>
          </div>
        </div>
      </div>
    </div>
  )
}

export default Register