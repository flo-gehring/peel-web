<script setup lang="ts">
import type { IDockviewHeaderActionsProps } from 'dockview-vue'
import { computed, ref } from 'vue'
import { api } from '@/adapter/client'
import { renderConfigurationSchema } from '@/adapter/renderConfigurationSchema'
import { useEditorDraftStore } from '@/stores/editorDrafts'
import { useRunBindingsStore } from '@/stores/runBindings'
import { useRunOutputStore } from '@/stores/runOutput'

const props = defineProps<{
  params: IDockviewHeaderActionsProps
}>()

const showActions = computed(() => props.params.activePanel?.id.startsWith('editor-') ?? false)
const isSaving = ref(false)
const isRunning = ref(false)
const saveError = ref<string | null>(null)

const draftStore = useEditorDraftStore()
const bindingsStore = useRunBindingsStore()
const runOutputStore = useRunOutputStore()

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
  const context = getActiveEditorContext()
  if (!context || isSaving.value) {
    return
  }

  isSaving.value = true
  saveError.value = null
  try {
    const content =
      draftStore.getDraftByReference(context.panelId, context.documentId) ?? context.initialContent

    if (context.documentKind === 'renderConfig') {
      if (!context.documentId) {
        showSaveError('This render configuration has no document ID and cannot be saved.')
        return
      }

      let parsedJson: unknown
      try {
        parsedJson = JSON.parse(content)
      } catch (error) {
        const message = error instanceof Error ? error.message : 'Invalid JSON.'
        showSaveError(`Render configuration must contain valid JSON. ${message}`)
        return
      }

      const parsedConfig = renderConfigurationSchema.safeParse(parsedJson)
      if (!parsedConfig.success) {
        const issues = parsedConfig.error.issues
          .map((issue) => `${issue.path.join('.') || 'configuration'}: ${issue.message}`)
          .join('\n')
        showSaveError(`Render configuration is invalid.\n${issues}`)
        return
      }

      const { error } = await api.PUT('/render-config/update/{id}', {
        params: { path: { id: context.documentId } },
        body: {
          name: context.name,
          renderConfigurationDto: parsedConfig.data,
        },
      })
      if (error) {
        showSaveError('The render configuration could not be saved. Please try again.')
        return
      }

      draftStore.markSavedByReference(context.panelId, content, context.documentId)
      return
    }

    const { data, error } = await api.POST('/scripts', {
      body: { id: context.documentId, name: context.name, script: content },
    })
    if (error) {
      showSaveError('The script could not be saved. Please try again.')
      return
    }

    const savedId = data?.id ?? context.documentId
    const savedName = data?.name ?? context.name
    if (savedId) {
      draftStore.bindPanelToDocument(context.panelId, savedId)
      draftStore.markSavedByReference(context.panelId, content, savedId)
    }
    if (savedName && props.params.activePanel) {
      props.params.activePanel.api.setTitle(savedName)
    }
  } catch (error) {
    console.error('Unexpected save error:', error)
    showSaveError('An unexpected error occurred while saving.')
  } finally {
    isSaving.value = false
  }
}

async function handleRun() {
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
    const { data, error } = await api.POST('/run', { body: { script, bindings } })
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
    <button class="action-btn" :disabled="isSaving" title="Save file" @click="handleSave">Save</button>
    <button
      v-if="activeDocumentKind === 'peel'"
      class="action-btn"
      :disabled="isRunning"
      title="Run script"
      @click="handleRun"
    >
      Run
    </button>
    <div v-if="saveError" class="save-error-backdrop" @click.self="saveError = null">
      <section class="save-error-dialog" role="alertdialog" aria-modal="true" aria-labelledby="save-error-title">
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
  gap: 4px;
  padding: 0 4px;
}

.action-btn {
  background: transparent;
  border: none;
  cursor: pointer;
  color: inherit;
  font-size: 12px;
  padding: 2px 4px;
  border-radius: 3px;
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
