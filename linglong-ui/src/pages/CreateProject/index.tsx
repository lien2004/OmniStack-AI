import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Card,
  Steps,
  Form,
  Input,
  Select,
  Button,
  Radio,
  Upload,
  message,
  Result,
} from 'antd'
import {
  UploadOutlined,
  LoadingOutlined,
  CheckCircleOutlined,
} from '@ant-design/icons'

const { Step } = Steps
const { TextArea } = Input
const { Option } = Select

function CreateProject() {
  const navigate = useNavigate()
  const [currentStep, setCurrentStep] = useState(0)
  const [form] = Form.useForm()
  const [creating, setCreating] = useState(false)
  const [created, setCreated] = useState(false)

  const steps = [
    { title: '基本信息', description: '填写项目信息' },
    { title: '需求输入', description: '上传或输入需求' },
    { title: '技术选型', description: '选择技术栈' },
    { title: '确认创建', description: '确认并创建' },
  ]

  const handleNext = async () => {
    try {
      await form.validateFields()
      if (currentStep < steps.length - 1) {
        setCurrentStep(currentStep + 1)
      } else {
        handleCreate()
      }
    } catch (error) {
      console.error('表单验证失败:', error)
    }
  }

  const handlePrev = () => {
    setCurrentStep(currentStep - 1)
  }

  const handleCreate = async () => {
    setCreating(true)
    // TODO: 调用创建项目API
    setTimeout(() => {
      setCreating(false)
      setCreated(true)
      message.success('项目创建成功！')
    }, 2000)
  }

  const renderStepContent = () => {
    switch (currentStep) {
      case 0:
        return (
          <>
            <Form.Item
              name="name"
              label="项目名称"
              rules={[{ required: true, message: '请输入项目名称' }]}
            >
              <Input placeholder="请输入项目名称" />
            </Form.Item>
            <Form.Item name="description" label="项目描述">
              <TextArea rows={4} placeholder="请输入项目描述" />
            </Form.Item>
          </>
        )
      case 1:
        return (
          <>
            <Form.Item label="需求输入方式">
              <Radio.Group defaultValue="text">
                <Radio value="text">文本输入</Radio>
                <Radio value="file">文件上传</Radio>
              </Radio.Group>
            </Form.Item>
            <Form.Item
              name="requirementDoc"
              label="需求文档"
              rules={[{ required: true, message: '请输入需求文档' }]}
            >
              <TextArea
                rows={10}
                placeholder="请详细描述您的需求，包括功能需求、业务流程、数据模型等..."
              />
            </Form.Item>
            <Form.Item label="或上传文档">
              <Upload>
                <Button icon={<UploadOutlined />}>上传文档</Button>
              </Upload>
            </Form.Item>
          </>
        )
      case 2:
        return (
          <>
            <Form.Item
              name="techStack"
              label="技术栈"
              rules={[{ required: true, message: '请选择技术栈' }]}
            >
              <Select placeholder="请选择技术栈">
                <Option value="springboot-postgresql">
                  Spring Boot + PostgreSQL
                </Option>
                <Option value="springboot-mysql">
                  Spring Boot + MySQL
                </Option>
                <Option value="springboot-mongodb">
                  Spring Boot + MongoDB
                </Option>
                <Option value="springcloud">
                  Spring Cloud 微服务
                </Option>
              </Select>
            </Form.Item>
            <Form.Item
              name="architectureType"
              label="架构类型"
              rules={[{ required: true, message: '请选择架构类型' }]}
            >
              <Select placeholder="请选择架构类型">
                <Option value="ddd">DDD领域驱动设计</Option>
                <Option value="mvc">MVC分层架构</Option>
                <Option value="microservice">微服务架构</Option>
              </Select>
            </Form.Item>
            <Form.Item name="javaVersion" label="Java版本">
              <Select defaultValue="21">
                <Option value="21">Java 21 (推荐)</Option>
                <Option value="17">Java 17</Option>
                <Option value="11">Java 11</Option>
              </Select>
            </Form.Item>
          </>
        )
      case 3:
        return (
          <div style={{ padding: '24px 0' }}>
            <h3>请确认以下信息：</h3>
            <div style={{ marginTop: 16, lineHeight: 2 }}>
              <p>
                <strong>项目名称：</strong>
                {form.getFieldValue('name')}
              </p>
              <p>
                <strong>项目描述：</strong>
                {form.getFieldValue('description') || '无'}
              </p>
              <p>
                <strong>技术栈：</strong>
                {form.getFieldValue('techStack')}
              </p>
              <p>
                <strong>架构类型：</strong>
                {form.getFieldValue('architectureType')}
              </p>
            </div>
          </div>
        )
      default:
        return null
    }
  }

  if (created) {
    return (
      <Card>
        <Result
          icon={<CheckCircleOutlined style={{ color: '#52c41a' }} />}
          title="项目创建成功！"
          subTitle="AI Agent 将开始分析您的需求并生成代码"
          extra={[
            <Button
              type="primary"
              key="view"
              onClick={() => navigate('/projects')}
            >
              查看项目
            </Button>,
            <Button key="back" onClick={() => navigate('/')}>
              返回首页
            </Button>,
          ]}
        />
      </Card>
    )
  }

  return (
    <Card title="创建新项目">
      <Steps current={currentStep} style={{ marginBottom: 40 }}>
        {steps.map((step) => (
          <Step key={step.title} title={step.title} description={step.description} />
        ))}
      </Steps>

      <Form
        form={form}
        layout="vertical"
        style={{ maxWidth: 600, margin: '0 auto' }}
      >
        {renderStepContent()}

        <div style={{ marginTop: 40, textAlign: 'center' }}>
          {currentStep > 0 && (
            <Button style={{ marginRight: 8 }} onClick={handlePrev}>
              上一步
            </Button>
          )}
          <Button
            type="primary"
            onClick={handleNext}
            loading={creating}
            icon={creating ? <LoadingOutlined /> : undefined}
          >
            {currentStep === steps.length - 1 ? '创建项目' : '下一步'}
          </Button>
        </div>
      </Form>
    </Card>
  )
}

export default CreateProject
