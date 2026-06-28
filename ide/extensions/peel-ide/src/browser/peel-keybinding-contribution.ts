import { injectable } from '@theia/core/shared/inversify'
import { KeybindingContribution, KeybindingRegistry } from '@theia/core/lib/browser'

import { PeelCommands } from './peel-commands'

@injectable()
export class PeelKeybindingContribution implements KeybindingContribution {
  registerKeybindings(keybindings: KeybindingRegistry): void {
    keybindings.registerKeybinding({
      command: PeelCommands.RunDefault.id,
      keybinding: 'ctrlcmd+enter',
    })

    keybindings.registerKeybinding({
      command: PeelCommands.ValidateDefault.id,
      keybinding: 'shift+ctrlcmd+enter',
    })

    keybindings.registerKeybinding({
      command: PeelCommands.OpenProjectsView.id,
      keybinding: 'ctrlcmd+alt+p',
    })
  }
}
