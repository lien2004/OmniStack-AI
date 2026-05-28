import { useState, useRef } from 'react'
import {
  Button, Card, Input, Select, Tag, Typography, Space,
  Spin, Tooltip, message as antdMessage, Steps,
} from 'antd'
import {
  ThunderboltOutlined, FilePptOutlined,
  EyeOutlined, FilePdfOutlined, Html5Outlined,
  ClearOutlined, BulbOutlined,
} from '@ant-design/icons'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import pptxgen from 'pptxgenjs'
import html2canvas from 'html2canvas'
import jsPDF from 'jspdf'
import './style.css'

const { TextArea } = Input
const { Text, Title } = Typography

const STYLE_OPTIONS = [
  { value: 'professional', label: '商务专业', desc: '数据驱动，适合企业汇报' },
  { value: 'education', label: '教育学术', desc: '严谨权威，适合课件学术' },
  { value: 'creative', label: '创意设计', desc: '视觉冲击，适合品牌发布' },
  { value: 'minimal', label: '极简风格', desc: '大字少量，适合TED演讲' },
]

interface Slide {
  pageNum: number
  title: string
  bullets: string[]
  notes: string[]
}

const PPTBuilder: React.FC = () => {
  const [topic, setTopic] = useState('')
  const [style, setStyle] = useState('professional')
  const [loading, setLoading] = useState(false)
  const [streamOutput, setStreamOutput] = useState('')
  const [slides, setSlides] = useState<Slide[]>([])
  const [currentStep, setCurrentStep] = useState(0)
  const [exporting, setExporting] = useState<string | null>(null)
  const previewRef = useRef<HTMLDivElement>(null)

  const parseSlides = (markdown: string): Slide[] => {
    const parsed: Slide[] = []
    const lines = markdown.split('\n')
    let currentSlide: Slide | null = null
    let inNotes = false

    for (const line of lines) {
      // Match "## Page N: Title" or "## 第N页：Title"
      const pageMatch = line.match(/^##\s*(?:第\s*(\d+)\s*页[：:]\s*)?(.+)/)
      if (pageMatch) {
        if (currentSlide) parsed.push(currentSlide)
        currentSlide = {
          pageNum: pageMatch[1] ? parseInt(pageMatch[1]) : parsed.length + 1,
          title: pageMatch[2].trim(),
          bullets: [],
          notes: [],
        }
        inNotes = false
        continue
      }

      // Match H1 title (PPT title slide)
      if (line.startsWith('# ') && parsed.length === 0) {
        currentSlide = {
          pageNum: 1,
          title: line.replace(/^#\s*/, '').trim(),
          bullets: [],
          notes: [],
        }
        continue
      }

      if (!currentSlide) continue

      // Notes
      if (line.trim().startsWith('> ')) {
        currentSlide.notes.push(line.trim().replace(/^>\s*/, ''))
        inNotes = true
        continue
      }

      // Bullet points
      if (line.trim().startsWith('- ') || line.trim().startsWith('* ')) {
        const bullet = line.trim().replace(/^[-*]\s*/, '')
        if (bullet && !bullet.startsWith('```') && !bullet.startsWith('#')) {
          currentSlide.bullets.push(bullet)
        }
        continue
      }

      // Content after bullet
      if (line.trim() && !inNotes && !line.startsWith('#') && !line.startsWith('```')) {
        const trimmed = line.trim()
        if (trimmed.length > 0 && !trimmed.startsWith('|')) {
          currentSlide.bullets.push(trimmed)
        }
      }
    }
    if (currentSlide) parsed.push(currentSlide)
    return parsed
  }

  const handleGenerate = async () => {
    if (!topic.trim()) {
      antdMessage.warning('请输入PPT主题或内容大纲')
      return
    }
    setLoading(true)
    setStreamOutput('')
    setSlides([])
    setCurrentStep(1)

    try {
      const response = await fetch('/api/agent/PptAgent/stream', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          input: topic.trim(),
          config: { style },
        }),
      })

      if (!response.ok) throw new Error(`HTTP ${response.status}`)
      if (!response.body) throw new Error('响应体为空')

      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''
      let fullContent = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() ?? ''

        for (const line of lines) {
          if (line.startsWith('data:')) {
            const data = line.slice(5).trim()
            if (data === '[DONE]') continue

            // Check for step indicator
            if (data.includes('```step')) {
              try {
                const stepJson = data.replace(/```step\s*/, '').replace(/```\s*$/, '')
                const step = JSON.parse(stepJson)
                setCurrentStep(step.step)
                setStreamOutput(prev => prev + `\n📋 **${step.title}**：${step.description}\n\n---\n\n`)
              } catch {
                fullContent += data
                setStreamOutput(prev => prev + data)
              }
            } else {
              fullContent += data
              setStreamOutput(prev => prev + data)
            }
          }
        }
      }

      // Parse slides after streaming completes
      const parsed = parseSlides(fullContent)
      setSlides(parsed.length > 0 ? parsed : fallbackParseSlides(fullContent))
      setCurrentStep(4)
    } catch (e: any) {
      antdMessage.error('PPT生成失败：' + (e.message || '未知错误'))
    } finally {
      setLoading(false)
    }
  }

  /** Fallback: derive slides from any markdown with headings */
  const fallbackParseSlides = (md: string): Slide[] => {
    const sections = md.split(/(?=^## )/m)
    return sections.map((sec, i) => {
      const lines = sec.trim().split('\n')
      return {
        pageNum: i + 1,
        title: lines[0].replace(/^##\s*/, ''),
        bullets: lines.slice(1).filter(l => l.trim() && !l.startsWith('> ')),
        notes: lines.filter(l => l.trim().startsWith('> ')).map(l => l.replace(/^>\s*/, '')),
      }
    })
  }

  // ---- Export functions ----

  const exportPPTX = async () => {
    setExporting('pptx')
    try {
      const pres = new pptxgen()
      pres.layout = 'LAYOUT_WIDE'
      pres.author = '灵龙AI'
      pres.title = topic || 'AI生成的演示文稿'

      for (const slide of slides) {
        const s = pres.addSlide()
        s.addText(slide.title, {
          x: 0.8, y: 0.4, w: '85%', h: 0.6,
          fontSize: 22, bold: true, color: '1F2937',
        })
        if (slide.pageNum > 1) {
          s.addShape(pres.ShapeType.rect, {
            x: 0, y: 0, w: '100%', h: 0.08,
            fill: { color: '1677ff' },
          })
        }
        const bullets = slide.bullets.map(b => ({ text: b, options: { fontSize: 14, color: '4B5563', bullet: true } }))
        s.addText(bullets, {
          x: 0.8, y: 1.2, w: '85%', h: 3.5,
          fontSize: 14, color: '4B5563', lineSpacing: 28,
        })
        if (slide.notes.length > 0) {
          s.addText('演讲备注: ' + slide.notes[0], {
            x: 0.8, y: 5.0, w: '85%', h: 0.4,
            fontSize: 9, color: '9CA3AF', italic: true,
          })
        }
      }

      await pres.writeFile({ fileName: `${topic || 'presentation'}.pptx` })
      antdMessage.success('PPTX 已下载')
    } catch (e: any) {
      antdMessage.error('导出PPTX失败：' + e.message)
    } finally {
      setExporting(null)
    }
  }

  const exportPDF = async () => {
    if (!previewRef.current) return
    setExporting('pdf')
    try {
      const canvas = await html2canvas(previewRef.current, {
        scale: 2,
        backgroundColor: '#ffffff',
        useCORS: true,
      })
      const imgData = canvas.toDataURL('image/png')
      const pdf = new jsPDF('landscape', 'mm', 'a4')
      const pageWidth = pdf.internal.pageSize.getWidth()
      const pageHeight = pdf.internal.pageSize.getHeight()
      const imgWidth = pageWidth
      const imgHeight = (canvas.height * pageWidth) / canvas.width
      let heightLeft = imgHeight
      let position = 0

      pdf.addImage(imgData, 'PNG', 0, position, imgWidth, imgHeight)
      heightLeft -= pageHeight

      while (heightLeft > 0) {
        position = -(imgHeight - heightLeft)
        pdf.addPage()
        pdf.addImage(imgData, 'PNG', 0, position, imgWidth, imgHeight)
        heightLeft -= pageHeight
      }

      pdf.save(`${topic || 'presentation'}.pdf`)
      antdMessage.success('PDF 已下载')
    } catch (e: any) {
      antdMessage.error('导出PDF失败：' + e.message)
    } finally {
      setExporting(null)
    }
  }

  const exportHTML = () => {
    const htmlContent = buildHTML(slides, topic)
    const blob = new Blob([htmlContent], { type: 'text/html;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    window.open(url, '_blank')
    URL.revokeObjectURL(url)
    antdMessage.success('HTML 已在新标签页打开')
  }

  const buildHTML = (s: Slide[], title: string) => `<!DOCTYPE html>
<html><head><meta charset="UTF-8"><title>${title}</title>
<style>
  body{font-family:-apple-system,sans-serif;margin:0;background:#f0f2f5}
  .slide{width:960px;min-height:540px;margin:40px auto;background:#fff;
    border-radius:12px;box-shadow:0 4px 24px rgba(0,0,0,.08);padding:48px 64px;page-break-after:always}
  h2{font-size:28px;color:#1f2937;border-bottom:3px solid #1677ff;padding-bottom:12px;margin:0 0 24px}
  ul{font-size:16px;color:#4b5563;line-height:2}
  .notes{font-size:12px;color:#9ca3af;border-left:3px solid #e5e7eb;padding-left:12px;margin-top:24px}
</style></head><body>
  ${s.map(sl => `
    <div class="slide">
      <h2>${sl.title}</h2>
      <ul>${sl.bullets.map(b => `<li>${b}</li>`).join('')}</ul>
      ${sl.notes.length ? `<div class="notes">${sl.notes[0]}</div>` : ''}
    </div>`).join('')}
</body></html>`

  const steps = [
    { title: '分析主题', description: '解析核心要点' },
    { title: '构建大纲', description: '规划内容结构' },
    { title: '生成内容', description: '逐页撰写文案' },
    { title: '完成', description: '预览与导出' },
  ]

  return (
    <div className="ppt-builder">
      {/* Header */}
      <div className="ppt-header">
        <div className="ppt-header-left">
          <FilePptOutlined className="ppt-icon" />
          <div>
            <Title level={4} style={{ margin: 0 }}>灵龙PPT</Title>
            <Text type="secondary" style={{ fontSize: 12 }}>
              输入主题，AI 自动生成演示文稿 · 支持导出 PPTX / PDF / HTML
            </Text>
          </div>
        </div>
        <Tag color="purple">DeepSeek V4 Pro</Tag>
      </div>

      <div className="ppt-body">
        {/* Input card */}
        <Card className="ppt-input-card" size="small">
          <Text strong style={{ display: 'block', marginBottom: 8 }}>PPT 主题或内容大纲</Text>
          <TextArea
            value={topic}
            onChange={e => setTopic(e.target.value)}
            placeholder="输入您想要生成的PPT主题，例如：『2025年AI行业发展趋速报告』或粘贴已有的内容大纲..."
            autoSize={{ minRows: 3, maxRows: 6 }}
            disabled={loading}
            style={{ borderRadius: 8 }}
          />
          <div className="ppt-input-row">
            <Space size={12} wrap>
              <Select
                value={style}
                onChange={setStyle}
                options={STYLE_OPTIONS}
                style={{ width: 140 }}
                disabled={loading}
              />
              <Button
                type="primary"
                icon={<ThunderboltOutlined />}
                onClick={handleGenerate}
                loading={loading}
                disabled={!topic.trim()}
                size="large"
              >
                {loading ? 'AI 正在生成...' : '开始生成'}
              </Button>
              {streamOutput && !loading && (
                <Button
                  icon={<ClearOutlined />}
                  onClick={() => { setStreamOutput(''); setSlides([]); setTopic(''); setCurrentStep(0) }}
                >
                  清空重来
                </Button>
              )}
            </Space>

            {slides.length > 0 && (
              <Space size={4}>
                <Tooltip title="下载 PPTX">
                  <Button
                    icon={<FilePptOutlined />}
                    onClick={exportPPTX}
                    loading={exporting === 'pptx'}
                    type="primary"
                    ghost
                  >
                    PPTX
                  </Button>
                </Tooltip>
                <Tooltip title="下载 PDF">
                  <Button
                    icon={<FilePdfOutlined />}
                    onClick={exportPDF}
                    loading={exporting === 'pdf'}
                  >
                    PDF
                  </Button>
                </Tooltip>
                <Tooltip title="在新标签页打开 HTML">
                  <Button icon={<Html5Outlined />} onClick={exportHTML}>
                    HTML
                  </Button>
                </Tooltip>
              </Space>
            )}
          </div>
        </Card>

        {/* Steps indicator */}
        {(loading || streamOutput) && (
          <Card size="small" className="ppt-steps-card">
            <Steps
              current={currentStep - 1}
              size="small"
              items={steps.map(s => ({ title: s.title, description: s.description }))}
            />
          </Card>
        )}

        {/* Preview area */}
        <div className="ppt-content-area">
          {/* Raw markdown output */}
          {streamOutput && (
            <Card
              size="small"
              title={<><EyeOutlined /> 生成内容</>}
              className="ppt-stream-card"
            >
              <Spin spinning={loading} tip="AI 正在撰写...">
                <div className="ppt-stream-content">
                  <ReactMarkdown remarkPlugins={[remarkGfm]}>
                    {streamOutput}
                  </ReactMarkdown>
                </div>
              </Spin>
            </Card>
          )}

          {/* Slide preview */}
          {slides.length > 0 && (
            <Card
              size="small"
              title={<><BulbOutlined /> 幻灯片预览</>}
              extra={<Tag>{slides.length} 页</Tag>}
              className="ppt-preview-card"
            >
              <div ref={previewRef} className="ppt-slide-preview">
                {slides.map((slide, idx) => (
                  <div key={idx} className="ppt-slide">
                    <div className="ppt-slide-header">
                      <span className="ppt-slide-num">{slide.pageNum}</span>
                      <h2>{slide.title}</h2>
                    </div>
                    <ul className="ppt-slide-bullets">
                      {slide.bullets.map((b, bi) => (
                        <li key={bi}>{b}</li>
                      ))}
                    </ul>
                    {slide.notes.length > 0 && (
                      <div className="ppt-slide-notes">
                        <Text type="secondary" style={{ fontSize: 11 }}>
                          🎤 {slide.notes[0]}
                        </Text>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </Card>
          )}

          {/* Empty state */}
          {!streamOutput && !loading && (
            <div className="ppt-empty">
              <FilePptOutlined style={{ fontSize: 48, color: '#d1d5db' }} />
              <p style={{ color: '#9ca3af', marginTop: 12 }}>
                输入主题后点击「开始生成」，AI 将为您创建专业演示文稿
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

export default PPTBuilder
