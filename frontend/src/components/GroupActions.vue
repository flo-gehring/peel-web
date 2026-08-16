<!-- GroupActions.vue -->
<script setup lang="ts">
import type { IDockviewHeaderActionsProps } from 'dockview-vue'
import { computed, ref } from 'vue'
import { api } from '@/adapter/client'
import { useEditorDraftStore } from '@/stores/editorDrafts'

// Dockview passes a `params` prop to header action components
const props = defineProps<{
  params: IDockviewHeaderActionsProps
}>()

const showActions = computed(() => props.params.activePanel?.id.startsWith('editor-') ?? false)
const isSaving = ref(false)

const draftStore = useEditorDraftStore()

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

const handleCustomAction = () => {
  console.log('Active Panel in Group:', props.params.activePanel?.id)
  console.log('Group ID:', props.params.group.id)
}
</script>

<template>
  <div v-if="showActions" class="group-header-actions">
    <button class="action-btn" :disabled="isSaving" title="Save file" @click="handleSave">💾</button>
    <button class="action-btn" title="Run Action" @click="handleCustomAction">▶️</button>
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
