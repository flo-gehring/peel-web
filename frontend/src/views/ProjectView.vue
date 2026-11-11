<script setup lang="ts">
import { shallowRef, watch, type DefineComponent } from 'vue'
import {
  onBeforeRouteLeave,
  onBeforeRouteUpdate,
  useRoute,
  useRouter,
  type RouteLocationNormalized,
} from 'vue-router'
import {
  DockviewVue,
  themeAbyss,
  type DockviewReadyEvent,
  type DockviewApi,
  type VueComponent,
} from 'dockview-vue'
import { api } from '@/adapter/client'
import type { PeelWorkspaceDocument } from '@/adapter/ClientTypeDefinition'
import { useEditorDraftStore } from '@/stores/editorDrafts'
import { useDocumentDraftStore, type DocumentDraft } from '@/stores/documentDrafts'
import { useDocumentPreviewStore } from '@/stores/documentPreview'
import { useWorkspaceSelectionStore } from '@/stores/workspaceSelection'
import { useProjectStore } from '@/stores/projects'

// Mandatory CSS theme import for Dockview
import 'dockview-vue/dist/styles/dockview.css'
// Import panel components
import FileTreePanel from '../components/FileTreePanel.vue'
import EditorPanel from '../components/EditorPanel.vue'
import DocumentPanel from '../components/DocumentPanel.vue'
import RenderPreviewPanel from '../components/RenderPreviewPanel.vue'
import BindingsPanel from '../components/BindingsPanel.vue'
import OutputPanel from '../components/OutputPanel.vue'
import GroupActions from '../components/GroupActions.vue'
import EditorWelcome from '../components/EditorWelcome.vue'

// 1. Component Registry: Map identifier strings to Vue component definitions
const components: Record<string, DefineComponent<Record<string, unknown>>> = {
  welcome: EditorWelcome as DefineComponent<Record<string, unknown>>,
  fileTree: FileTreePanel as DefineComponent<Record<string, unknown>>,
  editor: EditorPanel as DefineComponent<Record<string, unknown>>,
  documentEditor: DocumentPanel as DefineComponent<Record<string, unknown>>,
  renderPreview: RenderPreviewPanel as DefineComponent<Record<string, unknown>>,
  bindings: BindingsPanel as DefineComponent<Record<string, unknown>>,
  output: OutputPanel as DefineComponent<Record<string, unknown>>,
}

const rightHeaderActionsComponent = GroupActions as unknown as VueComponent
const workspaceTheme = {
  ...themeAbyss,
  name: 'peel',
  className: 'dockview-theme-abyss dockview-theme-peel',
  gap: 1,
}

const percentageWidth = (percent: number): number => {
  if (typeof window === 'undefined') return 0
  return Math.round((window.innerWidth * percent) / 100)
}

// Store reference to Dockview API
const dockviewApi = shallowRef<DockviewApi | null>(null)
const editorGroupId = shallowRef<string | null>(null)
const draftStore = useEditorDraftStore()
const documentDraftStore = useDocumentDraftStore()
const documentPreviewStore = useDocumentPreviewStore()
const selectionStore = useWorkspaceSelectionStore()
const projectStore = useProjectStore()
const route = useRoute()
const router = useRouter()
const pendingOpenById = new Map<string, Promise<void>>()
const pendingProjectRoute = shallowRef<RouteLocationNormalized | null>(null)
const skipProjectSwitchGuard = shallowRef(false)

watch(
  () => route.params.projectId,
  async (projectId) => {
    if (typeof projectId !== 'string') return
    try {
      await projectStore.loadProject(projectId)
    } catch (error) {
      console.error('Failed to load project:', error)
    }
  },
  { immediate: true },
)

function hasDirtyDrafts(): boolean {
  return (
    Object.values(draftStore.draftsById).some((draft) => draft.dirty) ||
    Object.values(documentDraftStore.draftsById).some((draft) => draft.dirty)
  )
}

function discardDrafts(): void {
  draftStore.clear()
  documentDraftStore.clear()
  selectionStore.clear()
  if (!dockviewApi.value) return
  for (const panelId of Object.keys(dockviewApi.value.toJSON().panels)) {
    if (panelId.startsWith('editor-')) dockviewApi.value.getPanel(panelId)?.api.close()
  }
}

