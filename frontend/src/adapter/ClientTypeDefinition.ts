export type PeelWorkspaceDocument =
  | { kind: 'peel'; id: string; name: string; icon: string }
  | { kind: 'renderConfig'; id: string; name: string; icon: string }
  | { kind: 'document'; id: string; name: string; icon: string }
