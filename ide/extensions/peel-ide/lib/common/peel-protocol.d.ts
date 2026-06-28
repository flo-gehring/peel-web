export declare const PeelServicePath = "/services/peel-service";
export declare const PeelService: unique symbol;
export interface PeelService {
    listProjects(): Promise<import('./peel-types').PeelProjectSummary[]>;
}
