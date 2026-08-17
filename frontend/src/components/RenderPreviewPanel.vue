<script setup lang="ts">
import { useDocumentPreviewStore } from '@/stores/documentPreview'

const previewStore = useDocumentPreviewStore()
</script>

<template>
  <div class="render-preview-panel">
    <div class="preview-header">RENDER PREVIEW</div>
    <p v-if="previewStore.isLoading" class="preview-message">Rendering document...</p>
    <p v-else-if="previewStore.error" class="preview-message preview-error">{{ previewStore.error }}</p>
    <iframe
      v-else-if="previewStore.html !== null"
      class="preview-frame"
      title="Rendered document preview"
      sandbox
      :srcdoc="previewStore.html"
    ></iframe>
    <p v-else class="preview-message">Preview a document to see its rendered HTML.</p>
  </div>
</template>

<style scoped>
.render-preview-panel { height: 100%; display: flex; flex-direction: column; min-height: 0; background: #202328; color: #ccc; }
.preview-header { padding: 8px 12px; border-bottom: 1px solid #34373c; background: #25272b; color: #999; font-size: 11px; font-weight: bold; }
.preview-frame { flex: 1; min-height: 0; border: 0; background: #fff; }
.preview-message { margin: auto; padding: 24px; color: #aaa; text-align: center; }
.preview-error { color: #f48771; white-space: pre-wrap; }
</style>
