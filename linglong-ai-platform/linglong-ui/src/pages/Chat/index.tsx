import React, { useState, useRef, useEffect, useCallback } from 'react';
import {
  Input, Button, Avatar, Typography, Tag, Select, Tooltip,
  message as antdMessage, Popconfirm, Switch,
} from 'antd';
import {
  SendOutlined, RobotOutlined, UserOutlined, ClearOutlined,
  PlusOutlined, MessageOutlined, ThunderboltOutlined,
  CopyOutlined, LikeOutlined, DislikeOutlined,
  ApiOutlined, DatabaseOutlined, CheckOutlined, DeleteOutlined,
  PaperClipOutlined, FileTextOutlined, CloseCircleOutlined, DownloadOutlined,
  SearchOutlined,
} from '@ant-design/icons';
import axios from 'axios';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import rehypeRaw from 'rehype-raw';
import './index.css';

const { TextArea } = Input;
const { Text } = Typography;

// ── 可用模型列表（支持多提供商） ───────────────────────────────────────────

const MODEL_LIST = [
  // 智谱AI - GLM 系列
  { value: 'glm-4.6',     label: 'GLM-4.6',       desc: '最新旗舰，综合能力强',     tag: '推荐',  tagColor: '#1677ff',  provider: '智谱AI' },
  { value: 'glm-4.7',     label: 'GLM-4.7',       desc: '增强旗舰，逻辑推理优',     tag: '旗舰',  tagColor: '#722ed1',  provider: '智谱AI' },
  { value: 'glm-5',       label: 'GLM-5',         desc: '高能旗舰，全模态理解',     tag: '强大',  tagColor: '#eb2f96',  provider: '智谱AI' },
  { value: 'glm-5-turbo', label: 'GLM-5-Turbo',   desc: '极速推理，低延迟首选',     tag: '快速',  tagColor: '#52c41a',  provider: '智谱AI' },
  { value: 'glm-5.1',     label: 'GLM-5.1',       desc: '下一代旗舰，最强能力',     tag: 'NEW',   tagColor: '#fa8c16',  provider: '智谱AI' },
  { value: 'glm-4.5',     label: 'GLM-4.5',       desc: '通用均衡，稳定可靠',       tag: '均衡',  tagColor: '#13c2c2',  provider: '智谱AI' },
  { value: 'glm-4.5-air', label: 'GLM-4.5-Air',   desc: '轻量高效，快速响应',       tag: '轻量',  tagColor: '#8c8c8c',  provider: '智谱AI' },
  // CodeFlow/灵龙AI 系列
  { value: 'gpt-5.4-mini',label: '灵龙AI Mini',   desc: '灵龙AI · 轻量快速',       tag: '快速',  tagColor: '#52c41a',  provider: '灵龙AI' },
  { value: 'gpt-5',       label: '灵龙AI GPT-5',  desc: '灵龙AI · 旗舰版',         tag: '旗舰',  tagColor: '#722ed1',  provider: '灵龙AI' },
  { value: 'linglong',    label: '灵龙AI 默认',    desc: 'CodeFlow · 智能编程助手', tag: 'CODE',  tagColor: '#1677ff',  provider: '灵龙AI' },
];

// 快捷提示词
const QUICK_PROMPTS = [
  { icon: '✍️', text: '帮我写一篇文章' },
  { icon: '💻', text: '用 Java 写一个单例模式' },
  { icon: '🔍', text: '解释一下 RAG 是什么' },
  { icon: '📊', text: '分析这段代码的问题' },
];

// ── 支持的文档格式 ───────────────────────────────────────────────────────
const ACCEPT_FORMATS = [
  '.pdf', '.docx', '.doc', '.rtf',
  '.xlsx', '.xls', '.xlsm',
  '.pptx', '.ppt', '.pps',
  '.txt', '.md', '.markdown',
  '.html', '.htm',
  '.json', '.yaml', '.yml', '.xml', '.csv',
].join(',');

// ── 类型定义 ──────────────────────────────────────────────────────────────

interface Message {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: string;
  loading?: boolean;
  model?: string;
  fromCache?: boolean;
  fileAttachment?: string;
}

interface Session {
  chatId: string;
  title: string;
  lastTime: string;
  messageCount: number;
}

// ── UUID 生成 ─────────────────────────────────────────────────────────────
const genChatId = (): string => {
  return 'chat-' + Date.now().toString(36) + '-' + Math.random().toString(36).slice(2, 8);
};

