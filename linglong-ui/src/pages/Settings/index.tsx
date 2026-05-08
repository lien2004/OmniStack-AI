import { useState, useEffect } from 'react'
import {Card, Form, Input, Button, Select, Switch, Tabs, message, Space, Table, Tag, Modal, InputNumber, Tooltip, Popconfirm, Badge, Divider, Alert} from 'antd'
import {
  SettingOutlined,
  SafetyOutlined,
  DatabaseOutlined,
  ApiOutlined,
  PlusOutlined,
  DeleteOutlined,
  EditOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  ExperimentOutlined,
  ThunderboltOutlined,
  GlobalOutlined,
  LaptopOutlined,
  StarOutlined,
  StarFilled,
  ReloadOutlined,
} from '@ant-design/icons'
import {
  getPlatformSettings,
  savePlatformSettings,
  testDatabaseConnection,
  getDatabaseInfo,
  getLlmProviders,
  saveLlmProvider,
  deleteLlmProvider,
  toggleLlmProviderEnabled,
  setLlmProviderDefault,
  getDatabaseConfigs,
  saveDatabaseConfig,
  deleteDatabaseConfig,
  saveSecuritySettings,
  testLlmConnection,
} from '../../services/settings'

const {TabPane} = Tabs

// LLM厂商配置
// vendor标识 → {显示名、预设基址、预置模型列表}
const VENDOR_PRESETS: Record<string, { label: string; baseUrl: string; models: string[]; icon: string; color: string }> = {
  openai:    { label: 'OpenAI', baseUrl: 'https://api.openai.com/v1', icon: '🤖', color: '#10a37f',
               models: ['gpt-4o', 'gpt-4o-mini', 'gpt-4-turbo', 'gpt-4', 'gpt-3.5-turbo', 'o1', 'o1-mini'] },
  anthropic: { label: 'Anthropic Claude', baseUrl: 'https://api.anthropic.com/v1', icon: '🧡', color: '#d97706',
               models: ['claude-opus-4-7', 'claude-sonnet-4-6', 'claude-opus-4-6', 'claude-3-5-sonnet-20241022', 'claude-3-haiku-20240307'] },
  zhipu:     { label: '智谱AI (GLM)', baseUrl: 'https://open.bigmodel.cn/api/paas/v4', icon: '🐉', color: '#1677ff',
               models: ['glm-5.1', 'glm-5', 'glm-5-turbo', 'glm-4.7', 'glm-4.6', 'glm-4.5', 'glm-4.5-air'] },
  deepseek:  { label: 'DeepSeek', baseUrl: 'https://api.deepseek.com/v1', icon: '🔭', color: '#722ed1',
               models: ['deepseek-v4-pro', 'deepseek-v4-flash', 'deepseek-chat', 'deepseek-reasoner'] },
  moonshot:  { label: 'Kimi (月之暗面)', baseUrl: 'https://api.moonshot.cn/v1', icon: '🌙', color: '#17aeae',
               models: ['moonshot-v1-128k', 'moonshot-v1-32k', 'moonshot-v1-8k'] },
  aliyun:    { label: '阿里通义千问', baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1', icon: '☁️', color: '#f97316',
               models: ['qwen-max', 'qwen-plus', 'qwen-turbo', 'qwen-coder-plus', 'qwen-long'] },
  doubao:    { label: '字节豆包', baseUrl: 'https://ark.cn-beijing.volces.com/api/v3', icon: '🫘', color: '#3b82f6',
               models: ['doubao-pro-32k', 'doubao-pro-4k', 'doubao-lite-32k'] },
  baidu:     { label: '百度文心一言', baseUrl: 'https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat', icon: '🌊', color: '#2563eb',
               models: ['ernie-4.0-8k', 'ernie-4.0-turbo-8k', 'ernie-bot'] },
  ollama:    { label: 'Ollama (本地)', baseUrl: 'http://localhost:11434/v1', icon: '💻', color: '#52c41a',
               models: ['llama3', 'llama3.1', 'mistral', 'codellama', 'qwen2', 'gemma2'] },
  codeflow:  { label: 'CodeFlow / 灵龙AI', baseUrl: 'https://codeflow.asia/v1', icon: '🚀', color: '#8b5cf6',
               models: ['gpt-5.5', 'gpt-5.4', 'gpt-5.4-mini', 'claude-opus-4-7', 'claude-sonnet-4-6'] },
  custom:    { label: '自建模型', baseUrl: '', icon: '⚙️', color: '#8c8c8c', models: [] },
}

const VENDOR_OPTIONS = Object.entries(VENDOR_PRESETS).map(([value, v]) => ({ value, label: `${v.icon} ${v.label}` }))

