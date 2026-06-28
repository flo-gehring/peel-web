import React from '@theia/core/shared/react';
import { ContextMenuRenderer, NodeProps, TreeNode, TreeProps, TreeWidget } from '@theia/core/lib/browser';
import { PeelProjectTreeModel } from './peel-project-tree-model';
export declare class PeelProjectTreeWidget extends TreeWidget {
    static readonly ID = "peel-projects-tree";
    static readonly LABEL = "Peel Projects";
    constructor(props: TreeProps, model: PeelProjectTreeModel, contextMenuRenderer: ContextMenuRenderer);
    protected renderIcon(node: TreeNode, props: NodeProps): React.ReactNode;
    toNodeName(node: TreeNode): string;
}
