import { useEffect, useState } from 'react'

import { defaultDocumentContent } from '../lib/content'

const STORAGE_KEY = 'peel-documents-draft-v1'

const DEFAULT_BINDINGS = {
  gross: 125000,
  expenses: 83000,
}

type DocumentDraft = {
  name: string
  contentText: string
  bindingsText: string
  selectedDocumentId: string | null
}

function fallbackDraft(): DocumentDraft {
  return {
    name: 'Untitled document',
    contentText: JSON.stringify(defaultDocumentContent(), null, 2),
    bindingsText: JSON.stringify(DEFAULT_BINDINGS, null, 2),
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
      contentText:
        typeof parsed.contentText === 'string' ? parsed.contentText : fallback.contentText,
      bindingsText:
        typeof parsed.bindingsText === 'string' ? parsed.bindingsText : fallback.bindingsText,
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
  const [contentText, setContentText] = useState(seed.contentText)
  const [bindingsText, setBindingsText] = useState(seed.bindingsText)
  const [selectedDocumentId, setSelectedDocumentId] = useState<string | null>(seed.selectedDocumentId)

  useEffect(() => {
    const payload: DocumentDraft = {
      name,
      contentText,
      bindingsText,
      selectedDocumentId,
    }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(payload))
  }, [bindingsText, contentText, name, selectedDocumentId])

  const resetToDefault = () => {
    const fallback = fallbackDraft()
    setSelectedDocumentId(null)
    setName(fallback.name)
    setContentText(fallback.contentText)
    setBindingsText(fallback.bindingsText)
  }

  return {
    name,
    setName,
    contentText,
    setContentText,
    bindingsText,
    setBindingsText,
    selectedDocumentId,
    setSelectedDocumentId,
    resetToDefault,
  }
}
