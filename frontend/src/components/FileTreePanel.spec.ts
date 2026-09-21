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
    vi.stubGlobal('confirm', vi.fn(() => true))
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

  it('deletes a render config using only its endpoint and publishes the deletion', async () => {
    const selectionStore = useWorkspaceSelectionStore()
    const projectStore = useProjectStore()
    projectStore.activeProject = { projectIdApi: { id: 'project-1' } }
    const deletionSpy = vi.spyOn(selectionStore, 'deleteDocument')
    const wrapper = mount(FileTreePanel)
    await flushPromises()

    await wrapper.get('[title="Delete Config"]').trigger('click')
    await flushPromises()

    expect(mockApi.DELETE).toHaveBeenCalledTimes(1)
    expect(mockApi.DELETE).toHaveBeenCalledWith('/api/render-config/{id}', {
      params: { path: { id: 'config-1' } },
    })
    expect(wrapper.text()).not.toContain('Config')
    expect(wrapper.text()).toContain('Document')
    expect(deletionSpy).toHaveBeenCalledWith({ kind: 'renderConfig', id: 'config-1', name: 'Config', icon: '{}' })
  })

  it('does not call any endpoint when render-config deletion is cancelled', async () => {
    vi.stubGlobal('confirm', vi.fn(() => false))
    const projectStore = useProjectStore()
    projectStore.activeProject = { projectIdApi: { id: 'project-1' } }
    const wrapper = mount(FileTreePanel)
    await flushPromises()

    await wrapper.get('[title="Delete Config"]').trigger('click')

    expect(mockApi.DELETE).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Config')
  })
})
