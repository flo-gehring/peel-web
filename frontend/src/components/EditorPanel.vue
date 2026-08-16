<script setup lang="ts">
import { ref, computed, shallowRef, watch } from 'vue'
import { VueMonacoEditor } from '@guolao/vue-monaco-editor'

// 1. Accept props passed by Dockview via params
const props = defineProps<{
  params?: {
    documentId?: string
    filename?: string
    content?: string
  }
}>()

// 2. Reactive code buffer
console.log('EditorPanel.vue props:', props)
console.log('EditorPanel.vue params:', props.params)
const code = ref(
  props.params?.params?.content ||
    `Trouble loading content for ${props.params?.params?.filename ?? 'unknown file'}`,
)

// 3. Dynamic language detection based on file extension
const language = computed(() => {
  const filename = props.params?.params.filename?.toLowerCase() || ''
  if (filename.endsWith('.java')) return 'java'
  if (filename.endsWith('.ts') || filename.endsWith('.js')) return 'typescript'
  if (filename.endsWith('.json')) return 'json'
  if (filename.endsWith('.xml') || filename.endsWith('.pom')) return 'xml'
  if (filename.endsWith('.md')) return 'markdown'
  if (filename.endsWith('.css')) return 'css'
  if (filename.endsWith('.html')) return 'html'
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
  useEditorDraftStore.setDraft(props.params?.params?.documentId ?? '', newVal)
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
