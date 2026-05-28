import React, { useEffect, useState } from 'react'
import {
  Alert,
  Breadcrumb,
  Button,
  Card,
  Drawer,
  Input,
  Modal,
  Popconfirm,
  Space,
  Switch,
  Table,
  Tag,
  Tooltip,
  Typography,
  message as antdMessage,
} from 'antd'
import {
  ApartmentOutlined,
  ArrowLeftOutlined,
  DeleteOutlined,
  EditOutlined,
  EyeOutlined,
  HomeOutlined,
  ReloadOutlined,
  SyncOutlined,
} from '@ant-design/icons'
import { useNavigate, useParams } from 'react-router-dom'
import {
  batchDeleteChunks,
  batchRebuildChunks,
  batchSetChunkEnabled,
  fetchChunks,
  fetchDocumentDetail,
  fetchKbDetail,
  updateChunkContent,
  type KnowledgeBaseItem,
  type KnowledgeChunkItem,
  type KnowledgeDocumentItem,
} from '@/services/knowledgeBase'
import './chunks.css'

const { Title, Text, Paragraph } = Typography
const { TextArea } = Input

const ChunksPage: React.FC = () => {
  const navigate = useNavigate()
  const { kbId, docId } = useParams<{ kbId: string; docId: string }>()
  const [kb, setKb] = useState<KnowledgeBaseItem | null>(null)
  const [doc, setDoc] = useState<KnowledgeDocumentItem | null>(null)
  const [items, setItems] = useState<KnowledgeChunkItem[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(0)
  const [pageSize] = useState(10)
  const [keyword, setKeyword] = useState('')
  const [loading, setLoading] = useState(false)
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([])

  const [editingChunk, setEditingChunk] = useState<KnowledgeChunkItem | null>(null)
  const [editingContent, setEditingContent] = useState('')
  const [savingEdit, setSavingEdit] = useState(false)
  const [previewing, setPreviewing] = useState<KnowledgeChunkItem | null>(null)

  const loadMeta = async () => {
    if (!kbId || !docId) return
    try {
      const [k, d] = await Promise.all([fetchKbDetail(kbId), fetchDocumentDetail(kbId, docId)])
      setKb(k)
      setDoc(d)
    } catch (e: any) {
      antdMessage.error(e?.response?.data?.message || '加载信息失败')
    }
  }

  const loadChunks = async () => {
    if (!kbId || !docId) return
    setLoading(true)
    try {
      const data = await fetchChunks(kbId, docId, { page, size: pageSize, keyword })
      setItems(data.items || [])
      setTotal(data.total || 0)
    } catch (e: any) {
      antdMessage.error(e?.response?.data?.message || '加载分块失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { loadMeta() }, [kbId, docId])
  useEffect(() => { loadChunks() }, [kbId, docId, page])

  const handleSearch = () => { setPage(0); loadChunks() }

  const handleToggleEnabled = async (chunk: KnowledgeChunkItem, enabled: boolean) => {
    try {
      await batchSetChunkEnabled([chunk.id], enabled)
      antdMessage.success(enabled ? '已启用' : '已禁用')
      loadChunks()
    } catch (e: any) {
      antdMessage.error(e?.response?.data?.message || '操作失败')
    }
  }

  const handleDeleteOne = async (chunk: KnowledgeChunkItem) => {
    try {
      await batchDeleteChunks([chunk.id])
      antdMessage.success('已删除')
      loadChunks()
    } catch (e: any) {
      antdMessage.error(e?.response?.data?.message || '删除失败')
    }
  }

  const handleBatch = async (action: 'enable' | 'disable' | 'delete' | 'rebuild') => {
    if (selectedRowKeys.length === 0) {
      antdMessage.warning('请先选择分块')
      return
    }
    const ids = selectedRowKeys as string[]
    try {
      if (action === 'enable') {
        await batchSetChunkEnabled(ids, true)
        antdMessage.success(`已启用 ${ids.length} 个分块`)
      } else if (action === 'disable') {
        await batchSetChunkEnabled(ids, false)
        antdMessage.success(`已禁用 ${ids.length} 个分块`)
      } else if (action === 'delete') {
        await batchDeleteChunks(ids)
        antdMessage.success(`已删除 ${ids.length} 个分块`)
      } else if (action === 'rebuild') {
        await batchRebuildChunks(ids)
        antdMessage.success(`已重建 ${ids.length} 个分块的向量`)
      }
      setSelectedRowKeys([])
      loadChunks()
    } catch (e: any) {
      antdMessage.error(e?.response?.data?.message || '批量操作失败')
    }
  }

  const handleSaveEdit = async () => {
    if (!editingChunk) return
    if (!editingContent.trim()) {
      antdMessage.warning('内容不能为空')
      return
    }
    setSavingEdit(true)
    try {
      await updateChunkContent(editingChunk.id, editingContent)
      antdMessage.success('已保存，向量已重新生成')
      setEditingChunk(null)
      loadChunks()
    } catch (e: any) {
      antdMessage.error(e?.response?.data?.message || '保存失败')
    } finally {
      setSavingEdit(false)
    }
  }

  const columns = [
    {
      title: '#',
      dataIndex: 'chunkIndex',
      key: 'chunkIndex',
      width: 80,
      render: (v: number, record: KnowledgeChunkItem) => (
        <Tag color="blue">{(v ?? 0) + 1}/{record.totalChunks}</Tag>
      ),
    },
    {
      title: '内容预览',
      dataIndex: 'content',
      key: 'content',
      render: (v: string, record: KnowledgeChunkItem) => (
        <div className={record.enabled ? '' : 'chunk-disabled'}>
          <Paragraph
            style={{ margin: 0, fontSize: 13, lineHeight: 1.55 }}
            ellipsis={{ rows: 2, tooltip: false }}
          >
            {v}
          </Paragraph>
          <Text type="secondary" style={{ fontSize: 11 }}>
            {record.contentLength} 字符
          </Text>
        </div>
      ),
    },
    {
      title: '启用',
      dataIndex: 'enabled',
      key: 'enabled',
      width: 80,
      render: (v: boolean, record: KnowledgeChunkItem) => (
        <Switch
          size="small"
          checked={v}
          checkedChildren="开"
          unCheckedChildren="关"
          onChange={val => handleToggleEnabled(record, val)}
        />
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 220,
      fixed: 'right' as const,
      render: (_: any, record: KnowledgeChunkItem) => (
        <Space size={4}>
          <Tooltip title="查看完整内容">
            <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => setPreviewing(record)}>
              查看
            </Button>
          </Tooltip>
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => {
              setEditingChunk(record)
              setEditingContent(record.content)
            }}
          >
            编辑
          </Button>
          <Popconfirm
            title="确认删除该分块？"
            okType="danger"
            onConfirm={() => handleDeleteOne(record)}
          >
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div className="kb-chunks-page">
      <div className="kb-chunks-header">
        <Breadcrumb
          items={[
            { title: <a onClick={() => navigate('/knowledge')}><HomeOutlined /> 知识库</a> },
            { title: <a onClick={() => navigate(`/knowledge/${kbId}/documents`)}>{kb?.name || kbId}</a> },
            { title: doc?.name || '加载中...' },
            { title: '分块管理' },
          ]}
        />
        <div className="kb-chunks-title-row">
          <Space size="middle">
            <Button
              icon={<ArrowLeftOutlined />}
              onClick={() => navigate(`/knowledge/${kbId}/documents`)}
            >
              返回
            </Button>
            <ApartmentOutlined style={{ fontSize: 22, color: '#1677ff' }} />
            <div>
              <Title level={4} style={{ margin: 0 }}>{doc?.name}</Title>
              <Space size={4}>
                <Tag color="blue">{kb?.name}</Tag>
                {doc?.filename && doc.filename !== doc.name && (
                  <Text type="secondary" style={{ fontSize: 12 }}>{doc.filename}</Text>
                )}
              </Space>
            </div>
          </Space>
          <Space>
            <Tag color="blue">{doc?.chunkCount ?? 0} 个分块</Tag>
            <Tag color={doc?.enabled ? 'success' : 'default'}>
              {doc?.enabled ? '已启用' : '已禁用'}
            </Tag>
          </Space>
        </div>
      </div>

      <Card className="kb-chunks-card">
        <div className="kb-chunks-toolbar">
          <Space>
            <Input.Search
              allowClear
              placeholder="搜索分块内容"
              style={{ width: 280 }}
              value={keyword}
              onChange={e => setKeyword(e.target.value)}
              onSearch={handleSearch}
            />
            <Button icon={<ReloadOutlined />} onClick={loadChunks}>刷新</Button>
          </Space>
        </div>

        {selectedRowKeys.length > 0 && (
          <Alert
            type="info"
            showIcon
            style={{ marginBottom: 12 }}
            message={`已选择 ${selectedRowKeys.length} 个分块`}
            action={
              <Space>
                <Button size="small" onClick={() => handleBatch('enable')}>启用</Button>
                <Button size="small" onClick={() => handleBatch('disable')}>禁用</Button>
                <Popconfirm
                  title="重新生成所选分块的向量？"
                  onConfirm={() => handleBatch('rebuild')}
                >
                  <Button size="small" icon={<SyncOutlined />}>重建向量</Button>
                </Popconfirm>
                <Popconfirm
                  title={`确认删除 ${selectedRowKeys.length} 个分块？`}
                  okType="danger"
                  onConfirm={() => handleBatch('delete')}
                >
                  <Button size="small" danger icon={<DeleteOutlined />}>批量删除</Button>
                </Popconfirm>
                <Button size="small" type="text" onClick={() => setSelectedRowKeys([])}>取消</Button>
              </Space>
            }
          />
        )}

        <Table
          rowKey="id"
          columns={columns}
          dataSource={items}
          loading={loading}
          rowSelection={{
            selectedRowKeys,
            onChange: setSelectedRowKeys,
          }}
          pagination={{
            current: page + 1,
            pageSize,
            total,
            onChange: p => setPage(p - 1),
            showTotal: t => `共 ${t} 个分块`,
            showSizeChanger: false,
          }}
        />
      </Card>

      {/* 编辑 */}
      <Modal
        title="编辑分块内容"
        open={!!editingChunk}
        onCancel={() => setEditingChunk(null)}
        onOk={handleSaveEdit}
        okText="保存并重新生成向量"
        cancelText="取消"
        width={720}
        confirmLoading={savingEdit}
      >
        {editingChunk && (
          <div>
            <Alert
              type="warning"
              showIcon
              message="保存后将自动重新计算该分块的向量"
              style={{ marginBottom: 12 }}
            />
            <TextArea
              value={editingContent}
              onChange={e => setEditingContent(e.target.value)}
              rows={14}
              showCount
              maxLength={4000}
            />
          </div>
        )}
      </Modal>

      {/* 预览 */}
      <Drawer
        title="分块完整内容"
        width={680}
        open={!!previewing}
        onClose={() => setPreviewing(null)}
      >
        {previewing && (
          <Space direction="vertical" size="middle" style={{ width: '100%' }}>
            <Space wrap>
              <Tag color="blue">块 {(previewing.chunkIndex ?? 0) + 1}/{previewing.totalChunks}</Tag>
              <Tag color={previewing.enabled ? 'success' : 'default'}>
                {previewing.enabled ? '已启用' : '已禁用'}
              </Tag>
              <Tag>{previewing.contentLength} 字符</Tag>
            </Space>
            <pre className="chunk-pre">{previewing.content}</pre>
          </Space>
        )}
      </Drawer>
    </div>
  )
}

export default ChunksPage
