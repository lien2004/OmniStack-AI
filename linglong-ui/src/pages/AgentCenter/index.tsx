import { useState, useEffect, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Button, Tag, Modal, Input, message, Spin, Empty, Switch,
  Form, Divider, Tooltip, Upload, Checkbox, Radio, Space,
} from 'antd'
import {
  RobotOutlined, PlayCircleOutlined, SettingOutlined,
  CheckCircleOutlined, CloseCircleOutlined, LoadingOutlined,
  ThunderboltOutlined, EyeOutlined, EyeInvisibleOutlined,
  HistoryOutlined, ReloadOutlined, UploadOutlined,
  FolderOutlined, FileTextOutlined, FolderOpenOutlined,
  PlusOutlined, SearchOutlined,
} from '@ant-design/icons'
import axios from '@/services/api'
import type { UploadFile, UploadProps } from 'antd/es/upload/interface'

const { TextArea } = Input
const Text = ({ children, style, ...p }: any) => <span {...p} style={{ ...style }}>{children}</span>

interface AgentInfo {
  name: string
  description: string
  type: string
  systemPrompt: string
  displayName?: string
  usageCount?: number
  enabled?: boolean
  lastExecutionTime?: string
  parameters?: Record<string, unknown>
}

interface WorkflowInfo {
  id: string
  type: string
  status: string
  currentStepIndex: number
  totalSteps: number
  steps: StepInfo[]
}

interface StepInfo {
  agentName: string
  name: string
  status: string
  output?: string
  success?: boolean
  errorMessage?: string
}

const typeEmojiMap: Record<string, string> = {
  REQUIREMENT_ANALYSIS: '🔍',
  DOMAIN_MODELING: '🏗️',
  ARCHITECTURE_DESIGN: '🏛️',
  CODE_GENERATION: '💻',
  CODE_REVIEW: '🔎',
  TEST_GENERATION: '🧪',
  DOCUMENTATION: '📝',
  DEPLOYMENT: '🚀',
  DEBUGGING: '🐛',
  OPTIMIZATION: '⚡',
  RESUME: '📄',
}

const agentTagsMap: Record<string, string[]> = {
  REQUIREMENT_ANALYSIS: ['ReAct', 'analysis', 'requirement'],
  DOMAIN_MODELING: ['Plan-Execute', 'modeling', 'domain'],
  ARCHITECTURE_DESIGN: ['Plan-Execute', 'architecture', 'design'],
  CODE_GENERATION: ['ReAct', 'code', 'generation', 'developer'],
  CODE_REVIEW: ['ReAct', 'code', 'review', 'developer'],
  TEST_GENERATION: ['Plan-Execute', 'test', 'qa'],
  DOCUMENTATION: ['ReAct', 'doc', 'writer'],
  DEPLOYMENT: ['Plan-Execute', 'deploy', 'ops'],
  DEBUGGING: ['ReAct', 'debug', 'fix'],
  OPTIMIZATION: ['ReAct', 'optimize', 'performance'],
  RESUME: ['ReAct', 'resume', 'hr'],
}

