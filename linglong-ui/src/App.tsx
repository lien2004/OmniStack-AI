import { Routes, Route, Navigate } from 'react-router-dom'
import Layout from './components/Layout'
import Dashboard from './pages/Dashboard'
import ProjectList from './pages/ProjectList'
import ProjectDetail from './pages/ProjectDetail'
import CreateProject from './pages/CreateProject'
import AgentCenter from './pages/AgentCenter'
import CodeFlow from './pages/CodeFlow'
import ResumeBuilder from './pages/ResumeBuilder'
import ImageGeneration from './pages/ImageGeneration'
import MCPHub from './pages/MCPHub'
import Settings from './pages/Settings'
import Chat from './pages/Chat'
import KnowledgeBase from './pages/KnowledgeBase'
import KnowledgeDocuments from './pages/KnowledgeBase/Documents'
import KnowledgeChunks from './pages/KnowledgeBase/Chunks'
import Login from './pages/Login'
import Register from './pages/Login/Register'
import Profile from './pages/Profile'
import AdminUsers from './pages/AdminUsers'

function App() {
  return (
    <Routes>
      <Route path="/" element={<Layout />}>
        <Route index element={<Dashboard />} />
        <Route path="chat" element={<Chat />} />
        <Route path="knowledge" element={<KnowledgeBase />} />
        <Route path="knowledge/:kbId/documents" element={<KnowledgeDocuments />} />
        <Route path="knowledge/:kbId/documents/:docId/chunks" element={<KnowledgeChunks />} />
        <Route path="projects" element={<ProjectList />} />
        <Route path="projects/create" element={<CreateProject />} />
        <Route path="projects/:id" element={<ProjectDetail />} />
        <Route path="agents" element={<AgentCenter />} />
        <Route path="codeflow" element={<CodeFlow />} />
        <Route path="resume" element={<ResumeBuilder />} />
        <Route path="image-generation" element={<ImageGeneration />} />
        <Route path="mcp" element={<MCPHub />} />
        <Route path="settings" element={<Settings />} />
        <Route path="profile" element={<Profile />} />
        <Route path="admin/users" element={<AdminUsers />} />
      </Route>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default App
