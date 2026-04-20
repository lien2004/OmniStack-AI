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

// 注册接口
export const register = async (data: {
  username: string
  password: string
  mobile: string
  verifyCode: string
  role?: number
}): Promise<ApiResponse> => {
  const response = await api.post('/user/register', data)
  return response.data as ApiResponse
}

// 发送短信验证码
export const sendSmsCode = async (data: { mobile: string }): Promise<ApiResponse> => {
  const response = await api.post('/user/send-code', data)
  return response.data as ApiResponse
}

// 登录接口
export const login = async (data: { username: string; password: string }): Promise<ApiResponse> => {
  const response = await api.post('/user/login', data)
  return response.data as ApiResponse
}

// 获取个人信息
export const getUserProfile = async (): Promise<ApiResponse> => {
  const response = await api.get('/user/profile')
  return response.data as ApiResponse
}

// 更新个人信息
export const updateUserProfile = async (data: {
  mobile?: string
  oldPassword?: string
  newPassword?: string
}): Promise<ApiResponse> => {
  const response = await api.put('/user/profile', data)
  return response.data as ApiResponse
}

// 管理员：获取所有用户
export const adminGetUsers = async (): Promise<ApiResponse> => {
  const response = await api.get('/user/admin/users')
  return response.data as ApiResponse
}

// 管理员：更新用户
export const adminUpdateUser = async (
  id: number,
  data: { username?: string; mobile?: string; role?: number; password?: string }
): Promise<ApiResponse> => {
  const response = await api.put(`/user/admin/users/${id}`, data)
  return response.data as ApiResponse
}

// 管理员：删除用户
export const adminDeleteUser = async (id: number): Promise<ApiResponse> => {
  const response = await api.delete(`/user/admin/users/${id}`)
  return response.data as ApiResponse
}

// 退出登录
export const logoutApi = async (): Promise<void> => {
  try {
    await api.post('/user/logout')
  } catch (_) {
    // ignore errors
  }
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
