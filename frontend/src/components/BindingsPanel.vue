<script setup lang="ts">
import { t } from '@/i18n'
import { computed } from 'vue'
import { ListPlus } from '@lucide/vue'
import { VueMonacoEditor } from '@guolao/vue-monaco-editor'
import { useRunBindingsStore } from '@/stores/runBindings'
import { definePeelEditorTheme } from '@/adapter/editorTheme'
import { useWorkspaceSelectionStore } from '@/stores/workspaceSelection'
import { useProjectFileStore } from '@/stores/projectFiles'

const bindingsStore = useRunBindingsStore()
const selection = useWorkspaceSelectionStore()
const files = useProjectFileStore()
const expectedNames = computed(() =>
  selection.activeScriptId
    ? files
        .expectedBindings(selection.activeScriptId)
        .flatMap((binding) => (binding.name ? [binding.name] : []))
    : [],
)

const bindingsJson = computed({
  get: () => bindingsStore.rawJson,
  set: (value: string) => bindingsStore.setRawJson(value),
})

const editorOptions = {
  automaticLayout: true,
  theme: 'peel-dark',
  fontSize: 13,
  fontFamily: "'Fira Code', 'Cascadia Code', Consolas, monospace",
  minimap: { enabled: false },
  padding: { top: 14, bottom: 14 },
  scrollBeyondLastLine: false,
  tabSize: 2,
}
</script>

<template>
  <div class="bindings-container">
    <div class="bindings-status" :class="{ invalid: !!bindingsStore.parseError }">
      <span v-if="bindingsStore.parseError"
        >{{ t('invalidJson') }} {{ bindingsStore.parseError }}</span
      >
      <span v-else>{{ t('validJson') }}</span>
      <button
        type="button"
        class="fill-bindings"
        :disabled="!!bindingsStore.parseError || !expectedNames.length"
        :title="t('fillExpectedBindingsHint')"
        :aria-label="t('fillExpectedBindings')"
        @click="bindingsStore.fillExpectedBindings(expectedNames)"
      >
        <ListPlus :size="16" aria-hidden="true" />
      </button>
    </div>
    <VueMonacoEditor
      v-model:value="bindingsJson"
      language="json"
      :options="editorOptions"
      @before-mount="definePeelEditorTheme"
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
  background-color: var(--peel-background);
}

.bindings-status {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  padding: 8px 14px;
  font-size: 12px;
  color: #a6e3a1;
  border-bottom: 1px solid var(--peel-border);
  background: var(--peel-surface);
}

.bindings-status.invalid {
  color: #f38ba8;
}

.fill-bindings {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 8px;
  border: 1px solid var(--peel-border);
  border-radius: 6px;
  background: var(--peel-background);
  color: var(--peel-text);
  cursor: pointer;
}
.fill-bindings:hover:not(:disabled) {
  background: var(--peel-border);
}
.fill-bindings:disabled {
  opacity: 0.5;
  cursor: default;
}
.fill-bindings:focus-visible {
  outline: 2px solid var(--peel-accent);
  outline-offset: 2px;
}

.bindings-editor {
  flex: 1;
  min-height: 0;
}
</style>
