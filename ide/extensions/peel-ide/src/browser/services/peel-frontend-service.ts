import { inject, injectable } from '@theia/core/shared/inversify'
import { WebSocketConnectionProvider } from '@theia/core/lib/browser/messaging/ws-connection-provider'

import { PeelService, PeelServicePath } from '../../common/peel-protocol'
import type {
  CreateProjectRequest,
  PeelProjectDetail,
  PeelProjectSummary,
  PeelScriptDetail,
  PeelScriptSummary,
  RunProjectScriptRequest,
  RunProjectScriptResponse,
  SaveProjectScriptRequest,
} from '../../common/peel-types'

@injectable()
export class PeelFrontendService {
  private proxy: PeelService | undefined
  private activeProjectId: string | undefined

  constructor(
    @inject(WebSocketConnectionProvider)
    private readonly wsConnectionProvider: WebSocketConnectionProvider,
  ) {}

  async listProjects(): Promise<PeelProjectSummary[]> {
    return this.getProxy().listProjects()
  }

  async createProject(request: CreateProjectRequest): Promise<PeelProjectDetail> {
    return this.getProxy().createProject(request)
  }

  async getProject(projectId: string): Promise<PeelProjectDetail> {
    return this.getProxy().getProject(projectId)
  }

  getActiveProjectId(): string | undefined {
    return this.activeProjectId
  }

  setActiveProjectId(projectId: string | undefined): void {
    this.activeProjectId = projectId
  }

  async listProjectScripts(projectId: string): Promise<PeelScriptSummary[]> {
    return this.getProxy().listProjectScripts(projectId)
  }

  async saveProjectScript(projectId: string, request: SaveProjectScriptRequest): Promise<PeelScriptDetail> {
    return this.getProxy().saveProjectScript(projectId, request)
  }

  async getProjectScript(projectId: string, scriptId: string): Promise<PeelScriptDetail> {
    return this.getProxy().getProjectScript(projectId, scriptId)
  }

  async runProjectScript(projectId: string, request: RunProjectScriptRequest): Promise<RunProjectScriptResponse> {
    return this.getProxy().runProjectScript(projectId, request)
  }

  private getProxy(): PeelService {
    if (!this.proxy) {
      this.proxy = this.wsConnectionProvider.createProxy<PeelService>(PeelServicePath)
    }
    return this.proxy
  }
}
