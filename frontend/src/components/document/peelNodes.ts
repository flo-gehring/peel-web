import { Node, mergeAttributes } from '@tiptap/core'
import { VueNodeViewRenderer } from '@tiptap/vue-3'
import PeelNodeView from './PeelNodeView.vue'
import PeelTableColumnView from './PeelTableColumnView.vue'

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

export const PeelTable = Node.create({
  name: 'peelTable',
  group: 'block',
  content: 'peelTableColumn+',
  defining: true,
  isolating: true,
  draggable: true,
  addAttributes: attributes,
  parseHTML: () => [{ tag: 'section[data-peel="table"]' }],
  renderHTML: ({ HTMLAttributes }) => [
    'section',
    mergeAttributes(HTMLAttributes, { 'data-peel': 'table' }),
    0,
  ],
  addNodeView: () => VueNodeViewRenderer(PeelNodeView),
})

export const PeelTableColumn = Node.create({
  name: 'peelTableColumn',
  content: '(paragraph | peelCalculation)+',
  defining: true,
  isolating: true,
  addAttributes: () => ({ title: { default: '' }, align: { default: 'left' } }),
  parseHTML: () => [{ tag: 'div[data-peel="column"]' }],
  renderHTML: ({ HTMLAttributes }) => [
    'div',
    mergeAttributes(HTMLAttributes, { 'data-peel': 'column' }),
    0,
  ],
  addNodeView: () => VueNodeViewRenderer(PeelTableColumnView),
})

export const peelNodes = [PeelCalculation, PeelRepeat, PeelValue, PeelTable, PeelTableColumn]
