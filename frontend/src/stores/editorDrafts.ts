import { ref } from 'vue'
import { defineStore } from 'pinia'

type DraftEntry = {
  content: string
  dirty: boolean
  updatedAt: number
}

export const useEditorDraftStore = defineStore('editorDrafts', () => {
  const draftsById = ref<Record<string, DraftEntry>>({})
  const panelToDocumentId = ref<Record<string, string>>({})

  function hasDraft(id: string): boolean {
    return !!draftsById.value[id]
  }

  function getDraft(id: string): string | undefined {
    return draftsById.value[id]?.content
  }

  function markLoaded(id: string, content: string): void {
    if (draftsById.value[id]) {
      return
    }

    draftsById.value[id] = {
      content,
      dirty: false,
      updatedAt: Date.now(),
    }
  }

  function setDraft(id: string, content: string): void {
    draftsById.value[id] = {
      content,
      dirty: true,
      updatedAt: Date.now(),
    }
  }

  function markSaved(id: string, content?: string): void {
    const existing = draftsById.value[id]
    draftsById.value[id] = {
      content: content ?? existing?.content ?? '',
      dirty: false,
      updatedAt: Date.now(),
    }
  }

  function bindPanelToDocument(panelId: string, documentId: string): void {
    panelToDocumentId.value[panelId] = documentId

    const panelDraft = draftsById.value[panelId]
    if (panelDraft) {
      draftsById.value[documentId] = {
        content: panelDraft.content,
        dirty: panelDraft.dirty,
        updatedAt: Date.now(),
      }
      delete draftsById.value[panelId]
    }
  }

  function getDocumentIdForPanel(panelId: string): string | undefined {
    return panelToDocumentId.value[panelId]
  }

  function resolveDraftKey(panelId: string, documentId?: string): string {
    return documentId || panelToDocumentId.value[panelId] || panelId
  }

  function setDraftByReference(panelId: string, content: string, documentId?: string): void {
    const key = resolveDraftKey(panelId, documentId)
    setDraft(key, content)
  }

  function getDraftByReference(panelId: string, documentId?: string): string | undefined {
    const key = resolveDraftKey(panelId, documentId)
    return getDraft(key)
  }

  function markSavedByReference(panelId: string, content: string, documentId?: string): void {
    const key = resolveDraftKey(panelId, documentId)
    markSaved(key, content)
  }

  function removeDocument(id: string): void {
    delete draftsById.value[id]

    for (const [panelId, documentId] of Object.entries(panelToDocumentId.value)) {
      if (documentId === id) {
        delete panelToDocumentId.value[panelId]
      }
    }
  }

  return {
    draftsById,
    panelToDocumentId,
    hasDraft,
    getDraft,
    markLoaded,
    setDraft,
    markSaved,
    bindPanelToDocument,
    getDocumentIdForPanel,
    setDraftByReference,
    getDraftByReference,
    markSavedByReference,
    removeDocument,
  }
})
