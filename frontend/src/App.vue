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
import { useWorkspaceSelectionStore } from '@/stores/workspaceSelection'

// Mandatory CSS theme import for Dockview
import 'dockview-vue/dist/styles/dockview.css'
// Import panel components
import FileTreePanel from './components/FileTreePanel.vue'
import EditorPanel from './components/EditorPanel.vue'
import OutputPanel from './components/OutputPanel.vue'
import GroupActions from './components/GroupActions.vue'

// 1. Component Registry: Map identifier strings to Vue component definitions
const components: Record<string, DefineComponent<Record<string, unknown>>> = {
  fileTree: FileTreePanel as DefineComponent<Record<string, unknown>>,
  editor: EditorPanel as DefineComponent<Record<string, unknown>>,
  output: OutputPanel as DefineComponent<Record<string, unknown>>,
}

const rightHeaderActionsComponent = GroupActions as unknown as VueComponent
type PeelScriptDocument = Extract<PeelWorkspaceDocument, { kind: 'peel' }>

const percentageWidth = (percent: number): number => {
  if (typeof window === 'undefined') return 0
  return Math.round((window.innerWidth * percent) / 100)
}

// Store reference to Dockview API
const dockviewApi = shallowRef<DockviewApi | null>(null)
const editorGroupId = shallowRef<string | null>(null)
const draftStore = useEditorDraftStore()
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
    const selected = selectionStore.selectedScript
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

// 3. Dynamic Action: Open or Switch Editor Tabs Programmatically
async function openFileInEditor(file: PeelScriptDocument) {
  console.log(`Opening new editor tab for file: ${file.name} (ID: ${file.id})`)

  if (!dockviewApi.value) return

  const panelId = `editor-script-${file.id}`
  const existingPanel = dockviewApi.value.getPanel(panelId)

  if (existingPanel) {
    existingPanel.api.setActive()
    return
  }

  const inFlightOpen = pendingOpenById.get(file.id)
  if (inFlightOpen) {
    await inFlightOpen
    const panelAfterOpen = dockviewApi.value?.getPanel(panelId)
    panelAfterOpen?.api.setActive()
    return
  }

  const openPromise = (async () => {
    let content = draftStore.getDraft(file.id)

    if (content === undefined) {
      const { data, error } = await api.GET('/scripts/{id}', {
        params: {
          path: { id: file.id },
        },
      })
      console.log(`Fetched script content for ${file.name}:`, data, error)

      if (error) {
        console.error('Failed to load script content for editor tab:', error)
        content = ''
      } else {
        content = data?.script ?? ''
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

  pendingOpenById.set(file.id, openPromise)

  try {
    await openPromise
  } finally {
    pendingOpenById.delete(file.id)
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
