<script setup lang="ts">
import { shallowRef, watch, type DefineComponent } from 'vue'
import {
  DockviewVue,
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

// Mandatory CSS theme import for Dockview
import 'dockview-vue/dist/styles/dockview.css'
// Import panel components
import FileTreePanel from './components/FileTreePanel.vue'
import EditorPanel from './components/EditorPanel.vue'
import DocumentPanel from './components/DocumentPanel.vue'
import RenderPreviewPanel from './components/RenderPreviewPanel.vue'
import BindingsPanel from './components/BindingsPanel.vue'
import OutputPanel from './components/OutputPanel.vue'
import GroupActions from './components/GroupActions.vue'

// 1. Component Registry: Map identifier strings to Vue component definitions
const components: Record<string, DefineComponent<Record<string, unknown>>> = {
  fileTree: FileTreePanel as DefineComponent<Record<string, unknown>>,
  editor: EditorPanel as DefineComponent<Record<string, unknown>>,
  documentEditor: DocumentPanel as DefineComponent<Record<string, unknown>>,
  renderPreview: RenderPreviewPanel as DefineComponent<Record<string, unknown>>,
  bindings: BindingsPanel as DefineComponent<Record<string, unknown>>,
  output: OutputPanel as DefineComponent<Record<string, unknown>>,
}

const rightHeaderActionsComponent = GroupActions as unknown as VueComponent

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
const pendingOpenById = new Map<string, Promise<void>>()

// 2. Layout Initialization Callback
const onReady = (event: DockviewReadyEvent) => {
  const dockApi = event.api
  dockviewApi.value = dockApi

  const editorGroup = dockApi.addGroup({ direction: 'right', id: 'editor-group' })
  editorGroupId.value = editorGroup.id

  const leftGroup = dockApi.addEdgeGroup('left', {
    id: 'left-group',
    initialSize: percentageWidth(20),
    minimumSize: 50,
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
    id: 'editor-App.java',
    component: 'editor',
    title: 'App.java',
    params: { filename: 'App.java' },
    position: {
      referenceGroup: editorGroup,
    },
  })

  dockApi.addPanel({
    id: 'bindings-json',
    component: 'bindings',
    title: 'Bindings',
    params: {},
    position: {
      referenceGroup: editorGroup,
    },
  })

  dockApi.addPanel({
    id: 'output-console',
    component: 'output',
    title: 'Output',
    initialHeight: 180,
    position: {
      referenceGroup: editorGroup,
      direction: 'below',
    },
  })
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
        const [{ data: document, error: documentError }, { data: renderConfigurations }] = await Promise.all([
          api.GET('/documents/{id}', { params: { path: { id: file.id } } }),
          api.GET('/render-config/list-ids'),
        ])
        if (documentError || !document) {
          console.error('Failed to load document for editor tab:', documentError)
          return
        }
        draft = {
          name: document.name ?? file.name,
          editorStateJson: document.editorStateJson ?? '{"type":"doc","content":[{"type":"paragraph"}]}',
          templateHtml: document.templateHtml ?? '<p></p>',
          scriptNameTags: document.scriptNameTags ?? {},
          renderConfigurationId: document.renderConfigurationId ?? '',
          renderConfigurations: (renderConfigurations ?? []).map((config) => ({
            id: config.id ?? '',
            name: config.name ?? '',
          })),
          dirty: false,
        } satisfies DocumentDraft
        documentDraftStore.setDraft(file.id, draft)
      }

      const editorGroup = dockviewApi.value?.groups.find((group) => group.id === editorGroupId.value)
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
        const { data, error } = await api.GET('/scripts/{id}', {
          params: { path: { id: file.id } },
        })
        if (error) {
          console.error('Failed to load script content for editor tab:', error)
          content = ''
        } else {
          content = data?.script ?? ''
        }
      } else {
        const { data, error } = await api.GET('/render-config/{id}', {
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
    <DockviewVue
      class="dockview-root dockview-theme-abyss"
      :components="components"
      :rightHeaderActionsComponent="rightHeaderActionsComponent"
      @ready="onReady"
    />
  </div>
</template>

<style>
/* Fullscreen view reset */
html,
body,
#app {
  margin: 0;
  padding: 0;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
}

.app-layout {
  height: 100vh;
  width: 100vw;
  display: flex;
  min-height: 0;
  min-width: 0;
}

.dockview-root {
  flex: 1;
  width: 100%;
  height: 100%;
  min-height: 0;
  min-width: 0;
}

/* Optional Dockview Theme Customizations */
.dockview-theme-abyss {
  --dv-background-color: #c73a3a;
  --dv-panegroup-header-background-color: #252526;
  --dv-activegroup-visiblepanel-tab-background-color: #1e1e1e;
  --dv-separator-border: #2b2b2b;
}
</style>
