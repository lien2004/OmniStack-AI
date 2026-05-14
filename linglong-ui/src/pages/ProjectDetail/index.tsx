import { useParams, useNavigate } from 'react-router-dom'
import {
  Card,
  Descriptions,
  Tag,
  Button,
  Tabs,
  Empty,
  Spin,
  Space,
  message,
  Typography,
} from 'antd'
import {
  PlayCircleOutlined,
  ArrowLeftOutlined,
  CodeOutlined,
  FileTextOutlined,
  FolderOutlined,
  FolderOpenOutlined,
  FileOutlined,
  DownloadOutlined,
  ThunderboltOutlined,
  CopyOutlined,
} from '@ant-design/icons'
import { useState, useEffect } from 'react'
import { getProjectDetail } from '@/services/api'
import JSZip from 'jszip'
import { saveAs } from 'file-saver'

const { Text } = Typography

const statusMap: Record<string, { color: string; text: string }> = {
  init: { color: 'default', text: '初始化' },
  analyzing: { color: 'blue', text: '需求分析中' },
  designing: { color: 'orange', text: '架构设计中' },
  coding: { color: 'processing', text: '代码生成中' },
  testing: { color: 'purple', text: '测试验证中' },
  completed: { color: 'success', text: '已完成' },
  failed: { color: 'error', text: '失败' },
}

interface CodeFile {
  path: string
  content: string
  lang: string
}

