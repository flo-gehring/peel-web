import { afterEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { DockviewVue, type DockviewReadyEvent } from 'dockview-vue'
import ProjectView from './ProjectView.vue'
import { useWorkspaceSelectionStore } from '@/stores/workspaceSelection'

vi.mock('@guolao/vue-monaco-editor', () => ({
  VueMonacoEditor: { template: '<div class="test-monaco" />' },
}))
vi.mock('@/adapter/client', () => ({
  api: {
    GET: vi.fn(async (path: string, options?: { params?: { path?: { fileId?: string } } }) => ({
      data:
        path === '/api/projects/{projectId}/files/{fileId}'
          ? options?.params?.path?.fileId === 'document-1'
            ? {
                id: 'document-1',
                projectId: { id: 'invoices' },
                version: 0,
                name: 'Invoice',
                content: {
                  type: 'document',
                  templateHtml: '<p>Invoice</p>',
                  editorStateJson: '{}',
                  scriptNameTags: {},
                  renderConfigurationId: 'config',
                },
              }
            : {
                id: 'script-1',
                projectId: { id: 'invoices' },
                version: 0,
                name: 'Customer',
                content: { type: 'script', source: 'customer' },
              }
          : path === '/api/projects/{projectId}/files'
            ? [{ id: 'script-1', name: 'Customer', type: 'script', version: 0 }]
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
    const pinia = createPinia()
    wrapper = mount(ProjectView, {
      attachTo: document.body,
      global: { plugins: [pinia, router] },
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
    const selection = useWorkspaceSelectionStore(pinia)
    selection.selectDocument({ kind: 'document', id: 'document-1', name: 'Invoice', icon: 'DOC' })
    await flushPromises()
    expect(dock.getPanel('editor-document-document-1')).toBeDefined()
    expect(dock.getPanel('output-console')).toBeUndefined()
    expect(wrapper.findAll('.file-toolbar').length).toBeGreaterThan(0)
    selection.requestOutput()
    await flushPromises()
    expect(dock.getPanel('output-console')).toBeDefined()
    dock.getPanel('editor-script-script-1')?.api.setActive()
    dock.getPanel('editor-document-document-1')?.api.setActive()
    await flushPromises()
    expect(dock.getPanel('output-console')).toBeUndefined()
  })
})
