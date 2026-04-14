import { Routes, Route, Navigate } from 'react-router-dom'
import Layout from './components/Layout'
import Dashboard from './pages/Dashboard'
import ProjectList from './pages/ProjectList'
import ProjectDetail from './pages/ProjectDetail'
import CreateProject from './pages/CreateProject'
import AgentCenter from './pages/AgentCenter'
import MCPHub from './pages/MCPHub'
import Settings from './pages/Settings'
import Chat from './pages/Chat'
import KnowledgeBase from './pages/KnowledgeBase'

function App() {
  return (
    <Routes>
      <Route path="/" element={<Layout />}>
        <Route index element={<Dashboard />} />
        <Route path="chat" element={<Chat />} />
        <Route path="knowledge" element={<KnowledgeBase />} />
        <Route path="projects" element={<ProjectList />} />
        <Route path="projects/create" element={<CreateProject />} />
        <Route path="projects/:id" element={<ProjectDetail />} />
        <Route path="agents" element={<AgentCenter />} />
        <Route path="mcp" element={<MCPHub />} />
        <Route path="settings" element={<Settings />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default App