async function continueProjectSwitch(): Promise<void> {
  const target = pendingProjectRoute.value
  pendingProjectRoute.value = null
  if (!target) return
  skipProjectSwitchGuard.value = true
  await router.push(target)
  skipProjectSwitchGuard.value = false
}

async function discardAndSwitch(): Promise<void> {
  discardDrafts()
  await continueProjectSwitch()
}

async function saveAndSwitch(): Promise<void> {
  const project = projectStore.activeProject
  if (!project) return

  try {
    for (const [id, draft] of Object.entries(draftStore.draftsById)) {
      if (!draft.dirty) continue
      const script = project.scripts?.find((item) => item.id === id)
      if (script) {
        const { error } = await api.PUT('/api/scripts/{id}', {
          params: { path: { id } },
          body: { name: script.name, scriptContent: draft.content },
        })
        if (error) throw error
        draftStore.markSaved(id)
        continue
      }
      const renderConfig = project.renderConfigurations?.find((item) => item.id === id)
      if (renderConfig) {
        const renderConfigurationDto = JSON.parse(draft.content)
        const { error } = await api.PUT('/api/render-config/{id}', {
          params: { path: { id } },
          body: { name: renderConfig.name, renderConfigurationDto },
        })
        if (error) throw error
        draftStore.markSaved(id)
      }
    }

    for (const [id, draft] of Object.entries(documentDraftStore.draftsById)) {
      if (!draft.dirty) continue
      const { error } = await api.PUT('/api/documents/{id}', {
        params: { path: { id } },
        body: {
          name: draft.name,
          scriptNameTags: draft.scriptNameTags,
          templateHtml: draft.templateHtml,
          editorStateJson: draft.editorStateJson,
          renderConfigurationId: draft.renderConfigurationId,
        },
      })
      if (error) throw error
      documentDraftStore.markSaved(id)
    }
  } catch (error) {
    console.error('Failed to save drafts before switching projects:', error)
    return
  }

  await continueProjectSwitch()
}

onBeforeRouteLeave((to) => {
  if (skipProjectSwitchGuard.value || !hasDirtyDrafts()) {
    discardDrafts()
    return true
  }
  pendingProjectRoute.value = to
  return false
})

onBeforeRouteUpdate((to) => {
  if (to.params.projectId === route.params.projectId) return true
  if (skipProjectSwitchGuard.value || !hasDirtyDrafts()) {
    discardDrafts()
    return true
  }
  pendingProjectRoute.value = to
  return false
})

// 2. Layout Initialization Callback
const onReady = (event: DockviewReadyEvent) => {
  const dockApi = event.api
  dockviewApi.value = dockApi

  const editorGroup = dockApi.addGroup({ direction: 'right', id: 'editor-group' })
  editorGroupId.value = editorGroup.id

  const leftGroup = dockApi.addEdgeGroup('left', {
    id: 'left-group',
    initialSize: Math.max(220, percentageWidth(18)),
    minimumSize: 180,
  })

  dockApi.addPanel({
    id: 'file-tree',
    component: 'fileTree',
    title: 'Explorer',
    initialWidth: percentageWidth(15),
    position: {
      referenceGroup: leftGroup.id,
    },
  })

  dockApi.addPanel({
    id: 'workspace-welcome',
    component: 'welcome',
    title: 'Start',
    position: { referenceGroup: editorGroup },
  })

  dockApi.addPanel({
    id: 'output-console',
    component: 'output',
    title: 'Output',
    initialHeight: Math.max(220, Math.round(window.innerHeight * 0.3)),
    position: {
      referenceGroup: editorGroup,
      direction: 'below',
    },
  })

  dockApi.addPanel({
    id: 'bindings-json',
    component: 'bindings',
    title: 'Bindings',
    initialWidth: percentageWidth(24),
    position: { referenceGroup: editorGroup, direction: 'right' },
  })
  dockApi.getPanel('workspace-welcome')?.api.setActive()
}

watch(
  () => selectionStore.selectionVersion,
  async () => {
    const selected = selectionStore.selectedDocument
    if (!selected) {
      return
    }

    try {
      await openFileInEditor(selected)
    } catch (error) {
      console.error('Failed while handling file selection:', error)
    }
  },
)

