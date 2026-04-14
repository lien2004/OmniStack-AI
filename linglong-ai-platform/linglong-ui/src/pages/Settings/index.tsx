import { useState, useEffect, SetStateAction} from 'react'
import {Card, Form, Input, Button, Select, Switch, Tabs, message, Space, Table, Tag, Modal, InputNumber} from 'antd'
import {
  SettingOutlined,
  SafetyOutlined,
  DatabaseOutlined,
  ApiOutlined,
  PlusOutlined,
  DeleteOutlined,
  EditOutlined,
} from '@ant-design/icons'
import {
  getPlatformSettings,
  savePlatformSettings,
  testDatabaseConnection,
  getDatabaseInfo,
  getLlmProviders,
  saveLlmProvider,
  deleteLlmProvider,
  getDatabaseConfigs,
  saveDatabaseConfig,
  deleteDatabaseConfig,
  saveSecuritySettings
} from '../../services/settings'

const {TabPane} = Tabs

// LLM厂商配置
const LLM_PROVIDERS = [
  {value: 'openai', label: 'OpenAI', models: ['gpt-4', 'gpt-4-turbo', 'gpt-3.5-turbo', 'gpt-4o', 'gpt-4o-mini']},
  {
    value: 'anthropic',
    label: 'Anthropic Claude',
    models: ['claude-3-opus', 'claude-3-sonnet', 'claude-3-haiku', 'claude-3-5-sonnet']
  },
  {value: 'google', label: 'Google Gemini', models: ['gemini-pro', 'gemini-pro-vision', 'gemini-ultra']},
  {value: 'baidu', label: '百度文心一言', models: ['ernie-bot', 'ernie-bot-turbo', 'ernie-bot-4']},
  {value: 'aliyun', label: '阿里通义千问', models: ['qwen-turbo', 'qwen-plus', 'qwen-max', 'qwen-long']},
  {value: 'doubao', label: '字节豆包', models: ['doubao-lite', 'doubao-pro', 'doubao-vision']},
  {value: 'deepseek', label: 'DeepSeek', models: ['deepseek-chat', 'deepseek-coder', 'deepseek-reasoner']},
  {value: 'moonshot', label: 'Kimi (月之暗面)', models: ['moonshot-v1-8k', 'moonshot-v1-32k', 'moonshot-v1-128k']},
  {
    value: 'meta',
    label: 'Meta Llama',
    models: ['llama-2-7b', 'llama-2-13b', 'llama-2-70b', 'llama-3-8b', 'llama-3-70b']
  },
  {value: 'mistral', label: 'Mistral AI', models: ['mistral-tiny', 'mistral-small', 'mistral-medium', 'mistral-large']},
  {value: 'zhipu', label: '智谱 GLM', models: ['glm-4', 'glm-4v', 'glm-3-turbo', 'chatglm-turbo']},
  {value: 'qwen', label: '通义千问 (独立)', models: ['qwen-7b', 'qwen-14b', 'qwen-72b', 'qwen-110b']},
  {value: 'yi', label: '零一万物 Yi', models: ['yi-6b', 'yi-34b', 'yi-34b-chat']},
  {value: 'baichuan', label: '百川智能', models: ['baichuan2-7b', 'baichuan2-13b', 'baichuan2-53b']},
  {value: 'ollama', label: 'Ollama (本地)', models: ['llama2', 'llama3', 'mistral', 'codellama', 'vicuna']},
  {value: 'azure', label: 'Azure OpenAI', models: ['gpt-4', 'gpt-4-turbo', 'gpt-35-turbo']},
  {value: 'cohere', label: 'Cohere', models: ['command', 'command-light', 'command-nightly']},
  {value: 'ai21', label: 'AI21 Labs', models: ['j2-ultra', 'j2-mid', 'j2-light']},
]

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
  const [selectedProvider, setSelectedProvider] = useState('openai')
  const [, setSelectedDbType] = useState('postgresql')
  const [loading, setLoading] = useState(false)
  const [testResult, setTestResult] = useState<any>(null)
  const [dbInfoModalVisible, setDbInfoModalVisible] = useState(false)
  const [currentDbInfo, setCurrentDbInfo] = useState<any>(null)

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

      const providers = await getLlmProviders()
      setLlmProviders(providers || [])

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
    llmModalForm.resetFields()
    llmModalForm.setFieldsValue({
      provider: 'openai',
      model: 'gpt-4',
      temperature: 0.7,
      maxTokens: 4096,
      isDefault: false
    })
    setSelectedProvider('openai')
    setLlmModalVisible(true)
  }

  const handleEditLlmProvider = (record: SetStateAction<null>) => {
    setEditingProvider(record)
    llmModalForm.setFieldsValue(record)
    // @ts-ignore
    setSelectedProvider(record.provider)
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
      const saved = await saveLlmProvider({ ...values, id: editingProvider?.id })
      
      if (editingProvider) {
        setLlmProviders(llmProviders.map(p => p.id === editingProvider.id ? saved : p))
      } else {
        setLlmProviders([...llmProviders, saved])
      }
      
      setLlmModalVisible(false)
      message.success('保存成功')
    } catch (error) {
      message.error('保存失败')
    }
  }

  const handleProviderChange = (value: string) => {
    setSelectedProvider(value)
    const provider = LLM_PROVIDERS.find(p => p.value === value)
    if (provider) {
      llmModalForm.setFieldsValue({ model: provider.models[0] })
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

  // LLM表格列
  const llmColumns = [
    { title: '厂商', dataIndex: 'provider', key: 'provider', render: (v: string) => LLM_PROVIDERS.find(p => p.value === v)?.label || v },
    { title: '模型', dataIndex: 'model', key: 'model' },
    { title: 'API Key', dataIndex: 'apiKey', key: 'apiKey', render: (v: string) => v ? '••••••••' + v.slice(-4) : '-' },
    { title: '默认', dataIndex: 'isDefault', key: 'isDefault', render: (v: boolean) => v ? <Tag color="blue">默认</Tag> : null },
    { title: '操作', key: 'action', render: (_: any, record: any) => (
      <Space>
        <Button type="link" icon={<EditOutlined />} onClick={() => handleEditLlmProvider(record)}>编辑</Button>
        <Button type="link" danger icon={<DeleteOutlined />} onClick={() => handleDeleteLlmProvider(record.id)}>删除</Button>
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

  const currentProvider = LLM_PROVIDERS.find(p => p.value === selectedProvider)

  return (
    <div className="settings">
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

          <TabPane
            tab={
              <span>
                <ApiOutlined /> LLM配置
              </span>
            }
            key="llm"
          >
            <div style={{ marginBottom: 16 }}>
              <Button type="primary" icon={<PlusOutlined />} onClick={handleAddLlmProvider}>
                添加LLM厂商
              </Button>
            </div>
            <Table 
              columns={llmColumns} 
              dataSource={llmProviders} 
              rowKey="id"
              pagination={false}
            />
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

      {/* LLM厂商配置弹窗 */}
      <Modal
        title={editingProvider ? '编辑LLM配置' : '添加LLM配置'}
        open={llmModalVisible}
        onOk={handleSaveLlmProvider}
        onCancel={() => setLlmModalVisible(false)}
        width={600}
      >
        <Form form={llmModalForm} layout="vertical">
          <Form.Item
            label="模型提供商"
            name="provider"
            rules={[{ required: true }]}
          >
            <Select onChange={handleProviderChange}>
              {LLM_PROVIDERS.map(p => (
                <Select.Option key={p.value} value={p.value}>{p.label}</Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item
            label="模型"
            name="model"
            rules={[{ required: true }]}
          >
            <Select>
              {currentProvider?.models.map(m => (
                <Select.Option key={m} value={m}>{m}</Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item
            label="API Key"
            name="apiKey"
            rules={[{ required: true, message: '请输入API Key' }]}
          >
            <Input.Password placeholder="请输入API Key" />
          </Form.Item>
          <Form.Item
            label="Base URL (可选)"
            name="baseUrl"
          >
            <Input placeholder="自定义API基础URL，如使用代理" />
          </Form.Item>
          <Form.Item
            label="温度参数 (Temperature)"
            name="temperature"
            initialValue={0.7}
          >
            <InputNumber min={0} max={2} step={0.1} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item
            label="最大Token数"
            name="maxTokens"
            initialValue={4096}
          >
            <InputNumber min={100} max={32000} step={100} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item
            label="设为默认"
            name="isDefault"
            valuePropName="checked"
          >
            <Switch />
          </Form.Item>
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
  )
}

export default Settings
