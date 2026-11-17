<script setup lang="ts">
import { formatDateTime, t } from '@/i18n'
import { computed } from 'vue'
import { useDocumentPreviewStore } from '@/stores/documentPreview'

const props = defineProps<{ documentId: string; stale?: boolean }>()
const previewStore = useDocumentPreviewStore()
const preview = computed(() => previewStore.previews[props.documentId])
</script>

<template>
  <div class="render-preview-panel">
    <div class="preview-header">
      {{ preview?.name || t('document') }}
      <span v-if="preview?.renderedAt"> · {{ formatDateTime(preview.renderedAt, true) }}</span>
    </div>
    <p v-if="stale && preview?.pdfUrl" class="stale-notice">
      {{ t('theDraftChangedSinceThisPreviewUseRenderPreview') }}
    </p>
    <ul v-if="preview?.warnings?.length" class="stale-notice" role="status">
      <li v-for="warning in preview.warnings" :key="warning">{{ warning }}</li>
    </ul>
    <p v-if="preview?.isLoading" class="preview-message">{{ t('renderingDocument') }}</p>
    <p v-else-if="preview?.error" class="preview-message preview-error">{{ preview?.error }}</p>
    <iframe
      v-else-if="preview?.pdfUrl"
      class="preview-frame"
      :title="t('renderedDocumentPreview')"
      :src="preview.pdfUrl"
    ></iframe>
    <p v-else class="preview-message">{{ t('previewADocumentToSeeItsRenderedPdf') }}</p>
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
