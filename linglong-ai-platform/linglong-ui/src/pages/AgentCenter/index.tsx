import { useState, useEffect } from 'react'
import {
  Card,
  Row,
  Col,
  Tag,
  Button,
  Avatar,
  Badge,
  Modal,
  Input,
  message,
  Spin,
  Typography,
  Steps,
  Descriptions,
  Empty,
  Switch,
  Form,
  Divider,
  Tooltip,
  Upload,
  Checkbox,
  Radio,
  Space,
} from 'antd'
import {
  RobotOutlined,
  PlayCircleOutlined,
  SettingOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  LoadingOutlined,
  ThunderboltOutlined,
  EyeOutlined,
  EyeInvisibleOutlined,
  HistoryOutlined,
  ReloadOutlined,
  UploadOutlined,
  FolderOutlined,
  FileTextOutlined,
  FolderOpenOutlined,

} from '@ant-design/icons'
import axios from '@/services/api'
import type { UploadFile, UploadProps } from 'antd/es/upload/interface'

const { TextArea } = Input
const { Text, Paragraph } = Typography
const { Step } = Steps

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

const typeLabelMap: Record<string, string> = {
  REQUIREMENT_ANALYSIS: '需求分析',
  DOMAIN_MODELING: '领域建模',
  ARCHITECTURE_DESIGN: '架构设计',
  CODE_GENERATION: '代码生成',
  CODE_REVIEW: '代码审查',
  TEST_GENERATION: '测试生成',
  DOCUMENTATION: '文档生成',
  DEPLOYMENT: '部署',
  DEBUGGING: '调试',
  OPTIMIZATION: '优化',
}

const typeColorMap: Record<string, string> = {
  REQUIREMENT_ANALYSIS: 'blue',
  DOMAIN_MODELING: 'cyan',
  ARCHITECTURE_DESIGN: 'purple',
  CODE_GENERATION: 'green',
  CODE_REVIEW: 'orange',
  TEST_GENERATION: 'magenta',
  DOCUMENTATION: 'geekblue',
  DEPLOYMENT: 'volcano',
  DEBUGGING: 'red',
  OPTIMIZATION: 'gold',
}

