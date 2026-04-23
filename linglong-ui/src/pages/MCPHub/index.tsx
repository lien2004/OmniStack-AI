import { useState, useEffect, useCallback } from 'react'
import {
  Card, Tag, Button, Input, Tabs, Switch, Modal,
  Spin, Empty, Tooltip, Badge, Divider, Table,
  message as antdMessage, Typography, Space, Select, Alert,
  Form, InputNumber, Radio, Upload,
} from 'antd'
import {
  ToolOutlined, PlusOutlined, SearchOutlined, CodeOutlined,
  FileTextOutlined, DatabaseOutlined, CloudOutlined,
  BranchesOutlined, ApartmentOutlined, PlayCircleOutlined,
  InfoCircleOutlined, CheckCircleOutlined, CloseCircleOutlined,
  ReloadOutlined, ThunderboltOutlined, CopyOutlined,
  FolderOpenOutlined, EditOutlined, UnorderedListOutlined,
  UploadOutlined,
} from '@ant-design/icons'
import axios from 'axios'

const { TextArea } = Input
const { Text, Title, Paragraph } = Typography
const { Option } = Select
const { Password } = Input

// -------- 类型定义 --------

interface ParameterDefinition {
  name: string
  description?: string
  type: string
  required: boolean
  defaultValue?: unknown
  enumValues?: string[]
}

interface ToolDefinition {
  name: string
  description: string
  category: string
  parameters: ParameterDefinition[]
  returnType: string
  async: boolean
  metadata?: Record<string, unknown>
}

interface ToolGroup {
  groupName: string
  category: string
  description: string
  tools: ToolDefinition[]
}

// -------- 工具参数模板（每个工具方法的示例参数） --------

const TOOL_TEMPLATES: Record<string, Record<string, unknown>> = {
  // ---- DatabaseTool ----
  db_query: {
    sql: 'SELECT id, metadata->>\'question\' AS question, metadata->>\'model\' AS model FROM vector_store LIMIT 10',
  },
  db_execute: {
    sql: 'UPDATE vector_store SET metadata = metadata WHERE id IS NOT NULL',
  },
  db_list_tables: {
    schema: 'public',
  },
  db_describe_table: {
    tableName: 'vector_store',
    schema: 'public',
  },

  // ---- ReadFileTool ----
  read_file: {
    filePath: 'E:/OmniStack AI/linglong-ai-platform/linglong-mcp-server/src/main/resources/application.yml',
    maxLength: 5000,
  },
  read_files_batch: {
    filePaths: [
      'E:/OmniStack AI/linglong-ai-platform/linglong-mcp-server/src/main/resources/application.yml',
    ],
  },
  read_directory: {
    directoryPath: 'E:/OmniStack AI/linglong-ai-platform/linglong-mcp-server/src/main/resources',
    extensions: ['.yml', '.yaml', '.properties'],
  },
  file_exists: {
    filePath: 'E:/OmniStack AI/linglong-ai-platform/linglong-mcp-server/src/main/resources/application.yml',
  },

  // ---- WriteCodeTool ----
  write_code: {
    filePath: 'E:/temp/test-output.txt',
    content: 'Hello from MCP WriteCodeTool!\n',
    append: false,
  },
  write_code_batch: {
    files: [
      { filePath: 'E:/temp/file1.txt', content: 'Content 1' },
      { filePath: 'E:/temp/file2.txt', content: 'Content 2' },
    ],
  },
  create_project_structure: {
    basePath: 'E:/temp',
    projectName: 'my-demo-project',
    packageName: 'com.demo.app',
  },

  // ---- GitTool ----
  git_status: {
    directory: 'E:/OmniStack AI',
  },
  git_add: {
    directory: 'E:/OmniStack AI',
    filePattern: '.',
  },
  git_commit: {
    directory: 'E:/OmniStack AI',
    message: 'feat: implement MCP tools',
  },
  git_clone: {
    remoteUrl: 'https://github.com/octocat/Hello-World.git',
    directory: 'E:/temp/cloned-repo',
  },
  git_pull: {
    directory: 'E:/OmniStack AI',
  },
  git_push: {
    directory: 'E:/OmniStack AI',
  },
  git_branch_list: {
    directory: 'E:/OmniStack AI',
  },
  git_create_branch: {
    directory: 'E:/OmniStack AI',
    branchName: 'feature/new-feature',
  },
  git_init: {
    directory: 'E:/temp/my-repo',
  },

  // ---- DockerTool ----
  docker_ps: {},
  docker_images: {},
  docker_build: {
    dockerfilePath: 'E:/temp/Dockerfile',
    imageName: 'my-app',
    tag: 'latest',
  },
  docker_run: {
    imageName: 'nginx',
    containerName: 'my-nginx',
    ports: ['80:80'],
  },
  docker_stop: {
    containerId: 'container-id-or-name',
  },
  docker_logs: {
    containerId: 'container-id-or-name',
    tail: 100,
  },
  docker_remove_container: {
    containerId: 'container-id-or-name',
    force: false,
  },

  // ---- DiagramTool ----
  generate_mermaid_diagram: {
    mermaidCode: 'graph TD\n    A[Start] --> B[Process]\n    B --> C[End]',
    outputFormat: 'svg',
    outputPath: 'E:/temp/diagram.svg',
  },
  generate_architecture_code: {
    components: [
      { id: 'frontend', name: '前端应用', type: 'user' },
      { id: 'backend', name: '后端服务', type: 'service' },
      { id: 'database', name: '数据库', type: 'database' },
      { id: 'cache', name: '缓存', type: 'service' },
    ],
    relations: [
      { from: 'frontend', to: 'backend', label: 'HTTP请求' },
      { from: 'backend', to: 'database', label: 'SQL查询' },
      { from: 'backend', to: 'cache', label: '读写缓存' },
    ],
    title: '系统架构图',
  },
  generate_class_diagram_code: {
    classes: [
      {
        name: 'UserService',
        attributes: ['-String name', '-String email'],
        methods: ['+createUser()', '+deleteUser()'],
      },
      {
        name: 'UserRepository',
        attributes: ['-List<User> users'],
        methods: ['+findById()', '+save()'],
      },
    ],
    relations: [
      { from: 'UserService', to: 'UserRepository', type: 'dependency' },
    ],
  },
  validate_mermaid: {
    mermaidCode: 'graph TD\n    A --> B',
  },
  generate_plantuml_diagram: {
    plantUmlCode: '@startuml\nAlice -> Bob: Hello\n@enduml',
    outputPath: 'E:/temp/plantuml.svg',
  },
}

