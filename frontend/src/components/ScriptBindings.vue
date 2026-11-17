<script setup lang="ts">
import { t } from '@/i18n'
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
    <h3>{{ t('expectedBindings') }}</h3>
    <p>{{ t('expectedBindingsHint') }}</p>
    <div
      v-for="(binding, index) in files.expectedBindings(documentId)"
      :key="index"
      class="binding"
    >
      <label :for="`binding-name-${documentId}-${index}`">{{ t('name') }}</label>
      <input
        :id="`binding-name-${documentId}-${index}`"
        :value="binding.name"
        @input="update(index, 'name', ($event.target as HTMLInputElement).value)"
      />
      <label :for="`binding-description-${documentId}-${index}`">{{ t('description') }}</label>
      <textarea
        :id="`binding-description-${documentId}-${index}`"
        :value="binding.description"
        rows="2"
        @input="update(index, 'description', ($event.target as HTMLTextAreaElement).value)"
      />
      <button type="button" @click="remove(index)">{{ t('removeExpectedBinding') }}</button>
    </div>
    <button type="button" @click="add">{{ t('addExpectedBinding') }}</button>
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
  margin: 12px 0 0;
  font-size: 13px;
}
p,
label {
  font-size: 12px;
}
p {
  color: var(--peel-muted);
}
input,
textarea,
button {
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
button {
  cursor: pointer;
}
</style>