// ── 相对时间格式化 ────────────────────────────────────────────────────────
const formatRelativeTime = (isoOrDatetime: string): string => {
  try {
    const diff = Date.now() - new Date(isoOrDatetime).getTime();
    if (diff < 60_000)     return '刚刚';
    if (diff < 3_600_000)  return `${Math.floor(diff / 60_000)} 分钟前`;
    if (diff < 86_400_000) return `${Math.floor(diff / 3_600_000)} 小时前`;
    const d = new Date(isoOrDatetime);
    return `${d.getMonth() + 1}/${d.getDate()}`;
  } catch {
    return '';
  }
};

// ── 流式读取 SSE 工具函数 ─────────────────────────────────────────────────
const readStream = async (
  url: string,
  body: object,
  onChunk: (chunk: string) => void,
  onDone: () => void,
  onError: (err: string) => void
) => {
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    });
    if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
    if (!resp.body) throw new Error('响应体为空');

    const reader  = resp.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });

      const lines = buffer.split('\n');
      buffer = lines.pop() ?? '';   // 保留最后未完整的行

      for (const line of lines) {
        const trimmed = line.trim();
        if (!trimmed || !trimmed.startsWith('data:')) continue;
        const data = trimmed.slice(5).trim();
        if (data === '[DONE]') { onDone(); return; }
        // 检查是否是 error 事件
        if (trimmed.startsWith('event:error')) continue;
        onChunk(data);
      }
    }
    onDone();
  } catch (e: any) {
    onError(e.message || '未知错误');
  }
};

// ── 文档流式读取 (multipart) ─────────────────────────────────────────────
const readDocStream = async (
  formData: FormData,
  onChunk: (chunk: string) => void,
  onDone: () => void,
  onError: (err: string) => void
) => {
  try {
    const resp = await fetch('/ai/document/stream', {
      method: 'POST',
      body: formData,
    });
    if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
    if (!resp.body) throw new Error('响应体为空');

    const reader  = resp.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });

      const lines = buffer.split('\n');
      buffer = lines.pop() ?? '';

      for (const line of lines) {
        const trimmed = line.trim();
        if (!trimmed || !trimmed.startsWith('data:')) continue;
        const data = trimmed.slice(5).trim();
        if (data === '[DONE]') { onDone(); return; }
        onChunk(data);
      }
    }
    onDone();
  } catch (e: any) {
    onError(e.message || '未知错误');
  }
};

// ═════════════════════════════════════════════════════════════════════════
// 主组件
// ═════════════════════════════════════════════════════════════════════════

