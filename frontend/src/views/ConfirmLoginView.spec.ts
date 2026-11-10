import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ConfirmLoginView from './ConfirmLoginView.vue'

const { get, post } = vi.hoisted(() => ({ get: vi.fn(), post: vi.fn() }))
vi.mock('@/adapter/client', () => ({
  api: { GET: get, POST: post },
  clearCsrf: vi.fn(),
  cancelWorkspaceRequests: vi.fn(),
}))

describe('ConfirmLoginView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  async function openLink() {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/login/confirm', name: 'confirm-login', component: ConfirmLoginView },
        { path: '/login', name: 'login', component: { template: '<div>Login</div>' } },
        { path: '/projects/:id', component: { template: '<div>Project</div>' } },
      ],
    })
    await router.push('/login/confirm?token=secret-token&redirect=/projects/one')
    const wrapper = mount(ConfirmLoginView, { global: { plugins: [router] } })
    await flushPromises()
    return { wrapper, router }
  }

  it('does not consume a link until confirmed and removes it from the URL', async () => {
    const { wrapper, router } = await openLink()
    expect(post).not.toHaveBeenCalled()
    expect(router.currentRoute.value.query.token).toBeUndefined()
    get
      .mockResolvedValueOnce({ data: { authenticated: false } })
      .mockResolvedValueOnce({
        data: { authenticated: true, userId: 'one', email: 'one@example.test' },
      })
    post.mockResolvedValueOnce({ response: { status: 204 } })
    await wrapper.get('button').trigger('click')
    await flushPromises()
    expect(post).toHaveBeenCalledWith(
      '/api/auth/login',
      expect.objectContaining({ body: { token: 'secret-token' } }),
    )
    expect(router.currentRoute.value.fullPath).toBe('/projects/one')
    wrapper.unmount()
  })

  it('offers a new link after a consumed or expired token', async () => {
    const { wrapper } = await openLink()
    get.mockResolvedValueOnce({ data: { authenticated: false } })
    post.mockResolvedValueOnce({
      error: { error: { message: 'This sign-in link is no longer valid.' } },
      response: { status: 401 },
    })
    await wrapper.get('button').trigger('click')
    await flushPromises()
    expect(wrapper.get('[role="alert"]').text()).toContain('no longer valid')
    expect(wrapper.get('button').attributes('disabled')).toBeDefined()
    wrapper.unmount()
  })
})
