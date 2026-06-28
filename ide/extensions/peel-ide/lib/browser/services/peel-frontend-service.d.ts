import { WebSocketConnectionProvider } from '@theia/core/lib/browser/messaging/ws-connection-provider';
import type { PeelProjectSummary } from '../../common/peel-types';
export declare class PeelFrontendService {
    private readonly wsConnectionProvider;
    private proxy;
    constructor(wsConnectionProvider: WebSocketConnectionProvider);
    listProjects(): Promise<PeelProjectSummary[]>;
}
