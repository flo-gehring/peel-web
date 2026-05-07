import type { JsonObject } from '../../../lib/api/types'

const EMPTY_DOCUMENT_TEXT =
  'Start writing your document.\n\nPreview renders this editor content into a PDF.'

type LexicalNode = {
  type: string
  children?: LexicalNode[]
  text?: string
  tag?: string
  listType?: string
  scriptId?: string
  message?: string
}

type LexicalState = {
  root: LexicalNode
}

export function defaultDocumentContent(): JsonObject {
  return lexicalStateToJsonObject(createEmptyLexicalState())
}

export function defaultDocumentBindings(): JsonObject {
  return {}
}

export function normalizeDocumentContent(content: unknown): JsonObject {
  const lexical = normalizeToLexicalState(content)
  return lexicalStateToJsonObject(lexical)
}

export function parseDocumentContent(content: unknown): LexicalState {
  return normalizeToLexicalState(content)
}

export function lexicalStateToJsonObject(state: LexicalState): JsonObject {
  return JSON.parse(JSON.stringify(state)) as JsonObject
}

export function documentContentToPlainText(content: JsonObject): string {
  const lexical = normalizeToLexicalState(content)
  const chunks: string[] = []
  appendNodeText(lexical.root, chunks, 0)
  const combined = chunks.join('')
  return combined.replace(/\n{3,}/g, '\n\n').trimEnd()
}

function normalizeToLexicalState(content: unknown): LexicalState {
  if (isLexicalState(content)) {
    return content
  }

  if (isTiptapDocument(content)) {
    return tiptapToLexical(content)
  }

  if (isLegacyDocument(content)) {
    return legacyToLexical(content)
  }

  return createEmptyLexicalState()
}

function createEmptyLexicalState(): LexicalState {
  return {
    root: {
      type: 'root',
      children: [
        {
          type: 'paragraph',
          children: [
            {
              type: 'text',
              text: EMPTY_DOCUMENT_TEXT,
            },
          ],
        },
      ],
    },
  }
}

