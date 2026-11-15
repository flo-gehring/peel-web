<script setup lang="ts">
import { t } from '@/i18n'
import { computed, ref, watch } from 'vue'
import { Eye, Play, Save, Settings, PanelBottom } from '@lucide/vue'
import { api } from '@/adapter/client'
import { apiErrorMessage } from '@/adapter/apiError'
import { renderConfigurationSchema } from '@/adapter/renderConfigurationSchema'
import { useProjectFileStore, type FileContent } from '@/stores/projectFiles'
import { useEditorDraftStore } from '@/stores/editorDrafts'
import { useDocumentDraftStore } from '@/stores/documentDrafts'
import { useProjectStore } from '@/stores/projects'
import { useRunBindingsStore } from '@/stores/runBindings'
import { useRunOutputStore } from '@/stores/runOutput'
import { useWorkspaceSelectionStore } from '@/stores/workspaceSelection'

const props = withDefaults(
  defineProps<{
    documentId: string
    documentKind: 'peel' | 'renderConfig' | 'document'
    panelId?: string
    initialContent?: string
    settingsOpen?: boolean
    previewing?: boolean
    actionError?: string | null
  }>(),
  { initialContent: '', panelId: '', actionError: null },
)
const emit = defineEmits<{ settings: []; preview: [] }>()
const files = useProjectFileStore()
const drafts = useEditorDraftStore()
const documents = useDocumentDraftStore()
const projects = useProjectStore()
const bindings = useRunBindingsStore()
const output = useRunOutputStore()
const selection = useWorkspaceSelectionStore()
const saving = ref(false)
const saveError = ref<string | null>(null)
const dirty = computed(
  () =>
    files.metadataDirty(props.documentId) ||
    (props.documentKind === 'document'
      ? documents.getDraft(props.documentId)?.dirty
      : drafts.draftsById[props.documentId]?.dirty),
)
const status = computed(() =>
  projects.isReadOnly
    ? t('readOnly')
    : saving.value
      ? t('saving')
      : saveError.value
        ? t('saveFailed')
        : dirty.value
          ? t('unsavedChanges')
          : t('saved'),
)

async function save(): Promise<boolean> {
  if (projects.isReadOnly || saving.value) return false
  const id = props.documentId
  const projectId = projects.activeProject?.projectIdApi?.id
  const metadata = { ...files.metadata(id) }
  const document = documents.getDraft(id)
  const text = drafts.getDraft(id) ?? props.initialContent
  saving.value = true
  saveError.value = null
  try {
    if (!projectId || !files.loaded[id]) throw new Error(t('reopenTheFileBeforeSaving'))
    let content: FileContent
    if (props.documentKind === 'document') {
      if (!document) throw new Error(t('theDocumentDraftIsUnavailable'))
      content = {
        type: 'document',
        document: document.document,
        renderConfigurationId: document.renderConfigurationId,
      }
    } else if (props.documentKind === 'renderConfig') {
      content = { type: 'render-config', ...renderConfigurationSchema.parse(JSON.parse(text)) }
    } else content = { type: 'script', source: text }
    await files.save(projectId, id, { ...metadata, content })
    if (props.documentKind === 'document') {
      if (documents.getDraft(id) === document) documents.markSaved(id)
    } else if (drafts.getDraft(id) === text) drafts.markSaved(id, text)
    return true
  } catch (error) {
    saveError.value = error instanceof Error ? error.message : t('theFileCouldNotBeSaved')
    return false
  } finally {
    saving.value = false
  }
}

async function execute(): Promise<void> {
  if (projects.isReadOnly) return
  if (props.documentKind === 'document') {
    if (!props.previewing) emit('preview')
    return
  }
  if (props.documentKind !== 'peel' || output.isRunning) return
  const id = props.documentId
  const projectId = projects.activeProject?.projectIdApi?.id
  const script = drafts.getDraft(id) ?? props.initialContent
  const parsedBindings = bindings.parseBindings()
  const name = files.metadata(id).name || t('script')
  selection.requestOutput()
  if (!parsedBindings || !projectId || !script) {
    output.startRun({ panelId: props.panelId, name, script, bindings: parsedBindings ?? {} })
    output.setRunError(
      bindings.parseError ||
        (!projectId ? t('openAnActiveProject') : t('noScriptContentAvailableToRun')),
    )
    return
  }
  const requestId = output.startRun({
    panelId: props.panelId,
    name,
    script,
    bindings: parsedBindings,
  })
  try {
    const { data, error } = await api.PUT('/api/scripts/run', {
      body: { projectId: { id: projectId }, script, bindings: parsedBindings },
    })
    if (error) output.setRunError(apiErrorMessage(error, t('runRequestFailed')), requestId)
    else output.setRunSuccess({ trace: data?.trace, result: data?.result }, requestId)
  } catch (error) {
    output.setRunError(error instanceof Error ? error.message : t('unexpectedRunError'), requestId)
  }
}

