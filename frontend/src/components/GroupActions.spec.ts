import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import GroupActions from './GroupActions.vue'
import { api } from '@/adapter/client'
import { useEditorDraftStore } from '@/stores/editorDrafts'
import { useRunBindingsStore } from '@/stores/runBindings'
import { useRunOutputStore } from '@/stores/runOutput'
import { useProjectStore } from '@/stores/projects'
import { useProjectFileStore } from '@/stores/projectFiles'

vi.mock('@/adapter/client', () => ({
  api: {
    GET: vi.fn(),
    POST: vi.fn(),
    PUT: vi.fn(),
    DELETE: vi.fn(),
  },
}))

const mockApi = vi.mocked(api)

function mountActions(documentKind: 'peel' | 'renderConfig' = 'peel') {
  const outputPanel = { api: { setActive: vi.fn() } }
  const params = {
    activePanel: { id: 'editor-script-1', api: { setTitle: vi.fn() } },
    containerApi: {
      toJSON: () => ({
        panels: {
          'editor-script-1': {
            title: 'Example',
            params: { documentId: 'script-1', documentKind, content: 'saved script' },
          },
        },
      }),
      getPanel: vi.fn(() => outputPanel),
    },
  }
  return { wrapper: mount(GroupActions, { props: { params: params as never } }), outputPanel }
}

describe('GroupActions', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    setActivePinia(createPinia())
    useProjectStore().activeProject = { projectIdApi: { id: 'project' }, state: 'ACTIVE' }
    mockApi.PUT.mockResolvedValue({
      data: { trace: { step: 1 }, result: { value: 1 } },
      error: undefined,
    })
  })

  it('runs the latest draft through the generated script-run endpoint', async () => {
    const draftStore = useEditorDraftStore()
    const bindingsStore = useRunBindingsStore()
    const outputStore = useRunOutputStore()
    draftStore.setDraft('script-1', 'unsaved script')
    bindingsStore.setRawJson('{"amount":5}')
    const { wrapper, outputPanel } = mountActions()

    await wrapper.get('[title="Run script"]').trigger('click')
    await flushPromises()

    expect(mockApi.PUT).toHaveBeenCalledWith('/api/scripts/run', {
      body: { projectId: { id: 'project' }, script: 'unsaved script', bindings: { amount: 5 } },
    })
    expect(outputStore.response).toEqual({ trace: { step: 1 }, result: { value: 1 } })
    expect(outputPanel.api.setActive).toHaveBeenCalled()
  })

  it('does not execute with invalid bindings and opens output', async () => {
    const bindingsStore = useRunBindingsStore()
    bindingsStore.setRawJson('[]')
    const { wrapper, outputPanel } = mountActions()

    await wrapper.get('[title="Run script"]').trigger('click')

    expect(mockApi.PUT).not.toHaveBeenCalled()
    expect(useRunOutputStore().error).toBe('Bindings JSON must be an object at the top level.')
    expect(outputPanel.api.setActive).toHaveBeenCalled()
  })

  it('does not show Run for render configurations', () => {
    const { wrapper } = mountActions('renderConfig')

    expect(wrapper.find('[title="Run script"]').exists()).toBe(false)
  })

  it('saves using the acknowledged version and preserves metadata', async () => {
    const files = useProjectFileStore()
    files.loaded['script-1'] = {
      id: 'script-1',
      projectId: { id: 'project' },
      version: 7,
      name: 'Example',
      description: 'Purpose',
      content: { type: 'script', source: 'old' },
    }
    const drafts = useEditorDraftStore()
    drafts.setDraft('script-1', 'new')
    mockApi.PUT.mockResolvedValueOnce({ data: { ...files.loaded['script-1'], version: 8 } })
    const { wrapper } = mountActions()
    await wrapper.get('[title="Save file"]').trigger('click')
    await flushPromises()
    expect(mockApi.PUT).toHaveBeenCalledWith('/api/projects/{projectId}/files/{fileId}', {
      params: { path: { projectId: 'project', fileId: 'script-1' } },
      body: {
        expectedVersion: 7,
        name: 'Example',
        description: 'Purpose',
        content: { type: 'script', source: 'new' },
      },
    })
    expect(drafts.draftsById['script-1']?.dirty).toBe(false)
  })

  it('shows a conflict and retains the unsaved draft', async () => {
    const files = useProjectFileStore()
    files.loaded['script-1'] = {
      projectId: { id: 'project' },
      version: 7,
      name: 'Example',
      content: { type: 'script', source: 'old' },
    }
    useEditorDraftStore().setDraft('script-1', 'new')
    mockApi.PUT.mockResolvedValueOnce({
      error: { error: { message: 'The file changed. Reload and merge.' } },
    } as never)
    const { wrapper } = mountActions()
    await wrapper.get('[title="Save file"]').trigger('click')
    await flushPromises()
    expect(wrapper.get('[role="alertdialog"]').text()).toContain('Reload and merge')
    expect(useEditorDraftStore().draftsById['script-1']?.dirty).toBe(true)
    expect(files.loaded['script-1']?.version).toBe(7)
  })

  it('keeps edits made during a save dirty', async () => {
    const files = useProjectFileStore()
    files.loaded['script-1'] = {
      projectId: { id: 'project' },
      version: 7,
      name: 'Example',
      content: { type: 'script', source: 'old' },
    }
    const drafts = useEditorDraftStore()
    drafts.setDraft('script-1', 'sent')
    let finish!: (value: never) => void
    mockApi.PUT.mockReturnValueOnce(new Promise((resolve) => (finish = resolve)))
    const { wrapper } = mountActions()
    await wrapper.get('[title="Save file"]').trigger('click')
    drafts.setDraft('script-1', 'typed while saving')
    finish({ data: { ...files.loaded['script-1'], version: 8 } } as never)
    await flushPromises()
    expect(drafts.getDraft('script-1')).toBe('typed while saving')
    expect(drafts.draftsById['script-1']?.dirty).toBe(true)
    expect(files.loaded['script-1']?.version).toBe(8)
  })

  it('saves structured render configurations through the same versioned API', async () => {
    const files = useProjectFileStore()
    const configuration = {
      renderConfigurations: { LITERAL: '{{ valueText }}' },
      namedOverrides: {},
    }
    files.loaded['script-1'] = {
      projectId: { id: 'project' },
      version: 4,
      name: 'Config',
      content: { type: 'render-config', ...configuration },
    }
    useEditorDraftStore().setDraft('script-1', JSON.stringify(configuration))
    mockApi.PUT.mockResolvedValueOnce({ data: files.loaded['script-1'] })
    const { wrapper } = mountActions('renderConfig')
    await wrapper.get('[title="Save file"]').trigger('click')
    await flushPromises()
    expect(mockApi.PUT).toHaveBeenCalledWith('/api/projects/{projectId}/files/{fileId}', {
      params: { path: { projectId: 'project', fileId: 'script-1' } },
      body: {
        expectedVersion: 4,
        name: 'Config',
        description: '',
        content: { type: 'render-config', ...configuration },
      },
    })
    expect(useEditorDraftStore().draftsById['script-1']?.dirty).toBe(false)
  })
})
