export interface PeelProjectSummary {
  id: string
  name: string
  description: string | null
}

export interface PeelProjectDetail {
  id: string
  name: string
  description: string | null
}

export interface CreateProjectRequest {
  name: string
  id?: string
  description?: string
  template?: 'empty' | 'demo'
}

export interface PeelProjectListResponse {
  projects: PeelProjectSummary[]
}

export interface PeelProjectDetailResponse {
  id: string
  name: string
  description: string | null
}

export interface PeelScriptSummary {
  id: string
  name: string
}

export interface PeelScriptSummaryResponse {
  id: string
  name: string
}

export interface PeelScriptDetail {
  id: string
  name: string
  script: string
}

export interface SaveProjectScriptRequest {
  id?: string
  name: string
  script: string
}

export interface RunProjectScriptRequest {
  scriptId?: string
  script?: string
  bindings?: Record<string, unknown>
}

export interface RunProjectScriptResponse {
  trace: Record<string, unknown>
  result: Record<string, unknown>
}

export type PeelNodeType = 'project' | 'scripts' | 'documents' | 'render-configs'

export interface PeelTreeNode {
  id: string
  name: string
  type: PeelNodeType
  parentId?: string
}
