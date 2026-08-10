import { injectable, inject } from '@theia/core/shared/inversify'

import { PeelService } from '../common/peel-protocol'
import type {
  CreateProjectRequest,
  PeelProjectDetail,
  PeelProjectSummary,
  PeelScriptDetail,
  PeelScriptSummary,
  RunProjectScriptRequest,
  RunProjectScriptResponse,
  SaveProjectScriptRequest,
} from '../common/peel-types'
import { SpringProjectsClient } from './spring-projects-client'

@injectable()
export class PeelBackendService implements PeelService {
  constructor(
    @inject(SpringProjectsClient)
    private readonly springProjectsClient: SpringProjectsClient,
  ) {}

  async listProjects(): Promise<PeelProjectSummary[]> {
    return this.springProjectsClient.listProjects()
  }

  async createProject(request: CreateProjectRequest): Promise<PeelProjectDetail> {
    return this.springProjectsClient.createProject(request)
  }

  async getProject(projectId: string): Promise<PeelProjectDetail> {
    return this.springProjectsClient.getProject(projectId)
  }

  async listProjectScripts(projectId: string): Promise<PeelScriptSummary[]> {
    return this.springProjectsClient.listProjectScripts(projectId)
  }

  async saveProjectScript(projectId: string, request: SaveProjectScriptRequest): Promise<PeelScriptDetail> {
    return this.springProjectsClient.saveProjectScript(projectId, request)
  }

  async getProjectScript(projectId: string, scriptId: string): Promise<PeelScriptDetail> {
    return this.springProjectsClient.getProjectScript(projectId, scriptId)
  }

  async runProjectScript(projectId: string, request: RunProjectScriptRequest): Promise<RunProjectScriptResponse> {
    return this.springProjectsClient.runProjectScript(projectId, request)
  }
}
