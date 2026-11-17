import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { getSchema, generateHTML, generateJSON, type JSONContent } from '@tiptap/core'
import StarterKit from '@tiptap/starter-kit'
import { createMemoryHistory, createRouter } from 'vue-router'
import { peelNodes } from './peelNodes'
import DocumentPanel from '../DocumentPanel.vue'
import { EditorContent } from '@tiptap/vue-3'
import { useProjectStore } from '@/stores/projects'
import { useDocumentDraftStore } from '@/stores/documentDrafts'
import { documentFromEditor } from '@/adapter/documentModel'
import { setLocale } from '@/i18n'

const { get } = vi.hoisted(() => ({ get: vi.fn() }))
vi.mock('@/adapter/client', () => ({ api: { GET: get } }))
const column = (title: string, binding: string): JSONContent => ({
  type: 'peelTableColumn',
  attrs: { title, align: 'right' },
  content: [
    {
      type: 'paragraph',
      content: [{ type: 'peelValue', attrs: { scope: 'current', binding, decimals: 2 } }],
    },
  ],
})
const tableDocument = (): JSONContent => ({
  type: 'doc',
  content: [
    {
      type: 'peelTable',
      attrs: { scope: 'script', scriptId: 's', block: 'row' },
      content: [column('A', 'a'), column('B', 'b')],
    },
  ],
})

describe('trace tables', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    setLocale('en')
    get.mockReset().mockResolvedValue({
      data: [
        { name: 'row', reachableBlocks: ['row', 'detail'] },
        { name: 'detail', reachableBlocks: ['detail'] },
        { name: 'outside', reachableBlocks: ['outside'] },
      ],
    })
    useProjectStore().activeProject = {
      projectIdApi: { id: 'p' },
      scripts: [{ id: 's', name: 'Script' }],
    }
    useDocumentDraftStore().setDraft('d', {
      name: 'Table',
      dirty: false,
      renderConfigurationId: '',
      renderConfigurations: [],
      document: documentFromEditor(tableDocument()),
    })
  })
  async function open() {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div />' } }],
    })
    await router.push('/')
    const wrapper = mount(DocumentPanel, {
      props: { params: { params: { documentId: 'd' } } },
      global: { plugins: [router] },
    })
    await flushPromises()
    return wrapper
  }
  const savedTable = () => useDocumentDraftStore().getDraft('d')!.document.content![0]!

  it('round-trips the schema, JSON and clipboard with column order, headings and cell references', () => {
    const extensions = [StarterKit, ...peelNodes]
    const schema = getSchema(extensions)
    const source = schema.nodeFromJSON(tableDocument())
    source.check()
    expect(schema.nodeFromJSON(documentFromEditor(source.toJSON())).eq(source)).toBe(true)
    const copied = schema.nodeFromJSON(
      generateJSON(generateHTML(source.toJSON(), extensions), extensions),
    )
    copied.check()
    expect(copied.eq(source)).toBe(true)
    expect(() =>
      schema.nodeFromJSON({ type: 'doc', content: [column('X', 'x')] }).check(),
    ).toThrow()
    const invalid = tableDocument()
    invalid.content![0]!.content![0]!.content = [tableDocument().content![0]!]
    expect(() => schema.nodeFromJSON(invalid).check()).toThrow()
  })

  it('edits and reorders columns, prevents removing the last column, and supports undo', async () => {
    const wrapper = await open()
    await vi.waitFor(() => expect(wrapper.findAll('.peel-table-column')).toHaveLength(2))
    const editor = wrapper.findComponent(EditorContent).props('editor')!
    await wrapper.findAll('.column-settings input')[0]!.setValue('Year')
    await wrapper.findAll('.column-settings select')[0]!.setValue('center')
    await wrapper.get('[aria-label="Move column right"]').trigger('click')
    await flushPromises()
    expect(savedTable().content?.map((col) => col.attrs?.title)).toEqual(['B', 'Year'])
    expect(savedTable().content?.[1]?.attrs?.align).toBe('center')
    expect(savedTable().content?.[1]?.content?.[0]?.content?.[0]?.attrs?.binding).toBe('a')
    await wrapper.findAll('[aria-label="Remove column"]')[0]!.trigger('click')
    await flushPromises()
    expect(savedTable().content).toHaveLength(1)
    expect(wrapper.get('[aria-label="Remove column"]').attributes()).toHaveProperty('disabled')
    editor.commands.undo()
    await flushPromises()
    expect(savedTable().content).toHaveLength(2)
    await wrapper.get('.peel-node-settings > button').trigger('click')
    await flushPromises()
    expect(savedTable().content).toHaveLength(3)
    wrapper.unmount()
  })

  it('uses the table as relative context and disallows table/repeat insertion in cells', async () => {
    const wrapper = await open()
    const editor = wrapper.findComponent(EditorContent).props('editor')!
    let pos = 0
    editor.state.doc.descendants((node, at) => {
      if (node.type.name === 'peelTableColumn' && !pos) pos = at
    })
    editor.commands.setTextSelection(pos + 2)
    await flushPromises()
    await wrapper.get('[title="Script links"]').trigger('click')
    const choices = wrapper.findAll('.trace-insert button')
    expect(choices.find((button) => button.text() === 'Repetition')!.attributes()).toHaveProperty(
      'disabled',
    )
    expect(
      choices.find((button) => button.text() === 'Table from repetition')!.attributes(),
    ).toHaveProperty('disabled')
    const valueNode = wrapper.findAll('.peel-inline')[0]!
    const blocks = valueNode
      .findAll('label')
      .find((label) => label.text().startsWith('Block'))!
      .get('select')
    expect(blocks.find('option[value="detail"]').exists()).toBe(true)
    expect(blocks.find('option[value="outside"]').exists()).toBe(false)
    wrapper.unmount()
  })

  it('inserts a new table and closes the picker after successful insertion', async () => {
    useDocumentDraftStore().setDraft('d', {
      name: 'New',
      dirty: false,
      renderConfigurationId: '',
      renderConfigurations: [],
      document: { type: 'doc', content: [{ type: 'paragraph' }] },
    })
    const wrapper = await open()
    await wrapper.get('[title="Script links"]').trigger('click')
    await wrapper
      .findAll('.trace-insert button')
      .find((button) => button.text() === 'Table from repetition')!
      .trigger('click')
    await flushPromises()
    expect(wrapper.find('.trace-insert').exists()).toBe(false)
    expect(wrapper.findAll('.peel-table-column')).toHaveLength(2)
    wrapper.unmount()
  })
})
