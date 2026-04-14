import axios from 'axios'

// 通用响应接口
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  success: boolean
  timestamp: number
}

// 创建axios实例
const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 请求拦截器
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  (response) => {
    return response
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.href = '/login'
    }
    return Promise.reject(error.response?.data || error)
  }
)

// 登录接口
export const login = async (username: string, password: string): Promise<ApiResponse> => {
  const response = await api.post('/auth/login', { username, password })
  return response.data as ApiResponse
}

// 获取当前用户信息
export const getCurrentUser = async () => {
  const response = await api.get('/auth/me')
  return response
}

// 退出登录
export const logout = async () => {
  const response = await api.post('/auth/logout')
  return response
}

// 获取项目列表
export const getProjects = async () => {
  const response = await api.get('/v1/projects')
  return response
}

// 创建项目
export const createProject = async (data: any) => {
  const response = await api.post('/v1/projects', data)
  return response
}

// 获取Agent列表
export const getAgents = async () => {
  const response = await api.get('/agents')
  return response
}

// 执行Agent
export const executeAgent = async (agentName: string, input: any, sharedContext?: any) => {
  const response = await api.post(`/agents/${agentName}/execute`, {
    input,
    sharedContext,
  })
  return response
}

// 流式执行Agent
export const executeAgentStream = async (agentName: string, input: any) => {
  const response = await fetch(`/api/agents/${agentName}/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${localStorage.getItem('token')}`,
    },
    body: JSON.stringify({ input }),
  })
  return response
}

// 启动工作流
export const startWorkflow = async (requirement: string, projectName: string) => {
  const response = await api.post('/agents/workflow/start', {
    requirement,
    projectName,
  })
  return response
}

// 获取工作流状态
export const getWorkflowStatus = async (workflowId: string) => {
  const response = await api.get(`/agents/workflow/${workflowId}/status`)
  return response
}

// AI对话
export const aiChat = async (message: string) => {
  const response = await api.get(`/ai/generate`, {
    params: { message },
  })
  return response
}

export default api
