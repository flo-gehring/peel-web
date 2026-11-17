<script setup lang="ts">
import { t } from '@/i18n'
import { Info, Plus, Trash2 } from '@lucide/vue'
import { useProjectFileStore, type ExpectedBinding } from '@/stores/projectFiles'
const props = defineProps<{ documentId: string }>()
const files = useProjectFileStore()
function update(index: number, field: keyof ExpectedBinding, value: string): void {
  const bindings = files.expectedBindings(props.documentId).map((binding) => ({ ...binding }))
  bindings[index] = { ...bindings[index]!, [field]: value }
  files.setExpectedBindings(props.documentId, bindings)
}
function remove(index: number): void {
  files.setExpectedBindings(
    props.documentId,
    files.expectedBindings(props.documentId).filter((_, i) => i !== index),
  )
}
function add(): void {
  files.setExpectedBindings(props.documentId, [
    ...files.expectedBindings(props.documentId),
    { name: '', description: '' },
  ])
}
</script>
<template>
  <section class="script-bindings">
    <div class="bindings-header">
      <h3>{{ t('expectedBindings') }}</h3>
      <span
        class="help-icon"
        role="img"
        tabindex="0"
        :title="t('expectedBindingsHint')"
        :aria-label="t('expectedBindingsHint')"
      >
        <Info :size="14" aria-hidden="true" />
      </span>
      <button
        type="button"
        class="icon-button add-button"
        :title="t('addExpectedBinding')"
        :aria-label="t('addExpectedBinding')"
        @click="add"
      >
        <Plus :size="17" aria-hidden="true" />
      </button>
    </div>
    <div
      v-for="(binding, index) in files.expectedBindings(documentId)"
      :key="index"
      class="binding"
    >
      <label :for="`binding-name-${documentId}-${index}`">{{ t('name') }}</label>
      <div class="binding-name">
        <input
          :id="`binding-name-${documentId}-${index}`"
          :value="binding.name"
          @input="update(index, 'name', ($event.target as HTMLInputElement).value)"
        />
        <button
          type="button"
          class="icon-button remove-button"
          :title="t('removeExpectedBinding')"
          :aria-label="t('removeExpectedBinding')"
          @click="remove(index)"
        >
          <Trash2 :size="16" aria-hidden="true" />
        </button>
      </div>
      <label :for="`binding-description-${documentId}-${index}`">{{ t('description') }}</label>
      <textarea
        :id="`binding-description-${documentId}-${index}`"
        :value="binding.description"
        rows="2"
        @input="update(index, 'description', ($event.target as HTMLTextAreaElement).value)"
      />
    </div>
  </section>
</template>
<style scoped>
.script-bindings,
.binding {
  display: grid;
  gap: 8px;
}
.binding {
  border-bottom: 1px solid var(--peel-border);
  padding-bottom: 12px;
}
h3 {
  margin: 0;
  font-size: 13px;
}
label {
  font-size: 12px;
}
.bindings-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
}
.help-icon {
  display: flex;
  color: var(--peel-muted);
  cursor: help;
}
.add-button {
  margin-left: auto;
}
.binding-name {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 28px;
  align-items: center;
  gap: 6px;
}
input,
textarea {
  width: 100%;
  box-sizing: border-box;
  padding: 7px;
  color: var(--peel-text);
  background: var(--peel-background);
  border: 1px solid var(--peel-border);
  border-radius: 6px;
}
textarea {
  resize: vertical;
}
.icon-button {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  padding: 4px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: var(--peel-text);
  cursor: pointer;
}
.icon-button:hover:not(:disabled) {
  background: var(--peel-border);
}
.remove-button:hover:not(:disabled) {
  color: #ff6b6b;
}
.icon-button:focus-visible,
.help-icon:focus-visible {
  outline: 2px solid var(--peel-accent);
  outline-offset: 2px;
}
</style>
