import type { JSONContent } from '@tiptap/core'
import type { components } from '@/types/api'

export type DocumentNode = components['schemas']['DocumentNode']
export function emptyDocument(): DocumentNode {
  return { type: 'doc', content: [{ type: 'paragraph' }] }
}
export function documentFromEditor(node: JSONContent): DocumentNode {
  if (!node.type) throw new Error('Document nodes need a type')
  return {
    type: node.type,
    ...(node.attrs ? { attrs: node.attrs } : {}),
    ...(node.text !== undefined ? { text: node.text } : {}),
    ...(node.content ? { content: node.content.map(documentFromEditor) } : {}),
    ...(node.marks
      ? {
          marks: node.marks.map((mark) => ({
            type: mark.type,
            ...(mark.attrs ? { attrs: mark.attrs } : {}),
          })),
        }
      : {}),
  }
}
