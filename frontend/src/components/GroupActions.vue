<!-- GroupActions.vue -->
<script setup lang="ts">
import type { IDockviewHeaderActionsProps } from 'dockview-vue'
import { computed, ref } from 'vue'
import { api } from '@/adapter/client'
import { useEditorDraftStore } from '@/stores/editorDrafts'
import { useRunBindingsStore } from '@/stores/runBindings'
import { useRunOutputStore } from '@/stores/runOutput'
import { useWorkspaceSelectionStore } from '@/stores/workspaceSelection'

// Dockview passes a `params` prop to header action components
const props = defineProps<{
  params: IDockviewHeaderActionsProps
}>()

const showActions = computed(() => props.params.activePanel?.id.startsWith('editor-') ?? false)
const isSaving = ref(false)
const isRunning = ref(false)

const draftStore = useEditorDraftStore()
const bindingsStore = useRunBindingsStore()
const runOutputStore = useRunOutputStore()
const workspaceSelectionStore = useWorkspaceSelectionStore()

type EditorPanelParams = {
  filename?: string
  content?: string
  documentId?: string
  params?: {
    filename?: string
    content?: string
    documentId?: string
  }
}

function getActiveEditorContext(): {
  panelId: string
  name: string
  documentId?: string
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

  const name = nestedParams.filename || rawParams.filename || persistedPanel?.title || 'Untitled'
  const documentId = nestedParams.documentId || rawParams.documentId
  const initialContent = nestedParams.content || rawParams.content || ''

  return {
    panelId,
    name,
    documentId,
    initialContent,
  }
}

// Perform actions using the group API or container API
const handleSave = async () => {
  const context = getActiveEditorContext()
  if (!context || isSaving.value) {
    return
  }

  isSaving.value = true

  try {
    const latestDraft = draftStore.getDraftByReference(context.panelId, context.documentId)
    const script = latestDraft ?? context.initialContent
    const isNewScript = !context.documentId
    const payload = {
      id: context.documentId,
      name: context.name,
      script,
    }

    const { data, error } = await api.POST('/scripts', {
      body: payload,
    })

    if (error) {
      console.error('Save failed:', error)
      return
    }

    const savedId = data?.id ?? context.documentId
    const savedName = data?.name ?? context.name

    if (savedId) {
      draftStore.bindPanelToDocument(context.panelId, savedId)
      draftStore.markSavedByReference(context.panelId, script, savedId)
      if (isNewScript) {
        workspaceSelectionStore.notifyScriptsChanged()
      }
    }

    if (savedName && props.params.activePanel) {
      props.params.activePanel.api.setTitle(savedName)
    }
  } catch (error) {
    console.error('Unexpected save error:', error)
  } finally {
    isSaving.value = false
  }
}

const handleRun = async () => {
  const context = getActiveEditorContext()
  if (!context || isRunning.value) {
    return
  }

  const latestDraft = draftStore.getDraftByReference(context.panelId, context.documentId)
  const script = latestDraft ?? context.initialContent

  if (!script) {
    runOutputStore.setRunError('No script content available to run.')
    return
  }

  const bindings = bindingsStore.parseBindings()
  if (!bindings) {
    const message = bindingsStore.parseError || 'Invalid bindings JSON.'
    runOutputStore.setRunError(message)
    const outputPanel = props.params.containerApi.getPanel('output-console')
    outputPanel?.api.setActive()
    return
  }

  isRunning.value = true
  runOutputStore.startRun({
    panelId: context.panelId,
    name: context.name,
    script,
    bindings,
  })

  try {
    const { data, error } = await api.POST('/run', {
      body: {
        script,
        bindings,
      },
    })

    if (error) {
      const message = (error as { message?: string })?.message || 'Run request failed.'
      runOutputStore.setRunError(message)
      return
    }

    runOutputStore.setRunSuccess({
      trace: data?.trace,
      result: data?.result,
    })

    const outputPanel = props.params.containerApi.getPanel('output-console')
    outputPanel?.api.setActive()
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Unexpected run error.'
    runOutputStore.setRunError(message)
  } finally {
    isRunning.value = false
  }
}
</script>

<template>
  <div v-if="showActions" class="group-header-actions">
    <button class="action-btn" :disabled="isSaving" title="Save file" @click="handleSave">💾</button>
    <button class="action-btn" :disabled="isRunning" title="Run script" @click="handleRun">▶️</button>
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
</style>
