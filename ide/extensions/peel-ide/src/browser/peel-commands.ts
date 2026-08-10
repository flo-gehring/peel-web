import { Command } from '@theia/core/lib/common'

export namespace PeelCommands {
  export const SelectProject: Command = {
    id: 'peel.project.select',
    label: 'Peel: Select Project',
  }

  export const OpenProjectsView: Command = {
    id: 'peel.projects.openView',
    label: 'Peel: Open Projects View',
  }

  export const RefreshProjects: Command = {
    id: 'peel.projects.refresh',
    label: 'Peel: Refresh Projects',
  }

  export const NewProject: Command = {
    id: 'peel.project.new',
    label: 'Peel: New Project',
  }

  export const NewScript: Command = {
    id: 'peel.script.new',
    label: 'Peel: New Script',
  }

  export const NewDocument: Command = {
    id: 'peel.document.new',
    label: 'Peel: New Document',
  }

  export const NewRenderConfig: Command = {
    id: 'peel.renderConfig.new',
    label: 'Peel: New Render Configuration',
  }

  export const RunDefault: Command = {
    id: 'peel.project.runDefault',
    label: 'Peel: Run Default Script',
  }

  export const ValidateDefault: Command = {
    id: 'peel.project.validateDefault',
    label: 'Peel: Validate Default Script',
  }

  export const RenderPreview: Command = {
    id: 'peel.project.renderPreview',
    label: 'Peel: Render Preview',
  }
}
