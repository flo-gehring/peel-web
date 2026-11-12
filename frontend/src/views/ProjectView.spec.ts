import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import { createMemoryHistory, createRouter } from 'vue-router'
import ProjectView from './ProjectView.vue'

const { dockviewApi } = vi.hoisted(() => ({
  dockviewApi: {
    onDidActivePanelChange: vi.fn(() => ({ dispose: vi.fn() })),
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
    themeAbyss: { name: 'abyss', className: 'dockview-theme-abyss' },
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
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/projects/:projectId', component: ProjectView }],
    })
    await router.push('/projects/test-project')
    mount(ProjectView, { global: { plugins: [router] } })
    await nextTick()

    expect(dockviewApi.addPanel).toHaveBeenCalledWith(expect.objectContaining({ id: 'file-tree' }))
    expect(dockviewApi.addPanel).toHaveBeenCalledWith(
      expect.objectContaining({ id: 'bindings-json' }),
    )
    expect(dockviewApi.addPanel).toHaveBeenCalledWith(
      expect.objectContaining({ id: 'output-console' }),
    )
    expect(dockviewApi.addPanel).toHaveBeenCalledWith(
      expect.objectContaining({ id: 'workspace-welcome' }),
    )
    expect(dockviewApi.addPanel).toHaveBeenCalledWith(
      expect.objectContaining({
        id: 'bindings-json',
        position: expect.objectContaining({ direction: 'right' }),
      }),
    )
    expect(dockviewApi.addPanel).not.toHaveBeenCalledWith(
      expect.objectContaining({ id: 'editor-App.java' }),
    )
  })
})
