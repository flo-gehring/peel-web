import { ref } from 'vue'
import { defineStore } from 'pinia'
import type { DocumentNode } from '@/adapter/documentModel'

export type DocumentDraft = {
  name: string
  document: DocumentNode
  renderConfigurationId: string
  renderConfigurations: Array<{ id: string; name: string }>
  dirty: boolean
}

export const useDocumentDraftStore = defineStore('documentDrafts', () => {
  const draftsById = ref<Record<string, DocumentDraft>>({})

  function getDraft(id: string): DocumentDraft | undefined {
    return draftsById.value[id]
  }

  function setDraft(id: string, draft: DocumentDraft): void {
    draftsById.value[id] = draft
  }

  function updateDraft(id: string, update: Partial<DocumentDraft>): void {
    const existing = draftsById.value[id]
    if (!existing) return
    draftsById.value[id] = { ...existing, ...update, dirty: true }
  }

  function markSaved(id: string): void {
    const existing = draftsById.value[id]
    if (existing) {
      draftsById.value[id] = { ...existing, dirty: false }
    }
  }

  function removeDocument(id: string): void {
    delete draftsById.value[id]
  }

  function clear(): void {
    draftsById.value = {}
  }

  return { draftsById, getDraft, setDraft, updateDraft, markSaved, removeDocument, clear }
})
