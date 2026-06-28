import { PeelService } from '../common/peel-protocol';
import type { PeelProjectSummary } from '../common/peel-types';
import { SpringProjectsClient } from './spring-projects-client';
export declare class PeelBackendService implements PeelService {
    private readonly springProjectsClient;
    constructor(springProjectsClient: SpringProjectsClient);
    listProjects(): Promise<PeelProjectSummary[]>;
}