// -------- 分类配置 --------

const CATEGORY_CONFIG: Record<string, { label: string; icon: React.ReactNode; color: string; tagColor: string }> = {
  document: { label: '文档处理', icon: <FileTextOutlined />, color: '#1677ff', tagColor: 'blue' },
  code:     { label: '代码操作', icon: <CodeOutlined />,     color: '#52c41a', tagColor: 'green' },
  git:      { label: '版本控制', icon: <BranchesOutlined />, color: '#fa8c16', tagColor: 'orange' },
  docker:   { label: '容器化',   icon: <CloudOutlined />,    color: '#13c2c2', tagColor: 'cyan' },
  diagram:  { label: '图表生成', icon: <ApartmentOutlined />, color: '#722ed1', tagColor: 'purple' },
  database: { label: '数据库',   icon: <DatabaseOutlined />, color: '#eb2f96', tagColor: 'magenta' },
}

const USAGE_COUNTS: Record<string, number> = {
  ReadFileTool: 523, WriteCodeTool: 892, GitTool: 456,
  DockerTool: 234,   DiagramTool: 178,  DatabaseTool: 345,
}

// -------- 工具方法 --------

function getTemplate(toolName: string): string {
  const tpl = TOOL_TEMPLATES[toolName] ?? {}
  return JSON.stringify(tpl, null, 2)
}

function tryParseJson(str: string): { ok: true; value: unknown } | { ok: false; error: string } {
  try { return { ok: true, value: JSON.parse(str) } }
  catch (e) { return { ok: false, error: (e as Error).message } }
}

// ======== 主组件 ========

