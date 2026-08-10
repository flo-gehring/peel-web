import { inject, injectable } from '@theia/core/shared/inversify'
import { CommandContribution, CommandRegistry, MessageService, QuickPickService } from '@theia/core/lib/common'
import { SingleTextInputDialog } from '@theia/core/lib/browser'
import { OpenerService, open } from '@theia/core/lib/browser/opener-service'
import { EditorManager } from '@theia/editor/lib/browser/editor-manager'
import { WorkspaceService } from '@theia/workspace/lib/browser'

import { PeelCommands } from './peel-commands'
import { PeelFrontendService } from './services/peel-frontend-service'
import { PeelProjectTreeModel } from './widgets/peel-project-tree-model'
import { PeelProjectNode } from './widgets/peel-project-node'

@injectable()
export class PeelCommandContribution implements CommandContribution {
  @inject(MessageService)
  protected readonly messageService!: MessageService

  @inject(PeelProjectTreeModel)
  protected readonly treeModel!: PeelProjectTreeModel

  @inject(PeelFrontendService)
  protected readonly peelFrontendService!: PeelFrontendService

  @inject(QuickPickService)
  protected readonly quickPickService!: QuickPickService

  @inject(OpenerService)
  protected readonly openerService!: OpenerService

  @inject(EditorManager)
  protected readonly editorManager!: EditorManager

  @inject(WorkspaceService)
  protected readonly workspaceService!: WorkspaceService

  registerCommands(registry: CommandRegistry): void {
    registry.registerCommand(PeelCommands.NewProject, {
      execute: () => this.handleCreateProject(),
    })

    registry.registerCommand(PeelCommands.NewScript, {
      execute: () => this.handleCreateScript(),
    })

    registry.registerCommand(PeelCommands.NewDocument, {
      execute: () => this.messageService.info('Create document flow will be added next.'),
    })

    registry.registerCommand(PeelCommands.NewRenderConfig, {
      execute: () => this.messageService.info('Create render config flow will be added next.'),
    })

    registry.registerCommand(PeelCommands.RunDefault, {
      execute: () => this.handleRunCurrentScript(),
    })

    registry.registerCommand(PeelCommands.ValidateDefault, {
      execute: () => this.messageService.info('Validate default flow will be added next.'),
    })

    registry.registerCommand(PeelCommands.RenderPreview, {
      execute: () => this.messageService.info('Render preview flow will be added next.'),
    })

    registry.registerCommand(PeelCommands.OpenProjectsView, {
      execute: () => {
        this.messageService.info('Peel projects view is available in the left panel.')
      },
    })

    registry.registerCommand(PeelCommands.RefreshProjects, {
      execute: async () => {
        await this.treeModel.refreshTree()
        this.messageService.info('Projects refreshed.')
      },
    })

    registry.registerCommand(PeelCommands.SelectProject, {
      execute: () => this.handleSelectProject(),
    })
  }

  private async handleCreateProject(): Promise<void> {
    const nameDialog = new SingleTextInputDialog({
      title: 'Create PEEL Project',
      initialValue: 'Untitled project',
      confirmButtonLabel: 'Continue',
      validate: (value) => (value.trim().length === 0 ? 'Project name is required.' : ''),
    })
    const name = await nameDialog.open()
    if (!name) {
      return
    }

    const idDialog = new SingleTextInputDialog({
      title: 'Project ID (optional)',
      initialValue: slugify(name),
      confirmButtonLabel: 'Create',
      validate: (value) => {
        const trimmed = value.trim()
        if (!trimmed) {
          return ''
        }
        if (!/^[a-z0-9][a-z0-9-]{1,62}$/.test(trimmed)) {
          return 'Project ID must match ^[a-z0-9][a-z0-9-]{1,62}$.'
        }
        return ''
      },
    })
    const idInput = await idDialog.open()
    if (idInput === undefined) {
      return
    }

    await this.peelFrontendService.createProject({
      name: name.trim(),
      id: idInput.trim().length > 0 ? idInput.trim() : undefined,
      template: 'empty',
    })
    await this.treeModel.refreshTree()
    this.messageService.info('Project created.')
  }

