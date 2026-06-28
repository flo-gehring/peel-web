import { CommandContribution, CommandRegistry, MessageService } from '@theia/core/lib/common';
import { PeelProjectTreeModel } from './widgets/peel-project-tree-model';
export declare class PeelCommandContribution implements CommandContribution {
    protected readonly messageService: MessageService;
    protected readonly treeModel: PeelProjectTreeModel;
    registerCommands(registry: CommandRegistry): void;
}
