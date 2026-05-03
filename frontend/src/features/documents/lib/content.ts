import type { JsonObject, JsonValue } from '../../../lib/api/types'

const EMPTY_DOCUMENT_TEXT =
  'Start writing your document.\n\nPreview renders this editor content into a PDF.'

export function defaultDocumentContent(): JsonObject {
  return {
    type: 'doc',
    content: [
      {
        type: 'paragraph',
        content: [
          {
            type: 'text',
            text: EMPTY_DOCUMENT_TEXT,
          },
        ],
      },
    ],
  }
}

export function defaultDocumentBindings(): JsonObject {
  return {}
}

export function normalizeDocumentContent(content: unknown): JsonObject {
  if (isTiptapDocument(content)) {
    return content
  }

  if (isLegacyDocument(content)) {
    return legacyToTiptap(content)
  }

  return defaultDocumentContent()
}

export function documentContentToPlainText(content: JsonObject): string {
  const chunks: string[] = []
  appendNodeText(content, chunks)
  const combined = chunks.join('')
  return combined.replace(/\n{3,}/g, '\n\n').trim()
}

function appendNodeText(node: unknown, chunks: string[]): void {
  if (!node || typeof node !== 'object' || Array.isArray(node)) {
    return
  }

  const record = node as Record<string, unknown>
  const nodeType = typeof record.type === 'string' ? record.type : null

  if (nodeType === 'text' && typeof record.text === 'string') {
    chunks.push(record.text)
    return
  }

  if (nodeType === 'hardBreak') {
    chunks.push('\n')
    return
  }

  const contentNodes = Array.isArray(record.content) ? record.content : []

  if (nodeType === 'bulletList' || nodeType === 'orderedList') {
    for (const listItem of contentNodes) {
      chunks.push('- ')
      appendNodeText(listItem, chunks)
      chunks.push('\n')
    }
    chunks.push('\n')
    return
  }

  for (const childNode of contentNodes) {
    appendNodeText(childNode, chunks)
  }

  if (
    nodeType === 'paragraph' ||
    nodeType === 'heading' ||
    nodeType === 'listItem' ||
    nodeType === 'blockquote'
  ) {
    chunks.push('\n')
  }
}

function isTiptapDocument(content: unknown): content is JsonObject {
  if (!content || typeof content !== 'object' || Array.isArray(content)) {
    return false
  }

  const candidate = content as Record<string, unknown>
  return candidate.type === 'doc' && Array.isArray(candidate.content)
}

function isLegacyDocument(content: unknown): content is JsonObject {
  if (!content || typeof content !== 'object' || Array.isArray(content)) {
    return false
  }
  const candidate = content as Record<string, unknown>
  return candidate.type === 'doc' && Array.isArray(candidate.blocks)
}

function legacyToTiptap(content: JsonObject): JsonObject {
  const blocks = Array.isArray((content as Record<string, JsonValue>).blocks)
    ? ((content as Record<string, JsonValue>).blocks as JsonValue[])
    : []

  const paragraphs = blocks
    .map((block) => {
      if (!block || typeof block !== 'object' || Array.isArray(block)) {
        return null
      }
      const blockRecord = block as Record<string, unknown>
      if (blockRecord.type !== 'paragraph' || typeof blockRecord.text !== 'string') {
        return null
      }
      return {
        type: 'paragraph',
        content: [{ type: 'text', text: blockRecord.text }],
      }
    })
    .filter((value): value is { type: 'paragraph'; content: { type: 'text'; text: string }[] } => value !== null)

  if (paragraphs.length === 0) {
    return defaultDocumentContent()
  }

  return {
    type: 'doc',
    content: paragraphs,
  }
}
