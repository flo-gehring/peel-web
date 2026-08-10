import { inject, injectable, postConstruct } from '@theia/core/shared/inversify'
import { CompositeTreeNode, SelectableTreeNode, TreeModelImpl } from '@theia/core/lib/browser'

import { PeelFrontendService } from '../services/peel-frontend-service'
import { PeelProjectNode, PeelRootNode, PeelTreeGroupNode } from './peel-project-node'

const ROOT_ID = 'peel-projects-root'

@injectable()
export class PeelProjectTreeModel extends TreeModelImpl {
  @inject(PeelFrontendService)
  protected readonly peelFrontendService!: PeelFrontendService

  @postConstruct()
  protected override init(): void {
    super.init()
    this.toDispose.push(this.onSelectionChanged((selection) => {
      const first = selection[0]
      if (first && PeelProjectNode.is(first)) {
        this.peelFrontendService.setActiveProjectId(first.projectId)
      }
    }))
    void this.refreshTree()
  }

  async refreshTree(): Promise<void> {
    const projects = await this.peelFrontendService.listProjects()

    const root: PeelRootNode = {
      id: ROOT_ID,
      name: 'PEEL Projects',
      parent: undefined,
      visible: false,
      selected: false,
      children: [],
      kind: 'root',
    }

    const activeProjectId = this.peelFrontendService.getActiveProjectId()
    let activeProjectNode: PeelProjectNode | undefined

    for (const project of projects) {
      const projectNode: PeelProjectNode = {
        id: `peel-project-${project.id}`,
        name: project.name,
        parent: root,
        children: [],
        kind: 'project',
        projectId: project.id,
        description: project.description ?? undefined,
        selected: false,
        active: project.id === activeProjectId,
        expanded: true,
      }

      if (project.id === activeProjectId) {
        activeProjectNode = projectNode
      }

      CompositeTreeNode.addChild(projectNode, this.newGroupNode(projectNode, 'scripts', 'Scripts'))
      CompositeTreeNode.addChild(projectNode, this.newGroupNode(projectNode, 'documents', 'Documents'))
      CompositeTreeNode.addChild(projectNode, this.newGroupNode(projectNode, 'render-configs', 'Render Configurations'))
      CompositeTreeNode.addChild(root, projectNode)
    }

    this.tree.root = root

    if (!activeProjectNode && projects.length > 0) {
      const fallbackProjectId = projects[0].id
      this.peelFrontendService.setActiveProjectId(fallbackProjectId)
      activeProjectNode = this.tree.getNode(`peel-project-${fallbackProjectId}`) as PeelProjectNode | undefined
    }

    if (activeProjectNode && SelectableTreeNode.is(activeProjectNode)) {
      this.selectNode(activeProjectNode)
    }
  }

  private newGroupNode(
    parent: PeelProjectNode,
    groupType: PeelTreeGroupNode['groupType'],
    name: string,
  ): PeelTreeGroupNode {
    return {
      id: `${parent.id}-${groupType}`,
      name,
      parent,
      children: [],
      kind: 'group',
      groupType,
      selected: false,
      expanded: false,
    }
  }
}
