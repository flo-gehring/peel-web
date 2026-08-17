<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'

import { api } from '@/adapter/client'
import type { PeelWorkspaceDocument } from '@/adapter/ClientTypeDefinition'
import { useWorkspaceSelectionStore } from '@/stores/workspaceSelection'

type PeelScriptDocument = Extract<PeelWorkspaceDocument, { kind: 'peel' }>
type RenderConfigDocument = Extract<PeelWorkspaceDocument, { kind: 'renderConfig' }>
type CreatableDocumentKind = 'peel' | 'renderConfig'

// Dockview passes panel props automatically to registered components
defineProps<{ params?: Record<string, never> }>()

const scripts = ref<PeelScriptDocument[]>([])
const renderConfigs = ref<RenderConfigDocument[]>([])
const selectionStore = useWorkspaceSelectionStore()

const isLoading = ref(false)
const activeFileId = ref<string | null>(null)
const isCreateDialogOpen = ref(false)
const newDocumentName = ref('')
const newDocumentKind = ref<CreatableDocumentKind>('peel')
const isCreating = ref(false)
const deletingScriptId = ref<string | null>(null)

async function fetchFiles() {
  isLoading.value = true
  try {
    const [scriptsResponse, renderConfigsResponse] = await Promise.all([
      api.GET('/scripts'),
      api.GET('/render-config/list-ids'),
    ])

    if (scriptsResponse.error) {
      console.error('Error fetching scripts:', scriptsResponse.error)
    } else {
      scripts.value = (scriptsResponse.data ?? []).map((script) => ({
        kind: 'peel',
        id: script.id ?? '',
        name: script.name ?? '',
        icon: '🍌',
      }))
    }

    if (renderConfigsResponse.error) {
      console.error('Error fetching render configurations:', renderConfigsResponse.error)
    } else {
      renderConfigs.value = (renderConfigsResponse.data ?? []).map((renderConfig) => ({
        kind: 'renderConfig',
        id: renderConfig.id ?? '',
        name: renderConfig.name ?? '',
        icon: '{}',
      }))
    }
  } catch (error) {
    console.error('Unexpected workspace item fetch error:', error)
  } finally {
    isLoading.value = false
  }
}

function selectDocument(file: PeelScriptDocument | RenderConfigDocument) {
  activeFileId.value = `${file.kind}-${file.id}`
  selectionStore.selectDocument(file)
}

function openCreateDialog() {
  newDocumentName.value = ''
  newDocumentKind.value = 'peel'
  isCreateDialogOpen.value = true
}

async function createDocument() {
  const name = newDocumentName.value.trim()
  if (!name || isCreating.value) {
    return
  }

  isCreating.value = true

  try {
    if (newDocumentKind.value === 'peel') {
      const { data, error } = await api.POST('/scripts', {
        body: { name, script: '' },
      })

      if (error || !data?.id) {
        console.error('Failed to create script:', error)
        return
      }

      const file: PeelScriptDocument = {
        kind: 'peel',
        id: data.id,
        name: data.name ?? name,
        icon: '🍌',
      }
      scripts.value.push(file)
      isCreateDialogOpen.value = false
      selectDocument(file)
      return
    }

    const { data: defaultConfig, error: defaultError } = await api.GET('/render-config/default')
    if (defaultError || !defaultConfig) {
      console.error('Failed to load default render configuration:', defaultError)
      return
    }

    const { data, error } = await api.POST('/render-config/save', {
      body: {
        name,
        renderConfigurationDto: defaultConfig,
      },
    })
    if (error || !data?.id) {
      console.error('Failed to create render configuration:', error)
      return
    }

    const file: RenderConfigDocument = {
      kind: 'renderConfig',
      id: data.id,
      name,
      icon: '{}',
    }
    renderConfigs.value.push(file)
    isCreateDialogOpen.value = false
    selectDocument(file)
  } catch (error) {
    console.error('Unexpected workspace item creation error:', error)
  } finally {
    isCreating.value = false
  }
}

async function deletePeelScript(file: PeelScriptDocument) {
  if (deletingScriptId.value || !window.confirm(`Delete "${file.name}"?`)) {
    return
  }

  deletingScriptId.value = file.id

  try {
    const { error } = await api.DELETE('/scripts/{id}', {
      params: {
        path: { id: file.id },
      },
    })

    if (error) {
      console.error('Failed to delete script:', error)
      return
    }

    scripts.value = scripts.value.filter((item) => item.id !== file.id)
    selectionStore.deleteScript(file.id)
  } catch (error) {
    console.error('Unexpected script deletion error:', error)
  } finally {
    deletingScriptId.value = null
  }
}

