import { injectable } from '@theia/core/shared/inversify'
import { MenuContribution, MenuModelRegistry } from '@theia/core/lib/common'
import { CommonMenus } from '@theia/core/lib/browser'

import { PeelCommands } from './peel-commands'

@injectable()
export class PeelMenuContribution implements MenuContribution {
  registerMenus(menus: MenuModelRegistry): void {
    menus.registerMenuAction(CommonMenus.FILE_NEW, {
      commandId: PeelCommands.NewProject.id,
      label: 'PEEL Project',
    })

    menus.registerMenuAction(CommonMenus.VIEW_VIEWS, {
      commandId: PeelCommands.OpenProjectsView.id,
      label: 'Peel Projects',
    })

    menus.registerMenuAction(CommonMenus.VIEW_VIEWS, {
      commandId: PeelCommands.RefreshProjects.id,
      label: 'Refresh Peel Projects',
    })

    menus.registerMenuAction(CommonMenus.VIEW_VIEWS, {
      commandId: PeelCommands.SelectProject.id,
      label: 'Select Active Project',
    })
  }
}
