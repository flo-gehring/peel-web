export interface PeelProjectSummary {
  id: string
  name: string
  description: string | null
}

export interface PeelProjectListResponse {
  projects: PeelProjectSummary[]
}

export type PeelNodeType = 'project' | 'scripts' | 'documents' | 'render-configs'

export interface PeelTreeNode {
  id: string
  name: string
  type: PeelNodeType
  parentId?: string
}
