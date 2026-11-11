import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ProjectSettingsView from './ProjectSettingsView.vue'
const { get, post, put, remove } = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  remove: vi.fn(),
}))
vi.mock('@/adapter/client', () => ({ api: { GET: get, POST: post, PUT: put, DELETE: remove } }))
const settings = (role = 'OWNER', state = 'ACTIVE') => ({
  id: 'project',
  name: 'Invoices',
  description: 'Letters',
  state,
  role,
  members: [
    { id: 'owner', email: 'owner@example.test', role: 'OWNER' },
    { id: 'member', email: 'member@example.test', role: 'MEMBER' },
  ],
  invitations: [],
})
beforeEach(() => {
  setActivePinia(createPinia())
  vi.clearAllMocks()
})
async function open(role = 'OWNER', state = 'ACTIVE') {
  get.mockResolvedValue({ data: settings(role, state) })
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/projects/:projectId/settings', component: ProjectSettingsView },
      { path: '/projects/:projectId', name: 'project', component: { template: '<div />' } },
      { path: '/', component: { template: '<div />' } },
    ],
  })
  await router.push('/projects/project/settings')
  const wrapper = mount(ProjectSettingsView, { global: { plugins: [router] } })
  await flushPromises()
  return wrapper
}
describe('Project settings', () => {
  it('shows active metadata controls to members but reserves administrative controls', async () => {
    const wrapper = await open('MEMBER')
    expect(wrapper.find('#project-name').exists()).toBe(true)
    expect(wrapper.find('#invite-email').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('Make admin')
    expect(wrapper.text()).toContain('Leave project')
    expect(wrapper.text()).not.toContain('Archive project')
    wrapper.unmount()
  })
  it('allows admins to invite without appointing admins', async () => {
    const wrapper = await open('ADMIN')
    post.mockResolvedValue({})
    await wrapper.get('#invite-email').setValue('new@example.test')
    await wrapper
      .get('#invite-email')
      .element.closest('form')!
      .dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }))
    await flushPromises()
    expect(post).toHaveBeenCalledWith(
      '/api/projects/{id}/invitations',
      expect.objectContaining({ body: { email: 'new@example.test' } }),
    )
    expect(wrapper.text()).not.toContain('Make admin')
    wrapper.unmount()
  })
  it('requires email and exact transfer text before requesting ownership transfer', async () => {
    const wrapper = await open()
    await wrapper.get('#transfer-target').setValue('member@example.test')
    const button = wrapper.findAll('button').find((button) => button.text() === 'Request transfer')!
    expect(button.attributes('disabled')).toBeDefined()
    await wrapper.get('#transfer-email').setValue('member@example.test')
    await wrapper.get('#transfer-confirmation').setValue('TRANSFER Invoices')
    expect(button.attributes('disabled')).toBeUndefined()
    wrapper.unmount()
  })
  it('makes archived settings read-only and requires the name for hard deletion', async () => {
    const wrapper = await open('OWNER', 'ARCHIVED')
    expect(wrapper.get('#project-name').element.closest('fieldset')?.disabled).toBe(true)
    expect(wrapper.text()).toContain('Restore project')
    const button = wrapper
      .findAll('button')
      .find((button) => button.text() === 'Permanently delete project')!
    expect(button.attributes('disabled')).toBeDefined()
    await wrapper.get('#delete-confirmation').setValue('Invoices')
    expect(button.attributes('disabled')).toBeUndefined()
    wrapper.unmount()
  })
})
