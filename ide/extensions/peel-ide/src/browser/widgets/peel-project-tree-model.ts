import { inject, injectable, postConstruct } from '@theia/core/shared/inversify'
import { CompositeTreeNode, TreeModelImpl } from '@theia/core/lib/browser'

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
    void this.refreshTree()
  }

  async refreshTree(): Promise<void> {
    const projects = await this.peelFrontendService.listProjects()

    const root: PeelRootNode = {
      id: ROOT_ID,
      name: 'PEEL Projects',
      parent: undefined,
      visible: false,
      children: [],
      kind: 'root',
    }

    for (const project of projects) {
      const projectNode: PeelProjectNode = {
        id: `peel-project-${project.id}`,
        name: project.name,
        parent: root,
        children: [],
        kind: 'project',
        projectId: project.id,
        description: project.description ?? undefined,
        expanded: true,
      }

      CompositeTreeNode.addChild(projectNode, this.newGroupNode(projectNode, 'scripts', 'Scripts'))
      CompositeTreeNode.addChild(projectNode, this.newGroupNode(projectNode, 'documents', 'Documents'))
      CompositeTreeNode.addChild(projectNode, this.newGroupNode(projectNode, 'render-configs', 'Render Configurations'))
      CompositeTreeNode.addChild(root, projectNode)
    }

    this.tree.root = root
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
      expanded: false,
    }
  }
}