function AgentCenter() {
  const navigate = useNavigate()
  const [agents, setAgents] = useState<AgentInfo[]>([])
  const [loading, setLoading] = useState(true)
  const [runModalVisible, setRunModalVisible] = useState(false)
  const [workflowModalVisible, setWorkflowModalVisible] = useState(false)
  const [configModalVisible, setConfigModalVisible] = useState(false)
  const [currentAgent, setCurrentAgent] = useState<AgentInfo | null>(null)
  const [inputText, setInputText] = useState('')
  const [outputText, setOutputText] = useState('')
  const [executing, setExecuting] = useState(false)
  const [workflowInput, setWorkflowInput] = useState('')
  const [currentWorkflow, setCurrentWorkflow] = useState<WorkflowInfo | null>(null)
  const [workflowLoading, setWorkflowLoading] = useState(false)
  const [configForm] = Form.useForm()
  const [savingConfig, setSavingConfig] = useState(false)
  const [showSystemPrompt, setShowSystemPrompt] = useState(false)
  const [searchQuery, setSearchQuery] = useState('')
  const [filterTag, setFilterTag] = useState('all')

  // 文件上传相关状态
  const [fileList, setFileList] = useState<UploadFile[]>([])
  const [outputPath, setOutputPath] = useState('')
  const [autoSave, setAutoSave] = useState(false)
  const [fileInputMode, setFileInputMode] = useState<'upload' | 'path'>('upload')
  const [filePaths, setFilePaths] = useState<string>('')

  useEffect(() => { loadAgents() }, [])

  const loadAgents = async () => {
    setLoading(true)
    try {
      const res = await axios.get('/agent/list/detail')
      if (res.data?.code === 200 || res.data?.success) {
        setAgents(res.data.data || [])
        setLoading(false)
        return
      }
    } catch {
      // ignore
    }
    try {
      const res = await axios.get('/agent/list')
      if (res.data?.code === 200 || res.data?.success) {
        setAgents((res.data.data || []).map((a: AgentInfo) => ({ ...a, usageCount: 0, enabled: true })))
      }
    } catch {
      message.error('加载Agent列表失败')
      setAgents([])
    } finally {
      setLoading(false)
    }
  }

  // 搜索+筛选后的列表
  const filteredAgents = useMemo(() => {
    let list = agents
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase()
      list = list.filter(a =>
        (a.displayName || a.name).toLowerCase().includes(q) ||
        (a.description || '').toLowerCase().includes(q)
      )
    }
    if (filterTag === 'enabled') {
      list = list.filter(a => a.enabled !== false)
    } else if (filterTag === 'disabled') {
      list = list.filter(a => a.enabled === false)
    } else if (filterTag !== 'all') {
      list = list.filter(a => {
        const tags = agentTagsMap[a.type] || []
        return tags.includes(filterTag) || a.type === filterTag
      })
    }
    return list
  }, [agents, searchQuery, filterTag])

  const openRunModal = (agent: AgentInfo) => {
    if (agent.enabled === false) { message.warning('该Agent已被禁用'); return }
    setCurrentAgent(agent)
    setInputText(''); setOutputText(''); setFileList([]); setOutputPath(''); setAutoSave(false); setFileInputMode('upload'); setFilePaths('')
    setRunModalVisible(true)
  }

  const openConfigModal = (agent: AgentInfo) => {
    setCurrentAgent(agent)
    configForm.setFieldsValue({
      displayName: agent.displayName || agent.name.replace('Agent', ''),
      description: agent.description,
      systemPrompt: agent.systemPrompt,
      enabled: agent.enabled !== false,
    })
    setShowSystemPrompt(false)
    setConfigModalVisible(true)
  }

  const executeAgent = async () => {
    if (!currentAgent || !inputText.trim()) { message.warning('请输入内容'); return }
    setExecuting(true); setOutputText('')
    try {
      let res
      if (fileInputMode === 'path' && filePaths.trim()) {
        res = await axios.post(`/agent/${currentAgent.name}/execute`, {
          input: inputText, outputPath, autoSave,
          filePaths: filePaths.split('\n').map(s => s.trim()).filter(Boolean),
        }, { timeout: 300000 })
      } else if (fileList.length > 0) {
        const formData = new FormData()
        formData.append('input', inputText); formData.append('outputPath', outputPath); formData.append('autoSave', autoSave.toString())
        fileList.forEach((file) => { const f = (file as any).originFileObj || file; if (f) formData.append('files', f as Blob) })
        res = await axios.post(`/agent/${currentAgent.name}/execute-with-files`, formData, {
          headers: { 'Content-Type': 'multipart/form-data' }, timeout: 300000,
        })
      } else {
        res = await axios.post(`/agent/${currentAgent.name}/execute`, { input: inputText, outputPath, autoSave }, { timeout: 300000 })
      }
      if (res.data?.code === 200) {
        const agentResult = res.data.data
        let output = agentResult?.output || '执行完成'
        if (agentResult?.data?.savedPath) output += `\n\n---\n结果已保存到: ${agentResult.data.savedPath}`
        setOutputText(output)
        loadAgents()
      } else {
        setOutputText(`执行失败: ${res.data?.message || '未知错误'}`)
      }
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } }; message?: string }
      setOutputText(`执行失败: ${err.response?.data?.message || err.message || '未知错误'}`)
    } finally {
      setExecuting(false)
    }
  }

  const uploadProps: UploadProps = {
    onRemove: (file) => { const idx = fileList.indexOf(file); const n = fileList.slice(); n.splice(idx, 1); setFileList(n) },
    beforeUpload: (file) => { setFileList([...fileList, file]); return false },
    fileList, multiple: true,
    accept: '.pdf,.docx,.doc,.rtf,.xlsx,.xls,.xlsm,.pptx,.ppt,.pps,.txt,.md,.markdown,.html,.htm,.json,.yaml,.yml,.xml,.csv,.java,.py,.js,.ts,.go,.cpp,.c',
  }

  const saveAgentConfig = async (values: { displayName: string; description: string; systemPrompt: string; enabled: boolean }) => {
    if (!currentAgent) return
    setSavingConfig(true)
    try {
      const res = await axios.post(`/agent/${currentAgent.name}/config`, {
        agentName: currentAgent.name, displayName: values.displayName,
        description: values.description, systemPrompt: values.systemPrompt, enabled: values.enabled,
      })
      if (res.data?.success) { message.success('配置保存成功'); setConfigModalVisible(false); loadAgents() }
      else { message.error(res.data?.message || '保存失败') }
    } catch { message.error('保存配置失败') }
    finally { setSavingConfig(false) }
  }

  const toggleAgentStatus = async (agent: AgentInfo, enabled: boolean) => {
    try {
      const url = enabled ? `/agent/${agent.name}/enable` : `/agent/${agent.name}/disable`
      const res = await axios.post(url)
      if (res.data?.success) { message.success(enabled ? 'Agent已启用' : 'Agent已禁用'); loadAgents() }
      else { message.error(res.data?.message || '操作失败') }
    } catch { message.error('操作失败') }
  }

  const startWorkflow = async () => {
    if (!workflowInput.trim()) { message.warning('请输入需求描述'); return }
    setWorkflowLoading(true)
    try {
      const res = await axios.post('/agent/workflow/start', { input: workflowInput })
      if (res.data?.success) { setCurrentWorkflow(res.data.data); message.success('工作流已启动'); pollWorkflowStatus(res.data.data.id) }
      else { message.error(res.data?.message || '启动失败') }
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } }; message?: string }
      message.error(err.response?.data?.message || err.message || '启动失败')
    } finally { setWorkflowLoading(false) }
  }

  const pollWorkflowStatus = async (workflowId: string) => {
    const poll = async () => {
      try {
        const res = await axios.get(`/agent/workflow/${workflowId}`)
        if (res.data?.success) {
          const w = res.data.data
          setCurrentWorkflow(w)
          if (w.status === 'RUNNING') setTimeout(poll, 2000)
        }
      } catch { /* ignore */ }
    }
    poll()
  }

  const getStepIcon = (status: string) => {
    switch (status) { case 'COMPLETED': return <CheckCircleOutlined />; case 'FAILED': return <CloseCircleOutlined />; case 'RUNNING': return <LoadingOutlined />; default: return undefined }
  }

  const filterTags = [
    { key: 'all', label: '全部' },
    { key: 'ReAct', label: 'ReAct' },
    { key: 'Plan-Execute', label: 'Plan-Execute' },
    { key: 'enabled', label: '已启用' },
    { key: 'disabled', label: '已停用' },
  ]

  return (
    <div style={{ minHeight: '100%', background: '#faf8f5', padding: '24px 32px 32px' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 24 }}>
        <div>
          <div style={{ fontSize: 11, fontWeight: 600, color: '#8c8c8c', textTransform: 'uppercase', letterSpacing: 1.5, marginBottom: 8 }}>Agent Studio</div>
          <h1 style={{ margin: 0, fontSize: 28, fontWeight: 700, color: '#1a1a2e' }}>智能体管理</h1>
          <p style={{ color: '#8c8c8c', margin: '6px 0 0', fontSize: 14 }}>创建、编辑和管理你的 AI 智能体</p>
        </div>
        <Space>
          <Button type="primary" size="large" icon={<ThunderboltOutlined />} style={{ borderRadius: 8 }} onClick={() => navigate('/codeflow')}>
            全AI开发
          </Button>
          <Button size="large" icon={<PlusOutlined />} style={{ borderRadius: 8 }}>
            新建智能体
          </Button>
        </Space>
      </div>

      {/* Search & Filter */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 24, flexWrap: 'wrap' }}>
        <Input
          prefix={<SearchOutlined style={{ color: '#bfbfbf' }} />}
          placeholder="搜索智能体..."
          value={searchQuery}
          onChange={e => setSearchQuery(e.target.value)}
          style={{ width: 280, borderRadius: 10, background: '#fff' }}
          allowClear
        />
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
          {filterTags.map(tag => (
            <button
              key={tag.key}
              onClick={() => setFilterTag(tag.key)}
              style={{
                padding: '6px 16px', borderRadius: 20, border: '1px solid #e8e8e8',
                background: filterTag === tag.key ? '#fff2e8' : '#fff',
                color: filterTag === tag.key ? '#d46b08' : '#595959',
                fontSize: 13, cursor: 'pointer', fontWeight: 500,
                transition: 'all 0.2s', outline: 'none',
              }}
            >
              {tag.label}
            </button>
          ))}
        </div>
        <Button icon={<ReloadOutlined />} onClick={loadAgents} loading={loading} style={{ marginLeft: 'auto' }}>
          刷新
        </Button>
      </div>

      {/* Agent Grid */}
      <Spin spinning={loading}>
        {filteredAgents.length === 0 ? (
          <Empty description={agents.length === 0 ? '暂无Agent，请确保Agent服务已启动' : '未找到匹配的智能体'} style={{ padding: 60 }} />
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))', gap: 16 }}>
            {filteredAgents.map((agent) => {
              const tags = agentTagsMap[agent.type] || ['ReAct']
              const time = agent.lastExecutionTime
                ? new Date(agent.lastExecutionTime).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).replace('/', '-')
                : ''
              return (
                <div
                  key={agent.name}
                  style={{
                    background: '#fff', borderRadius: 16, padding: 20,
                    border: '1px solid #f0f0f0',
                    opacity: agent.enabled === false ? 0.65 : 1,
                    transition: 'all 0.2s ease',
                    cursor: 'pointer',
                  }}
                  onMouseEnter={e => { (e.currentTarget as HTMLElement).style.boxShadow = '0 4px 16px rgba(0,0,0,0.06)'; (e.currentTarget as HTMLElement).style.borderColor = '#e8e8e8' }}
                  onMouseLeave={e => { (e.currentTarget as HTMLElement).style.boxShadow = 'none'; (e.currentTarget as HTMLElement).style.borderColor = '#f0f0f0' }}
                >
                  {/* Top: emoji + switch */}
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 14 }}>
                    <div style={{
                      width: 44, height: 44, borderRadius: 12, background: '#fff7e6',
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                      fontSize: 22, border: '1px solid #ffe7ba',
                    }}>
                      {typeEmojiMap[agent.type] || '🤖'}
                    </div>
                    <Switch
                      size="small"
                      checked={agent.enabled !== false}
                      onChange={(checked) => toggleAgentStatus(agent, checked)}
                    />
                  </div>

                  {/* Name */}
                  <div style={{ fontSize: 16, fontWeight: 600, color: '#1a1a2e', marginBottom: 8, lineHeight: 1.4 }}>
                    {agent.displayName || agent.name.replace('Agent', '')}
                  </div>

                  {/* Description */}
                  <div style={{
                    fontSize: 13, color: '#8c8c8c', lineHeight: 1.6,
                    marginBottom: 14, minHeight: 42,
                    display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical' as any,
                    overflow: 'hidden', textOverflow: 'ellipsis',
                  }}>
                    {agent.description || '暂无描述'}
                  </div>

                  {/* Tags */}
                  <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', marginBottom: 14 }}>
                    {tags.slice(0, 4).map((t, i) => (
                      <span key={i} style={{
                        fontSize: 11, color: '#d46b08', background: '#fff2e8',
                        padding: '2px 8px', borderRadius: 10, fontWeight: 500,
                      }}>
                        {t}
                      </span>
                    ))}
                  </div>

                  {/* Divider + Meta */}
                  <div style={{ borderTop: '1px solid #f0f0f0', paddingTop: 12, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span style={{ fontSize: 12, color: '#bfbfbf' }}>
                      {time || '未执行'}
                    </span>
                    <Space size={4}>
                      <Tooltip title="运行">
                        <Button
                          type="text"
                          size="small"
                          icon={<PlayCircleOutlined />}
                          disabled={agent.enabled === false}
                          onClick={() => openRunModal(agent)}
                        />
                      </Tooltip>
                      <Tooltip title="配置">
                        <Button type="text" size="small" icon={<SettingOutlined />} onClick={() => openConfigModal(agent)} />
                      </Tooltip>
                      <Tooltip title={`使用: ${agent.usageCount || 0}`}>
                        <span style={{ fontSize: 11, color: '#bfbfbf', padding: '0 4px' }}>
                          <HistoryOutlined /> {agent.usageCount || 0}
                        </span>
                      </Tooltip>
                    </Space>
                  </div>
                </div>
              )
            })}
          </div>
        )}
      </Spin>

      {/* Run Modal */}
      <Modal
        title={<span><RobotOutlined style={{ marginRight: 8, color: '#1677ff' }} />{currentAgent?.name.replace('Agent', '')} - 执行</span>}
        open={runModalVisible}
        onCancel={() => setRunModalVisible(false)}
        width={800}
        footer={null}
      >
        <div style={{ marginBottom: 16 }}>
          <Text style={{ fontWeight: 600, display: 'block', marginBottom: 8 }}>输入内容</Text>
          <TextArea rows={4} value={inputText} onChange={e => setInputText(e.target.value)} placeholder="请输入要处理的内容..." />
        </div>

        <div style={{ marginBottom: 16 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
            <Text style={{ fontWeight: 600 }}>文件输入（可选）</Text>
            <Radio.Group size="small" value={fileInputMode} onChange={e => { setFileInputMode(e.target.value); setFileList([]); setFilePaths('') }}>
              <Radio.Button value="upload"><UploadOutlined /> 上传文件</Radio.Button>
              <Radio.Button value="path"><FolderOpenOutlined /> 文件路径</Radio.Button>
            </Radio.Group>
          </div>
          {fileInputMode === 'upload' ? (
            <div>
              <Upload {...uploadProps}><Button icon={<UploadOutlined />}>选择文件</Button></Upload>
              <span style={{ fontSize: 12, color: '#8c8c8c', display: 'block', marginTop: 4 }}>支持：PDF、Word、Excel、PPT、Markdown、TXT 等多种格式</span>
            </div>
          ) : (
            <div>
              <Input value={filePaths} onChange={e => setFilePaths(e.target.value)} placeholder="输入文件绝对路径，多个文件用换行分隔..." prefix={<FileTextOutlined style={{ color: '#bfbfbf' }} />} />
              <span style={{ fontSize: 12, color: '#8c8c8c', display: 'block', marginTop: 4 }}>示例：E:/docs/report.pdf（多个文件每行一个）</span>
            </div>
          )}
        </div>

        <div style={{ marginBottom: 16, padding: 16, background: '#f6ffed', borderRadius: 8, border: '1px solid #b7eb8f' }}>
          <Text style={{ fontWeight: 600, display: 'block', marginBottom: 8, color: '#52c41a' }}><FolderOutlined style={{ marginRight: 4 }} />输出保存配置</Text>
          <div style={{ marginBottom: 8 }}>
            <Checkbox checked={autoSave} onChange={e => setAutoSave(e.target.checked)}>自动保存执行结果到文件</Checkbox>
          </div>
          {autoSave && (
            <div>
              <Text style={{ fontSize: 12, display: 'block', marginBottom: 4 }}>保存路径（可选）</Text>
              <Input placeholder="例如: E:/MyProjects/AgentOutputs" value={outputPath} onChange={e => setOutputPath(e.target.value)} prefix={<FolderOutlined />} />
            </div>
          )}
        </div>

        <div style={{ marginBottom: 16, textAlign: 'right' }}>
          <Button type="primary" loading={executing} onClick={executeAgent}>执行</Button>
        </div>

        {outputText && (
          <div>
            <Text style={{ fontWeight: 600, display: 'block', marginBottom: 8 }}>执行结果</Text>
            <pre style={{ margin: 0, padding: 16, background: '#f5f5f5', borderRadius: 8, maxHeight: 400, overflow: 'auto', whiteSpace: 'pre-wrap', wordBreak: 'break-word', fontSize: 13 }}>
              {outputText}
            </pre>
          </div>
        )}
      </Modal>

      {/* Config Modal */}
      <Modal
        title={<span><SettingOutlined style={{ marginRight: 8, color: '#1677ff' }} />{currentAgent?.displayName || currentAgent?.name?.replace('Agent', '')} - 配置</span>}
        open={configModalVisible}
        onCancel={() => setConfigModalVisible(false)}
        width={700}
        footer={null}
      >
        <Form form={configForm} layout="vertical" onFinish={saveAgentConfig} style={{ marginTop: 16 }}>
          <Form.Item name="displayName" label="显示名称" rules={[{ required: true, message: '请输入显示名称' }]}>
            <Input placeholder="请输入显示名称" />
          </Form.Item>
          <Form.Item name="description" label="描述" rules={[{ required: true, message: '请输入描述' }]}>
            <Input.TextArea rows={2} placeholder="请输入Agent描述" />
          </Form.Item>
          <Form.Item name="systemPrompt" label={
            <span>系统提示词
              <Button type="link" size="small" icon={showSystemPrompt ? <EyeInvisibleOutlined /> : <EyeOutlined />} onClick={() => setShowSystemPrompt(!showSystemPrompt)} style={{ marginLeft: 8 }}>
                {showSystemPrompt ? '隐藏' : '显示'}
              </Button>
            </span>
          }>
            <Input.TextArea rows={showSystemPrompt ? 10 : 3} placeholder="系统提示词定义了Agent的行为和角色" style={{ fontFamily: 'monospace' }} />
          </Form.Item>
          <Form.Item name="enabled" label="状态" valuePropName="checked">
            <Switch checkedChildren="启用" unCheckedChildren="禁用" onChange={(checked) => { if (currentAgent) toggleAgentStatus(currentAgent, checked) }} />
          </Form.Item>
          <Divider />
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div style={{ color: '#666', fontSize: 12 }}>
              {currentAgent && <div>使用次数: {currentAgent.usageCount || 0}</div>}
              {currentAgent?.lastExecutionTime && <div>最后执行: {new Date(currentAgent.lastExecutionTime).toLocaleString()}</div>}
            </div>
            <div>
              <Button style={{ marginRight: 8 }} onClick={() => setConfigModalVisible(false)}>取消</Button>
              <Button type="primary" loading={savingConfig} htmlType="submit">保存</Button>
            </div>
          </div>
        </Form>
      </Modal>

      {/* Workflow Modal */}
      <Modal
        title={<span><ThunderboltOutlined style={{ marginRight: 8, color: '#1677ff' }} />全AI智能开发工作流</span>}
        open={workflowModalVisible}
        onCancel={() => setWorkflowModalVisible(false)}
        width={900}
        footer={null}
      >
        {!currentWorkflow ? (
          <div>
            <Text style={{ fontWeight: 600, display: 'block', marginBottom: 8 }}>需求描述</Text>
            <TextArea rows={6} value={workflowInput} onChange={e => setWorkflowInput(e.target.value)} placeholder="请描述您的软件开发需求..." />
            <div style={{ marginTop: 16, textAlign: 'right' }}>
              <Button type="primary" size="large" loading={workflowLoading} onClick={startWorkflow}>启动工作流</Button>
            </div>
          </div>
        ) : (
          <div>
            {/* Steps */}
            <div style={{ display: 'flex', gap: 8, marginBottom: 24, overflow: 'auto' }}>
              {currentWorkflow.steps.map((step, idx) => (
                <div key={idx} style={{
                  flex: 1, minWidth: 100, textAlign: 'center', padding: '12px 8px',
                  borderRadius: 10,
                  background: step.status === 'COMPLETED' ? '#f6ffed' : step.status === 'FAILED' ? '#fff2f0' : step.status === 'RUNNING' ? '#e6f4ff' : '#f5f5f5',
                  border: `1px solid ${step.status === 'COMPLETED' ? '#b7eb8f' : step.status === 'FAILED' ? '#ffccc7' : step.status === 'RUNNING' ? '#91caff' : '#d9d9d9'}`,
                }}>
                  <div style={{ fontSize: 18, marginBottom: 6 }}>{getStepIcon(step.status)}</div>
                  <div style={{ fontSize: 12, fontWeight: 500 }}>{step.name}</div>
                </div>
              ))}
            </div>
            <div style={{ marginBottom: 16 }}>
              <Tag color={currentWorkflow.status === 'COMPLETED' ? 'success' : currentWorkflow.status === 'FAILED' ? 'error' : 'processing'}>{currentWorkflow.status}</Tag>
              <span style={{ marginLeft: 12, color: '#8c8c8c', fontSize: 13 }}>进度: {currentWorkflow.currentStepIndex + 1} / {currentWorkflow.totalSteps}</span>
            </div>
            {currentWorkflow.steps.map((step, idx) => step.output && (
              <div key={idx} style={{ marginTop: 12 }}>
                <Text style={{ fontWeight: 600, fontSize: 13, display: 'block', marginBottom: 6 }}>{step.name} 输出</Text>
                <pre style={{ margin: 0, padding: 12, background: '#f5f5f5', borderRadius: 8, maxHeight: 200, overflow: 'auto', whiteSpace: 'pre-wrap', fontSize: 12 }}>
                  {step.output.substring(0, 500)}{step.output.length > 500 ? '...' : ''}
                </pre>
              </div>
            ))}
          </div>
        )}
      </Modal>
    </div>
  )
}

export default AgentCenter
