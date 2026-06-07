import type {
  DOMConversionMap,
  DOMConversionOutput,
  DOMExportOutput,
  EditorConfig,
  LexicalNode,
  NodeKey,
  SerializedLexicalNode,
  Spread,
} from 'lexical'

import { addClassNamesToElement } from '@lexical/utils'
import { $applyNodeReplacement, DecoratorNode } from 'lexical'

type SerializedPebbleBlockNode = Spread<
  {
    markup: string
  },
  SerializedLexicalNode
>

const BLOCK_ATTR = 'data-peel-block'

export class PebbleBlockNode extends DecoratorNode<null> {
  __markup: string

  static getType(): string {
    return 'pebble-block'
  }

  static clone(node: PebbleBlockNode): PebbleBlockNode {
    return new PebbleBlockNode(node.__markup, node.__key)
  }

  static importJSON(serializedNode: SerializedPebbleBlockNode): PebbleBlockNode {
    return $createPebbleBlockNode(serializedNode.markup)
  }

  static importDOM(): DOMConversionMap | null {
    return {
      div: (domNode: Node) => {
        if (!(domNode instanceof HTMLElement)) {
          return null
        }
        const markup = domNode.getAttribute(BLOCK_ATTR)
        if (!markup) {
          return null
        }
        return {
          conversion: () => ({ node: $createPebbleBlockNode(markup) }),
          priority: 3,
        }
      },
    }
  }

  constructor(markup: string, key?: NodeKey) {
    super(key)
    this.__markup = normalizeMarkup(markup)
  }

  createDOM(_config: EditorConfig): HTMLElement {
    const element = document.createElement('div')
    addClassNamesToElement(element, 'rounded', 'border', 'border-cyan-700/50', 'bg-cyan-950/20', 'p-3', 'my-2')
    element.setAttribute('contenteditable', 'false')
    element.setAttribute(BLOCK_ATTR, this.__markup)
    const label = document.createElement('div')
    label.className = 'text-[11px] uppercase tracking-[0.12em] text-cyan-300'
    label.textContent = 'Pebble Block'
    const body = document.createElement('pre')
    body.className = 'mt-2 whitespace-pre-wrap text-xs text-cyan-100'
    body.textContent = this.__markup
    element.append(label, body)
    return element
  }

  updateDOM(prevNode: PebbleBlockNode, dom: HTMLElement): boolean {
    if (prevNode.__markup !== this.__markup) {
      const pre = dom.querySelector('pre')
      if (pre) {
        pre.textContent = this.__markup
      }
      dom.setAttribute(BLOCK_ATTR, this.__markup)
    }
    return false
  }

  exportDOM(): DOMExportOutput {
    const element = document.createElement('div')
    element.setAttribute(BLOCK_ATTR, this.__markup)
    element.textContent = this.__markup
    return { element }
  }

  exportJSON(): SerializedPebbleBlockNode {
    return {
      type: 'pebble-block',
      version: 1,
      markup: this.__markup,
    }
  }

  decorate(): null {
    return null
  }

  isInline(): boolean {
    return false
  }

  setMarkup(markup: string): void {
    const writable = this.getWritable()
    writable.__markup = normalizeMarkup(markup)
  }

  getMarkup(): string {
    return this.getLatest().__markup
  }
}

export function $createPebbleBlockNode(markup: string): PebbleBlockNode {
  return $applyNodeReplacement(new PebbleBlockNode(markup))
}

export function $isPebbleBlockNode(node: LexicalNode | null | undefined): node is PebbleBlockNode {
  return node instanceof PebbleBlockNode
}

export function parsePebbleBlockElement(domNode: Node): DOMConversionOutput | null {
  if (!(domNode instanceof HTMLElement)) {
    return null
  }
  const markup = domNode.getAttribute(BLOCK_ATTR)
  if (!markup) {
    return null
  }
  return { node: $createPebbleBlockNode(markup) }
}

function normalizeMarkup(markup: string): string {
  const trimmed = markup.trim()
  if (trimmed.length === 0) {
    return '{% if condition %}\n...\n{% endif %}'
  }
  return trimmed
}
