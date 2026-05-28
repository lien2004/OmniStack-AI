import { useState, useRef } from 'react'
import {
  Button, Card, Input, Select, Tag, Typography, Space,
  Spin, message as antdMessage, Steps, Tooltip,
} from 'antd'
import {
  FilePptOutlined,
  EyeOutlined, FilePdfOutlined, Html5Outlined,
  ClearOutlined, BulbOutlined,
  SendOutlined,
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
  { value: 'professional', label: '商务专业', desc: '数据驱动，适合企业汇报与客户提案' },
  { value: 'education', label: '教育学术', desc: '严谨权威，适合课件与学术汇报' },
  { value: 'creative', label: '创意设计', desc: '视觉冲击，适合品牌发布与创意提案' },
  { value: 'minimal', label: '极简风格', desc: '大字少量，适合 TED 演讲类场景' },
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

    for (const line of lines) {
      const pageMatch = line.match(/^##\s*(?:第\s*(\d+)\s*页[：:]\s*)?(.+)/)
      if (pageMatch) {
        if (currentSlide) parsed.push(currentSlide)
        currentSlide = {
          pageNum: pageMatch[1] ? parseInt(pageMatch[1]) : parsed.length + 1,
          title: pageMatch[2].trim(),
          bullets: [],
          notes: [],
        }
        continue
      }
      if (line.startsWith('# ') && parsed.length === 0) {
        currentSlide = { pageNum: 1, title: line.replace(/^#\s*/, '').trim(), bullets: [], notes: [] }
        continue
      }
      if (!currentSlide) continue
      if (line.trim().startsWith('> ')) {
        currentSlide.notes.push(line.trim().replace(/^>\s*/, ''))
        continue
      }
      const trimmed = line.trim().replace(/^[-*]\s*/, '')
      if (trimmed && !trimmed.startsWith('```') && !trimmed.startsWith('#') && !trimmed.startsWith('|')) {
        currentSlide.bullets.push(trimmed)
      }
    }
    if (currentSlide) parsed.push(currentSlide)
    return parsed
  }

  const handleGenerate = async () => {
    if (!topic.trim()) { antdMessage.warning('请输入PPT主题或内容大纲'); return }
    setLoading(true); setStreamOutput(''); setSlides([]); setCurrentStep(1)

    try {
      const response = await fetch('/api/agent/PptAgent/stream', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ input: topic.trim(), config: { style } }),
      })
      if (!response.ok) throw new Error(`HTTP ${response.status}`)
      if (!response.body) throw new Error('响应体为空')

      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = '', fullContent = ''

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
            if (data.includes('```step')) {
              try {
                const stepJson = data.replace(/```step\s*/, '').replace(/```\s*$/, '')
                const step = JSON.parse(stepJson)
                setCurrentStep(step.step)
                setStreamOutput(prev => prev + `\n📋 **${step.title}**：${step.description}\n\n---\n\n`)
              } catch { fullContent += data; setStreamOutput(prev => prev + data) }
            } else { fullContent += data; setStreamOutput(prev => prev + data) }
          }
        }
      }
      const parsed = parseSlides(fullContent)
      setSlides(parsed.length > 0 ? parsed : fallbackParseSlides(fullContent))
      setCurrentStep(4)
    } catch (e: any) { antdMessage.error('生成失败：' + (e.message || '未知错误')) }
    finally { setLoading(false) }
  }

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

  const handleReset = () => { setStreamOutput(''); setSlides([]); setTopic(''); setCurrentStep(0) }

  // ---- Export ----

  const exportPPTX = async () => {
    setExporting('pptx')
    try {
      const pres = new pptxgen(); pres.layout = 'LAYOUT_WIDE'; pres.author = '灵龙AI'; pres.title = topic || 'AI演示文稿'
      for (const slide of slides) {
        const s = pres.addSlide()
        s.addText(slide.title, { x: 0.8, y: 0.4, w: '85%', h: 0.6, fontSize: 22, bold: true, color: '1F2937' })
        if (slide.pageNum > 1) s.addShape(pres.ShapeType.rect, { x: 0, y: 0, w: '100%', h: 0.08, fill: { color: '1677ff' } })
        const bullets = slide.bullets.map(b => ({ text: b, options: { fontSize: 14, color: '4B5563', bullet: true } }))
        s.addText(bullets, { x: 0.8, y: 1.2, w: '85%', h: 3.5, fontSize: 14, color: '4B5563', lineSpacing: 28 })
        if (slide.notes.length > 0) s.addText('备注: ' + slide.notes[0], { x: 0.8, y: 5.0, w: '85%', h: 0.4, fontSize: 9, color: '9CA3AF', italic: true })
      }
      await pres.writeFile({ fileName: `${topic || 'presentation'}.pptx` })
      antdMessage.success('PPTX 已下载')
    } catch (e: any) { antdMessage.error('导出失败：' + e.message) }
    finally { setExporting(null) }
  }

  const exportPDF = async () => {
    if (!previewRef.current) return; setExporting('pdf')
    try {
      const canvas = await html2canvas(previewRef.current, { scale: 2, backgroundColor: '#ffffff', useCORS: true })
      const imgData = canvas.toDataURL('image/png')
      const pdf = new jsPDF('landscape', 'mm', 'a4')
      const pw = pdf.internal.pageSize.getWidth(), ph = pdf.internal.pageSize.getHeight()
      const iw = pw, ih = (canvas.height * pw) / canvas.width
      let hl = ih, pos = 0
      pdf.addImage(imgData, 'PNG', 0, pos, iw, ih); hl -= ph
      while (hl > 0) { pos = -(ih - hl); pdf.addPage(); pdf.addImage(imgData, 'PNG', 0, pos, iw, ih); hl -= ph }
      pdf.save(`${topic || 'presentation'}.pdf`); antdMessage.success('PDF 已下载')
    } catch (e: any) { antdMessage.error('导出失败：' + e.message) }
    finally { setExporting(null) }
  }

  const exportHTML = () => {
    const html = buildHTML(slides, topic)
    const blob = new Blob([html], { type: 'text/html;charset=utf-8' })
    const url = URL.createObjectURL(blob); window.open(url, '_blank'); URL.revokeObjectURL(url)
    antdMessage.success('HTML 已在新标签页打开')
  }

  const buildHTML = (s: Slide[], title: string) => `<!DOCTYPE html>
<html><head><meta charset="UTF-8"><title>${title}</title>
<style>
  *{margin:0;padding:0;box-sizing:border-box}
  body{font-family:Arial,Helvetica,'Microsoft YaHei',sans-serif;background:#f0f2f5;margin:0;padding:20px}
  .slide{width:720pt;height:405pt;margin:20px auto;background:#fff;border-radius:4pt;padding:36pt 48pt;position:relative;overflow:hidden;page-break-after:always;box-shadow:0 2pt 8pt rgba(0,0,0,.06)}
  .slide-cover{display:flex;flex-direction:column;justify-content:center;align-items:center;text-align:center}
  .slide-cover h1{font-size:28pt;color:#1f2937;margin-bottom:12pt}
  .slide-cover .subtitle{font-size:14pt;color:#6b7280}
  .slide-top-bar{position:absolute;top:0;left:0;width:100%;height:3pt;background:#1677ff}
  h2{font-size:20pt;color:#1f2937;border-bottom:2pt solid #e5e7eb;padding-bottom:8pt;margin-bottom:16pt}
  ul{list-style:disc;padding-left:20pt}
  li{font-size:13pt;color:#4b5563;line-height:1.8;margin-bottom:4pt}
  .notes{position:absolute;bottom:16pt;left:48pt;right:48pt;font-size:9pt;color:#9ca3af;border-left:2pt solid #d1d5db;padding-left:10pt}
  .page-num{position:absolute;bottom:12pt;right:48pt;font-size:8pt;color:#d1d5db}
  .slide-end{display:flex;flex-direction:column;justify-content:center;align-items:center;text-align:center}
  .slide-end h1{font-size:24pt;color:#1f2937;margin-bottom:8pt}
</style></head><body>${s.map((sl, i) => {
  const isFirst = i === 0, isLast = i === s.length - 1
  const cls = isFirst ? 'slide slide-cover' : isLast ? 'slide slide-end' : 'slide'
  return `<div class="${cls}">${!isFirst && !isLast ? '<div class="slide-top-bar"></div>' : ''}${isFirst ? `<h1>${sl.title}</h1>${sl.bullets[0] ? `<p class="subtitle">${sl.bullets[0]}</p>` : ''}` : isLast ? `<h1>${sl.title}</h1>${sl.bullets[0] ? `<p>${sl.bullets[0]}</p>` : ''}` : `<h2>${sl.title}</h2>`}${isFirst || isLast ? '' : `<ul>${sl.bullets.map(b => `<li>${b}</li>`).join('')}</ul>`}${sl.notes.length && !isFirst && !isLast ? `<div class="notes"><p>${sl.notes[0]}</p></div>` : ''}<div class="page-num"><p>${sl.pageNum} / ${s.length}</p></div></div>`
}).join('')}</body></html>`

  const steps = [
    { title: '分析主题', description: '解析核心要点与目标受众' },
    { title: '构建大纲', description: '规划演示结构与逻辑流' },
    { title: '生成内容', description: '逐页撰写标题与要点文案' },
    { title: '完成', description: '预览、导出与分享' },
  ]

  return (
    <div className="ppt-builder">
      {/* ---- 顶部标题栏 ---- */}
      <div className="ppt-topbar">
        <div className="ppt-topbar-left">
          <div className="ppt-topbar-icon">
            <FilePptOutlined />
          </div>
          <div>
            <Title level={3} style={{ margin: 0, fontSize: 20 }}>灵龙PPT</Title>
            <Text type="secondary" style={{ fontSize: 12 }}>
              AI 智能生成演示文稿 · 支持导出 PPTX / PDF / HTML
            </Text>
          </div>
        </div>
        <Space size={8}>
          <Tag color="blue" style={{ borderRadius: 6 }}>GPT-5.5</Tag>
          <Tag style={{ borderRadius: 6, background: '#f0fdf4', color: '#16a34a', border: '1px solid #bbf7d0' }}>4 步生成</Tag>
        </Space>
      </div>

      {/* ---- 主体 ---- */}
      <div className="ppt-workspace">
        {/* ---- 左侧输入区 ---- */}
        <div className="ppt-input-panel">
          <Card className="ppt-input-card" bordered={false}>
            <div className="ppt-input-label">
              <BulbOutlined style={{ color: '#1677ff' }} />
              <Text strong>输入主题</Text>
            </div>
            <TextArea
              value={topic}
              onChange={e => setTopic(e.target.value)}
              placeholder="输入您想要生成的PPT主题，例如：『2025年AI行业发展趋势报告』或粘贴已有的内容大纲..."
              autoSize={{ minRows: 5, maxRows: 9 }}
              disabled={loading}
              className="ppt-textarea"
            />
            <div className="ppt-input-options">
              <div className="ppt-option-item">
                <Text type="secondary" style={{ fontSize: 12, display: 'block', marginBottom: 4 }}>设计风格</Text>
                <Select
                  value={style}
                  onChange={setStyle}
                  options={STYLE_OPTIONS}
                  style={{ width: '100%' }}
                  disabled={loading}
                  size="middle"
                />
              </div>
            </div>
            <div className="ppt-input-actions">
              <Button
                type="primary"
                icon={loading ? undefined : <SendOutlined />}
                onClick={handleGenerate}
                loading={loading}
                disabled={!topic.trim()}
                size="large"
                block
              >
                {loading ? 'AI 正在生成...' : '开始生成'}
              </Button>
              {streamOutput && !loading && (
                <Button icon={<ClearOutlined />} onClick={handleReset} size="middle" block style={{ marginTop: 8 }}>
                  清空重来
                </Button>
              )}
            </div>
          </Card>
        </div>

        {/* ---- 右侧内容区 ---- */}
        <div className="ppt-content-panel">
          {/* 步骤指示 */}
          {(currentStep > 0) && (
            <Card className="ppt-steps-card" bordered={false}>
              <Steps
                current={currentStep - 1}
                size="small"
                items={steps.map(s => ({ title: s.title, description: s.description }))}
              />
            </Card>
          )}

          {/* 流式输出 */}
          {streamOutput && (
            <Card
              className="ppt-stream-card"
              bordered={false}
              title={<><EyeOutlined style={{ color: '#1677ff' }} /> 生成内容</>}
              extra={slides.length > 0 && <Tag color="blue">{slides.length} 页</Tag>}
            >
              <Spin spinning={loading} tip="AI 正在撰写中...">
                <div className="ppt-stream-content">
                  <ReactMarkdown remarkPlugins={[remarkGfm]}>{streamOutput}</ReactMarkdown>
                </div>
              </Spin>
            </Card>
          )}

          {/* 幻灯片预览 */}
          {slides.length > 0 && (
            <Card
              className="ppt-preview-card"
              bordered={false}
              title={<><FilePptOutlined style={{ color: '#1677ff' }} /> 幻灯片预览</>}
              extra={
                <Space size={4}>
                  <Tooltip title="下载 PPTX"><Button icon={<FilePptOutlined />} type="primary" ghost size="small" onClick={exportPPTX} loading={exporting === 'pptx'}>PPTX</Button></Tooltip>
                  <Tooltip title="下载 PDF"><Button icon={<FilePdfOutlined />} size="small" onClick={exportPDF} loading={exporting === 'pdf'}>PDF</Button></Tooltip>
                  <Tooltip title="打开 HTML"><Button icon={<Html5Outlined />} size="small" onClick={exportHTML}>HTML</Button></Tooltip>
                </Space>
              }
            >
              <div ref={previewRef} className="ppt-slide-preview">
                {slides.map((slide, idx) => (
                  <div key={idx} className="ppt-slide">
                    <div className="ppt-slide-header">
                      <span className="ppt-slide-num">{slide.pageNum}</span>
                      <h2>{slide.title}</h2>
                    </div>
                    <ul className="ppt-slide-bullets">
                      {slide.bullets.map((b, bi) => <li key={bi}>{b}</li>)}
                    </ul>
                    {slide.notes.length > 0 && (
                      <div className="ppt-slide-notes">
                        <Text type="secondary" style={{ fontSize: 11 }}>{slide.notes[0]}</Text>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </Card>
          )}

          {/* 空状态 */}
          {!streamOutput && !loading && (
            <div className="ppt-empty">
              <div className="ppt-empty-icon">
                <FilePptOutlined />
              </div>
              <Text type="secondary" style={{ fontSize: 15, marginTop: 16, display: 'block' }}>
                输入主题后点击「开始生成」
              </Text>
              <Text type="secondary" style={{ fontSize: 13 }}>
                AI 将自动分析主题、构建大纲、逐页生成专业演示文稿
              </Text>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

export default PPTBuilder
