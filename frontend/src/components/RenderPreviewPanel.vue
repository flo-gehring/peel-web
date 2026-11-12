<script setup lang="ts">
import { computed } from 'vue'
import { useDocumentPreviewStore } from '@/stores/documentPreview'

const props = defineProps<{ documentId: string; stale?: boolean }>()
const previewStore = useDocumentPreviewStore()
const preview = computed(() => previewStore.previews[props.documentId])
</script>

<template>
  <div class="render-preview-panel">
    <div class="preview-header">
      {{ preview?.name || 'Document' }}
      <span v-if="preview?.renderedAt">
        · {{ new Date(preview.renderedAt).toLocaleTimeString() }}</span
      >
    </div>
    <p v-if="stale && preview?.pdfUrl" class="stale-notice">
      The draft changed since this preview. Use Render preview to render the current draft.
    </p>
    <p v-if="preview?.isLoading" class="preview-message">Rendering document...</p>
    <p v-else-if="preview?.error" class="preview-message preview-error">{{ preview?.error }}</p>
    <iframe
      v-else-if="preview?.pdfUrl"
      class="preview-frame"
      title="Rendered document preview"
      :src="preview.pdfUrl"
    ></iframe>
    <p v-else class="preview-message">Preview a document to see its rendered PDF.</p>
  </div>
</template>

<style scoped>
.render-preview-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: #202328;
  color: #ccc;
}
.preview-header {
  padding: 8px 12px;
  border-bottom: 1px solid #34373c;
  background: #25272b;
  color: #999;
  font-size: 11px;
  font-weight: bold;
}
.stale-notice {
  margin: 0;
  padding: 8px 12px;
  font-size: 12px;
  color: var(--peel-accent);
}
.preview-frame {
  flex: 1;
  min-height: 0;
  border: 0;
  background: #fff;
}
.preview-message {
  margin: auto;
  padding: 24px;
  color: #aaa;
  text-align: center;
}
.preview-error {
  color: #f48771;
  white-space: pre-wrap;
}
</style>
