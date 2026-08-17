<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'

import { api } from '@/adapter/client'
import type { PeelWorkspaceDocument } from '@/adapter/ClientTypeDefinition'
import { useWorkspaceSelectionStore } from '@/stores/workspaceSelection'

type PeelScriptDocument = Extract<PeelWorkspaceDocument, { kind: 'peel' }>

// Dockview passes panel props automatically to registered components
defineProps<{ params?: Record<string, never> }>()

const files = ref<PeelScriptDocument[]>([])
const selectionStore = useWorkspaceSelectionStore()

const isLoading = ref(false)
const activeFile = ref<string | null>(null)
const isCreateDialogOpen = ref(false)
const newScriptName = ref('')
const isCreating = ref(false)
const deletingScriptId = ref<string | null>(null)

async function fetchFiles() {
  console.log('Fetching files...')
  isLoading.value = true
  api
    .GET('/scripts')
    .then(({ data }) => {
      files.value = (data ?? []).map((script) => ({
        kind: 'peel',
        id: script.id ?? '',
        name: script.name ?? '',
        icon: '🍌',
      }))
    })
    .catch((error) => {
      console.error('Error fetching files:', error)
    })
  console.log('Files fetched:', files.value)
  isLoading.value = false
}

function selectDocument(file: PeelScriptDocument) {
  activeFile.value = file.name
  selectionStore.selectScript(file)
}

function openCreateDialog() {
  newScriptName.value = ''
  isCreateDialogOpen.value = true
}

async function createPeelScript() {
  const name = newScriptName.value.trim()
  if (!name || isCreating.value) {
    return
  }

  isCreating.value = true

  try {
    const { data, error } = await api.POST('/scripts', {
      body: {
        name,
        script: '',
      },
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

    files.value.push(file)
    isCreateDialogOpen.value = false
    selectDocument(file)
  } catch (error) {
    console.error('Unexpected script creation error:', error)
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

    files.value = files.value.filter((item) => item.id !== file.id)
    selectionStore.deleteScript(file.id)
  } catch (error) {
    console.error('Unexpected script deletion error:', error)
  } finally {
    deletingScriptId.value = null
  }
}

onMounted(() => fetchFiles())
watch(() => selectionStore.scriptsVersion, fetchFiles)
</script>
<template>
  <div class="file-tree-container">
    <div class="panel-header">
      <span>PROJECT EXPLORER</span>
      <button class="create-button" title="New workspace item" @click="openCreateDialog">+</button>
    </div>
    <ul class="file-list">
      <li
        v-for="file in files"
        :key="file.id"
        :class="{ active: activeFile === file.name }"
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

    <div v-if="isCreateDialogOpen" class="create-dialog-backdrop" @click.self="isCreateDialogOpen = false">
      <form class="create-dialog" @submit.prevent="createPeelScript">
        <h2>New Workspace Item</h2>
        <label for="workspace-item-type">Type</label>
        <select id="workspace-item-type">
          <option value="peel">PEEL Script</option>
        </select>
        <label for="new-script-name">Name</label>
        <input id="new-script-name" v-model="newScriptName" autofocus placeholder="Script name" />
        <div class="dialog-actions">
          <button type="button" @click="isCreateDialogOpen = false">Cancel</button>
          <button type="submit" :disabled="!newScriptName.trim() || isCreating">
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
