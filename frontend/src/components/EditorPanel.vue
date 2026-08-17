<script setup lang="ts">
import { ref, computed, shallowRef, watch } from 'vue'
import { VueMonacoEditor } from '@guolao/vue-monaco-editor'
import { useEditorDraftStore } from '@/stores/editorDrafts'

// 1. Accept props passed by Dockview via params
const props = defineProps<{
  params?: {
    api?: {
      id?: string
    }
    documentId?: string
    documentKind?: 'peel' | 'renderConfig'
    filename?: string
    content?: string
    id?: string
    params?: {
      documentId?: string
      documentKind?: 'peel' | 'renderConfig'
      filename?: string
      content?: string
    }
  }
}>()

const draftStore = useEditorDraftStore()

const panelId = computed(() => props.params?.api?.id || props.params?.id || '')
const documentId = computed(() => props.params?.params?.documentId || props.params?.documentId || '')
const filename = computed(() => props.params?.params?.filename || props.params?.filename || 'untitled')
const documentKind = computed(
  () => props.params?.params?.documentKind || props.params?.documentKind || 'peel',
)
const initialContent = computed(
  () =>
    props.params?.params?.content ??
    props.params?.content ??
    `Trouble loading content for ${filename.value}`,
)

if (panelId.value && documentId.value) {
  draftStore.bindPanelToDocument(panelId.value, documentId.value)
}

const existingDraft = panelId.value
  ? draftStore.getDraftByReference(panelId.value, documentId.value || undefined)
  : undefined

// 2. Reactive code buffer
const code = ref(existingDraft ?? initialContent.value)

// 3. Dynamic language detection based on file extension
const language = computed(() => {
  if (documentKind.value === 'renderConfig') return 'json'
  const lowered = filename.value.toLowerCase()
  if (lowered.endsWith('.java')) return 'java'
  if (lowered.endsWith('.ts') || lowered.endsWith('.js')) return 'typescript'
  if (lowered.endsWith('.json')) return 'json'
  if (lowered.endsWith('.xml') || lowered.endsWith('.pom')) return 'xml'
  if (lowered.endsWith('.md')) return 'markdown'
  if (lowered.endsWith('.css')) return 'css'
  if (lowered.endsWith('.html')) return 'html'
  return 'plaintext'
})

// 4. IMPORTANT: Use shallowRef for the editor instance to prevent
// Vue from making the Monaco internals reactive (which degrades performance)
const editorRef = shallowRef(null)

// Called when Monaco mounts inside the DOM
const handleMount = (editorInstance: any) => {
  editorRef.value = editorInstance
}

// 5. Monaco configuration options
const editorOptions = {
  // CRITICAL FOR DOCKVIEW: Tells Monaco to observe container size changes
  // and auto-resize when panel splitters are dragged
  automaticLayout: true,

  theme: 'vs-dark',
  fontSize: 14,
  fontFamily: "'Fira Code', 'Cascadia Code', Consolas, monospace",
  minimap: { enabled: true },
  scrollBeyondLastLine: false,
  tabSize: 4,
  smoothScrolling: true,
  cursorBlinking: 'smooth',
}

// 6. Notify parent or handle content updates
watch(code, (newVal) => {
  if (!panelId.value) {
    return
  }

  draftStore.setDraftByReference(panelId.value, newVal, documentId.value || undefined)
})
</script>

<template>
  <div class="editor-container">
    <VueMonacoEditor
      v-model:value="code"
      :language="language"
      :options="editorOptions"
      @mount="handleMount"
      class="monaco-editor-instance"
    />
  </div>
</template>

<style scoped>
.editor-container {
  width: 100%;
  height: 100%;
  overflow: hidden;
  background-color: #1e1e1e;
}

.monaco-editor-instance {
  width: 100%;
  height: 100%;
}
</style>
