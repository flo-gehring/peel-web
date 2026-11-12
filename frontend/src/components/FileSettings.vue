<script setup lang="ts">
import { X } from '@lucide/vue'
import { useProjectFileStore } from '@/stores/projectFiles'
import { useProjectStore } from '@/stores/projects'
withDefaults(
  defineProps<{ documentId: string; title?: string; showDetails?: boolean; panelId?: string }>(),
  { title: 'File settings', showDetails: true },
)
const emit = defineEmits<{ close: [] }>()
const files = useProjectFileStore()
const projects = useProjectStore()
</script>
<template>
  <aside
    :id="panelId || `file-settings-${documentId}`"
    class="file-settings"
    :aria-label="title"
    @keydown.esc.stop="emit('close')"
  >
    <header>
      <h2>{{ title }}</h2>
      <button title="Close settings" aria-label="Close settings" @click="emit('close')">
        <X :size="18" />
      </button>
    </header>
    <fieldset :disabled="projects.isReadOnly">
      <template v-if="showDetails">
        <label :for="`file-name-${documentId}`">Name</label>
        <input
          :id="`file-name-${documentId}`"
          :value="files.metadata(documentId).name"
          @input="
            files.setMetadata(documentId, { name: ($event.target as HTMLInputElement).value })
          "
        />
        <label :for="`file-description-${documentId}`">Description</label>
        <textarea
          :id="`file-description-${documentId}`"
          rows="3"
          :value="files.metadata(documentId).description"
          @input="
            files.setMetadata(documentId, {
              description: ($event.target as HTMLTextAreaElement).value,
            })
          "
        />
      </template>
      <slot />
    </fieldset>
    <p>
      {{
        showDetails
          ? 'Use Save to save file details and content together.'
          : 'Use Save to save document changes.'
      }}
    </p>
  </aside>
</template>
<style scoped>
.file-settings {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  z-index: 5;
  width: min(320px, 100%);
  padding: 16px;
  box-sizing: border-box;
  overflow: auto;
  background: var(--peel-surface);
  border-left: 1px solid var(--peel-border);
  box-shadow: -8px 0 24px #0004;
  color: var(--peel-text);
}
header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
h2 {
  margin: 0;
  font-size: 15px;
}
header button {
  display: grid;
  place-items: center;
  padding: 4px;
  border: 0;
  background: transparent;
  color: inherit;
  cursor: pointer;
}
fieldset {
  display: grid;
  gap: 8px;
  margin: 18px 0;
  padding: 0;
  border: 0;
  min-width: 0;
}
input,
textarea {
  width: 100%;
  box-sizing: border-box;
  background: var(--peel-background);
  color: var(--peel-text);
  border: 1px solid var(--peel-border);
  border-radius: 6px;
  padding: 7px;
}
textarea {
  resize: vertical;
}
label,
p {
  font-size: 12px;
}
p {
  color: var(--peel-muted);
}
</style>
