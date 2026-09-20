import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import FileTreePanel from './FileTreePanel.vue'
import { api } from '@/adapter/client'
import { useWorkspaceSelectionStore } from '@/stores/workspaceSelection'

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
      if (path === '/api/scripts/list') {
        return Promise.resolve({ data: [{ id: 'script-1', name: 'Script' }], error: undefined })
      }
      if (path === '/api/render-config/list-ids') {
        return Promise.resolve({ data: [{ id: 'config-1', name: 'Config' }], error: undefined })
      }
      return Promise.resolve({ data: [{ id: 'document-1', name: 'Document' }], error: undefined })
    })
    mockApi.DELETE.mockResolvedValue({ data: undefined, error: undefined })
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    vi.clearAllMocks()
  })

  it('loads all workspace collections through generated API paths', async () => {
    mount(FileTreePanel)
    await flushPromises()

    expect(mockApi.GET).toHaveBeenCalledWith('/api/scripts/list')
    expect(mockApi.GET).toHaveBeenCalledWith('/api/render-config/list-ids')
    expect(mockApi.GET).toHaveBeenCalledWith('/api/documents')
  })

  it('deletes a render config using only its endpoint and publishes the deletion', async () => {
    const selectionStore = useWorkspaceSelectionStore()
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
    const wrapper = mount(FileTreePanel)
    await flushPromises()

    await wrapper.get('[title="Delete Config"]').trigger('click')

    expect(mockApi.DELETE).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Config')
  })
})