function AgentCenter() {
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
  
  // 文件上传相关状态
  const [fileList, setFileList] = useState<UploadFile[]>([])
  const [outputPath, setOutputPath] = useState('')
  const [autoSave, setAutoSave] = useState(false)
  // 文件输入方式：upload(文件流) | path(文件路径)
  const [fileInputMode, setFileInputMode] = useState<'upload' | 'path'>('upload')
  const [filePaths, setFilePaths] = useState<string>('')

  useEffect(() => {
    loadAgents()
  }, [])

  const loadAgents = async () => {
    setLoading(true)
    console.log('开始加载Agent列表...')
    try {
      // 首先尝试获取详细信息
      console.log('请求 /agent/list/detail')
      const res = await axios.get('/agent/list/detail')
      console.log('获取Agent详细列表响应:', res.data)
      if (res.data?.code === 200 || res.data?.success) {
        setAgents(res.data.data || [])
        setLoading(false)
        return
      }
    } catch (error: any) {
      console.warn('获取Agent详细列表失败，尝试获取基础列表', error)
      console.warn('错误详情:', error.message, error.response?.status, error.response?.data)
    }

    // 如果详细列表失败，尝试获取基础列表
    try {
      console.log('请求 /agent/list')
      const res = await axios.get('/agent/list')
      console.log('获取Agent基础列表响应:', res.data)
      if (res.data?.code === 200 || res.data?.success) {
        // 为基础列表添加默认值
        const basicAgents = (res.data.data || []).map((agent: AgentInfo) => ({
          ...agent,
          usageCount: 0,
          enabled: true,
        }))
        setAgents(basicAgents)
      } else {
        setAgents([])
      }
    } catch (error: any) {
      console.error('加载Agent列表失败', error)
      console.error('错误详情:', error.message, error.response?.status, error.response?.data)
      message.error('加载Agent列表失败，请检查Agent服务是否启动 (端口: 8085)')
      setAgents([])
    } finally {
      setLoading(false)
    }
  }

  const openRunModal = (agent: AgentInfo) => {
    if (agent.enabled === false) {
      message.warning('该Agent已被禁用，请先启用')
      return
    }
    setCurrentAgent(agent)
    setInputText('')
    setOutputText('')
    setFileList([])
    setOutputPath('')
    setAutoSave(false)
    setFileInputMode('upload')
    setFilePaths('')
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

  const openWorkflowModal = () => {
    setWorkflowInput('')
    setCurrentWorkflow(null)
    setWorkflowModalVisible(true)
  }

  const executeAgent = async () => {
    if (!currentAgent || !inputText.trim()) {
      message.warning('请输入内容')
      return
    }

    setExecuting(true)
    setOutputText('')

    try {
      // 文件路径模式：将路径作为附加参数传递
      if (fileInputMode === 'path' && filePaths.trim()) {
        const paths = filePaths.split('\n').map(s => s.trim()).filter(Boolean)
        const res = await axios.post(`/agent/${currentAgent.name}/execute`, {
          input: inputText,
          outputPath: outputPath,
          autoSave: autoSave,
          filePaths: paths,
        }, { timeout: 300000 })
        if (res.data?.code === 200) {
          const agentResult = res.data.data
          let output = agentResult?.output || '执行完成'
          const savedPath = agentResult?.data?.savedPath
          if (savedPath) output += `\n\n---\n结果已保存到: ${savedPath}`
          setOutputText(output)
          loadAgents()
        } else {
          setOutputText(`执行失败: ${res.data?.message || '未知错误'}`)
        }
        return
      }

      // 如果有文件上传，使用 multipart/form-data
      if (fileList.length > 0) {
        const formData = new FormData()
        formData.append('input', inputText)
        formData.append('outputPath', outputPath)
        formData.append('autoSave', autoSave.toString())
        
        // 添加文件
        fileList.forEach((file) => {
          const fileObj = (file as any).originFileObj || file
          if (fileObj) {
            formData.append('files', fileObj as Blob)
          }
        })

        const res = await axios.post(`/agent/${currentAgent.name}/execute-with-files`, formData, {
          headers: { 'Content-Type': 'multipart/form-data' },
          timeout: 300000,
        })

        if (res.data?.code === 200) {
          const agentResult = res.data.data
          let output = agentResult?.output || '执行完成'
          const savedPath = agentResult?.data?.savedPath
          if (savedPath) output += `\n\n---\n结果已保存到: ${savedPath}`
          setOutputText(output)
          loadAgents()
        } else {
          setOutputText(`执行失败: ${res.data?.message || '未知错误'}`)
        }
      } else {
        // 没有文件上传，使用普通 JSON 请求
        const res = await axios.post(`/agent/${currentAgent.name}/execute`, {
          input: inputText,
          outputPath: outputPath,
          autoSave: autoSave,
        }, { timeout: 300000 })

        if (res.data?.code === 200) {
          const agentResult = res.data.data
          let output = agentResult?.output || '执行完成'
          const savedPath = agentResult?.data?.savedPath
          if (savedPath) output += `\n\n---\n结果已保存到: ${savedPath}`
          setOutputText(output)
          loadAgents()
        } else {
          setOutputText(`执行失败: ${res.data?.message || '未知错误'}`)
        }
      }
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } }; message?: string }
      setOutputText(`执行失败: ${err.response?.data?.message || err.message || '未知错误'}`)
    } finally {
      setExecuting(false)
    }
  }

  // 文件上传配置
  const uploadProps: UploadProps = {
    onRemove: (file) => {
      const index = fileList.indexOf(file)
      const newFileList = fileList.slice()
      newFileList.splice(index, 1)
      setFileList(newFileList)
    },
    beforeUpload: (file) => {
      setFileList([...fileList, file])
      return false // 阿止 → 阿止自动上传
    },
    fileList,
    multiple: true,
    accept: '.pdf,.docx,.doc,.rtf,.xlsx,.xls,.xlsm,.pptx,.ppt,.pps,.txt,.md,.markdown,.html,.htm,.json,.yaml,.yml,.xml,.csv,.java,.py,.js,.ts,.go,.cpp,.c',
  }

  const saveAgentConfig = async (values: {
    displayName: string
    description: string
    systemPrompt: string
    enabled: boolean
  }) => {
    if (!currentAgent) return

    setSavingConfig(true)
    try {
      const res = await axios.post(`/agent/${currentAgent.name}/config`, {
        agentName: currentAgent.name,
        displayName: values.displayName,
        description: values.description,
        systemPrompt: values.systemPrompt,
        enabled: values.enabled,
      })

      if (res.data?.success) {
        message.success('配置保存成功')
        setConfigModalVisible(false)
        loadAgents()
      } else {
        message.error(res.data?.message || '保存失败')
      }
    } catch (error) {
      console.error('保存配置失败', error)
      message.error('保存配置失败')
    } finally {
      setSavingConfig(false)
    }
  }

  const toggleAgentStatus = async (agent: AgentInfo, enabled: boolean) => {
    try {
      const url = enabled
        ? `/agent/${agent.name}/enable`
        : `/agent/${agent.name}/disable`
      const res = await axios.post(url)

      if (res.data?.success) {
        message.success(enabled ? 'Agent已启用' : 'Agent已禁用')
        loadAgents()
      } else {
        message.error(res.data?.message || '操作失败')
      }
    } catch (error) {
      console.error('切换Agent状态失败', error)
      message.error('操作失败')
    }
  }

  const startWorkflow = async () => {
    if (!workflowInput.trim()) {
      message.warning('请输入需求描述')
      return
    }

    setWorkflowLoading(true)
    try {
      const res = await axios.post('/agent/workflow/start', {
        input: workflowInput,
      })

      if (res.data?.success) {
        setCurrentWorkflow(res.data.data)
        message.success('工作流已启动')
        // 开始轮询状态
        pollWorkflowStatus(res.data.data.id)
      } else {
        message.error(res.data?.message || '启动失败')
      }
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } }; message?: string }
      message.error(err.response?.data?.message || err.message || '启动失败')
    } finally {
      setWorkflowLoading(false)
    }
  }

  const pollWorkflowStatus = async (workflowId: string) => {
    const poll = async () => {
      try {
        const res = await axios.get(`/agent/workflow/${workflowId}`)
        if (res.data?.success) {
          const workflow = res.data.data
          setCurrentWorkflow(workflow)
          
          if (workflow.status === 'RUNNING') {
            setTimeout(poll, 2000)
          }
        }
      } catch (error) {
        console.error('轮询工作流状态失败', error)
      }
    }
    poll()
  }

  const getStepStatus = (status: string) => {
    switch (status) {
      case 'COMPLETED':
        return 'finish'
      case 'FAILED':
        return 'error'
      case 'RUNNING':
        return 'process'
      default:
        return 'wait'
    }
  }

  const getStepIcon = (status: string) => {
    switch (status) {
      case 'COMPLETED':
        return <CheckCircleOutlined />
      case 'FAILED':
        return <CloseCircleOutlined />
      case 'RUNNING':
        return <LoadingOutlined />
      default:
        return undefined
    }
  }

  return (
    <div className="agent-center">
      {/* 页面标题区 */}
      <div className="g-page-header">
        <div className="g-page-header-left">
          <h1 className="g-page-title">智能体中心</h1>
          <p className="g-page-subtitle">创建、配置与运行 AI 智能体，支持全链路智能软件开发工作流</p>
        </div>
        <Button type="primary" size="large" icon={<ThunderboltOutlined />} onClick={openWorkflowModal}>
          启动全AI开发
        </Button>
      </div>

      <div style={{ padding: '24px 32px' }}>
      {/* 全智能开发入口 */}
      <Card
        style={{ marginBottom: 24, borderRadius: 12 }}
        bodyStyle={{ textAlign: 'center', padding: '32px' }}
      >
        <ThunderboltOutlined style={{ fontSize: 48, color: '#1677ff', marginBottom: 16 }} />
        <h2 style={{ marginBottom: 8 }}>全AI智能开发</h2>
        <p style={{ color: '#666', marginBottom: 24 }}>
          一键启动完整开发流程：需求分析 → 领域建模 → 架构设计 → 代码生成 → 代码审查 → 测试生成
        </p>
        <Button type="primary" size="large" icon={<ThunderboltOutlined />} onClick={openWorkflowModal}>
          启动智能开发
        </Button>
      </Card>

      {/* Agent列表 */}
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Text strong style={{ fontSize: 16 }}>智能体列表</Text>
        <Button icon={<ReloadOutlined />} onClick={loadAgents} loading={loading}>
          刷新
        </Button>
      </div>
      <Spin spinning={loading}>
        {agents.length === 0 ? (
          <Empty
            description={
              <div>
                <div>暂无Agent</div>
                <div style={{ fontSize: 12, color: '#999', marginTop: 8 }}>
                  请确保Agent服务已启动 (端口: 8085)
                </div>
                <Button type="primary" style={{ marginTop: 16 }} onClick={loadAgents}>
                  重新加载
                </Button>
              </div>
            }
          />
        ) : (
          <Row gutter={[24, 24]}>
            {agents.map((agent) => (
              <Col span={8} key={agent.name}>
                <Card
                  hoverable
                  style={{
                    borderRadius: 12,
                    opacity: agent.enabled === false ? 0.7 : 1,
                  }}
                  actions={[
                    <Tooltip title={agent.enabled === false ? 'Agent已禁用' : '运行Agent'}>
                      <Button
                        type="link"
                        icon={<PlayCircleOutlined />}
                        onClick={() => openRunModal(agent)}
                        disabled={agent.enabled === false}
                      >
                        运行
                      </Button>
                    </Tooltip>,
                    <Button
                      type="link"
                      icon={<SettingOutlined />}
                      onClick={() => openConfigModal(agent)}
                    >
                      配置
                    </Button>,
                  ]}
                >
                  <Card.Meta
                    avatar={
                      <Avatar
                        size={64}
                        icon={<RobotOutlined />}
                        style={{
                          backgroundColor: agent.enabled === false ? '#999' : '#1677ff',
                        }}
                      />
                    }
                    title={
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                        {agent.displayName || agent.name.replace('Agent', '')}
                        <Badge
                          status={agent.enabled === false ? 'default' : 'success'}
                          text={agent.enabled === false ? '已禁用' : '运行中'}
                        />
                      </div>
                    }
                    description={
                      <div>
                        <p style={{ marginTop: 8, minHeight: 44 }}>{agent.description}</p>
                        <div style={{ marginTop: 12, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                          <Tag color={typeColorMap[agent.type] || 'blue'}>
                            {typeLabelMap[agent.type] || agent.type}
                          </Tag>
                          <Tooltip title="使用次数">
                            <span style={{ color: '#666', fontSize: 12 }}>
                              <HistoryOutlined style={{ marginRight: 4 }} />
                              使用: {agent.usageCount || 0}
                            </span>
                          </Tooltip>
                        </div>
                      </div>
                    }
                  />
                </Card>
              </Col>
            ))}
          </Row>
        )}
      </Spin>

      {/* Agent运行对话框 */}
      <Modal
        title={
          <span>
            <RobotOutlined style={{ marginRight: 8, color: '#1677ff' }} />
            {currentAgent?.name.replace('Agent', '')} - 执行
          </span>
        }
        open={runModalVisible}
        onCancel={() => setRunModalVisible(false)}
        width={800}
        footer={null}
      >
        <div style={{ marginBottom: 16 }}>
          <Text strong>输入内容</Text>
          <TextArea
            rows={4}
            value={inputText}
            onChange={(e) => setInputText(e.target.value)}
            placeholder="请输入要处理的内容..."
            style={{ marginTop: 8 }}
          />
        </div>

        {/* 文件输入区域 */}
        <div style={{ marginBottom: 16 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
            <Text strong>文件输入（可选）</Text>
            <Radio.Group
              size="small"
              value={fileInputMode}
              onChange={e => { setFileInputMode(e.target.value); setFileList([]); setFilePaths('') }}
            >
              <Radio.Button value="upload"><UploadOutlined /> 上传文件</Radio.Button>
              <Radio.Button value="path"><FolderOpenOutlined /> 文件路径</Radio.Button>
            </Radio.Group>
          </div>

          {fileInputMode === 'upload' ? (
            <div>
              <Upload {...uploadProps}>
                <Button icon={<UploadOutlined />}>选择文件</Button>
              </Upload>
              <Text type="secondary" style={{ fontSize: 12, display: 'block', marginTop: 4 }}>
                支持：PDF、Word(DOCX/DOC)、Excel(XLSX/XLS)、PPT(PPTX/PPT)、RTF、Markdown、TXT、XML、HTML、JSON、CSV等多种格式
              </Text>
            </div>
          ) : (
            <div>
              <Space.Compact style={{ width: '100%' }}>
                <Input
                  value={filePaths}
                  onChange={e => setFilePaths(e.target.value)}
                  placeholder="输入文件绝对路径，多个文件用换行分隔..."
                  prefix={<FileTextOutlined style={{ color: '#bfbfbf' }} />}
                />
              </Space.Compact>
              <Text type="secondary" style={{ fontSize: 12, display: 'block', marginTop: 4 }}>
                示例：E:/docs/report.pdf（多个文件每行一个），支持所有文档格式
              </Text>
            </div>
          )}
        </div>

        {/* 输出保存路径配置 */}
        <div style={{ marginBottom: 16, padding: 16, background: '#f6ffed', borderRadius: 8, border: '1px solid #b7eb8f' }}>
          <Text strong style={{ display: 'block', marginBottom: 8, color: '#52c41a' }}>
            <FolderOutlined style={{ marginRight: 4 }} />
            输出保存配置
          </Text>
          <div style={{ marginBottom: 8 }}>
            <Checkbox 
              checked={autoSave} 
              onChange={(e) => setAutoSave(e.target.checked)}
            >
              自动保存执行结果到文件
            </Checkbox>
          </div>
          {autoSave && (
            <div>
              <Text style={{ fontSize: 12, display: 'block', marginBottom: 4 }}>保存路径（可选，留空使用默认路径）</Text>
              <Input
                placeholder="例如: E:/MyProjects/AgentOutputs"
                value={outputPath}
                onChange={(e) => setOutputPath(e.target.value)}
                prefix={<FolderOutlined />}
              />
              <Text type="secondary" style={{ fontSize: 12, display: 'block', marginTop: 4 }}>
                不填写则保存到默认输出目录
              </Text>
            </div>
          )}
        </div>

        <div style={{ marginBottom: 16, textAlign: 'right' }}>
          <Button type="primary" loading={executing} onClick={executeAgent}>
            执行
          </Button>
        </div>

        {outputText && (
          <div>
            <Text strong>执行结果</Text>
            <Paragraph
              style={{
                marginTop: 8,
                padding: 16,
                background: '#f5f5f5',
                borderRadius: 8,
                maxHeight: 400,
                overflow: 'auto',
                whiteSpace: 'pre-wrap',
              }}
            >
              {outputText}
            </Paragraph>
          </div>
        )}
      </Modal>

      {/* Agent配置对话框 */}
      <Modal
        title={
          <span>
            <SettingOutlined style={{ marginRight: 8, color: '#1677ff' }} />
            {currentAgent?.displayName || currentAgent?.name?.replace('Agent', '')} - 配置
          </span>
        }
        open={configModalVisible}
        onCancel={() => setConfigModalVisible(false)}
        width={700}
        footer={null}
      >
        <Form
          form={configForm}
          layout="vertical"
          onFinish={saveAgentConfig}
          style={{ marginTop: 16 }}
        >
          <Form.Item
            name="displayName"
            label="显示名称"
            rules={[{ required: true, message: '请输入显示名称' }]}
          >
            <Input placeholder="请输入显示名称" />
          </Form.Item>

          <Form.Item
            name="description"
            label="描述"
            rules={[{ required: true, message: '请输入描述' }]}
          >
            <Input.TextArea rows={2} placeholder="请输入Agent描述" />
          </Form.Item>

          <Form.Item
            name="systemPrompt"
            label={
              <span>
                系统提示词
                <Button
                  type="link"
                  size="small"
                  icon={showSystemPrompt ? <EyeInvisibleOutlined /> : <EyeOutlined />}
                  onClick={() => setShowSystemPrompt(!showSystemPrompt)}
                  style={{ marginLeft: 8 }}
                >
                  {showSystemPrompt ? '隐藏' : '显示'}
                </Button>
              </span>
            }
          >
            <Input.TextArea
              rows={showSystemPrompt ? 10 : 3}
              placeholder="系统提示词定义了Agent的行为和角色"
              style={{ fontFamily: 'monospace' }}
            />
          </Form.Item>

          <Form.Item
            name="enabled"
            label="状态"
            valuePropName="checked"
          >
            <Switch
              checkedChildren="启用"
              unCheckedChildren="禁用"
              onChange={(checked) => {
                if (currentAgent) {
                  toggleAgentStatus(currentAgent, checked)
                }
              }}
            />
          </Form.Item>

          <Divider />

          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              {currentAgent && (
                <div style={{ color: '#666', fontSize: 12 }}>
                  <div>使用次数: {currentAgent.usageCount || 0}</div>
                  {currentAgent.lastExecutionTime && (
                    <div>最后执行: {new Date(currentAgent.lastExecutionTime).toLocaleString()}</div>
                  )}
                </div>
              )}
            </div>
            <div>
              <Button style={{ marginRight: 8 }} onClick={() => setConfigModalVisible(false)}>
                取消
              </Button>
              <Button type="primary" loading={savingConfig} htmlType="submit">
                保存
              </Button>
            </div>
          </div>
        </Form>
      </Modal>

      {/* 全智能开发工作流对话框 */}
      <Modal
        title={
          <span>
            <ThunderboltOutlined style={{ marginRight: 8, color: '#1677ff' }} />
            全AI智能开发工作流
          </span>
        }
        open={workflowModalVisible}
        onCancel={() => setWorkflowModalVisible(false)}
        width={900}
        footer={null}
      >
        {!currentWorkflow ? (
          <div>
            <Text strong>需求描述</Text>
            <TextArea
              rows={6}
              value={workflowInput}
              onChange={(e) => setWorkflowInput(e.target.value)}
              placeholder="请描述您的软件开发需求，例如：&#10;开发一个用户管理系统，包含用户注册、登录、权限管理等功能。&#10;需要支持角色权限控制，用户组管理，操作日志记录等。"
              style={{ marginTop: 8 }}
            />
            <div style={{ marginTop: 16, textAlign: 'right' }}>
              <Button type="primary" size="large" loading={workflowLoading} onClick={startWorkflow}>
                启动工作流
              </Button>
            </div>
          </div>
        ) : (
          <div>
            <Steps
              current={currentWorkflow.currentStepIndex}
              status={currentWorkflow.status === 'FAILED' ? 'error' : 'process'}
              style={{ marginBottom: 24 }}
            >
              {currentWorkflow.steps.map((step, index) => (
                <Step
                  key={index}
                  title={step.name}
                  status={getStepStatus(step.status)}
                  icon={getStepIcon(step.status)}
                />
              ))}
            </Steps>

            <Descriptions bordered column={1}>
              <Descriptions.Item label="工作流ID">{currentWorkflow.id}</Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag
                  color={
                    currentWorkflow.status === 'COMPLETED'
                      ? 'success'
                      : currentWorkflow.status === 'FAILED'
                        ? 'error'
                        : 'processing'
                  }
                >
                  {currentWorkflow.status}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="进度">
                {currentWorkflow.currentStepIndex + 1} / {currentWorkflow.totalSteps}
              </Descriptions.Item>
            </Descriptions>

            {/* 当前步骤输出 */}
            {currentWorkflow.steps.map(
              (step, index) =>
                step.output && (
                  <div key={index} style={{ marginTop: 16 }}>
                    <Text strong>{step.name} 输出</Text>
                    <Paragraph
                      style={{
                        marginTop: 8,
                        padding: 12,
                        background: '#f5f5f5',
                        borderRadius: 8,
                        maxHeight: 200,
                        overflow: 'auto',
                        whiteSpace: 'pre-wrap',
                        fontSize: 12,
                      }}
                    >
                      {step.output.substring(0, 500)}
                      {step.output.length > 500 ? '...' : ''}
                    </Paragraph>
                  </div>
                )
            )}
          </div>
        )}
      </Modal>
      </div>
    </div>
  )
}

export default AgentCenter
