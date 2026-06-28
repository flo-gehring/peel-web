import { injectable } from '@theia/core/shared/inversify'
import { FrontendApplication, FrontendApplicationContribution } from '@theia/core/lib/browser'

@injectable()
export class PeelFrontendContribution implements FrontendApplicationContribution {
  onStart(_app: FrontendApplication): void {
    // placeholder for startup logic (workspace detection, config loading)
  }
}
