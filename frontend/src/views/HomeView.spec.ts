import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { api } from '@/adapter/client'
import HomeView from './HomeView.vue'

vi.mock('@/adapter/client', () => ({ api: { GET: vi.fn(), POST: vi.fn() } }))

const client = vi.mocked(api)
let wrapper: VueWrapper

async function openHome() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: HomeView },
      { path: '/projects/:projectId', name: 'project', component: { template: '<div />' } },
    ],
  })
  await router.push('/')
  wrapper = mount(HomeView, {
    attachTo: document.body,
    global: { plugins: [router], stubs: { ReceivedInvitations: true } },
  })
  await flushPromises()
  return router
}

beforeEach(() => {
  setActivePinia(createPinia())
  vi.resetAllMocks()
  client.GET.mockResolvedValue({
    data: [{ name: 'Invoices', projectIdApi: { id: 'invoices' } }],
  } as never)
})
afterEach(() => wrapper?.unmount())

describe('Project start page', () => {
  it('opens creation inline, focuses the name, and cancels back to the project list', async () => {
    await openHome()
    const newProject = wrapper.get('button[aria-controls="new-project-form"]')
    await newProject.trigger('click')
    expect(document.activeElement).toBe(wrapper.get('input').element)
    expect(wrapper.get('.project-collection').find('form').exists()).toBe(true)
    expect(wrapper.get('.project-row').text()).toContain('Invoices')
    await wrapper.get('input').setValue('Draft name')
    await wrapper.get('input').trigger('keydown', { key: 'Escape' })
    expect(wrapper.find('form').exists()).toBe(false)
    expect(document.activeElement).toBe(newProject.element)
    expect(client.POST).not.toHaveBeenCalled()
  })

  it('submits a trimmed name once and opens the returned project', async () => {
    const router = await openHome()
    client.POST.mockResolvedValue({
      data: { name: 'Labels', projectIdApi: { id: 'labels' } },
    } as never)
    await wrapper.get('button[aria-controls]').trigger('click')
    await wrapper.get('input').setValue('  Labels  ')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(client.POST).toHaveBeenCalledTimes(1)
    expect(client.POST).toHaveBeenCalledWith('/api/projects/{projectId}', {
      params: { path: { projectId: 'Labels' } },
    })
    expect(router.currentRoute.value.fullPath).toBe('/projects/labels')
  })

  it('keeps creation and existing projects visible when creation fails', async () => {
    await openHome()
    client.POST.mockRejectedValue(new Error('Offline'))
    await wrapper.get('button[aria-controls]').trigger('click')
    await wrapper.get('input').setValue('Labels')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(wrapper.get('[role="alert"]').text()).toContain('could not be created')
    expect(wrapper.get<HTMLInputElement>('input').element.value).toBe('Labels')
    expect(wrapper.get('.project-row').text()).toContain('Invoices')
    expect(wrapper.get('button[type="submit"]').attributes('disabled')).toBeUndefined()
  })
})
