import { inject, injectable } from '@theia/core/shared/inversify'
import { CommandContribution, CommandRegistry, MessageService } from '@theia/core/lib/common'

import { PeelCommands } from './peel-commands'
import { PeelProjectTreeModel } from './widgets/peel-project-tree-model'

@injectable()
export class PeelCommandContribution implements CommandContribution {
  @inject(MessageService)
  protected readonly messageService!: MessageService

  @inject(PeelProjectTreeModel)
  protected readonly treeModel!: PeelProjectTreeModel

  registerCommands(registry: CommandRegistry): void {
    registry.registerCommand(PeelCommands.NewProject, {
      execute: () => this.messageService.info('Create project flow will be added next.'),
    })

    registry.registerCommand(PeelCommands.NewScript, {
      execute: () => this.messageService.info('Create script flow will be added next.'),
    })

    registry.registerCommand(PeelCommands.NewDocument, {
      execute: () => this.messageService.info('Create document flow will be added next.'),
    })

    registry.registerCommand(PeelCommands.NewRenderConfig, {
      execute: () => this.messageService.info('Create render config flow will be added next.'),
    })

    registry.registerCommand(PeelCommands.RunDefault, {
      execute: () => this.messageService.info('Run default flow will be added next.'),
    })

    registry.registerCommand(PeelCommands.ValidateDefault, {
      execute: () => this.messageService.info('Validate default flow will be added next.'),
    })

    registry.registerCommand(PeelCommands.RenderPreview, {
      execute: () => this.messageService.info('Render preview flow will be added next.'),
    })

    registry.registerCommand(PeelCommands.OpenProjectsView, {
      execute: () => {
        this.messageService.info('Peel projects view is available in the left panel.')
      },
    })

    registry.registerCommand(PeelCommands.RefreshProjects, {
      execute: async () => {
        await this.treeModel.refreshTree()
        this.messageService.info('Projects refreshed.')
      },
    })
  }
}
