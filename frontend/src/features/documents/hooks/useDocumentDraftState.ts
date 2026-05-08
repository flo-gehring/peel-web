import { useEffect, useState } from 'react'

const STORAGE_KEY = 'peel-documents-draft-v3'

type DocumentDraft = {
  name: string
  script: string
  template: string
  bindingsText: string
  selectedDocumentId: string | null
}

function fallbackDraft(): DocumentDraft {
  return {
    name: 'Untitled document',
    script: '',
    template: '<ul>\n{% for statement in statements %}\n  <li>{{ statement | renderTraceExpression }}</li>\n{% endfor %}\n</ul>',
    bindingsText: '{}',
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
      script: typeof parsed.script === 'string' ? parsed.script : fallback.script,
      template: typeof parsed.template === 'string' ? parsed.template : fallback.template,
      bindingsText: typeof parsed.bindingsText === 'string' ? parsed.bindingsText : fallback.bindingsText,
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
  const [script, setScript] = useState(seed.script)
  const [template, setTemplate] = useState(seed.template)
  const [bindingsText, setBindingsText] = useState(seed.bindingsText)
  const [selectedDocumentId, setSelectedDocumentId] = useState<string | null>(seed.selectedDocumentId)

  useEffect(() => {
    const payload: DocumentDraft = {
      name,
      script,
      template,
      bindingsText,
      selectedDocumentId,
    }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(payload))
  }, [bindingsText, name, script, selectedDocumentId, template])

  const resetToDefault = () => {
    const fallback = fallbackDraft()
    setSelectedDocumentId(null)
    setName(fallback.name)
    setScript(fallback.script)
    setTemplate(fallback.template)
    setBindingsText(fallback.bindingsText)
  }

  return {
    name,
    setName,
    script,
    setScript,
    template,
    setTemplate,
    bindingsText,
    setBindingsText,
    selectedDocumentId,
    setSelectedDocumentId,
    resetToDefault,
  }
}
