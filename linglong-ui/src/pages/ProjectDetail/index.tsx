import { useParams } from 'react-router-dom'
import {
  Card,
  Steps,
  Descriptions,
  Tag,
  Button,
  Tabs,
  List,
  Empty,
  Progress,
} from 'antd'
import {
  PlayCircleOutlined,
  PauseCircleOutlined,
  ReloadOutlined,
  FileTextOutlined,
  CodeOutlined,
  BranchesOutlined,
  BugOutlined,
} from '@ant-design/icons'
import { useState } from 'react'

const { Step } = Steps
const { TabPane } = Tabs

const statusMap: Record<string, { color: string; text: string }> = {
  init: { color: 'default', text: '初始化' },
  analyzing: { color: 'blue', text: '需求分析' },
  designing: { color: 'orange', text: '架构设计' },
  coding: { color: 'processing', text: '代码生成' },
  testing: { color: 'purple', text: '测试验证' },
  completed: { color: 'success', text: '已完成' },
}

const agentSteps = [
  { title: '需求分析', agent: 'RequirementAnalysisAgent' },
  { title: '领域建模', agent: 'DomainModelingAgent' },
  { title: '架构设计', agent: 'ArchitectureDesignAgent' },
  { title: '代码生成', agent: 'CodeGenerationAgent' },
  { title: '代码审查', agent: 'CodeReviewAgent' },
  { title: '测试生成', agent: 'TestGenerationAgent' },
]

function ProjectDetail() {
  const { id } = useParams<{ id: string }>()
  const [activeTab, setActiveTab] = useState('overview')
  const [currentStep] = useState(3)

  // TODO: 根据id获取项目详情
  const project = {
    id,
    name: '电商订单系统',
    description: '基于DDD的电商订单管理服务',
    status: 'coding',
    techStack: 'Spring Boot + PostgreSQL',
    architectureType: 'DDD领域驱动设计',
    createTime: '2024-01-15',
    updateTime: '2024-01-20',
  }

  return (
    <div className="project-detail">
      <Card
        title={project.name}
        extra={
          <div>
            <Button icon={<PlayCircleOutlined />} type="primary" style={{ marginRight: 8 }}>
              继续生成
            </Button>
            <Button icon={<PauseCircleOutlined />} style={{ marginRight: 8 }}>
              暂停
            </Button>
            <Button icon={<ReloadOutlined />}>重新开始</Button>
          </div>
        }
      >
        <Descriptions bordered column={2}>
          <Descriptions.Item label="项目ID">{project.id}</Descriptions.Item>
          <Descriptions.Item label="项目状态">
            <Tag color={statusMap[project.status].color}>
              {statusMap[project.status].text}
            </Tag>
          </Descriptions.Item>
          <Descriptions.Item label="技术栈">{project.techStack}</Descriptions.Item>
          <Descriptions.Item label="架构类型">{project.architectureType}</Descriptions.Item>
          <Descriptions.Item label="创建时间">{project.createTime}</Descriptions.Item>
          <Descriptions.Item label="更新时间">{project.updateTime}</Descriptions.Item>
          <Descriptions.Item label="描述" span={2}>
            {project.description}
          </Descriptions.Item>
        </Descriptions>
      </Card>

      <Card style={{ marginTop: 24 }}>
        <Tabs activeKey={activeTab} onChange={setActiveTab}>
          <TabPane
            tab={<span><FileTextOutlined />执行进度</span>}
            key="overview"
          >
            <Steps
              direction="vertical"
              current={currentStep}
              style={{ marginTop: 16 }}
            >
              {agentSteps.map((step, index) => (
                <Step
                  key={step.title}
                  title={step.title}
                  description={
                    <div>
                      <div>Agent: {step.agent}</div>
                      {index === currentStep && (
                        <Progress percent={65} status="active" size="small" style={{ marginTop: 8 }} />
                      )}
                    </div>
                  }
                />
              ))}
            </Steps>
          </TabPane>
          <TabPane
            tab={<span><CodeOutlined />生成代码</span>}
            key="code"
          >
            <List
              bordered
              dataSource={[
                { name: 'Order.java', type: 'Entity', size: '2.3 KB' },
                { name: 'OrderService.java', type: 'Service', size: '4.1 KB' },
                { name: 'OrderRepository.java', type: 'Repository', size: '1.2 KB' },
                { name: 'OrderController.java', type: 'Controller', size: '3.5 KB' },
              ]}
              renderItem={(item) => (
                <List.Item
                  actions={[
                    <Button type="link">查看</Button>,
                    <Button type="link">下载</Button>,
                  ]}
                >
                  <List.Item.Meta
                    title={item.name}
                    description={`类型: ${item.type} | 大小: ${item.size}`}
                  />
                </List.Item>
              )}
            />
          </TabPane>
          <TabPane
            tab={<span><BranchesOutlined />架构图</span>}
            key="diagram"
          >
            <Empty description="架构图生成中..." />
          </TabPane>
          <TabPane
            tab={<span><BugOutlined />测试报告</span>}
            key="test"
          >
            <Empty description="测试尚未开始" />
          </TabPane>
        </Tabs>
      </Card>
    </div>
  )
}

export default ProjectDetail
