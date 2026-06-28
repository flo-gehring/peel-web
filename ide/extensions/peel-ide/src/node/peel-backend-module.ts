import { ContainerModule } from '@theia/core/shared/inversify'
import { ConnectionHandler, RpcConnectionHandler } from '@theia/core/lib/common'

import { PeelService, PeelServicePath } from '../common/peel-protocol'
import { PeelBackendService } from './peel-backend-service'
import { SpringProjectsClient } from './spring-projects-client'

export default new ContainerModule((bind) => {
  bind(SpringProjectsClient).toSelf().inSingletonScope()
  bind(PeelBackendService).toSelf().inSingletonScope()
  bind(PeelService).toService(PeelBackendService)

  bind(ConnectionHandler)
    .toDynamicValue((ctx) =>
      new RpcConnectionHandler<PeelService>(PeelServicePath, () => ctx.container.get(PeelBackendService)),
    )
    .inSingletonScope()
})
