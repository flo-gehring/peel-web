<script setup lang="ts">
import { t } from '@/i18n'
import { computed, ref, watch } from 'vue'
import { documentFromEditor, emptyDocument } from '@/adapter/documentModel'
import { peelNodes } from './document/peelNodes'
import { EditorContent, useEditor } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import { Banana, Palette, Printer } from '@lucide/vue'
import FileToolbar from './FileToolbar.vue'
import CreatePrintJobDialog from './CreatePrintJobDialog.vue'
import { useRouter } from 'vue-router'
import FileSettings from './FileSettings.vue'
import RenderPreviewPanel from './RenderPreviewPanel.vue'
import { apiErrorMessage } from '@/adapter/apiError'
import { useProjectFileStore } from '@/stores/projectFiles'
import { api } from '@/adapter/client'
import type { DocumentDraft } from '@/stores/documentDrafts'
import { useDocumentDraftStore } from '@/stores/documentDrafts'
import { useDocumentPreviewStore } from '@/stores/documentPreview'
import { useRunBindingsStore } from '@/stores/runBindings'
import { useProjectStore } from '@/stores/projects'

const props = defineProps<{
  params?: {
    documentId?: string
    params?: {
      documentId?: string
      draft?: DocumentDraft
    }
  }
}>()

const documentId = props.params?.params?.documentId || props.params?.documentId || ''
const initialDraft = props.params?.params?.draft
const draftStore = useDocumentDraftStore()
const previewStore = useDocumentPreviewStore()
const bindingsStore = useRunBindingsStore()
const projectStore = useProjectStore()
const files = useProjectFileStore()
const draft = computed(() => draftStore.getDraft(documentId) ?? initialDraft)
const toolbar = ref<InstanceType<typeof FileToolbar> | null>(null)
const settingsOpen = ref(false)
const router = useRouter()
const printJobDialog = ref(false)
const printJobNeedsSave = computed(() => !!draft.value?.dirty || files.metadataDirty(documentId))
async function preparePrintJob(): Promise<void> {
  if (projectStore.isReadOnly) throw new Error(t('theProjectIsReadOnly'))
  if (printJobNeedsSave.value && !(await toolbar.value?.save()))
    throw new Error(t('theDocumentTemplateCouldNotBeSavedCheckThe'))
  if (printJobNeedsSave.value) throw new Error(t('theTemplateChangedWhileSavingPleaseSaveAgain'))
}
function printJobCreated(id: string): void {
  printJobDialog.value = false
  void router.push({
    name: 'print-job-detail',
    params: { projectId: projectStore.activeProject?.projectIdApi?.id, id },
  })
}
const setupOpen = ref<'scripts' | 'renderConfig' | null>(null)
function toggleSetup(section: 'scripts' | 'renderConfig'): void {
  settingsOpen.value = false
  setupOpen.value = setupOpen.value === section ? null : section
}
function toggleSettings(): void {
  setupOpen.value = null
  settingsOpen.value = !settingsOpen.value
}
const view = ref<'edit' | 'preview'>('edit')
const saveError = ref<string | null>(null)
const preview = computed(() => previewStore.previews[documentId])
const isPreviewing = computed(() => !!preview.value?.isLoading)
const signature = computed(() =>
  JSON.stringify({
    document: draft.value?.document,
    config: draft.value?.renderConfigurationId,
    bindings: bindingsStore.rawJson,
  }),
)

const editor = useEditor({
  editable: !projectStore.isReadOnly,
  extensions: [StarterKit, ...peelNodes],
  content: draft.value?.document ?? emptyDocument(),
  onUpdate: ({ editor }) => {
    draftStore.updateDraft(documentId, {
      document: documentFromEditor(editor.getJSON()),
    })
  },
})
watch(
  [editor, () => projectStore.isReadOnly],
  ([instance, readOnly]) => instance?.setEditable(!readOnly, false),
  { immediate: true },
)

function toggleHeading(level: 1 | 2 | 3): void {
  editor.value?.chain().focus().toggleHeading({ level }).run()
}

function toggleBold(): void {
  editor.value?.chain().focus().toggleBold().run()
}

function toggleItalic(): void {
  editor.value?.chain().focus().toggleItalic().run()
}

function toggleBulletList(): void {
  editor.value?.chain().focus().toggleBulletList().run()
}

function toggleOrderedList(): void {
  editor.value?.chain().focus().toggleOrderedList().run()
}

function toggleBlockquote(): void {
  editor.value?.chain().focus().toggleBlockquote().run()
}