  private async handleSelectProject(): Promise<void> {
    const projects = await this.peelFrontendService.listProjects()
    if (projects.length === 0) {
      this.messageService.info('No projects available. Create one first.')
      return
    }

    const activeProjectId = this.peelFrontendService.getActiveProjectId()
    const picked = await this.quickPickService.show(
      projects.map((project) => ({
        id: project.id,
        label: project.name,
        description: project.id,
        detail: project.id === activeProjectId ? 'Active project' : project.description ?? '',
      })),
      {
        placeholder: 'Select active PEEL project',
      },
    )

    if (!picked?.id) {
      return
    }

    this.peelFrontendService.setActiveProjectId(picked.id)
    await this.treeModel.refreshTree()
    this.messageService.info(`Active project: ${picked.label}`)
  }

  private async handleCreateScript(): Promise<void> {
    const projectId = await this.ensureActiveProjectId()
    if (!projectId) {
      return
    }

    const scriptNameDialog = new SingleTextInputDialog({
      title: 'New PEEL Script',
      initialValue: 'main.peel',
      confirmButtonLabel: 'Create',
      validate: (value) => {
        const trimmed = value.trim()
        if (!trimmed) {
          return 'Script name is required.'
        }
        if (trimmed.includes('..') || trimmed.includes(':') || trimmed.startsWith('/')) {
          return 'Invalid script name.'
        }
        return ''
      },
    })
    const rawScriptName = await scriptNameDialog.open()
    if (!rawScriptName) {
      return
    }

    const normalizedName = rawScriptName.trim().toLowerCase().endsWith('.peel')
      ? rawScriptName.trim()
      : `${rawScriptName.trim()}.peel`

    const savedScript = await this.peelFrontendService.saveProjectScript(projectId, {
      id: normalizedName,
      name: normalizedName,
      script: '',
    })

    const workspaceRoot = this.workspaceService.tryGetRoots()[0]?.resource
    if (!workspaceRoot) {
      this.messageService.warn('Could not resolve workspace root to open the script.')
      return
    }

    const scriptUri = workspaceRoot.resolve(`scripts/${savedScript.id}`)
    await open(this.openerService, scriptUri)
    this.messageService.info(`Script created: ${savedScript.id}`)
  }

  private async handleRunCurrentScript(): Promise<void> {
    const projectId = await this.ensureActiveProjectId()
    if (!projectId) {
      return
    }

    const activeEditor = this.editorManager.currentEditor
    if (!activeEditor) {
      this.messageService.warn('Open a .peel script to run it.')
      return
    }

    if (!activeEditor.editor.uri.path.toString().toLowerCase().endsWith('.peel')) {
      this.messageService.warn('The active editor is not a .peel script.')
      return
    }

    const uri = activeEditor.editor.uri
    const scriptId = this.resolveScriptIdForProject(projectId, uri.path.toString())
    if (!scriptId) {
      this.messageService.warn('The active editor is not inside the selected project scripts directory.')
      return
    }

    const scriptContent = activeEditor.editor.document.getText()
    const result = await this.peelFrontendService.runProjectScript(projectId, {
      scriptId,
      script: scriptContent,
      bindings: {},
    })
    this.messageService.info(`Run successful. Result keys: ${Object.keys(result.result).join(', ') || 'none'}`)
  }

  private async ensureActiveProjectId(): Promise<string | undefined> {
    const activeProjectId = this.peelFrontendService.getActiveProjectId()
    if (activeProjectId) {
      return activeProjectId
    }

    const selectedNode = this.treeModel.selectedNodes[0]
    if (selectedNode && PeelProjectNode.is(selectedNode)) {
      this.peelFrontendService.setActiveProjectId(selectedNode.projectId)
      return selectedNode.projectId
    }

    const projects = await this.peelFrontendService.listProjects()
    if (projects.length === 0) {
      this.messageService.warn('No project found. Create a project first.')
      return undefined
    }

    this.peelFrontendService.setActiveProjectId(projects[0].id)
    await this.treeModel.refreshTree()
    return projects[0].id
  }

  private resolveScriptIdForProject(projectId: string, path: string): string | undefined {
    const marker = `/projects/${projectId}/scripts/`
    const index = path.indexOf(marker)
    if (index === -1) {
      return undefined
    }

    const rawScriptId = path.slice(index + marker.length)
    if (!rawScriptId) {
      return undefined
    }
    return decodeURIComponent(rawScriptId)
  }
}

function slugify(value: string): string {
  const slug = value
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+/, '')
    .replace(/-+$/, '')
  return slug.length > 0 ? slug : 'untitled-project'
}
