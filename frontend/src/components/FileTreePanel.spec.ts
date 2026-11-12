import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import FileTreePanel from './FileTreePanel.vue'
import { api } from '@/adapter/client'
import { useWorkspaceSelectionStore } from '@/stores/workspaceSelection'
import { useProjectStore } from '@/stores/projects'

vi.mock('@/adapter/client', () => ({
  api: {
    GET: vi.fn(),
    POST: vi.fn(),
    PUT: vi.fn(),
    DELETE: vi.fn(),
  },
}))

const mockApi = vi.mocked(api)

describe('FileTreePanel', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.stubGlobal(
      'confirm',
      vi.fn(() => true),
    )
    mockApi.GET.mockImplementation((path) => {
      if (path === '/api/projects/{projectId}') {
        return Promise.resolve({
          data: {
            projectIdApi: { id: 'project-1' },
            scripts: [{ id: 'script-1', name: 'Script' }],
            renderConfigurations: [{ id: 'config-1', name: 'Config' }],
            documentTemplates: [{ id: 'document-1', name: 'Document' }],
          },
          error: undefined,
        })
      }
      if (path === '/api/projects/{projectId}/files')
        return Promise.resolve({
          data: [
            { id: 'script-1', name: 'Script', type: 'script', version: 2 },
            { id: 'config-1', name: 'Config', type: 'render-config', version: 3 },
            { id: 'document-1', name: 'Document', type: 'document', version: 0 },
          ],
        })
      return Promise.resolve({ data: undefined, error: undefined })
    })
    mockApi.DELETE.mockResolvedValue({ data: undefined, error: undefined })
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    vi.clearAllMocks()
  })

  it('loads workspace collections through the active project', async () => {
    const projectStore = useProjectStore()
    projectStore.activeProject = { projectIdApi: { id: 'project-1' } }
    mount(FileTreePanel)
    await flushPromises()

    expect(mockApi.GET).toHaveBeenCalledWith('/api/projects/{projectId}', {
      params: { path: { projectId: 'project-1' } },
    })
  })

  it('deletes with the last known version and publishes the deletion', async () => {
    const selectionStore = useWorkspaceSelectionStore()
    const projectStore = useProjectStore()
    projectStore.activeProject = { projectIdApi: { id: 'project-1' } }
    const deletionSpy = vi.spyOn(selectionStore, 'deleteDocument')
    const wrapper = mount(FileTreePanel)
    await flushPromises()

    await wrapper.get('.document-section:nth-of-type(2) [title="Delete"]').trigger('click')
    await flushPromises()

    expect(mockApi.DELETE).toHaveBeenCalledTimes(1)
    expect(mockApi.DELETE).toHaveBeenCalledWith('/api/projects/{projectId}/files/{fileId}', {
      params: {
        path: { projectId: 'project-1', fileId: 'config-1' },
        query: { expectedVersion: 3 },
      },
    })
    expect(wrapper.text()).not.toContain('Config')
    expect(wrapper.text()).toContain('Document')
    expect(deletionSpy).toHaveBeenCalledWith({
      kind: 'renderConfig',
      id: 'config-1',
      name: 'Config',
      icon: '{}',
    })
  })

  it('does not call any endpoint when render-config deletion is cancelled', async () => {
    vi.stubGlobal(
      'confirm',
      vi.fn(() => false),
    )
    const projectStore = useProjectStore()
    projectStore.activeProject = { projectIdApi: { id: 'project-1' } }
    const wrapper = mount(FileTreePanel)
    await flushPromises()

    await wrapper.get('.document-section:nth-of-type(2) [title="Delete"]').trigger('click')

    expect(mockApi.DELETE).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Config')
  })

  it('retries creation with the same ID and payload after a network failure', async () => {
    useProjectStore().activeProject = { projectIdApi: { id: 'project-1' } }
    mockApi.POST.mockRejectedValueOnce(new TypeError('Connection lost'))
    mockApi.POST.mockImplementationOnce(
      async (_path: unknown, options: unknown) =>
        ({
          data: {
            ...(options as { body: Record<string, unknown> }).body,
            projectId: { id: 'project-1' },
            version: 0,
          },
        }) as never,
    )
    const wrapper = mount(FileTreePanel)
    await flushPromises()
    await wrapper.get('[title="New script"]').trigger('click')
    await wrapper.get('#new-document-name').setValue('Created')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(wrapper.text()).toContain('Connection lost')
    expect(wrapper.get('#new-document-name').attributes('disabled')).toBeDefined()
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(mockApi.POST).toHaveBeenCalledTimes(2)
    expect(mockApi.POST.mock.calls[0]).toEqual(mockApi.POST.mock.calls[1])
    expect(mockApi.POST.mock.calls[0]?.[0]).toBe('/api/projects/{projectId}/files')
    expect(useWorkspaceSelectionStore().selectedDocument?.name).toBe('Created')
  })

  it('shows blocking usages instead of removing a referenced file', async () => {
    useProjectStore().activeProject = { projectIdApi: { id: 'project-1' } }
    mockApi.DELETE.mockResolvedValueOnce({
      error: { error: { message: 'File in use.', details: { usages: [{ name: 'Invoice' }] } } },
    } as never)
    const wrapper = mount(FileTreePanel)
    await flushPromises()
    await wrapper.get('.document-section:nth-of-type(2) [title="Delete"]').trigger('click')
    await flushPromises()
    expect(wrapper.get('[role="alert"]').text()).toContain('Invoice')
    expect(wrapper.text()).toContain('Config')
    expect(useWorkspaceSelectionStore().deletedDocument).toBeNull()
  })

  it('collapses groups independently without selecting a file', async () => {
    useProjectStore().activeProject = { projectIdApi: { id: 'project-1' } }
    const wrapper = mount(FileTreePanel)
    await flushPromises()
    const toggle = wrapper.get('[aria-controls="files-peel"]')
    await toggle.trigger('click')
    expect(toggle.attributes('aria-expanded')).toBe('false')
    expect((wrapper.get('#files-peel').element as HTMLElement).style.display).toBe('none')
    expect((wrapper.get('#files-renderConfig').element as HTMLElement).style.display).not.toBe(
      'none',
    )
    expect(useWorkspaceSelectionStore().selectedDocument).toBeNull()
    await toggle.trigger('click')
    expect((wrapper.get('#files-peel').element as HTMLElement).style.display).not.toBe('none')
  })

  it.each(['New script', 'New render configuration', 'New document'])(
    'opens the matching creation form from %s',
    async (title) => {
      useProjectStore().activeProject = { projectIdApi: { id: 'project-1' } }
      const wrapper = mount(FileTreePanel)
      await flushPromises()
      await wrapper.get(`[title="${title}"]`).trigger('click')
      expect(wrapper.get('.create-dialog h2').text()).toBe(title)
      expect(wrapper.find('#workspace-item-type').exists()).toBe(false)
      expect(wrapper.find('#new-document-render-config').exists()).toBe(title === 'New document')
    },
  )
})
