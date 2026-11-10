import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import DocumentPanel from './DocumentPanel.vue'
import { useDocumentDraftStore } from '@/stores/documentDrafts'
import { useProjectStore } from '@/stores/projects'

const { post } = vi.hoisted(() => ({ post: vi.fn() }))
vi.mock('@/adapter/client', () => ({ api: { POST: post } }))
vi.mock('@tiptap/vue-3', () => ({ useEditor: () => null, EditorContent: { template: '<div />' } }))

describe('DocumentPanel preview project context', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    useDocumentDraftStore().setDraft('document', {
      name: 'Template',
      templateHtml: '<p>Test</p>',
      editorStateJson: '{}',
      scriptNameTags: {},
      renderConfigurationId: 'config',
      renderConfigurations: [],
      dirty: false,
    })
  })

  it('sends the active project with an unsaved document preview', async () => {
    useProjectStore().activeProject = { projectIdApi: { id: 'project' } }
    post.mockResolvedValueOnce({ error: { error: { code: 'not_found' } } })
    const wrapper = mount(DocumentPanel, {
      props: { params: { params: { documentId: 'document' } } },
    })
    await wrapper
      .findAll('button')
      .find((button) => button.text() === 'Render Preview')!
      .trigger('click')
    await flushPromises()
    expect(post).toHaveBeenCalledWith(
      '/api/documents/preview',
      expect.objectContaining({ body: expect.objectContaining({ projectId: { id: 'project' } }) }),
    )
    wrapper.unmount()
  })

  it('does not send a preview without a project', async () => {
    const wrapper = mount(DocumentPanel, {
      props: { params: { params: { documentId: 'document' } } },
    })
    await wrapper
      .findAll('button')
      .find((button) => button.text() === 'Render Preview')!
      .trigger('click')
    expect(post).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Open a project before previewing a document.')
    wrapper.unmount()
  })
})
