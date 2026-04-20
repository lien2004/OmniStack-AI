import { useState, useEffect } from 'react'
import { Card, Form, Input, Button, message, Tag, Avatar, Divider, Modal } from 'antd'
import { UserOutlined, MobileOutlined, LockOutlined, CrownOutlined, TeamOutlined } from '@ant-design/icons'
import { getUserProfile, updateUserProfile } from '../../services/api'
import { useUserStore } from '../../stores/userStore'
import './index.css'

const Profile = () => {
  const [profile, setProfile] = useState<any>(null)
  const [loading, setLoading] = useState(false)
  const [pwdVisible, setPwdVisible] = useState(false)
  const [form] = Form.useForm()
  const [pwdForm] = Form.useForm()
  const { setUser } = useUserStore()

  useEffect(() => {
    loadProfile()
  }, [])

  const loadProfile = async () => {
    try {
      const res = await getUserProfile()
      if (res.success) {
        setProfile(res.data)
        form.setFieldsValue({ mobile: res.data.mobile })
      }
    } catch (e: any) {
      message.error(e.message || '加载个人信息失败')
    }
  }

  const handleUpdateMobile = async (values: any) => {
    setLoading(true)
    try {
      const res = await updateUserProfile({ mobile: values.mobile })
      if (res.success) {
        message.success('手机号更新成功')
        const updated = { ...profile, mobile: values.mobile }
        setProfile(updated)
        setUser(updated)
      } else {
        message.error(res.message || '更新失败')
      }
    } catch (e: any) {
      message.error(e.message || '更新失败')
    } finally {
      setLoading(false)
    }
  }

  const handleChangePassword = async (values: any) => {
    setLoading(true)
    try {
      const res = await updateUserProfile({
        oldPassword: values.oldPassword,
        newPassword: values.newPassword,
      })
      if (res.success) {
        message.success('密码修改成功')
        setPwdVisible(false)
        pwdForm.resetFields()
      } else {
        message.error(res.message || '修改失败')
      }
    } catch (e: any) {
      message.error(e.message || '修改失败')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="profile-container">
      {/* 头部信息卡片 */}
      <Card className="profile-header-card">
        <div className="profile-avatar-section">
          <Avatar
            size={80}
            style={{ background: 'linear-gradient(135deg, #667eea, #764ba2)', fontSize: 32 }}
            icon={<UserOutlined />}
          />
          <div className="profile-basic-info">
            <div className="profile-username">{profile?.username || '--'}</div>
            <Tag
              color={profile?.role === 1 ? 'gold' : 'blue'}
              icon={profile?.role === 1 ? <CrownOutlined /> : <TeamOutlined />}
            >
              {profile?.role === 1 ? '系统管理员' : '普通用户'}
            </Tag>
          </div>
        </div>
      </Card>

      {/* 账号信息 */}
      <Card title="账号信息" style={{ marginTop: 16 }}>
        <div className="profile-info-row">
          <span className="profile-label">用户名</span>
          <span className="profile-value">{profile?.username || '--'}</span>
        </div>
        <Divider style={{ margin: '12px 0' }} />
        <div className="profile-info-row">
          <span className="profile-label">账号 ID</span>
          <span className="profile-value">{profile?.id || '--'}</span>
        </div>
        <Divider style={{ margin: '12px 0' }} />
        <div className="profile-info-row">
          <span className="profile-label">角色</span>
          <span className="profile-value">
            <Tag color={profile?.role === 1 ? 'gold' : 'blue'}>
              {profile?.role === 1 ? '系统管理员' : '普通用户'}
            </Tag>
          </span>
        </div>
        <Divider style={{ margin: '12px 0' }} />
        <div className="profile-info-row">
          <span className="profile-label">登录密码</span>
          <span className="profile-value">
            ••••••••
            <Button
              type="link"
              size="small"
              style={{ marginLeft: 8 }}
              onClick={() => setPwdVisible(true)}
            >
              修改密码
            </Button>
          </span>
        </div>
      </Card>

      {/* 修改手机号 */}
      <Card title="手机号" style={{ marginTop: 16 }}>
        <Form form={form} onFinish={handleUpdateMobile} layout="inline">
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
              style={{ width: 220 }}
            />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading}>
              保存修改
            </Button>
          </Form.Item>
        </Form>
      </Card>

      {/* 修改密码弹窗 */}
      <Modal
        title="修改密码"
        open={pwdVisible}
        onCancel={() => { setPwdVisible(false); pwdForm.resetFields() }}
        footer={null}
      >
        <Form form={pwdForm} onFinish={handleChangePassword} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item
            name="oldPassword"
            label="原密码"
            rules={[{ required: true, message: '请输入原密码' }]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="请输入原密码" />
          </Form.Item>
          <Form.Item
            name="newPassword"
            label="新密码"
            rules={[
              { required: true, message: '请输入新密码' },
              { min: 6, message: '密码不能少于6位' },
            ]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="新密码（至少6位）" />
          </Form.Item>
          <Form.Item
            name="confirmPassword"
            label="确认新密码"
            dependencies={['newPassword']}
            rules={[
              { required: true, message: '请再次输入新密码' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('newPassword') === value) {
                    return Promise.resolve()
                  }
                  return Promise.reject(new Error('两次密码输入不一致'))
                },
              }),
            ]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="再次输入新密码" />
          </Form.Item>
          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Button onClick={() => { setPwdVisible(false); pwdForm.resetFields() }} style={{ marginRight: 8 }}>
              取消
            </Button>
            <Button type="primary" htmlType="submit" loading={loading}>
              确认修改
            </Button>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default Profile
