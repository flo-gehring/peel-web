<script setup lang="ts">
import { ref, onMounted } from 'vue'

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

onMounted(() => fetchFiles())
</script>
<template>
  <div class="file-tree-container">
    <div class="panel-header">PROJECT EXPLORER</div>
    <ul class="file-list">
      <li
        v-for="file in files"
        :key="file.id"
        :class="{ active: activeFile === file.name }"
        @click="selectDocument(file)"
      >
        <span class="file-icon">{{ file.icon }}</span>
        <span class="file-name">{{ file.name }}</span>
      </li>
    </ul>
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
  font-weight: bold;
  font-size: 11px;
  color: #888888;
  border-bottom: 1px solid #2b2b2b;
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
.file-list li:hover {
  background-color: #2a2d2e;
}
.file-list li.active {
  background-color: #37373d;
  color: #ffffff;
}
</style>
