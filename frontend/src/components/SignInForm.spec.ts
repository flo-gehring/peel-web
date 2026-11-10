import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import SignInForm from './SignInForm.vue'

const { post } = vi.hoisted(() => ({ post: vi.fn() }))
vi.mock('@/adapter/client', () => ({ api: { POST: post }, clearCsrf: vi.fn() }))

describe('SignInForm', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })
  afterEach(() => vi.useRealTimers())

  it('shows inbox instructions only after successful delivery', async () => {
    post.mockResolvedValueOnce({ response: { status: 204 } })
    const wrapper = mount(SignInForm, { props: { redirect: '/projects/one' } })
    await wrapper.get('input').setValue('test@example.test')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(post).toHaveBeenCalledWith('/api/auth/link', {
      body: { email: 'test@example.test', redirect: '/projects/one' },
    })
    expect(wrapper.text()).toContain('Check your inbox')
    expect(wrapper.emitted('sent')).toHaveLength(1)
    wrapper.unmount()
  })

  it('shows delivery failures without claiming success', async () => {
    post.mockResolvedValueOnce({
      error: { error: { message: 'Mail unavailable' } },
      response: { status: 503 },
    })
    const wrapper = mount(SignInForm)
    await wrapper.get('input').setValue('test@example.test')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(wrapper.get('[role="alert"]').text()).toBe('Mail unavailable')
    expect(wrapper.text()).not.toContain('Check your inbox')
    wrapper.unmount()
  })

  it('honors the server retry-after limit', async () => {
    vi.useFakeTimers()
    post.mockResolvedValueOnce({
      error: { error: { message: 'Please wait' } },
      response: { status: 429, headers: new Headers({ 'Retry-After': '120' }) },
    })
    const wrapper = mount(SignInForm)
    await wrapper.get('input').setValue('test@example.test')
    await wrapper.get('form').trigger('submit')
    await vi.advanceTimersByTimeAsync(0)
    expect(wrapper.get('button').attributes('disabled')).toBeDefined()
    expect(wrapper.text()).toContain('120 seconds')
    await vi.advanceTimersByTimeAsync(120_000)
    expect(wrapper.get('button').attributes('disabled')).toBeUndefined()
    wrapper.unmount()
  })
})
