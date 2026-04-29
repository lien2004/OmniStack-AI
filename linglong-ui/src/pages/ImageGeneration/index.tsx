import { useState, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Button,
  Input,
  Card,
  Tabs,
  Upload,
  message,
  Spin,
  Empty,
  Tag,
  Space,
  Tooltip,
} from 'antd'
import type { UploadFile, UploadProps } from 'antd/es/upload/interface'
import {
  PictureOutlined,
  UploadOutlined,
  DownloadOutlined,
  ArrowLeftOutlined,
  BulbOutlined,
  DeleteOutlined,
  ThunderboltOutlined,
  FileImageOutlined,
} from '@ant-design/icons'
import { generateImage } from '@/services/api'
import './style.css'

const { TextArea } = Input
const { TabPane } = Tabs

interface GeneratedImage {
  format: string
  base64: string
  dataUrl: string
}

const EXAMPLE_PROMPTS = [
  '一只穿着宇航服的猫咪在月球上漫步，背景是璀璨的星空和蓝色的地球',
  '赛博朋克风格的中国古城，霓虹灯照亮了古老的城墙和运河',
  '一片梦幻的樱花树林，花瓣随风飘落，阳光透过树枝洒下金色的光芒',
  '未来城市的夜景，飞行的汽车，全息广告牌，科技感十足',
]

