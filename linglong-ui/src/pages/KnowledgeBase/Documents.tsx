import React, { useEffect, useState } from 'react'
import {
  Alert,
  Badge,
  Breadcrumb,
  Button,
  Card,
  Drawer,
  Input,
  Popconfirm,
  Progress,
  Radio,
  Select,
  Space,
  Switch,
  Table,
  Tag,
  Tooltip,
  Typography,
  Upload,
  message as antdMessage,
} from 'antd'
import type { UploadProps } from 'antd'
import {
  ApartmentOutlined,
  ArrowLeftOutlined,
  CloudUploadOutlined,
  DatabaseOutlined,
  DeleteOutlined,
  FileTextOutlined,
  HomeOutlined,
  InboxOutlined,
  ReloadOutlined,
  SyncOutlined,
} from '@ant-design/icons'
import { useNavigate, useParams } from 'react-router-dom'
import {
  deleteDocument,
  fetchDocuments,
  fetchKbDetail,
  rebuildDocument,
  setDocumentEnabled,
  uploadDocument,
  type KnowledgeBaseItem,
  type KnowledgeDocumentItem,
} from '@/services/knowledgeBase'
import './documents.css'

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

const STATUS_OPTIONS = [
  { value: '', label: '全部状态' },
  { value: 'ready', label: '已就绪' },
  { value: 'parsing', label: '解析中' },
  { value: 'failed', label: '失败' },
]

const formatSize = (bytes?: number) => {
  if (!bytes || bytes <= 0) return '-'
  const units = ['B', 'KB', 'MB', 'GB']
  let v = bytes
  let i = 0
  while (v >= 1024 && i < units.length - 1) {
    v /= 1024
    i++
  }
  return `${v.toFixed(v >= 100 ? 0 : 1)} ${units[i]}`
}

const statusTag = (status: string) => {
  const map: Record<string, { color: string; text: string; icon?: React.ReactNode }> = {
    ready: { color: 'success', text: '已就绪' },
    parsing: { color: 'processing', text: '解析中', icon: <SyncOutlined spin /> },
    failed: { color: 'error', text: '失败' },
  }
  const meta = map[status] || { color: 'default', text: status }
  return <Tag color={meta.color} icon={meta.icon}>{meta.text}</Tag>
}

