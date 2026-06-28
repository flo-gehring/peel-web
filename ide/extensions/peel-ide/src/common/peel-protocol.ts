export const PeelServicePath = '/services/peel-service'

export const PeelService = Symbol('PeelService')

export interface PeelService {
  listProjects(): Promise<import('./peel-types').PeelProjectSummary[]>
}
