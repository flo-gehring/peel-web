import { ref } from 'vue'
import { defineStore } from 'pinia'

type DraftEntry = {
  content: string
  dirty: boolean
  updatedAt: number
}

export const useEditorDraftStore = defineStore('editorDrafts', () => {
  const draftsById = ref<Record<string, DraftEntry>>({})

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

  return {
    draftsById,
    hasDraft,
    getDraft,
    markLoaded,
    setDraft,
  }
})
