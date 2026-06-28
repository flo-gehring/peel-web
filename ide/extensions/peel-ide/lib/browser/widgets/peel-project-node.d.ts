import { CompositeTreeNode, ExpandableTreeNode, TreeNode } from '@theia/core/lib/browser';
export interface PeelTreeGroupNode extends CompositeTreeNode, ExpandableTreeNode {
    readonly kind: 'group';
    readonly groupType: 'scripts' | 'documents' | 'render-configs';
}
export declare namespace PeelTreeGroupNode {
    function is(node: unknown): node is PeelTreeGroupNode;
}
export interface PeelProjectNode extends CompositeTreeNode, ExpandableTreeNode {
    readonly kind: 'project';
    readonly projectId: string;
    readonly description?: string;
}
export declare namespace PeelProjectNode {
    function is(node: unknown): node is PeelProjectNode;
}
export interface PeelRootNode extends CompositeTreeNode {
    readonly kind: 'root';
}
export declare namespace PeelRootNode {
    function is(node: unknown): node is PeelRootNode;
}
export declare function isPeelNode(node: TreeNode): node is PeelRootNode | PeelProjectNode | PeelTreeGroupNode;
