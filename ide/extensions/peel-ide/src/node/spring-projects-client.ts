import { injectable } from '@theia/core/shared/inversify'

import type { PeelProjectListResponse, PeelProjectSummary } from '../common/peel-types'

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

  private apiBaseUrl(): string {
    const configured = process.env.PEEL_API_BASE_URL?.trim()
    if (!configured) {
      return DEFAULT_API_BASE_URL
    }
    return configured.endsWith('/') ? configured.slice(0, -1) : configured
  }
}