function ImageGeneration() {
  const navigate = useNavigate()
  const [mode, setMode] = useState<'text' | 'image'>('text')
  const [prompt, setPrompt] = useState('')
  const [fileList, setFileList] = useState<UploadFile[]>([])
  const [generating, setGenerating] = useState(false)
  const [generatedImages, setGeneratedImages] = useState<GeneratedImage[]>([])
  const [history, setHistory] = useState<{ prompt: string; images: GeneratedImage[]; mode: string }[]>([])
  const resultRef = useRef<HTMLDivElement>(null)

  const uploadProps: UploadProps = {
    listType: 'picture-card',
    maxCount: 1,
    fileList,
    beforeUpload: (file) => {
      const isImage = file.type.startsWith('image/')
      if (!isImage) {
        message.error('只能上传图片文件!')
        return Upload.LIST_IGNORE
      }
      const isLt10M = file.size / 1024 / 1024 < 10
      if (!isLt10M) {
        message.error('图片大小不能超过 10MB!')
        return Upload.LIST_IGNORE
      }
      setFileList([file])
      return false
    },
    onRemove: () => {
      setFileList([])
    },
  }

  const handleGenerate = async () => {
    if (!prompt.trim()) {
      message.warning('请输入图片描述')
      return
    }
    if (mode === 'image' && fileList.length === 0) {
      message.warning('请上传一张参考图片')
      return
    }

    setGenerating(true)
    setGeneratedImages([])

    try {
      const formData = new FormData()
      formData.append('prompt', prompt)
      if (mode === 'image' && fileList.length > 0) {
        const file = fileList[0] as any
        const fileObj = file.originFileObj || file
        formData.append('image', fileObj)
      }

      const res = await generateImage(formData)

      if (res.data?.code === 200 && res.data?.success) {
        const images: GeneratedImage[] = res.data.data || []
        setGeneratedImages(images)
        setHistory((prev) => [{ prompt, images, mode }, ...prev].slice(0, 20))
        message.success(`成功生成 ${images.length} 张图片`)
        setTimeout(() => {
          resultRef.current?.scrollIntoView({ behavior: 'smooth' })
        }, 200)
      } else {
        message.error(res.data?.message || '图片生成失败')
      }
    } catch (error: any) {
      console.error('图片生成失败', error)
      message.error(error.response?.data?.message || error.message || '图片生成请求失败')
    } finally {
      setGenerating(false)
    }
  }

  const handleDownload = (image: GeneratedImage, index: number) => {
    const link = document.createElement('a')
    link.href = image.dataUrl
    link.download = `generated-image-${Date.now()}-${index + 1}.${image.format}`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    message.success('图片下载已开始')
  }

  const handleUseExample = (text: string) => {
    setPrompt(text)
  }

  const handleClear = () => {
    setPrompt('')
    setFileList([])
    setGeneratedImages([])
  }

  return (
    <div className="image-generation-page">
      {/* 页面头部 */}
      <div className="g-page-header">
        <div className="g-page-header-left">
          <Button
            icon={<ArrowLeftOutlined />}
            onClick={() => navigate('/')}
            style={{ marginRight: 12 }}
          >
            返回
          </Button>
          <h1 className="g-page-title">AI 图片生成</h1>
          <p className="g-page-subtitle">基于智谱 GLM-Image 模型，支持文生图创作</p>
        </div>
      </div>

      <div style={{ padding: '24px 32px' }}>
        <div className="image-gen-container">
          {/* 左侧输入区 */}
          <div className="image-gen-input-panel">
            <Card
              title={
                <Space>
                  <PictureOutlined style={{ color: '#1677ff' }} />
                  <span>创作面板</span>
                </Space>
              }
              extra={
                <Button type="link" danger icon={<DeleteOutlined />} onClick={handleClear}>
                  清空
                </Button>
              }
            >
              {/* 模式切换 */}
              <Tabs
                activeKey={mode}
                onChange={(key) => {
                  setMode(key as 'text' | 'image')
                  setGeneratedImages([])
                }}
                className="image-gen-tabs"
              >
                <TabPane
                  tab={
                    <Space>
                      <BulbOutlined />
                      文生图
                    </Space>
                  }
                  key="text"
                />
                <TabPane
                  tab={
                    <Space>
                      <FileImageOutlined />
                      图生图
                    </Space>
                  }
                  key="image"
                />
              </Tabs>

              {/* 提示词输入 */}
              <div style={{ marginBottom: 16 }}>
                <div style={{ marginBottom: 8, fontWeight: 500 }}>
                  {mode === 'text' ? '图片描述' : '参考图片 + 修改描述'}
                </div>
                <TextArea
                  rows={4}
                  value={prompt}
                  onChange={(e) => setPrompt(e.target.value)}
                  placeholder={
                    mode === 'text'
                      ? '请详细描述您想要生成的画面内容，例如：风格、场景、主体、颜色、光线等...'
                      : '请描述您希望对参考图片进行的修改或风格转换...'
                  }
                  maxLength={1000}
                  showCount
                />
              </div>

              {/* 图生图：图片上传 */}
              {mode === 'image' && (
                <div style={{ marginBottom: 16 }}>
                  <div style={{ marginBottom: 8, fontWeight: 500 }}>参考图片</div>
                  <Upload {...uploadProps}>
                    {fileList.length < 1 && (
                      <div style={{ textAlign: 'center' }}>
                        <UploadOutlined style={{ fontSize: 24 }} />
                        <div style={{ marginTop: 8 }}>上传图片</div>
                      </div>
                    )}
                  </Upload>
                  <div style={{ marginTop: 8, color: '#999', fontSize: 12 }}>
                    支持 JPG、PNG、WEBP 格式，单张不超过 10MB
                  </div>
                </div>
              )}

              {/* 示例提示词 */}
              {mode === 'text' && (
                <div style={{ marginBottom: 16 }}>
                  <div style={{ marginBottom: 8, color: '#666', fontSize: 13 }}>示例提示词（点击使用）：</div>
                  <Space size={8} wrap>
                    {EXAMPLE_PROMPTS.map((text, idx) => (
                      <Tag
                        key={idx}
                        color="blue"
                        style={{ cursor: 'pointer', maxWidth: 280, whiteSpace: 'normal' }}
                        onClick={() => handleUseExample(text)}
                      >
                        {text.length > 20 ? text.slice(0, 20) + '...' : text}
                      </Tag>
                    ))}
                  </Space>
                </div>
              )}

              {/* 生成按钮 */}
              <Button
                type="primary"
                size="large"
                block
                icon={<ThunderboltOutlined />}
                loading={generating}
                onClick={handleGenerate}
                style={{ height: 48, fontSize: 16 }}
              >
                {generating ? '正在生成中...' : mode === 'text' ? '开始文生图' : '开始图生图'}
              </Button>
            </Card>
          </div>

          {/* 右侧结果区 */}
          <div className="image-gen-result-panel" ref={resultRef}>
            <Card
              title={
                <Space>
                  <PictureOutlined style={{ color: '#52c41a' }} />
                  <span>生成结果</span>
                  {generatedImages.length > 0 && (
                    <Tag color="success">{generatedImages.length} 张</Tag>
                  )}
                </Space>
              }
            >
              {generating ? (
                <div className="image-gen-loading">
                  <Spin size="large" tip="AI 正在创作中，请稍候..." />
                  <p style={{ marginTop: 16, color: '#999' }}>
                    GLM-Image 模型正在根据您的描述生成图片，这个过程可能需要 10~30 秒
                  </p>
                </div>
              ) : generatedImages.length > 0 ? (
                <div className="image-gen-results">
                  {generatedImages.map((img, idx) => (
                    <div key={idx} className="image-gen-result-item">
                      <div className="image-gen-img-wrapper">
                        <img
                          src={img.dataUrl}
                          alt={`生成图片 ${idx + 1}`}
                          className="image-gen-img"
                        />
                      </div>
                      <div className="image-gen-img-actions">
                        <Tooltip title="下载图片">
                          <Button
                            type="primary"
                            icon={<DownloadOutlined />}
                            onClick={() => handleDownload(img, idx)}
                          >
                            下载
                          </Button>
                        </Tooltip>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <Empty
                  image={Empty.PRESENTED_IMAGE_SIMPLE}
                  description={
                    <div>
                      <div>暂无生成结果</div>
                      <div style={{ fontSize: 12, color: '#999', marginTop: 8 }}>
                        在左侧输入描述并点击生成按钮开始创作
                      </div>
                    </div>
                  }
                />
              )}
            </Card>

            {/* 历史记录 */}
            {history.length > 0 && !generating && (
              <Card
                title="最近生成"
                style={{ marginTop: 16 }}
                size="small"
              >
                <div className="image-gen-history">
                  {history.map((item, idx) => (
                    <div
                      key={idx}
                      className="image-gen-history-item"
                      onClick={() => {
                        setPrompt(item.prompt)
                        setMode(item.mode as 'text' | 'image')
                        setGeneratedImages(item.images)
                      }}
                    >
                      <img
                        src={item.images[0]?.dataUrl}
                        alt=""
                        className="image-gen-history-thumb"
                      />
                      <div className="image-gen-history-info">
                        <div className="image-gen-history-prompt">{item.prompt}</div>
                        <Space size={4}>
                          <Tag>{item.mode === 'text' ? '文生图' : '图生图'}</Tag>
                          <span style={{ fontSize: 12, color: '#999' }}>
                            {item.images.length} 张
                          </span>
                        </Space>
                      </div>
                    </div>
                  ))}
                </div>
              </Card>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

export default ImageGeneration
