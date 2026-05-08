import { useState, useRef, useEffect, useCallback } from 'react'
import {
  Button, Input, Select, message, Spin, Tag, Space, Empty,
  Typography, Badge, Card, Divider,
} from 'antd'
import {
  ThunderboltOutlined, PlayCircleOutlined, PauseCircleOutlined,
  ReloadOutlined, CheckCircleOutlined, ClockCircleOutlined,
  LoadingOutlined, CloseCircleOutlined, RobotOutlined,
  FileTextOutlined, BranchesOutlined, ApartmentOutlined,
  CodeOutlined, SafetyOutlined, ExperimentOutlined,
  ArrowRightOutlined, FolderOutlined, FileOutlined,
  DownloadOutlined, FolderOpenOutlined, FileZipOutlined,
} from '@ant-design/icons'
import axios from '@/services/api'
import JSZip from 'jszip'
import { saveAs } from 'file-saver'

const { TextArea } = Input
const { Text} = Typography

// ── 工作流步骤定义 ─────────────────────────────────────────────────────────

interface WorkflowStep {
  key: string
  name: string
  icon: React.ReactNode
  color: string
  bgColor: string
  description: string
}

const WORKFLOW_STEPS: WorkflowStep[] = [
  { key: 'REQUIREMENT_ANALYSIS', name: '需求分析', icon: <FileTextOutlined />, color: '#1677ff', bgColor: '#e6f4ff', description: '分析并理解软件需求' },
  { key: 'DOMAIN_MODELING', name: '领域建模', icon: <BranchesOutlined />, color: '#52c41a', bgColor: '#f6ffed', description: '构建领域模型与实体关系' },
  { key: 'ARCHITECTURE_DESIGN', name: '架构设计', icon: <ApartmentOutlined />, color: '#722ed1', bgColor: '#f9f0ff', description: '设计系统架构与技术选型' },
  { key: 'CODE_GENERATION', name: '代码生成', icon: <CodeOutlined />, color: '#fa8c16', bgColor: '#fff7e6', description: '生成核心业务代码' },
  { key: 'CODE_REVIEW', name: '代码审查', icon: <SafetyOutlined />, color: '#eb2f96', bgColor: '#fff0f6', description: '审查代码质量与规范' },
  { key: 'TEST_GENERATION', name: '测试生成', icon: <ExperimentOutlined />, color: '#13c2c2', bgColor: '#e6fffb', description: '生成单元测试与集成测试' },
]

// ── 模型定义 ──────────────────────────────────────────────────────────────

interface ModelOption {
  value: string
  label: string
  provider: string
  providerLabel: string
  icon: string
  desc: string
}

const MODEL_OPTIONS: ModelOption[] = [
  // DeepSeek
  { value: 'deepseek-v4-pro', label: 'DeepSeek V4 Pro', provider: 'deepseek', providerLabel: 'DeepSeek', icon: '🔭', desc: '深度思考 · 最强推理' },
  { value: 'deepseek-v4-flash', label: 'DeepSeek V4 Flash', provider: 'deepseek', providerLabel: 'DeepSeek', icon: '🔭', desc: '快速响应 · 高效生成' },
  { value: 'deepseek-reasoner', label: 'DeepSeek Reasoner', provider: 'deepseek', providerLabel: 'DeepSeek', icon: '🔭', desc: '推理增强 · 逻辑严谨' },
  // Zhipu
  { value: 'glm-5.1', label: 'GLM 5.1', provider: 'zhipu', providerLabel: '智谱AI', icon: '🐉', desc: '最新旗舰 · 全能模型' },
  { value: 'glm-5', label: 'GLM 5', provider: 'zhipu', providerLabel: '智谱AI', icon: '🐉', desc: '旗舰模型 · 全面领先' },
  { value: 'glm-5-turbo', label: 'GLM 5 Turbo', provider: 'zhipu', providerLabel: '智谱AI', icon: '🐉', desc: '极速响应 · 高性价比' },
  { value: 'glm-4.7', label: 'GLM 4.7', provider: 'zhipu', providerLabel: '智谱AI', icon: '🐉', desc: '高性能 · 长上下文' },
  { value: 'glm-4.6', label: 'GLM 4.6', provider: 'zhipu', providerLabel: '智谱AI', icon: '🐉', desc: '均衡性能 · 稳定可靠' },
  // Qianwen
  { value: 'qwen-max', label: 'Qwen Max', provider: 'aliyun', providerLabel: '通义千问', icon: '☁️', desc: '旗舰模型 · 最强性能' },
  { value: 'qwen-plus', label: 'Qwen Plus', provider: 'aliyun', providerLabel: '通义千问', icon: '☁️', desc: '高性能 · 复杂任务' },
  { value: 'qwen-turbo', label: 'Qwen Turbo', provider: 'aliyun', providerLabel: '通义千问', icon: '☁️', desc: '快速响应 · 日常任务' },
  { value: 'qwen-coder-plus', label: 'Qwen Coder Plus', provider: 'aliyun', providerLabel: '通义千问', icon: '☁️', desc: '代码专家 · 编程首选' },
]