function ProjectDetail() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [loading, setLoading] = useState(true)
  const [project, setProject] = useState<any>(null)
  const [codeFiles, setCodeFiles] = useState<CodeFile[]>([])
  const [selectedFilePath, setSelectedFilePath] = useState<string | null>(null)
  const [fileTreeExpanded, setFileTreeExpanded] = useState(true)
  const [activeTab, setActiveTab] = useState('code')

  useEffect(() => {
    if (!id) return
    loadProject()
  }, [id])

  const loadProject = async () => {
    setLoading(true)
    try {
      const res = await getProjectDetail(id!)
      if (res.data?.success) {
        const p = res.data.data
        setProject(p)
        // 解析代码文件
        if (p.codeFiles) {
          try {
            const files = JSON.parse(p.codeFiles)
            setCodeFiles(files || [])
            if (files?.length > 0) {
              setSelectedFilePath(files[0].path)
            }
          } catch {
            setCodeFiles([])
          }
        }
      } else {
        message.error('项目不存在')
        navigate('/projects')
      }
    } catch (err) {
      message.error('加载项目失败')
      navigate('/projects')
    } finally {
      setLoading(false)
    }
  }

  // 启动AI开发
  const handleStartAIDev = () => {
    if (!project) return
    navigate('/codeflow', {
      state: {
        projectId: project.id,
        requirement: project.requirement,
        model: project.model,
        projectName: project.name,
      },
    })
  }

  // 下载单个文件
  const downloadSingleFile = (file: CodeFile) => {
    const blob = new Blob([file.content], { type: 'text/plain;charset=utf-8' })
    saveAs(blob, file.path.split('/').pop() || 'file.txt')
  }

  // 打包下载所有文件
  const downloadAllAsZip = async () => {
    if (codeFiles.length === 0) return
    const zip = new JSZip()
    codeFiles.forEach(file => {
      zip.file(file.path, file.content)
    })
    const blob = await zip.generateAsync({ type: 'blob' })
    saveAs(blob, `${project?.name || 'project'}-${Date.now()}.zip`)
    message.success(`已打包 ${codeFiles.length} 个文件`)
  }

  // 复制代码
  const copyCode = () => {
    const file = codeFiles.find(f => f.path === selectedFilePath)
    if (file) {
      navigator.clipboard.writeText(file.content)
      message.success('代码已复制到剪贴板')
    }
  }

  // 构建文件树
  const buildFileTree = (files: CodeFile[]) => {
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

  const renderFileTree = (node: Record<string, any>, level = 0) => {
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
          </div>
        )
      }
      return (
        <div key={name}>
          <div style={{ padding: '4px 8px 4px ' + (12 + level * 16) + 'px', fontSize: 13, color: '#8c8c8c', display: 'flex', alignItems: 'center', gap: 6, fontWeight: 500 }}>
            <FolderOutlined style={{ fontSize: 12 }} />
            {name}
          </div>
          {renderFileTree(child, level + 1)}
        </div>
      )
    })
  }

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: 400 }}>
        <Spin size="large" />
      </div>
    )
  }

  if (!project) {
    return <Empty description="项目不存在" />
  }

  const statusInfo = statusMap[project.status] || { color: 'default', text: project.status }

  return (
    <div className="project-detail">
      {/* 顶部信息卡片 */}
      <Card
        title={
          <Space>
            <Button type="text" icon={<ArrowLeftOutlined />} onClick={() => navigate('/projects')} />
            <span style={{ fontSize: 18, fontWeight: 600 }}>{project.name}</span>
            <Tag color={statusInfo.color}>{statusInfo.text}</Tag>
          </Space>
        }
        extra={
          <Space>
            {project.status !== 'completed' && (
              <Button icon={<ThunderboltOutlined />} type="primary" onClick={handleStartAIDev}>
                启动AI开发
              </Button>
            )}
            {project.status === 'completed' && codeFiles.length > 0 && (
              <Button icon={<DownloadOutlined />} onClick={downloadAllAsZip}>
                打包下载
              </Button>
            )}
            {project.status === 'completed' && (
              <Button icon={<PlayCircleOutlined />} onClick={handleStartAIDev}>
                重新开发
              </Button>
            )}
          </Space>
        }
      >
        <Descriptions bordered column={2} size="small">
          <Descriptions.Item label="项目ID">
            <Text copyable style={{ fontSize: 12 }}>{project.id}</Text>
          </Descriptions.Item>
          <Descriptions.Item label="技术栈">{project.techStack || '-'}</Descriptions.Item>
          <Descriptions.Item label="架构类型">{project.architectureType || '-'}</Descriptions.Item>
          <Descriptions.Item label="AI模型">{project.model || '-'}</Descriptions.Item>
          <Descriptions.Item label="创建时间">
            {project.createTime ? new Date(project.createTime).toLocaleString('zh-CN') : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="更新时间">
            {project.updateTime ? new Date(project.updateTime).toLocaleString('zh-CN') : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="描述" span={2}>
            {project.description || '-'}
          </Descriptions.Item>
        </Descriptions>
      </Card>

      {/* 内容区域 */}
      <Card style={{ marginTop: 16 }}>
        <Tabs activeKey={activeTab} onChange={setActiveTab} items={[
          {
            key: 'code',
            label: <span><CodeOutlined /> 生成代码 {codeFiles.length > 0 && <Tag style={{ marginLeft: 4 }}>{codeFiles.length}</Tag>}</span>,
            children: codeFiles.length === 0 ? (
              <Empty description="暂无代码文件" style={{ padding: 60 }}>
                <Button type="primary" icon={<ThunderboltOutlined />} onClick={handleStartAIDev}>
                  启动AI开发生成代码
                </Button>
              </Empty>
            ) : (
              <Card style={{ borderRadius: 12, border: '1px solid #f0f0f0', overflow: 'hidden', padding: 0 }} styles={{ body: { padding: 0 } }}>
                <div style={{ display: 'flex', height: 520 }}>
                  {/* 左侧文件树 */}
                  <div style={{ width: 260, borderRight: '1px solid #f0f0f0', background: '#fafafa', overflow: 'auto' }}>
                    <div
                      style={{ padding: '12px 16px', fontSize: 12, fontWeight: 600, color: '#8c8c8c', display: 'flex', alignItems: 'center', gap: 6, cursor: 'pointer' }}
                      onClick={() => setFileTreeExpanded(!fileTreeExpanded)}
                    >
                      {fileTreeExpanded ? <FolderOpenOutlined /> : <FolderOutlined />}
                      文件目录 ({codeFiles.length})
                    </div>
                    {fileTreeExpanded && (
                      <div style={{ padding: '0 8px 12px' }}>
                        {renderFileTree(buildFileTree(codeFiles))}
                      </div>
                    )}
                  </div>

                  {/* 右侧代码预览 */}
                  <div style={{ flex: 1, display: 'flex', flexDirection: 'column', background: '#fff' }}>
                    {selectedFilePath ? (
                      <>
                        <div style={{ padding: '10px 16px', borderBottom: '1px solid #f0f0f0', display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: '#fafafa' }}>
                          <Space>
                            <FileOutlined style={{ color: '#1677ff' }} />
                            <Text style={{ fontWeight: 500, fontSize: 13 }}>{selectedFilePath}</Text>
                            <Tag style={{ fontSize: 11, margin: 0 }}>{codeFiles.find(f => f.path === selectedFilePath)?.lang || 'text'}</Tag>
                          </Space>
                          <Space>
                            <Button size="small" icon={<CopyOutlined />} onClick={copyCode}>复制</Button>
                            <Button size="small" icon={<DownloadOutlined />} onClick={() => {
                              const f = codeFiles.find(x => x.path === selectedFilePath)
                              if (f) downloadSingleFile(f)
                            }}>下载</Button>
                          </Space>
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
            ),
          },
          {
            key: 'requirement',
            label: <span><FileTextOutlined /> 需求描述</span>,
            children: (
              <div style={{ padding: 16 }}>
                {project.requirement ? (
                  <pre style={{
                    margin: 0, fontSize: 14, lineHeight: 1.8,
                    whiteSpace: 'pre-wrap', wordBreak: 'break-word',
                    fontFamily: "'Segoe UI', system-ui, sans-serif",
                    color: '#262626', background: '#fafafa', padding: 20, borderRadius: 8,
                  }}>
                    {project.requirement}
                  </pre>
                ) : (
                  <Empty description="暂无需求描述" />
                )}
              </div>
            ),
          },
        ]} />
      </Card>
    </div>
  )
}

export default ProjectDetail
