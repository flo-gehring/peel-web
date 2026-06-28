import { ContainerModule } from '@theia/core/shared/inversify'
import {
  bindViewContribution,
  createTreeContainer,
  FrontendApplicationContribution,
  KeybindingContribution,
  TreeModel,
  WidgetFactory,
} from '@theia/core/lib/browser'
import { CommandContribution, MenuContribution } from '@theia/core/lib/common'

import { PeelCommandContribution } from './peel-command-contribution'
import { PeelFrontendContribution } from './peel-frontend-contribution'
import { PeelKeybindingContribution } from './peel-keybinding-contribution'
import { PeelMenuContribution } from './peel-menu-contribution'
import { PeelViewContribution } from './peel-view-contribution'
import { PeelFrontendService } from './services/peel-frontend-service'
import { PeelProjectTreeModel } from './widgets/peel-project-tree-model'
import { PeelProjectTreeWidget } from './widgets/peel-project-tree-widget'

export default new ContainerModule((bind) => {
  bind(PeelFrontendService).toSelf().inSingletonScope()

  bind(CommandContribution).to(PeelCommandContribution).inSingletonScope()
  bind(MenuContribution).to(PeelMenuContribution).inSingletonScope()
  bind(KeybindingContribution).to(PeelKeybindingContribution).inSingletonScope()
  bind(FrontendApplicationContribution).to(PeelFrontendContribution).inSingletonScope()

  bindViewContribution(bind, PeelViewContribution)
  bind(FrontendApplicationContribution).toService(PeelViewContribution)

  bind(WidgetFactory)
    .toDynamicValue((ctx) => ({
      id: PeelProjectTreeWidget.ID,
      createWidget: () => {
        const child = createTreeContainer(ctx.container, {
          model: PeelProjectTreeModel,
          widget: PeelProjectTreeWidget,
          props: {
            search: true,
            multiSelect: false,
          },
        })
        child.bind(PeelProjectTreeModel).toSelf().inSingletonScope()
        child.bind(TreeModel).toService(PeelProjectTreeModel)
        child.bind(PeelProjectTreeWidget).toSelf()
        return child.get(PeelProjectTreeWidget)
      },
    }))
    .inSingletonScope()
})
