import { injectable } from '@theia/core/shared/inversify'
import { AbstractViewContribution, FrontendApplication, FrontendApplicationContribution } from '@theia/core/lib/browser'

import { PeelCommands } from './peel-commands'
import { PeelProjectTreeWidget } from './widgets/peel-project-tree-widget'

@injectable()
export class PeelViewContribution
  extends AbstractViewContribution<PeelProjectTreeWidget>
  implements FrontendApplicationContribution
{
  constructor() {
    super({
      widgetId: PeelProjectTreeWidget.ID,
      widgetName: PeelProjectTreeWidget.LABEL,
      defaultWidgetOptions: {
        area: 'left',
      },
      toggleCommandId: PeelCommands.OpenProjectsView.id,
      toggleKeybinding: 'ctrlcmd+alt+p',
    })
  }

  async onStart(_app: FrontendApplication): Promise<void> {
    await this.openView({ activate: false, reveal: true })
  }
}