onMounted(() => fetchFiles())
</script>
<template>
  <div class="file-tree-container">
    <div class="panel-header">
      <span>PROJECT EXPLORER</span>
      <button class="create-button" title="New workspace item" @click="openCreateDialog">+</button>
    </div>
    <div v-if="isLoading" class="loading">Loading...</div>
    <section class="document-section">
      <h2>PEEL SCRIPTS</h2>
      <ul class="file-list">
        <li
          v-for="file in scripts"
          :key="`peel-${file.id}`"
          :class="{ active: activeFileId === `peel-${file.id}` }"
          @click="selectDocument(file)"
        >
          <span class="file-icon">{{ file.icon }}</span>
          <span class="file-name">{{ file.name }}</span>
          <button
            class="delete-button"
            :disabled="deletingScriptId === file.id"
            :title="`Delete ${file.name}`"
            @click.stop="deletePeelScript(file)"
          >
            {{ deletingScriptId === file.id ? '...' : '×' }}
          </button>
        </li>
      </ul>
    </section>
    <section class="document-section">
      <h2>RENDER CONFIGS</h2>
      <ul class="file-list">
        <li
          v-for="file in renderConfigs"
          :key="`renderConfig-${file.id}`"
          :class="{ active: activeFileId === `renderConfig-${file.id}` }"
          @click="selectDocument(file)"
        >
          <span class="file-icon">{{ file.icon }}</span>
          <span class="file-name">{{ file.name }}</span>
        </li>
      </ul>
    </section>

    <div v-if="isCreateDialogOpen" class="create-dialog-backdrop" @click.self="isCreateDialogOpen = false">
      <form class="create-dialog" @submit.prevent="createDocument">
        <h2>New Workspace Item</h2>
        <label for="workspace-item-type">Type</label>
        <select id="workspace-item-type" v-model="newDocumentKind">
          <option value="peel">PEEL Script</option>
          <option value="renderConfig">Render Config</option>
        </select>
        <label for="new-document-name">Name</label>
        <input id="new-document-name" v-model="newDocumentName" autofocus placeholder="Name" />
        <div class="dialog-actions">
          <button type="button" @click="isCreateDialogOpen = false">Cancel</button>
          <button type="submit" :disabled="!newDocumentName.trim() || isCreating">
            {{ isCreating ? 'Creating...' : 'Create' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
.file-tree-container {
  height: 100%;
  background-color: #181818;
  color: #cccccc;
  font-family: system-ui, sans-serif;
  font-size: 13px;
  user-select: none;
}
.panel-header {
  padding: 8px 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
  font-size: 11px;
  color: #888888;
  border-bottom: 1px solid #2b2b2b;
}
.create-button {
  border: 0;
  background: transparent;
  color: #cccccc;
  font-size: 18px;
  line-height: 16px;
  cursor: pointer;
}
.create-button:hover {
  color: #ffffff;
}
.file-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.document-section h2 {
  margin: 0;
  padding: 12px 12px 4px;
  color: #888888;
  font-size: 11px;
  font-weight: bold;
}
.loading {
  padding: 8px 12px;
  color: #888888;
}
.file-list li {
  padding: 6px 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
}
.file-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.delete-button {
  visibility: hidden;
  border: 0;
  background: transparent;
  color: #cccccc;
  cursor: pointer;
  font-size: 18px;
  line-height: 16px;
}
.file-list li:hover .delete-button,
.file-list li.active .delete-button {
  visibility: visible;
}
.delete-button:hover {
  color: #f48771;
}
.delete-button:disabled {
  cursor: default;
  opacity: 0.5;
}
.file-list li:hover {
  background-color: #2a2d2e;
}
.file-list li.active {
  background-color: #37373d;
  color: #ffffff;
}
.create-dialog-backdrop {
  position: fixed;
  inset: 0;
  z-index: 10;
  display: grid;
  place-items: center;
  background: rgba(0, 0, 0, 0.45);
}
.create-dialog {
  width: min(320px, calc(100vw - 32px));
  display: grid;
  gap: 12px;
  padding: 16px;
  border: 1px solid #3c3c3c;
  border-radius: 6px;
  background: #252526;
}
.create-dialog h2 {
  margin: 0;
  font-size: 15px;
}
.create-dialog select,
.create-dialog input[type='text'],
.create-dialog input:not([type]) {
  width: 100%;
  box-sizing: border-box;
  padding: 7px;
  border: 1px solid #3c3c3c;
  background: #1e1e1e;
  color: #ffffff;
}
.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
.dialog-actions button {
  padding: 5px 10px;
  border: 1px solid #3c3c3c;
  border-radius: 3px;
  background: #333333;
  color: #ffffff;
  cursor: pointer;
}
.dialog-actions button:disabled {
  cursor: default;
  opacity: 0.5;
}
</style>