function isLexicalState(content: unknown): content is LexicalState {
  if (!content || typeof content !== 'object' || Array.isArray(content)) {
    return false
  }

  const candidate = content as Record<string, unknown>
  if (!candidate.root || typeof candidate.root !== 'object' || Array.isArray(candidate.root)) {
    return false
  }

  const root = candidate.root as Record<string, unknown>
  return root.type === 'root' && Array.isArray(root.children)
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

function tiptapToLexical(content: JsonObject): LexicalState {
  const tiptapNodes = Array.isArray((content as Record<string, unknown>).content)
    ? ((content as Record<string, unknown>).content as unknown[])
    : []

  const rootChildren = tiptapNodes
    .map((node) => tiptapNodeToLexicalNode(node))
    .filter((node): node is LexicalNode => node !== null)

  if (rootChildren.length === 0) {
    return createEmptyLexicalState()
  }

  return {
    root: {
      type: 'root',
      children: rootChildren,
    },
  }
}

function tiptapNodeToLexicalNode(node: unknown): LexicalNode | null {
  if (!node || typeof node !== 'object' || Array.isArray(node)) {
    return null
  }

  const record = node as Record<string, unknown>
  const type = typeof record.type === 'string' ? record.type : null
  if (!type) {
    return null
  }

  if (type === 'text') {
    if (typeof record.text !== 'string') {
      return null
    }
    return {
      type: 'text',
      text: record.text,
    }
  }

  if (type === 'hardBreak') {
    return { type: 'linebreak' }
  }

  if (type === 'heading') {
    const attrs =
      record.attrs && typeof record.attrs === 'object' && !Array.isArray(record.attrs)
        ? (record.attrs as Record<string, unknown>)
        : {}
    const level = typeof attrs.level === 'number' ? attrs.level : 1
    return {
      type: 'heading',
      tag: level >= 3 ? 'h3' : level === 2 ? 'h2' : 'h1',
      children: mapChildNodes(record.content),
    }
  }

  if (type === 'paragraph') {
    return {
      type: 'paragraph',
      children: mapChildNodes(record.content),
    }
  }

  if (type === 'blockquote') {
    return {
      type: 'quote',
      children: mapChildNodes(record.content),
    }
  }

  if (type === 'codeBlock') {
    return {
      type: 'code',
      children: mapChildNodes(record.content),
    }
  }

  if (type === 'bulletList' || type === 'orderedList') {
    const listType = type === 'orderedList' ? 'number' : 'bullet'
    const items = mapChildNodes(record.content).map((child) => {
      if (child.type === 'listitem') {
        return child
      }
      return {
        type: 'listitem',
        children: [child],
      }
    })
    return {
      type: 'list',
      listType,
      children: items,
    }
  }

  if (type === 'listItem') {
    return {
      type: 'listitem',
      children: mapChildNodes(record.content),
    }
  }

  return {
    type: 'paragraph',
    children: mapChildNodes(record.content),
  }
}

function mapChildNodes(content: unknown): LexicalNode[] {
  const children = Array.isArray(content) ? content : []
  const mapped = children
    .map((child) => tiptapNodeToLexicalNode(child))
    .filter((child): child is LexicalNode => child !== null)

  if (mapped.length > 0) {
    return mapped
  }

  return [{ type: 'text', text: '' }]
}

function legacyToLexical(content: JsonObject): LexicalState {
  const blocks = Array.isArray((content as Record<string, unknown>).blocks)
    ? ((content as Record<string, unknown>).blocks as unknown[])
    : []

  const paragraphs: LexicalNode[] = []
  for (const block of blocks) {
    if (!block || typeof block !== 'object' || Array.isArray(block)) {
      continue
    }
    const blockRecord = block as Record<string, unknown>
    if (blockRecord.type !== 'paragraph' || typeof blockRecord.text !== 'string') {
      continue
    }

    paragraphs.push({
      type: 'paragraph',
      children: [{ type: 'text', text: blockRecord.text }],
    })
  }

  if (paragraphs.length === 0) {
    return createEmptyLexicalState()
  }

  return {
    root: {
      type: 'root',
      children: paragraphs,
    },
  }
}

function appendNodeText(node: LexicalNode, chunks: string[], listDepth: number): void {
  if (node.type === 'text') {
    const raw = typeof node.text === 'string' ? node.text : ''
    chunks.push(raw)
    return
  }

  if (node.type === 'linebreak') {
    chunks.push('\n')
    return
  }

  if (node.type === 'scriptRef') {
    if (typeof node.scriptId === 'string' && node.scriptId.trim().length > 0) {
      chunks.push(`{{script:${node.scriptId}}}`)
    }
    return
  }

  if (node.type === 'scriptResult') {
    chunks.push(typeof node.text === 'string' ? node.text : '')
    return
  }

  if (node.type === 'scriptError') {
    chunks.push(typeof node.message === 'string' ? `[script error: ${node.message}]` : '[script error]')
    return
  }

  if (node.type === 'pageBreak' || node.type === 'horizontalrule') {
    chunks.push('\n\n')
    return
  }

  const children = Array.isArray(node.children) ? node.children : []

  if (node.type === 'list') {
    const isNumbered = node.listType === 'number'
    children.forEach((child, index) => {
      const prefix = isNumbered ? `${index + 1}. ` : '- '
      chunks.push('  '.repeat(listDepth))
      chunks.push(prefix)
      appendNodeText(child, chunks, listDepth + 1)
      chunks.push('\n')
    })
    chunks.push('\n')
    return
  }

  for (const child of children) {
    appendNodeText(child, chunks, listDepth)
  }

  if (
    node.type === 'root' ||
    node.type === 'paragraph' ||
    node.type === 'heading' ||
    node.type === 'quote' ||
    node.type === 'code' ||
    node.type === 'listitem'
  ) {
    chunks.push('\n')
  }
}
