import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import DocumentPanel from '../DocumentPanel.vue'
import { useDocumentDraftStore } from '@/stores/documentDrafts'
import { useProjectStore } from '@/stores/projects'
import { useProjectFileStore } from '@/stores/projectFiles'
import { setLocale } from '@/i18n'
import { createMemoryHistory, createRouter } from 'vue-router'

const { get, post, put } = vi.hoisted(() => ({ get: vi.fn(), post: vi.fn(), put: vi.fn() }))
vi.mock('@/adapter/client', () => ({ api: { GET: get, POST: post, PUT: put } }))

describe('editing trace blocks', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    setLocale('en')
    vi.resetAllMocks()
    get.mockResolvedValue({ data: ['nachweis', 'detail'] })
    useProjectStore().activeProject = {
      projectIdApi: { id: 'p' },
      scripts: [{ id: 's', name: 'Calculation' }],
    }
    useDocumentDraftStore().setDraft('d', {
      name: 'Report',
      dirty: false,
      renderConfigurationId: '',
      renderConfigurations: [],
      document: {
        type: 'doc',
        content: [
          {
            type: 'peelRepeat',
            attrs: { scope: 'script', scriptId: 's', block: 'nachweis' },
            content: [
              {
                type: 'paragraph',
                content: [
                  { type: 'text', text: 'Bauteil ' },
                  { type: 'peelValue', attrs: { scope: 'current', binding: 'bauteil.name' } },
                ],
              },
              { type: 'peelCalculation', attrs: { scope: 'current', mode: 'detailed' } },
            ],
          },
        ],
      },
    })
  })

  it('edits a real node view and retains its nested template when switching to preview', async () => {
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
    await vi.waitFor(() => expect(wrapper.findAll('.peel-node')).toHaveLength(3))
    expect(wrapper.get('.peel-repeat-content').text()).toContain('Bauteil')
    const calculation = wrapper
      .findAll('.peel-node')
      .find((node) => node.text().startsWith('Calculation'))!
    const display = calculation.findAll('select').at(-1)!
    await display.setValue('compact')
    expect(
      useDocumentDraftStore().getDraft('d')?.document.content?.[0]?.content?.[1]?.attrs?.mode,
    ).toBe('compact')
    expect(useDocumentDraftStore().getDraft('d')?.dirty).toBe(true)
    expect(get).toHaveBeenCalledWith('/api/projects/{projectId}/files/{fileId}/blocks', {
      params: { path: { projectId: 'p', fileId: 's' } },
    })
    post.mockResolvedValue({ error: { error: { message: 'Preview test' } } })
    await wrapper.get('[title="Render preview"]').trigger('click')
    await flushPromises()
    expect(post).toHaveBeenCalledWith(
      '/api/documents/preview',
      expect.objectContaining({
        body: expect.objectContaining({
          document: expect.objectContaining({ type: 'doc' }),
          renderConfigId: '',
        }),
      }),
    )
    await wrapper.get('[aria-label="Document view"] button:first-child').trigger('click')
    expect(wrapper.findAll('.peel-node')).toHaveLength(3)
    wrapper.unmount()
  })

  it('keeps an acknowledged document saved after editor focus and selection transactions', async () => {
    useDocumentDraftStore().getDraft('d')!.document.content!.push({ type: 'paragraph' })
    const files = useProjectFileStore()
    files.loaded.d = {
      id: 'd',
      projectId: { id: 'p' },
      name: 'Report',
      description: '',
      version: 0,
      content: {
        type: 'document',
        document: useDocumentDraftStore().getDraft('d')!.document,
        renderConfigurationId: '',
      },
    }
    put.mockImplementation(async (_path, options) => ({
      data: { ...files.loaded.d, version: 1, content: options.body.content },
    }))
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
    await wrapper.get('[title="Save file"]').trigger('click')
    await flushPromises()
    expect(put).toHaveBeenCalledOnce()
    expect(useDocumentDraftStore().getDraft('d')?.dirty).toBe(false)
    await wrapper.get('.ProseMirror').trigger('focus')
    await wrapper.get('[aria-label="Document view"] button:last-child').trigger('click')
    await wrapper.get('[aria-label="Document view"] button:first-child').trigger('click')
    await flushPromises()
    expect(useDocumentDraftStore().getDraft('d')?.dirty).toBe(false)
    wrapper.unmount()
  })
})
