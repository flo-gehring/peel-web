import type {
  DOMConversionMap,
  DOMConversionOutput,
  DOMExportOutput,
  EditorConfig,
  LexicalEditor,
  LexicalNode,
  NodeKey,
  SerializedLexicalNode,
  Spread,
} from 'lexical'
import type { ChangeEvent, ReactElement } from 'react'

import { addClassNamesToElement } from '@lexical/utils'
import { $applyNodeReplacement, $getNodeByKey, DecoratorNode } from 'lexical'
import { useEffect, useState } from 'react'

type SerializedPebbleBlockNode = Spread<
  {
    markup: string
  },
  SerializedLexicalNode
>

const BLOCK_ATTR = 'data-peel-block'

export class PebbleBlockNode extends DecoratorNode<ReactElement> {
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
    addClassNamesToElement(element, 'my-2')
    element.setAttribute('contenteditable', 'false')
    return element
  }

  updateDOM(prevNode: PebbleBlockNode, dom: HTMLElement): boolean {
    if (prevNode.__markup !== this.__markup) {
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

  decorate(editor: LexicalEditor): ReactElement {
    return (
      <PebbleBlockEditor
        nodeKey={this.__key}
        markup={this.__markup}
        editor={editor}
      />
    )
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

function PebbleBlockEditor({ nodeKey, markup, editor }: { nodeKey: NodeKey; markup: string; editor: LexicalEditor }) {
  const [draft, setDraft] = useState(markup)

  useEffect(() => {
    setDraft(markup)
  }, [markup])

  const handleChange = (event: ChangeEvent<HTMLTextAreaElement>) => {
    const nextMarkup = event.target.value
    setDraft(nextMarkup)
    editor.update(() => {
      const node = $getNodeByKey(nodeKey)
      if ($isPebbleBlockNode(node)) {
        node.setMarkup(nextMarkup)
      }
    })
  }

  const handleDelete = () => {
    editor.update(() => {
      const node = $getNodeByKey(nodeKey)
      if ($isPebbleBlockNode(node)) {
        node.remove()
      }
    })
  }

  return (
    <div className="peel-block-card" data-peel-block={markup}>
      <div className="peel-block-header">
        <span className="peel-block-label">Pebble Block</span>
        <button
          type="button"
          className="peel-block-delete"
          onMouseDown={(event) => event.stopPropagation()}
          onClick={handleDelete}
        >
          Delete
        </button>
      </div>
      <textarea
        value={draft}
        onChange={handleChange}
        onMouseDown={(event) => event.stopPropagation()}
        className="peel-block-textarea"
        spellCheck={false}
        aria-label="Pebble block markup"
      />
    </div>
  )
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
