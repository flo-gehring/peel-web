import { useEffect, useState } from 'react'

const STORAGE_KEY = 'peel-workbench-draft-v1'

const DEFAULT_SCRIPT = `var income = gross;
var cost = expenses;

if (income > cost) {
  income - cost;
} else {
  0;
}`

const DEFAULT_BINDINGS = {
  gross: 125000,
  expenses: 83000,
}

export type DraftState = {
  script: string
  scriptName: string
  bindingsText: string
  selectedScriptId: string | null
}

function loadDraft(): DraftState {
  const fallback: DraftState = {
    script: DEFAULT_SCRIPT,
    scriptName: 'Profit example',
    bindingsText: JSON.stringify(DEFAULT_BINDINGS, null, 2),
    selectedScriptId: null,
  }

  const rawDraft = localStorage.getItem(STORAGE_KEY)
  if (!rawDraft) {
    return fallback
  }

  try {
    const parsed = JSON.parse(rawDraft) as Partial<DraftState>
    return {
      script: typeof parsed.script === 'string' ? parsed.script : fallback.script,
      scriptName:
        typeof parsed.scriptName === 'string' && parsed.scriptName.trim().length > 0
          ? parsed.scriptName
          : fallback.scriptName,
      bindingsText:
        typeof parsed.bindingsText === 'string' ? parsed.bindingsText : fallback.bindingsText,
      selectedScriptId:
        typeof parsed.selectedScriptId === 'string' ? parsed.selectedScriptId : fallback.selectedScriptId,
    }
  } catch {
    localStorage.removeItem(STORAGE_KEY)
    return fallback
  }
}

export function useDraftState() {
  const [seed] = useState<DraftState>(() => loadDraft())
  const [script, setScript] = useState(seed.script)
  const [scriptName, setScriptName] = useState(seed.scriptName)
  const [bindingsText, setBindingsText] = useState(seed.bindingsText)
  const [selectedScriptId, setSelectedScriptId] = useState<string | null>(seed.selectedScriptId)

  useEffect(() => {
    const payload: DraftState = {
      script,
      scriptName,
      bindingsText,
      selectedScriptId,
    }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(payload))
  }, [bindingsText, script, scriptName, selectedScriptId])

  const resetToDefault = () => {
    setSelectedScriptId(null)
    setScriptName('Untitled script')
    setScript(DEFAULT_SCRIPT)
  }

  const duplicate = () => {
    setSelectedScriptId(null)
    setScriptName((current) => `${current} copy`)
  }

  return {
    script,
    setScript,
    scriptName,
    setScriptName,
    bindingsText,
    setBindingsText,
    selectedScriptId,
    setSelectedScriptId,
    resetToDefault,
    duplicate,
  }
}
