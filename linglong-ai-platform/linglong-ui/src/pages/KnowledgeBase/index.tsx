import React, { useState, useEffect } from 'react';
import {
  Upload, Button, Table, Tag, Space, Popconfirm, Input,
  Select, message as antdMessage, Card, Statistic, Progress,
  Tooltip, Badge, Modal,
} from 'antd';
import {
  DeleteOutlined, DatabaseOutlined,
  FileTextOutlined, ReloadOutlined, EyeOutlined, InboxOutlined,
} from '@ant-design/icons';
import type { UploadProps } from 'antd';
import axios from 'axios';
import './index.css';

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

  const uploadProps: UploadProps = {
    name: 'file',
    multiple: false,
    accept: ACCEPT_FORMATS,
    showUploadList: false,
    customRequest: async ({ file, onSuccess, onError }: any) => {
      setUploading(true);
      setUploadProgress(10);
      const formData = new FormData();
      formData.append('file', file);
      formData.append('category', uploadCategory);
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
          antdMessage.success(`「${res.data.filename}」已成功建索，共 ${res.data.chunks} 个文本块`);
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
      title: '文件名',
      key: 'filename',
      render: (_: any, record: KbItem) => {
        const meta = typeof record.metadata === 'string'
          ? JSON.parse(record.metadata) : record.metadata;
        return (
          <Space>
            <FileTextOutlined style={{ color: '#1677ff' }} />
            <span style={{ fontWeight: 500 }}>{meta?.title || meta?.filename || '未命名'}</span>
            {meta?.chunkIndex !== undefined && (
              <Tag color="blue" style={{ fontSize: 11 }}>块 {meta.chunkIndex + 1}/{meta.totalChunks}</Tag>
            )}
          </Space>
        );
      },
    },
    {
      title: '分类',
      key: 'category',
      width: 110,
      render: (_: any, record: KbItem) => {
        const meta = typeof record.metadata === 'string'
          ? JSON.parse(record.metadata) : record.metadata;
        const cat = CATEGORY_OPTIONS.find(c => c.value === meta?.category);
        return <Tag color="geekblue">{cat?.label || meta?.category || '通用文档'}</Tag>;
      },
    },
    {
      title: '内容预览',
      dataIndex: 'contentPreview',
      key: 'contentPreview',
      render: (v: string) => (
        <span style={{ color: '#595959', fontSize: 13 }}>{v}</span>
      ),
    },
    {
      title: '字符数',
      dataIndex: 'contentLength',
      key: 'contentLength',
      width: 90,
      render: (v: number) => <span style={{ color: '#8c8c8c' }}>{v.toLocaleString()}</span>,
    },
    {
      title: '操作',
      key: 'action',
      width: 120,
      render: (_: any, record: KbItem) => (
        <Space>
          <Tooltip title="查看内容">
            <Button
              type="text" size="small" icon={<EyeOutlined />}
              onClick={() => setPreviewItem(record)}
            />
          </Tooltip>
          <Popconfirm
            title="确认删除该文本块？"
            onConfirm={() => handleDelete(record.id)}
            okText="删除" cancelText="取消"
          >
            <Button type="text" size="small" danger icon={<DeleteOutlined />} />
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
            <span className="kb-label">文档分类</span>
            <Select
              value={uploadCategory}
              onChange={setUploadCategory}
              options={CATEGORY_OPTIONS}
              style={{ width: '100%' }}
            />
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
                <>
                  <Select
                    placeholder="筛选分类"
                    allowClear
                    options={CATEGORY_OPTIONS}
                    value={filterCategory || undefined}
                    onChange={(v) => { setFilterCategory(v ?? ''); setPage(0); }}
                    style={{ width: 140 }}
                  />
                  <Button icon={<ReloadOutlined />} onClick={loadList} />
                </>
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
            <Table
              columns={columns}
              dataSource={items}
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
            />
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
        title="文本块内容"
        open={!!previewItem}
        onCancel={() => setPreviewItem(null)}
        footer={null}
        width={700}
      >
        {previewItem && (
          <div>
            <div style={{ marginBottom: 12 }}>
              {(() => {
                const meta = typeof previewItem.metadata === 'string'
                  ? JSON.parse(previewItem.metadata) : previewItem.metadata;
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
            <pre style={{
              background: '#f6f8fa', padding: 16, borderRadius: 6,
              fontSize: 13, lineHeight: 1.6, maxHeight: 500, overflow: 'auto',
              whiteSpace: 'pre-wrap', wordBreak: 'break-word',
            }}>
              {previewItem.contentPreview.replace('...', '')}
            </pre>
          </div>
        )}
      </Modal>
    </div>
  );
};

export default KnowledgeBasePage;
