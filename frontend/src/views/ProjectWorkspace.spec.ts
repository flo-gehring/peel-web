import { afterEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { DockviewVue, type DockviewReadyEvent } from 'dockview-vue'
import ProjectView from './ProjectView.vue'

vi.mock('@guolao/vue-monaco-editor', () => ({
  VueMonacoEditor: { template: '<div class="test-monaco" />' },
}))
vi.mock('@/adapter/client', () => ({
  api: {
    GET: vi.fn(async (path: string) => ({
      data:
        path === '/api/scripts/{id}'
          ? { id: 'script-1', name: 'Customer', script: 'customer' }
          : {
              projectIdApi: { id: 'invoices' },
              name: 'Invoices',
              scripts: [{ id: 'script-1', name: 'Customer' }],
            },
    })),
  },
}))

let wrapper: VueWrapper
afterEach(() => wrapper?.unmount())

describe('Dockview workspace integration', () => {
  it('initializes the real layout and opens explorer files in the editor group', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/projects/:projectId', component: ProjectView }],
    })
    await router.push('/projects/invoices')
    wrapper = mount(ProjectView, {
      attachTo: document.body,
      global: { plugins: [createPinia(), router] },
    })
    await flushPromises()
    const ready = wrapper
      .findComponent(DockviewVue)
      .emitted('ready')?.[0]?.[0] as DockviewReadyEvent
    const dock = ready.api
    expect(dock.panels.map((panel) => panel.id)).toEqual(
      expect.arrayContaining(['workspace-welcome', 'file-tree', 'bindings-json', 'output-console']),
    )
    expect(dock.getPanel('bindings-json')?.group.id).not.toBe('editor-group')
    expect(wrapper.find('.dockview-theme-peel').exists()).toBe(true)
    await wrapper.get('.file-name').trigger('click')
    await flushPromises()
    expect(dock.getPanel('editor-script-script-1')?.group.id).toBe('editor-group')
    expect(wrapper.text()).toContain('Run')
    expect(dock.getPanel('bindings-json')).toBeDefined()
    expect(dock.getPanel('output-console')).toBeDefined()
  })
})