function shortcut(event: KeyboardEvent): void {
  if (!(event.ctrlKey || event.metaKey) || event.altKey || event.isComposing) return
  if (event.key.toLowerCase() === 's') {
    event.preventDefault()
    event.stopPropagation()
    void save()
  } else if (event.key === 'Enter' && props.documentKind !== 'renderConfig') {
    event.preventDefault()
    event.stopPropagation()
    void execute()
  }
}
defineExpose({ shortcut, save, execute })
watch(
  () => selection.actionVersion,
  () => {
    if (selection.fileAction?.id !== props.documentId) return
    if (selection.fileAction.action === 'save') void save()
    else void execute()
  },
)
</script>

<template>
  <div class="file-toolbar-shell">
    <div class="file-toolbar" role="toolbar" :aria-label="t('fileActions')">
      <span
        class="save-status"
        :class="{ dirty, failed: !!saveError }"
        role="status"
        :title="status"
        >{{ status }}</span
      >
      <div class="toolbar-actions">
        <slot name="views" />
        <slot name="configuration" />
        <button
          class="icon-button"
          :title="t('showOutput')"
          :aria-label="t('showOutput')"
          @click="selection.requestOutput"
        >
          <PanelBottom :size="16" aria-hidden="true" />
        </button>
        <button
          class="icon-button"
          :title="t('fileSettings')"
          :aria-label="t('fileSettings')"
          :aria-expanded="settingsOpen"
          :aria-controls="`file-settings-${documentId}`"
          @click="emit('settings')"
        >
          <Settings :size="16" aria-hidden="true" />
        </button>
        <button
          :title="t('saveFile')"
          :aria-label="t('saveFile')"
          class="save-button"
          :disabled="saving || projects.isReadOnly"
          @click="save"
        >
          <Save :size="14" aria-hidden="true" /><span>{{ t('save') }}</span>
        </button>
        <button
          v-if="documentKind !== 'renderConfig'"
          :title="documentKind === 'document' ? t('renderPreview') : t('runScript')"
          :aria-label="documentKind === 'document' ? t('renderPreview') : t('runScript')"
          class="primary-action"
          :disabled="
            projects.isReadOnly || (documentKind === 'document' ? previewing : output.isRunning)
          "
          @click="execute"
        >
          <Eye v-if="documentKind === 'document'" :size="16" aria-hidden="true" /><Play
            v-else
            :size="16"
            aria-hidden="true"
          /><span class="sr-only">{{
            documentKind === 'document'
              ? previewing
                ? t('rendering')
                : t('preview')
              : output.isRunning
                ? t('running')
                : t('run')
          }}</span>
        </button>
      </div>
    </div>
    <p v-if="saveError || actionError" role="alert" class="action-error">
      {{ saveError || actionError }}
    </p>
  </div>
</template>

<style scoped>
.file-toolbar-shell {
  container-type: inline-size;
  flex-shrink: 0;
  border-bottom: 1px solid var(--peel-border);
  background: var(--peel-surface);
}
.file-toolbar {
  min-height: 42px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 10px;
}
.save-status {
  margin-right: auto;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--peel-muted);
  font-size: 11px;
}
.save-status.dirty {
  color: var(--peel-accent);
}
.save-status.failed {
  color: var(--peel-error);
}
.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}
button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  height: 28px;
  padding: 0 9px;
  border: 1px solid var(--peel-border);
  border-radius: 6px;
  background: var(--peel-raised);
  color: var(--peel-text);
  font-size: 12px;
  cursor: pointer;
}
button.icon-button {
  padding: 0;
  width: 28px;
  border-color: transparent;
  background: transparent;
}
button:hover:not(:disabled) {
  background: var(--peel-border);
}
button.primary-action {
  background: var(--peel-accent);
  border-color: var(--peel-accent);
  color: #221d0c;
  font-weight: 650;
}
button.primary-action:hover:not(:disabled) {
  filter: brightness(1.08);
}
button:disabled {
  opacity: 0.5;
  cursor: default;
}
button:focus-visible {
  outline: 2px solid var(--peel-accent);
  outline-offset: 2px;
}
.action-error {
  margin: 0;
  padding: 8px 12px;
  color: var(--peel-error);
  font-size: 12px;
  white-space: pre-wrap;
  max-height: 100px;
  overflow: auto;
}
button.primary-action {
  width: 28px;
  padding: 0;
}
@container (max-width: 420px) {
  .save-button span {
    display: none;
  }
  button.save-button {
    width: 28px;
    padding: 0;
  }
}
@container (max-width: 320px) {
  .save-status {
    position: absolute;
    width: 1px;
    height: 1px;
    clip-path: inset(50%);
    overflow: hidden;
  }
  .toolbar-actions {
    margin-left: auto;
    gap: 4px;
  }
  .file-toolbar {
    padding-inline: 5px;
  }
}
</style>
