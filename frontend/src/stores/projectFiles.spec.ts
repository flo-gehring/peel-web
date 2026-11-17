import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { api } from '@/adapter/client'
import { useProjectFileStore, type ProjectFile } from './projectFiles'

vi.mock('@/adapter/client', () => ({
  api: { GET: vi.fn(), POST: vi.fn(), PUT: vi.fn(), DELETE: vi.fn() },
}))
const mockApi = vi.mocked(api)
const state = {
  name: 'Script',
  description: 'Purpose',
  content: { type: 'script' as const, source: '1', expectedBindings: [] },
}
const initial: ProjectFile = { id: 'file', projectId: { id: 'project' }, version: 7, ...state }

describe('project file requests', () => {
  it('keeps binding edits made during a save dirty and visible to navigation', async () => {
    setActivePinia(createPinia())
    const store = useProjectFileStore()
    store.loaded.file = initial
    store.setExpectedBindings('file', [{ name: 'first', description: '' }])
    const content = store.scriptContent('file', 'draft')
    let acknowledge!: (value: unknown) => void
    mockApi.PUT.mockReturnValueOnce(
      new Promise((resolve) => {
        acknowledge = resolve
      }) as never,
    )
    const saving = store.save('project', 'file', { ...state, content })
    store.setExpectedBindings('file', [{ name: 'second', description: '' }])
    acknowledge({ data: { ...initial, content, version: 8 } })
    await saving
    expect(store.expectedBindings('file')[0]?.name).toBe('second')
    expect(store.metadataDirty('file')).toBe(true)
    expect(Object.keys(store.metadataDrafts)).toContain('file')
    expect(store.loaded.file?.version).toBe(8)
  })

  it('preserves the contract draft and baseline on conflict, then clears them on explicit reload', async () => {
    setActivePinia(createPinia())
    const store = useProjectFileStore()
    store.loaded.file = initial
    store.setExpectedBindings('file', [{ name: 'input', description: 'Required' }])
    mockApi.PUT.mockResolvedValueOnce({ error: { error: { message: 'Conflict' } } } as never)
    await expect(
      store.save('project', 'file', { ...state, content: store.scriptContent('file', 'draft') }),
    ).rejects.toThrow()
    expect(store.loaded.file?.version).toBe(7)
    expect(store.metadataDirty('file')).toBe(true)
    mockApi.GET.mockResolvedValueOnce({ data: initial } as never)
    await store.load('project', 'file', true)
    expect(store.expectedBindings('file')).toEqual([])
    expect(store.metadataDirty('file')).toBe(false)
  })
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.resetAllMocks()
  })

  it('retries the original version after a lost response and advances only after acknowledgement', async () => {
    const store = useProjectFileStore()
    store.loaded.file = initial
    const changed = {
      ...state,
      content: { type: 'script' as const, source: '2', expectedBindings: [] },
    }
    mockApi.PUT.mockRejectedValueOnce(new TypeError('Network failed'))
    mockApi.PUT.mockResolvedValueOnce({ data: { ...initial, ...changed, version: 8 } })
    await expect(store.save('project', 'file', changed)).rejects.toThrow('Network failed')
    expect(store.loaded.file?.version).toBe(7)
    await store.save('project', 'file', changed)
    for (const call of mockApi.PUT.mock.calls)
      expect(call).toEqual([
        '/api/projects/{projectId}/files/{fileId}',
        {
          params: { path: { projectId: 'project', fileId: 'file' } },
          body: { expectedVersion: 7, ...changed },
        },
      ])
    expect(store.loaded.file?.version).toBe(8)
    expect(mockApi.GET).not.toHaveBeenCalled()
  })

  it('keeps the baseline on a conflict and never silently rebases', async () => {
    const store = useProjectFileStore()
    store.loaded.file = initial
    mockApi.PUT.mockResolvedValue({
      error: {
        error: {
          code: 'file_version_conflict',
          message: 'File changed',
          details: { currentVersion: 9 },
        },
      },
    } as never)
    await expect(store.save('project', 'file', state)).rejects.toThrow('The file changed')
    expect(store.loaded.file).toEqual(initial)
    expect(mockApi.GET).not.toHaveBeenCalled()
  })

  it('does not use a freshly fetched version to save an unknown draft', async () => {
    await expect(useProjectFileStore().save('project', 'file', state)).rejects.toThrow(
      'original version',
    )
    expect(mockApi.GET).not.toHaveBeenCalled()
    expect(mockApi.PUT).not.toHaveBeenCalled()
  })

  it('uses explicit creation semantics with a caller-supplied ID', async () => {
    mockApi.POST.mockResolvedValueOnce({ data: { ...initial, version: 0 } })
    await useProjectFileStore().create('project', 'file', state)
    expect(mockApi.POST).toHaveBeenCalledWith('/api/projects/{projectId}/files', {
      params: { path: { projectId: 'project' } },
      body: { id: 'file', ...state },
    })
    expect(mockApi.PUT).not.toHaveBeenCalled()
  })

  it('reports blocking references on delete without losing the loaded file', async () => {
    const store = useProjectFileStore()
    store.loaded.file = initial
    mockApi.DELETE.mockResolvedValueOnce({
      error: { error: { message: 'File in use.', details: { usages: [{ name: 'Invoice' }] } } },
    } as never)
    await expect(store.remove('project', 'file', 7)).rejects.toThrow('Invoice')
    expect(store.loaded.file).toEqual(initial)
    expect(mockApi.DELETE).toHaveBeenCalledWith('/api/projects/{projectId}/files/{fileId}', {
      params: { path: { projectId: 'project', fileId: 'file' }, query: { expectedVersion: 7 } },
    })
  })

  it('shares concurrent initial loads so late reads cannot change a draft baseline', async () => {
    let finish!: (value: never) => void
    mockApi.GET.mockReturnValueOnce(new Promise((resolve) => (finish = resolve)))
    const store = useProjectFileStore()
    const first = store.load('project', 'file')
    const second = store.load('project', 'file')
    expect(mockApi.GET).toHaveBeenCalledTimes(1)
    finish({ data: initial } as never)
    expect(await first).toEqual(initial)
    expect(await second).toEqual(initial)
    expect(await store.load('project', 'file')).toEqual(initial)
    expect(mockApi.GET).toHaveBeenCalledTimes(1)
  })

  it('does not restore loaded files after the workspace was cleared', async () => {
    let finish!: (value: never) => void
    mockApi.GET.mockReturnValueOnce(new Promise((resolve) => (finish = resolve)))
    const store = useProjectFileStore()
    const pending = store.load('project', 'file')
    store.clear()
    finish({ data: initial } as never)
    await expect(pending).rejects.toThrow('workspace changed')
    expect(store.loaded).toEqual({})
  })
})