watch(
  () => documentPreviewStore.previewVersion,
  () => {
    if (!dockviewApi.value) return

    const existingPanel = dockviewApi.value.getPanel('render-preview')
    if (existingPanel) {
      existingPanel.api.setActive()
      return
    }

    const editorGroup = dockviewApi.value.groups.find((group) => group.id === editorGroupId.value)
    const targetGroup = editorGroup || dockviewApi.value.activeGroup || dockviewApi.value.groups[0]
    dockviewApi.value.addPanel({
      id: 'render-preview',
      component: 'renderPreview',
      title: 'Render Preview',
      ...(targetGroup ? { position: { referenceGroup: targetGroup } } : {}),
    })
  },
)

watch(
  () => selectionStore.deletionVersion,
  () => {
    const deletedDocument = selectionStore.deletedDocument
    if (!deletedDocument) {
      return
    }

    draftStore.removeDocument(deletedDocument.id)
    documentDraftStore.removeDocument(deletedDocument.id)
    const panelType = deletedDocument.kind === 'peel' ? 'script' : deletedDocument.kind
    dockviewApi.value?.getPanel(`editor-${panelType}-${deletedDocument.id}`)?.api.close()
  },
)

// 3. Dynamic Action: Open or Switch Editor Tabs Programmatically
async function openFileInEditor(file: PeelWorkspaceDocument) {
  if (!dockviewApi.value) return

  const panelId = `editor-${file.kind === 'peel' ? 'script' : file.kind === 'renderConfig' ? 'render-config' : 'document'}-${file.id}`
  const existingPanel = dockviewApi.value.getPanel(panelId)

  if (existingPanel) {
    existingPanel.api.setActive()
    return
  }

  const pendingKey = `${file.kind}-${file.id}`
  const inFlightOpen = pendingOpenById.get(pendingKey)
  if (inFlightOpen) {
    await inFlightOpen
    const panelAfterOpen = dockviewApi.value?.getPanel(panelId)
    panelAfterOpen?.api.setActive()
    return
  }

  const openPromise = (async () => {
    if (file.kind === 'document') {
      let draft = documentDraftStore.getDraft(file.id)
      if (!draft) {
        const { data: document, error: documentError } = await api.GET('/api/documents/{id}', {
          params: { path: { id: file.id } },
        })
        if (documentError || !document) {
          console.error('Failed to load document for editor tab:', documentError)
          return
        }
        draft = {
          name: document.name ?? file.name,
          editorStateJson:
            document.editorStateJson ?? '{"type":"doc","content":[{"type":"paragraph"}]}',
          templateHtml: document.templateHtml ?? '<p></p>',
          scriptNameTags: document.scriptNameTags ?? {},
          renderConfigurationId: document.renderConfigurationId ?? '',
          renderConfigurations: (projectStore.activeProject?.renderConfigurations ?? []).map(
            (config) => ({
              id: config.id ?? '',
              name: config.name ?? '',
            }),
          ),
          dirty: false,
        } satisfies DocumentDraft
        documentDraftStore.setDraft(file.id, draft)
      }

      const editorGroup = dockviewApi.value?.groups.find(
        (group) => group.id === editorGroupId.value,
      )
      const targetGroup =
        editorGroup || dockviewApi.value?.activeGroup || dockviewApi.value?.groups[0]
      dockviewApi.value?.addPanel({
        id: panelId,
        component: 'documentEditor',
        title: file.name,
        params: { documentId: file.id, draft },
        ...(targetGroup ? { position: { referenceGroup: targetGroup } } : {}),
      })
      return
    }

    let content = draftStore.getDraft(file.id)

    if (content === undefined) {
      if (file.kind === 'peel') {
        const { data, error } = await api.GET('/api/scripts/{id}', {
          params: { path: { id: file.id } },
        })
        if (error) {
          console.error('Failed to load script content for editor tab:', error)
          content = ''
        } else {
          content = data?.script ?? ''
        }
      } else {
        const { data, error } = await api.GET('/api/render-config/{id}', {
          params: { path: { id: file.id } },
        })
        if (error) {
          console.error('Failed to load render configuration for editor tab:', error)
          content = ''
        } else {
          content = JSON.stringify(data?.renderConfigurationDto ?? {}, null, 2)
        }
      }

      draftStore.markLoaded(file.id, content)
    }

    const editorGroup = dockviewApi.value?.groups.find((group) => group.id === editorGroupId.value)
    const targetGroup =
      editorGroup || dockviewApi.value?.activeGroup || dockviewApi.value?.groups[0]

    dockviewApi.value?.addPanel({
      id: panelId,
      component: 'editor',
      title: file.name,
      params: {
        filename: file.name,
        content,
        documentId: file.id,
        documentKind: file.kind,
      },
      ...(targetGroup
        ? {
            position: {
              referenceGroup: targetGroup,
            },
          }
        : {}),
    })
  })()

  pendingOpenById.set(pendingKey, openPromise)

  try {
    await openPromise
  } finally {
    pendingOpenById.delete(pendingKey)
  }
}
</script>

