import { useState, useRef, useEffect, useCallback } from 'react'
import {
  Button, Input, Select, message, Spin, Tag, Space, Empty,
  Typography, Badge, Card, Divider, Upload, Tooltip,
} from 'antd'
import {
  ThunderboltOutlined, PlayCircleOutlined, PauseCircleOutlined,
  ReloadOutlined, CheckCircleOutlined, ClockCircleOutlined,
  LoadingOutlined, CloseCircleOutlined, RobotOutlined,
  FileTextOutlined, BranchesOutlined, ApartmentOutlined,
  CodeOutlined, SafetyOutlined, ExperimentOutlined,
  ArrowRightOutlined, FolderOutlined, FileOutlined,
  DownloadOutlined, FolderOpenOutlined, FileZipOutlined,
  InboxOutlined, PaperClipOutlined, DeleteOutlined,
} from '@ant-design/icons'
import axios from '@/services/api'
import JSZip from 'jszip'
import { saveAs } from 'file-saver'
import { getLlmProviders } from '@/services/settings'

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

// ── 完整厂商内置模型列表（含全部10个厂商） ─────────────────────────────────
const BUILTIN_VENDOR_MODELS: {
  provider: string; providerLabel: string; icon: string; color: string
  models: { value: string; label: string; desc: string }[]
}[] = [
  {
    provider: 'deepseek', providerLabel: 'DeepSeek', icon: '🔭', color: '#722ed1',
    models: [
      { value: 'deepseek-v4-pro', label: 'DeepSeek V4 Pro', desc: '深度思考 · 最强推理' },
      { value: 'deepseek-v4-flash', label: 'DeepSeek V4 Flash', desc: '快速响应 · 高效生成' },
      { value: 'deepseek-chat', label: 'DeepSeek Chat', desc: '通用对话 · 均衡性能' },
      { value: 'deepseek-reasoner', label: 'DeepSeek Reasoner', desc: '推理增强 · 逻辑严谨' },
    ],
  },
  {
    provider: 'zhipu', providerLabel: '智谱AI (GLM)', icon: '🐉', color: '#1677ff',
    models: [
      { value: 'glm-5.1', label: 'GLM 5.1', desc: '最新旗舰 · 全能模型' },
      { value: 'glm-5', label: 'GLM 5', desc: '旗舰模型 · 全面领先' },
      { value: 'glm-5-turbo', label: 'GLM 5 Turbo', desc: '极速响应 · 高性价比' },
      { value: 'glm-4.7', label: 'GLM 4.7', desc: '高性能 · 长上下文' },
      { value: 'glm-4.6', label: 'GLM 4.6', desc: '均衡性能 · 稳定可靠' },
      { value: 'glm-4.5', label: 'GLM 4.5', desc: '高效推理 · 多模态' },
      { value: 'glm-4.5-air', label: 'GLM 4.5 Air', desc: '轻量快速 · 低成本' },
    ],
  },
  {
    provider: 'aliyun', providerLabel: '阿里通义千问', icon: '☁️', color: '#f97316',
    models: [
      { value: 'qwen3-max', label: 'Qwen3 Max', desc: 'Qwen3旗舰 · 最新最强' },
      { value: 'qwen3-coder-plus', label: 'Qwen3 Coder Plus', desc: 'Qwen3编程增强 · 代码首选' },
      { value: 'qwen3-coder-flash', label: 'Qwen3 Coder Flash', desc: 'Qwen3编程快速 · 高效' },
      { value: 'qwq-plus', label: 'QwQ Plus', desc: '深度推理 · 类似R1' },
      { value: 'qwen-max', label: 'Qwen Max', desc: '旗舰模型 · 最强性能' },
      { value: 'qwen-plus', label: 'Qwen Plus', desc: '高性能 · 复杂任务' },
      { value: 'qwen-turbo', label: 'Qwen Turbo', desc: '快速响应 · 日常任务' },
      { value: 'qwen-flash', label: 'Qwen Flash', desc: '极速响应 · 超低成本' },
      { value: 'qwen-coder-plus', label: 'Qwen Coder Plus', desc: '代码专家 · 编程首选' },
      { value: 'qwen-long', label: 'Qwen Long', desc: '超长上下文 · 文档分析' },
      { value: 'qwen-math-plus', label: 'Qwen Math Plus', desc: '数学推理 · 逻辑专精' },
    ],
  },
  {
    provider: 'openai', providerLabel: 'OpenAI', icon: '🤖', color: '#10a37f',
    models: [
      { value: 'gpt-4o', label: 'GPT-4o', desc: '最强旗舰 · 多模态' },
      { value: 'gpt-4o-mini', label: 'GPT-4o Mini', desc: '高性价比 · 快速响应' },
      { value: 'gpt-4-turbo', label: 'GPT-4 Turbo', desc: '强大推理 · 长上下文' },
      { value: 'gpt-4', label: 'GPT-4', desc: '经典旗舰 · 强大推理' },
      { value: 'gpt-3.5-turbo', label: 'GPT-3.5 Turbo', desc: '高速响应 · 日常任务' },
      { value: 'o1', label: 'O1', desc: '深度推理 · 复杂问题' },
      { value: 'o1-mini', label: 'O1 Mini', desc: '快速推理 · 轻量版' },
    ],
  },
  {
    provider: 'anthropic', providerLabel: 'Anthropic Claude', icon: '🧡', color: '#d97706',
    models: [
      { value: 'claude-opus-4-7', label: 'Claude Opus 4.7', desc: '顶级旗舰 · 极强推理' },
      { value: 'claude-sonnet-4-6', label: 'Claude Sonnet 4.6', desc: '均衡性能 · 代码优化' },
      { value: 'claude-opus-4-6', label: 'Claude Opus 4.6', desc: '强大推理 · 长文本' },
      { value: 'claude-3-5-sonnet-20241022', label: 'Claude 3.5 Sonnet', desc: '最强Claude · 全能' },
      { value: 'claude-3-haiku-20240307', label: 'Claude 3 Haiku', desc: '高速轻量 · 低成本' },
    ],
  },
  {
    provider: 'moonshot', providerLabel: 'Kimi (月之暗面)', icon: '🌙', color: '#17aeae',
    models: [
      { value: 'moonshot-v1-128k', label: 'Moonshot 128K', desc: '超长上下文 · 128K' },
      { value: 'moonshot-v1-32k', label: 'Moonshot 32K', desc: '长上下文 · 32K' },
      { value: 'moonshot-v1-8k', label: 'Moonshot 8K', desc: '标准模型 · 快速' },
    ],
  },
  {
    provider: 'doubao', providerLabel: '字节豆包', icon: '🫘', color: '#3b82f6',
    models: [
      { value: 'doubao-pro-32k', label: 'Doubao Pro 32K', desc: '旗舰专业 · 32K' },
      { value: 'doubao-pro-4k', label: 'Doubao Pro 4K', desc: '旗舰专业 · 标准' },
      { value: 'doubao-lite-32k', label: 'Doubao Lite 32K', desc: '轻量快速 · 低成本' },
    ],
  },
  {
    provider: 'baidu', providerLabel: '百度文心一言', icon: '🌊', color: '#2563eb',
    models: [
      { value: 'ernie-4.0-8k', label: 'ERNIE 4.0 8K', desc: '文心旗舰 · 中文优化' },
      { value: 'ernie-4.0-turbo-8k', label: 'ERNIE 4.0 Turbo', desc: '高速版 · 快速响应' },
      { value: 'ernie-bot', label: 'ERNIE Bot', desc: '经典文心 · 稳定可靠' },
    ],
  },
  {
    provider: 'ollama', providerLabel: 'Ollama (本地)', icon: '💻', color: '#52c41a',
    models: [
      { value: 'llama3', label: 'Llama 3', desc: '本地开源 · 强大通用' },
      { value: 'llama3.1', label: 'Llama 3.1', desc: '本地最新 · 增强版' },
      { value: 'mistral', label: 'Mistral', desc: '本地高效 · 欧洲优化' },
      { value: 'codellama', label: 'Code Llama', desc: '代码专用 · 本地运行' },
      { value: 'qwen2', label: 'Qwen2', desc: '本地千问 · 中英双语' },
      { value: 'gemma2', label: 'Gemma2', desc: 'Google开源 · 本地高效' },
    ],
  },
  {
    provider: 'codeflow', providerLabel: 'CodeFlow / 灵龙AI', icon: '🚀', color: '#8b5cf6',
    models: [
      { value: 'gpt-5.5', label: 'GPT-5.5', desc: '最新旗舰 · 超强推理' },
      { value: 'gpt-5.5-openai-compact', label: 'GPT-5.5 Compact', desc: '旗舰轻量 · 快速响应' },
      { value: 'gpt-5.4', label: 'GPT-5.4', desc: '旗舰模型 · 全能开发' },
      { value: 'gpt-5.4-mini', label: 'GPT-5.4 Mini', desc: '轻量版 · 快速响应' },
      { value: 'gpt-5.4-openai-compact', label: 'GPT-5.4 Compact', desc: '均衡版 · 高性价比' },
      { value: 'gpt-5.3-codex', label: 'GPT-5.3 Codex', desc: '代码专精 · 编程首选' },
      { value: 'gpt-5.3-codex-openai-compact', label: 'GPT-5.3 Codex Compact', desc: '代码轻量 · 高效编程' },
      { value: 'gpt-5.3-codex-spark', label: 'GPT-5.3 Codex Spark', desc: '代码极速 · 辅助补全' },
      { value: 'gpt-5.2', label: 'GPT-5.2', desc: '均衡版 · 稳定可靠' },
      { value: 'gpt-5.2-openai-compact', label: 'GPT-5.2 Compact', desc: '轻量稳定 · 低成本' },
      { value: 'claude-opus-4-7', label: 'Claude Opus 4.7', desc: '顶级推理 · 代码优化' },
      { value: 'claude-sonnet-4-6', label: 'Claude Sonnet 4.6', desc: '均衡高效 · 企业级' },
      { value: 'claude-opus-4-6', label: 'Claude Opus 4.6', desc: '深度思考 · 复杂任务' },
      { value: 'claude-haiku-4-5-20251001', label: 'Claude Haiku 4.5', desc: '极速轻量 · 低成本' },
    ],
  },
]

