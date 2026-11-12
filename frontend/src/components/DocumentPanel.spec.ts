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
      .find((button) => button.attributes('title') === 'Render preview')!
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
      .find((button) => button.attributes('title') === 'Render preview')!
      .trigger('click')
    expect(post).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Open a project before previewing a document.')
    wrapper.unmount()
  })
  it('previews with Ctrl+Enter without saving the document', async () => {
    useProjectStore().activeProject = { projectIdApi: { id: 'project' } }
    post.mockResolvedValueOnce({ error: { error: { message: 'Preview failed' } } })
    const wrapper = mount(DocumentPanel, {
      props: { params: { params: { documentId: 'document' } } },
    })
    await wrapper.get('.document-panel').trigger('keydown', { key: 'Enter', ctrlKey: true })
    await flushPromises()
    expect(post).toHaveBeenCalledWith('/api/documents/preview', expect.anything())
    expect(put).not.toHaveBeenCalled()
    expect(wrapper.get('.render-preview-panel').text()).toContain('Preview failed')
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

  it('opens settings sideways and switches views without discarding the document draft', async () => {
    const wrapper = mount(DocumentPanel, {
      props: { params: { params: { documentId: 'document' } } },
    })
    expect(wrapper.find('aside.file-settings').exists()).toBe(false)
    await wrapper.get('[title="File settings"]').trigger('click')
    expect(wrapper.get('aside.file-settings').text()).not.toContain('Script Links')
    await wrapper.get('[title="Script links"]').trigger('click')
    expect(wrapper.get('aside.file-settings').attributes('aria-label')).toBe('Script links')
    expect(wrapper.find('[aria-label="File settings"][aria-expanded="true"]').exists()).toBe(false)
    expect(wrapper.find('#file-name-document').exists()).toBe(false)
    await wrapper.get('[title="Render configuration"]').trigger('click')
    expect(wrapper.get('aside.file-settings').attributes('aria-label')).toBe('Render configuration')
    expect(wrapper.find('.render-config-field').exists()).toBe(true)
    expect(wrapper.find('.script-link-row').exists()).toBe(false)
    expect(wrapper.find('.document-actions').exists()).toBe(false)
    await wrapper.get('[aria-label="Document view"] button:last-child').trigger('click')
    expect((wrapper.get('.document-edit').element as HTMLElement).style.display).toBe('none')
    expect(wrapper.find('.render-preview-panel').exists()).toBe(true)
    await wrapper.get('[aria-label="Document view"] button:first-child').trigger('click')
    expect((wrapper.get('.document-edit').element as HTMLElement).style.display).not.toBe('none')
    expect(useDocumentDraftStore().getDraft('document')?.templateHtml).toBe('<p>Test</p>')
    wrapper.unmount()
  })

  it('handles Ctrl+S in the editor and reports metadata changes as unsaved', async () => {
    useProjectStore().activeProject = { projectIdApi: { id: 'project' } }
    const files = useProjectFileStore()
    files.loaded.document = {
      projectId: { id: 'project' },
      version: 0,
      name: 'Old',
      description: '',
      content: {
        type: 'document',
        scriptNameTags: {},
        templateHtml: '<p>Test</p>',
        editorStateJson: '{}',
        renderConfigurationId: 'config',
      },
    }
    const wrapper = mount(DocumentPanel, {
      props: { params: { params: { documentId: 'document' } } },
    })
    await wrapper.get('[title="File settings"]').trigger('click')
    await wrapper.get('#file-name-document').setValue('Renamed')
    expect(wrapper.get('.save-status').text()).toBe('Unsaved changes')
    put.mockResolvedValueOnce({ data: { ...files.loaded.document, name: 'Renamed', version: 1 } })
    await wrapper.get('.document-panel').trigger('keydown', { key: 's', ctrlKey: true })
    await flushPromises()
    expect(put).toHaveBeenCalledWith(
      '/api/projects/{projectId}/files/{fileId}',
      expect.objectContaining({
        body: expect.objectContaining({ name: 'Renamed', expectedVersion: 0 }),
      }),
    )
    expect(wrapper.get('.save-status').text()).toBe('Saved')
    wrapper.unmount()
  })
})
