export const PeelServicePath = '/services/peel-service'

export const PeelService = Symbol('PeelService')

import type {
  CreateProjectRequest,
  PeelProjectDetail,
  PeelProjectSummary,
  PeelScriptDetail,
  PeelScriptSummary,
  RunProjectScriptRequest,
  RunProjectScriptResponse,
  SaveProjectScriptRequest,
} from './peel-types'

export interface PeelService {
  listProjects(): Promise<PeelProjectSummary[]>
  createProject(request: CreateProjectRequest): Promise<PeelProjectDetail>
  getProject(projectId: string): Promise<PeelProjectDetail>

  listProjectScripts(projectId: string): Promise<PeelScriptSummary[]>
  saveProjectScript(projectId: string, request: SaveProjectScriptRequest): Promise<PeelScriptDetail>
  getProjectScript(projectId: string, scriptId: string): Promise<PeelScriptDetail>
  runProjectScript(projectId: string, request: RunProjectScriptRequest): Promise<RunProjectScriptResponse>
}
