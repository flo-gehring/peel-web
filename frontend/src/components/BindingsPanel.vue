<script setup lang="ts">
import { computed } from 'vue'
import { VueMonacoEditor } from '@guolao/vue-monaco-editor'
import { useRunBindingsStore } from '@/stores/runBindings'

const bindingsStore = useRunBindingsStore()

const bindingsJson = computed({
  get: () => bindingsStore.rawJson,
  set: (value: string) => bindingsStore.setRawJson(value),
})

const editorOptions = {
  automaticLayout: true,
  theme: 'vs-dark',
  fontSize: 13,
  fontFamily: "'Fira Code', 'Cascadia Code', Consolas, monospace",
  minimap: { enabled: false },
  scrollBeyondLastLine: false,
  tabSize: 2,
}
</script>

<template>
  <div class="bindings-container">
    <div class="bindings-status" :class="{ invalid: !!bindingsStore.parseError }">
      <span v-if="bindingsStore.parseError">Invalid JSON: {{ bindingsStore.parseError }}</span>
      <span v-else>Bindings JSON is valid</span>
    </div>
    <VueMonacoEditor
      v-model:value="bindingsJson"
      language="json"
      :options="editorOptions"
      class="bindings-editor"
    />
  </div>
</template>

<style scoped>
.bindings-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  min-height: 0;
  background-color: #1e1e1e;
}

.bindings-status {
  padding: 6px 10px;
  font-size: 12px;
  color: #a6e3a1;
  border-bottom: 1px solid #2b2b2b;
  background: #202020;
}

.bindings-status.invalid {
  color: #f38ba8;
}

.bindings-editor {
  flex: 1;
  min-height: 0;
}
</style>
