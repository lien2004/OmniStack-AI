import { useState, useRef } from 'react'
import {
  Card,
  Button,
  Input,
  Tabs,
  message,
  Spin,
  Typography,
  Tag,
  Space,
  Divider,
  Upload,
  Modal,
  Radio,
  Steps,
  Empty,
} from 'antd'
import {
  FileTextOutlined,
  RobotOutlined,
  DownloadOutlined,
  UploadOutlined,
  EditOutlined,
  EyeOutlined,
  CheckCircleOutlined,
  ThunderboltOutlined,
  CopyOutlined,
  DeleteOutlined,
} from '@ant-design/icons'
import axios from '@/services/api'
import ReactMarkdown from 'react-markdown'
import type { UploadFile, UploadProps } from 'antd/es/upload/interface'

const { TextArea } = Input
const { Text, Paragraph } = Typography

type Mode = 'generate' | 'evaluate' | 'optimize'
type StepStatus = 'wait' | 'process' | 'finish' | 'error'

interface ResumeState {
  content: string
  evaluation: string
  optimized: string
}

const MODE_CONFIG: Record<Mode, { label: string; tag: string; color: string; icon: React.ReactNode }> = {
  generate: { label: 'AI 生成简历', tag: '生成', color: '#1677ff', icon: <RobotOutlined /> },
  evaluate: { label: 'AI 测评简历', tag: '测评', color: '#fa8c16', icon: <CheckCircleOutlined /> },
  optimize: { label: 'AI 优化简历', tag: '优化', color: '#52c41a', icon: <ThunderboltOutlined /> },
}

const DEFAULT_RESUME = `## 张三
**求职意向：高级Java开发工程师 | 意向城市：北京**

---

### 联系方式
- 手机：138-xxxx-xxxx
- 邮箱：zhangsan@example.com
- GitHub：github.com/zhangsan

### 教育背景
**XX大学**  计算机科学与技术（本科）  2015.09 - 2019.06
- GPA：3.8/4.0，专业排名前10%
- 荣誉：国家奖学金、校级优秀毕业生

### 工作经历
**XX科技有限公司**  高级Java开发工程师  2021.03 - 至今
- 负责公司核心交易系统的架构设计与开发，系统日处理订单量超500万笔
- 主导微服务架构迁移，将单体应用拆分为30+微服务，系统可用性从99.5%提升至99.99%
- 设计并实现分布式缓存方案，接口平均响应时间从200ms降至50ms

**YY互联网公司**  Java开发工程师  2019.07 - 2021.02
- 参与电商平台订单模块开发，支持日均100万订单的高并发处理
- 使用Redis+RocketMQ实现订单状态机，订单处理成功率提升至99.95%

### 项目经验
**智能推荐系统重构**
- 技术栈：Spring Cloud、Flink、Elasticsearch、Kafka
- 负责实时推荐引擎开发，QPS从5000提升至30000
- 引入Flink实时特征计算，推荐点击率提升25%

### 专业技能
- 编程语言：Java、Python、Go
- 框架：Spring Boot、Spring Cloud、MyBatis、Netty
- 中间件：Redis、Kafka、RocketMQ、Elasticsearch、Nginx
- 数据库：MySQL、PostgreSQL、MongoDB、TiDB
- 其他：Docker、Kubernetes、Jenkins、Prometheus

### 自我评价
6年Java后端开发经验，擅长高并发系统设计与微服务架构。具备较强的技术攻坚能力和团队协作精神，持续关注云原生和AI技术发展趋势。
`

