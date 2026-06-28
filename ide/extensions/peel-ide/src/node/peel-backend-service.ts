import { injectable, inject } from '@theia/core/shared/inversify'

import { PeelService } from '../common/peel-protocol'
import type { PeelProjectSummary } from '../common/peel-types'
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
}