// ── 状态 ──────────────────────────────────────────────────────────────────

type StepStatus = 'pending' | 'running' | 'completed' | 'failed'

interface StepState {
  status: StepStatus
  output: string
  startTime?: string
  endTime?: string
}

// ── 组件 ──────────────────────────────────────────────────────────────────

function CodeFlowPage() {
  const [requirement, setRequirement] = useState('')
  const [selectedModel, setSelectedModel] = useState('deepseek-v4-pro')
  const [running, setRunning] = useState(false)
  const [workflowId, setWorkflowId] = useState<string | null>(null)
  const [stepStates, setStepStates] = useState<Record<string, StepState>>({})
  const [, setCurrentStepIndex] = useState(-1)
  const [globalStatus, setGlobalStatus] = useState<'idle' | 'running' | 'completed' | 'failed'>('idle')
  const [elapsedTime, setElapsedTime] = useState(0)
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null)
  const abortRef = useRef(false)

  // 代码文件导出相关状态
  const [codeFiles, setCodeFiles] = useState<{ path: string; content: string; lang: string }[]>([])
  const [selectedFilePath, setSelectedFilePath] = useState<string | null>(null)
  const [fileTreeExpanded, setFileTreeExpanded] = useState(true)

  // 计时器
  useEffect(() => {
    if (running && globalStatus === 'running') {
      timerRef.current = setInterval(() => setElapsedTime(t => t + 1), 1000)
    } else {
      if (timerRef.current) clearInterval(timerRef.current)
    }
    return () => { if (timerRef.current) clearInterval(timerRef.current) }
  }, [running, globalStatus])

  // 格式化时间
  const fmtTime = (s: number) => {
    const m = Math.floor(s / 60)
    const sec = s % 60
    return `${m.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}`
  }

  // 启动工作流
  const startWorkflow = async () => {
    if (!requirement.trim()) { message.warning('请输入需求描述'); return }
    abortRef.current = false
    setRunning(true)
    setGlobalStatus('running')
    setElapsedTime(0)
    setCurrentStepIndex(0)
    setStepStates({})
    setWorkflowId(null)

    // 初始化步骤状态
    const initStates: Record<string, StepState> = {}
    WORKFLOW_STEPS.forEach((step, i) => {
      initStates[step.key] = { status: i === 0 ? 'running' : 'pending', output: '' }
    })
    setStepStates(initStates)

    try {
      const res = await axios.post('/agent/workflow/start', {
        input: requirement,
        model: selectedModel,
      })
      if (res.data?.success) {
        const wf = res.data.data
        setWorkflowId(wf.id)
        message.success('工作流已启动')
        pollWorkflow(wf.id)
      } else {
        message.error(res.data?.message || '启动失败')
        setRunning(false)
        setGlobalStatus('failed')
      }
    } catch (err: any) {
      message.error('启动失败: ' + (err.response?.data?.message || err.message))
      setRunning(false)
      setGlobalStatus('failed')
    }
  }

  // 轮询工作流状态
  const pollWorkflow = useCallback(async (id: string) => {
    if (abortRef.current) return
    try {
      const res = await axios.get(`/agent/workflow/${id}`)
      if (!res.data?.success) return
      const wf = res.data.data

      // 更新步骤状态
      const newStates: Record<string, StepState> = { ...stepStates }
      let activeIdx = -1

      wf.steps.forEach((step: any, idx: number) => {
        const stepKey = WORKFLOW_STEPS[idx]?.key
        if (!stepKey) return
        let status: StepStatus = 'pending'
        if (step.status === 'COMPLETED') status = 'completed'
        else if (step.status === 'FAILED') status = 'failed'
        else if (step.status === 'RUNNING') { status = 'running'; activeIdx = idx }

        const existing = newStates[stepKey]
        newStates[stepKey] = {
          status,
          output: step.output || existing?.output || '',
          startTime: step.startTime || existing?.startTime,
          endTime: step.endTime || existing?.endTime,
        }
      })

      setStepStates(newStates)
      setCurrentStepIndex(activeIdx >= 0 ? activeIdx : wf.currentStepIndex)

      // 全局状态
      if (wf.status === 'COMPLETED') {
        setGlobalStatus('completed')
        setRunning(false)
      } else if (wf.status === 'FAILED') {
        setGlobalStatus('failed')
        setRunning(false)
      } else {
        setTimeout(() => pollWorkflow(id), 1500)
      }
    } catch {
      setTimeout(() => pollWorkflow(id), 2000)
    }
  }, [stepStates])

  // 停止工作流
  const stopWorkflow = () => {
    abortRef.current = true
    setRunning(false)
    setGlobalStatus('idle')
    message.info('已停止')
  }

  // 重置
  const resetWorkflow = () => {
    setRunning(false)
    setGlobalStatus('idle')
    setWorkflowId(null)
    setStepStates({})
    setCurrentStepIndex(-1)
    setElapsedTime(0)
    setRequirement('')
    setCodeFiles([])
    setSelectedFilePath(null)
  }

  // ── 代码文件解析 ─────────────────────────────────────────────────────────

  const parseCodeBlocks = useCallback((allOutputs: string) => {
    const files: { path: string; content: string; lang: string }[] = []
    // 匹配 markdown 代码块，支持 ```lang:path 或 ```lang 或 ``` 前面带文件名注释
    const blockRegex = /```(?:([\w+-]+))?(?::([^\n\r]*))?\n([\s\S]*?)```/g
    let match
    while ((match = blockRegex.exec(allOutputs)) !== null) {
      const lang = (match[1] || '').trim().toLowerCase()
      const pathHint = (match[2] || '').trim()
      const content = match[3].trimEnd()
      if (!content) continue
      let path = pathHint
      if (!path) {
        // 尝试从代码块前一行提取文件名，如 "// src/App.tsx" 或 "# file: src/App.tsx"
        const before = allOutputs.slice(0, match.index)
        const prevLine = before.split(/\r?\n/).filter(Boolean).pop() || ''
        const fileMatch = prevLine.match(/(?:file|path|文件名)[：:]\s*(.+)/i) || prevLine.match(/`([^`]+\.[\w]+)`/)
        if (fileMatch) path = fileMatch[1].trim()
      }
      if (!path) {
        // 从语言推断扩展名
        const extMap: Record<string, string> = {
          typescript: 'ts', tsx: 'tsx', ts: 'ts',
          javascript: 'js', jsx: 'jsx', js: 'js',
          java: 'java', python: 'py', py: 'py',
          go: 'go', rust: 'rs', csharp: 'cs', cs: 'cs',
          php: 'php', ruby: 'rb', swift: 'swift', kotlin: 'kt',
          sql: 'sql', yaml: 'yml', yml: 'yml', json: 'json',
          xml: 'xml', html: 'html', css: 'css', scss: 'scss',
          markdown: 'md', md: 'md', shell: 'sh', bash: 'sh', sh: 'sh',
          dockerfile: 'Dockerfile', docker: 'Dockerfile',
        }
        const ext = extMap[lang] || lang || 'txt'
        path = `generated/${WORKFLOW_STEPS[files.length % WORKFLOW_STEPS.length].key.toLowerCase()}/file_${files.length + 1}.${ext}`
      }
      // 去重：相同路径追加内容
      const existing = files.find(f => f.path === path)
      if (existing) {
        existing.content += '\n\n' + content
      } else {
        files.push({ path, content, lang: lang || 'text' })
      }
    }
    setCodeFiles(files)
    if (files.length > 0 && !selectedFilePath) {
      setSelectedFilePath(files[0].path)
    }
  }, [selectedFilePath])

  // 从步骤输出收集全部文本
  const collectAllOutputs = useCallback(() => {
    let text = ''
    WORKFLOW_STEPS.forEach(step => {
      const s = stepStates[step.key]
      if (s?.output) text += '\n\n' + s.output
    })
    return text
  }, [stepStates])

  // 工作流完成后自动解析
  useEffect(() => {
    if (globalStatus === 'completed' || globalStatus === 'failed') {
      const text = collectAllOutputs()
      if (text.trim()) parseCodeBlocks(text)
    }
  }, [globalStatus, stepStates, collectAllOutputs, parseCodeBlocks])

  // ── 下载功能 ─────────────────────────────────────────────────────────────

  const downloadSingleFile = (file: { path: string; content: string }) => {
    const blob = new Blob([file.content], { type: 'text/plain;charset=utf-8' })
    saveAs(blob, file.path.split('/').pop() || 'file.txt')
  }

  const downloadAllAsZip = async () => {
    if (codeFiles.length === 0) { message.warning('没有可下载的文件'); return }
    const zip = new JSZip()
    codeFiles.forEach(file => {
      zip.file(file.path, file.content)
    })
    const blob = await zip.generateAsync({ type: 'blob' })
    saveAs(blob, `ai-generated-${Date.now()}.zip`)
    message.success(`已打包 ${codeFiles.length} 个文件`)
  }

  // 构建文件树
  const buildFileTree = (files: { path: string }[]) => {
    const root: Record<string, any> = {}
    files.forEach(f => {
      const parts = f.path.split('/')
      let node = root
      parts.forEach((part, i) => {
        if (i === parts.length - 1) {
          node[part] = { __file: true, path: f.path }
        } else {
          if (!node[part]) node[part] = {}
          node = node[part]
        }
      })
    })
    return root
  }

  // 步骤状态图标
  const StepStatusIcon = ({ status }: { status: StepStatus }) => {
    switch (status) {
      case 'completed': return <CheckCircleOutlined style={{ color: '#52c41a', fontSize: 18 }} />
      case 'running': return <LoadingOutlined style={{ color: '#1677ff', fontSize: 18 }} spin />
      case 'failed': return <CloseCircleOutlined style={{ color: '#ff4d4f', fontSize: 18 }} />
      default: return <ClockCircleOutlined style={{ color: '#d9d9d9', fontSize: 18 }} />
    }
  }

  // 模型标签渲染
  const modelLabel = (value: string) => {
    const m = MODEL_OPTIONS.find(o => o.value === value)
    if (!m) return value
    return (
      <Space>
        <span>{m.icon}</span>
        <span>{m.label}</span>
        <Tag color="default" style={{ fontSize: 11, margin: 0 }}>{m.providerLabel}</Tag>
      </Space>
    )
  }

  const completedCount = Object.values(stepStates).filter(s => s.status === 'completed').length

  return (
    <div style={{ minHeight: '100%', background: '#faf8f5', padding: '24px 32px 32px' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 24 }}>
        <div>
          <div style={{ fontSize: 11, fontWeight: 600, color: '#8c8c8c', textTransform: 'uppercase', letterSpacing: 1.5, marginBottom: 8 }}>AI Studio</div>
          <h1 style={{ margin: 0, fontSize: 28, fontWeight: 700, color: '#1a1a2e' }}>全AI智能开发</h1>
          <p style={{ color: '#8c8c8c', margin: '6px 0 0', fontSize: 14 }}>一站式AI驱动软件开发：需求分析 → 领域建模 → 架构设计 → 代码生成 → 代码审查 → 测试生成</p>
        </div>
        {globalStatus === 'completed' && (
          <Button icon={<ReloadOutlined />} size="large" style={{ borderRadius: 8 }} onClick={resetWorkflow}>
            重新开始
          </Button>
        )}
      </div>

      {/* 配置栏 */}
      <Card style={{ borderRadius: 16, marginBottom: 24, border: '1px solid #f0f0f0' }}>
        <div style={{ display: 'flex', gap: 20, alignItems: 'flex-end', flexWrap: 'wrap' }}>
          {/* 模型选择 */}
          <div style={{ flex: 1, minWidth: 300 }}>
            <div style={{ fontSize: 12, fontWeight: 600, color: '#595959', marginBottom: 8, display: 'flex', alignItems: 'center', gap: 6 }}>
              <RobotOutlined /> 选择大模型
            </div>
            <Select
              value={selectedModel}
              onChange={setSelectedModel}
              style={{ width: '100%' }}
              size="large"
              optionLabelProp="value"
              dropdownStyle={{ borderRadius: 12 }}
              disabled={running}
            >
              {/* DeepSeek 分组 */}
              <Select.OptGroup label={<span style={{ color: '#722ed1', fontWeight: 600 }}>🔭 DeepSeek</span>}>
                {MODEL_OPTIONS.filter(m => m.provider === 'deepseek').map(m => (
                  <Select.Option key={m.value} value={m.value}>
                    <Space>
                      <span style={{ fontSize: 16 }}>{m.icon}</span>
                      <span style={{ fontWeight: 500 }}>{m.label}</span>
                      <Text style={{ fontSize: 12, color: '#8c8c8c' }}>{m.desc}</Text>
                    </Space>
                  </Select.Option>
                ))}
              </Select.OptGroup>
              {/* 智谱分组 */}
              <Select.OptGroup label={<span style={{ color: '#1677ff', fontWeight: 600 }}>🐉 智谱AI (GLM)</span>}>
                {MODEL_OPTIONS.filter(m => m.provider === 'zhipu').map(m => (
                  <Select.Option key={m.value} value={m.value}>
                    <Space>
                      <span style={{ fontSize: 16 }}>{m.icon}</span>
                      <span style={{ fontWeight: 500 }}>{m.label}</span>
                      <Text style={{ fontSize: 12, color: '#8c8c8c' }}>{m.desc}</Text>
                    </Space>
                  </Select.Option>
                ))}
              </Select.OptGroup>
              {/* 通义千问分组 */}
              <Select.OptGroup label={<span style={{ color: '#f97316', fontWeight: 600 }}>☁️ 阿里通义千问</span>}>
                {MODEL_OPTIONS.filter(m => m.provider === 'aliyun').map(m => (
                  <Select.Option key={m.value} value={m.value}>
                    <Space>
                      <span style={{ fontSize: 16 }}>{m.icon}</span>
                      <span style={{ fontWeight: 500 }}>{m.label}</span>
                      <Text style={{ fontSize: 12, color: '#8c8c8c' }}>{m.desc}</Text>
                    </Space>
                  </Select.Option>
                ))}
              </Select.OptGroup>
            </Select>
          </div>

          {/* 当前模型信息 */}
          <div style={{ minWidth: 200 }}>
            <div style={{ fontSize: 12, fontWeight: 600, color: '#595959', marginBottom: 8 }}>当前模型</div>
            <div style={{ padding: '8px 12px', background: '#f5f5f5', borderRadius: 10, display: 'flex', alignItems: 'center', gap: 8 }}>
              {modelLabel(selectedModel)}
            </div>
          </div>
        </div>

        <Divider style={{ margin: '16px 0' }} />

        {/* 需求输入 */}
        <div style={{ marginBottom: 16 }}>
          <div style={{ fontSize: 12, fontWeight: 600, color: '#595959', marginBottom: 8, display: 'flex', alignItems: 'center', gap: 6 }}>
            <FileTextOutlined /> 需求描述
          </div>
          <TextArea
            rows={3}
            value={requirement}
            onChange={e => setRequirement(e.target.value)}
            placeholder="请描述您的软件开发需求，例如：开发一个图书管理系统，包含图书的增删改查、借阅管理、用户权限控制等功能..."
            style={{ borderRadius: 10, fontSize: 14 }}
            disabled={running}
          />
        </div>

        {/* 操作按钮 */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Space>
            {running ? (
              <Button type="primary" danger icon={<PauseCircleOutlined />} size="large" style={{ borderRadius: 8 }} onClick={stopWorkflow}>
                停止
              </Button>
            ) : (
              <Button type="primary" icon={<PlayCircleOutlined />} size="large" style={{ borderRadius: 8 }} onClick={startWorkflow} disabled={!requirement.trim()}>
                启动智能开发
              </Button>
            )}
            {!running && globalStatus !== 'idle' && (
              <Button icon={<ReloadOutlined />} size="large" style={{ borderRadius: 8 }} onClick={resetWorkflow}>
                重置
              </Button>
            )}
          </Space>

          {running && (
            <Space>
              <Badge status="processing" text="运行中" />
              <Text style={{ fontSize: 14, color: '#8c8c8c', fontFamily: 'monospace' }}>{fmtTime(elapsedTime)}</Text>
            </Space>
          )}
          {globalStatus === 'completed' && (
            <Tag color="success" style={{ fontSize: 13 }}>已完成 {completedCount}/{WORKFLOW_STEPS.length} 步</Tag>
          )}
          {globalStatus === 'failed' && (
            <Tag color="error" style={{ fontSize: 13 }}>执行失败</Tag>
          )}
        </div>
      </Card>

      {/* 步骤流程 */}
      <div style={{ marginBottom: 24 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 16 }}>
          <ThunderboltOutlined style={{ color: '#d46b08', fontSize: 16 }} />
          <Text style={{ fontSize: 14, fontWeight: 600, color: '#262626' }}>开发流程</Text>
          {workflowId && (
            <Text style={{ fontSize: 12, color: '#8c8c8c' }}>工作流 ID: {workflowId}</Text>
          )}
        </div>

        {/* 步骤卡片行 */}
        <div style={{ display: 'flex', gap: 12, overflowX: 'auto', paddingBottom: 8 }}>
          {WORKFLOW_STEPS.map((step, idx) => {
            const state = stepStates[step.key]
            const status = state?.status || 'pending'
            const isActive = status === 'running'
            return (
              <div key={step.key} style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                <div
                  style={{
                    minWidth: 140, padding: '16px 14px', borderRadius: 14,
                    background: isActive ? step.bgColor : status === 'completed' ? '#f6ffed' : '#fff',
                    border: `2px solid ${isActive ? step.color : status === 'completed' ? '#b7eb8f' : '#f0f0f0'}`,
                    transition: 'all 0.3s ease',
                    position: 'relative',
                    opacity: status === 'pending' && !isActive ? 0.7 : 1,
                  }}
                >
                  {/* 顶部图标行 */}
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 }}>
                    <div style={{
                      width: 36, height: 36, borderRadius: 10,
                      background: isActive ? step.color : status === 'completed' ? '#52c41a' : '#f5f5f5',
                      color: isActive || status === 'completed' ? '#fff' : '#bfbfbf',
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                      fontSize: 16,
                    }}>
                      {step.icon}
                    </div>
                    <StepStatusIcon status={status} />
                  </div>
                  {/* 名称 */}
                  <div style={{ fontSize: 14, fontWeight: 600, color: '#1a1a2e', marginBottom: 4 }}>
                    {step.name}
                  </div>
                  {/* 描述 */}
                  <div style={{ fontSize: 11, color: '#8c8c8c', lineHeight: 1.4 }}>
                    {step.description}
                  </div>
                  {/* 序号 */}
                  <div style={{
                    position: 'absolute', top: -8, left: 12,
                    background: isActive ? step.color : '#f0f0f0',
                    color: isActive ? '#fff' : '#8c8c8c',
                    fontSize: 10, fontWeight: 700, padding: '2px 8px',
                    borderRadius: 10,
                  }}>
                    Step {idx + 1}
                  </div>
                </div>
                {/* 箭头 */}
                {idx < WORKFLOW_STEPS.length - 1 && (
                  <ArrowRightOutlined style={{ color: '#d9d9d9', fontSize: 16, flexShrink: 0 }} />
                )}
              </div>
            )
          })}
        </div>
      </div>

      {/* 输出区域 */}
      <div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 16 }}>
          <FileTextOutlined style={{ color: '#1677ff', fontSize: 16 }} />
          <Text style={{ fontSize: 14, fontWeight: 600, color: '#262626' }}>执行输出</Text>
        </div>

        {Object.keys(stepStates).length === 0 ? (
          <Empty description="请输入需求并启动工作流" style={{ padding: 60, background: '#fff', borderRadius: 16 }} />
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            {WORKFLOW_STEPS.map((step) => {
              const state = stepStates[step.key]
              if (!state || (state.status === 'pending' && !state.output)) return null
              return (
                <div
                  key={step.key}
                  style={{
                    background: '#fff', borderRadius: 16, border: '1px solid #f0f0f0',
                    overflow: 'hidden',
                  }}
                >
                  {/* 输出头部 */}
                  <div style={{
                    padding: '14px 20px', background: step.bgColor,
                    borderBottom: `1px solid ${step.color}20`,
                    display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                  }}>
                    <Space>
                      <span style={{ color: step.color, fontSize: 16 }}>{step.icon}</span>
                      <Text style={{ fontWeight: 600, fontSize: 14, color: '#262626' }}>{step.name} 输出</Text>
                      {state.status === 'running' && <Badge status="processing" text="生成中..." />}
                      {state.status === 'completed' && <Tag color="success" style={{ fontSize: 11, margin: 0 }}>已完成</Tag>}
                      {state.status === 'failed' && <Tag color="error" style={{ fontSize: 11, margin: 0 }}>失败</Tag>}
                    </Space>
                    <Space>
                      {state.startTime && (
                        <Text style={{ fontSize: 12, color: '#8c8c8c' }}>
                          {new Date(state.startTime).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })}
                        </Text>
                      )}
                    </Space>
                  </div>
                  {/* 输出内容 */}
                  <div style={{ padding: '16px 20px', maxHeight: 400, overflow: 'auto' }}>
                    {state.status === 'running' && !state.output ? (
                      <div style={{ textAlign: 'center', padding: 20 }}>
                        <Spin size="small" /><span style={{ marginLeft: 8, color: '#8c8c8c' }}>正在生成...</span>
                      </div>
                    ) : state.output ? (
                      <pre style={{
                        margin: 0, fontSize: 13, lineHeight: 1.7,
                        whiteSpace: 'pre-wrap', wordBreak: 'break-word',
                        fontFamily: "'Segoe UI', system-ui, sans-serif",
                        color: '#262626',
                      }}>
                        {state.output}
                      </pre>
                    ) : (
                      <Text style={{ color: '#bfbfbf', fontSize: 13 }}>暂无输出</Text>
                    )}
                  </div>
                </div>
              )
            })}
          </div>
        )}
      </div>

      {/* 代码文件导出区域 */}
      {codeFiles.length > 0 && (
        <div style={{ marginTop: 32 }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <FolderOpenOutlined style={{ color: '#f97316', fontSize: 16 }} />
              <Text style={{ fontSize: 14, fontWeight: 600, color: '#262626' }}>生成文件</Text>
              <Tag color="orange" style={{ margin: 0 }}>{codeFiles.length} 个文件</Tag>
            </div>
            <Space>
              <Button icon={<FileZipOutlined />} onClick={downloadAllAsZip}>
                打包下载 (.zip)
              </Button>
            </Space>
          </div>

          <Card style={{ borderRadius: 16, border: '1px solid #f0f0f0', overflow: 'hidden', padding: 0 }} bodyStyle={{ padding: 0 }}>
            <div style={{ display: 'flex', height: 520 }}>
              {/* 左侧文件树 */}
              <div style={{ width: 260, borderRight: '1px solid #f0f0f0', background: '#fafafa', overflow: 'auto' }}>
                <div style={{ padding: '12px 16px', fontSize: 12, fontWeight: 600, color: '#8c8c8c', display: 'flex', alignItems: 'center', gap: 6, cursor: 'pointer' }}
                  onClick={() => setFileTreeExpanded(!fileTreeExpanded)}>
                  {fileTreeExpanded ? <FolderOpenOutlined /> : <FolderOutlined />}
                  文件目录
                </div>
                {fileTreeExpanded && (
                  <div style={{ padding: '0 8px 12px' }}>
                    {(() => {
                      const tree = buildFileTree(codeFiles)
                      const renderTree = (node: Record<string, any>, level = 0) => {
                        const entries = Object.entries(node).sort((a, b) => {
                          const aIsFile = a[1].__file
                          const bIsFile = b[1].__file
                          if (aIsFile && !bIsFile) return 1
                          if (!aIsFile && bIsFile) return -1
                          return a[0].localeCompare(b[0])
                        })
                        return entries.map(([name, child]) => {
                          const isFile = child.__file
                          const path = isFile ? child.path : null
                          const isSelected = path === selectedFilePath
                          if (isFile) {
                            return (
                              <div
                                key={path}
                                onClick={() => setSelectedFilePath(path!)}
                                className="codeflow-file-row"
                                style={{
                                  padding: '6px 8px 6px ' + (12 + level * 16) + 'px',
                                  cursor: 'pointer', borderRadius: 6, fontSize: 13,
                                  display: 'flex', alignItems: 'center', gap: 6,
                                  background: isSelected ? '#e6f4ff' : 'transparent',
                                  color: isSelected ? '#1677ff' : '#262626',
                                  fontWeight: isSelected ? 500 : 400,
                                  transition: 'background 0.2s',
                                }}
                              >
                                <FileOutlined style={{ fontSize: 12, color: isSelected ? '#1677ff' : '#8c8c8c', flexShrink: 0 }} />
                                <span style={{ flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{name}</span>
                                <DownloadOutlined style={{ fontSize: 12, color: '#bfbfbf', opacity: 0, transition: 'opacity 0.2s' }}
                                  onClick={(e) => { e.stopPropagation(); const f = codeFiles.find(x => x.path === path); if (f) downloadSingleFile(f) }}
                                  className="codeflow-file-download"
                                />
                              </div>
                            )
                          }
                          return (
                            <div key={name}>
                              <div style={{ padding: '4px 8px 4px ' + (12 + level * 16) + 'px', fontSize: 13, color: '#8c8c8c', display: 'flex', alignItems: 'center', gap: 6, fontWeight: 500 }}>
                                <FolderOutlined style={{ fontSize: 12 }} />
                                {name}
                              </div>
                              {renderTree(child, level + 1)}
                            </div>
                          )
                        })
                      }
                      return renderTree(tree)
                    })()}
                  </div>
                )}
              </div>

              {/* 右侧代码预览 */}
              <div style={{ flex: 1, display: 'flex', flexDirection: 'column', background: '#fff' }}>
                {selectedFilePath ? (
                  <>
                    <div style={{ padding: '12px 20px', borderBottom: '1px solid #f0f0f0', display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: '#fafafa' }}>
                      <Space>
                        <FileOutlined style={{ color: '#1677ff' }} />
                        <Text style={{ fontWeight: 500, fontSize: 13 }}>{selectedFilePath}</Text>
                        <Tag style={{ fontSize: 11, margin: 0 }}>{codeFiles.find(f => f.path === selectedFilePath)?.lang || 'text'}</Tag>
                      </Space>
                      <Button size="small" icon={<DownloadOutlined />} onClick={() => {
                        const f = codeFiles.find(x => x.path === selectedFilePath)
                        if (f) downloadSingleFile(f)
                      }}>
                        下载
                      </Button>
                    </div>
                    <div style={{ flex: 1, overflow: 'auto', padding: 16 }}>
                      <pre style={{
                        margin: 0, fontSize: 13, lineHeight: 1.7,
                        whiteSpace: 'pre', fontFamily: "'JetBrains Mono', 'Fira Code', 'SF Mono', Consolas, monospace",
                        color: '#262626',
                      }}>
                        {codeFiles.find(f => f.path === selectedFilePath)?.content || ''}
                      </pre>
                    </div>
                  </>
                ) : (
                  <Empty description="选择左侧文件预览" style={{ margin: 'auto' }} />
                )}
              </div>
            </div>
          </Card>
        </div>
      )}
    </div>
  )
}

export default CodeFlowPage
