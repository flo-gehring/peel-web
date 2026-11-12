import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import DocumentPanel from './DocumentPanel.vue'
import { useDocumentDraftStore } from '@/stores/documentDrafts'
import { useProjectStore } from '@/stores/projects'
import { useProjectFileStore } from '@/stores/projectFiles'

const { post, put } = vi.hoisted(() => ({ post: vi.fn(), put: vi.fn() }))
vi.mock('@/adapter/client', () => ({ api: { POST: post, PUT: put } }))
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

  it('saves document references and editor state using the acknowledged file version', async () => {
    useProjectStore().activeProject = { projectIdApi: { id: 'project' } }
    const files = useProjectFileStore()
    files.loaded.document = {
      projectId: { id: 'project' },
      version: 5,
      name: 'Template',
      description: 'Purpose',
      content: {
        type: 'document',
        scriptNameTags: {},
        templateHtml: '<p>Test</p>',
        editorStateJson: '{}',
        renderConfigurationId: 'config',
      },
    }
    useDocumentDraftStore().updateDraft('document', { scriptNameTags: { calculation: 'script' } })
    put.mockResolvedValueOnce({ data: { ...files.loaded.document, version: 6 } })
    const wrapper = mount(DocumentPanel, {
      props: { params: { params: { documentId: 'document' } } },
    })
    await wrapper
      .findAll('button')
      .find((button) => button.text() === 'Save')!
      .trigger('click')
    await flushPromises()
    expect(put).toHaveBeenCalledWith('/api/projects/{projectId}/files/{fileId}', {
      params: { path: { projectId: 'project', fileId: 'document' } },
      body: {
        expectedVersion: 5,
        name: 'Template',
        description: 'Purpose',
        content: {
          type: 'document',
          scriptNameTags: { calculation: 'script' },
          templateHtml: '<p>Test</p>',
          editorStateJson: '{}',
          renderConfigurationId: 'config',
        },
      },
    })
    expect(files.loaded.document?.version).toBe(6)
    expect(useDocumentDraftStore().getDraft('document')?.dirty).toBe(false)
    wrapper.unmount()
  })
})