function insertTraceBlock(type: 'peelCalculation' | 'peelRepeat' | 'peelValue'): void {
  const instance = editor.value
  if (!instance || projectStore.isReadOnly) return
  const relative = instance.isActive('peelRepeat')
  const attrs = {
    scriptId: relative ? '' : (projectStore.activeProject?.scripts?.[0]?.id ?? ''),
    block: '',
    scope: relative ? 'current' : 'script',
    mode: 'detailed',
  }
  const content =
    type === 'peelRepeat'
      ? [
          { type: 'paragraph' },
          { type: 'peelCalculation', attrs: { scope: 'current', mode: 'detailed' } },
        ]
      : undefined
  const inserted = instance
    .chain()
    .focus()
    .insertContent({ type, attrs, ...(content ? { content } : {}) })
    .run()
  if (inserted) setupOpen.value = null
}

async function previewDocument(): Promise<void> {
  if (projectStore.isReadOnly) return
  const current = draft.value
  if (!current || isPreviewing.value) return

  const projectId = projectStore.activeProject?.projectIdApi?.id
  if (!projectId) {
    saveError.value = t('openAProjectBeforePreviewingADocument')
    return
  }

  const bindings = bindingsStore.parseBindings()
  if (!bindings) {
    saveError.value = bindingsStore.parseError || t('invalidBindingsJson')
    return
  }

  view.value = 'preview'
  saveError.value = null
  const requestId = previewStore.start(
    documentId,
    files.metadata(documentId).name || current.name,
    signature.value,
  )
  try {
    const { data, error } = await api.POST('/api/documents/preview', {
      body: {
        projectId: { id: projectId },
        document: current.document,
        bindings,
        renderConfigId: current.renderConfigurationId,
      },
    })
    if (error) {
      previewStore.setError(
        documentId,
        requestId,
        apiErrorMessage(error, t('theDocumentCouldNotBeRenderedCheckScriptLinks')),
      )
    } else {
      if (!data?.pdfData) {
        previewStore.setError(documentId, requestId, t('theDocumentPreviewDidNotContainAPdf'))
      } else {
        previewStore.setPreview(documentId, requestId, data.pdfData, data.warnings ?? [])
      }
    }
  } catch {
    previewStore.setError(
      documentId,
      requestId,
      t('anUnexpectedErrorOccurredWhileRenderingTheDocument'),
    )
  }
}
</script>

