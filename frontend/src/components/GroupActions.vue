<script setup lang="ts">
import type { IDockviewHeaderActionsProps } from 'dockview-vue'
import { computed, ref } from 'vue'
import { useProjectFileStore } from '@/stores/projectFiles'
import { api } from '@/adapter/client'
import { renderConfigurationSchema } from '@/adapter/renderConfigurationSchema'
import { useEditorDraftStore } from '@/stores/editorDrafts'
import { useRunBindingsStore } from '@/stores/runBindings'
import { useRunOutputStore } from '@/stores/runOutput'
import { useProjectStore } from '@/stores/projects'

const props = defineProps<{
  params: IDockviewHeaderActionsProps
}>()

const showActions = computed(() => {
  const panelId = props.params.activePanel?.id
  return panelId?.startsWith('editor-') && !panelId.startsWith('editor-document-')
})
const isSaving = ref(false)
const isRunning = ref(false)
const saveError = ref<string | null>(null)

const draftStore = useEditorDraftStore()
const bindingsStore = useRunBindingsStore()
const runOutputStore = useRunOutputStore()
const projectStore = useProjectStore()
const files = useProjectFileStore()

type EditorPanelParams = {
  filename?: string
  content?: string
  documentId?: string
  documentKind?: 'peel' | 'renderConfig'
  params?: {
    filename?: string
    content?: string
    documentId?: string
    documentKind?: 'peel' | 'renderConfig'
  }
}

function getActiveEditorContext(): {
  panelId: string
  name: string
  documentId?: string
  documentKind: 'peel' | 'renderConfig'
  initialContent: string
} | null {
  const activePanel = props.params.activePanel
  if (!activePanel) {
    return null
  }

  const panelId = activePanel.id
  const persistedPanel = props.params.containerApi.toJSON().panels[panelId]
  const rawParams = (persistedPanel?.params ?? {}) as EditorPanelParams
  const nestedParams = rawParams.params ?? {}

  return {
    panelId,
    name: nestedParams.filename || rawParams.filename || persistedPanel?.title || 'Untitled',
    documentId: nestedParams.documentId || rawParams.documentId,
    documentKind: nestedParams.documentKind || rawParams.documentKind || 'peel',
    initialContent: nestedParams.content || rawParams.content || '',
  }
}

const activeDocumentKind = computed(() => getActiveEditorContext()?.documentKind)

function showSaveError(message: string): void {
  saveError.value = message
}

async function handleSave() {
  if (projectStore.isReadOnly) return
  const context = getActiveEditorContext()
  if (!context || isSaving.value) {
    return
  }

  isSaving.value = true
  saveError.value = null
  try {
    const content =
      draftStore.getDraftByReference(context.panelId, context.documentId) ?? context.initialContent

    const projectId = projectStore.activeProject?.projectIdApi?.id
    if (!context.documentId || !projectId)
      throw new Error('Open a saved project file before saving.')
    const baseline = files.loaded[context.documentId]
    if (!baseline) throw new Error('Reopen the file before saving.')
    let fileContent
    if (context.documentKind === 'renderConfig') {
      const parsed = renderConfigurationSchema.safeParse(JSON.parse(content))
      if (!parsed.success) throw new Error(`Invalid render configuration: ${parsed.error.message}`)
      fileContent = { type: 'render-config' as const, ...parsed.data }
    } else {
      fileContent = { type: 'script' as const, source: content }
    }
    const saved = await files.save(projectId, context.documentId, {
      name: baseline.name ?? context.name,
      description: baseline.description ?? '',
      content: fileContent,
    })
    // Edits made while the request was in flight remain dirty.
    if (draftStore.getDraftByReference(context.panelId, context.documentId) === content)
      draftStore.markSavedByReference(context.panelId, content, context.documentId)
    props.params.activePanel?.api.setTitle(saved.name ?? context.name)
  } catch (error) {
    console.error('Unexpected save error:', error)
    showSaveError(error instanceof Error ? error.message : 'The file could not be saved.')
  } finally {
    isSaving.value = false
  }
}