const ChatPage: React.FC = () => {
  // 每次进入页面生成新的 chatId，确保会话独立
  const [currentChatId, setCurrentChatId] = useState<string>(() => genChatId());
  const [sessions, setSessions]           = useState<Session[]>([]);
  const [messages, setMessages]           = useState<Message[]>([]);
  const [inputValue, setInputValue]       = useState('');
  const [loading, setLoading]             = useState(false);
  const [selectedModel, setSelectedModel] = useState('glm-4.6');
  const [copiedId, setCopiedId]           = useState<string | null>(null);
  const [uploadedFile, setUploadedFile]   = useState<File | null>(null);
  const [exportLoading, setExportLoading] = useState(false);
  const [useRag, setUseRag]               = useState(false);

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const fileInputRef   = useRef<HTMLInputElement>(null);
  const currentChatIdRef = useRef<string>(currentChatId);
  currentChatIdRef.current = currentChatId;

  const scrollToBottom = () =>
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });

  useEffect(() => { scrollToBottom(); }, [messages]);

  // 页面加载时拉取历史会话列表
  useEffect(() => {
    loadSessions();
  }, []);

  const loadSessions = async () => {
    try {
      const res = await axios.get('/ai/conversation/sessions');
      const data: any[] = res.data || [];
      setSessions(data.map(s => ({
        chatId:       String(s.chat_id ?? s.chatId ?? ''),
        title:        String(s.title ?? '新对话'),
        lastTime:     String(s.last_time ?? s.lastTime ?? ''),
        messageCount: Number(s.message_count ?? s.messageCount ?? 0),
      })));
    } catch {
      // MySQL 未连接时静默降级
    }
  };

  // ── 新建对话 ───────────────────────────────────────────────────────────
  const handleNewConversation = () => {
    const newId = genChatId();
    setCurrentChatId(newId);
    setMessages([]);
    setUploadedFile(null);
  };

  // ── 切换历史会话 ───────────────────────────────────────────────────────
  const handleSelectSession = async (session: Session) => {
    setCurrentChatId(session.chatId);
    try {
      const res = await axios.get(`/ai/conversation/history/${session.chatId}`);
      const rows: any[] = res.data || [];
      const msgs: Message[] = rows.map((r, idx) => ({
        id:             String(r.id ?? idx),
        role:           r.role as 'user' | 'assistant',
        content:        String(r.content ?? ''),
        timestamp:      String(r.created_at ?? r.createdAt ?? new Date().toISOString()),
        model:          r.model,
        fileAttachment: r.file_attachment ?? r.fileAttachment,
      }));
      setMessages(msgs);
    } catch {
      antdMessage.error('加载对话历史失败');
    }
  };

  // ── 删除会话 ───────────────────────────────────────────────────────────
  const handleDeleteSession = async (e: React.MouseEvent, chatId: string) => {
    e.stopPropagation();
    try {
      await axios.delete(`/ai/conversation/sessions/${chatId}`);
      setSessions(prev => prev.filter(s => s.chatId !== chatId));
      if (currentChatId === chatId) handleNewConversation();
      antdMessage.success('对话已删除');
    } catch {
      antdMessage.error('删除失败');
    }
  };

  // ── 清空所有历史 ──────────────────────────────────────────────────────
  const handleClearAll = async () => {
    try {
      await axios.delete('/ai/conversation/sessions');
      setSessions([]);
      handleNewConversation();
      antdMessage.success('历史已清空');
    } catch {
      antdMessage.error('清空失败');
    }
  };

  // ── 文件选择 ───────────────────────────────────────────────────────────
  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setUploadedFile(file);
      antdMessage.success(`文件「${file.name}」已选择，请输入问题后发送`);
    }
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  // ── 移除文件 ───────────────────────────────────────────────────────────
  const handleRemoveFile = () => setUploadedFile(null);

  // ── 导出 DOCX ─────────────────────────────────────────────────────────
  const handleExport = async () => {
    const validMsgs = messages.filter(m => !m.loading && m.content);
    if (validMsgs.length === 0 || exportLoading) return;
    setExportLoading(true);
    try {
      const convTitle = sessions.find(s => s.chatId === currentChatId)?.title || '对话记录';
      const res = await axios.post('/ai/document/export', {
        title: convTitle,
        messages: validMsgs.map(m => ({
          role: m.role, content: m.content,
          timestamp: m.timestamp, model: m.model, fileAttachment: m.fileAttachment,
        })),
      }, { responseType: 'blob' });
      const blob = new Blob([res.data], {
        type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      });
      const url = URL.createObjectURL(blob);
      const a   = document.createElement('a');
      a.href = url; a.download = `${convTitle}.docx`;
      document.body.appendChild(a); a.click();
      document.body.removeChild(a); URL.revokeObjectURL(url);
      antdMessage.success('对话已导出为 DOCX 文件');
    } catch {
      antdMessage.error('导出失败，请重试');
    } finally {
      setExportLoading(false);
    }
  };

  // ── 刷新会话列表 ──────────────────────────────────────────────────────
  const refreshSessions = useCallback(async () => {
    await loadSessions();
  }, []);

  // ── 发送消息（核心） ───────────────────────────────────────────────────
  const handleSend = async () => {
    if (!inputValue.trim() || loading) return;

    const now         = new Date().toISOString();
    const currentFile = uploadedFile;
    const chatId      = currentChatIdRef.current;

    const userMsg: Message = {
      id: Date.now().toString(), role: 'user',
      content: inputValue.trim(), timestamp: now,
      fileAttachment: currentFile?.name,
    };

    const assistantId = (Date.now() + 1).toString();
    const placeholder: Message = {
      id: assistantId, role: 'assistant', content: '',
      timestamp: new Date().toISOString(), loading: true, model: selectedModel,
    };

    setMessages(prev => [...prev, userMsg, placeholder]);
    setInputValue('');
    setUploadedFile(null);
    setLoading(true);

    // 流式内容累加器
    let accumulated = '';

    const appendChunk = (chunk: string) => {
      accumulated += chunk;
      setMessages(prev => prev.map(m =>
        m.id === assistantId ? { ...m, content: accumulated, loading: false } : m
      ));
    };

    const onDone = () => {
      setMessages(prev => prev.map(m =>
        m.id === assistantId ? { ...m, content: accumulated || m.content, loading: false } : m
      ));
      setLoading(false);
      refreshSessions();
    };

    const onError = (err: string) => {
      setMessages(prev => prev.map(m =>
        m.id === assistantId
          ? { ...m, content: `⚠️ 请求失败：${err}`, loading: false }
          : m
      ));
      setLoading(false);
    };

    if (currentFile) {
      // ── 文档流式对话 ────────────────────────────────────────────────
      const formData = new FormData();
      formData.append('file',    currentFile);
      formData.append('message', userMsg.content);
      formData.append('model',   selectedModel);
      formData.append('chatId',  chatId);
      formData.append('useRag',  String(useRag));

      await readDocStream(formData, appendChunk, onDone, onError);
    } else {
      // ── 普通流式对话 ────────────────────────────────────────────────
      await readStream(
        '/ai/conversation/stream',
        { chatId, message: userMsg.content, model: selectedModel, useRag, temperature: 0.7, maxTokens: 4000 },
        appendChunk, onDone, onError
      );
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); handleSend(); }
  };

  const handleCopy = (id: string, content: string) => {
    navigator.clipboard.writeText(content);
    setCopiedId(id);
    antdMessage.success('已复制');
    setTimeout(() => setCopiedId(null), 2000);
  };

  const currentModel = MODEL_LIST.find(m => m.value === selectedModel) ?? MODEL_LIST[0];

  // ── 渲染 ───────────────────────────────────────────────────────────────
  return (
    <div className="chat-layout">
      {/* ── 左侧会话列表 ── */}
      <div className="chat-sidebar">
        <div className="sidebar-header">
          <Button
            type="primary" icon={<PlusOutlined />} block
            className="new-chat-btn"
            onClick={handleNewConversation}
          >
            新建对话
          </Button>
        </div>

        <div className="sidebar-model-info">
          <div className="model-info-label">当前模型</div>
          <div className="model-info-tag">
            <ApiOutlined />
            <span>{currentModel.label}</span>
            <Tag color={currentModel.tagColor} style={{ marginLeft: 4, fontSize: 10 }}>
              {currentModel.tag}
            </Tag>
          </div>
          <div className="model-info-desc">{currentModel.desc}</div>
          <div style={{ marginTop: 6, fontSize: 11, color: '#8c8c8c' }}>
            提供商：{currentModel.provider}
          </div>
        </div>

        <div className="sidebar-conv-list">
          <div className="conv-list-title">
            历史对话
            {sessions.length > 0 && (
              <Popconfirm
                title="清空所有历史对话？此操作不可恢复"
                onConfirm={handleClearAll}
                okText="清空" cancelText="取消"
              >
                <button className="conv-clear-all">清空</button>
              </Popconfirm>
            )}
          </div>
          {sessions.length === 0 ? (
            <div className="conv-empty">暂无历史对话</div>
          ) : (
            sessions.map(sess => (
              <div
                key={sess.chatId}
                className={`conv-item ${sess.chatId === currentChatId ? 'active' : ''}`}
                onClick={() => handleSelectSession(sess)}
              >
                <MessageOutlined className="conv-icon" />
                <div className="conv-info">
                  <span className="conv-title">
                    {(sess.title ?? '').slice(0, 22)}{(sess.title ?? '').length > 22 ? '…' : ''}
                  </span>
                  <span className="conv-time">{formatRelativeTime(sess.lastTime)}</span>
                </div>
                <Tooltip title="删除" placement="right">
                  <button
                    className="conv-delete-btn"
                    onClick={e => handleDeleteSession(e, sess.chatId)}
                  >
                    <DeleteOutlined />
                  </button>
                </Tooltip>
              </div>
            ))
          )}
        </div>
      </div>

      {/* ── 右侧主区域 ── */}
      <div className="chat-main">
        {/* 顶部栏 */}
        <div className="chat-topbar">
          <div className="topbar-left">
            <RobotOutlined className="topbar-robot-icon" />
            <span className="topbar-title">灵龙AI 对话助手</span>
            <div className="topbar-status">
              <span className="status-dot" />
              在线
            </div>
          </div>
          <div className="topbar-right">
            {/* RAG 开关 */}
            <Tooltip title="开启后从知识库检索相关文档辅助回答">
              <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginRight: 8 }}>
                <SearchOutlined style={{ color: useRag ? '#1677ff' : '#8c8c8c' }} />
                <span style={{ fontSize: 12, color: useRag ? '#1677ff' : '#8c8c8c' }}>RAG</span>
                <Switch size="small" checked={useRag} onChange={setUseRag} />
              </div>
            </Tooltip>

            {/* 模型选择器 */}
            <Select
              value={selectedModel}
              onChange={setSelectedModel}
              className="model-selector"
              popupClassName="model-selector-popup"
              optionLabelProp="label"
              style={{ minWidth: 160 }}
            >
              {/* 智谱AI 分组 */}
              <Select.OptGroup label="智谱AI (GLM)">
                {MODEL_LIST.filter(m => m.provider === '智谱AI').map(m => (
                  <Select.Option key={m.value} value={m.value} label={
                    <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                      <ApiOutlined style={{ color: m.tagColor }} />
                      {m.label}
                    </span>
                  }>
                    <div className="model-option">
                      <div className="model-option-left">
                        <span className="model-option-name">{m.label}</span>
                        <span className="model-option-desc">{m.desc}</span>
                      </div>
                      <Tag color={m.tagColor} style={{ fontSize: 10 }}>{m.tag}</Tag>
                    </div>
                  </Select.Option>
                ))}
              </Select.OptGroup>
              {/* 灵龙AI 分组 */}
              <Select.OptGroup label="灵龙AI (CodeFlow)">
                {MODEL_LIST.filter(m => m.provider === '灵龙AI').map(m => (
                  <Select.Option key={m.value} value={m.value} label={
                    <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                      <ThunderboltOutlined style={{ color: m.tagColor }} />
                      {m.label}
                    </span>
                  }>
                    <div className="model-option">
                      <div className="model-option-left">
                        <span className="model-option-name">{m.label}</span>
                        <span className="model-option-desc">{m.desc}</span>
                      </div>
                      <Tag color={m.tagColor} style={{ fontSize: 10 }}>{m.tag}</Tag>
                    </div>
                  </Select.Option>
                ))}
              </Select.OptGroup>
            </Select>

            <Tooltip title="导出 DOCX">
              <Button
                icon={<DownloadOutlined />}
                onClick={handleExport}
                loading={exportLoading}
                disabled={messages.filter(m => !m.loading).length === 0}
                className="topbar-btn"
              />
            </Tooltip>
            <Tooltip title="新建对话">
              <Button
                icon={<ClearOutlined />}
                onClick={handleNewConversation}
                className="topbar-btn"
              />
            </Tooltip>
          </div>
        </div>

        {/* 消息区域 */}
        <div className="chat-messages">
          {messages.length === 0 ? (
            <div className="chat-welcome">
              <div className="welcome-avatar"><RobotOutlined /></div>
              <h2 className="welcome-title">你好，我是灵龙AI</h2>
              <p className="welcome-subtitle">
                基于 <strong>{currentModel.label}</strong> 模型 ·{' '}
                {useRag ? <span style={{ color: '#1677ff' }}>RAG 知识库增强已开启</span> : '向量语义缓存加速'}
              </p>
              <div className="quick-prompts">
                {QUICK_PROMPTS.map((p, i) => (
                  <div key={i} className="quick-prompt-item" onClick={() => setInputValue(p.text)}>
                    <span className="quick-prompt-icon">{p.icon}</span>
                    <span>{p.text}</span>
                  </div>
                ))}
              </div>
            </div>
          ) : (
            messages.map(msg => (
              <div key={msg.id} className={`msg-row ${msg.role}`}>
                <div className="msg-avatar-wrap">
                  {msg.role === 'user'
                    ? <Avatar className="msg-avatar user-avatar" icon={<UserOutlined />} />
                    : <Avatar className="msg-avatar ai-avatar" icon={<RobotOutlined />} />
                  }
                </div>

                <div className="msg-body-wrap">
                  <div className="msg-meta">
                    <span className="msg-name">{msg.role === 'user' ? '我' : '灵龙AI'}</span>
                    {msg.role === 'assistant' && msg.model && (
                      <Tag className="msg-model-tag" color="default">{msg.model}</Tag>
                    )}
                    {msg.fromCache && (
                      <Tag className="msg-cache-tag" color="green" icon={<ThunderboltOutlined />}>
                        缓存命中
                      </Tag>
                    )}
                    <span className="msg-time">{new Date(msg.timestamp).toLocaleTimeString()}</span>
                  </div>

                  <div className={`msg-bubble ${msg.role}`}>
                    {msg.role === 'user' && msg.fileAttachment && (
                      <div className="msg-file-badge">
                        <FileTextOutlined />
                        <span>{msg.fileAttachment}</span>
                      </div>
                    )}
                    {msg.loading ? (
                      <div className="msg-loading">
                        <span className="typing-dot" /><span className="typing-dot" /><span className="typing-dot" />
                        <span className="typing-text">思考中</span>
                      </div>
                    ) : msg.role === 'assistant' ? (
                      <ReactMarkdown
                        remarkPlugins={[remarkGfm]}
                        rehypePlugins={[rehypeRaw]}
                        components={{
                          code({ node, inline, className, children, ...props }: any) {
                            return inline
                              ? <code className="inline-code" {...props}>{children}</code>
                              : <pre className="code-block"><code className={className} {...props}>{children}</code></pre>;
                          },
                        }}
                      >
                        {msg.content}
                      </ReactMarkdown>
                    ) : (
                      <Text>{msg.content}</Text>
                    )}
                  </div>

                  {msg.role === 'assistant' && !msg.loading && (
                    <div className="msg-actions">
                      <Tooltip title={copiedId === msg.id ? '已复制' : '复制'}>
                        <button className="msg-action-btn" onClick={() => handleCopy(msg.id, msg.content)}>
                          {copiedId === msg.id ? <CheckOutlined /> : <CopyOutlined />}
                        </button>
                      </Tooltip>
                      <Tooltip title="有帮助">
                        <button className="msg-action-btn"><LikeOutlined /></button>
                      </Tooltip>
                      <Tooltip title="没帮助">
                        <button className="msg-action-btn"><DislikeOutlined /></button>
                      </Tooltip>
                    </div>
                  )}
                </div>
              </div>
            ))
          )}
          <div ref={messagesEndRef} />
        </div>

        {/* 输入区域 */}
        <div className="chat-input-wrap">
          {/* 隐藏文件选择器 */}
          <input
            ref={fileInputRef}
            type="file"
            accept={ACCEPT_FORMATS}
            style={{ display: 'none' }}
            onChange={handleFileChange}
          />

          <div className="input-toolbar">
            <span className="input-hint">
              <DatabaseOutlined style={{ marginRight: 4 }} />
              {useRag
                ? <span style={{ color: '#1677ff' }}>RAG 增强已开启 · 知识库检索辅助回答</span>
                : '向量语义缓存加速 · 相似问题自动加速'}
            </span>
            <span className="input-shortcut">Enter 发送 · Shift+Enter 换行</span>
          </div>

          {/* 文件附件预览 */}
          {uploadedFile && (
            <div className="file-attachment-bar">
              <FileTextOutlined className="file-attach-icon" />
              <span className="file-attach-name">{uploadedFile.name}</span>
              <span className="file-attach-size">({(uploadedFile.size / 1024).toFixed(1)} KB)</span>
              <button className="file-attach-remove" onClick={handleRemoveFile}>
                <CloseCircleOutlined />
              </button>
            </div>
          )}

          <div className="input-box">
            <Tooltip title="上传文档（PDF/Word/Excel/PPT/RTF/Markdown/TXT/XML/HTML/JSON/CSV）">
              <button
                className={`upload-file-btn${uploadedFile ? ' has-file' : ''}`}
                onClick={() => fileInputRef.current?.click()}
                disabled={loading}
              >
                <PaperClipOutlined />
              </button>
            </Tooltip>

            <TextArea
              value={inputValue}
              onChange={e => setInputValue(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder={uploadedFile
                ? `就「${uploadedFile.name}」提问或说明需求…`
                : `向 ${currentModel.label} 发送消息...`}
              autoSize={{ minRows: 1, maxRows: 6 }}
              disabled={loading}
              className="chat-textarea"
              bordered={false}
            />

            <Button
              type="primary"
              icon={<SendOutlined />}
              onClick={handleSend}
              loading={loading}
              disabled={!inputValue.trim()}
              className="send-btn"
            >
              发送
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ChatPage;
