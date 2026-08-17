import { ref } from 'vue'
import { defineStore } from 'pinia'
import type { PeelWorkspaceDocument } from '@/adapter/ClientTypeDefinition'

type PeelScriptDocument = Extract<PeelWorkspaceDocument, { kind: 'peel' }>

export const useWorkspaceSelectionStore = defineStore('workspaceSelection', () => {
  const selectedScript = ref<PeelScriptDocument | null>(null)
  const selectionVersion = ref(0)
  const scriptsVersion = ref(0)
  const deletedScriptId = ref<string | null>(null)
  const deletionVersion = ref(0)

  function selectScript(file: PeelScriptDocument): void {
    selectedScript.value = file
    selectionVersion.value += 1
  }

  function notifyScriptsChanged(): void {
    scriptsVersion.value += 1
  }

  function deleteScript(id: string): void {
    if (selectedScript.value?.id === id) {
      selectedScript.value = null
    }
    deletedScriptId.value = id
    deletionVersion.value += 1
    notifyScriptsChanged()
  }

  return {
    selectedScript,
    selectionVersion,
    scriptsVersion,
    deletedScriptId,
    deletionVersion,
    selectScript,
    notifyScriptsChanged,
    deleteScript,
  }
})
