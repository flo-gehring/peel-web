<script setup lang="ts">
import { ref, onMounted } from 'vue'

import { api } from '@/adapter/client'

// Dockview passes panel props automatically to registered components
defineProps<{
  params?: {
    onFileSelect?: (filename: string) => void
  }
}>()

const files = ref<{ id: string; name: string; icon: string }[]>([])

const isLoading = ref(false)
const activeFile = ref<string | null>(null)

async function fetchFiles() {
  console.log('Fetching files...')
  isLoading.value = true
  api
    .GET('/scripts')
    .then(({ data }) => {
      files.value = (data ?? []).map((script) => ({
        id: script.id ?? '',
        name: script.name ?? '',
        icon: '📄',
      }))
    })
    .catch((error) => {
      console.error('Error fetching files:', error)
    })
  console.log('Files fetched:', files.value)
  isLoading.value = false
}

function selectFile(filename: string, onFileSelect?: (fn: string) => void) {
  activeFile.value = filename
  if (onFileSelect) {
    onFileSelect(filename)
  }
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
        @click="selectFile(file.name, params?.onFileSelect)"
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
