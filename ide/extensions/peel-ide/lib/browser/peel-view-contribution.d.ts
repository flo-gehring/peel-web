import { AbstractViewContribution, FrontendApplication, FrontendApplicationContribution } from '@theia/core/lib/browser';
import { PeelProjectTreeWidget } from './widgets/peel-project-tree-widget';
export declare class PeelViewContribution extends AbstractViewContribution<PeelProjectTreeWidget> implements FrontendApplicationContribution {
    constructor();
    onStart(_app: FrontendApplication): Promise<void>;
}
