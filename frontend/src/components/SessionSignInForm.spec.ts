import { mount, flushPromises, type VueWrapper } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useAuthStore } from '@/stores/auth'
import { useEditorDraftStore } from '@/stores/editorDrafts'
import SessionSignInForm from './SessionSignInForm.vue'

const { get, post } = vi.hoisted(() => ({ get: vi.fn(), post: vi.fn() }))
vi.mock('@/adapter/client', () => ({
  api: { GET: get, POST: post },
  clearCsrf: vi.fn(),
  cancelWorkspaceRequests: vi.fn(),
}))
let wrapper: VueWrapper
const account = { authenticated: true, userId: 'one', email: 'one@example.test' }

beforeEach(async () => {
  setActivePinia(createPinia())
  vi.resetAllMocks()
  get.mockResolvedValueOnce({ data: account })
  await useAuthStore().refresh()
  useAuthStore().expireSession()
  useEditorDraftStore().setDraft('script', 'unsaved changes')
})
afterEach(() => wrapper?.unmount())

describe('Session sign-in', () => {
  it('prefills the previous account and sends an email without navigation', async () => {
    wrapper = mount(SessionSignInForm, { props: { redirect: '/projects/one' } })
    expect(wrapper.get<HTMLInputElement>('input[type="email"]').element.value).toBe(
      'one@example.test',
    )
    expect(post).not.toHaveBeenCalled()
    post.mockResolvedValueOnce({ response: { status: 204 } })
    await wrapper.get('.login-card form').trigger('submit')
    await flushPromises()
    expect(post).toHaveBeenCalledWith('/api/auth/link', {
      body: { email: 'one@example.test', redirect: '/projects/one' },
    })
    expect(wrapper.find('#session-sign-in-code').exists()).toBe(true)
    expect(useAuthStore().sessionExpired).toBe(true)
  })

  it('accepts a pasted code and restores the same account without losing drafts', async () => {
    wrapper = mount(SessionSignInForm, { props: { redirect: '/projects/one' } })
    get
      .mockResolvedValueOnce({ data: { authenticated: false } })
      .mockResolvedValueOnce({ data: account })
    post.mockResolvedValueOnce({ response: { status: 204 } })
    await wrapper.get('#session-sign-in-code').setValue('  email-token  ')
    await wrapper.get('.code-form').trigger('submit')
    await flushPromises()
    expect(post).toHaveBeenCalledWith(
      '/api/auth/login',
      expect.objectContaining({
        body: { token: 'email-token' },
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      }),
    )
    expect(useAuthStore().sessionExpired).toBe(false)
    expect(useEditorDraftStore().getDraft('script')).toBe('unsaved changes')
    expect(wrapper.find('#session-sign-in-code').exists()).toBe(false)
  })

  it('clears invalid codes and keeps drafts while a new email can be requested', async () => {
    wrapper = mount(SessionSignInForm, { props: { redirect: '/projects/one' } })
    get.mockResolvedValueOnce({ data: { authenticated: false } })
    post.mockResolvedValueOnce({
      error: { error: { message: 'Code expired.' } },
      response: { status: 401 },
    })
    await wrapper.get('#session-sign-in-code').setValue('expired')
    await wrapper.get('.code-form').trigger('submit')
    await flushPromises()
    expect(wrapper.get('[role="alert"]').text()).toBe('Code expired.')
    expect(wrapper.get<HTMLInputElement>('#session-sign-in-code').element.value).toBe('')
    expect(useAuthStore().sessionExpired).toBe(true)
    expect(useEditorDraftStore().getDraft('script')).toBe('unsaved changes')
  })

  it('recovers an already restored session without consuming the pasted token', async () => {
    wrapper = mount(SessionSignInForm, { props: { redirect: '/projects/one' } })
    get.mockResolvedValueOnce({ data: account })
    await wrapper.get('#session-sign-in-code').setValue('unused')
    await wrapper.get('.code-form').trigger('submit')
    await flushPromises()
    expect(post).not.toHaveBeenCalled()
    expect(useAuthStore().sessionExpired).toBe(false)
  })

  it('keeps a successfully consumed code cleared if the final session check fails', async () => {
    wrapper = mount(SessionSignInForm, { props: { redirect: '/projects/one' } })
    get
      .mockResolvedValueOnce({ data: { authenticated: false } })
      .mockRejectedValueOnce(new Error('Offline'))
    post.mockResolvedValueOnce({ response: { status: 204 } })
    await wrapper.get('#session-sign-in-code').setValue('valid')
    await wrapper.get('.code-form').trigger('submit')
    await flushPromises()
    expect(wrapper.find('#session-sign-in-code').exists()).toBe(false)
    expect(wrapper.text()).toContain('Sign-in succeeded')
    expect(useAuthStore().statusError).toBeTruthy()
  })
})