<template>
  <div class="document-panel" @keydown.capture="toolbar?.shortcut($event)">
    <FileToolbar
      ref="toolbar"
      :document-id="documentId"
      document-kind="document"
      :settings-open="settingsOpen"
      :previewing="isPreviewing"
      :action-error="saveError"
      @settings="toggleSettings"
      @preview="previewDocument"
    >
      <template #views
        ><div class="view-switch" role="group" :aria-label="t('documentView')">
          <button :aria-pressed="view === 'edit'" @click="view = 'edit'">{{ t('edit') }}</button
          ><button :aria-pressed="view === 'preview'" @click="view = 'preview'">
            {{ t('preview') }}
          </button>
        </div></template
      >
      <template #configuration>
        <div class="setup-actions">
          <button
            :title="t('scriptLinks')"
            :aria-label="t('scriptLinks')"
            :aria-expanded="setupOpen === 'scripts'"
            :aria-controls="`document-setup-${documentId}`"
            @click="toggleSetup('scripts')"
          >
            <Banana :size="16" aria-hidden="true" />
          </button>
          <button
            :title="t('renderConfiguration')"
            :aria-label="t('renderConfiguration')"
            :aria-expanded="setupOpen === 'renderConfig'"
            :aria-controls="`document-setup-${documentId}`"
            @click="toggleSetup('renderConfig')"
          >
            <Palette :size="16" aria-hidden="true" />
          </button>
          <button
            :title="t('createPrintJob')"
            :aria-label="t('createPrintJob')"
            :disabled="projectStore.isReadOnly"
            @click="printJobDialog = true"
          >
            <Printer :size="16" aria-hidden="true" />
          </button>
        </div>
      </template>
    </FileToolbar>
    <CreatePrintJobDialog
      v-if="printJobDialog && projectStore.activeProject?.projectIdApi?.id"
      :project-id="projectStore.activeProject.projectIdApi.id"
      :document-id="documentId"
      :document-name="files.metadata(documentId).name || draft?.name || ''"
      :save-required="printJobNeedsSave"
      :prepare="preparePrintJob"
      @cancel="printJobDialog = false"
      @created="printJobCreated"
    />
    <div class="document-body">
      <FileSettings v-if="settingsOpen" :document-id="documentId" @close="settingsOpen = false" />
      <FileSettings
        v-if="setupOpen"
        :document-id="documentId"
        :panel-id="`document-setup-${documentId}`"
        :title="setupOpen === 'scripts' ? t('scriptLinks') : t('renderConfiguration')"
        :show-details="false"
        @close="setupOpen = null"
      >
        <div v-if="draft" class="document-links">
          <div v-if="setupOpen === 'scripts'" class="trace-insert">
            <p>{{ t('traceInsertHint') }}</p>
            <button
              :disabled="projectStore.isReadOnly"
              @click="insertTraceBlock('peelCalculation')"
            >
              {{ t('traceCalculation') }}
            </button>
            <button :disabled="projectStore.isReadOnly" @click="insertTraceBlock('peelRepeat')">
              {{ t('traceRepeat') }}
            </button>
            <button :disabled="projectStore.isReadOnly" @click="insertTraceBlock('peelValue')">
              {{ t('traceValue') }}
            </button>
            <p>{{ t('traceSavedScripts') }}</p>
          </div>
          <label v-else class="render-config-field"
            >{{ t('renderConfig')
            }}<select
              :value="draft.renderConfigurationId"
              @change="
                draftStore.updateDraft(documentId, {
                  renderConfigurationId: ($event.target as HTMLSelectElement).value,
                })
              "
            >
              <option value="">{{ t('standardRendering') }}</option>
              <option
                v-for="config in draft.renderConfigurations"
                :key="config.id"
                :value="config.id"
              >
                {{ files.loaded[config.id]?.name ?? config.name }}
              </option>
            </select>
          </label>
        </div>
      </FileSettings>
      <div v-show="view === 'edit'" class="document-edit">
        <div
          class="editor-toolbar"
          :inert="projectStore.isReadOnly"
          role="toolbar"
          :aria-label="t('documentFormatting')"
        >
          <button
            :class="{ active: editor?.isActive('paragraph') }"
            :title="t('paragraph')"
            @click="editor?.chain().focus().setParagraph().run()"
          >
            P
          </button>
          <button
            :class="{ active: editor?.isActive('heading', { level: 1 }) }"
            :title="t('heading1')"
            @click="toggleHeading(1)"
          >
            H1
          </button>
          <button
            :class="{ active: editor?.isActive('heading', { level: 2 }) }"
            :title="t('heading2')"
            @click="toggleHeading(2)"
          >
            H2
          </button>
          <span class="toolbar-divider"></span>
          <button
            :class="{ active: editor?.isActive('bold') }"
            :title="t('bold')"
            @click="toggleBold"
          >
            <strong>B</strong>
          </button>
          <button
            :class="{ active: editor?.isActive('italic') }"
            :title="t('italic')"
            @click="toggleItalic"
          >
            <em>I</em>
          </button>
          <button
            :class="{ active: editor?.isActive('strike') }"
            :title="t('strikethrough')"
            @click="editor?.chain().focus().toggleStrike().run()"
          >
            <s>S</s>
          </button>
          <span class="toolbar-divider"></span>
          <button
            :class="{ active: editor?.isActive('bulletList') }"
            :title="t('bulletList')"
            @click="toggleBulletList"
          >
            {{ t('list') }}
          </button>
          <button
            :class="{ active: editor?.isActive('orderedList') }"
            :title="t('numberedList')"
            @click="toggleOrderedList"
          >
            1.
          </button>
          <button
            :class="{ active: editor?.isActive('blockquote') }"
            :title="t('quote')"
            @click="toggleBlockquote"
          >
            {{ t('quote') }}
          </button>
          <span class="toolbar-divider"></span>
          <button
            :title="t('undo')"
            :disabled="!editor?.can().undo()"
            @click="editor?.chain().focus().undo().run()"
          >
            {{ t('undo') }}
          </button>
          <button
            :title="t('redo')"
            :disabled="!editor?.can().redo()"
            @click="editor?.chain().focus().redo().run()"
          >
            {{ t('redo') }}
          </button>
        </div>
        <div class="document-canvas">
          <article class="a4-page">
            <EditorContent :editor="editor" class="tiptap-editor" />
          </article>
        </div>
      </div>
      <RenderPreviewPanel
        v-if="view === 'preview'"
        :document-id="documentId"
        :stale="preview?.signature !== signature"
      />
    </div>
  </div>
