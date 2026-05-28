import React, { useEffect, useState } from 'react'
import {
  Alert,
  Button,
  Card,
  Drawer,
  Form,
  Input,
  Modal,
  Popconfirm,
  Progress,
  Radio,
  Select,
  Space,
  Statistic,
  Table,
  Tag,
  Tooltip,
  Typography,
  Upload,
  message as antdMessage,
} from 'antd'
import type { UploadProps } from 'antd'
import {
  AppstoreOutlined,
  CloudUploadOutlined,
  DatabaseOutlined,
  DeleteOutlined,
  EditOutlined,
  FileTextOutlined,
  FolderOpenOutlined,
  HddOutlined,
  InboxOutlined,
  PlusOutlined,
  ReloadOutlined,
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import {
  createKb,
  deleteKb,
  fetchKbList,
  fetchOverview,
  updateKb,
  uploadDocument,
  type KnowledgeBaseItem,
  type OverviewStats,
} from '@/services/knowledgeBase'
import './kbList.css'

const { Title, Text } = Typography
const { Dragger } = Upload

const ACCEPT = [
  '.pdf', '.docx', '.doc', '.rtf',
  '.xlsx', '.xls', '.xlsm',
  '.pptx', '.ppt',
  '.txt', '.md', '.markdown',
  '.html', '.htm',
  '.json', '.yaml', '.yml', '.xml', '.csv',
].join(',')

const CATEGORY_OPTIONS = [
  { value: 'document', label: '通用文档' },
  { value: 'technical', label: '技术文档' },
  { value: 'business', label: '业务资料' },
  { value: 'faq', label: '常见问题' },
  { value: 'other', label: '其他' },
]

const EMBEDDING_MODELS = [
  { value: 'embedding-3', label: 'embedding-3 (智谱)' },
  { value: 'bge-m3', label: 'bge-m3 (BAAI)' },
  { value: 'text-embedding-3-large', label: 'text-embedding-3-large (OpenAI)' },
  { value: 'text-embedding-v3', label: 'text-embedding-v3 (通义)' },
]

const formatSize = (bytes?: number) => {
  if (!bytes || bytes <= 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let v = bytes
  let i = 0
  while (v >= 1024 && i < units.length - 1) {
    v /= 1024
    i++
  }
  return `${v.toFixed(v >= 100 ? 0 : 1)} ${units[i]}`
}

const KnowledgeBaseListPage: React.FC = () => {
  const navigate = useNavigate()
  const [list, setList] = useState<KnowledgeBaseItem[]>([])
  const [overview, setOverview] = useState<OverviewStats>({
    knowledgeBaseCount: 0,
    documentCount: 0,
    chunkCount: 0,
    sizeBytes: 0,
  })
  const [loading, setLoading] = useState(false)
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<KnowledgeBaseItem | null>(null)
  const [form] = Form.useForm()

  // 快速上传
  const [uploadOpen, setUploadOpen] = useState(false)
  const [uploadKbId, setUploadKbId] = useState<string | undefined>(undefined)
  const [uploadName, setUploadName] = useState('')
  const [uploadCategory, setUploadCategory] = useState('document')
  const [uploading, setUploading] = useState(false)
  const [uploadProgress, setUploadProgress] = useState(0)

  const loadAll = async () => {
    setLoading(true)
    try {
      const [items, stats] = await Promise.all([fetchKbList(), fetchOverview()])
      setList(items)
      setOverview(stats)
    } catch (e: any) {
      antdMessage.error(e?.response?.data?.message || '加载知识库失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadAll()
  }, [])

  const openCreate = () => {
    setEditing(null)
    form.resetFields()
    form.setFieldsValue({
      embeddingModel: 'embedding-3',
      dimensions: 1024,
    })
    setModalOpen(true)
  }

  const openEdit = (kb: KnowledgeBaseItem) => {
    setEditing(kb)
    form.setFieldsValue(kb)
    setModalOpen(true)
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      if (editing) {
        await updateKb(editing.id, values)
        antdMessage.success('知识库已更新')
      } else {
        await createKb(values)
        antdMessage.success('知识库已创建')
      }
      setModalOpen(false)
      loadAll()
    } catch (e: any) {
      if (e?.errorFields) return
      antdMessage.error(e?.response?.data?.message || '操作失败')
    }
  }

  const handleDelete = async (kb: KnowledgeBaseItem) => {
    try {
      await deleteKb(kb.id)
      antdMessage.success('知识库已删除')
      loadAll()
    } catch (e: any) {
      antdMessage.error(e?.response?.data?.message || '删除失败')
    }
  }

  const openUpload = () => {
    if (list.length === 0) {
      antdMessage.warning('请先创建知识库')
      return
    }
    // 默认选第一个，若已有选择则保留
    setUploadKbId(prev => prev ?? list[0].id)
    setUploadName('')
    setUploadCategory('document')
    setUploadOpen(true)
  }

  const uploadProps: UploadProps = {
    name: 'file',
    multiple: true,
    accept: ACCEPT,
    showUploadList: false,
    customRequest: async ({ file, onSuccess, onError }: any) => {
      if (!uploadKbId) {
        antdMessage.warning('请先选择目标知识库')
        onError?.(new Error('未选择知识库'))
        return
      }
      setUploading(true)
      setUploadProgress(5)
      try {
        await uploadDocument(
          uploadKbId,
          file as File,
          uploadName.trim() || undefined,
          uploadCategory,
          (e: any) => {
            if (e.total) {
              setUploadProgress(Math.min(95, Math.round((e.loaded / e.total) * 95)))
            }
          }
        )
        setUploadProgress(100)
        antdMessage.success(`「${(file as File).name}」已上传并向量化`)
        setUploadName('')
        loadAll()
        onSuccess?.({})
      } catch (e: any) {
        antdMessage.error(e?.response?.data?.message || '上传失败')
        onError?.(e)
      } finally {
        setUploading(false)
        setTimeout(() => setUploadProgress(0), 1500)
      }
    },
  }

  const columns = [
    {
      title: '名称',
      dataIndex: 'name',
      key: 'name',
      render: (text: string, record: KnowledgeBaseItem) => (
        <div>
          <a className="kb-link" onClick={() => navigate(`/knowledge/${record.id}/documents`)}>
            <DatabaseOutlined style={{ marginRight: 6, color: '#1677ff' }} />
            {text}
          </a>
          {record.description && (
            <div style={{ fontSize: 12, color: '#8c8c8c', marginTop: 2 }}>
              {record.description}
            </div>
          )}
        </div>
      ),
    },
    {
      title: 'Embedding 模型',
      dataIndex: 'embeddingModel',
      key: 'embeddingModel',
      width: 200,
      render: (v: string, record: KnowledgeBaseItem) => (
        <Space size={4}>
          <Tag color="purple">{v}</Tag>
          <Text type="secondary" style={{ fontSize: 12 }}>{record.dimensions} 维</Text>
        </Space>
      ),
    },
    {
      title: 'Collection',
      dataIndex: 'collectionName',
      key: 'collectionName',
      width: 160,
      render: (v: string) => <Tag color="geekblue">{v}</Tag>,
    },
    {
      title: '文档数',
      dataIndex: 'documentCount',
      key: 'documentCount',
      width: 90,
      align: 'right' as const,
      render: (v: number) => <Text strong>{v ?? 0}</Text>,
    },
    {
      title: '分块数',
      dataIndex: 'chunkCount',
      key: 'chunkCount',
      width: 90,
      align: 'right' as const,
      render: (v: number) => <Text>{(v ?? 0).toLocaleString()}</Text>,
    },
    {
      title: '容量',
      dataIndex: 'sizeBytes',
      key: 'sizeBytes',
      width: 100,
      align: 'right' as const,
      render: (v: number) => <Text type="secondary">{formatSize(v)}</Text>,
    },
    {
      title: '负责人',
      dataIndex: 'owner',
      key: 'owner',
      width: 100,
      render: (v: string) => v || '-',
    },
    {
      title: '更新时间',
      dataIndex: 'updatedAt',
      key: 'updatedAt',
      width: 170,
      render: (v: string) => v ? new Date(v).toLocaleString() : '-',
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      fixed: 'right' as const,
      render: (_: any, record: KnowledgeBaseItem) => (
        <Space size={4}>
          <Tooltip title="管理文档">
            <Button
              type="link"
              size="small"
              icon={<FolderOpenOutlined />}
              onClick={() => navigate(`/knowledge/${record.id}/documents`)}
            >
              文档
            </Button>
          </Tooltip>
          <Tooltip title="编辑">
            <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openEdit(record)}>
              编辑
            </Button>
          </Tooltip>
          <Popconfirm
            title="确认删除该知识库？"
            description="将连同其下所有文档和向量一并删除，不可恢复"
            okType="danger"
            onConfirm={() => handleDelete(record)}
          >
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div className="kb-list-page">
      {/* 顶部 4 个统计卡 */}
      <div className="kb-stats-grid">
        <Card className="kb-stat-card kb-stat-blue">
          <Statistic
            title="知识库总数"
            value={overview.knowledgeBaseCount}
            prefix={<DatabaseOutlined />}
          />
        </Card>
        <Card className="kb-stat-card kb-stat-green">
          <Statistic
            title="文档总数"
            value={overview.documentCount}
            prefix={<FileTextOutlined />}
          />
        </Card>
        <Card className="kb-stat-card kb-stat-orange">
          <Statistic
            title="分块总数"
            value={overview.chunkCount}
            prefix={<AppstoreOutlined />}
          />
        </Card>
        <Card className="kb-stat-card kb-stat-purple">
          <Statistic
            title="存储容量"
            value={formatSize(overview.sizeBytes)}
            prefix={<HddOutlined />}
          />
        </Card>
      </div>

      {/* 知识库列表 */}
      <Card
        className="kb-table-card"
        title={
          <Space>
            <Title level={5} style={{ margin: 0 }}>知识库管理</Title>
            <Text type="secondary" style={{ fontSize: 12 }}>
              管理多个知识库，每个知识库可独立选择 Embedding 模型
            </Text>
          </Space>
        }
        extra={
          <Space>
            <Button icon={<ReloadOutlined />} onClick={loadAll}>刷新</Button>
            <Button icon={<CloudUploadOutlined />} onClick={openUpload}>
              上传文件
            </Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
              新建知识库
            </Button>
          </Space>
        }
      >
        <Table
          rowKey="id"
          columns={columns}
          dataSource={list}
          loading={loading}
          pagination={{ pageSize: 10, showTotal: t => `共 ${t} 个知识库` }}
          scroll={{ x: 1200 }}
        />
      </Card>

      {/* 创建/编辑弹窗 */}
      <Modal
        title={editing ? '编辑知识库' : '新建知识库'}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        okText={editing ? '保存' : '创建'}
        cancelText="取消"
        width={560}
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item
            name="name"
            label="知识库名称"
            rules={[{ required: true, message: '请输入名称' }]}
          >
            <Input placeholder="如：产品文档库" maxLength={50} showCount />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea
              placeholder="用一两句话描述这个知识库的用途"
              rows={2}
              maxLength={200}
              showCount
            />
          </Form.Item>
          {!editing && (
            <Form.Item
              name="collectionName"
              label="Collection 名称"
              extra="可选；不填将自动生成。同一 PostgreSQL 实例内需唯一"
            >
              <Input placeholder="如：product_docs（建议小写英文）" maxLength={50} />
            </Form.Item>
          )}
          <Form.Item
            name="embeddingModel"
            label="Embedding 模型"
            rules={[{ required: true, message: '请选择模型' }]}
          >
            <Select options={EMBEDDING_MODELS} />
          </Form.Item>
          <Form.Item
            name="dimensions"
            label="向量维度"
            rules={[{ required: true, message: '请输入维度' }]}
          >
            <Input type="number" min={1} max={4096} suffix="维" />
          </Form.Item>
          <Form.Item name="owner" label="负责人">
            <Input placeholder="如：张三" maxLength={50} />
          </Form.Item>
        </Form>
      </Modal>

      {/* 快速上传抽屉 */}
      <Drawer
        title="上传文件到知识库"
        width={520}
        open={uploadOpen}
        onClose={() => setUploadOpen(false)}
        destroyOnClose
      >
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <Alert
            type="info"
            showIcon
            message="文件上传后将自动解析、按 800 字符切分（重叠 100），并使用所选知识库配置的 Embedding 模型生成向量"
          />

          <div>
            <Text strong style={{ display: 'block', marginBottom: 6 }}>
              目标知识库 <Text type="danger">*</Text>
            </Text>
            <Select
              placeholder="请选择要上传到哪个知识库"
              style={{ width: '100%' }}
              value={uploadKbId}
              onChange={setUploadKbId}
              options={list.map(kb => ({
                value: kb.id,
                label: (
                  <Space>
                    <DatabaseOutlined style={{ color: '#1677ff' }} />
                    {kb.name}
                    <Tag color="purple">{kb.embeddingModel}</Tag>
                  </Space>
                ),
              }))}
            />
          </div>

          <div>
            <Text strong style={{ display: 'block', marginBottom: 6 }}>文档名称（可选）</Text>
            <Input
              placeholder="留空则使用文件名"
              value={uploadName}
              onChange={e => setUploadName(e.target.value)}
              allowClear
            />
          </div>

          <div>
            <Text strong style={{ display: 'block', marginBottom: 6 }}>文档分类</Text>
            <Radio.Group
              value={uploadCategory}
              onChange={e => setUploadCategory(e.target.value)}
              optionType="button"
              buttonStyle="solid"
            >
              {CATEGORY_OPTIONS.map(opt => (
                <Radio.Button key={opt.value} value={opt.value}>
                  {opt.label}
                </Radio.Button>
              ))}
            </Radio.Group>
          </div>

          <Dragger {...uploadProps} disabled={uploading || !uploadKbId}>
            <p className="ant-upload-drag-icon">
              <InboxOutlined style={{ color: uploading ? '#d9d9d9' : '#1677ff' }} />
            </p>
            <p className="ant-upload-text">
              {uploading ? '正在解析与向量化...' : '点击或拖拽文件到此处上传'}
            </p>
            <p className="ant-upload-hint">
              支持 PDF / Word / Excel / PPT / TXT / Markdown / HTML / JSON / CSV，可多文件
            </p>
          </Dragger>

          {uploadProgress > 0 && (
            <Progress
              percent={uploadProgress}
              status={uploadProgress < 100 ? 'active' : 'success'}
            />
          )}
        </Space>
      </Drawer>
    </div>
  )
}

export default KnowledgeBaseListPage
