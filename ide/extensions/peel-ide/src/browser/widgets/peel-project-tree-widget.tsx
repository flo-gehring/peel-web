import React from '@theia/core/shared/react'
import { inject, injectable } from '@theia/core/shared/inversify'
import {
  ContextMenuRenderer,
  NodeProps,
  TreeNode,
  TreeProps,
  TreeWidget,
} from '@theia/core/lib/browser'

import { PeelProjectTreeModel } from './peel-project-tree-model'
import { PeelProjectNode, PeelRootNode, PeelTreeGroupNode } from './peel-project-node'

@injectable()
export class PeelProjectTreeWidget extends TreeWidget {
  static readonly ID = 'peel-projects-tree'
  static readonly LABEL = 'Peel Projects'

  constructor(
    @inject(TreeProps) props: TreeProps,
    @inject(PeelProjectTreeModel) model: PeelProjectTreeModel,
    @inject(ContextMenuRenderer) contextMenuRenderer: ContextMenuRenderer,
  ) {
    super(props, model, contextMenuRenderer)

    this.id = PeelProjectTreeWidget.ID
    this.title.label = PeelProjectTreeWidget.LABEL
    this.title.caption = PeelProjectTreeWidget.LABEL
    this.title.closable = true
    this.title.iconClass = 'codicon codicon-folder-library'
  }

  protected override renderIcon(node: TreeNode, props: NodeProps): React.ReactNode {
    if (PeelRootNode.is(node)) {
      return <div className="codicon codicon-folder-opened" />
    }
    if (PeelProjectNode.is(node)) {
      return <div className="codicon codicon-package" />
    }
    if (PeelTreeGroupNode.is(node)) {
      return <div className="codicon codicon-folder" />
    }
    return super.renderIcon(node, props)
  }

  override toNodeName(node: TreeNode): string {
    if (PeelProjectNode.is(node)) {
      return node.active ? `${node.name ?? node.id} (active)` : node.name ?? node.id
    }
    if (PeelRootNode.is(node) || PeelTreeGroupNode.is(node)) {
      return node.name ?? node.id
    }
    return super.toNodeName(node)
  }
}