</template>

<style scoped>
.trace-insert {
  display: flex;
  flex-direction: column;
  gap: 10px;
  font-size: 12px;
}
.document-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  background: #17191c;
  color: #ccc;
}
.document-body {
  position: relative;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}
.document-edit {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}
.view-switch {
  display: flex;
  gap: 0;
  border: 1px solid var(--peel-border);
  border-radius: 6px;
  overflow: hidden;
}
.view-switch button {
  border: 0;
  padding: 4px 7px;
  font-size: 11px;
  background: transparent;
  color: var(--peel-muted);
}
.view-switch button[aria-pressed='true'] {
  color: var(--peel-text);
  background: var(--peel-raised);
}
.setup-actions {
  display: flex;
  gap: 4px;
}
.setup-actions button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  padding: 0;
  border: 0;
  border-radius: 6px;
  color: var(--peel-muted);
  background: transparent;
}
.setup-actions button:hover,
.setup-actions button[aria-expanded='true'] {
  color: var(--peel-text);
  background: var(--peel-raised);
}
.setup-actions button:focus-visible {
  outline: 2px solid var(--peel-accent);
  outline-offset: 2px;
}
.document-links {
  display: grid;
  gap: 8px;
  padding: 12px 0;
  border-top: 1px solid var(--peel-border);
}
.links-heading,
.script-link-row {
  display: flex;
  gap: 8px;
  align-items: center;
}
.script-link-row input,
.render-config-field select {
  flex: 1;
  min-width: 0;
  padding: 6px;
  border: 1px solid #3c3c3c;
  background: #252526;
  color: #fff;
}
.render-config-field {
  display: grid;
  gap: 4px;
}
button {
  width: fit-content;
  padding: 5px 10px;
  border: 1px solid #3c3c3c;
  background: #333;
  color: #fff;
  cursor: pointer;
}
button:disabled {
  cursor: default;
  opacity: 0.45;
}
.save-error {
  margin: 0;
  color: #f48771;
}
.document-actions {
  display: flex;
  gap: 8px;
}
.editor-toolbar {
  position: relative;
  z-index: 2;
  display: flex;
  flex-wrap: nowrap;
  overflow-x: auto;
  flex-shrink: 0;
  gap: 5px;
  align-items: center;
  padding: 8px 14px;
  border-bottom: 1px solid #34373c;
  background: #25272b;
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.22);
}
.editor-toolbar button {
  flex-shrink: 0;
  min-width: 31px;
  padding: 4px 7px;
  border-color: transparent;
  background: transparent;
  font-size: 12px;
}
.editor-toolbar button:hover,
.editor-toolbar button.active {
  background: #3d4148;
}
.toolbar-divider {
  width: 1px;
  height: 20px;
  margin: 0 3px;
  background: #4a4d52;
}
.document-canvas {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 24px 16px 60px;
  background: radial-gradient(circle at top, #30343a 0, #202328 52%, #17191c 100%);
}
.a4-page {
  width: min(794px, 100%);
  min-height: 1123px;
  box-sizing: border-box;
  margin: 0 auto;
  padding: 76px 76px 92px;
  background: #fbfbfa;
  color: #202124;
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.45);
}
.tiptap-editor :deep(.ProseMirror) {
  min-height: 955px;
  outline: none;
  font-family: Georgia, 'Times New Roman', serif;
  font-size: 16px;
  line-height: 1.65;
}
.tiptap-editor :deep(h1) {
  margin: 0 0 0.7em;
  font-size: 2.1em;
  line-height: 1.15;
}
.tiptap-editor :deep(h2) {
  margin: 1.2em 0 0.55em;
  font-size: 1.55em;
  line-height: 1.2;
}
.tiptap-editor :deep(h3) {
  margin: 1em 0 0.45em;
  font-size: 1.2em;
}
.tiptap-editor :deep(p) {
  margin: 0 0 0.9em;
}
.tiptap-editor :deep(ul),
.tiptap-editor :deep(ol) {
  padding-left: 1.5em;
  margin: 0 0 0.9em;
}
.tiptap-editor :deep(blockquote) {
  margin: 1em 0;
  padding-left: 1em;
  border-left: 3px solid #9aa4b1;
  color: #50545a;
}
@media (max-width: 700px) {
  .document-canvas {
    padding: 16px 0 40px;
  }
  .a4-page {
    min-height: calc(100vh - 250px);
    padding: 36px 24px 60px;
    box-shadow: none;
  }
}
</style>
