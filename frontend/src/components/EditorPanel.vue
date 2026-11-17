<script setup lang="ts">
import { t } from '@/i18n'
import { ref, computed, shallowRef, watch, onUnmounted } from 'vue'
import FileToolbar from './FileToolbar.vue'
import FileSettings from './FileSettings.vue'
import ScriptBindings from './ScriptBindings.vue'
import { VueMonacoEditor } from '@guolao/vue-monaco-editor'
import { useEditorDraftStore } from '@/stores/editorDrafts'
import { definePeelEditorTheme } from '@/adapter/editorTheme'
import { registerPeelLanguage, bindPeelModel } from '@/adapter/peelLanguage'
import { useProjectFileStore } from '@/stores/projectFiles'
import { useProjectStore } from '@/stores/projects'
import { renderConfigurationSchema } from '@/adapter/renderConfigurationSchema'
import type { editor as MonacoEditor } from 'monaco-editor'

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
const projectStore = useProjectStore()
const files = useProjectFileStore()
const analysisAvailable = ref(true)
let languageSession: ReturnType<typeof bindPeelModel> | undefined
let modelListener: { dispose(): void } | undefined
const toolbar = ref<InstanceType<typeof FileToolbar> | null>(null)
const settingsOpen = ref(false)

const panelId = computed(() => props.params?.api?.id || props.params?.id || '')
const documentId = computed(
  () => props.params?.params?.documentId || props.params?.documentId || '',
)
const filename = computed(
  () => props.params?.params?.filename || props.params?.filename || 'untitled',
)
const documentKind = computed(
  () => props.params?.params?.documentKind || props.params?.documentKind || 'peel',
)
const initialContent = computed(
  () =>
    props.params?.params?.content ??
    props.params?.content ??
    t('troubleLoadingContentForName', { name: filename.value }),
)

if (panelId.value && documentId.value) {
  draftStore.bindPanelToDocument(panelId.value, documentId.value)
}

const existingDraft = panelId.value
  ? draftStore.getDraftByReference(panelId.value, documentId.value || undefined)
  : undefined

// 2. Reactive code buffer
const code = ref(existingDraft ?? initialContent.value)

const language = computed(() => {
  if (documentKind.value === 'renderConfig') return 'json'
  return 'peel'
})
const modelPath = computed(
  () =>
    'peel://workspace/' +
    encodeURIComponent(documentId.value || panelId.value) +
    (documentKind.value === 'renderConfig' ? '.json' : '.peel'),
)

// 4. IMPORTANT: Use shallowRef for the editor instance to prevent
// Vue from making the Monaco internals reactive (which degrades performance)
const editorRef = shallowRef<MonacoEditor.IStandaloneCodeEditor | null>(null)
let monacoApi: typeof import('monaco-editor') | undefined
function beforeMount(monaco: typeof import('monaco-editor')): void {
  monacoApi = monaco
  definePeelEditorTheme(monaco)
  registerPeelLanguage(monaco)
}
function validateConfiguration(): void {
  const model = editorRef.value?.getModel()
  if (!monacoApi || !model || documentKind.value !== 'renderConfig') return
  const markers: MonacoEditor.IMarkerData[] = []
  try {
    const result = renderConfigurationSchema.safeParse(JSON.parse(code.value))
    if (!result.success)
      for (const issue of result.error.issues) {
        const key = issue.path.at(-1)
        const offset =
          typeof key === 'string' ? Math.max(0, code.value.indexOf(JSON.stringify(key))) : 0
        const position = model.getPositionAt(offset)
        markers.push({
          severity: monacoApi.MarkerSeverity.Error,
          message: issue.message,
          startLineNumber: position.lineNumber,
          startColumn: position.column,
          endLineNumber: position.lineNumber,
          endColumn: position.column + (typeof key === 'string' ? key.length + 2 : 1),
        })
      }
  } catch {
    /* Monaco's JSON diagnostics mark syntax errors. */
  }
  monacoApi.editor.setModelMarkers(model, 'peel-render-config', markers)
}

// Called when Monaco mounts inside the DOM
const handleMount = (editorInstance: MonacoEditor.IStandaloneCodeEditor) => {
  editorRef.value = editorInstance
  validateConfiguration()
  const bind = () => {
    languageSession?.dispose()
    const model = editorInstance.getModel()
    if (monacoApi && model && documentKind.value === 'peel') {
      languageSession = bindPeelModel(
        monacoApi,
        model,
        () => ({
          projectId: projectStore.activeProject?.projectIdApi?.id ?? '',
          expectedBindings: files.expectedBindings(documentId.value),
        }),
        (available) => {
          analysisAvailable.value = available
        },
      )
    }
  }
  modelListener = editorInstance.onDidChangeModel(bind)
  bind()
}
watch(
  () => [files.expectedBindings(documentId.value), projectStore.activeProject?.projectIdApi?.id],
  () => languageSession?.refresh(),
  { deep: true, flush: 'sync' },
)
onUnmounted(() => {
  languageSession?.dispose()
  modelListener?.dispose()
})

// 5. Monaco configuration options
const editorOptions = computed(() => ({
  readOnly: projectStore.isReadOnly,
  // CRITICAL FOR DOCKVIEW: Tells Monaco to observe container size changes
  // and auto-resize when panel splitters are dragged
  automaticLayout: true,

  theme: 'peel-dark',
  'semanticHighlighting.enabled': true,
  fontSize: 14,
  fontFamily: "'Fira Code', 'Cascadia Code', Consolas, monospace",
  minimap: { enabled: false },
  padding: { top: 16, bottom: 16 },
  scrollBeyondLastLine: false,
  tabSize: 4,
  smoothScrolling: true,
  cursorBlinking: 'smooth',
}))

// 6. Notify parent or handle content updates
watch(code, (newVal) => {
  validateConfiguration()
  if (!panelId.value) {
    return
  }

  draftStore.setDraftByReference(panelId.value, newVal, documentId.value || undefined)
})
</script>

<template>
  <div class="editor-container" @keydown.capture="toolbar?.shortcut($event)">
    <FileToolbar
      ref="toolbar"
      :document-id="documentId"
      :document-kind="documentKind"
      :panel-id="panelId"
      :initial-content="initialContent"
      :settings-open="settingsOpen"
      @settings="settingsOpen = !settingsOpen"
    />
    <div class="editor-body">
      <FileSettings v-if="settingsOpen" :document-id="documentId" @close="settingsOpen = false">
        <ScriptBindings v-if="documentKind === 'peel'" :document-id="documentId" />
      </FileSettings>
      <VueMonacoEditor
        v-model:value="code"
        :language="language"
        :path="modelPath"
        :options="editorOptions"
        @before-mount="beforeMount"
        @mount="handleMount"
        class="monaco-editor-instance"
      />
    </div>
    <p v-if="documentKind === 'peel' && !analysisAvailable" class="analysis-status" role="status">
      {{ t('analysisUnavailable') }}
    </p>
  </div>
</template>

<style scoped>
.editor-container {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  background-color: var(--peel-background);
}

.editor-body {
  position: relative;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}
.monaco-editor-instance {
  width: 100%;
  height: 100%;
}
.analysis-status {
  margin: 0;
  padding: 4px 12px;
  color: var(--peel-muted);
  font-size: 12px;
}
</style>
