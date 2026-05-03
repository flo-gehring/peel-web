import { useEffect, useState } from 'react'

import type { JsonObject } from '../../../lib/api/types'
import {
  defaultDocumentBindings,
  defaultDocumentContent,
  normalizeDocumentContent,
} from '../lib/content'

const STORAGE_KEY = 'peel-documents-draft-v1'

type DocumentDraft = {
  name: string
  content: JsonObject
  bindingsText: string
  selectedDocumentId: string | null
}

function fallbackDraft(): DocumentDraft {
  return {
    name: 'Untitled document',
    content: defaultDocumentContent(),
    bindingsText: JSON.stringify(defaultDocumentBindings(), null, 2),
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
    const parsed = JSON.parse(raw) as Partial<DocumentDraft> & {
      contentText?: string
    }
    return {
      name:
        typeof parsed.name === 'string' && parsed.name.trim().length > 0
          ? parsed.name
          : fallback.name,
      content: parseStoredContent(parsed, fallback.content),
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
  const [content, setContent] = useState(seed.content)
  const [bindingsText, setBindingsText] = useState(seed.bindingsText)
  const [selectedDocumentId, setSelectedDocumentId] = useState<string | null>(seed.selectedDocumentId)

  useEffect(() => {
    const payload: DocumentDraft = {
      name,
      content,
      bindingsText,
      selectedDocumentId,
    }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(payload))
  }, [bindingsText, content, name, selectedDocumentId])

  const resetToDefault = () => {
    const fallback = fallbackDraft()
    setSelectedDocumentId(null)
    setName(fallback.name)
    setContent(fallback.content)
    setBindingsText(fallback.bindingsText)
  }

  return {
    name,
    setName,
    content,
    setContent,
    bindingsText,
    setBindingsText,
    selectedDocumentId,
    setSelectedDocumentId,
    resetToDefault,
  }
}

function parseStoredContent(
  parsed: Partial<DocumentDraft> & { contentText?: string },
  fallback: JsonObject,
): JsonObject {
  if (parsed.content && typeof parsed.content === 'object' && !Array.isArray(parsed.content)) {
    return normalizeDocumentContent(parsed.content)
  }

  if (typeof parsed.contentText === 'string') {
    try {
      const contentFromText = JSON.parse(parsed.contentText) as unknown
      return normalizeDocumentContent(contentFromText)
    } catch {
      return fallback
    }
  }

  return fallback
}
