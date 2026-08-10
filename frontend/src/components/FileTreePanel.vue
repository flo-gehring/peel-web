<script setup lang="ts">
import { ref } from 'vue'

// Dockview passes panel props automatically to registered components
defineProps<{
  params?: {
    onFileSelect?: (filename: string) => void
  }
}>()

// Static file tree mock data
const files = ref([
  { id: '1', name: 'src/main/java/App.java', icon: '📄' },
  { id: '2', name: 'src/main/java/Controller.java', icon: '📄' },
  { id: '3', name: 'pom.xml', icon: '⚙️' },
  { id: '4', name: 'README.md', icon: '📝' },
])

const activeFile = ref('App.java')

function selectFile(filename: string, onFileSelect?: (fn: string) => void) {
  activeFile.value = filename
  if (onFileSelect) {
    onFileSelect(filename)
  }
}
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
