import type {
  DOMConversionMap,
  DOMConversionOutput,
  DOMExportOutput,
  EditorConfig,
  LexicalNode,
  NodeKey,
  SerializedTextNode,
  Spread,
} from 'lexical'

import { addClassNamesToElement } from '@lexical/utils'
import { $applyNodeReplacement, TextNode } from 'lexical'

export type SerializedPebbleInlineNode = Spread<
  {
    expression: string
  },
  SerializedTextNode
>

const INLINE_ATTR = 'data-peel-inline'

export class PebbleInlineNode extends TextNode {
  __expression: string

  static getType(): string {
    return 'pebble-inline'
  }

  static clone(node: PebbleInlineNode): PebbleInlineNode {
    return new PebbleInlineNode(node.__expression, node.__text, node.__key)
  }

  static importJSON(serializedNode: SerializedPebbleInlineNode): PebbleInlineNode {
    return $createPebbleInlineNode(serializedNode.expression).updateFromJSON(serializedNode)
  }

  static importDOM(): DOMConversionMap | null {
    return {
      span: (domNode: Node) => {
        if (!(domNode instanceof HTMLElement)) {
          return null
        }
        const expression = domNode.getAttribute(INLINE_ATTR)
        if (!expression) {
          return null
        }
        return {
          conversion: () => ({ node: $createPebbleInlineNode(expression) }),
          priority: 3,
        }
      },
    }
  }

  constructor(expression: string, text?: string, key?: NodeKey) {
    super(text ?? tokenFromExpression(expression), key)
    this.__expression = normalizeExpression(expression)
  }

  createDOM(config: EditorConfig): HTMLElement {
    const element = super.createDOM(config)
    addClassNamesToElement(element, 'peel-script-chip')
    element.setAttribute(INLINE_ATTR, this.__expression)
    element.setAttribute('contenteditable', 'false')
    return element
  }

  exportDOM(): DOMExportOutput {
    const element = document.createElement('span')
    element.setAttribute(INLINE_ATTR, this.__expression)
    element.textContent = this.getTextContent()
    element.className = 'peel-script-chip'
    return { element }
  }

  exportJSON(): SerializedPebbleInlineNode {
    return {
      ...super.exportJSON(),
      type: 'pebble-inline',
      version: 1,
      text: this.getTextContent(),
      expression: this.__expression,
    }
  }

  canInsertTextBefore(): boolean {
    return false
  }

  canInsertTextAfter(): boolean {
    return false
  }

  isTextEntity(): true {
    return true
  }

  setExpression(expression: string): void {
    const writable = this.getWritable()
    writable.__expression = normalizeExpression(expression)
    writable.__text = tokenFromExpression(writable.__expression)
  }

  getExpression(): string {
    return this.getLatest().__expression
  }
}

export function $createPebbleInlineNode(expression: string): PebbleInlineNode {
  return $applyNodeReplacement(new PebbleInlineNode(expression))
}

export function $isPebbleInlineNode(node: LexicalNode | null | undefined): node is PebbleInlineNode {
  return node instanceof PebbleInlineNode
}

function normalizeExpression(expression: string): string {
  const trimmed = expression.trim()
  if (trimmed.length === 0) {
    return '{{ value }}'
  }
  return trimmed
}

function tokenFromExpression(expression: string): string {
  return expression
}

export function parsePebbleInlineElement(domNode: Node): DOMConversionOutput | null {
  if (!(domNode instanceof HTMLElement)) {
    return null
  }
  const expression = domNode.getAttribute(INLINE_ATTR)
  if (!expression) {
    return null
  }
  return { node: $createPebbleInlineNode(expression) }
}
