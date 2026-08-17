import { ref } from 'vue'
import { defineStore } from 'pinia'
import type { PeelWorkspaceDocument } from '@/adapter/ClientTypeDefinition'

export const useWorkspaceSelectionStore = defineStore('workspaceSelection', () => {
  const selectedDocument = ref<PeelWorkspaceDocument | null>(null)
  const selectionVersion = ref(0)
  const deletedScriptId = ref<string | null>(null)
  const deletionVersion = ref(0)

  function selectDocument(document: PeelWorkspaceDocument): void {
    selectedDocument.value = document
    selectionVersion.value += 1
  }

  function clearSelection(id: string): void {
    if (selectedDocument.value?.id === id) {
      selectedDocument.value = null
    }
  }

  function deleteScript(id: string): void {
    clearSelection(id)
    deletedScriptId.value = id
    deletionVersion.value += 1
  }

  return {
    selectedDocument,
    selectionVersion,
    deletedScriptId,
    deletionVersion,
    selectDocument,
    clearSelection,
    deleteScript,
  }
})
