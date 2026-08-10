<script setup lang="ts">
import { shallowRef, type DefineComponent } from 'vue'
import { DockviewVue, type DockviewReadyEvent, type DockviewApi } from 'dockview-vue'

// Mandatory CSS theme import for Dockview
import 'dockview-vue/dist/styles/dockview.css'
// Import panel components
import FileTreePanel from './components/FileTreePanel.vue'
import EditorPanel from './components/EditorPanel.vue'
import OutputPanel from './components/OutputPanel.vue'

// 1. Component Registry: Map identifier strings to Vue component definitions
const components: Record<string, DefineComponent<Record<string, unknown>>> = {
  fileTree: FileTreePanel as DefineComponent<Record<string, unknown>>,
  editor: EditorPanel as DefineComponent<Record<string, unknown>>,
  output: OutputPanel as DefineComponent<Record<string, unknown>>,
}

// Store reference to Dockview API
const dockviewApi = shallowRef<DockviewApi | null>(null)

// 2. Layout Initialization Callback
const onReady = (event: DockviewReadyEvent) => {
  const api = event.api
  dockviewApi.value = api

  const fileTreePanel = api.addPanel({
    id: 'file-tree',
    component: 'fileTree',
    title: 'Explorer',
    initialWidth: 250,
    params: {
      onFileSelect: (filename: string) => openFileInEditor(filename),
    },
  })

  const editorPanel = api.addPanel({
    id: 'editor-App.java',
    component: 'editor',
    title: 'App.java',
    params: { filename: 'App.java' },
    position: {
      referencePanel: fileTreePanel,
      direction: 'right',
    },
  })

  api.addPanel({
    id: 'output-console',
    component: 'output',
    title: 'Output',
    initialHeight: 180,
    position: {
      referencePanel: editorPanel,
      direction: 'below',
    },
  })
}

// 3. Dynamic Action: Open or Switch Editor Tabs Programmatically
function openFileInEditor(filename: string) {
  if (!dockviewApi.value) return

  const panelId = `editor-${filename}`
  const existingPanel = dockviewApi.value.getPanel(panelId)

  if (existingPanel) {
    // Panel already exists: Focus its tab
    existingPanel.api.setActive()
  } else {
    const activeGroup = dockviewApi.value.activeGroup || dockviewApi.value.groups[0]

    dockviewApi.value.addPanel({
      id: panelId,
      component: 'editor',
      title: filename,
      params: { filename },
      ...(activeGroup
        ? {
            position: {
              referenceGroup: activeGroup,
            },
          }
        : {}),
    })
  }
}
</script>

<template>
  <div class="app-layout">
    <DockviewVue
      class="dockview-root dockview-theme-abyss"
      :components="components"
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