const PROVIDER_TYPE_MAP: Record<string, { label: string; color: string; icon: any }> = {
  commercial:   { label: '商用', color: '#1677ff', icon: <ThunderboltOutlined /> },
  open_source:  { label: '开源', color: '#52c41a', icon: <GlobalOutlined /> },
  self_hosted:  { label: '自建', color: '#fa8c16', icon: <LaptopOutlined /> },
}

// 数据库类型配置
const DATABASE_TYPES = [
  {value: 'postgresql', label: 'PostgreSQL', defaultPort: 5432},
  {value: 'mysql', label: 'MySQL', defaultPort: 3306},
  {value: 'mariadb', label: 'MariaDB', defaultPort: 3306},
  {value: 'sqlserver', label: 'SQL Server', defaultPort: 1433},
  {value: 'oracle', label: 'Oracle', defaultPort: 1521},
  {value: 'mongodb', label: 'MongoDB', defaultPort: 27017},
  {value: 'redis', label: 'Redis', defaultPort: 6379},
  {value: 'sqlite', label: 'SQLite', defaultPort: null},
  {value: 'clickhouse', label: 'ClickHouse', defaultPort: 8123},
  {value: 'elasticsearch', label: 'Elasticsearch', defaultPort: 9200},
]

function Settings() {
  const [generalForm] = Form.useForm()
  const [securityForm] = Form.useForm()
  const [llmModalForm] = Form.useForm()
  const [dbModalForm] = Form.useForm()

  const [activeTab, setActiveTab] = useState('general')
  const [llmProviders, setLlmProviders] = useState<any[]>([])
  const [databaseConfigs, setDatabaseConfigs] = useState<any[]>([])
  const [llmModalVisible, setLlmModalVisible] = useState(false)
  const [dbModalVisible, setDbModalVisible] = useState(false)
  const [editingProvider, setEditingProvider] = useState<any>(null)
  const [editingDbConfig, setEditingDbConfig] = useState<any>(null)
  const [selectedVendor, setSelectedVendor] = useState('openai')
  const [, setSelectedDbType] = useState('postgresql')
  const [loading, setLoading] = useState(false)
  const [testLoading, setTestLoading] = useState(false)
  const [testResult, setTestResult] = useState<any>(null)
  const [llmTestResult, setLlmTestResult] = useState<any>(null)
  const [dbInfoModalVisible, setDbInfoModalVisible] = useState(false)
  const [currentDbInfo, setCurrentDbInfo] = useState<any>(null)
  const [llmRefreshing, setLlmRefreshing] = useState(false)

  useEffect(() => {
    loadSettings()
  }, [])

  const loadSettings = async () => {
    try {
      const settings = await getPlatformSettings()
      if (settings) {
        generalForm.setFieldsValue(settings.general || {})
        securityForm.setFieldsValue(settings.security || {})
      }

      let providers = await getLlmProviders()
      providers = providers || []

      // 自动初始化通义千问（如未配置）
      const hasAliyun = providers.some((p: any) => p.vendor === 'aliyun')
      if (!hasAliyun) {
        const aliyunPreset = VENDOR_PRESETS.aliyun
        const newProvider = {
          id: Date.now().toString(),
          name: aliyunPreset.label,
          vendor: 'aliyun',
          providerType: 'commercial',
          baseUrl: aliyunPreset.baseUrl,
          apiKey: 'sk-0d90148f24b0484c8f2d5cf0098656cb',
          defaultModel: 'qwen-max',
          customModels: aliyunPreset.models,
          temperature: 0.7,
          maxTokens: 4000,
          topP: 1.0,
          contextLength: 32000,
          streamEnabled: true,
          timeoutSeconds: 120,
          isDefault: false,
          enabled: true,
        }
        try {
          await saveLlmProvider(newProvider)
          providers = [...providers, newProvider]
          message.success('已自动添加阿里通义千问配置')
        } catch {
          // 本地模式：直接加入列表
          providers = [...providers, newProvider]
        }
      }

      setLlmProviders(providers)

      const dbConfigs = await getDatabaseConfigs()
      setDatabaseConfigs(dbConfigs || [])
    } catch (error) {
      console.error('加载设置失败:', error)
    }
  }

  // 基本设置保存
  const handleSaveGeneral = async () => {
    try {
      const values = await generalForm.validateFields()
      console.log('保存基本设置:', values)
      await savePlatformSettings({general: values})
      message.success('基本设置已保存')
    } catch (error) {
      console.error('保存失败:', error)
      // @ts-ignore
      message.error('保存失败: ' + (error.message || '未知错误'))
    }
  }

  // LLM配置相关
  const handleAddLlmProvider = () => {
    setEditingProvider(null)
    setLlmTestResult(null)
    llmModalForm.resetFields()
    llmModalForm.setFieldsValue({
      vendor: 'openai',
      providerType: 'commercial',
      temperature: 0.7,
      maxTokens: 4000,
      topP: 1.0,
      contextLength: 32000,
      streamEnabled: true,
      timeoutSeconds: 120,
      isDefault: false,
      enabled: true,
    })
    setSelectedVendor('openai')
    setLlmModalVisible(true)
  }

  const handleEditLlmProvider = (record: any) => {
    setEditingProvider(record)
    setLlmTestResult(null)
    const formData = {
      ...record,
      customModels: Array.isArray(record.customModels)
        ? record.customModels.join(',')
        : record.customModels || '',
    }
    llmModalForm.setFieldsValue(formData)
    setSelectedVendor(record.vendor || 'custom')
    setLlmModalVisible(true)
  }

  const handleDeleteLlmProvider = async (id: string) => {
    try {
      await deleteLlmProvider(id)
      setLlmProviders(llmProviders.filter(p => p.id !== id))
      message.success('删除成功')
    } catch (error) {
      message.error('删除失败')
    }
  }

  const handleSaveLlmProvider = async () => {
    try {
      const values = await llmModalForm.validateFields()
      // 将逽号分隔的字符串转为数组
      if (typeof values.customModels === 'string') {
        values.customModels = values.customModels.split(',').map((s: string) => s.trim()).filter(Boolean)
      }
      const saved = await saveLlmProvider({ ...values, id: editingProvider?.id })
      if (editingProvider) {
        setLlmProviders(llmProviders.map(p => p.id === editingProvider.id ? (saved || { ...values, id: editingProvider.id }) : p))
      } else {
        setLlmProviders([...llmProviders, saved || { ...values, id: Date.now().toString() }])
      }
      setLlmModalVisible(false)
      message.success('保存成功')
    } catch (error: any) {
      if (error?.errorFields) return // 表单校验错误
      message.error('保存失败')
    }
  }

  const handleVendorChange = (value: string) => {
    setSelectedVendor(value)
    const preset = VENDOR_PRESETS[value]
    if (preset) {
      llmModalForm.setFieldsValue({
        baseUrl: preset.baseUrl,
        defaultModel: preset.models[0] || '',
      })
    }
  }

  const handleToggleEnabled = async (id: string, enabled: boolean) => {
    await toggleLlmProviderEnabled(id, enabled)
    setLlmProviders(llmProviders.map(p => p.id === id ? { ...p, enabled } : p))
    message.success(enabled ? '已启用' : '已禁用')
  }

  const handleSetDefault = async (id: string) => {
    await setLlmProviderDefault(id)
    setLlmProviders(llmProviders.map(p => ({ ...p, isDefault: p.id === id })))
    message.success('已设为默认供应商')
  }

  const handleTestLlmConnection = async () => {
    try {
      const values = llmModalForm.getFieldsValue()
      if (!values.apiKey) {
        setLlmTestResult({ success: false, message: '请先填写 API Key' })
        return
      }
      setTestLoading(true)
      setLlmTestResult(null)
      const result = await testLlmConnection(values)
      setLlmTestResult(result)
    } finally {
      setTestLoading(false)
    }
  }

  const handleRefreshLlm = async () => {
    setLlmRefreshing(true)
    try {
      const providers = await getLlmProviders()
      setLlmProviders(providers || [])
      message.success('已刷新')
    } finally {
      setLlmRefreshing(false)
    }
  }

  // 数据库配置相关
  const handleAddDbConfig = () => {
    setEditingDbConfig(null)
    dbModalForm.resetFields()
    dbModalForm.setFieldsValue({ 
      dbType: 'postgresql',
      host: 'localhost',
      port: 5432,
      database: 'linglong',
      username: 'admin'
    })
    setSelectedDbType('postgresql')
    setDbModalVisible(true)
  }

  const handleEditDbConfig = (record: any) => {
    setEditingDbConfig(record)
    dbModalForm.setFieldsValue(record)
    setSelectedDbType(record.dbType)
    setDbModalVisible(true)
  }

  const handleDeleteDbConfig = async (id: string) => {
    try {
      await deleteDatabaseConfig(id)
      setDatabaseConfigs(databaseConfigs.filter(d => d.id !== id))
      message.success('删除成功')
    } catch (error) {
      message.error('删除失败')
    }
  }

  const handleDbTypeChange = (value: string) => {
    setSelectedDbType(value)
    const dbType = DATABASE_TYPES.find(d => d.value === value)
    if (dbType && dbType.defaultPort) {
      dbModalForm.setFieldsValue({ port: dbType.defaultPort })
    }
  }

  const handleTestDbConnection = async () => {
    try {
      setLoading(true)
      setTestResult(null)
      const values = await dbModalForm.validateFields()
      const result = await testDatabaseConnection(values)
      setTestResult(result)
      if (result.success) {
        message.success(`连接测试成功！${result.databaseProductName || ''} ${result.databaseProductVersion || ''}`)
      } else {
        message.error(`连接测试失败: ${result.message}`)
      }
    } catch (error) {
      message.error('连接测试失败')
    } finally {
      setLoading(false)
    }
  }
  
  const handleViewDbInfo = async (record: any) => {
    try {
      console.log('获取数据库详情:', record.id, record)
      
      // 如果是本地存储的数据（id是时间戳格式），直接显示基本信息
      if (record.id && record.id.length > 10 && !isNaN(Number(record.id))) {
        setCurrentDbInfo({
          productName: DATABASE_TYPES.find(d => d.value === record.dbType)?.label || record.dbType,
          productVersion: '本地存储模式',
          driverName: '本地驱动',
          driverVersion: '-',
          tableCount: 0,
          tables: [],
          note: '此配置存储在本地，无法获取远程数据库信息。请确保后端服务已启动并重新保存配置。'
        })
        setDbInfoModalVisible(true)
        return
      }
      
      const info = await getDatabaseInfo(record.id)
      console.log('数据库详情结果:', info)
      
      if (info) {
        setCurrentDbInfo(info)
        setDbInfoModalVisible(true)
      } else {
        message.error('获取数据库信息失败，请检查后端服务是否启动')
      }
    } catch (error: any) {
      console.error('获取数据库信息失败:', error)
      message.error('获取数据库信息失败: ' + (error.message || '未知错误'))
    }
  }

  const handleSaveDbConfig = async () => {
    try {
      const values = await dbModalForm.validateFields()
      const saved = await saveDatabaseConfig({ ...values, id: editingDbConfig?.id })
      
      if (editingDbConfig) {
        setDatabaseConfigs(databaseConfigs.map(d => d.id === editingDbConfig.id ? saved : d))
      } else {
        setDatabaseConfigs([...databaseConfigs, saved])
      }
      
      setDbModalVisible(false)
      message.success('保存成功')
    } catch (error) {
      message.error('保存失败')
    }
  }

  // 安全设置保存
  const handleSaveSecurity = async () => {
    try {
      const values = await securityForm.validateFields()
      await saveSecuritySettings(values)
      message.success('安全设置已保存')
    } catch (error) {
      message.error('保存失败')
    }
  }

  // LLM表格列（列表模式）
  const llmColumns = [
    { title: '名称', dataIndex: 'name', key: 'name',
      render: (v: string, r: any) => {
        const preset = VENDOR_PRESETS[r.vendor] || VENDOR_PRESETS.custom
        return (
          <Space>
            <span style={{ fontSize: 18 }}>{preset.icon}</span>
            <div>
              <div style={{ fontWeight: 600 }}>{v}</div>
              <div style={{ fontSize: 11, color: '#8c8c8c' }}>{preset.label}</div>
            </div>
          </Space>
        )
      }
    },
    { title: '默认模型', dataIndex: 'defaultModel', key: 'defaultModel' },
    { title: 'API Key', dataIndex: 'apiKeyMasked', key: 'apiKeyMasked',
      render: (v: string, r: any) => v || (r.apiKey ? '••••••••' + (r.apiKey || '').slice(-4) : <span style={{ color: '#d9d9d9' }}>未设置</span>) },
    { title: '类型', dataIndex: 'providerType', key: 'providerType',
      render: (v: string) => {
        const t = PROVIDER_TYPE_MAP[v] || PROVIDER_TYPE_MAP.commercial
        return <Tag color={t.color}>{t.icon} {t.label}</Tag>
      }
    },
    { title: '状态', dataIndex: 'enabled', key: 'enabled',
      render: (v: boolean, r: any) => (
        <Space>
          <Switch size="small" checked={v} onChange={e => handleToggleEnabled(r.id, e)} />
          {r.isDefault && <Tag color="gold"><StarFilled style={{ marginRight: 3 }} />默认</Tag>}
        </Space>
      )
    },
    { title: '路由关键词', dataIndex: 'routingKeywords', key: 'routingKeywords',
      render: (v: string) => v ? v.split(',').map((k: string) => <Tag key={k} style={{ fontSize: 11 }}>{k.trim()}</Tag>) : <span style={{ color: '#d9d9d9' }}>-</span>
    },
    { title: '操作', key: 'action', render: (_: any, record: any) => (
      <Space>
        <Tooltip title="设为默认">
          <Button type="text" icon={record.isDefault ? <StarFilled style={{ color: '#faad14' }} /> : <StarOutlined />}
            onClick={() => handleSetDefault(record.id)} />
        </Tooltip>
        <Button type="link" icon={<EditOutlined />} onClick={() => handleEditLlmProvider(record)}>编辑</Button>
        <Popconfirm title="确认删除此供应商？" onConfirm={() => handleDeleteLlmProvider(record.id)} okText="删除" cancelText="取消">
          <Button type="link" danger icon={<DeleteOutlined />}>删除</Button>
        </Popconfirm>
      </Space>
    )},
  ]

  // 数据库表格列
  const dbColumns = [
    { title: '数据库类型', dataIndex: 'dbType', key: 'dbType', render: (v: string) => DATABASE_TYPES.find(d => d.value === v)?.label || v },
    { title: '主机', dataIndex: 'host', key: 'host' },
    { title: '端口', dataIndex: 'port', key: 'port' },
    { title: '数据库名', dataIndex: 'database', key: 'database' },
    { title: '用户名', dataIndex: 'username', key: 'username' },
    { title: '状态', dataIndex: 'status', key: 'status', render: (v: string) => v === 'connected' ? <Tag color="green">已连接</Tag> : <Tag>未测试</Tag> },
    { title: '操作', key: 'action', render: (_: any, record: any) => (
      <Space>
        <Button type="link" onClick={() => handleViewDbInfo(record)}>详情</Button>
        <Button type="link" icon={<EditOutlined />} onClick={() => handleEditDbConfig(record)}>编辑</Button>
        <Button type="link" danger icon={<DeleteOutlined />} onClick={() => handleDeleteDbConfig(record.id)}>删除</Button>
      </Space>
    )},
  ]

  return (
    <div className="settings">
      {/* 页面标题区 */}
      <div className="g-page-header">
        <div className="g-page-header-left">
          <h1 className="g-page-title">模型管理</h1>
          <p className="g-page-subtitle">管理 LLM 供应商配置与参数，支持多模型切换与性能对比</p>
        </div>
      </div>
      <div style={{ padding: '24px 32px' }}>
      <Card>
        <Tabs activeKey={activeTab} onChange={setActiveTab}>
          <TabPane
            tab={
              <span>
                <SettingOutlined /> 基本设置
              </span>
            }
            key="general"
          >
            <Form
              form={generalForm}
              layout="vertical"
              style={{ maxWidth: 600 }}
              initialValues={{ platformName: '玲珑AI平台', notification: true, autoSave: true }}
            >
              <Form.Item
                label="平台名称"
                name="platformName"
                rules={[{ required: true, message: '请输入平台名称' }]}
              >
                <Input placeholder="请输入平台名称" />
              </Form.Item>
              <Form.Item
                label="系统通知"
                name="notification"
                valuePropName="checked"
              >
                <Switch />
              </Form.Item>
              <Form.Item
                label="自动保存"
                name="autoSave"
                valuePropName="checked"
              >
                <Switch />
              </Form.Item>
              <Form.Item>
                <Button type="primary" onClick={handleSaveGeneral}>
                  保存设置
                </Button>
              </Form.Item>
            </Form>
          </TabPane>

          <TabPane tab={<span><ApiOutlined /> LLM配置</span>} key="llm">
            {/* 顶部操作栏 */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
              <Space>
                <Button type="primary" icon={<PlusOutlined />} onClick={handleAddLlmProvider}>添加供应商</Button>
                <Button icon={<ReloadOutlined />} loading={llmRefreshing} onClick={handleRefreshLlm}>刷新</Button>
              </Space>
              <Space>
                <Badge count={llmProviders.filter(p => p.enabled).length} style={{ backgroundColor: '#52c41a' }}>
                  <Tag color="green">已启用 {llmProviders.filter(p => p.enabled).length}</Tag>
                </Badge>
                <Tag color="default">共 {llmProviders.length} 个供应商</Tag>
              </Space>
            </div>

            {/* 供应商卡片网格 */}
            {llmProviders.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '48px 0', color: '#8c8c8c' }}>
                <ApiOutlined style={{ fontSize: 48, marginBottom: 16 }} />
                <div style={{ fontSize: 16, marginBottom: 8 }}>暂无供应商配置</div>
                <div style={{ fontSize: 13 }}>点击『添加供应商』开始配置您的大语言模型</div>
              </div>
            ) : (
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(380px, 1fr))', gap: 16 }}>
                {llmProviders.map(p => {
                  const preset = VENDOR_PRESETS[p.vendor] || VENDOR_PRESETS.custom
                  const typeInfo = PROVIDER_TYPE_MAP[p.providerType] || PROVIDER_TYPE_MAP.commercial
                  return (
                    <Card
                      key={p.id}
                      size="small"
                      style={{ borderRadius: 12, border: p.isDefault ? `2px solid ${preset.color}` : undefined,
                               opacity: p.enabled ? 1 : 0.55 }}
                      bodyStyle={{ padding: '16px 20px' }}
                    >
                      {/* 卡片头部 */}
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 12 }}>
                        <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
                          <div style={{ width: 44, height: 44, borderRadius: 10,
                            background: `${preset.color}20`, display: 'flex', alignItems: 'center',
                            justifyContent: 'center', fontSize: 22 }}>
                            {preset.icon}
                          </div>
                          <div>
                            <div style={{ fontWeight: 700, fontSize: 15 }}>{p.name}</div>
                            <div style={{ color: '#8c8c8c', fontSize: 12 }}>{preset.label}</div>
                          </div>
                        </div>
                        <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
                          {p.isDefault && <Tag color="gold" style={{ fontSize: 11 }}><StarFilled /> 默认</Tag>}
                          <Tag color={typeInfo.color} style={{ fontSize: 11 }}>{typeInfo.label}</Tag>
                          <Switch size="small" checked={p.enabled} onChange={e => handleToggleEnabled(p.id, e)} />
                        </div>
                      </div>

                      {/* 卡片内容信息 */}
                      <div style={{ fontSize: 13, color: '#595959' }}>
                        <div style={{ marginBottom: 6 }}>
                          🧠 模型：<Tag color="blue" style={{ fontSize: 12 }}>{p.defaultModel || '-'}</Tag>
                        </div>
                        {p.routingKeywords && (
                          <div style={{ marginBottom: 6 }}>
                            🎯 路由：{p.routingKeywords.split(',').map((k: string) => (
                              <Tag key={k} style={{ fontSize: 11, marginLeft: 4 }}>{k.trim()}</Tag>
                            ))}
                          </div>
                        )}
                        <div style={{ display: 'flex', gap: 16, marginTop: 8, fontSize: 12, color: '#8c8c8c' }}>
                          <span>🌡️ {p.temperature}</span>
                          <span>📝 {p.maxTokens} Tokens</span>
                          <span>Top-P: {p.topP}</span>
                          {p.streamEnabled && <span style={{ color: '#52c41a' }}>• 流式</span>}
                        </div>
                      </div>

                      <Divider style={{ margin: '12px 0' }} />
                      <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
                        <Tooltip title="设为默认">
                          <Button size="small" type="text" icon={p.isDefault ? <StarFilled style={{ color: '#faad14' }} /> : <StarOutlined />}
                            onClick={() => handleSetDefault(p.id)} />
                        </Tooltip>
                        <Button size="small" icon={<EditOutlined />} onClick={() => handleEditLlmProvider(p)}>编辑</Button>
                        <Popconfirm title="确认删除？" onConfirm={() => handleDeleteLlmProvider(p.id)} okText="删除" cancelText="取消">
                          <Button size="small" danger icon={<DeleteOutlined />}>删除</Button>
                        </Popconfirm>
                      </div>
                    </Card>
                  )
                })}
              </div>
            )}

            {/* 列表详细表格（可过滑查看） */}
            {llmProviders.length > 0 && (
              <div style={{ marginTop: 24 }}>
                <Divider orientation="left" style={{ fontSize: 13, color: '#8c8c8c' }}>详细列表</Divider>
                <Table columns={llmColumns} dataSource={llmProviders} rowKey="id"
                  size="small" pagination={false} scroll={{ x: 900 }} />
              </div>
            )}
          </TabPane>

          <TabPane
            tab={
              <span>
                <DatabaseOutlined /> 数据库配置
              </span>
            }
            key="database"
          >
            <div style={{ marginBottom: 16 }}>
              <Button type="primary" icon={<PlusOutlined />} onClick={handleAddDbConfig}>
                添加数据库连接
              </Button>
            </div>
            <Table 
              columns={dbColumns} 
              dataSource={databaseConfigs} 
              rowKey="id"
              pagination={false}
            />
          </TabPane>

          <TabPane
            tab={
              <span>
                <SafetyOutlined /> 安全设置
              </span>
            }
            key="security"
          >
            <Form 
              form={securityForm} 
              layout="vertical" 
              style={{ maxWidth: 600 }}
              initialValues={{ jwtEnabled: false, tokenExpireHours: 24, operationLogEnabled: false }}
            >
              <Form.Item
                label="启用JWT认证"
                name="jwtEnabled"
                valuePropName="checked"
              >
                <Switch />
              </Form.Item>
              <Form.Item
                label="Token过期时间(小时)"
                name="tokenExpireHours"
                rules={[{ required: true, message: '请输入Token过期时间' }]}
              >
                <InputNumber min={1} max={720} style={{ width: '100%' }} />
              </Form.Item>
              <Form.Item
                label="启用操作日志"
                name="operationLogEnabled"
                valuePropName="checked"
              >
                <Switch />
              </Form.Item>
              <Form.Item>
                <Button type="primary" onClick={handleSaveSecurity}>
                  保存安全设置
                </Button>
              </Form.Item>
            </Form>
          </TabPane>
        </Tabs>
      </Card>

      {/* LLM供应商配置弹窗（全面升级） */}
      <Modal
        title={editingProvider ? '编辑 LLM 供应商配置' : '添加 LLM 供应商'}
        open={llmModalVisible}
        onOk={handleSaveLlmProvider}
        onCancel={() => { setLlmModalVisible(false); setLlmTestResult(null) }}
        width={700}
        okText="保存"
        footer={[
          <Button key="test" icon={<ExperimentOutlined />} loading={testLoading} onClick={handleTestLlmConnection}>
            测试连接
          </Button>,
          <Button key="cancel" onClick={() => { setLlmModalVisible(false); setLlmTestResult(null) }}>取消</Button>,
          <Button key="save" type="primary" onClick={handleSaveLlmProvider}>保存</Button>,
        ]}
      >
        <Form form={llmModalForm} layout="vertical" style={{ maxHeight: '65vh', overflowY: 'auto', paddingRight: 4 }}>
          <Divider orientation="left" style={{ fontSize: 13 }}>基本信息</Divider>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0 16px' }}>
            <Form.Item label="供应商名称" name="name" rules={[{ required: true, message: '请输入名称' }]}>
              <Input placeholder="如：智谱AI GLM-5.1供应商" />
            </Form.Item>
            <Form.Item label="厂商" name="vendor" rules={[{ required: true }]}>
              <Select options={VENDOR_OPTIONS} onChange={handleVendorChange} />
            </Form.Item>
            <Form.Item label="供应商类型" name="providerType">
              <Select>
                <Select.Option value="commercial">👾 商用 (Commercial)</Select.Option>
                <Select.Option value="open_source">🌏 开源 (Open Source)</Select.Option>
                <Select.Option value="self_hosted">💻 自建 (Self-Hosted)</Select.Option>
              </Select>
            </Form.Item>
            <Form.Item label="默认模型" name="defaultModel" rules={[{ required: true, message: '请输入模型名' }]}>
              {selectedVendor && VENDOR_PRESETS[selectedVendor]?.models.length > 0 ? (
                <Select showSearch allowClear>
                  {VENDOR_PRESETS[selectedVendor].models.map(m => (
                    <Select.Option key={m} value={m}>{m}</Select.Option>
                  ))}
                </Select>
              ) : (
                <Input placeholder="输入模型名，如 llama3" />
              )}
            </Form.Item>
          </div>

          <Form.Item label="API Key" name="apiKey">
            <Input.Password placeholder="请输入 API Key（不修改可留空）" />
          </Form.Item>
          <Form.Item label="Base URL" name="baseUrl" tooltip="API 基础地址，自建或代理时修改">
            <Input placeholder="如: https://api.openai.com/v1" />
          </Form.Item>
          <Form.Item label="自定义模型列表" name="customModels" tooltip="除默认模型外，这些模型名也可被调用，逗号分隔">
            <Input.TextArea rows={2} placeholder="多个模型用逗号分隔，如: model-a,model-b,model-c" />
          </Form.Item>
          <Form.Item label="路由关键词前缀" name="routingKeywords" tooltip="消息路由时，模型名以这些前缀开头就路由到此供应商，逗号分隔">
            <Input placeholder="如: glm,chatglm 或 gpt,claude" />
          </Form.Item>

          <Divider orientation="left" style={{ fontSize: 13 }}>模型参数</Divider>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0 16px' }}>
            <Form.Item label={
              <span>Temperature <span style={{ color: '#8c8c8c', fontSize: 12 }}>(控制随机性, 0~2)</span></span>
            } name="temperature">
              <InputNumber min={0} max={2} step={0.05} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item label="Top-P" name="topP">
              <InputNumber min={0} max={1} step={0.05} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item label="Max Tokens (单次最大输出)" name="maxTokens">
              <InputNumber min={100} max={128000} step={500} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item label="Context Length (上下文窗口)" name="contextLength">
              <InputNumber min={1000} max={2000000} step={1000} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item label="超时时间 (秒)" name="timeoutSeconds">
              <InputNumber min={10} max={600} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item label="开启流式输出" name="streamEnabled" valuePropName="checked">
              <Switch />
            </Form.Item>
          </div>

          <Divider orientation="left" style={{ fontSize: 13 }}>其他设置</Divider>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0 16px' }}>
            <Form.Item label="设为默认供应商" name="isDefault" valuePropName="checked">
              <Switch />
            </Form.Item>
            <Form.Item label="启用此供应商" name="enabled" valuePropName="checked">
              <Switch />
            </Form.Item>
          </div>
          <Form.Item label="备注" name="remark">
            <Input.TextArea rows={2} placeholder="可选，说明此供应商的用途或配置背景" />
          </Form.Item>

          {/* 连接测试结果 */}
          {llmTestResult && (
            <Alert
              type={llmTestResult.success ? 'success' : 'error'}
              icon={llmTestResult.success ? <CheckCircleOutlined /> : <CloseCircleOutlined />}
              showIcon
              message={llmTestResult.success ? '连接测试成功' : '连接测试失败'}
              description={llmTestResult.message}
              style={{ marginTop: 8 }}
            />
          )}
        </Form>
      </Modal>

      {/* 数据库配置弹窗 */}
      <Modal
        title={editingDbConfig ? '编辑数据库配置' : '添加数据库配置'}
        open={dbModalVisible}
        onOk={handleSaveDbConfig}
        onCancel={() => setDbModalVisible(false)}
        width={600}
        footer={[
          <Button key="test" onClick={handleTestDbConnection} loading={loading}>
            测试连接
          </Button>,
          <Button key="cancel" onClick={() => setDbModalVisible(false)}>
            取消
          </Button>,
          <Button key="save" type="primary" onClick={handleSaveDbConfig}>
            保存
          </Button>,
        ]}
      >
        <Form form={dbModalForm} layout="vertical">
          <Form.Item
            label="数据库类型"
            name="dbType"
            rules={[{ required: true }]}
          >
            <Select onChange={handleDbTypeChange}>
              {DATABASE_TYPES.map(d => (
                <Select.Option key={d.value} value={d.value}>{d.label}</Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item
            label="主机地址"
            name="host"
            rules={[{ required: true, message: '请输入主机地址' }]}
          >
            <Input placeholder="localhost" />
          </Form.Item>
          <Form.Item
            label="端口"
            name="port"
            rules={[{ required: true, message: '请输入端口' }]}
          >
            <InputNumber style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item
            label="数据库名"
            name="database"
            rules={[{ required: true, message: '请输入数据库名' }]}
          >
            <Input placeholder="linglong" />
          </Form.Item>
          <Form.Item
            label="用户名"
            name="username"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input placeholder="admin" />
          </Form.Item>
          <Form.Item
            label="密码"
            name="password"
            rules={[{ required: true, message: '请输入密码' }]}
          >
            <Input.Password />
          </Form.Item>
          
          {/* 连接测试结果展示 */}
          {testResult && (
            <div style={{ marginTop: 16, padding: 12, backgroundColor: testResult.success ? '#f6ffed' : '#fff2f0', borderRadius: 4, border: `1px solid ${testResult.success ? '#b7eb8f' : '#ffccc7'}` }}>
              <div style={{ fontWeight: 'bold', color: testResult.success ? '#52c41a' : '#ff4d4f', marginBottom: 8 }}>
                {testResult.success ? '✓ 连接测试成功' : '✗ 连接测试失败'}
              </div>
              {testResult.success && (
                <>
                  <div>数据库: {testResult.databaseProductName} {testResult.databaseProductVersion}</div>
                  <div>驱动: {testResult.driverName} {testResult.driverVersion}</div>
                </>
              )}
              {!testResult.success && (
                <div style={{ color: '#ff4d4f' }}>{testResult.message}</div>
              )}
            </div>
          )}
        </Form>
      </Modal>
      
      {/* 数据库详情弹窗 */}
      <Modal
        title="数据库详情"
        open={dbInfoModalVisible}
        onCancel={() => setDbInfoModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setDbInfoModalVisible(false)}>
            关闭
          </Button>
        ]}
        width={700}
      >
        {currentDbInfo && (
          <div>
            {currentDbInfo.note && (
              <div style={{ marginBottom: 16, padding: 12, backgroundColor: '#fffbe6', border: '1px solid #ffe58f', borderRadius: 4, color: '#d48806' }}>
                <strong>提示:</strong> {currentDbInfo.note}
              </div>
            )}
            
            <div style={{ marginBottom: 16 }}>
              <h4>基本信息</h4>
              <p><strong>数据库产品:</strong> {currentDbInfo.productName} {currentDbInfo.productVersion}</p>
              <p><strong>驱动:</strong> {currentDbInfo.driverName} {currentDbInfo.driverVersion}</p>
              {currentDbInfo.tableCount !== undefined && (
                <p><strong>表数量:</strong> {currentDbInfo.tableCount}</p>
              )}
            </div>
            
            {currentDbInfo.tables && currentDbInfo.tables.length > 0 && (
              <div>
                <h4>表列表</h4>
                <div style={{ maxHeight: 300, overflow: 'auto', border: '1px solid #d9d9d9', borderRadius: 4, padding: 8 }}>
                  {currentDbInfo.tables.map((table: string) => (
                    <div key={table} style={{ padding: '4px 0', borderBottom: '1px solid #f0f0f0' }}>{table}</div>
                  ))}
                </div>
              </div>
            )}
            
            {currentDbInfo.error && (
              <div style={{ color: '#ff4d4f', padding: 12, backgroundColor: '#fff2f0', borderRadius: 4 }}>
                错误: {currentDbInfo.error}
              </div>
            )}
          </div>
        )}
      </Modal>
      </div>
    </div>
  )
}

export default Settings
