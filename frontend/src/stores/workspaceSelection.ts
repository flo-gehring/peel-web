import { ref } from 'vue'
import { defineStore } from 'pinia'
import type { PeelWorkspaceDocument } from '@/adapter/ClientTypeDefinition'

type PeelScriptDocument = Extract<PeelWorkspaceDocument, { kind: 'peel' }>

export const useWorkspaceSelectionStore = defineStore('workspaceSelection', () => {
  const selectedScript = ref<PeelScriptDocument | null>(null)
  const selectionVersion = ref(0)

  function selectScript(file: PeelScriptDocument): void {
    selectedScript.value = file
    selectionVersion.value += 1
  }

  return {
    selectedScript,
    selectionVersion,
    selectScript,
  }
})
