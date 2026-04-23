import React, { useState, useEffect } from 'react';
import {
  Upload, Button, Table, Tag, Space, Popconfirm, Input,
  Select, message as antdMessage, Card, Statistic, Progress,
  Tooltip, Badge, Modal, Alert, Typography, Radio
} from 'antd';
import {
  DeleteOutlined, DatabaseOutlined,
  FileTextOutlined, ReloadOutlined, EyeOutlined, InboxOutlined,
  QuestionCircleOutlined, MessageOutlined
} from '@ant-design/icons';
import type { UploadProps } from 'antd';
import axios from 'axios';
import './index.css';

const { Text} = Typography;

const { Dragger } = Upload;
const { Search } = Input;

// 支持的文件类型
const ACCEPT_FORMATS = [
  '.pdf', '.docx', '.doc', '.rtf',
  '.xlsx', '.xls', '.xlsm',
  '.pptx', '.ppt',
  '.txt', '.md', '.markdown',
  '.html', '.htm',
  '.json', '.yaml', '.yml', '.xml', '.csv',
].join(',');

const CATEGORY_OPTIONS = [
  { value: 'document', label: '通用文档' },
  { value: 'technical', label: '技术文档' },
  { value: 'business', label: '业务资料' },
  { value: 'faq', label: '常见问题' },
  { value: 'other', label: '其他' },
];

interface KbItem {
  id: string;
  contentPreview: string;
  contentLength: number;
  metadata: any;
}