async function handleRun() {
  if (projectStore.isReadOnly) return
  const context = getActiveEditorContext()
  if (!context || context.documentKind !== 'peel' || isRunning.value) {
    return
  }

  const script =
    draftStore.getDraftByReference(context.panelId, context.documentId) ?? context.initialContent
  if (!script) {
    runOutputStore.setRunError('No script content available to run.')
    return
  }

  const bindings = bindingsStore.parseBindings()
  if (!bindings) {
    runOutputStore.setRunError(bindingsStore.parseError || 'Invalid bindings JSON.')
    props.params.containerApi.getPanel('output-console')?.api.setActive()
    return
  }

  isRunning.value = true
  runOutputStore.startRun({ panelId: context.panelId, name: context.name, script, bindings })
  try {
    const projectId = projectStore.activeProject?.projectIdApi?.id
    if (!projectId || projectStore.isReadOnly) {
      runOutputStore.setRunError('Open an active project to run a script.')
      return
    }
    const { data, error } = await api.PUT('/api/scripts/run', {
      body: { projectId: { id: projectId }, script, bindings },
    })
    if (error) {
      runOutputStore.setRunError((error as { message?: string }).message || 'Run request failed.')
      return
    }

    runOutputStore.setRunSuccess({ trace: data?.trace, result: data?.result })
    props.params.containerApi.getPanel('output-console')?.api.setActive()
  } catch (error) {
    runOutputStore.setRunError(error instanceof Error ? error.message : 'Unexpected run error.')
  } finally {
    isRunning.value = false
  }
}
</script>

<template>
  <div v-if="showActions" class="group-header-actions">
    <button
      class="action-btn"
      :disabled="isSaving || projectStore.isReadOnly"
      title="Save file"
      @click="handleSave"
    >
      {{ isSaving ? 'Saving…' : 'Save' }}
    </button>
    <button
      v-if="activeDocumentKind === 'peel'"
      class="action-btn run-button"
      :disabled="isRunning || projectStore.isReadOnly"
      title="Run script"
      @click="handleRun"
    >
      {{ isRunning ? 'Running…' : '▶ Run' }}
    </button>
    <div v-if="saveError" class="save-error-backdrop" @click.self="saveError = null">
      <section
        class="save-error-dialog"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="save-error-title"
      >
        <h2 id="save-error-title">Cannot Save</h2>
        <p>{{ saveError }}</p>
        <button class="save-error-close" @click="saveError = null">Close</button>
      </section>
    </div>
  </div>
</template>

<style scoped>
.group-header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 10px;
}

.action-btn {
  background: var(--peel-raised);
  border: 1px solid var(--peel-border);
  cursor: pointer;
  color: inherit;
  font-size: 12px;
  padding: 5px 12px;
  border-radius: 8px;
}
.run-button {
  background: var(--peel-accent);
  border-color: var(--peel-accent);
  color: #221d0c;
  font-weight: 650;
}
.action-btn.run-button:hover {
  background: var(--peel-accent);
}

.action-btn:disabled {
  cursor: default;
  opacity: 0.5;
}

.action-btn:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.save-error-backdrop {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: grid;
  place-items: center;
  background: rgba(0, 0, 0, 0.45);
}

.save-error-dialog {
  width: min(440px, calc(100vw - 32px));
  padding: 16px;
  border: 1px solid #8c3b3b;
  border-radius: 6px;
  background: #252526;
  color: #f1f1f1;
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.45);
}

.save-error-dialog h2 {
  margin: 0 0 10px;
  color: #f48771;
  font-size: 15px;
}

.save-error-dialog p {
  margin: 0 0 16px;
  white-space: pre-wrap;
}

.save-error-close {
  float: right;
  padding: 5px 10px;
  border: 1px solid #3c3c3c;
  border-radius: 3px;
  background: #333333;
  color: #ffffff;
  cursor: pointer;
}
</style>