const DocumentsPage: React.FC = () => {
  const navigate = useNavigate()
  const { kbId } = useParams<{ kbId: string }>()
  const [kb, setKb] = useState<KnowledgeBaseItem | null>(null)
  const [items, setItems] = useState<KnowledgeDocumentItem[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(0)
  const [pageSize] = useState(10)
  const [loading, setLoading] = useState(false)
  const [keyword, setKeyword] = useState('')
  const [status, setStatus] = useState('')

  const [uploadOpen, setUploadOpen] = useState(false)
  const [uploadName, setUploadName] = useState('')
  const [uploadCategory, setUploadCategory] = useState('document')
  const [uploading, setUploading] = useState(false)
  const [uploadProgress, setUploadProgress] = useState(0)

  const loadKb = async () => {
    if (!kbId) return
    try {
      const data = await fetchKbDetail(kbId)
      setKb(data)
    } catch (e: any) {
      antdMessage.error(e?.response?.data?.message || '加载知识库信息失败')
    }
  }

  const loadDocs = async () => {
    if (!kbId) return
    setLoading(true)
    try {
      const data = await fetchDocuments(kbId, { page, size: pageSize, keyword, status })
      setItems(data.items || [])
      setTotal(data.total || 0)
    } catch (e: any) {
      antdMessage.error(e?.response?.data?.message || '加载文档失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { loadKb() }, [kbId])
  useEffect(() => { loadDocs() }, [kbId, page, status])

  const handleSearch = () => {
    setPage(0)
    loadDocs()
  }

  const handleToggleEnabled = async (record: KnowledgeDocumentItem, value: boolean) => {
    try {
      await setDocumentEnabled(kbId!, record.id, value)
      antdMessage.success(value ? '已启用' : '已禁用')
      loadDocs()
    } catch (e: any) {
      antdMessage.error(e?.response?.data?.message || '操作失败')
    }
  }

  const handleRebuild = async (record: KnowledgeDocumentItem) => {
    try {
      await rebuildDocument(kbId!, record.id)
      antdMessage.success('重建成功')
      loadDocs()
    } catch (e: any) {
      antdMessage.error(e?.response?.data?.message || '重建失败')
    }
  }

  const handleDelete = async (record: KnowledgeDocumentItem) => {
    try {
      await deleteDocument(kbId!, record.id)
      antdMessage.success('已删除')
      loadDocs()
    } catch (e: any) {
      antdMessage.error(e?.response?.data?.message || '删除失败')
    }
  }

  const uploadProps: UploadProps = {
    name: 'file',
    multiple: true,
    accept: ACCEPT,
    showUploadList: false,
    customRequest: async ({ file, onSuccess, onError }: any) => {
      if (!kbId) return
      setUploading(true)
      setUploadProgress(5)
      try {
        await uploadDocument(
          kbId,
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
        antdMessage.success(`「${(file as File).name}」已上传并完成向量化`)
        setUploadName('')
        loadDocs()
        loadKb()
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
      title: '文档',
      dataIndex: 'name',
      key: 'name',
      render: (text: string, record: KnowledgeDocumentItem) => (
        <Space>
          <FileTextOutlined style={{ color: '#1677ff', fontSize: 16 }} />
          <div>
            <a
              className="doc-link"
              onClick={() => navigate(`/knowledge/${kbId}/documents/${record.id}/chunks`)}
            >
              {text}
            </a>
            {record.filename && record.filename !== text && (
              <div style={{ fontSize: 12, color: '#8c8c8c' }}>{record.filename}</div>
            )}
          </div>
        </Space>
      ),
    },
    {
      title: '分类',
      dataIndex: 'category',
      key: 'category',
      width: 110,
      render: (v: string) => {
        const opt = CATEGORY_OPTIONS.find(c => c.value === v)
        return <Tag color="geekblue">{opt?.label || v || '-'}</Tag>
      },
    },
    {
      title: '大小',
      dataIndex: 'sizeBytes',
      key: 'sizeBytes',
      width: 90,
      align: 'right' as const,
      render: (v: number) => <Text type="secondary">{formatSize(v)}</Text>,
    },
    {
      title: '分块数',
      dataIndex: 'chunkCount',
      key: 'chunkCount',
      width: 90,
      align: 'right' as const,
      render: (v: number, record: KnowledgeDocumentItem) => (
        <Button
          type="link"
          size="small"
          onClick={() => navigate(`/knowledge/${kbId}/documents/${record.id}/chunks`)}
        >
          {(v ?? 0).toLocaleString()}
        </Button>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (v: string, record: KnowledgeDocumentItem) =>
        record.errorMessage ? (
          <Tooltip title={record.errorMessage}>{statusTag(v)}</Tooltip>
        ) : statusTag(v),
    },
    {
      title: '启用',
      dataIndex: 'enabled',
      key: 'enabled',
      width: 90,
      render: (v: boolean, record: KnowledgeDocumentItem) => (
        <Switch
          checked={v}
          checkedChildren="开"
          unCheckedChildren="关"
          onChange={(val) => handleToggleEnabled(record, val)}
        />
      ),
    },
    {
      title: '上传时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 170,
      render: (v: string) => v ? new Date(v).toLocaleString() : '-',
    },
    {
      title: '操作',
      key: 'action',
      width: 240,
      fixed: 'right' as const,
      render: (_: any, record: KnowledgeDocumentItem) => (
        <Space size={4}>
          <Button
            type="link"
            size="small"
            icon={<ApartmentOutlined />}
            onClick={() => navigate(`/knowledge/${kbId}/documents/${record.id}/chunks`)}
          >
            分块
          </Button>
          <Popconfirm
            title="重新生成所有分块的向量？"
            description="将基于当前文档内容重新分块并 embedding，耗时根据文档大小不同"
            onConfirm={() => handleRebuild(record)}
          >
            <Button type="link" size="small" icon={<SyncOutlined />}>重建</Button>
          </Popconfirm>
          <Popconfirm
            title="确认删除该文档？"
            description="删除后该文档下所有分块向量将一并丢失"
            okType="danger"
            onConfirm={() => handleDelete(record)}
          >
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div className="kb-docs-page">
      {/* 面包屑 + 标题 */}
      <div className="kb-docs-header">
        <Breadcrumb
          items={[
            { title: <a onClick={() => navigate('/knowledge')}><HomeOutlined /> 知识库</a> },
            { title: kb?.name || '加载中...' },
            { title: '文档管理' },
          ]}
        />
        <div className="kb-docs-title-row">
          <Space size="middle" align="center">
            <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/knowledge')}>
              返回
            </Button>
            <DatabaseOutlined style={{ fontSize: 22, color: '#1677ff' }} />
            <div>
              <Title level={4} style={{ margin: 0 }}>{kb?.name}</Title>
              {kb?.description && (
                <Text type="secondary" style={{ fontSize: 12 }}>{kb.description}</Text>
              )}
            </div>
          </Space>
          <Space>
            <Badge count={kb?.documentCount ?? 0} showZero color="#1677ff" overflowCount={9999}>
              <Tag color="blue" style={{ marginRight: 0 }}>文档</Tag>
            </Badge>
            <Badge count={kb?.chunkCount ?? 0} showZero color="#52c41a" overflowCount={99999}>
              <Tag color="green" style={{ marginRight: 0 }}>分块</Tag>
            </Badge>
            <Tag color="purple">{kb?.embeddingModel} · {kb?.dimensions} 维</Tag>
          </Space>
        </div>
      </div>

      {/* 工具栏 + 表格 */}
      <Card className="kb-docs-card">
        <div className="kb-docs-toolbar">
          <Space>
            <Input.Search
              allowClear
              placeholder="搜索文档名称"
              style={{ width: 260 }}
              value={keyword}
              onChange={e => setKeyword(e.target.value)}
              onSearch={handleSearch}
            />
            <Select
              value={status}
              options={STATUS_OPTIONS}
              onChange={v => { setStatus(v); setPage(0) }}
              style={{ width: 120 }}
            />
            <Button icon={<ReloadOutlined />} onClick={loadDocs}>刷新</Button>
          </Space>
          <Button type="primary" icon={<CloudUploadOutlined />} onClick={() => setUploadOpen(true)}>
            上传文档
          </Button>
        </div>

        <Table
          rowKey="id"
          columns={columns}
          dataSource={items}
          loading={loading}
          scroll={{ x: 1100 }}
          pagination={{
            current: page + 1,
            pageSize,
            total,
            onChange: p => setPage(p - 1),
            showTotal: t => `共 ${t} 个文档`,
            showSizeChanger: false,
          }}
        />
      </Card>

      {/* 上传抽屉 */}
      <Drawer
        title="上传文档到知识库"
        width={520}
        open={uploadOpen}
        onClose={() => setUploadOpen(false)}
        destroyOnClose
      >
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <Alert
            type="info"
            showIcon
            message="文档上传后将自动解析、按字符分块（800/100 重叠），并使用知识库配置的 Embedding 模型生成向量"
          />

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

          <Dragger {...uploadProps} disabled={uploading}>
            <p className="ant-upload-drag-icon">
              <InboxOutlined style={{ color: uploading ? '#d9d9d9' : '#1677ff' }} />
            </p>
            <p className="ant-upload-text">
              {uploading ? '正在解析与向量化...' : '点击或拖拽文件到此处上传'}
            </p>
            <p className="ant-upload-hint">
              支持 PDF / Word / Excel / PPT / TXT / Markdown / HTML / JSON / CSV 等格式，可多文件
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

export default DocumentsPage