function ResumeBuilder() {
  const [mode, setMode] = useState<Mode>('generate')
  const [inputText, setInputText] = useState('')
  const [resume, setResume] = useState<ResumeState>({
    content: DEFAULT_RESUME,
    evaluation: '',
    optimized: '',
  })
  const [activeTab, setActiveTab] = useState<'edit' | 'preview'>('preview')
  const [loading, setLoading] = useState(false)
  const [streamOutput, setStreamOutput] = useState('')
  const [currentStep, setCurrentStep] = useState(0)
  const [showImportModal, setShowImportModal] = useState(false)
  const [importText, setImportText] = useState('')
  const [fileList, setFileList] = useState<UploadFile[]>([])
  const abortControllerRef = useRef<AbortController | null>(null)

  const steps = [
    { title: '编辑/导入简历', description: '准备原始内容' },
    { title: mode === 'generate' ? 'AI生成' : mode === 'evaluate' ? 'AI测评' : 'AI优化', description: '智能处理' },
    { title: '导出成果', description: '保存最终简历' },
  ]

  const getStepStatus = (index: number): StepStatus => {
    if (index < currentStep) return 'finish'
    if (index === currentStep) return 'process'
    return 'wait'
  }

  // 执行Agent（SSE流式）
  const executeResumeAgent = async () => {
    if (!inputText.trim() && mode === 'generate') {
      message.warning('请输入简历生成提示')
      return
    }
    if (!resume.content.trim()) {
      message.warning('请先编辑或导入简历内容')
      return
    }

    setLoading(true)
    setStreamOutput('')
    setCurrentStep(1)

    try {
      const controller = new AbortController()
      abortControllerRef.current = controller

      const requestBody = {
        input: mode === 'generate' ? inputText : resume.content,
        config: {
          mode: mode,
          ...(mode === 'optimize' && { feedback: resume.evaluation }),
        },
      }

      const response = await fetch('/api/agent/ResumeAgent/stream', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(requestBody),
        signal: controller.signal,
      })

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`)
      }

      const reader = response.body?.getReader()
      const decoder = new TextDecoder()
      let fullText = ''

      if (!reader) {
        throw new Error('无法读取响应流')
      }

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        const chunk = decoder.decode(value, { stream: true })
        const lines = chunk.split('\n')

        for (const line of lines) {
          if (line.startsWith('data: ')) {
            const data = line.slice(6).trim()
            if (data === '[DONE]') continue
            if (data.startsWith('{')) {
              try {
                const parsed = JSON.parse(data)
                if (parsed.error) {
                  message.error(parsed.error)
                  setLoading(false)
                  return
                }
              } catch {
                // not JSON, treat as text
              }
            }
            fullText += data
            setStreamOutput(fullText)
          }
        }
      }

      // 保存结果
      if (mode === 'generate') {
        setResume(prev => ({ ...prev, content: fullText }))
        setActiveTab('preview')
      } else if (mode === 'evaluate') {
        setResume(prev => ({ ...prev, evaluation: fullText }))
      } else if (mode === 'optimize') {
        setResume(prev => ({ ...prev, optimized: fullText, content: fullText }))
        setActiveTab('preview')
      }

      setCurrentStep(2)
      message.success(mode === 'generate' ? '简历生成完成' : mode === 'evaluate' ? '简历测评完成' : '简历优化完成')
    } catch (error: any) {
      if (error.name === 'AbortError') {
        message.info('已取消')
      } else {
        message.error('执行失败: ' + (error.message || '未知错误'))
      }
    } finally {
      setLoading(false)
      abortControllerRef.current = null
    }
  }

  // 取消流式请求
  const cancelStream = () => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort()
    }
  }

  // 导出Markdown
  const exportResume = () => {
    const blob = new Blob([resume.content], { type: 'text/markdown' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `简历_${new Date().toLocaleDateString()}.md`
    a.click()
    URL.revokeObjectURL(url)
    message.success('简历已导出')
  }

  // 复制内容
  const copyContent = () => {
    navigator.clipboard.writeText(resume.content)
    message.success('已复制到剪贴板')
  }

  // 导入文本
  const handleImportText = () => {
    if (importText.trim()) {
      setResume(prev => ({ ...prev, content: importText }))
      setImportText('')
      setShowImportModal(false)
      message.success('简历内容已导入')
      setCurrentStep(0)
    }
  }

  // 文件上传
  const uploadProps: UploadProps = {
    onRemove: (file) => {
      const index = fileList.indexOf(file)
      const newFileList = fileList.slice()
      newFileList.splice(index, 1)
      setFileList(newFileList)
    },
    beforeUpload: (file) => {
      setFileList([...fileList, file])
      return false
    },
    fileList,
    accept: '.txt,.md,.doc,.docx,.pdf',
  }

  // 通过Agent导入文件
  const handleImportFile = async () => {
    if (fileList.length === 0) {
      message.warning('请选择文件')
      return
    }

    setLoading(true)
    try {
      const formData = new FormData()
      formData.append('input', '请提取并整理这份简历的内容，以Markdown格式输出')
      fileList.forEach((file) => {
        const fileObj = (file as any).originFileObj || file
        if (fileObj) {
          formData.append('files', fileObj as Blob)
        }
      })

      const res = await axios.post('/api/agent/ResumeAgent/execute-with-files', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
        timeout: 300000,
      })

      if (res.data?.code === 200) {
        const output = res.data.data?.output || ''
        setResume(prev => ({ ...prev, content: output }))
        setFileList([])
        setShowImportModal(false)
        message.success('文件导入成功')
      } else {
        message.error(res.data?.message || '导入失败')
      }
    } catch (error: any) {
      message.error('导入失败: ' + (error.response?.data?.message || error.message))
    } finally {
      setLoading(false)
    }
  }

  // 一键优化：先测评再优化
  const handleOneClickOptimize = async () => {
    if (!resume.content.trim()) {
      message.warning('请先编辑或导入简历内容')
      return
    }

    setLoading(true)
    setStreamOutput('')
    setCurrentStep(1)

    try {
      // 第一步：测评
      message.loading({ content: '正在测评简历...', key: 'optimize', duration: 0 })
      const evalRes = await axios.post('/api/agent/ResumeAgent/execute', {
        input: resume.content,
        config: { mode: 'evaluate' },
      }, { timeout: 300000 })

      if (evalRes.data?.code !== 200) {
        throw new Error(evalRes.data?.message || '测评失败')
      }

      const evaluation = evalRes.data.data?.output || ''
      setResume(prev => ({ ...prev, evaluation }))

      // 第二步：优化
      message.loading({ content: '正在优化简历...', key: 'optimize', duration: 0 })
      const optRes = await axios.post('/api/agent/ResumeAgent/execute', {
        input: resume.content,
        config: { mode: 'optimize', feedback: evaluation },
      }, { timeout: 300000 })

      if (optRes.data?.code !== 200) {
        throw new Error(optRes.data?.message || '优化失败')
      }

      const optimized = optRes.data.data?.output || ''
      setResume(prev => ({ ...prev, optimized, content: optimized }))
      setActiveTab('preview')
      setCurrentStep(2)
      message.success({ content: '测评与优化完成', key: 'optimize' })
    } catch (error: any) {
      message.error({ content: '一键优化失败: ' + (error.message || '未知错误'), key: 'optimize' })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="resume-builder">
      {/* 页面头部 */}
      <div className="g-page-header">
        <div className="g-page-header-left">
          <h1 className="g-page-title">
            <FileTextOutlined style={{ marginRight: 8, color: '#52c41a' }} />
            AI 简历助手
          </h1>
          <p className="g-page-subtitle">
            基于 gpt-5.5 模型，智能生成简历、专业测评分析、一键优化完善
          </p>
        </div>
        <Space>
          <Button icon={<UploadOutlined />} onClick={() => setShowImportModal(true)}>
            导入简历
          </Button>
          <Button type="primary" icon={<DownloadOutlined />} onClick={exportResume}>
            导出简历
          </Button>
        </Space>
      </div>

      <div style={{ padding: '24px 32px' }}>
        {/* 步骤条 */}
        <Steps current={currentStep} status={loading ? 'process' : undefined} style={{ marginBottom: 24 }}>
          {steps.map((s, i) => (
            <Steps.Step key={i} title={s.title} description={s.description} status={getStepStatus(i)} />
          ))}
        </Steps>

        {/* 模式选择 */}
        <Card style={{ marginBottom: 24, borderRadius: 12 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 16 }}>
            <Radio.Group
              value={mode}
              onChange={(e) => {
                setMode(e.target.value)
                setStreamOutput('')
                setCurrentStep(0)
              }}
              buttonStyle="solid"
              size="large"
            >
              {(Object.keys(MODE_CONFIG) as Mode[]).map((m) => (
                <Radio.Button key={m} value={m}>
                  {MODE_CONFIG[m].icon} {MODE_CONFIG[m].label}
                </Radio.Button>
              ))}
            </Radio.Group>

            <Button
              type="primary"
              danger
              icon={<ThunderboltOutlined />}
              size="large"
              onClick={handleOneClickOptimize}
              loading={loading}
              disabled={mode !== 'optimize'}
            >
              一键测评+优化
            </Button>
          </div>
        </Card>

        <div style={{ display: 'flex', gap: 24, flexWrap: 'wrap' }}>
          {/* 左侧：简历编辑区 */}
          <div style={{ flex: 1, minWidth: 400 }}>
            <Card
              title={
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span>
                    <EditOutlined style={{ marginRight: 8 }} />
                    简历内容
                  </span>
                  <Space>
                    <Button size="small" icon={<CopyOutlined />} onClick={copyContent}>
                      复制
                    </Button>
                    <Button size="small" icon={<DeleteOutlined />} danger onClick={() => setResume({ content: '', evaluation: '', optimized: '' })}>
                      清空
                    </Button>
                  </Space>
                </div>
              }
              style={{ borderRadius: 12, minHeight: 600 }}
            >
              <Tabs activeKey={activeTab} onChange={(k) => setActiveTab(k as any)}>
                <Tabs.TabPane tab={<span><EditOutlined /> 编辑</span>} key="edit">
                  <TextArea
                    value={resume.content}
                    onChange={(e) => setResume(prev => ({ ...prev, content: e.target.value }))}
                    rows={25}
                    style={{ fontFamily: 'monospace', fontSize: 14 }}
                    placeholder="在此编辑简历内容（支持Markdown格式）..."
                  />
                </Tabs.TabPane>
                <Tabs.TabPane tab={<span><EyeOutlined /> 预览</span>} key="preview">
                  <div
                    style={{
                      padding: 24,
                      background: '#fff',
                      borderRadius: 8,
                      minHeight: 500,
                      border: '1px solid #f0f0f0',
                      fontSize: 14,
                      lineHeight: 1.8,
                    }}
                    className="markdown-preview"
                  >
                    {resume.content ? (
                      <ReactMarkdown>{resume.content}</ReactMarkdown>
                    ) : (
                      <Empty description="暂无内容，请先生成或导入简历" />
                    )}
                  </div>
                </Tabs.TabPane>
              </Tabs>
            </Card>
          </div>

          {/* 右侧：AI 功能区 */}
          <div style={{ flex: 1, minWidth: 400 }}>
            <Card
              title={
                <span>
                  <RobotOutlined style={{ marginRight: 8, color: '#1677ff' }} />
                  {MODE_CONFIG[mode].label}
                  <Tag color={MODE_CONFIG[mode].color} style={{ marginLeft: 12 }}>
                    {MODE_CONFIG[mode].tag}
                  </Tag>
                </span>
              }
              style={{ borderRadius: 12, minHeight: 600 }}
            >
              {/* 输入区域 */}
              {mode === 'generate' && (
                <div style={{ marginBottom: 16 }}>
                  <Text strong>请描述您的基本信息和求职意向</Text>
                  <Paragraph type="secondary" style={{ fontSize: 12, marginTop: 4 }}>
                    例如：姓名、年龄、学历、工作年限、目标岗位、擅长技术、项目经验等
                  </Paragraph>
                  <TextArea
                    value={inputText}
                    onChange={(e) => setInputText(e.target.value)}
                    rows={6}
                    placeholder="我叫张三，5年Java开发经验，擅长微服务架构..."
                    style={{ marginTop: 8 }}
                  />
                </div>
              )}

              {mode === 'evaluate' && (
                <div style={{ marginBottom: 16 }}>
                  <Text strong>左侧编辑区中的简历将被测评</Text>
                  <Paragraph type="secondary" style={{ fontSize: 12, marginTop: 4 }}>
                    请确保左侧已填写或导入需要测评的简历内容
                  </Paragraph>
                </div>
              )}

              {mode === 'optimize' && (
                <div style={{ marginBottom: 16 }}>
                  <Text strong>左侧编辑区中的简历将被优化</Text>
                  {resume.evaluation && (
                    <div style={{ marginTop: 8, padding: 12, background: '#fffbe6', borderRadius: 8, border: '1px solid #ffe58f' }}>
                      <Text strong style={{ fontSize: 12, color: '#d48806' }}>
                        <CheckCircleOutlined style={{ marginRight: 4 }} />
                        已加载测评反馈，优化将基于此反馈进行
                      </Text>
                    </div>
                  )}
                  {!resume.evaluation && (
                    <Paragraph type="secondary" style={{ fontSize: 12, marginTop: 4 }}>
                      建议先进行测评，再基于测评结果优化（也可直接优化）
                    </Paragraph>
                  )}
                </div>
              )}

              {/* 执行按钮 */}
              <div style={{ marginBottom: 16, textAlign: 'right' }}>
                {loading ? (
                  <Button danger onClick={cancelStream}>
                    取消
                  </Button>
                ) : (
                  <Button type="primary" icon={MODE_CONFIG[mode].icon} onClick={executeResumeAgent} size="large">
                    {mode === 'generate' ? '生成简历' : mode === 'evaluate' ? '开始测评' : '一键优化'}
                  </Button>
                )}
              </div>

              <Divider />

              {/* 输出区域 */}
              <div>
                <Text strong>AI 输出</Text>
                <Spin spinning={loading && !streamOutput}>
                  <div
                    style={{
                      marginTop: 8,
                      padding: 16,
                      background: '#f6ffed',
                      borderRadius: 8,
                      border: '1px solid #b7eb8f',
                      minHeight: 300,
                      maxHeight: 500,
                      overflow: 'auto',
                      whiteSpace: 'pre-wrap',
                      fontSize: 14,
                      lineHeight: 1.8,
                    }}
                  >
                    {streamOutput ? (
                      <ReactMarkdown>{streamOutput}</ReactMarkdown>
                    ) : mode === 'evaluate' && resume.evaluation ? (
                      <ReactMarkdown>{resume.evaluation}</ReactMarkdown>
                    ) : mode === 'optimize' && resume.optimized ? (
                      <ReactMarkdown>{resume.optimized}</ReactMarkdown>
                    ) : (
                      <Empty description="AI输出将在此显示" image={Empty.PRESENTED_IMAGE_SIMPLE} />
                    )}
                  </div>
                </Spin>
              </div>
            </Card>
          </div>
        </div>
      </div>

      {/* 导入弹窗 */}
      <Modal
        title="导入简历"
        open={showImportModal}
        onCancel={() => { setShowImportModal(false); setImportText(''); setFileList([]) }}
        footer={null}
        width={600}
      >
        <Tabs defaultActiveKey="text">
          <Tabs.TabPane tab="粘贴文本" key="text">
            <TextArea
              value={importText}
              onChange={(e) => setImportText(e.target.value)}
              rows={10}
              placeholder="将简历内容粘贴到此处..."
              style={{ marginBottom: 16 }}
            />
            <Button type="primary" block onClick={handleImportText}>
              确认导入
            </Button>
          </Tabs.TabPane>
          <Tabs.TabPane tab="上传文件" key="file">
            <Upload {...uploadProps}>
              <Button icon={<UploadOutlined />}>选择文件</Button>
            </Upload>
            <Text type="secondary" style={{ fontSize: 12, display: 'block', marginTop: 8, marginBottom: 16 }}>
              支持：TXT、Markdown、Word、PDF 格式（文件内容将由AI提取整理）
            </Text>
            <Button type="primary" block onClick={handleImportFile} loading={loading}>
              导入文件
            </Button>
          </Tabs.TabPane>
        </Tabs>
      </Modal>
    </div>
  )
}

export default ResumeBuilder
