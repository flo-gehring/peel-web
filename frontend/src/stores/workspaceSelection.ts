import { ref } from 'vue'
import { defineStore } from 'pinia'
import type { PeelWorkspaceDocument } from '@/adapter/ClientTypeDefinition'

export const useWorkspaceSelectionStore = defineStore('workspaceSelection', () => {
  const selectedDocument = ref<PeelWorkspaceDocument | null>(null)
  const selectionVersion = ref(0)
  const deletedDocument = ref<PeelWorkspaceDocument | null>(null)
  const deletionVersion = ref(0)
  const reloadedDocument = ref<PeelWorkspaceDocument | null>(null)
  const reloadVersion = ref(0)

  function reloadDocument(document: PeelWorkspaceDocument): void {
    reloadedDocument.value = document
    reloadVersion.value += 1
  }

  function selectDocument(document: PeelWorkspaceDocument): void {
    selectedDocument.value = document
    selectionVersion.value += 1
  }

  function clearSelection(id: string): void {
    if (selectedDocument.value?.id === id) {
      selectedDocument.value = null
    }
  }

  function deleteDocument(document: PeelWorkspaceDocument): void {
    clearSelection(document.id)
    deletedDocument.value = document
    deletionVersion.value += 1
  }

  function clear(): void {
    selectedDocument.value = null
    deletedDocument.value = null
    reloadedDocument.value = null
  }

  return {
    selectedDocument,
    selectionVersion,
    deletedDocument,
    deletionVersion,
    reloadedDocument,
    reloadVersion,
    reloadDocument,
    selectDocument,
    clearSelection,
    deleteDocument,
    clear,
  }
})
