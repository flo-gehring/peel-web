import type { EditorConfig, LexicalNode, NodeKey, SerializedTextNode, Spread } from 'lexical'

import { addClassNamesToElement } from '@lexical/utils'
import { $applyNodeReplacement, TextNode } from 'lexical'

export type SerializedScriptRefNode = Spread<
  {
    scriptId: string
    refId: string
  },
  SerializedTextNode
>

export class ScriptRefNode extends TextNode {
  __scriptId: string

  static getType(): string {
    return 'scriptRef'
  }

  static clone(node: ScriptRefNode): ScriptRefNode {
    return new ScriptRefNode(node.__scriptId, node.__text, node.__key)
  }

  static importJSON(serializedNode: SerializedScriptRefNode): ScriptRefNode {
    return $createScriptRefNode(serializedNode.scriptId, serializedNode.text).updateFromJSON(serializedNode)
  }

  constructor(scriptId: string, text?: string, key?: NodeKey) {
    super(text ?? tokenFromScriptId(scriptId), key)
    this.__scriptId = normalizeScriptId(scriptId)
  }

  createDOM(config: EditorConfig): HTMLElement {
    const element = super.createDOM(config)
    addClassNamesToElement(element, 'peel-script-chip')
    return element
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

  exportJSON(): SerializedScriptRefNode {
    return {
      ...super.exportJSON(),
      type: 'scriptRef',
      version: 1,
      text: this.getTextContent(),
      scriptId: this.__scriptId,
      refId: `script-ref-${this.getKey()}`,
    }
  }

  setScriptId(scriptId: string): void {
    const writable = this.getWritable()
    writable.__scriptId = normalizeScriptId(scriptId)
    writable.__text = tokenFromScriptId(writable.__scriptId)
  }

  getScriptId(): string {
    return this.getLatest().__scriptId
  }
}

export function $createScriptRefNode(scriptId: string, text?: string): ScriptRefNode {
  return $applyNodeReplacement(new ScriptRefNode(scriptId, text))
}

export function $isScriptRefNode(node: LexicalNode | null | undefined): node is ScriptRefNode {
  return node instanceof ScriptRefNode
}

function normalizeScriptId(scriptId: string): string {
  const trimmed = scriptId.trim()
  if (trimmed.length === 0) {
    return 'SCRIPT_ID'
  }
  return trimmed
}

function tokenFromScriptId(scriptId: string): string {
  return `{{script:${normalizeScriptId(scriptId)}}}`
}
