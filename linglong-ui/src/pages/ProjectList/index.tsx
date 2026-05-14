import { useState, useEffect, useCallback } from 'react'
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
  message,
  Empty,
  Spin,
} from 'antd'
import {
  PlusOutlined,
  SearchOutlined,
  MoreOutlined,
  EditOutlined,
  DeleteOutlined,
  PlayCircleOutlined,
  ThunderboltOutlined,
  EyeOutlined,
  ReloadOutlined,
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { getProjects, deleteProject } from '@/services/api'

interface Project {
  id: string
  name: string
  description: string
  status: string
  techStack: string
  architectureType: string
  model: string
  requirement: string
  workflowId: string
  createTime: string
  updateTime: string
}

const statusMap: Record<string, { color: string; text: string }> = {
  init: { color: 'default', text: '初始化' },
  analyzing: { color: 'blue', text: '需求分析中' },
  designing: { color: 'orange', text: '架构设计中' },
  coding: { color: 'processing', text: '代码生成中' },
  testing: { color: 'purple', text: '测试验证中' },
  completed: { color: 'success', text: '已完成' },
  failed: { color: 'error', text: '失败' },
}

function ProjectList() {
  const navigate = useNavigate()
  const [searchText, setSearchText] = useState('')
  const [deleteModalVisible, setDeleteModalVisible] = useState(false)
  const [selectedProject, setSelectedProject] = useState<Project | null>(null)
  const [projects, setProjects] = useState<Project[]>([])
  const [loading, setLoading] = useState(false)

  // 加载项目列表
  const loadProjects = useCallback(async (keyword?: string) => {
    setLoading(true)
    try {
      const res = await getProjects(keyword ? { keyword } : undefined)
      if (res.data?.success) {
        setProjects(res.data.data || [])
      } else {
        setProjects([])
      }
    } catch (err) {
      console.error('获取项目列表失败', err)
      setProjects([])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadProjects()
  }, [loadProjects])

  // 搜索
  const handleSearch = () => {
    loadProjects(searchText.trim() || undefined)
  }

  const handleDelete = (project: Project) => {
    setSelectedProject(project)
    setDeleteModalVisible(true)
  }

  const confirmDelete = async () => {
    if (!selectedProject) return
    try {
      const res = await deleteProject(selectedProject.id)
      if (res.data?.success) {
        message.success('项目已删除')
        loadProjects()
      } else {
        message.error(res.data?.message || '删除失败')
      }
    } catch (err) {
      message.error('删除失败')
    }
    setDeleteModalVisible(false)
  }

  // 启动AI开发（跳转到CodeFlow并带上项目信息）
  const handleStartAIDev = (project: Project) => {
    navigate('/codeflow', {
      state: {
        projectId: project.id,
        requirement: project.requirement,
        model: project.model,
        projectName: project.name,
      },
    })
  }

  const columns: ColumnsType<Project> = [
    {
      title: '项目名称',
      dataIndex: 'name',
      key: 'name',
      width: 200,
      render: (text, record) => (
        <Button type="link" style={{ padding: 0, fontWeight: 500 }} onClick={() => navigate(`/projects/${record.id}`)}>
          {text}
        </Button>
      ),
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
      width: 280,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 120,
      render: (status) => {
        const s = statusMap[status] || { color: 'default', text: status }
        return <Tag color={s.color}>{s.text}</Tag>
      },
    },
    {
      title: '技术栈',
      dataIndex: 'techStack',
      key: 'techStack',
      width: 200,
      render: (text) => text || '-',
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 160,
      render: (text) => text ? new Date(text).toLocaleDateString('zh-CN') : '-',
    },
    {
      title: '更新时间',
      dataIndex: 'updateTime',
      key: 'updateTime',
      width: 160,
      render: (text) => text ? new Date(text).toLocaleDateString('zh-CN') : '-',
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      render: (_, record) => (
        <Space>
          <Button
            type="text"
            icon={<EyeOutlined />}
            title="查看详情"
            onClick={() => navigate(`/projects/${record.id}`)}
          />
          {record.status !== 'completed' && (
            <Button
              type="text"
              icon={<ThunderboltOutlined style={{ color: '#f97316' }} />}
              title="启动AI开发"
              onClick={() => handleStartAIDev(record)}
            />
          )}
          <Dropdown
            menu={{
              items: [
                {
                  key: 'view',
                  icon: <EyeOutlined />,
                  label: '查看代码',
                  onClick: () => navigate(`/projects/${record.id}`),
                },
                {
                  key: 'ai-dev',
                  icon: <PlayCircleOutlined />,
                  label: '启动AI开发',
                  onClick: () => handleStartAIDev(record),
                },
                {
                  key: 'edit',
                  icon: <EditOutlined />,
                  label: '编辑',
                  onClick: () => navigate(`/projects/${record.id}`),
                },
                { type: 'divider' },
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
    <div className="project-list" style={{ padding: '0' }}>
      <Card
        title={
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <span style={{ fontSize: 18, fontWeight: 600 }}>项目列表</span>
            <Tag color="blue">{projects.length} 个项目</Tag>
          </div>
        }
        extra={
          <Space>
            <Input
              placeholder="搜索项目"
              prefix={<SearchOutlined />}
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
              onPressEnter={handleSearch}
              style={{ width: 250 }}
              allowClear
            />
            <Button icon={<ReloadOutlined />} onClick={() => loadProjects()}>
              刷新
            </Button>
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
        <Spin spinning={loading}>
          {projects.length === 0 && !loading ? (
            <Empty
              description="暂无项目"
              style={{ padding: 60 }}
            >
              <Space>
                <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/projects/create')}>
                  新建项目
                </Button>
                <Button icon={<ThunderboltOutlined />} onClick={() => navigate('/codeflow')}>
                  Agent智能开发
                </Button>
              </Space>
            </Empty>
          ) : (
            <Table
              columns={columns}
              dataSource={projects}
              rowKey="id"
              pagination={{
                pageSize: 10,
                showSizeChanger: true,
                showTotal: (total) => `共 ${total} 条`,
              }}
            />
          )}
        </Spin>
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
        <p>此操作不可恢复，项目关联的代码文件也将被删除。</p>
      </Modal>
    </div>
  )
}

export default ProjectList