const KnowledgeBasePage: React.FC = () => {
  const [items, setItems] = useState<KbItem[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [pageSize] = useState(20);
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [filterCategory, setFilterCategory] = useState('');
  const [previewItem, setPreviewItem] = useState<KbItem | null>(null);
  const [uploadCategory, setUploadCategory] = useState('document');
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<any[]>([]);
  const [searchLoading, setSearchLoading] = useState(false);
  const [activeTab, setActiveTab] = useState<'list' | 'search'>('list');
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const [isBatchDeleting, setIsBatchDeleting] = useState(false);
  const [typeFilter, setTypeFilter] = useState<'all' | 'knowledge' | 'conversation'>('all');
  const [uploadName, setUploadName] = useState('');
  const [previewContent, setPreviewContent] = useState<string | null>(null);
  const [previewLoading, setPreviewLoading] = useState(false);

  useEffect(() => {
    loadList();
  }, [page, filterCategory]);

  const loadList = async () => {
    setLoading(true);
    try {
      const res = await axios.get('/vector/list', {
        params: { page, size: pageSize, category: filterCategory },
      });
      const data = res.data;
      setItems(data.items || []);
      setTotal(data.total || 0);
    } catch {
      antdMessage.error('加载知识库列表失败');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await axios.delete('/vector/docs', { data: [id] });
      antdMessage.success('已删除');
      loadList();
    } catch {
      antdMessage.error('删除失败');
    }
  };

  const handleBatchDelete = async () => {
    if (selectedRowKeys.length === 0) {
      antdMessage.warning('请先选择要删除的文本块');
      return;
    }
    setIsBatchDeleting(true);
    try {
      await axios.delete('/vector/docs', { data: selectedRowKeys });
      antdMessage.success(`已删除 ${selectedRowKeys.length} 个文本块`);
      setSelectedRowKeys([]);
      loadList();
    } catch {
      antdMessage.error('批量删除失败');
    } finally {
      setIsBatchDeleting(false);
    }
  };

  const handleSearch = async () => {
    if (!searchQuery.trim()) return;
    setSearchLoading(true);
    try {
      const res = await axios.get('/vector/search', {
        params: { query: searchQuery, topK: 10 },
      });
      setSearchResults(res.data || []);
      setActiveTab('search');
    } catch {
      antdMessage.error('搜索失败');
    } finally {
      setSearchLoading(false);
    }
  };

  const handleOpenPreview = async (record: KbItem) => {
    setPreviewItem(record);
    setPreviewContent(null);
    const meta = typeof record.metadata === 'string' ? JSON.parse(record.metadata) : record.metadata;
    // 对话缓存类型不需要额外请求
    if (meta?.type === 'conversation') return;
    setPreviewLoading(true);
    try {
      const res = await axios.get(`/vector/doc/${record.id}`);
      setPreviewContent(res.data?.content || record.contentPreview);
    } catch {
      setPreviewContent(record.contentPreview);
    } finally {
      setPreviewLoading(false);
    }
  };

  const uploadProps: UploadProps = {
    name: 'file',
    multiple: true,
    accept: ACCEPT_FORMATS,
    showUploadList: false,
    customRequest: async ({ file, onSuccess, onError }: any) => {
      setUploading(true);
      setUploadProgress(10);
      const formData = new FormData();
      formData.append('file', file);
      formData.append('category', uploadCategory);
      if (uploadName.trim()) formData.append('title', uploadName.trim());
      try {
        setUploadProgress(40);
        const res = await axios.post('/vector/upload', formData, {
          headers: { 'Content-Type': 'multipart/form-data' },
          onUploadProgress: (e) => {
            if (e.total) setUploadProgress(Math.round((e.loaded / e.total) * 60));
          },
        });
        setUploadProgress(100);
        if (res.data.success) {
          antdMessage.success(`「${uploadName.trim() || res.data.filename}」已成功建索，共 ${res.data.chunks} 个文本块`);
          setUploadName('');
          loadList();
          onSuccess(res.data);
        } else {
          antdMessage.error(res.data.message || '上传失败');
          onError(new Error(res.data.message));
        }
      } catch (e: any) {
        antdMessage.error('上传失败：' + (e.response?.data?.message || e.message));
        onError(e);
      } finally {
        setUploading(false);
        setTimeout(() => setUploadProgress(0), 1500);
      }
    },
  };

  const columns = [
    {
      title: '类型',
      key: 'type',
      width: 100,
      render: (_: any, record: KbItem) => {
        const meta = typeof record.metadata === 'string'
          ? JSON.parse(record.metadata) : record.metadata;
        if (meta?.type === 'conversation') {
          return (
            <Tag color="green" icon={<MessageOutlined />}>
              问答对
            </Tag>
          );
        }
        return (
          <Tag color="blue" icon={<FileTextOutlined />}>
            文档
          </Tag>
        );
      },
    },
    {
      title: '内容',
      key: 'content',
      render: (_: any, record: KbItem) => {
        const meta = typeof record.metadata === 'string'
          ? JSON.parse(record.metadata) : record.metadata;
        // 对话缓存类型，显示问题和答案
        if (meta?.type === 'conversation') {
          const question = meta?.question || '';
          const answer = meta?.answer || '';
          return (
            <div style={{ fontSize: 13 }}>
              <div style={{ 
                color: '#1677ff', 
                marginBottom: 6,
                padding: '4px 8px',
                background: '#f0f5ff',
                borderRadius: 4,
                borderLeft: '3px solid #1677ff'
              }}>
                <QuestionCircleOutlined style={{ marginRight: 4 }} />
                <Text strong style={{ color: '#1677ff' }}>问题：</Text>
                <Text style={{ color: '#1677ff' }}>
                  {question.length > 50 ? question.slice(0, 50) + '...' : question}
                </Text>
              </div>
              <div style={{ 
                color: '#52c41a',
                padding: '4px 8px',
                background: '#f6ffed',
                borderRadius: 4,
                borderLeft: '3px solid #52c41a'
              }}>
                <MessageOutlined style={{ marginRight: 4 }} />
                <Text strong style={{ color: '#52c41a' }}>答案：</Text>
                <Text style={{ color: '#595959' }}>
                  {answer.length > 60 ? answer.slice(0, 60) + '...' : answer}
                </Text>
              </div>
            </div>
          );
        }
        // 普通文档类型
        const cat = CATEGORY_OPTIONS.find(c => c.value === meta?.category);
        return (
          <div style={{ fontSize: 13 }}>
            <div style={{ marginBottom: 4 }}>
              <Text strong style={{ color: '#262626' }}>
                {meta?.title || meta?.filename || '未命名'}
              </Text>
              {meta?.chunkIndex !== undefined && (
                <Tag color="blue" style={{ fontSize: 11, marginLeft: 8 }}>
                  块 {meta.chunkIndex + 1}/{meta.totalChunks}
                </Tag>
              )}
            </div>
            <div style={{ color: '#8c8c8c', fontSize: 12 }}>
              <Tag color="geekblue" style={{ fontSize: 11 }}>
                {cat?.label || meta?.category || '通用文档'}
              </Tag>
              <span style={{ marginLeft: 8 }}>{record.contentLength.toLocaleString()} 字符</span>
            </div>
            <div style={{ color: '#595959', marginTop: 4 }}>
              {record.contentPreview}
            </div>
          </div>
        );
      },
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      render: (_: any, record: KbItem) => (
        <Space direction="vertical" size="small">
          <Tooltip title="查看详情">
            <Button
              type="primary" 
              size="small" 
              icon={<EyeOutlined />}
              onClick={() => handleOpenPreview(record)}
            >
              查看
            </Button>
          </Tooltip>
          <Popconfirm
            title="确认删除？"
            description="删除后不可恢复"
            onConfirm={() => handleDelete(record.id)}
            okText="删除" 
            cancelText="取消"
          >
            <Button type="default" size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const searchColumns = [
    {
      title: '相关文本',
      dataIndex: 'content',
      key: 'content',
      render: (v: string) => <span style={{ fontSize: 13 }}>{v?.slice(0, 200)}{v?.length > 200 ? '...' : ''}</span>,
    },
    {
      title: '元数据',
      dataIndex: 'metadata',
      key: 'metadata',
      width: 220,
      render: (meta: any) => {
        const m = typeof meta === 'string' ? JSON.parse(meta) : meta;
        return (
          <Space direction="vertical" size={2}>
            {m?.filename && <Tag>{m.filename}</Tag>}
            {m?.category && <Tag color="blue">{m.category}</Tag>}
          </Space>
        );
      },
    },
  ];

  return (
    <div className="kb-page">
      {/* 顶部统计 */}
      <div className="kb-header">
        <div className="kb-header-left">
          <DatabaseOutlined className="kb-header-icon" />
          <div>
            <div className="kb-header-title">知识库管理</div>
            <div className="kb-header-sub">上传文档并向量化存储，供 RAG 检索使用</div>
          </div>
        </div>
        <div className="kb-stats">
          <Card size="small" className="kb-stat-card">
            <Statistic title="文本块总数" value={total} prefix={<DatabaseOutlined />} />
          </Card>
        </div>
      </div>

      <div className="kb-body">
        {/* 左侧：上传区 */}
        <div className="kb-upload-panel">
          <div className="kb-panel-title">上传到知识库</div>

          <div className="kb-category-row">
            <span className="kb-label">文档命名</span>
            <Input
              placeholder="自定义文档名（选填，默认使用文件名）"
              value={uploadName}
              onChange={e => setUploadName(e.target.value)}
              allowClear
              size="small"
              style={{ width: '100%' }}
            />
          </div>

          <div className="kb-category-row">
            <span className="kb-label">文档分类</span>
            <Radio.Group 
              value={uploadCategory}
              onChange={(e) => setUploadCategory(e.target.value)}
              style={{ width: '100%' }}
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

          <Dragger {...uploadProps} disabled={uploading} className="kb-dragger">
            <p className="ant-upload-drag-icon">
              <InboxOutlined style={{ color: uploading ? '#d9d9d9' : '#1677ff' }} />
            </p>
            <p className="ant-upload-text">
              {uploading ? '正在向量化处理...' : '点击或拖拽文件上传'}
            </p>
            <p className="ant-upload-hint">
              支持 PDF、Word、Excel、PPT、TXT、Markdown、HTML、JSON、CSV 等格式
            </p>
          </Dragger>

          {uploadProgress > 0 && (
            <Progress
              percent={uploadProgress}
              status={uploadProgress < 100 ? 'active' : 'success'}
              style={{ marginTop: 12 }}
            />
          )}

          <div className="kb-upload-tips">
            <div className="kb-tip-item">• 上传后自动分块并向量化存储</div>
            <div className="kb-tip-item">• 对话时开启 RAG 开关即可检索</div>
            <div className="kb-tip-item">• 支持多次上传，内容累积建索</div>
          </div>
        </div>

        {/* 右侧：列表 + 搜索 */}
        <div className="kb-content-panel">
          {/* 工具栏 */}
          <div className="kb-toolbar">
            <div className="kb-toolbar-left">
              <Button
                type={activeTab === 'list' ? 'primary' : 'default'}
                onClick={() => setActiveTab('list')}
              >
                文档列表
                <Badge count={total} style={{ marginLeft: 6 }} overflowCount={9999} showZero />
              </Button>
              <Button
                type={activeTab === 'search' ? 'primary' : 'default'}
                onClick={() => setActiveTab('search')}
              >
                语义搜索
              </Button>
            </div>
            <div className="kb-toolbar-right">
              {activeTab === 'list' ? (
                <Space>
                  <Radio.Group 
                    value={typeFilter}
                    onChange={(e) => { setTypeFilter(e.target.value); setPage(0); }}
                    optionType="button"
                    buttonStyle="solid"
                    size="small"
                  >
                    <Radio.Button value="all">全部</Radio.Button>
                    <Radio.Button value="knowledge">文档</Radio.Button>
                    <Radio.Button value="conversation">问答对</Radio.Button>
                  </Radio.Group>
                  <Select
                    placeholder="筛选分类"
                    allowClear
                    options={CATEGORY_OPTIONS}
                    value={filterCategory || undefined}
                    onChange={(v) => { setFilterCategory(v ?? ''); setPage(0); }}
                    style={{ width: 120 }}
                    size="small"
                  />
                  <Button icon={<ReloadOutlined />} onClick={loadList} size="small" />
                </Space>
              ) : (
                <Search
                  placeholder="输入关键词语义搜索知识库..."
                  value={searchQuery}
                  onChange={e => setSearchQuery(e.target.value)}
                  onSearch={handleSearch}
                  loading={searchLoading}
                  enterButton
                  style={{ width: 320 }}
                />
              )}
            </div>
          </div>

          {activeTab === 'list' ? (
            <>
              {selectedRowKeys.length > 0 && (
                <Alert
                  message={`已选择 ${selectedRowKeys.length} 个文本块`}
                  type="info"
                  showIcon
                  action={
                    <Popconfirm
                      title={`确认删除选中的 ${selectedRowKeys.length} 个文本块？`}
                      description="此操作不可恢复"
                      onConfirm={handleBatchDelete}
                      okText="确认删除"
                      cancelText="取消"
                      okButtonProps={{ danger: true, loading: isBatchDeleting }}
                    >
                      <Button
                        type="primary"
                        danger
                        icon={<DeleteOutlined />}
                        loading={isBatchDeleting}
                      >
                        一键删除
                      </Button>
                    </Popconfirm>
                  }
                  style={{ marginBottom: 16 }}
                />
              )}
              <Table
                columns={columns}
                dataSource={items.filter(item => {
                  if (typeFilter === 'all') return true;
                  const meta = typeof item.metadata === 'string' 
                    ? JSON.parse(item.metadata) : item.metadata;
                  if (typeFilter === 'conversation') return meta?.type === 'conversation';
                  return meta?.type !== 'conversation';
                })}
                rowKey="id"
                loading={loading}
                pagination={{
                  current: page + 1,
                  pageSize,
                  total,
                  onChange: (p) => setPage(p - 1),
                  showTotal: (t) => `共 ${t} 条`,
                  showSizeChanger: false,
                }}
                size="small"
                className="kb-table"
                rowSelection={{
                  selectedRowKeys,
                  onChange: (newSelectedRowKeys) => setSelectedRowKeys(newSelectedRowKeys),
                }}
              />
            </>
          ) : (
            <Table
              columns={searchColumns}
              dataSource={searchResults}
              rowKey="id"
              loading={searchLoading}
              pagination={false}
              size="small"
              locale={{ emptyText: '请输入关键词进行搜索' }}
              className="kb-table"
            />
          )}
        </div>
      </div>

      {/* 内容预览弹窗 */}
      <Modal
        title={previewItem ? (() => {
          const meta = typeof previewItem.metadata === 'string'
            ? JSON.parse(previewItem.metadata) : previewItem.metadata;
          return meta?.type === 'conversation' ? '问答详情' : '文本块内容';
        })() : ''}
        open={!!previewItem}
        onCancel={() => { setPreviewItem(null); setPreviewContent(null); }}
        footer={null}
        width={700}
      >
        {previewItem && (
          <div>
            <div style={{ marginBottom: 12 }}>
              {(() => {
                const meta = typeof previewItem.metadata === 'string'
                  ? JSON.parse(previewItem.metadata) : previewItem.metadata;
                // 对话缓存类型
                if (meta?.type === 'conversation') {
                  return (
                    <Space wrap>
                      <Tag color="green">对话缓存</Tag>
                      <Tag color="blue">{meta?.model || '未知模型'}</Tag>
                      <Tag>{previewItem.contentLength} 字符</Tag>
                    </Space>
                  );
                }
                // 普通文档类型
                return (
                  <Space wrap>
                    <Tag color="blue">{meta?.filename}</Tag>
                    <Tag color="geekblue">{meta?.category}</Tag>
                    {meta?.chunkIndex !== undefined && (
                      <Tag>块 {meta.chunkIndex + 1}/{meta.totalChunks}</Tag>
                    )}
                    <Tag>{previewItem.contentLength} 字符</Tag>
                  </Space>
                );
              })()}
            </div>
            {(() => {
              const meta = typeof previewItem.metadata === 'string'
                ? JSON.parse(previewItem.metadata) : previewItem.metadata;
              // 对话缓存类型，显示问题和答案
              if (meta?.type === 'conversation') {
                return (
                  <div style={{
                    background: '#f6f8fa', padding: 16, borderRadius: 6,
                    fontSize: 13, lineHeight: 1.6, maxHeight: 500, overflow: 'auto',
                  }}>
                    <div style={{ marginBottom: 16 }}>
                      <div style={{ color: '#1677ff', fontWeight: 'bold', marginBottom: 8 }}>问题：</div>
                      <div style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>{meta?.question}</div>
                    </div>
                    <div style={{ borderTop: '1px solid #e8e8e8', paddingTop: 16 }}>
                      <div style={{ color: '#52c41a', fontWeight: 'bold', marginBottom: 8 }}>答案：</div>
                      <div style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>{meta?.answer}</div>
                    </div>
                  </div>
                );
              }
              // 普通文档类型
              return (
                <pre style={{
                  background: '#f6f8fa', padding: 16, borderRadius: 6,
                  fontSize: 13, lineHeight: 1.6, maxHeight: 500, overflow: 'auto',
                  whiteSpace: 'pre-wrap', wordBreak: 'break-word',
                }}>
                  {previewLoading ? '加载中...' : (previewContent ?? previewItem.contentPreview)}
                </pre>
              );
            })()}
          </div>
        )}
      </Modal>
    </div>
  );
};

export default KnowledgeBasePage;
