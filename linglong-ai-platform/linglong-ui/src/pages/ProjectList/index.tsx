import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Card,
  Table,
  Button,
  Input,
  Tag,
  Space,
  Dropdown,
  Modal,
} from 'antd'
import {
  PlusOutlined,
  SearchOutlined,
  MoreOutlined,
  EditOutlined,
  DeleteOutlined,
  PlayCircleOutlined,
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'

interface Project {
  id: string
  name: string
  description: string
  status: string
  techStack: string
  createTime: string
  updateTime: string
}

const mockData: Project[] = [
  {
    id: '1',
    name: '电商订单系统',
    description: '基于DDD的电商订单管理服务',
    status: 'coding',
    techStack: 'Spring Boot + PostgreSQL',
    createTime: '2024-01-15',
    updateTime: '2024-01-20',
  },
  {
    id: '2',
    name: '用户权限管理',
    description: 'RBAC权限管理系统',
    status: 'completed',
    techStack: 'Spring Boot + Redis',
    createTime: '2024-01-10',
    updateTime: '2024-01-18',
  },
]

const statusMap: Record<string, { color: string; text: string }> = {
  init: { color: 'default', text: '初始化' },
  analyzing: { color: 'blue', text: '需求分析' },
  designing: { color: 'orange', text: '架构设计' },
  coding: { color: 'processing', text: '代码生成' },
  testing: { color: 'purple', text: '测试验证' },
  completed: { color: 'success', text: '已完成' },
}

function ProjectList() {
  const navigate = useNavigate()
  const [searchText, setSearchText] = useState('')
  const [deleteModalVisible, setDeleteModalVisible] = useState(false)
  const [selectedProject, setSelectedProject] = useState<Project | null>(null)

  const handleDelete = (project: Project) => {
    setSelectedProject(project)
    setDeleteModalVisible(true)
  }

  const confirmDelete = () => {
    // TODO: 调用删除API
    setDeleteModalVisible(false)
  }

  const columns: ColumnsType<Project> = [
    {
      title: '项目名称',
      dataIndex: 'name',
      key: 'name',
      render: (text, record) => (
        <Button type="link" onClick={() => navigate(`/projects/${record.id}`)}>
          {text}
        </Button>
      ),
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status) => (
        <Tag color={statusMap[status].color}>{statusMap[status].text}</Tag>
      ),
    },
    {
      title: '技术栈',
      dataIndex: 'techStack',
      key: 'techStack',
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
    },
    {
      title: '更新时间',
      dataIndex: 'updateTime',
      key: 'updateTime',
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space>
          <Button
            type="text"
            icon={<PlayCircleOutlined />}
            onClick={() => navigate(`/projects/${record.id}`)}
          />
          <Dropdown
            menu={{
              items: [
                {
                  key: 'edit',
                  icon: <EditOutlined />,
                  label: '编辑',
                  onClick: () => navigate(`/projects/${record.id}`),
                },
                {
                  key: 'delete',
                  icon: <DeleteOutlined />,
                  label: '删除',
                  danger: true,
                  onClick: () => handleDelete(record),
                },
              ],
            }}
          >
            <Button type="text" icon={<MoreOutlined />} />
          </Dropdown>
        </Space>
      ),
    },
  ]

  return (
    <div className="project-list">
      <Card
        title="项目列表"
        extra={
          <Space>
            <Input
              placeholder="搜索项目"
              prefix={<SearchOutlined />}
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
              style={{ width: 250 }}
            />
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => navigate('/projects/create')}
            >
              新建项目
            </Button>
          </Space>
        }
      >
        <Table
          columns={columns}
          dataSource={mockData}
          rowKey="id"
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条`,
          }}
        />
      </Card>

      <Modal
        title="确认删除"
        open={deleteModalVisible}
        onOk={confirmDelete}
        onCancel={() => setDeleteModalVisible(false)}
        okText="确认"
        cancelText="取消"
        okButtonProps={{ danger: true }}
      >
        <p>
          确定要删除项目 <strong>{selectedProject?.name}</strong> 吗？
        </p>
        <p>此操作不可恢复。</p>
      </Modal>
    </div>
  )
}

export default ProjectList
