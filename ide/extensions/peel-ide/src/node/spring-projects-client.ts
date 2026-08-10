import { injectable } from '@theia/core/shared/inversify'

import type {
  CreateProjectRequest,
  PeelProjectDetail,
  PeelProjectDetailResponse,
  PeelProjectListResponse,
  PeelProjectSummary,
  PeelScriptDetail,
  PeelScriptSummary,
  PeelScriptSummaryResponse,
  RunProjectScriptRequest,
  RunProjectScriptResponse,
  SaveProjectScriptRequest,
} from '../common/peel-types'

const DEFAULT_API_BASE_URL = 'http://localhost:8080'

@injectable()
export class SpringProjectsClient {
  async listProjects(): Promise<PeelProjectSummary[]> {
    const response = await fetch(`${this.apiBaseUrl()}/api/projects`)
    if (!response.ok) {
      throw new Error(`Failed to load projects: ${response.status} ${response.statusText}`)
    }

    const payload = (await response.json()) as PeelProjectListResponse
    if (!Array.isArray(payload.projects)) {
      throw new Error('Invalid projects payload received from backend.')
    }

    return payload.projects
  }

  async createProject(request: CreateProjectRequest): Promise<PeelProjectDetail> {
    const response = await fetch(`${this.apiBaseUrl()}/api/projects`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    })

    if (!response.ok) {
      throw new Error(`Failed to create project: ${response.status} ${response.statusText}`)
    }

    const payload = (await response.json()) as PeelProjectDetailResponse
    return {
      id: payload.id,
      name: payload.name,
      description: payload.description,
    }
  }

  async getProject(projectId: string): Promise<PeelProjectDetail> {
    const response = await fetch(`${this.apiBaseUrl()}/api/projects/${encodeURIComponent(projectId)}`)
    if (!response.ok) {
      throw new Error(`Failed to load project: ${response.status} ${response.statusText}`)
    }
    const payload = (await response.json()) as PeelProjectDetailResponse
    return {
      id: payload.id,
      name: payload.name,
      description: payload.description,
    }
  }

  async listProjectScripts(projectId: string): Promise<PeelScriptSummary[]> {
    const response = await fetch(`${this.apiBaseUrl()}/api/projects/${encodeURIComponent(projectId)}/scripts`)
    if (!response.ok) {
      throw new Error(`Failed to load scripts: ${response.status} ${response.statusText}`)
    }

    const payload = (await response.json()) as PeelScriptSummaryResponse[]
    if (!Array.isArray(payload)) {
      throw new Error('Invalid script list payload received from backend.')
    }
    return payload
  }

  async saveProjectScript(projectId: string, request: SaveProjectScriptRequest): Promise<PeelScriptDetail> {
    const response = await fetch(`${this.apiBaseUrl()}/api/projects/${encodeURIComponent(projectId)}/scripts`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    })

    if (!response.ok) {
      throw new Error(`Failed to save script: ${response.status} ${response.statusText}`)
    }

    return (await response.json()) as PeelScriptDetail
  }

  async getProjectScript(projectId: string, scriptId: string): Promise<PeelScriptDetail> {
    const response = await fetch(
      `${this.apiBaseUrl()}/api/projects/${encodeURIComponent(projectId)}/scripts/${encodeURIComponent(scriptId)}`,
    )
    if (!response.ok) {
      throw new Error(`Failed to load script: ${response.status} ${response.statusText}`)
    }
    return (await response.json()) as PeelScriptDetail
  }

  async runProjectScript(projectId: string, request: RunProjectScriptRequest): Promise<RunProjectScriptResponse> {
    const response = await fetch(`${this.apiBaseUrl()}/api/projects/${encodeURIComponent(projectId)}/run`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    })
    if (!response.ok) {
      throw new Error(`Failed to run script: ${response.status} ${response.statusText}`)
    }
    return (await response.json()) as RunProjectScriptResponse
  }

  private apiBaseUrl(): string {
    const configured = process.env.PEEL_API_BASE_URL?.trim()
    if (!configured) {
      return DEFAULT_API_BASE_URL
    }
    return configured.endsWith('/') ? configured.slice(0, -1) : configured
  }
}
