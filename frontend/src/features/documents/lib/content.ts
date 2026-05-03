import type { JsonObject, JsonValue } from '../../../lib/api/types'

const EMPTY_DOCUMENT_TEXT =
  'Start writing your document.\\n\\nInsert script reference placeholders with refId and scriptId in JSON mode in milestone C.'

const DEFAULT_BINDINGS = {
  gross: 125000,
  expenses: 83000,
}

export function defaultDocumentContent(): JsonObject {
  return {
    type: 'doc',
    version: 1,
    blocks: [
      {
        type: 'paragraph',
        text: EMPTY_DOCUMENT_TEXT,
      },
    ],
  }
}

export function defaultDocumentBindings(): JsonObject {
  return {
    ...DEFAULT_BINDINGS,
  }
}

export function parseDocumentContent(text: string): { value: JsonObject | null; error: string | null } {
  try {
    const parsed = JSON.parse(text) as JsonValue
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      return {
        value: null,
        error: 'Document content must be a JSON object.',
      }
    }
    return {
      value: parsed as JsonObject,
      error: null,
    }
  } catch (error) {
    return {
      value: null,
      error: error instanceof Error ? error.message : 'Invalid JSON',
    }
  }
}
