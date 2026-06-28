import { inject, injectable } from '@theia/core/shared/inversify'
import { WebSocketConnectionProvider } from '@theia/core/lib/browser/messaging/ws-connection-provider'

import { PeelService, PeelServicePath } from '../../common/peel-protocol'
import type { PeelProjectSummary } from '../../common/peel-types'

@injectable()
export class PeelFrontendService {
  private proxy: PeelService | undefined

  constructor(
    @inject(WebSocketConnectionProvider)
    private readonly wsConnectionProvider: WebSocketConnectionProvider,
  ) {}

  async listProjects(): Promise<PeelProjectSummary[]> {
    if (!this.proxy) {
      this.proxy = this.wsConnectionProvider.createProxy<PeelService>(PeelServicePath)
    }
    return this.proxy.listProjects()
  }
}
