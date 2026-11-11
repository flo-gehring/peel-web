import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import InvitationView from './InvitationView.vue'
import { useAuthStore } from '@/stores/auth'
const { get, post } = vi.hoisted(() => ({ get: vi.fn(), post: vi.fn() }))
vi.mock('@/adapter/client', () => ({
  api: { GET: get, POST: post },
  clearCsrf: vi.fn(),
  cancelWorkspaceRequests: vi.fn(),
}))
beforeEach(() => {
  setActivePinia(createPinia())
  vi.clearAllMocks()
})
async function open(authenticated: boolean) {
  const auth = useAuthStore()
  auth.isAuthenticated = authenticated
  auth.userId = authenticated ? 'recipient' : null
  get.mockResolvedValue({ data: { projectName: 'Letters', email: 'recipient@example.test' } })
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/invitations/:token', component: InvitationView },
      { path: '/login', name: 'login', component: { template: '<div />' } },
      { path: '/', component: { template: '<div />' } },
    ],
  })
  await router.push('/invitations/' + 'a'.repeat(43))
  const wrapper = mount(InvitationView, { global: { plugins: [router] } })
  await flushPromises()
  return wrapper
}
describe('Invitation acceptance', () => {
  it('sends anonymous recipients to sign-in with the invitation destination preserved', async () => {
    const wrapper = await open(false)
    expect(get).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Sign in or register')
    expect(wrapper.get('a').attributes('href')).toContain('redirect=')
    wrapper.unmount()
  })
  it('never joins automatically and requires an explicit acceptance', async () => {
    const wrapper = await open(true)
    expect(post).not.toHaveBeenCalled()
    post.mockResolvedValue({})
    await wrapper
      .findAll('button')
      .find((button) => button.text() === 'Accept invitation')!
      .trigger('click')
    await flushPromises()
    expect(post).toHaveBeenCalledWith('/api/invitations/{token}/accept', expect.any(Object))
    expect(wrapper.text()).toContain('You have joined')
    wrapper.unmount()
  })
})
