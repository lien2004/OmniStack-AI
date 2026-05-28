import axios, { type AxiosProgressEvent } from 'axios'

const KB_BASE = '/ai/kb'

export interface KnowledgeBaseItem {
  id: string
  name: string
  description?: string
  collectionName: string
  embeddingModel: string
  dimensions: number
  owner?: string
  documentCount?: number
  chunkCount?: number
  sizeBytes?: number
  createdAt?: string
  updatedAt?: string
}

export interface KnowledgeDocumentItem {
  id: string
  kbId: string
  name: string
  filename?: string
  category?: string
  mimeType?: string
  sizeBytes?: number
  chunkCount?: number
  status: 'ready' | 'parsing' | 'failed'
  enabled: boolean
  errorMessage?: string
  uploader?: string
  createdAt?: string
  updatedAt?: string
}

export interface KnowledgeChunkItem {
  id: string
  content: string
  contentLength: number
  metadata: Record<string, any>
  chunkIndex: number
  totalChunks: number
  enabled: boolean
}

export interface OverviewStats {
  knowledgeBaseCount: number
  documentCount: number
  chunkCount: number
  sizeBytes: number
}

export interface PagedResult<T> {
  total: number
  page: number
  size: number
  items: T[]
}

// ============ 知识库 ============

export const fetchKbList = () =>
  axios.get<KnowledgeBaseItem[]>(KB_BASE).then(r => r.data)

export const fetchOverview = () =>
  axios.get<OverviewStats>(`${KB_BASE}/stats`).then(r => r.data)

export const fetchKbDetail = (id: string) =>
  axios.get<KnowledgeBaseItem>(`${KB_BASE}/${id}`).then(r => r.data)

export const createKb = (data: Partial<KnowledgeBaseItem>) =>
  axios.post<KnowledgeBaseItem>(KB_BASE, data).then(r => r.data)

export const updateKb = (id: string, data: Partial<KnowledgeBaseItem>) =>
  axios.put<KnowledgeBaseItem>(`${KB_BASE}/${id}`, data).then(r => r.data)

export const deleteKb = (id: string) =>
  axios.delete(`${KB_BASE}/${id}`).then(r => r.data)

// ============ 文档 ============

export const fetchDocuments = (
  kbId: string,
  params: { page?: number; size?: number; keyword?: string; status?: string } = {}
) =>
  axios
    .get<PagedResult<KnowledgeDocumentItem>>(`${KB_BASE}/${kbId}/documents`, { params })
    .then(r => r.data)

export const fetchDocumentDetail = (kbId: string, docId: string) =>
  axios.get<KnowledgeDocumentItem>(`${KB_BASE}/${kbId}/documents/${docId}`).then(r => r.data)

export const uploadDocument = (
  kbId: string,
  file: File,
  name?: string,
  category?: string,
  onUploadProgress?: (e: AxiosProgressEvent) => void
) => {
  const fd = new FormData()
  fd.append('file', file)
  if (name) fd.append('name', name)
  if (category) fd.append('category', category)
  return axios.post(`${KB_BASE}/${kbId}/documents/upload`, fd, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress,
    timeout: 300000,
  }).then(r => r.data)
}

export const setDocumentEnabled = (kbId: string, docId: string, enabled: boolean) =>
  axios
    .post<KnowledgeDocumentItem>(`${KB_BASE}/${kbId}/documents/${docId}/enabled`, { enabled })
    .then(r => r.data)

export const rebuildDocument = (kbId: string, docId: string) =>
  axios
    .post<KnowledgeDocumentItem>(`${KB_BASE}/${kbId}/documents/${docId}/rebuild`)
    .then(r => r.data)

export const deleteDocument = (kbId: string, docId: string) =>
  axios.delete(`${KB_BASE}/${kbId}/documents/${docId}`).then(r => r.data)

// ============ 分块 ============

export const fetchChunks = (
  kbId: string,
  docId: string,
  params: { page?: number; size?: number; keyword?: string } = {}
) =>
  axios
    .get<PagedResult<KnowledgeChunkItem>>(
      `${KB_BASE}/${kbId}/documents/${docId}/chunks`,
      { params }
    )
    .then(r => r.data)

export const fetchChunkDetail = (chunkId: string) =>
  axios.get<KnowledgeChunkItem>(`${KB_BASE}/chunks/${chunkId}`).then(r => r.data)

export const updateChunkContent = (chunkId: string, content: string) =>
  axios.put(`${KB_BASE}/chunks/${chunkId}`, { content }).then(r => r.data)

export const batchSetChunkEnabled = (ids: string[], enabled: boolean) =>
  axios.post(`${KB_BASE}/chunks/enabled`, { ids, enabled }).then(r => r.data)

export const batchDeleteChunks = (ids: string[]) =>
  axios.post(`${KB_BASE}/chunks/delete`, { ids }).then(r => r.data)

export const batchRebuildChunks = (ids: string[]) =>
  axios.post(`${KB_BASE}/chunks/rebuild`, { ids }).then(r => r.data)
