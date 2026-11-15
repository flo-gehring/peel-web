import { Node, mergeAttributes } from '@tiptap/core'
import { VueNodeViewRenderer } from '@tiptap/vue-3'
import PeelNodeView from './PeelNodeView.vue'

const attributes = () => ({
  scriptId: { default: '' },
  block: { default: '' },
  scope: { default: 'script' },
  mode: { default: 'detailed' },
  binding: { default: '' },
  decimals: {
    default: null,
    parseHTML: (element: HTMLElement) => {
      const raw = element.getAttribute('decimals')
      if (raw === null || raw === '') return null
      const value = Number(raw)
      return Number.isInteger(value) && value >= 0 && value <= 12 ? value : null
    },
  },
})

export const PeelCalculation = Node.create({
  name: 'peelCalculation',
  group: 'block',
  atom: true,
  selectable: true,
  draggable: true,
  addAttributes: attributes,
  parseHTML: () => [{ tag: 'div[data-peel="calculation"]' }],
  renderHTML: ({ HTMLAttributes }) => [
    'div',
    mergeAttributes(HTMLAttributes, { 'data-peel': 'calculation' }),
  ],
  addNodeView: () => VueNodeViewRenderer(PeelNodeView),
})

export const PeelRepeat = Node.create({
  name: 'peelRepeat',
  group: 'block',
  content: 'block+',
  defining: true,
  isolating: true,
  selectable: true,
  draggable: true,
  addAttributes: attributes,
  parseHTML: () => [{ tag: 'section[data-peel="repeat"]' }],
  renderHTML: ({ HTMLAttributes }) => [
    'section',
    mergeAttributes(HTMLAttributes, { 'data-peel': 'repeat' }),
    0,
  ],
  addNodeView: () => VueNodeViewRenderer(PeelNodeView),
})

export const PeelValue = Node.create({
  name: 'peelValue',
  group: 'inline',
  inline: true,
  atom: true,
  selectable: true,
  addAttributes: attributes,
  parseHTML: () => [{ tag: 'span[data-peel="value"]' }],
  renderHTML: ({ HTMLAttributes }) => [
    'span',
    mergeAttributes(HTMLAttributes, { 'data-peel': 'value' }),
  ],
  addNodeView: () => VueNodeViewRenderer(PeelNodeView),
})

export const peelNodes = [PeelCalculation, PeelRepeat, PeelValue]