function MCPHub() {
  const [toolGroups, setToolGroups]   = useState<ToolGroup[]>([])
  const [loading, setLoading]         = useState(true)
  const [searchText, setSearchText]   = useState('')
  const [activeTab, setActiveTab]     = useState('all')
  const [enabledMap, setEnabledMap]   = useState<Record<string, boolean>>({})

  // 测试弹窗
  const [testVisible, setTestVisible]     = useState(false)
  const [testGroup, setTestGroup]         = useState<ToolGroup | null>(null)
  const [selectedTool, setSelectedTool]   = useState('')
  const [jsonParam, setJsonParam]         = useState('{}')
  const [jsonError, setJsonError]         = useState('')
  const [testResult, setTestResult]       = useState<{ success: boolean; output?: string; error?: string; meta?: unknown; svgContent?: string } | null>(null)
  const [testLoading, setTestLoading]     = useState(false)
  const [execTime, setExecTime]           = useState<number | null>(null)
  const [formMode, setFormMode]           = useState(true) // true=表单模式, false=JSON模式
  const [formValues, setFormValues]       = useState<Record<string, unknown>>({})

  // 详情弹窗
  const [detailVisible, setDetailVisible] = useState(false)
  const [detailGroup, setDetailGroup]     = useState<ToolGroup | null>(null)

  // 文件上传相关状态
  const [uploadingPath, setUploadingPath]   = useState<Record<string, boolean>>({})

  // -------- 加载工具 --------

  const loadTools = useCallback(async () => {
    setLoading(true)
    try {
      const res = await axios.get<{ code: number; data: ToolDefinition[] }>('/api/mcp/tools')
      const tools: ToolDefinition[] = res.data?.data ?? []
      const groupMap: Record<string, ToolGroup> = {}
      tools.forEach(tool => {
        const groupName = (tool.metadata?.groupName as string) || tool.name
        if (!groupMap[groupName]) {
          groupMap[groupName] = { groupName, category: tool.category, description: tool.description, tools: [] }
        }
        groupMap[groupName].tools.push(tool)
      })
      const groups = Object.values(groupMap)
      setToolGroups(groups)
      const initEnabled: Record<string, boolean> = {}
      groups.forEach(g => { initEnabled[g.groupName] = true })
      setEnabledMap(initEnabled)
    } catch {
      antdMessage.warning('MCP Server 未启动，显示示例数据')
      const fallback: ToolGroup[] = [
        { groupName: 'ReadFileTool',  category: 'document', description: '读取PDF、Docx、Markdown等文档内容', tools: [] },
        { groupName: 'WriteCodeTool', category: 'code',     description: '将生成的代码写入文件系统',         tools: [] },
        { groupName: 'GitTool',       category: 'git',      description: '执行Git操作：commit、push、pull',  tools: [] },
        { groupName: 'DockerTool',    category: 'docker',   description: 'Docker构建和运行容器',            tools: [] },
        { groupName: 'DiagramTool',   category: 'diagram',  description: '调用Mermaid生成架构图',            tools: [] },
        { groupName: 'DatabaseTool',  category: 'database', description: '执行数据库操作和查询',            tools: [] },
      ]
      setToolGroups(fallback)
      const initEnabled: Record<string, boolean> = {}
      fallback.forEach(g => { initEnabled[g.groupName] = true })
      setEnabledMap(initEnabled)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { loadTools() }, [loadTools])

  // -------- 过滤 --------

  const filtered = toolGroups.filter(g => {
    const matchSearch =
      g.groupName.toLowerCase().includes(searchText.toLowerCase()) ||
      g.description.toLowerCase().includes(searchText.toLowerCase())
    const matchCategory = activeTab === 'all' || g.category === activeTab
    return matchSearch && matchCategory
  })

  // -------- 测试工具 --------

  // 获取当前选中工具的参数定义
  const getCurrentToolParams = useCallback((): ParameterDefinition[] => {
    if (!testGroup || !selectedTool) return []
    const tool = testGroup.tools.find(t => t.name === selectedTool)
    return tool?.parameters ?? []
  }, [testGroup, selectedTool])

  // 表单值变化时同步到JSON
  const updateFormValue = (paramName: string, value: unknown) => {
    const newValues = { ...formValues, [paramName]: value }
    setFormValues(newValues)
    setJsonParam(JSON.stringify(newValues, null, 2))
    setJsonError('')
  }

  // 根据参数类型渲染表单字段
  const renderParamField = (param: ParameterDefinition) => {
    const value = formValues[param.name] ?? param.defaultValue ?? ''
    const isPath = param.name.toLowerCase().includes('path') || param.name.toLowerCase().includes('directory')
    const isUrl = param.name.toLowerCase().includes('url') || param.name.toLowerCase().includes('uri')
    const isPassword = param.name.toLowerCase().includes('password') || param.name.toLowerCase().includes('token')
    const isLongText = param.type === 'string' && (param.name.toLowerCase().includes('message') || param.name.toLowerCase().includes('content') || param.name.toLowerCase().includes('code'))

    const label = (
      <Space>
        <Text>{param.name}</Text>
        {param.required && <Tag color="red" style={{ marginLeft: 4, fontSize: 10 }}>必填</Tag>}
      </Space>
    )

    // 布尔类型
    if (param.type === 'boolean') {
      return (
        <Form.Item key={param.name} label={label} style={{ marginBottom: 12 }}>
          <Switch
            checked={value === true}
            onChange={v => updateFormValue(param.name, v)}
          />
          {param.description && <Text type="secondary" style={{ display: 'block', fontSize: 12, marginTop: 4 }}>{param.description}</Text>}
        </Form.Item>
      )
    }

    // 数字类型
    if (param.type === 'number' || param.type === 'integer') {
      return (
        <Form.Item key={param.name} label={label} style={{ marginBottom: 12 }}>
          <InputNumber
            value={value as number}
            onChange={v => updateFormValue(param.name, v)}
            style={{ width: '100%' }}
            placeholder={param.description}
          />
          {param.description && <Text type="secondary" style={{ display: 'block', fontSize: 12, marginTop: 4 }}>{param.description}</Text>}
        </Form.Item>
      )
    }

    // 数组类型（简单字符串数组）
    if (param.type === 'array') {
      const arrValue = Array.isArray(value) ? value : []
      return (
        <Form.Item key={param.name} label={label} style={{ marginBottom: 12 }}>
          <Select
            mode="tags"
            value={arrValue.map(String)}
            onChange={v => updateFormValue(param.name, v)}
            style={{ width: '100%' }}
            placeholder={param.description || '输入后按回车添加'}
          />
          {param.description && <Text type="secondary" style={{ display: 'block', fontSize: 12, marginTop: 4 }}>{param.description}</Text>}
        </Form.Item>
      )
    }

    // 枚举类型
    if (param.enumValues && param.enumValues.length > 0) {
      return (
        <Form.Item key={param.name} label={label} style={{ marginBottom: 12 }}>
          <Select
            value={String(value)}
            onChange={v => updateFormValue(param.name, v)}
            style={{ width: '100%' }}
          >
            {param.enumValues.map(opt => (
              <Option key={opt} value={opt}>{opt}</Option>
            ))}
          </Select>
          {param.description && <Text type="secondary" style={{ display: 'block', fontSize: 12, marginTop: 4 }}>{param.description}</Text>}
        </Form.Item>
      )
    }

    // 密码字段
    if (isPassword) {
      return (
        <Form.Item key={param.name} label={label} style={{ marginBottom: 12 }}>
          <Password
            value={String(value)}
            onChange={e => updateFormValue(param.name, e.target.value)}
            placeholder={param.description}
          />
          {param.description && <Text type="secondary" style={{ display: 'block', fontSize: 12, marginTop: 4 }}>{param.description}</Text>}
        </Form.Item>
      )
    }

    // 长文本字段
    if (isLongText) {
      return (
        <Form.Item key={param.name} label={label} style={{ marginBottom: 12 }}>
          <TextArea
            value={String(value)}
            onChange={e => updateFormValue(param.name, e.target.value)}
            rows={4}
            placeholder={param.description}
          />
          {param.description && <Text type="secondary" style={{ display: 'block', fontSize: 12, marginTop: 4 }}>{param.description}</Text>}
        </Form.Item>
      )
    }

    // URL字段
    if (isUrl) {
      return (
        <Form.Item key={param.name} label={label} style={{ marginBottom: 12 }}>
          <Input
            value={String(value)}
            onChange={e => updateFormValue(param.name, e.target.value)}
            placeholder={param.description || 'https://...'}
            prefix={<Text type="secondary" style={{ fontSize: 12 }}>URL</Text>}
          />
          {param.description && <Text type="secondary" style={{ display: 'block', fontSize: 12, marginTop: 4 }}>{param.description}</Text>}
        </Form.Item>
      )
    }

    // 路径字段 - 支持手动输入或上传文件获取路径
    if (isPath) {
      // 检查是否是 git_clone 的 directory 字段，添加自动生成功能
      const isGitCloneDirectory = selectedTool === 'git_clone' && param.name === 'directory'
      // 是否是文件路径（区别于目录路径）
      const isFilePath = param.name.toLowerCase() === 'filepath' || param.name.toLowerCase() === 'file_path'
    
      // 自动生成目录名的函数
      const generateDirectory = () => {
        const remoteUrl = String(formValues['remoteUrl'] || '')
        if (!remoteUrl) {
          antdMessage.warning('请先输入远程仓库URL')
          return
        }
        let repoName = remoteUrl.split('/').pop() || 'repo'
        repoName = repoName.replace(/\.git$/, '')
        const timestamp = Date.now().toString(36)
        const newDir = `E:/temp/${repoName}-${timestamp}`
        updateFormValue(param.name, newDir)
      }
    
      // 上传文件获取路径
      const handleUploadForPath = async (file: File) => {
        setUploadingPath(prev => ({ ...prev, [param.name]: true }))
        try {
          const fd = new FormData()
          fd.append('file', file)
          const res = await axios.post('/ai/document/upload/temp', fd, {
            headers: { 'Content-Type': 'multipart/form-data' },
          })
          const path = res.data?.path || res.data
          updateFormValue(param.name, path)
          antdMessage.success(`文件已上传，路径已自动填入`)
        } catch {
          antdMessage.error('文件上传失败')
        } finally {
          setUploadingPath(prev => ({ ...prev, [param.name]: false }))
        }
        return false // 防止 antd Upload 自动上传
      }
    
      return (
        <Form.Item key={param.name} label={label} style={{ marginBottom: 12 }}>
          <Space.Compact style={{ width: '100%' }}>
            <Input
              value={String(value)}
              onChange={e => updateFormValue(param.name, e.target.value)}
              placeholder={param.description || 'E:/path/to/file'}
              prefix={<FolderOpenOutlined style={{ color: '#bfbfbf' }} />}
              style={{ flex: 1 }}
            />
            {isFilePath && (
              <Upload
                showUploadList={false}
                beforeUpload={handleUploadForPath}
                accept=".pdf,.docx,.doc,.rtf,.xlsx,.xls,.pptx,.ppt,.txt,.md,.html,.htm,.json,.yaml,.yml,.xml,.csv"
              >
                <Tooltip title="上传文件自动获取路径">
                  <Button
                    icon={<UploadOutlined />}
                    loading={uploadingPath[param.name]}
                    type="default"
                  >
                    上传
                  </Button>
                </Tooltip>
              </Upload>
            )}
            {isGitCloneDirectory && (
              <Tooltip title="根据仓库名自动生成唯一目录">
                <Button
                  type="primary"
                  icon={<EditOutlined />}
                  onClick={generateDirectory}
                >
                  自动生成
                </Button>
              </Tooltip>
            )}
          </Space.Compact>
          {param.description && <Text type="secondary" style={{ display: 'block', fontSize: 12, marginTop: 4 }}>{param.description}</Text>}
          {isFilePath && (
            <Text type="secondary" style={{ display: 'block', fontSize: 11, marginTop: 2, color: '#999' }}>
              支持手动输入路径，或点击「上传」按钟选择文件（PDF/Word/Excel/PPT/RTF/Markdown/TXT/XML/HTML/JSON/CSV）
            </Text>
          )}
          {isGitCloneDirectory && (
            <Text type="secondary" style={{ display: 'block', fontSize: 11, marginTop: 2, color: '#999' }}>
              提示：输入URL后点击「自动生成」可创建唯一目录，或手动输入自定义路径
            </Text>
          )}
        </Form.Item>
      )
    }

    // 默认文本字段
    return (
      <Form.Item key={param.name} label={label} style={{ marginBottom: 12 }}>
        <Input
          value={String(value)}
          onChange={e => updateFormValue(param.name, e.target.value)}
          placeholder={param.description}
        />
        {param.description && <Text type="secondary" style={{ display: 'block', fontSize: 12, marginTop: 4 }}>{param.description}</Text>}
      </Form.Item>
    )
  }

  const selectToolAndTemplate = (toolName: string) => {
    setSelectedTool(toolName)
    const template = getTemplate(toolName)
    setJsonParam(template)
    setJsonError('')
    setTestResult(null)
    setExecTime(null)
    // 同步到表单值
    const parsed = tryParseJson(template)
    if (parsed.ok) {
      setFormValues(parsed.value as Record<string, unknown>)
    }
  }

  const openTestModal = (group: ToolGroup) => {
    setTestGroup(group)
    setTestResult(null)
    setExecTime(null)
    const firstTool = group.tools[0]?.name ?? ''
    setSelectedTool(firstTool)
    const template = getTemplate(firstTool)
    setJsonParam(template)
    setJsonError('')
    setTestVisible(true)
    // 同步到表单值
    const parsed = tryParseJson(template)
    if (parsed.ok) {
      setFormValues(parsed.value as Record<string, unknown>)
    }
  }

  const handleJsonChange = (val: string) => {
    setJsonParam(val)
    const parsed = tryParseJson(val)
    setJsonError(parsed.ok ? '' : `JSON 格式错误: ${parsed.error}`)
  }

  const handleTest = async () => {
    if (!selectedTool) return antdMessage.warning('请选择要测试的工具方法')
    const parsed = tryParseJson(jsonParam)
    if (!parsed.ok) return antdMessage.error('请先修正 JSON 格式错误')

    setTestLoading(true)
    setTestResult(null)
    setExecTime(null)
    const t0 = Date.now()
    try {
      const res = await axios.post(`/api/mcp/tools/${selectedTool}/execute`, parsed.value)
      const elapsed = Date.now() - t0
      setExecTime(elapsed)
      const data = res.data?.data ?? res.data
      const isSuccess = data?.success !== false
      // 检查是否有SVG内容
      const svgContent = data?.metadata?.svgContent ?? null
      setTestResult({
        success: isSuccess,
        output: data?.output ?? (isSuccess ? JSON.stringify(data, null, 2) : undefined),
        error: !isSuccess ? (data?.errorMessage ?? '执行失败') : undefined,
        meta: data?.metadata,
        svgContent: svgContent,
      })
    } catch (e: unknown) {
      const elapsed = Date.now() - t0
      setExecTime(elapsed)
      const err = e as { response?: { data?: { message?: string } }; message?: string }
      setTestResult({ success: false, error: err.response?.data?.message ?? err.message ?? '请求失败' })
    } finally {
      setTestLoading(false)
    }
  }

  const copyResult = () => {
    const text = testResult?.output ?? testResult?.error ?? ''
    navigator.clipboard.writeText(text).then(() => antdMessage.success('已复制到剪贴板'))
  }

  // -------- 详情弹窗 --------

  const openDetailModal = (group: ToolGroup) => { setDetailGroup(group); setDetailVisible(true) }

  const paramColumns = [
    { title: '参数名', dataIndex: 'name', key: 'name', render: (v: string) => <Text code>{v}</Text> },
    { title: '类型',   dataIndex: 'type', key: 'type', render: (v: string) => <Tag>{v}</Tag> },
    { title: '必填',   dataIndex: 'required', key: 'required',
      render: (v: boolean) => v ? <Tag color="red">必填</Tag> : <Tag color="default">可选</Tag> },
    { title: '描述',   dataIndex: 'description', key: 'description', render: (v: string) => v ?? '-' },
  ]

  // -------- 渲染卡片 --------

  const renderCard = (group: ToolGroup) => {
    const catCfg = CATEGORY_CONFIG[group.category] ?? { label: group.category, icon: <ToolOutlined />, color: '#1677ff', tagColor: 'blue' }
    const usageCount = USAGE_COUNTS[group.groupName] ?? 100
    const enabled = enabledMap[group.groupName] ?? true

    return (
      <Card
        key={group.groupName}
        hoverable
        style={{ borderRadius: 12, border: '1px solid #f0f0f0', opacity: enabled ? 1 : 0.55 }}
        bodyStyle={{ padding: '20px 20px 12px' }}
        actions={[
          <Button
            type="link" icon={<PlayCircleOutlined />}
            disabled={!enabled || group.tools.length === 0}
            onClick={() => openTestModal(group)}
            key="test"
          >
            测试
          </Button>,
          <Button
            type="link" icon={<InfoCircleOutlined />}
            onClick={() => openDetailModal(group)}
            key="detail"
          >
            详情
          </Button>,
        ]}
      >
        <div style={{ display: 'flex', alignItems: 'flex-start', gap: 12, marginBottom: 12 }}>
          <div style={{
            width: 48, height: 48, borderRadius: 10,
            background: `${catCfg.color}18`,
            display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
          }}>
            <ToolOutlined style={{ fontSize: 22, color: catCfg.color }} />
          </div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <Text strong style={{ fontSize: 15 }}>{group.groupName}</Text>
              <Switch
                size="small" checked={enabled}
                onChange={v => setEnabledMap(prev => ({ ...prev, [group.groupName]: v }))}
              />
            </div>
            <Paragraph
              ellipsis={{ rows: 2 }}
              style={{ color: '#595959', marginTop: 4, marginBottom: 0, fontSize: 13 }}
            >
              {group.description}
            </Paragraph>
          </div>
        </div>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, marginTop: 8 }}>
          <Tag color={catCfg.tagColor} style={{ borderRadius: 6 }}>{catCfg.label}</Tag>
          <Tag style={{ borderRadius: 6 }}>使用: {usageCount}次</Tag>
          {group.tools.length > 0 && (
            <Tag color="geekblue" style={{ borderRadius: 6 }}>{group.tools.length} 个方法</Tag>
          )}
        </div>
      </Card>
    )
  }

  // ======== 渲染 ========

  return (
    <div style={{ padding: '0 4px' }}>
      <Card
        title={
          <Space>
            <ToolOutlined style={{ color: '#1677ff' }} />
            <span style={{ fontWeight: 600 }}>MCP工具中心</span>
            <Badge count={toolGroups.length} style={{ backgroundColor: '#1677ff' }} />
          </Space>
        }
        extra={
          <Space>
            <Tooltip title="刷新工具列表">
              <Button icon={<ReloadOutlined />} onClick={loadTools} loading={loading} />
            </Tooltip>
            <Button type="primary" icon={<PlusOutlined />}>添加工具</Button>
          </Space>
        }
        style={{ borderRadius: 12 }}
      >
        <div style={{ marginBottom: 20 }}>
          <Input
            placeholder="搜索工具"
            prefix={<SearchOutlined style={{ color: '#bfbfbf' }} />}
            value={searchText}
            onChange={e => setSearchText(e.target.value)}
            style={{ width: 360, borderRadius: 8 }}
            allowClear
          />
        </div>

        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          items={[
            { key: 'all', label: <span><ToolOutlined /> 全部工具</span> },
            ...Object.entries(CATEGORY_CONFIG).map(([key, cfg]) => ({
              key, label: <span>{cfg.icon} {cfg.label}</span>,
            })),
          ]}
          style={{ marginBottom: 16 }}
        />

        {loading ? (
          <div style={{ textAlign: 'center', padding: '60px 0' }}>
            <Spin size="large" tip="加载工具中..." />
          </div>
        ) : filtered.length === 0 ? (
          <Empty description="暂无工具" style={{ padding: '60px 0' }} />
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 20 }}>
            {filtered.map(renderCard)}
          </div>
        )}
      </Card>

      {/* ======== 测试弹窗 ======== */}
      <Modal
        title={
          <Space>
            <ThunderboltOutlined style={{ color: '#1677ff' }} />
            <span>执行工具：<Text strong>{testGroup?.groupName}</Text></span>
          </Space>
        }
        open={testVisible}
        onCancel={() => { setTestVisible(false); setTestResult(null) }}
        footer={[
          <Button key="cancel" onClick={() => setTestVisible(false)}>关闭</Button>,
          <Tooltip key="copy" title="复制结果">
            <Button icon={<CopyOutlined />} onClick={copyResult} disabled={!testResult}>复制结果</Button>
          </Tooltip>,
          <Button
            key="run" type="primary" icon={<PlayCircleOutlined />}
            loading={testLoading} onClick={handleTest}
            disabled={!selectedTool || !!jsonError || testGroup?.tools.length === 0}
          >
            执行
          </Button>,
        ]}
        width={720}
      >
        {testGroup && testGroup.tools.length > 0 ? (
          <>
            {/* 选择工具方法 */}
            <div style={{ marginBottom: 14 }}>
              <Text type="secondary" style={{ display: 'block', marginBottom: 6, fontSize: 13 }}>
                选择方法
              </Text>
              <Select
                value={selectedTool}
                onChange={selectToolAndTemplate}
                style={{ width: '100%' }}
                optionLabelProp="label"
              >
                {testGroup.tools.map(t => (
                  <Option key={t.name} value={t.name} label={t.name}>
                    <Space>
                      <Text code style={{ fontSize: 12 }}>{t.name}</Text>
                      <Text type="secondary" style={{ fontSize: 12 }}>{t.description}</Text>
                    </Space>
                  </Option>
                ))}
              </Select>
            </div>

            {/* 参数编辑区域 */}
            <div style={{ marginBottom: 14 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
                <Text type="secondary" style={{ fontSize: 13 }}>请求参数</Text>
                <Space>
                  <Radio.Group
                    value={formMode ? 'form' : 'json'}
                    onChange={e => setFormMode(e.target.value === 'form')}
                    size="small"
                  >
                    <Radio.Button value="form"><EditOutlined /> 表单</Radio.Button>
                    <Radio.Button value="json"><UnorderedListOutlined /> JSON</Radio.Button>
                  </Radio.Group>
                  <Button
                    size="small" type="link"
                    onClick={() => {
                      const template = getTemplate(selectedTool)
                      setJsonParam(template)
                      setJsonError('')
                      const parsed = tryParseJson(template)
                      if (parsed.ok) {
                        setFormValues(parsed.value as Record<string, unknown>)
                      }
                    }}
                    style={{ padding: 0, height: 'auto', fontSize: 12 }}
                  >
                    重置
                  </Button>
                </Space>
              </div>

              {/* 表单模式 */}
              {formMode ? (
                <div style={{
                  border: '1px solid #d9d9d9',
                  borderRadius: 8,
                  padding: '12px 16px',
                  background: '#fafafa',
                  maxHeight: 320,
                  overflow: 'auto',
                }}>
                  {getCurrentToolParams().length > 0 ? (
                    getCurrentToolParams().map(renderParamField)
                  ) : (
                    <Alert
                      type="info"
                      message="此方法无需参数"
                      showIcon
                      style={{ marginBottom: 0 }}
                    />
                  )}
                </div>
              ) : (
                /* JSON 模式 */
                <TextArea
                  value={jsonParam}
                  onChange={e => {
                    handleJsonChange(e.target.value)
                    // JSON 编辑后同步到表单
                    const parsed = tryParseJson(e.target.value)
                    if (parsed.ok) {
                      setFormValues(parsed.value as Record<string, unknown>)
                    }
                  }}
                  rows={8}
                  style={{
                    fontFamily: 'monospace', fontSize: 13,
                    borderColor: jsonError ? '#ff4d4f' : undefined,
                    background: '#fafafa',
                  }}
                  spellCheck={false}
                />
              )}
              {jsonError && (
                <Text type="danger" style={{ fontSize: 12, marginTop: 4, display: 'block' }}>
                  ⚠ {jsonError}
                </Text>
              )}
            </div>

            {/* 执行结果 */}
            {testResult && (
              <>
                <Divider style={{ margin: '12px 0' }} />
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
                  {testResult.success ? (
                    <Space>
                      <CheckCircleOutlined style={{ color: '#52c41a' }} />
                      <Text style={{ color: '#52c41a', fontWeight: 500 }}>执行成功</Text>
                    </Space>
                  ) : (
                    <Space>
                      <CloseCircleOutlined style={{ color: '#ff4d4f' }} />
                      <Text style={{ color: '#ff4d4f', fontWeight: 500 }}>执行失败</Text>
                    </Space>
                  )}
                  {execTime != null && (
                    <Text type="secondary" style={{ fontSize: 12 }}>耗时 {execTime}ms</Text>
                  )}
                </div>

                {testResult.success ? (
                  <TextArea
                    readOnly
                    value={testResult.output ?? '(无输出)'}
                    rows={10}
                    style={{
                      fontFamily: 'monospace', fontSize: 12,
                      background: '#f6ffed', borderColor: '#b7eb8f',
                    }}
                  />
                ) : (
                  <Alert
                    type="error"
                    message="错误信息"
                    description={<Text code style={{ fontSize: 12 }}>{testResult.error}</Text>}
                    showIcon
                  />
                )}

                {testResult.meta && (
                  <div style={{ marginTop: 8 }}>
                    <Text type="secondary" style={{ fontSize: 12 }}>元数据：</Text>
                    <Text code style={{ fontSize: 11, wordBreak: 'break-all' }}>
                      {JSON.stringify(testResult.meta)}
                    </Text>
                  </div>
                )}

                {/* SVG图片预览区域 */}
                {testResult.svgContent && (
                  <div style={{ marginTop: 16 }}>
                    <Divider style={{ margin: '12px 0' }} />
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
                      <Text strong style={{ fontSize: 13 }}>📊 图片预览</Text>
                      <Button
                        size="small"
                        onClick={() => {
                          const blob = new Blob([testResult.svgContent!], { type: 'image/svg+xml' })
                          const url = URL.createObjectURL(blob)
                          const a = document.createElement('a')
                          a.href = url
                          a.download = 'diagram.svg'
                          a.click()
                          URL.revokeObjectURL(url)
                        }}
                      >
                        下载SVG
                      </Button>
                    </div>
                    <div 
                      style={{ 
                        border: '1px solid #d9d9d9', 
                        borderRadius: 8, 
                        padding: 16, 
                        background: '#fafafa',
                        maxHeight: 400,
                        overflow: 'auto',
                      }}
                      dangerouslySetInnerHTML={{ __html: testResult.svgContent }}
                    />
                  </div>
                )}
              </>
            )}
          </>
        ) : (
          <Empty description="该工具组暂无可用方法（MCP Server 未连接）" />
        )}
      </Modal>

      {/* ======== 详情弹窗 ======== */}
      <Modal
        title={
          <Space>
            <InfoCircleOutlined style={{ color: '#1677ff' }} />
            工具详情：{detailGroup?.groupName}
          </Space>
        }
        open={detailVisible}
        onCancel={() => setDetailVisible(false)}
        footer={<Button onClick={() => setDetailVisible(false)}>关闭</Button>}
        width={780}
      >
        {detailGroup && (
          <>
            <div style={{ marginBottom: 16, padding: '12px 16px', background: '#fafafa', borderRadius: 8 }}>
              <Space wrap>
                {CATEGORY_CONFIG[detailGroup.category] && (
                  <Tag color={CATEGORY_CONFIG[detailGroup.category].tagColor}>
                    {CATEGORY_CONFIG[detailGroup.category].label}
                  </Tag>
                )}
                <Tag color="geekblue">{detailGroup.tools.length} 个方法</Tag>
                <Tag>使用: {USAGE_COUNTS[detailGroup.groupName] ?? '-'}次</Tag>
              </Space>
              <Paragraph style={{ marginTop: 10, marginBottom: 0, color: '#595959' }}>
                {detailGroup.description}
              </Paragraph>
            </div>

            {detailGroup.tools.length === 0 ? (
              <Empty description="MCP Server 未连接，方法列表不可用" />
            ) : (
              detailGroup.tools.map((tool, idx) => (
                <div key={tool.name} style={{ marginBottom: 20 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 6 }}>
                    <Title level={5} style={{ margin: 0 }}>
                      <Text code>{tool.name}</Text>
                    </Title>
                    {tool.async && <Tag color="orange">异步</Tag>}
                    <Button
                      size="small" type="link" icon={<PlayCircleOutlined />}
                      style={{ padding: 0, marginLeft: 'auto' }}
                      onClick={() => {
                        setDetailVisible(false)
                        setTimeout(() => {
                          const group = toolGroups.find(g => g.tools.some(t => t.name === tool.name))
                          if (group) {
                            setTestGroup(group)
                            setTestResult(null)
                            setExecTime(null)
                            setSelectedTool(tool.name)
                            setJsonParam(getTemplate(tool.name))
                            setJsonError('')
                            setTestVisible(true)
                          }
                        }, 150)
                      }}
                    >
                      立即测试
                    </Button>
                  </div>
                  <Text type="secondary" style={{ fontSize: 13 }}>{tool.description}</Text>

                  {/* 参数说明（如果有） */}
                  {tool.parameters.length > 0 ? (
                    <Table
                      size="small"
                      dataSource={tool.parameters}
                      columns={paramColumns}
                      rowKey="name"
                      pagination={false}
                      style={{ marginTop: 10 }}
                    />
                  ) : (
                    <div style={{ marginTop: 8 }}>
                      {TOOL_TEMPLATES[tool.name] && Object.keys(TOOL_TEMPLATES[tool.name]).length > 0 ? (
                        <div style={{ background: '#f5f5f5', borderRadius: 6, padding: '8px 12px' }}>
                          <Text type="secondary" style={{ fontSize: 12 }}>参数示例：</Text>
                          <pre style={{ margin: '4px 0 0', fontSize: 12, color: '#434343' }}>
                            {JSON.stringify(TOOL_TEMPLATES[tool.name], null, 2)}
                          </pre>
                        </div>
                      ) : (
                        <Text type="secondary" style={{ fontSize: 12 }}>无需参数</Text>
                      )}
                    </div>
                  )}

                  {idx < detailGroup.tools.length - 1 && <Divider style={{ margin: '16px 0' }} />}
                </div>
              ))
            )}
          </>
        )}
      </Modal>
    </div>
  )
}

export default MCPHub
