import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Card,
  Form,
  Input,
  Select,
  Button,
  message,
  Space,
  Divider,
  Typography,
  Switch,
} from 'antd'
import {
  ThunderboltOutlined,
  SaveOutlined,
  ArrowLeftOutlined,
  RocketOutlined,
} from '@ant-design/icons'
import { createProject } from '@/services/api'

const { TextArea } = Input
const { Option } = Select
const { Text } = Typography

function CreateProject() {
  const navigate = useNavigate()
  const [form] = Form.useForm()
  const [creating, setCreating] = useState(false)
  const [autoStartAI, setAutoStartAI] = useState(true)

  const handleCreate = async () => {
    try {
      const values = await form.validateFields()
      setCreating(true)

      const res = await createProject({
        name: values.name,
        description: values.description,
        techStack: values.techStack,
        architectureType: values.architectureType,
        model: values.model,
        requirement: values.requirement,
      })

      if (res.data?.success) {
        const project = res.data.data
        message.success('项目创建成功！')

        if (autoStartAI && values.requirement) {
          // 直接跳转到CodeFlow启动AI开发
          navigate('/codeflow', {
            state: {
              projectId: project.id,
              requirement: values.requirement,
              model: values.model,
              projectName: values.name,
            },
          })
        } else {
          // 跳转到项目详情
          navigate(`/projects/${project.id}`)
        }
      } else {
        message.error(res.data?.message || '创建项目失败')
      }
    } catch (error: any) {
      if (error?.errorFields) {
        // 表单验证失败
        return
      }
      message.error('创建项目失败')
    } finally {
      setCreating(false)
    }
  }

  return (
    <div style={{ maxWidth: 800, margin: '0 auto' }}>
      <Card
        title={
          <Space>
            <Button type="text" icon={<ArrowLeftOutlined />} onClick={() => navigate('/projects')} />
            <RocketOutlined style={{ color: '#1677ff', fontSize: 18 }} />
            <span style={{ fontSize: 18, fontWeight: 600 }}>新建项目</span>
          </Space>
        }
      >
        <Form
          form={form}
          layout="vertical"
          initialValues={{
            architectureType: 'ddd',
            model: 'gpt-5.5',
          }}
        >
          {/* 基本信息 */}
          <div style={{ marginBottom: 8 }}>
            <Text strong style={{ fontSize: 15 }}>基本信息</Text>
          </div>

          <Form.Item
            name="name"
            label="项目名称"
            rules={[{ required: true, message: '请输入项目名称' }]}
          >
            <Input placeholder="例如：电商订单系统、用户管理平台" size="large" />
          </Form.Item>

          <Form.Item name="description" label="项目描述">
            <TextArea rows={2} placeholder="简要描述项目的功能和用途" />
          </Form.Item>

          <Divider />

          {/* 技术选型 */}
          <div style={{ marginBottom: 8 }}>
            <Text strong style={{ fontSize: 15 }}>技术选型</Text>
          </div>

          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item name="techStack" label="技术栈" style={{ flex: 1 }}>
              <Select placeholder="请选择技术栈" allowClear>
                <Option value="Spring Boot + PostgreSQL">Spring Boot + PostgreSQL</Option>
                <Option value="Spring Boot + MySQL">Spring Boot + MySQL</Option>
                <Option value="Spring Boot + MongoDB">Spring Boot + MongoDB</Option>
                <Option value="Spring Cloud 微服务">Spring Cloud 微服务</Option>
                <Option value="React + Node.js">React + Node.js</Option>
                <Option value="Vue3 + Spring Boot">Vue3 + Spring Boot</Option>
                <Option value="Python + FastAPI">Python + FastAPI</Option>
              </Select>
            </Form.Item>

            <Form.Item name="architectureType" label="架构类型" style={{ flex: 1 }}>
              <Select placeholder="请选择架构类型">
                <Option value="ddd">DDD领域驱动设计</Option>
                <Option value="mvc">MVC分层架构</Option>
                <Option value="microservice">微服务架构</Option>
                <Option value="hexagonal">六边形架构</Option>
              </Select>
            </Form.Item>
          </div>

          <Form.Item name="model" label="AI模型">
            <Select placeholder="选择用于代码生成的AI模型">
              <Option value="gpt-5.5">GPT-5.5（推荐 · 最强推理）</Option>
              <Option value="gpt-5.4">GPT-5.4（旗舰 · 全能开发）</Option>
              <Option value="gpt-5.3-codex">GPT-5.3 Codex（代码专精）</Option>
              <Option value="claude-opus-4-7">Claude Opus 4.7（顶级推理）</Option>
              <Option value="deepseek-v4-pro">DeepSeek V4 Pro（深度思考）</Option>
              <Option value="qwen3-coder-plus">Qwen3 Coder Plus（编程增强）</Option>
            </Select>
          </Form.Item>

          <Divider />

          {/* 需求输入 */}
          <div style={{ marginBottom: 8 }}>
            <Text strong style={{ fontSize: 15 }}>需求描述</Text>
            <Text type="secondary" style={{ marginLeft: 8, fontSize: 12 }}>
              详细的需求描述将帮助AI更好地生成代码
            </Text>
          </div>

          <Form.Item
            name="requirement"
            rules={[{ required: autoStartAI, message: '启动AI开发时需要填写需求描述' }]}
          >
            <TextArea
              rows={8}
              placeholder={`请详细描述您的需求，例如：

开发一个图书管理系统，主要功能包括：
1. 图书的增删改查（书名、作者、ISBN、出版社、分类等）
2. 读者管理（注册、登录、个人信息维护）
3. 借阅管理（借书、还书、续借、逾期提醒）
4. 统计报表（热门图书、借阅排行、读者活跃度）
5. 权限控制（管理员、图书馆员、普通读者）

技术要求：
- 后端使用Spring Boot + MyBatis Plus
- 数据库使用PostgreSQL
- RESTful API设计
- 包含单元测试`}
              style={{ fontSize: 14 }}
            />
          </Form.Item>

          <Divider />

          {/* 操作区域 */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Space>
              <Switch
                checked={autoStartAI}
                onChange={setAutoStartAI}
              />
              <Text style={{ fontSize: 13 }}>
                创建后自动启动 <ThunderboltOutlined style={{ color: '#f97316' }} /> Agent智能开发
              </Text>
            </Space>

            <Space>
              <Button onClick={() => navigate('/projects')}>取消</Button>
              {!autoStartAI && (
                <Button
                  icon={<SaveOutlined />}
                  onClick={handleCreate}
                  loading={creating}
                >
                  仅保存项目
                </Button>
              )}
              <Button
                type="primary"
                icon={autoStartAI ? <ThunderboltOutlined /> : <SaveOutlined />}
                onClick={handleCreate}
                loading={creating}
                size="large"
                style={{ borderRadius: 8 }}
              >
                {autoStartAI ? '创建并启动AI开发' : '创建项目'}
              </Button>
            </Space>
          </div>
        </Form>
      </Card>
    </div>
  )
}

export default CreateProject