<template>
  <div class="app-layout">
    <div v-if="projectStore.isReadOnly" class="archive-banner" role="status">
      Archived project — read-only. Existing results can still be downloaded.
      <RouterLink :to="`/projects/${route.params.projectId}/settings`">Project settings</RouterLink>
    </div>
    <DockviewVue
      class="dockview-root"
      :theme="workspaceTheme"
      :components="components"
      :rightHeaderActionsComponent="rightHeaderActionsComponent"
      @ready="onReady"
    />
    <div v-if="pendingProjectRoute" class="switch-dialog-backdrop">
      <section
        class="switch-dialog"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="switch-project-title"
      >
        <h2 id="switch-project-title">Unsaved changes</h2>
        <p>Save your changes before switching projects?</p>
        <div>
          <button @click="saveAndSwitch">Save and switch</button>
          <button @click="discardAndSwitch">Discard and switch</button>
          <button @click="pendingProjectRoute = null">Cancel</button>
        </div>
      </section>
    </div>
  </div>
</template>

<style>
.app-layout {
  flex-direction: column;
  height: calc(100dvh - var(--app-header-height));
  width: 100%;
  display: flex;
  min-height: 0;
  min-width: 0;
}
.archive-banner {
  padding: 10px 18px;
  color: var(--peel-muted);
  background: var(--peel-surface);
  border-bottom: 1px solid var(--peel-border);
}

.dockview-root {
  flex: 1;
  width: 100%;
  height: 100%;
  min-height: 0;
  min-width: 0;
}

/* Optional Dockview Theme Customizations */
.dockview-theme-abyss.dockview-theme-peel {
  --dv-background-color: var(--peel-border);
  --dv-group-view-background-color: var(--peel-background);
  --dv-tabs-and-actions-container-background-color: var(--peel-surface);
  --dv-tabs-and-actions-container-height: 44px;
  --dv-tabs-and-actions-container-font-size: 13px;
  --dv-activegroup-visiblepanel-tab-background-color: var(--peel-background);
  --dv-activegroup-hiddenpanel-tab-background-color: var(--peel-surface);
  --dv-inactivegroup-visiblepanel-tab-background-color: var(--peel-background);
  --dv-inactivegroup-hiddenpanel-tab-background-color: var(--peel-surface);
  --dv-activegroup-visiblepanel-tab-color: var(--peel-text);
  --dv-activegroup-hiddenpanel-tab-color: var(--peel-muted);
  --dv-inactivegroup-visiblepanel-tab-color: var(--peel-text);
  --dv-inactivegroup-hiddenpanel-tab-color: var(--peel-muted);
  --dv-tab-divider-color: var(--peel-border);
  --dv-separator-border: var(--peel-border);
  --dv-active-sash-color: var(--peel-accent);
  --dv-drag-over-background-color: #ffcf3220;
  --dv-drag-over-border: 1px solid var(--peel-accent);
  --dv-icon-hover-background-color: var(--peel-raised);
}
.dockview-theme-peel .dv-tab.dv-active-tab {
  box-shadow: inset 0 -2px var(--peel-accent);
}
.dockview-theme-peel .dv-tab {
  padding-inline: 14px;
}
.dockview-theme-peel .dv-groupview {
  border-radius: 0;
}

.switch-dialog-backdrop {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: grid;
  place-items: center;
  background: rgba(0, 0, 0, 0.5);
}
.switch-dialog {
  width: min(420px, calc(100vw - 32px));
  padding: 24px;
  border: 1px solid var(--peel-border);
  border-radius: 12px;
  background: var(--peel-surface);
  color: var(--peel-text);
}
.switch-dialog h2,
.switch-dialog p {
  margin: 0 0 12px;
}
.switch-dialog div {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}
.switch-dialog button {
  padding: 6px 10px;
  border: 1px solid #3c3c3c;
  background: #333;
  color: inherit;
  cursor: pointer;
}
</style>
