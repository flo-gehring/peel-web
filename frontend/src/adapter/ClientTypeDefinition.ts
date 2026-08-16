export type PeelWorkspaceDocument =
  | { kind: 'peel'; id: string; name: string; icon: string }
  | { kind: 'renderConfig'; id: string } // placeholder for future type
  | { kind: 'document'; id: string } // placeholder for future type
