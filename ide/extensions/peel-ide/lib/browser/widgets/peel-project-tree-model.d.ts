import { TreeModelImpl } from '@theia/core/lib/browser';
import { PeelFrontendService } from '../services/peel-frontend-service';
export declare class PeelProjectTreeModel extends TreeModelImpl {
    protected readonly peelFrontendService: PeelFrontendService;
    protected init(): void;
    refreshTree(): Promise<void>;
    private newGroupNode;
}