// 兼容性：扁平化为旧格式（用于当前模型信息展示）
const MODEL_OPTIONS: ModelOption[] = BUILTIN_VENDOR_MODELS.flatMap(g =>
  g.models.map(m => ({
    value: m.value,
    label: m.label,
    provider: g.provider,
    providerLabel: g.providerLabel,
    icon: g.icon,
    desc: m.desc,
  }))
)

// ── 状态 ──────────────────────────────────────────────────────────────────

type StepStatus = 'pending' | 'running' | 'completed' | 'failed'

interface StepState {
  status: StepStatus
  output: string
  startTime?: string
  endTime?: string
}

interface UploadedFile {
  uid: string
  name: string
  content: string
  size: number
  fileType: string
}

// ── 组件 ──────────────────────────────────────────────────────────────────

function CodeFlowPage() {
  const [requirement, setRequirement] = useState('')
  const [selectedModel, setSelectedModel] = useState('gpt-5.5')
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
  // 参考文件上传状态
  const [uploadedFiles, setUploadedFiles] = useState<UploadedFile[]>([])
  // 动态模型供应商（从系统已配置的 LLM 供应商加载）
  const [dynamicProviders, setDynamicProviders] = useState<any[]>([])

  // 计时器
  useEffect(() => {
    if (running && globalStatus === 'running') {
      timerRef.current = setInterval(() => setElapsedTime(t => t + 1), 1000)
    } else {
      if (timerRef.current) clearInterval(timerRef.current)
    }
    return () => { if (timerRef.current) clearInterval(timerRef.current) }
  }, [running, globalStatus])

  // 加载系统已配置的 LLM 供应商（优先展示用户配置的模型）
  useEffect(() => {
    getLlmProviders().then((providers: any[]) => {
      if (providers && providers.length > 0) {
        setDynamicProviders(providers.filter((p: any) => p.enabled !== false))
      }
    }).catch(() => {})
  }, [])

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
      // 将上传的参考文件内容拼接到需求描述后发送
      let fullInput = requirement
      if (uploadedFiles.length > 0) {
        const fileContents = uploadedFiles.map(f =>
          `\n\n--- 参考文件：${f.name} ---\n${f.content}`
        ).join('')
        fullInput += fileContents
      }
      const res = await axios.post('/agent/workflow/start', {
        input: fullInput,
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
    setUploadedFiles([])
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
              showSearch
              optionFilterProp="label"
              dropdownStyle={{ borderRadius: 12 }}
              disabled={running}
              placeholder="请选择大模型"
            >
              {/* 始终展示全部内置模型 + 动态供应商额外自定义模型 */}
              {(() => {
                // 收集已配置供应商的 vendor 标识
                const configuredVendors = new Set(dynamicProviders.map((p: any) => p.vendor))
                // 收集已配置供应商中不在内置列表中的自定义模型
                const extraModels: { provider: any; models: string[] }[] = []
                dynamicProviders.forEach((p: any) => {
                  const builtinGroup = BUILTIN_VENDOR_MODELS.find(g => g.provider === p.vendor)
                  const builtinModelValues = builtinGroup ? builtinGroup.models.map(m => m.value) : []
                  const allProviderModels: string[] = []
                  if (p.defaultModel) allProviderModels.push(p.defaultModel)
                  const extras = Array.isArray(p.customModels)
                    ? p.customModels
                    : (typeof p.customModels === 'string' && p.customModels
                        ? p.customModels.split(',').map((s: string) => s.trim()).filter(Boolean)
                        : [])
                  extras.forEach((m: string) => { if (!allProviderModels.includes(m)) allProviderModels.push(m) })
                  const notInBuiltin = allProviderModels.filter(m => !builtinModelValues.includes(m))
                  if (notInBuiltin.length > 0) {
                    extraModels.push({ provider: p, models: notInBuiltin })
                  }
                })
                return (
                  <>
                    {/* 动态供应商的额外自定义模型（不在内置列表中的） */}
                    {extraModels.map(({ provider: p, models }) => {
                      const vendorIcon: Record<string, string> = {
                        deepseek: '🔭', zhipu: '🐉', aliyun: '☁️', openai: '🤖',
                        anthropic: '🧡', moonshot: '🌙', doubao: '🫘', baidu: '🌊',
                        ollama: '💻', codeflow: '🚀', custom: '⚙️',
                      }
                      const vendorColor: Record<string, string> = {
                        deepseek: '#722ed1', zhipu: '#1677ff', aliyun: '#f97316', openai: '#10a37f',
                        anthropic: '#d97706', moonshot: '#17aeae', doubao: '#3b82f6', baidu: '#2563eb',
                        ollama: '#52c41a', codeflow: '#8b5cf6', custom: '#8c8c8c',
                      }
                      const icon = vendorIcon[p.vendor] || '🤖'
                      const color = vendorColor[p.vendor] || '#8c8c8c'
                      return (
                        <Select.OptGroup key={`extra-${p.id}`} label={
                          <span style={{ color, fontWeight: 600 }}>{icon} {p.name}（自定义）</span>
                        }>
                          {models.map(mv => (
                            <Select.Option key={`extra-${p.id}-${mv}`} value={mv} label={mv}>
                              <Space>
                                <span style={{ fontSize: 15 }}>{icon}</span>
                                <span style={{ fontWeight: 500 }}>{mv}</span>
                                <Tag color="purple" style={{ fontSize: 10, margin: 0, padding: '0 4px' }}>自定义</Tag>
                              </Space>
                            </Select.Option>
                          ))}
                        </Select.OptGroup>
                      )
                    })}
                    {/* 内置全量模型列表（所有10个厂商） */}
                    {BUILTIN_VENDOR_MODELS.map(g => {
                      const isConfigured = configuredVendors.has(g.provider)
                      return (
                        <Select.OptGroup key={g.provider} label={
                          <span style={{ color: isConfigured ? g.color : '#bfbfbf', fontWeight: 600 }}>
                            {g.icon} {g.providerLabel}
                            {isConfigured
                              ? <Tag color="green" style={{ fontSize: 10, margin: '0 0 0 8px', padding: '0 4px' }}>可用</Tag>
                              : <Tag style={{ fontSize: 10, margin: '0 0 0 8px', padding: '0 4px', color: '#bfbfbf', borderColor: '#e8e8e8' }}>未配置</Tag>
                            }
                          </span>
                        }>
                          {g.models.map(m => (
                            <Select.Option key={m.value} value={m.value} label={m.label} disabled={!isConfigured}>
                              <Space>
                                <span style={{ fontSize: 15, opacity: isConfigured ? 1 : 0.35 }}>{g.icon}</span>
                                <span style={{ fontWeight: 500, color: isConfigured ? '#262626' : '#bfbfbf' }}>{m.label}</span>
                                <Text style={{ fontSize: 12, color: isConfigured ? '#8c8c8c' : '#d9d9d9' }}>{m.desc}</Text>
                                {!isConfigured && (
                                  <Tag style={{ fontSize: 9, margin: 0, padding: '0 3px', color: '#bfbfbf', borderColor: '#f0f0f0', background: '#fafafa' }}>未配置</Tag>
                                )}
                              </Space>
                            </Select.Option>
                          ))}
                        </Select.OptGroup>
                      )
                    })}
                  </>
                )
              })()}
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

          {/* 参考文件上传区域 */}
          <div style={{ marginTop: 14 }}>
            <div style={{ fontSize: 12, fontWeight: 600, color: '#595959', marginBottom: 8, display: 'flex', alignItems: 'center', gap: 8 }}>
              <PaperClipOutlined style={{ color: '#1677ff' }} />
              <span>参考文件（可选）</span>
              <span style={{ fontSize: 11, color: '#8c8c8c', fontWeight: 400 }}>支持上传需求文档、设计文档、接口文档等作为开发参考</span>
            </div>
            <Upload.Dragger
              multiple
              accept=".txt,.md,.markdown,.json,.csv,.yaml,.yml,.xml,.html,.htm,.ts,.tsx,.js,.jsx,.py,.java,.go,.rs,.cs,.php,.rb,.sql,.sh,.bash,.conf,.ini,.toml,.properties"
              showUploadList={false}
              disabled={running}
              customRequest={() => {}}
              beforeUpload={(file) => {
                const reader = new FileReader()
                reader.onload = (e) => {
                  const content = (e.target?.result as string) || ''
                  setUploadedFiles(prev => {
                    if (prev.some(f => f.name === file.name)) {
                      message.warning(`文件 "${file.name}" 已存在`)
                      return prev
                    }
                    message.success(`已添加参考文件：${file.name}`)
                    return [...prev, {
                      uid: `${Date.now()}-${Math.random()}`,
                      name: file.name,
                      content,
                      size: file.size,
                      fileType: file.type,
                    }]
                  })
                }
                reader.onerror = () => message.error(`读取文件 "${file.name}" 失败`)
                reader.readAsText(file, 'utf-8')
                return false
              }}
              style={{ borderRadius: 8, background: '#fafbff' }}
            >
              <p style={{ margin: '8px 0 4px' }}>
                <InboxOutlined style={{ fontSize: 22, color: '#1677ff' }} />
              </p>
              <p style={{ fontSize: 13, color: '#595959', margin: 0 }}>拖拽或点击上传参考文件</p>
              <p style={{ fontSize: 11, color: '#8c8c8c', margin: '4px 0 8px' }}>
                支持 txt, md, json, csv, yaml, xml, html, ts, js, py, java, go 等文本格式，可多文件上传
              </p>
            </Upload.Dragger>
            {uploadedFiles.length > 0 && (
              <div style={{ marginTop: 10, display: 'flex', flexWrap: 'wrap', gap: 8 }}>
                {uploadedFiles.map(f => (
                  <div
                    key={f.uid}
                    style={{
                      display: 'flex', alignItems: 'center', gap: 6,
                      padding: '5px 10px', borderRadius: 8,
                      background: '#f0f5ff', border: '1px solid #adc6ff',
                      fontSize: 12, color: '#262626', maxWidth: 260,
                    }}
                  >
                    <PaperClipOutlined style={{ color: '#1677ff', flexShrink: 0 }} />
                    <Tooltip title={`${f.name}（${(f.size / 1024).toFixed(1)} KB·${f.content.length} 字符）`}>
                      <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', flex: 1 }}>
                        {f.name}
                      </span>
                    </Tooltip>
                    <DeleteOutlined
                      style={{ color: '#ff4d4f', cursor: 'pointer', flexShrink: 0 }}
                      onClick={() => setUploadedFiles(prev => prev.filter(x => x.uid !== f.uid))}
                    />
                  </div>
                ))}
              </div>
            )}
          </div>
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
