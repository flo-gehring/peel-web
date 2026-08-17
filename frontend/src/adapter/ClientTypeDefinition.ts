export type PeelWorkspaceDocument =
  | { kind: 'peel'; id: string; name: string; icon: string }
  | { kind: 'renderConfig'; id: string; name: string; icon: string }
  | { kind: 'document'; id: string } // placeholder for future type
