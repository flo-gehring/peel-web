import type { PeelProjectSummary } from '../common/peel-types';
export declare class SpringProjectsClient {
    listProjects(): Promise<PeelProjectSummary[]>;
    private apiBaseUrl;
}
