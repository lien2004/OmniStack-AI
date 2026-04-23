import api from './api'

// 本地存储键名
const STORAGE_KEYS = {
  PLATFORM: 'platform_settings',
  LLM_PROVIDERS: 'llm_providers',
  DATABASE_CONFIGS: 'database_configs',
  SECURITY: 'security_settings'
}

// 平台设置相关接口
export const getPlatformSettings = async () => {
  try {
    const response = await api.get('/settings/platform')
    if (response.data?.code === 200) {
      return response.data?.data
    }
  } catch (error) {
    console.log('后端API未就绪，使用本地存储')
  }
  // 从本地存储获取
  const local = localStorage.getItem(STORAGE_KEYS.PLATFORM)
  if (local) {
    return JSON.parse(local)
  }
  // 返回默认值
  return {
    general: {
      platformName: '灵龙AI智能平台',
      notification: true,
      autoSave: true
    }
  }
}

export const savePlatformSettings = async (settings: any) => {
  try {
    const response = await api.post('/settings/platform', settings)
    if (response.data?.code === 200) {
      return response.data?.data
    }
  } catch (error) {
    console.log('后端API未就绪，保存到本地存储')
  }
  // 保存到本地存储
  localStorage.setItem(STORAGE_KEYS.PLATFORM, JSON.stringify(settings))
  return settings
}

// LLM厂商配置相关接口
export const getLlmProviders = async () => {
  try {
    const response = await api.get('/settings/llm/providers')
    if (response.data?.code === 200) {
      return response.data?.data
    }
  } catch (error) {
    console.log('后端API未就绪，使用本地存储')
  }
  const local = localStorage.getItem(STORAGE_KEYS.LLM_PROVIDERS)
  return local ? JSON.parse(local) : []
}

export const saveLlmProvider = async (provider: any) => {
  try {
    const response = await api.post('/settings/llm/providers', provider)
    if (response.data?.code === 200) {
      return response.data?.data
    }
  } catch (error) {
    console.log('后端API未就绪，保存到本地存储')
  }
  // 本地存储模式
  const providers = await getLlmProviders()
  if (provider.id) {
    const index = providers.findIndex((p: any) => p.id === provider.id)
    if (index > -1) {
      providers[index] = { ...provider }
    }
  } else {
    provider.id = Date.now().toString()
    providers.push(provider)
  }
  localStorage.setItem(STORAGE_KEYS.LLM_PROVIDERS, JSON.stringify(providers))
  return provider
}

export const deleteLlmProvider = async (id: string) => {
  try {
    const response = await api.delete(`/settings/llm/providers/${id}`)
    if (response.data?.code === 200) {
      return response.data
    }
  } catch (error) {
    console.log('后端API未就绪，从本地存储删除')
  }
  const providers = await getLlmProviders()
  const filtered = providers.filter((p: any) => p.id !== id)
  localStorage.setItem(STORAGE_KEYS.LLM_PROVIDERS, JSON.stringify(filtered))
}

export const testLlmConnection = async (provider: any) => {
  try {
    const response = await api.post('/settings/llm/test', provider)
    if (response.data?.code === 200) {
      return response.data?.data
    }
  } catch (error) {
    console.log('后端API未就绪，模拟测试成功')
  }
  // 模拟测试
  return { success: true, message: '连接测试成功（本地模式）' }
}

// 数据库配置相关接口
export const getDatabaseConfigs = async () => {
  try {
    const response = await api.get('/settings/database')
    if (response.data?.code === 200) {
      return response.data?.data
    }
  } catch (error) {
    console.log('后端API未就绪，使用本地存储')
  }
  const local = localStorage.getItem(STORAGE_KEYS.DATABASE_CONFIGS)
  return local ? JSON.parse(local) : []
}

export const saveDatabaseConfig = async (config: any) => {
  try {
    const response = await api.post('/settings/database', config)
    if (response.data?.code === 200) {
      return response.data?.data
    }
  } catch (error) {
    console.log('后端API未就绪，保存到本地存储')
  }
  const configs = await getDatabaseConfigs()
  if (config.id) {
    const index = configs.findIndex((c: any) => c.id === config.id)
    if (index > -1) {
      configs[index] = { ...config }
    }
  } else {
    config.id = Date.now().toString()
    configs.push(config)
  }
  localStorage.setItem(STORAGE_KEYS.DATABASE_CONFIGS, JSON.stringify(configs))
  return config
}

export const deleteDatabaseConfig = async (id: string) => {
  try {
    const response = await api.delete(`/settings/database/${id}`)
    if (response.data?.code === 200) {
      return response.data
    }
  } catch (error) {
    console.log('后端API未就绪，从本地存储删除')
  }
  const configs = await getDatabaseConfigs()
  const filtered = configs.filter((c: any) => c.id !== id)
  localStorage.setItem(STORAGE_KEYS.DATABASE_CONFIGS, JSON.stringify(filtered))
}

export const testDatabaseConnection = async (config: any) => {
  try {
    const response = await api.post('/settings/database/test', config)
    if (response.data?.code === 200) {
      return response.data?.data
    }
  } catch (error) {
    console.log('后端API未就绪，模拟测试')
  }
  // 模拟测试 - 简单验证
  if (config.host && config.port && config.database && config.username && config.password) {
    return { 
      success: true, 
      message: '连接测试成功（本地模式）',
      databaseProductName: config.dbType.toUpperCase(),
      databaseProductVersion: '模拟版本'
    }
  }
  return { success: false, message: '配置信息不完整' }
}

export const getDatabaseInfo = async (id: string) => {
  try {
    const response = await api.get(`/settings/database/${id}/info`)
    if (response.data?.code === 200) {
      return response.data?.data
    }
  } catch (error) {
    console.log('获取数据库信息失败')
  }
  return null
}

// 安全设置相关接口
export const getSecuritySettings = async () => {
  try {
    const response = await api.get('/settings/security')
    if (response.data?.code === 200) {
      return response.data?.data
    }
  } catch (error) {
    console.log('后端API未就绪，使用本地存储')
  }
  const local = localStorage.getItem(STORAGE_KEYS.SECURITY)
  if (local) {
    return JSON.parse(local)
  }
  return {
    jwtEnabled: false,
    tokenExpireHours: 24,
    operationLogEnabled: false
  }
}

export const saveSecuritySettings = async (settings: any) => {
  try {
    const response = await api.post('/settings/security', settings)
    if (response.data?.code === 200) {
      return response.data?.data
    }
  } catch (error) {
    console.log('后端API未就绪，保存到本地存储')
  }
  localStorage.setItem(STORAGE_KEYS.SECURITY, JSON.stringify(settings))
  return settings
}
