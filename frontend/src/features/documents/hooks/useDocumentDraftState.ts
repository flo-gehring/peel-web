import { useEffect, useState } from 'react'

const STORAGE_KEY = 'peel-documents-draft-v4'

type DocumentDraft = {
  name: string
  scriptNameTagsText: string
  template: string
  bindingsText: string
  renderConfigurationId: string
  selectedDocumentId: string | null
}

function fallbackDraft(): DocumentDraft {
  return {
    name: 'Untitled document',
    scriptNameTagsText: '{\n  "calc": "SCRIPT_ID"\n}',
    template:
      '<ul>\n{% for statement in calc.statements %}\n  <li>{{ statement | renderTraceExpression }}</li>\n{% endfor %}\n</ul>',
    bindingsText: '{}',
    renderConfigurationId: 'default',
    selectedDocumentId: null,
  }
}

function loadDraft(): DocumentDraft {
  const fallback = fallbackDraft()
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) {
    return fallback
  }

  try {
    const parsed = JSON.parse(raw) as Partial<DocumentDraft>
    return {
      name:
        typeof parsed.name === 'string' && parsed.name.trim().length > 0
          ? parsed.name
          : fallback.name,
      scriptNameTagsText:
        typeof parsed.scriptNameTagsText === 'string'
          ? parsed.scriptNameTagsText
          : fallback.scriptNameTagsText,
      template: typeof parsed.template === 'string' ? parsed.template : fallback.template,
      bindingsText: typeof parsed.bindingsText === 'string' ? parsed.bindingsText : fallback.bindingsText,
      renderConfigurationId:
        typeof parsed.renderConfigurationId === 'string' && parsed.renderConfigurationId.trim().length > 0
          ? parsed.renderConfigurationId
          : fallback.renderConfigurationId,
      selectedDocumentId:
        typeof parsed.selectedDocumentId === 'string' ? parsed.selectedDocumentId : null,
    }
  } catch {
    localStorage.removeItem(STORAGE_KEY)
    return fallback
  }
}

export function useDocumentDraftState() {
  const [seed] = useState<DocumentDraft>(() => loadDraft())
  const [name, setName] = useState(seed.name)
  const [scriptNameTagsText, setScriptNameTagsText] = useState(seed.scriptNameTagsText)
  const [template, setTemplate] = useState(seed.template)
  const [bindingsText, setBindingsText] = useState(seed.bindingsText)
  const [renderConfigurationId, setRenderConfigurationId] = useState(seed.renderConfigurationId)
  const [selectedDocumentId, setSelectedDocumentId] = useState<string | null>(seed.selectedDocumentId)

  useEffect(() => {
    const payload: DocumentDraft = {
      name,
      scriptNameTagsText,
      template,
      bindingsText,
      renderConfigurationId,
      selectedDocumentId,
    }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(payload))
  }, [bindingsText, name, renderConfigurationId, scriptNameTagsText, selectedDocumentId, template])

  const resetToDefault = () => {
    const fallback = fallbackDraft()
    setSelectedDocumentId(null)
    setName(fallback.name)
    setScriptNameTagsText(fallback.scriptNameTagsText)
    setTemplate(fallback.template)
    setBindingsText(fallback.bindingsText)
    setRenderConfigurationId(fallback.renderConfigurationId)
  }

  return {
    name,
    setName,
    scriptNameTagsText,
    setScriptNameTagsText,
    template,
    setTemplate,
    bindingsText,
    setBindingsText,
    renderConfigurationId,
    setRenderConfigurationId,
    selectedDocumentId,
    setSelectedDocumentId,
    resetToDefault,
  }
}
