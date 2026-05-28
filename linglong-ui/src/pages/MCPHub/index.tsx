import { useState, useEffect, useCallback } from 'react'
import {
  Card, Tag, Button, Input, Tabs, Switch, Modal,
  Spin, Empty, Tooltip, Badge, Divider, Table,
  message as antdMessage, Typography, Space, Upload,
} from 'antd'
import {
  ToolOutlined, PlusOutlined, SearchOutlined, CodeOutlined,
  FileTextOutlined, DatabaseOutlined, CloudOutlined,
  BranchesOutlined, ApartmentOutlined, PlayCircleOutlined,
  InfoCircleOutlined, CheckCircleOutlined,
  ReloadOutlined, ThunderboltOutlined, CopyOutlined,
  FolderOpenOutlined, UploadOutlined, CloudServerOutlined,
  SendOutlined, DownloadOutlined, EyeOutlined,
  DeleteOutlined, PauseCircleOutlined, FileAddOutlined,
} from '@ant-design/icons'
import axios from 'axios'

const { TextArea } = Input
const { Text, Paragraph } = Typography

// -------- 类型定义 --------

interface ToolDefinition {
  name: string
  description: string
  category: string
  parameters: { name: string; description?: string; type: string; required: boolean }[]
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

// -------- 分类配置 --------

const CATEGORY_CONFIG: Record<string, { label: string; icon: React.ReactNode; color: string; tagColor: string }> = {
  weather:  { label: '天气服务', icon: <CloudOutlined />,       color: '#1677ff', tagColor: 'blue' },
  document: { label: '文档处理', icon: <FileTextOutlined />,    color: '#1677ff', tagColor: 'blue' },
  code:     { label: '代码操作', icon: <CodeOutlined />,        color: '#52c41a', tagColor: 'green' },
  git:      { label: '版本控制', icon: <BranchesOutlined />,    color: '#fa8c16', tagColor: 'orange' },
  docker:   { label: '容器化',   icon: <CloudServerOutlined />, color: '#13c2c2', tagColor: 'cyan' },
  diagram:  { label: '图表生成', icon: <ApartmentOutlined />,   color: '#722ed1', tagColor: 'purple' },
  database: { label: '数据库',   icon: <DatabaseOutlined />,    color: '#eb2f96', tagColor: 'magenta' },
}

const USAGE_COUNTS: Record<string, number> = {
  WeatherTool: 1024, ReadFileTool: 523, WriteCodeTool: 892, GitTool: 456,
  DockerTool: 234, DiagramTool: 178, DatabaseTool: 345,
}

// -------- 工具方法 --------
// ======== 每个工具的独立App组件 ========

// ---- 天气工具 App ----
function WeatherApp({ onClose }: { onClose: () => void }) {
  const [city, setCity] = useState('')
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState<any>(null)
  const [error, setError] = useState('')

  const handleQuery = async () => {
    if (!city.trim()) return antdMessage.warning('请输入城市名称')
    setLoading(true); setError(''); setResult(null)
    try {
      // 同时请求实时天气和预报
      const [liveRes, forecastRes] = await Promise.allSettled([
        axios.post('/api/mcp/tools/query_weather_live/execute', { city: city.trim() }),
        axios.post('/api/mcp/tools/query_weather_forecast/execute', { city: city.trim() }),
      ])

      const combined: any = {}

      if (liveRes.status === 'fulfilled') {
        const toolResult = liveRes.value.data?.data ?? liveRes.value.data
        if (toolResult?.success !== false) {
          // 数据在metadata中（后端Map.of走metadata overload）
          const meta = toolResult?.metadata ?? toolResult?.data ?? {}
          if (meta?.liveWeather) combined.liveWeather = meta.liveWeather
          if (meta?.city) combined.city = meta.city
        }
      }

      if (forecastRes.status === 'fulfilled') {
        const toolResult = forecastRes.value.data?.data ?? forecastRes.value.data
        if (toolResult?.success !== false) {
          const meta = toolResult?.metadata ?? toolResult?.data ?? {}
          if (meta?.forecast) combined.forecast = meta.forecast
          if (meta?.cityName) combined.city = meta.cityName
          if (meta?.city) combined.city = meta.city
        }
      }

      if (!combined.liveWeather && !combined.forecast) {
        setError('查询失败，请检查城市名称是否正确')
        return
      }
      setResult(combined)
    } catch (e: any) {
      setError(e.response?.data?.message || e.message || '请求失败')
    } finally { setLoading(false) }
  }

  const getWeatherIcon = (w: string) => {
    if (w.includes('雨')) return '🌧️'
    if (w.includes('雪')) return '❄️'
    if (w.includes('云') || w.includes('阴')) return '☁️'
    if (w.includes('晴')) return '☀️'
    return '🌤️'
  }

  return (
    <Modal open title={null} onCancel={onClose} footer={null} width={720} destroyOnClose
      styles={{ body: { padding: 0 } }}>
      <div style={{ background: '#ffffff', padding: '24px 32px 20px', borderBottom: '1px solid #f0f0f0' }}>
        <h2 style={{ color: '#1f2937', margin: 0, fontSize: 18, fontWeight: 700 }}>🌤️ 天气查询</h2>
        <p style={{ color: '#6b7280', margin: '4px 0 0', fontSize: 13 }}>查询全国城市实时天气与未来预报</p>
      </div>
      <div style={{ padding: '24px 32px 32px' }}>
        <div style={{ display: 'flex', gap: 12, marginBottom: 24 }}>
          <Input
            size="large" placeholder="输入城市名称，如：北京、上海、深圳"
            value={city} onChange={e => setCity(e.target.value)}
            onPressEnter={handleQuery}
            prefix={<SearchOutlined style={{ color: '#bfbfbf' }} />}
            style={{ flex: 1, borderRadius: 10 }}
          />
          <Button type="primary" size="large" icon={<SendOutlined />} onClick={handleQuery}
            loading={loading} style={{ borderRadius: 10, minWidth: 100 }}>查询</Button>
        </div>

        {error && <div style={{ color: '#ff4d4f', padding: 12, background: '#fff2f0', borderRadius: 8, marginBottom: 16 }}>{error}</div>}

        {result && (
          <div>
            {/* 实时天气 */}
            {result.liveWeather && (
              <div style={{ background: '#f8fbff', borderRadius: 12, padding: '20px 24px', border: '1px solid #d6e4ff', marginBottom: 16 }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <div>
                    <Text strong style={{ fontSize: 20, display: 'block' }}>🌍 {result.liveWeather.city || result.city || city}</Text>
                    <Text type="secondary" style={{ fontSize: 12 }}>实时天气 · {result.liveWeather.reportTime}</Text>
                  </div>
                  <div style={{ textAlign: 'right' }}>
                    <Text style={{ fontSize: 42, fontWeight: 700, lineHeight: 1.2 }}>{result.liveWeather.temperature}°</Text>
                    <Text style={{ fontSize: 15, display: 'block' }}>{result.liveWeather.weather}</Text>
                  </div>
                </div>
                <Divider style={{ margin: '16px 0', borderColor: '#d6e4ff' }} />
                <div style={{ display: 'flex', gap: 32 }}>
                  <div><Text type="secondary" style={{ fontSize: 11, display: 'block' }}>湿度</Text><Text strong>{result.liveWeather.humidity}%</Text></div>
                  <div><Text type="secondary" style={{ fontSize: 11, display: 'block' }}>风向</Text><Text strong>{result.liveWeather.windDirection}</Text></div>
                  <div><Text type="secondary" style={{ fontSize: 11, display: 'block' }}>风力</Text><Text strong>{result.liveWeather.windPower}级</Text></div>
                </div>
              </div>
            )}
            {/* 预报 */}
            {result.forecast && (
              <div style={{ display: 'flex', gap: 12, overflowX: 'auto', paddingBottom: 4 }}>
                {result.forecast.map((day: any, idx: number) => (
                  <div key={idx} style={{ minWidth: 140, padding: '14px', borderRadius: 10, background: '#f8fbff', border: '1px solid #e5e7eb', flexShrink: 0 }}>
                    <Text strong style={{ fontSize: 13, display: 'block' }}>{day.week || `第${idx+1}天`}</Text>
                    <Text type="secondary" style={{ fontSize: 11, display: 'block', marginBottom: 8 }}>{day.date}</Text>
                    <Text style={{ fontSize: 28, display: 'block', marginBottom: 8 }}>{getWeatherIcon(day.dayWeather || '')}</Text>
                    <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                      <Text style={{ color: '#fa541c', fontWeight: 600 }}>{day.dayTemp}°</Text>
                      <Text style={{ color: '#1677ff', fontWeight: 600 }}>{day.nightTemp}°</Text>
                    </div>
                    <Text type="secondary" style={{ fontSize: 11, marginTop: 4, display: 'block' }}>{day.dayWeather}/{day.nightWeather}</Text>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>
    </Modal>
  )
}

// ---- Docker工具 App ----
function DockerApp({ onClose }: { onClose: () => void }) {
  const [loading, setLoading] = useState(false)
  const [containers, setContainers] = useState<any[]>([])
  const [images, setImages] = useState<any[]>([])
  const [activeView, setActiveView] = useState<'containers' | 'images'>('containers')
  const [error, setError] = useState('')

  const execTool = async (toolName: string, params: object = {}) => {
    const res = await axios.post(`/api/mcp/tools/${toolName}/execute`, params)
    const data = res.data?.data ?? res.data
    if (data?.success === false) throw new Error(data.errorMessage || '执行失败')
    // 数据优先从metadata取（后端Map.of走metadata overload），其次尝试解析output
    return data?.metadata ?? data?.data ?? (data?.output ? (() => { try { return JSON.parse(data.output) } catch { return data.output } })() : data)
  }

  const loadContainers = async () => {
    setLoading(true); setError('')
    try {
      const result = await execTool('docker_ps')
      const list = result?.containers ?? (Array.isArray(result) ? result : [])
      setContainers(list)
    }
    catch (e: any) { setError(e.message); setContainers([]) }
    finally { setLoading(false) }
  }

  const loadImages = async () => {
    setLoading(true); setError('')
    try {
      const result = await execTool('docker_images')
      const list = result?.images ?? (Array.isArray(result) ? result : [])
      setImages(list)
    }
    catch (e: any) { setError(e.message); setImages([]) }
    finally { setLoading(false) }
  }

  useEffect(() => { loadContainers() }, [])

  const handleStopContainer = async (id: string) => {
    try { await execTool('docker_stop', { containerId: id }); antdMessage.success('容器已停止'); loadContainers() }
    catch (e: any) { antdMessage.error(e.message) }
  }

  const handleRemoveContainer = async (id: string) => {
    try { await execTool('docker_remove_container', { containerId: id, force: true }); antdMessage.success('容器已删除'); loadContainers() }
    catch (e: any) { antdMessage.error(e.message) }
  }

  return (
    <Modal open title={null} onCancel={onClose} footer={null} width={860} destroyOnClose
      styles={{ body: { padding: 0 } }}>
      <div style={{ background: '#ffffff', padding: '24px 32px 20px', borderBottom: '1px solid #f0f0f0' }}>
        <h2 style={{ color: '#1f2937', margin: 0, fontSize: 18, fontWeight: 700 }}>🐳 Docker 管理</h2>
        <p style={{ color: '#6b7280', margin: '4px 0 0', fontSize: 13 }}>管理容器、镜像，执行Docker操作</p>
      </div>
      <div style={{ padding: '24px 32px 32px' }}>
        <div style={{ display: 'flex', gap: 12, marginBottom: 20 }}>
          <Button type={activeView === 'containers' ? 'primary' : 'default'} icon={<CloudServerOutlined />}
            onClick={() => { setActiveView('containers'); loadContainers() }}>容器列表</Button>
          <Button type={activeView === 'images' ? 'primary' : 'default'} icon={<DatabaseOutlined />}
            onClick={() => { setActiveView('images'); loadImages() }}>镜像列表</Button>
          <Button icon={<ReloadOutlined />} onClick={activeView === 'containers' ? loadContainers : loadImages} loading={loading} style={{ marginLeft: 'auto' }}>刷新</Button>
        </div>

        {error && <div style={{ color: '#ff4d4f', padding: 12, background: '#fff2f0', borderRadius: 8, marginBottom: 16 }}>{error}</div>}

        <Spin spinning={loading}>
          {activeView === 'containers' ? (
            containers.length === 0 ? <Empty description="暂无运行中的容器" /> : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                {containers.map((c, idx) => {
                  const isRunning = String(c.status || c.state || '').toLowerCase().includes('up') || String(c.status || '').toLowerCase().includes('running')
                  return (
                    <div key={idx} style={{ background: '#fafafa', border: '1px solid #f0f0f0', borderRadius: 12, padding: '16px 20px', display: 'flex', alignItems: 'center', gap: 14 }}>
                      <div style={{ width: 12, height: 12, borderRadius: '50%', background: isRunning ? '#52c41a' : '#bfbfbf', flexShrink: 0 }} />
                      <div style={{ flex: 1, minWidth: 0 }}>
                        <Text strong style={{ fontSize: 14, display: 'block' }}>{c.name || c.containerName || c.containerId || `容器 ${idx+1}`}</Text>
                        <Text type="secondary" style={{ fontSize: 12 }}>{c.image || ''} · {c.status || c.state || '未知'}</Text>
                      </div>
                      {c.ports && <Tag style={{ borderRadius: 6 }}>{typeof c.ports === 'string' ? c.ports : JSON.stringify(c.ports)}</Tag>}
                      <Space size={4}>
                        {isRunning && <Tooltip title="停止"><Button size="small" icon={<PauseCircleOutlined />} onClick={() => handleStopContainer(c.containerId || c.name)} /></Tooltip>}
                        <Tooltip title="删除"><Button size="small" danger icon={<DeleteOutlined />} onClick={() => handleRemoveContainer(c.containerId || c.name)} /></Tooltip>
                      </Space>
                    </div>
                  )
                })}
              </div>
            )
          ) : (
            images.length === 0 ? <Empty description="暂无镜像" /> : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                {images.map((img, idx) => (
                  <div key={idx} style={{ background: '#fafafa', border: '1px solid #f0f0f0', borderRadius: 12, padding: '14px 20px', display: 'flex', alignItems: 'center', gap: 14 }}>
                    <DatabaseOutlined style={{ fontSize: 20, color: '#13c2c2' }} />
                    <div style={{ flex: 1 }}>
                      <Text strong style={{ fontSize: 13 }}>{img.repository || img.name || img.imageId || `镜像 ${idx+1}`}</Text>
                      <Text type="secondary" style={{ fontSize: 12, display: 'block' }}>{img.tag || 'latest'} · {img.size || ''}</Text>
                    </div>
                  </div>
                ))}
              </div>
            )
          )}
        </Spin>
      </div>
    </Modal>
  )
}

// ---- Git工具 App ----
function GitApp({ onClose }: { onClose: () => void }) {
  const [directory, setDirectory] = useState('E:/OmniStack AI')
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState<any>(null)
  const [commitMsg, setCommitMsg] = useState('')
  const [error, setError] = useState('')

  const execTool = async (toolName: string, params: object) => {
    setLoading(true); setError(''); setResult(null)
    try {
      const res = await axios.post(`/api/mcp/tools/${toolName}/execute`, params)
      const data = res.data?.data ?? res.data
      if (data?.success === false) throw new Error(data.errorMessage || '执行失败')
      // 优先取metadata（结构化数据），否则尝试解析output或用描述文本
      const structured = data?.metadata ?? data?.data
      if (structured && Object.keys(structured).length > 0) {
        setResult(structured)
      } else {
        const output = data?.output ? (() => { try { return JSON.parse(data.output) } catch { return data.output } })() : data
        setResult(output)
      }
    } catch (e: any) { setError(e.message) }
    finally { setLoading(false) }
  }

  return (
    <Modal open title={null} onCancel={onClose} footer={null} width={800} destroyOnClose
      styles={{ body: { padding: 0 } }}>
      <div style={{ background: '#ffffff', padding: '24px 32px 20px', borderBottom: '1px solid #f0f0f0' }}>
        <h2 style={{ color: '#1f2937', margin: 0, fontSize: 18, fontWeight: 700 }}>📂 Git 版本控制</h2>
        <p style={{ color: '#6b7280', margin: '4px 0 0', fontSize: 13 }}>执行Git操作：查看状态、提交、推送、拉取</p>
      </div>
      <div style={{ padding: '24px 32px 32px' }}>
        <div style={{ marginBottom: 20 }}>
          <Text strong style={{ fontSize: 13, display: 'block', marginBottom: 8 }}>仓库路径</Text>
          <Input size="large" value={directory} onChange={e => setDirectory(e.target.value)}
            prefix={<FolderOpenOutlined style={{ color: '#bfbfbf' }} />} style={{ borderRadius: 10 }} />
        </div>

        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10, marginBottom: 20 }}>
          <Button icon={<EyeOutlined />} onClick={() => execTool('git_status', { directory })} loading={loading}>查看状态</Button>
          <Button icon={<BranchesOutlined />} onClick={() => execTool('git_branch_list', { directory })} loading={loading}>分支列表</Button>
          <Button icon={<DownloadOutlined />} onClick={() => execTool('git_pull', { directory })} loading={loading}>拉取更新</Button>
          <Button type="primary" icon={<SendOutlined />} onClick={() => execTool('git_push', { directory })} loading={loading}>推送</Button>
        </div>

        <div style={{ display: 'flex', gap: 12, marginBottom: 20 }}>
          <Input placeholder="提交信息..." value={commitMsg} onChange={e => setCommitMsg(e.target.value)}
            onPressEnter={() => { if (commitMsg.trim()) execTool('git_commit', { directory, message: commitMsg }) }}
            style={{ flex: 1, borderRadius: 8 }} />
          <Button type="primary" onClick={async () => {
            if (!commitMsg.trim()) return antdMessage.warning('请输入提交信息')
            await execTool('git_add', { directory, filePattern: '.' })
            await execTool('git_commit', { directory, message: commitMsg })
            setCommitMsg('')
          }} loading={loading}>暂存并提交</Button>
        </div>

        {error && <div style={{ color: '#ff4d4f', padding: 12, background: '#fff2f0', borderRadius: 8, marginBottom: 16 }}>{error}</div>}

        {result && (
          <div style={{ background: '#1e1e1e', borderRadius: 10, padding: '16px 20px', maxHeight: 300, overflow: 'auto' }}>
            <pre style={{ color: '#d4d4d4', fontSize: 12, lineHeight: 1.7, margin: 0, whiteSpace: 'pre-wrap', fontFamily: '"Fira Code", Consolas, monospace' }}>
              {typeof result === 'string' ? result : JSON.stringify(result, null, 2)}
            </pre>
          </div>
        )}
      </div>
    </Modal>
  )
}

// ---- 代码写入工具 App ----
function WriteCodeApp({ onClose }: { onClose: () => void }) {
  const [filePath, setFilePath] = useState('')
  const [content, setContent] = useState('')
  const [append, setAppend] = useState(false)
  const [loading, setLoading] = useState(false)
  const [success, setSuccess] = useState(false)
  const [error, setError] = useState('')

  const handleWrite = async () => {
    if (!filePath.trim()) return antdMessage.warning('请输入文件路径')
    if (!content.trim()) return antdMessage.warning('请输入文件内容')
    setLoading(true); setError(''); setSuccess(false)
    try {
      const res = await axios.post('/api/mcp/tools/write_code/execute', { filePath, content, append })
      const data = res.data?.data ?? res.data
      if (data?.success === false) throw new Error(data.errorMessage || '写入失败')
      setSuccess(true)
      antdMessage.success('文件写入成功！')
    } catch (e: any) { setError(e.message || '写入失败') }
    finally { setLoading(false) }
  }

  return (
    <Modal open title={null} onCancel={onClose} footer={null} width={800} destroyOnClose
      styles={{ body: { padding: 0 } }}>
      <div style={{ background: '#ffffff', padding: '24px 32px 20px', borderBottom: '1px solid #f0f0f0' }}>
        <h2 style={{ color: '#1f2937', margin: 0, fontSize: 18, fontWeight: 700 }}>✏️ 代码写入</h2>
        <p style={{ color: '#6b7280', margin: '4px 0 0', fontSize: 13 }}>将代码内容写入指定文件路径，支持自动创建目录</p>
      </div>
      <div style={{ padding: '24px 32px 32px' }}>
        <div style={{ marginBottom: 16 }}>
          <Text strong style={{ fontSize: 13, display: 'block', marginBottom: 8 }}>文件路径</Text>
          <Input size="large" value={filePath} onChange={e => setFilePath(e.target.value)}
            placeholder="E:/projects/src/Main.java" prefix={<FileAddOutlined style={{ color: '#bfbfbf' }} />} style={{ borderRadius: 10 }} />
        </div>

        <div style={{ marginBottom: 16 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
            <Text strong style={{ fontSize: 13 }}>文件内容</Text>
            <Space>
              <Text style={{ fontSize: 12 }}>追加模式</Text>
              <Switch size="small" checked={append} onChange={setAppend} />
            </Space>
          </div>
          <TextArea value={content} onChange={e => setContent(e.target.value)}
            rows={12} placeholder="输入代码或文本内容..."
            style={{ fontFamily: '"Fira Code", Consolas, monospace', fontSize: 13, borderRadius: 10, background: '#fafafa' }} />
        </div>

        {error && <div style={{ color: '#ff4d4f', padding: 12, background: '#fff2f0', borderRadius: 8, marginBottom: 16 }}>{error}</div>}
        {success && (
          <div style={{ padding: 12, background: '#f6ffed', borderRadius: 8, border: '1px solid #b7eb8f', marginBottom: 16, display: 'flex', alignItems: 'center', gap: 8 }}>
            <CheckCircleOutlined style={{ color: '#52c41a', fontSize: 18 }} />
            <Text>文件已成功写入到 <Text code>{filePath}</Text></Text>
          </div>
        )}

        <Button type="primary" size="large" icon={<SendOutlined />} onClick={handleWrite} loading={loading}
          style={{ borderRadius: 10, minWidth: 140 }} block>
          写入文件
        </Button>
      </div>
    </Modal>
  )
}

// ---- 文件读取工具 App ----
function ReadFileApp({ onClose }: { onClose: () => void }) {
  const [filePath, setFilePath] = useState('')
  const [loading, setLoading] = useState(false)
  const [content, setContent] = useState('')
  const [error, setError] = useState('')
  const [uploadLoading, setUploadLoading] = useState(false)

  const handleRead = async (path?: string) => {
    const p = path || filePath
    if (!p.trim()) return antdMessage.warning('请输入文件路径或上传文件')
    setLoading(true); setError(''); setContent('')
    try {
      const res = await axios.post('/api/mcp/tools/read_file/execute', { filePath: p, maxLength: 10000 })
      const data = res.data?.data ?? res.data
      if (data?.success === false) throw new Error(data.errorMessage || '读取失败')
      // read_file的output就是文件内容
      setContent(data?.output || (typeof data === 'string' ? data : JSON.stringify(data, null, 2)))
    } catch (e: any) { setError(e.message) }
    finally { setLoading(false) }
  }

  const handleUpload = async (file: File) => {
    setUploadLoading(true)
    try {
      const fd = new FormData()
      fd.append('file', file)
      const res = await axios.post('/ai/document/upload/temp', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
      const path = res.data?.path || res.data
      setFilePath(path)
      antdMessage.success('文件已上传')
      handleRead(path)
    } catch { antdMessage.error('上传失败') }
    finally { setUploadLoading(false) }
    return false
  }

  return (
    <Modal open title={null} onCancel={onClose} footer={null} width={860} destroyOnClose
      styles={{ body: { padding: 0 } }}>
      <div style={{ background: '#ffffff', padding: '24px 32px 20px', borderBottom: '1px solid #f0f0f0' }}>
        <h2 style={{ color: '#1f2937', margin: 0, fontSize: 18, fontWeight: 700 }}>📄 文档读取</h2>
        <p style={{ color: '#6b7280', margin: '4px 0 0', fontSize: 13 }}>读取文件内容，支持PDF、Word、Markdown、TXT等多种格式</p>
      </div>
      <div style={{ padding: '24px 32px 32px' }}>
        <div style={{ display: 'flex', gap: 12, marginBottom: 16 }}>
          <Input size="large" value={filePath} onChange={e => setFilePath(e.target.value)}
            placeholder="输入文件路径或上传文件" onPressEnter={() => handleRead()}
            prefix={<FolderOpenOutlined style={{ color: '#bfbfbf' }} />} style={{ flex: 1, borderRadius: 10 }} />
          <Button type="primary" size="large" icon={<EyeOutlined />} onClick={() => handleRead()} loading={loading} style={{ borderRadius: 10 }}>读取</Button>
        </div>

        <div style={{ marginBottom: 20, display: 'flex', alignItems: 'center', gap: 12 }}>
          <Upload showUploadList={false} beforeUpload={handleUpload as any}
            accept=".pdf,.docx,.doc,.txt,.md,.html,.json,.yaml,.yml,.xml,.csv,.xlsx,.xls,.pptx,.ppt">
            <Button icon={<UploadOutlined />} loading={uploadLoading} style={{ borderRadius: 8 }}>上传文件</Button>
          </Upload>
          <Text type="secondary" style={{ fontSize: 12 }}>支持 PDF、Word、Excel、PPT、Markdown、TXT、HTML、JSON、XML、CSV</Text>
        </div>

        {error && <div style={{ color: '#ff4d4f', padding: 12, background: '#fff2f0', borderRadius: 8, marginBottom: 16 }}>{error}</div>}

        {content && (
          <div style={{ position: 'relative' }}>
            <Button size="small" icon={<CopyOutlined />} style={{ position: 'absolute', top: 8, right: 8, zIndex: 1 }}
              onClick={() => { navigator.clipboard.writeText(content); antdMessage.success('已复制') }}>复制</Button>
            <pre style={{ background: '#1e1e1e', color: '#d4d4d4', borderRadius: 10, padding: '20px', fontSize: 12, lineHeight: 1.7, overflow: 'auto', maxHeight: 400, margin: 0, fontFamily: '"Fira Code", Consolas, monospace' }}>
              {content}
            </pre>
          </div>
        )}
      </div>
    </Modal>
  )
}

// ---- 图表生成工具 App ----
// 图表模板配置
const DIAGRAM_TEMPLATES = [
  { key: 'flowchart', emoji: '🔄', label: '流程图', prompt: '生成一个标准流程图，包含开始/结束节点、判断分支、处理步骤' },
  { key: 'architecture', emoji: '🏗️', label: '架构图', prompt: '生成一个系统架构图，包含前端、后端、数据库、缓存等分层组件' },
  { key: 'class', emoji: '📐', label: '类图', prompt: '生成一个UML类图，包含类的属性、方法、继承和关联关系' },
  { key: 'sequence', emoji: '⏱️', label: '时序图', prompt: '生成一个时序图，展示各模块间的交互调用顺序' },
  { key: 'er', emoji: '🗃️', label: 'ER图', prompt: '生成一个数据库ER图，展示表之间的关联关系' },
  { key: 'mindmap', emoji: '🧠', label: '思维导图', prompt: '生成一个思维导图，从中心主题向外展开各个分支' },
]

function DiagramApp({ onClose }: { onClose: () => void }) {
  const [prompt, setPrompt] = useState('')
  const [loading, setLoading] = useState(false)
  const [svgContent, setSvgContent] = useState('')
  const [mermaidCode, setMermaidCode] = useState('')
  const [drawioUrl, setDrawioUrl] = useState('')
  const [error, setError] = useState('')
  const [uploadLoading, setUploadLoading] = useState(false)
  const [exportLoading, setExportLoading] = useState(false)

  // 向已打开的draw.io窗口发送XML数据
  const sendXmlToDrawio = (drawioWin: Window, xml: string) => {
    const wrappedXml = xml.includes('<mxfile') ? xml :
      `<mxfile><diagram name="Page-1">${xml}</diagram></mxfile>`

    const handler = (evt: MessageEvent) => {
      if (evt.source !== drawioWin) return
      try {
        const msg = typeof evt.data === 'string' ? JSON.parse(evt.data) : evt.data
        if (msg.event === 'init') {
          // draw.io就绪，发送XML数据加载
          drawioWin.postMessage(JSON.stringify({
            action: 'load',
            xml: wrappedXml,
            autosave: 0
          }), '*')
          antdMessage.success('图表已加载到 draw.io，编辑完成后可直接导出保存')
        } else if (msg.event === 'save') {
          antdMessage.success('图表已保存')
        } else if (msg.event === 'exit') {
          window.removeEventListener('message', handler)
        }
      } catch { /* 忽略非JSON消息 */ }
    }
    window.addEventListener('message', handler)
    // 60秒超时清理监听器
    setTimeout(() => window.removeEventListener('message', handler), 60000)
  }

  // 下载.drawio文件（弹窗被拦截时的备选方案）
  const downloadDrawioFile = (xml: string) => {
    const wrappedXml = xml.includes('<mxfile') ? xml :
      `<mxfile><diagram name="Page-1">${xml}</diagram></mxfile>`
    const blob = new Blob([wrappedXml], { type: 'application/xml' })
    const blobUrl = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = blobUrl; a.download = 'diagram.drawio'; a.click()
    URL.revokeObjectURL(blobUrl)
    antdMessage.info('已下载 .drawio 文件，请用 draw.io 桌面版或网页版打开编辑')
  }

  const handleGenerate = async (format: 'mermaid' | 'drawio' = 'mermaid') => {
    if (!prompt.trim()) return antdMessage.warning('请描述你想生成的图表')

    // 关键：drawio格式时，先在用户点击的同步上下文中打开窗口，避免被浏览器拦截
    let drawioWin: Window | null = null
    if (format === 'drawio') {
      drawioWin = window.open(
        'https://embed.diagrams.net/?embed=1&proto=json&spin=1&lang=zh',
        '_blank'
      )
    }

    setLoading(true); setError(''); setSvgContent(''); setMermaidCode(''); setDrawioUrl('')
    try {
      const res = await axios.post('/api/mcp/tools/generate_smart_diagram/execute', { prompt, format })
      const data = res.data?.data ?? res.data
      if (data?.success === false) throw new Error(data.errorMessage || '生成失败')
      const meta = data?.metadata ?? {}

      if (format === 'drawio') {
        const xml = meta?.drawioXml
        if (xml && drawioWin) {
          sendXmlToDrawio(drawioWin, xml)
          setDrawioUrl('embed-mode')
        } else if (xml && !drawioWin) {
          // 弹窗被拦截，降级为下载
          downloadDrawioFile(xml)
        } else {
          drawioWin?.close()
          setError('draw.io 图表生成失败，请重试')
        }
      } else {
        if (meta?.mermaidCode) setMermaidCode(meta.mermaidCode)
        if (meta?.svgContent) setSvgContent(meta.svgContent)
        if (!meta?.mermaidCode && data?.output) setMermaidCode(data.output)
      }
    } catch (e: any) {
      drawioWin?.close()
      setError(e.message)
    }
    finally { setLoading(false) }
  }

  // 将已生成的mermaid图表导出到draw.io编辑
  const handleOpenInDrawio = async () => {
    if (!mermaidCode && !prompt.trim()) return antdMessage.warning('请先生成图表')

    // 先在用户点击事件中立即打开窗口
    const drawioWin = window.open(
      'https://embed.diagrams.net/?embed=1&proto=json&spin=1&lang=zh',
      '_blank'
    )

    setExportLoading(true)
    try {
      const res = await axios.post('/api/mcp/tools/generate_smart_diagram/execute', {
        prompt: prompt || '将以下Mermaid代码转换为draw.io格式:\n' + mermaidCode,
        format: 'drawio'
      })
      const data = res.data?.data ?? res.data
      if (data?.success === false) throw new Error(data.errorMessage || '导出失败')
      const meta = data?.metadata ?? {}
      const xml = meta?.drawioXml
      if (xml && drawioWin) {
        sendXmlToDrawio(drawioWin, xml)
      } else if (xml && !drawioWin) {
        downloadDrawioFile(xml)
      } else {
        drawioWin?.close()
        antdMessage.error('无法生成draw.io格式')
      }
    } catch (e: any) {
      drawioWin?.close()
      antdMessage.error('导出到draw.io失败: ' + e.message)
    }
    finally { setExportLoading(false) }
  }

  const handleUpload = async (file: File) => {
    setUploadLoading(true)
    try {
      const fd = new FormData()
      fd.append('file', file)
      const res = await axios.post('/ai/document/upload/temp', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
      const path = res.data?.path || res.data
      setPrompt(prev => prev + (prev ? '\n' : '') + `参考文件: ${path}`)
      antdMessage.success('文件已上传，路径已添加到描述中')
    } catch { antdMessage.error('上传失败') }
    finally { setUploadLoading(false) }
    return false
  }

  return (
    <Modal open title={null} onCancel={onClose} footer={null} width={960} destroyOnClose
      styles={{ body: { padding: 0 } }}>
      <div style={{ background: '#ffffff', padding: '24px 32px 20px', borderBottom: '1px solid #f0f0f0' }}>
        <h2 style={{ color: '#1f2937', margin: 0, fontSize: 18, fontWeight: 700 }}>📊 智能图表生成</h2>
        <p style={{ color: '#6b7280', margin: '4px 0 0', fontSize: 13 }}>AI生成图表 → 预览确认 → 一键导入 draw.io 自由编辑调整</p>
      </div>
      <div style={{ padding: '24px 32px 32px' }}>
        {/* 模板快捷选择 */}
        <div style={{ marginBottom: 16 }}>
          <Text strong style={{ fontSize: 13, display: 'block', marginBottom: 8 }}>快捷模板</Text>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
            {DIAGRAM_TEMPLATES.map(t => (
              <Button key={t.key} size="small"
                style={{ borderRadius: 8, fontSize: 12, display: 'flex', alignItems: 'center', gap: 4 }}
                onClick={() => setPrompt(prev => prev ? prev + '\n' + t.prompt : t.prompt)}>
                <span>{t.emoji}</span> {t.label}
              </Button>
            ))}
          </div>
        </div>

        <div style={{ marginBottom: 16 }}>
          <TextArea value={prompt} onChange={e => setPrompt(e.target.value)}
            rows={4} placeholder="描述你想要的图表，例如：&#10;• 生成一个电商系统的微服务架构图&#10;• 画一个用户登录的流程图&#10;• 生成订单模块的类图"
            style={{ borderRadius: 10, fontSize: 14 }} />
        </div>

        <div style={{ display: 'flex', gap: 12, marginBottom: 20, flexWrap: 'wrap' }}>
          <Button type="primary" size="large" icon={<ThunderboltOutlined />} onClick={() => handleGenerate('mermaid')}
            loading={loading} style={{ borderRadius: 10, minWidth: 160 }}>AI 生成预览</Button>
          <Button size="large" icon={<SendOutlined />} onClick={() => handleGenerate('drawio')}
            loading={loading} style={{ borderRadius: 10, minWidth: 180, borderColor: '#722ed1', color: '#722ed1' }}>
            直接生成到 draw.io
          </Button>
          <Upload showUploadList={false} beforeUpload={handleUpload as any} accept=".pdf,.docx,.doc,.txt,.md">
            <Button icon={<UploadOutlined />} size="large" loading={uploadLoading} style={{ borderRadius: 10 }}>上传参考文档</Button>
          </Upload>
        </div>

        {error && <div style={{ color: '#ff4d4f', padding: 12, background: '#fff2f0', borderRadius: 8, marginBottom: 16 }}>{error}</div>}

        {/* SVG预览 + 操作栏 */}
        {svgContent && (
          <div style={{ marginBottom: 16 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10, padding: '10px 14px', background: '#f6f0ff', borderRadius: 10, border: '1px solid #d3adf7' }}>
              <Text strong style={{ color: '#531dab' }}>📐 图表预览</Text>
              <Space size={8}>
                <Button type="primary" icon={<SendOutlined />} onClick={handleOpenInDrawio}
                  loading={exportLoading}
                  style={{ borderRadius: 8, background: '#722ed1', borderColor: '#722ed1' }}>
                  导入 draw.io 编辑
                </Button>
                <Button icon={<DownloadOutlined />} style={{ borderRadius: 8 }} onClick={() => {
                  const blob = new Blob([svgContent], { type: 'image/svg+xml' })
                  const url = URL.createObjectURL(blob)
                  const a = document.createElement('a'); a.href = url; a.download = 'diagram.svg'; a.click()
                  URL.revokeObjectURL(url)
                }}>下载 SVG</Button>
                <Button icon={<CopyOutlined />} style={{ borderRadius: 8 }} onClick={() => {
                  navigator.clipboard.writeText(mermaidCode || svgContent); antdMessage.success('已复制代码')
                }}>复制代码</Button>
              </Space>
            </div>
            <div style={{ border: '1px solid #f0f0f0', borderRadius: 10, padding: 16, background: '#fff', maxHeight: 420, overflow: 'auto' }}
              dangerouslySetInnerHTML={{ __html: svgContent }} />
          </div>
        )}

        {/* Mermaid代码预览（无SVG时） */}
        {mermaidCode && !svgContent && (
          <div style={{ marginBottom: 16 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10, padding: '10px 14px', background: '#f6f0ff', borderRadius: 10, border: '1px solid #d3adf7' }}>
              <Text strong style={{ color: '#531dab' }}>📝 Mermaid 代码（SVG渲染失败，可导入draw.io编辑）</Text>
              <Space size={8}>
                <Button type="primary" icon={<SendOutlined />} onClick={handleOpenInDrawio}
                  loading={exportLoading}
                  style={{ borderRadius: 8, background: '#722ed1', borderColor: '#722ed1' }}>
                  导入 draw.io 编辑
                </Button>
                <Button icon={<CopyOutlined />} style={{ borderRadius: 8 }} onClick={() => {
                  navigator.clipboard.writeText(mermaidCode); antdMessage.success('已复制Mermaid代码')
                }}>复制</Button>
              </Space>
            </div>
            <pre style={{ background: '#1e1e1e', color: '#d4d4d4', borderRadius: 10, padding: '20px', fontSize: 12, lineHeight: 1.7, overflow: 'auto', maxHeight: 300, margin: 0 }}>
              {mermaidCode}
            </pre>
          </div>
        )}

        {/* draw.io链接 */}
        {drawioUrl && (
          <div style={{ padding: '14px 18px', background: '#f6ffed', border: '1px solid #b7eb8f', borderRadius: 10, display: 'flex', alignItems: 'center', gap: 12 }}>
            <CheckCircleOutlined style={{ color: '#52c41a', fontSize: 20 }} />
            <div style={{ flex: 1 }}>
              <Text strong style={{ display: 'block', fontSize: 14 }}>图表已生成到 draw.io</Text>
              <Text type="secondary" style={{ fontSize: 12 }}>已在新标签页打开，可自由拖拽调整节点、连线和样式</Text>
            </div>
            <Button type="link" onClick={() => window.open(drawioUrl, '_blank')}>重新打开</Button>
          </div>
        )}
      </div>
    </Modal>
  )
}

// ---- 数据库工具 App ----
function DatabaseApp({ onClose }: { onClose: () => void }) {
  const [sql, setSql] = useState('')
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState<any>(null)
  const [error, setError] = useState('')
  const [schema, setSchema] = useState('public')

  const execQuery = async () => {
    if (!sql.trim()) return antdMessage.warning('请输入SQL语句')
    setLoading(true); setError(''); setResult(null)
    try {
      const isSelect = sql.trim().toLowerCase().startsWith('select') || sql.trim().toLowerCase().startsWith('show')
      const toolName = isSelect ? 'db_query' : 'db_execute'
      const res = await axios.post(`/api/mcp/tools/${toolName}/execute`, { sql })
      const data = res.data?.data ?? res.data
      if (data?.success === false) throw new Error(data.errorMessage || '执行失败')
      // db_query的output是JSON字符串（行数据），db_execute的output是描述文本
      const output = data?.output ? (() => { try { return JSON.parse(data.output) } catch { return data.output } })() : data
      setResult(output)
    } catch (e: any) { setError(e.message) }
    finally { setLoading(false) }
  }

  const loadTables = async () => {
    setLoading(true); setError(''); setResult(null)
    try {
      const res = await axios.post('/api/mcp/tools/db_list_tables/execute', { schema })
      const data = res.data?.data ?? res.data
      if (data?.success === false) throw new Error(data.errorMessage || '执行失败')
      // db_list_tables的output是JSON字符串
      const output = data?.output ? (() => { try { return JSON.parse(data.output) } catch { return data.output } })() : data
      setResult(output)
    } catch (e: any) { setError(e.message) }
    finally { setLoading(false) }
  }

  const renderResult = () => {
    if (!result) return null
    const rows = Array.isArray(result) ? result : (result?.rows || result?.records || result?.data)
    if (Array.isArray(rows) && rows.length > 0 && typeof rows[0] === 'object') {
      const columns = Object.keys(rows[0])
      return (
        <div style={{ border: '1px solid #f0f0f0', borderRadius: 10, overflow: 'hidden' }}>
          <Table size="small" dataSource={rows.map((r: any, i: number) => ({ ...r, key: i }))}
            columns={columns.map(c => ({ title: c, dataIndex: c, key: c, ellipsis: true,
              render: (v: any) => <Text style={{ fontSize: 12 }}>{typeof v === 'object' ? JSON.stringify(v) : String(v ?? '-')}</Text> }))}
            pagination={{ pageSize: 10, size: 'small' }} scroll={{ x: 'max-content' }} />
        </div>
      )
    }
    return (
      <pre style={{ background: '#1e1e1e', color: '#d4d4d4', borderRadius: 10, padding: '16px 20px', fontSize: 12, lineHeight: 1.7, overflow: 'auto', maxHeight: 300, margin: 0 }}>
        {typeof result === 'string' ? result : JSON.stringify(result, null, 2)}
      </pre>
    )
  }

  return (
    <Modal open title={null} onCancel={onClose} footer={null} width={900} destroyOnClose
      styles={{ body: { padding: 0 } }}>
      <div style={{ background: '#ffffff', padding: '24px 32px 20px', borderBottom: '1px solid #f0f0f0' }}>
        <h2 style={{ color: '#1f2937', margin: 0, fontSize: 18, fontWeight: 700 }}>🗄️ 数据库查询</h2>
        <p style={{ color: '#6b7280', margin: '4px 0 0', fontSize: 13 }}>执行SQL查询，查看表结构与数据</p>
      </div>
      <div style={{ padding: '24px 32px 32px' }}>
        <div style={{ display: 'flex', gap: 12, marginBottom: 16 }}>
          <Button icon={<DatabaseOutlined />} onClick={loadTables} loading={loading}>查看所有表</Button>
          <Input value={schema} onChange={e => setSchema(e.target.value)} placeholder="schema" style={{ width: 120, borderRadius: 8 }} prefix={<Text type="secondary" style={{ fontSize: 11 }}>Schema:</Text>} />
        </div>

        <div style={{ marginBottom: 16 }}>
          <TextArea value={sql} onChange={e => setSql(e.target.value)}
            rows={5} placeholder="输入SQL语句，如：&#10;SELECT * FROM users LIMIT 10;&#10;SHOW TABLES;"
            style={{ fontFamily: '"Fira Code", Consolas, monospace', fontSize: 13, borderRadius: 10, background: '#fafafa' }} />
        </div>

        <div style={{ display: 'flex', gap: 12, marginBottom: 20 }}>
          <Button type="primary" size="large" icon={<PlayCircleOutlined />} onClick={execQuery}
            loading={loading} style={{ borderRadius: 10, minWidth: 120 }}>执行查询</Button>
          <div style={{ display: 'flex', gap: 8, marginLeft: 'auto' }}>
            {['SELECT * FROM vector_store LIMIT 5', 'SHOW TABLES'].map(s => (
              <Button key={s} size="small" style={{ borderRadius: 6, fontSize: 11 }} onClick={() => setSql(s)}>{s.length > 25 ? s.slice(0, 25) + '...' : s}</Button>
            ))}
          </div>
        </div>

        {error && <div style={{ color: '#ff4d4f', padding: 12, background: '#fff2f0', borderRadius: 8, marginBottom: 16 }}>{error}</div>}
        {renderResult()}
      </div>
    </Modal>
  )
}

// ======== 主组件 ========

function MCPHub() {
  const [toolGroups, setToolGroups] = useState<ToolGroup[]>([])
  const [loading, setLoading] = useState(true)
  const [searchText, setSearchText] = useState('')
  const [activeTab, setActiveTab] = useState('all')
  const [enabledMap, setEnabledMap] = useState<Record<string, boolean>>({})

  // 工具App弹窗状态
  const [activeApp, setActiveApp] = useState<string | null>(null)

  // 详情弹窗
  const [detailVisible, setDetailVisible] = useState(false)
  const [detailGroup, setDetailGroup] = useState<ToolGroup | null>(null)

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
        { groupName: 'WeatherTool',  category: 'weather',  description: '查询指定城市的未来天气预报（未来3天）', tools: [] },
        { groupName: 'ReadFileTool',  category: 'document', description: '读取指定路径的文件内容，支持PDF、Word、Markdown、TXT等格式', tools: [] },
        { groupName: 'WriteCodeTool', category: 'code', description: '将代码内容写入指定文件路径，支持自动创建目录', tools: [] },
        { groupName: 'GitTool', category: 'git', description: '获取Git仓库的所有分支', tools: [] },
        { groupName: 'DockerTool', category: 'docker', description: '获取Docker容器列表', tools: [] },
        { groupName: 'DiagramTool', category: 'diagram', description: '生成系统架构图的Mermaid代码', tools: [] },
        { groupName: 'DatabaseTool', category: 'database', description: '查看指定表的字段结构（列名、数据类型、是否可为空）', tools: [] },
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

  // -------- 打开工具App --------
  const openToolApp = (groupName: string) => {
    setActiveApp(groupName)
  }

  // -------- 详情弹窗 --------
  const openDetailModal = (group: ToolGroup) => { setDetailGroup(group); setDetailVisible(true) }

  // -------- 渲染卡片 --------
  const renderCard = (group: ToolGroup) => {
    const catCfg = CATEGORY_CONFIG[group.category] ?? { label: group.category, icon: <ToolOutlined />, color: '#1677ff', tagColor: 'blue' }
    const usageCount = USAGE_COUNTS[group.groupName] ?? 100
    const enabled = enabledMap[group.groupName] ?? true

    // 工具特色图标
    const toolEmoji: Record<string, string> = {
      WeatherTool: '🌤️', DockerTool: '🐳', GitTool: '📂',
      WriteCodeTool: '✏️', ReadFileTool: '📄', DiagramTool: '📊', DatabaseTool: '🗄️',
    }

    return (
      <div
        key={group.groupName}
        style={{
          background: '#fff', borderRadius: 16, border: '1px solid #f0f0f0',
          padding: '24px', cursor: 'pointer', transition: 'all 0.25s ease',
          opacity: enabled ? 1 : 0.55, position: 'relative', overflow: 'hidden',
        }}
        onMouseEnter={e => { (e.currentTarget as HTMLElement).style.transform = 'translateY(-4px)'; (e.currentTarget as HTMLElement).style.boxShadow = '0 12px 40px rgba(0,0,0,0.08)' }}
        onMouseLeave={e => { (e.currentTarget as HTMLElement).style.transform = 'translateY(0)'; (e.currentTarget as HTMLElement).style.boxShadow = 'none' }}
        onClick={() => enabled && openToolApp(group.groupName)}
      >
        {/* 背景装饰 */}
        <div style={{ position: 'absolute', top: -20, right: -20, width: 80, height: 80, borderRadius: '50%', background: `${catCfg.color}10`, pointerEvents: 'none' }} />

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 16 }}>
          <div style={{ width: 52, height: 52, borderRadius: 14, background: `${catCfg.color}12`, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 26 }}>
            {toolEmoji[group.groupName] || '🔧'}
          </div>
          <div onClick={e => e.stopPropagation()}>
            <Switch size="small" checked={enabled}
              onChange={v => setEnabledMap(prev => ({ ...prev, [group.groupName]: v }))} />
          </div>
        </div>

        <Text strong style={{ fontSize: 16, display: 'block', marginBottom: 6 }}>{group.groupName}</Text>
        <Paragraph ellipsis={{ rows: 2 }} style={{ color: '#8c8c8c', marginBottom: 14, fontSize: 13, lineHeight: 1.6 }}>
          {group.description}
        </Paragraph>

        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div style={{ display: 'flex', gap: 6 }}>
            <Tag color={catCfg.tagColor} style={{ borderRadius: 6, fontSize: 11 }}>{catCfg.label}</Tag>
          </div>
          <Text type="secondary" style={{ fontSize: 12 }}>使用 {usageCount} 次</Text>
        </div>

        {/* 底部操作栏 */}
        <div style={{ display: 'flex', gap: 10, marginTop: 16, paddingTop: 16, borderTop: '1px solid #f5f5f5' }}>
          <Button type="primary" icon={<PlayCircleOutlined />}
            disabled={!enabled} onClick={e => { e.stopPropagation(); openToolApp(group.groupName) }}
            style={{ flex: 1, borderRadius: 8, fontSize: 13 }} size="middle">
            立即使用
          </Button>
          <Button icon={<InfoCircleOutlined />}
            onClick={e => { e.stopPropagation(); openDetailModal(group) }}
            style={{ borderRadius: 8, fontSize: 13 }} size="middle">
            详情
          </Button>
        </div>
      </div>
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
            placeholder="搜索工具..."
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

      {/* ======== 各工具App弹窗 ======== */}
      {activeApp === 'WeatherTool' && <WeatherApp onClose={() => setActiveApp(null)} />}
      {activeApp === 'DockerTool' && <DockerApp onClose={() => setActiveApp(null)} />}
      {activeApp === 'GitTool' && <GitApp onClose={() => setActiveApp(null)} />}
      {activeApp === 'WriteCodeTool' && <WriteCodeApp onClose={() => setActiveApp(null)} />}
      {activeApp === 'ReadFileTool' && <ReadFileApp onClose={() => setActiveApp(null)} />}
      {activeApp === 'DiagramTool' && <DiagramApp onClose={() => setActiveApp(null)} />}
      {activeApp === 'DatabaseTool' && <DatabaseApp onClose={() => setActiveApp(null)} />}

      {/* ======== 详情弹窗 ======== */}
      <Modal
        title={<Space><InfoCircleOutlined style={{ color: '#1677ff' }} />工具详情：{detailGroup?.groupName}</Space>}
        open={detailVisible}
        onCancel={() => setDetailVisible(false)}
        footer={<Button onClick={() => setDetailVisible(false)}>关闭</Button>}
        width={720}
      >
        {detailGroup && (
          <>
            <div style={{ marginBottom: 16, padding: '12px 16px', background: '#fafafa', borderRadius: 8 }}>
              <Space wrap>
                {CATEGORY_CONFIG[detailGroup.category] && (
                  <Tag color={CATEGORY_CONFIG[detailGroup.category].tagColor}>{CATEGORY_CONFIG[detailGroup.category].label}</Tag>
                )}
                <Tag color="geekblue">{detailGroup.tools.length} 个方法</Tag>
                <Tag>使用: {USAGE_COUNTS[detailGroup.groupName] ?? '-'}次</Tag>
              </Space>
              <Paragraph style={{ marginTop: 10, marginBottom: 0, color: '#595959' }}>{detailGroup.description}</Paragraph>
            </div>
            {detailGroup.tools.length === 0 ? (
              <Empty description="MCP Server 未连接，方法列表不可用" />
            ) : (
              detailGroup.tools.map((tool, idx) => (
                <div key={tool.name} style={{ marginBottom: 16 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 6 }}>
                    <Text strong style={{ fontSize: 14 }}>{tool.description || tool.name}</Text>
                    {tool.async && <Tag color="orange">异步</Tag>}
                  </div>
                  <Text type="secondary" style={{ fontSize: 12 }}>{tool.name}</Text>
                  {tool.parameters.length > 0 && (
                    <Table size="small" dataSource={tool.parameters} rowKey="name" pagination={false}
                      style={{ marginTop: 8 }}
                      columns={[
                        { title: '参数', dataIndex: 'name', render: (v: string) => <Text code>{v}</Text> },
                        { title: '类型', dataIndex: 'type', render: (v: string) => <Tag>{v}</Tag> },
                        { title: '必填', dataIndex: 'required', render: (v: boolean) => v ? <Tag color="red">必填</Tag> : <Tag>可选</Tag> },
                        { title: '描述', dataIndex: 'description', render: (v: string) => v || '-' },
                      ]}
                    />
                  )}
                  {idx < detailGroup.tools.length - 1 && <Divider style={{ margin: '12px 0' }} />}
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
