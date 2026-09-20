import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import GroupActions from './GroupActions.vue'
import { api } from '@/adapter/client'
import { useEditorDraftStore } from '@/stores/editorDrafts'
import { useRunBindingsStore } from '@/stores/runBindings'
import { useRunOutputStore } from '@/stores/runOutput'

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
    setActivePinia(createPinia())
    mockApi.PUT.mockResolvedValue({ data: { trace: { step: 1 }, result: { value: 1 } }, error: undefined })
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
      body: { script: 'unsaved script', bindings: { amount: 5 } },
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
})
