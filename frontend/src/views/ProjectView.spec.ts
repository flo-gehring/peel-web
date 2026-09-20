import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, nextTick, vi } from 'vitest'
import ProjectView from './ProjectView.vue'

const { dockviewApi } = vi.hoisted(() => ({
  dockviewApi: {
    addGroup: vi.fn(() => ({ id: 'editor-group' })),
    addEdgeGroup: vi.fn(() => ({ id: 'left-group' })),
    addPanel: vi.fn(),
    getPanel: vi.fn(),
    groups: [],
    activeGroup: undefined,
  },
}))

vi.mock('dockview-vue', async () => {
  const { defineComponent, h, onMounted } = await import('vue')
  return {
    DockviewVue: defineComponent({
      emits: ['ready'],
      setup(_, { emit }) {
        onMounted(() => emit('ready', { api: dockviewApi }))
        return () => h('div')
      },
    }),
  }
})

vi.mock('@/adapter/client', () => ({
  api: {
    GET: vi.fn(),
    POST: vi.fn(),
    PUT: vi.fn(),
    DELETE: vi.fn(),
  },
}))

describe('ProjectView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('starts with explorer, bindings, and output without a demo editor', async () => {
    mount(ProjectView)
    await nextTick()

    expect(dockviewApi.addPanel).toHaveBeenCalledWith(expect.objectContaining({ id: 'file-tree' }))
    expect(dockviewApi.addPanel).toHaveBeenCalledWith(expect.objectContaining({ id: 'bindings-json' }))
    expect(dockviewApi.addPanel).toHaveBeenCalledWith(expect.objectContaining({ id: 'output-console' }))
    expect(dockviewApi.addPanel).not.toHaveBeenCalledWith(expect.objectContaining({ id: 'editor-App.java' }))
  })
})
