import { useState, useEffect } from 'react'
import {
  Table, Button, Modal, Form, Input, Select, Popconfirm, message, Tag, Space, Card
} from 'antd'
import { CrownOutlined, TeamOutlined, EditOutlined, DeleteOutlined, PlusOutlined } from '@ant-design/icons'
import { adminGetUsers, adminUpdateUser, adminDeleteUser } from '../../services/api'
import { useUserStore } from '../../stores/userStore'
import { useNavigate } from 'react-router-dom'

const { Option } = Select

const AdminUsers = () => {
  const [users, setUsers] = useState<any[]>([])
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [editingUser, setEditingUser] = useState<any>(null)
  const [form] = Form.useForm()
  const { isAdmin } = useUserStore()
  const navigate = useNavigate()

  useEffect(() => {
    if (!isAdmin()) {
      message.error('无权限访问，仅系统管理员可用')
      navigate('/')
      return
    }
    loadUsers()
  }, [])

  const loadUsers = async () => {
    setLoading(true)
    try {
      const res = await adminGetUsers()
      if (res.success) {
        setUsers(res.data || [])
      }
    } catch (e: any) {
      message.error(e.message || '加载用户列表失败')
    } finally {
      setLoading(false)
    }
  }

  const handleEdit = (record: any) => {
    setEditingUser(record)
    form.setFieldsValue({
      username: record.username,
      mobile: record.mobile,
      role: record.role,
      password: '',
    })
    setModalVisible(true)
  }

  const handleDelete = async (id: number) => {
    try {
      const res = await adminDeleteUser(id)
      if (res.success) {
        message.success('删除成功')
        loadUsers()
      } else {
        message.error(res.message || '删除失败')
      }
    } catch (e: any) {
      message.error(e.message || '删除失败')
    }
  }

  const handleSave = async (values: any) => {
    if (!editingUser) return
    setLoading(true)
    try {
      const payload: any = {
        username: values.username,
        mobile: values.mobile,
        role: values.role,
      }
      if (values.password) {
        payload.password = values.password
      }
      const res = await adminUpdateUser(editingUser.id, payload)
      if (res.success) {
        message.success('修改成功')
        setModalVisible(false)
        loadUsers()
      } else {
        message.error(res.message || '修改失败')
      }
    } catch (e: any) {
      message.error(e.message || '修改失败')
    } finally {
      setLoading(false)
    }
  }

  const columns = [
    { title: 'ID', dataIndex: 'id', width: 60 },
    { title: '用户名', dataIndex: 'username' },
    { title: '手机号', dataIndex: 'mobile', render: (v: string) => v || '--' },
    {
      title: '角色',
      dataIndex: 'role',
      render: (role: number) =>
        role === 1 ? (
          <Tag color="gold" icon={<CrownOutlined />}>系统管理员</Tag>
        ) : (
          <Tag color="blue" icon={<TeamOutlined />}>普通用户</Tag>
        ),
    },
    {
      title: '操作',
      render: (_: any, record: any) => (
        <Space>
          <Button
            type="primary"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            编辑
          </Button>
          <Popconfirm
            title="确认删除该用户？"
            onConfirm={() => handleDelete(record.id)}
            okText="确认"
            cancelText="取消"
          >
            <Button danger size="small" icon={<DeleteOutlined />}>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div style={{ padding: 24 }}>
      <Card
        title={
          <Space>
            <CrownOutlined style={{ color: '#f59e0b' }} />
            用户管理
          </Space>
        }
        extra={
          <Button icon={<PlusOutlined />} onClick={loadUsers}>
            刷新
          </Button>
        }
      >
        <Table
          dataSource={users}
          columns={columns}
          rowKey="id"
          loading={loading}
          pagination={{ pageSize: 10, showSizeChanger: false }}
        />
      </Card>

      {/* 编辑弹窗 */}
      <Modal
        title="编辑用户"
        open={modalVisible}
        onCancel={() => setModalVisible(false)}
        footer={null}
      >
        <Form form={form} onFinish={handleSave} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item
            name="username"
            label="用户名"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input placeholder="用户名" />
          </Form.Item>
          <Form.Item
            name="mobile"
            label="手机号"
            rules={[{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' }]}
          >
            <Input placeholder="手机号" />
          </Form.Item>
          <Form.Item name="role" label="角色">
            <Select>
              <Option value={0}>普通用户</Option>
              <Option value={1}>系统管理员</Option>
            </Select>
          </Form.Item>
          <Form.Item name="password" label="新密码（不填则不修改）">
            <Input.Password placeholder="不填则保留原密码" />
          </Form.Item>
          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Button onClick={() => setModalVisible(false)} style={{ marginRight: 8 }}>
              取消
            </Button>
            <Button type="primary" htmlType="submit" loading={loading}>
              保存
            </Button>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default AdminUsers
