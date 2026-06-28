import { CompositeTreeNode, ExpandableTreeNode, TreeNode } from '@theia/core/lib/browser'

export interface PeelTreeGroupNode extends CompositeTreeNode, ExpandableTreeNode {
  readonly kind: 'group'
  readonly groupType: 'scripts' | 'documents' | 'render-configs'
}

export namespace PeelTreeGroupNode {
  export function is(node: unknown): node is PeelTreeGroupNode {
    return CompositeTreeNode.is(node) && ExpandableTreeNode.is(node) && (node as PeelTreeGroupNode).kind === 'group'
  }
}

export interface PeelProjectNode extends CompositeTreeNode, ExpandableTreeNode {
  readonly kind: 'project'
  readonly projectId: string
  readonly description?: string
}

export namespace PeelProjectNode {
  export function is(node: unknown): node is PeelProjectNode {
    return CompositeTreeNode.is(node) && ExpandableTreeNode.is(node) && (node as PeelProjectNode).kind === 'project'
  }
}

export interface PeelRootNode extends CompositeTreeNode {
  readonly kind: 'root'
}

export namespace PeelRootNode {
  export function is(node: unknown): node is PeelRootNode {
    return CompositeTreeNode.is(node) && (node as PeelRootNode).kind === 'root'
  }
}

export function isPeelNode(node: TreeNode): node is PeelRootNode | PeelProjectNode | PeelTreeGroupNode {
  return PeelRootNode.is(node) || PeelProjectNode.is(node) || PeelTreeGroupNode.is(node)
}
