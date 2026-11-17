import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { api } from '@/adapter/client'
import { useProjectStore } from '@/stores/projects'
import ReceivedInvitations from './ReceivedInvitations.vue'

vi.mock('@/adapter/client', () => ({ api: { GET: vi.fn(), POST: vi.fn() } }))
const client = vi.mocked(api)
let wrapper: VueWrapper
const invitation = {
  id: 'invitation',
  projectName: 'Invoices',
  invitedByEmail: 'owner@example.test',
  expiresAt: '2026-12-01T12:00:00Z',
}

beforeEach(() => {
  setActivePinia(createPinia())
  vi.resetAllMocks()
  client.GET.mockResolvedValue({ data: [invitation] } as never)
})
afterEach(() => wrapper?.unmount())
async function open() {
  wrapper = mount(ReceivedInvitations)
  await flushPromises()
}

describe('Received invitations', () => {
  it('shows invitation details without joining automatically, then refreshes projects after acceptance', async () => {
    await open()
    expect(wrapper.text()).toContain('Invoices')
    expect(wrapper.text()).toContain('owner@example.test')
    expect(wrapper.text()).toContain('Expires')
    expect(client.POST).not.toHaveBeenCalled()
    client.POST.mockResolvedValue({ response: { status: 204 } } as never)
    client.GET.mockResolvedValue({
      data: [{ name: 'Invoices', projectIdApi: { id: 'project' } }],
    } as never)
    await wrapper.get('.primary-button').trigger('click')
    await flushPromises()
    expect(client.POST).toHaveBeenCalledWith('/api/invitations/received/{id}/accept', {
      params: { path: { id: 'invitation' } },
    })
    expect(wrapper.find('li').exists()).toBe(false)
    expect(useProjectStore().projects[0]?.name).toBe('Invoices')
    expect(wrapper.text()).toContain('Invitation accepted.')
  })

  it('declines explicitly and does not load project contents', async () => {
    await open()
    client.POST.mockResolvedValue({} as never)
    await wrapper.get('.invitation-actions .quiet-button').trigger('click')
    await flushPromises()
    expect(client.POST).toHaveBeenCalledWith('/api/invitations/received/{id}/decline', {
      params: { path: { id: 'invitation' } },
    })
    expect(client.GET).toHaveBeenCalledTimes(1)
    expect(wrapper.find('li').exists()).toBe(false)
    expect(wrapper.text()).toContain('Invitation declined.')
  })

  it('preserves the invitation after failure and allows refreshing stale invitations', async () => {
    await open()
    client.POST.mockResolvedValue({ error: { error: { message: 'Invitation expired.' } } } as never)
    await wrapper.get('.primary-button').trigger('click')
    await flushPromises()
    expect(wrapper.get('[role="alert"]').text()).toBe('Invitation expired.')
    expect(wrapper.find('li').exists()).toBe(true)
    client.GET.mockResolvedValue({ data: [] } as never)
    await wrapper.get('.invitation-error button').trigger('click')
    await flushPromises()
    expect(wrapper.find('section').exists()).toBe(false)
  })

  it('offers a retry if invitations fail to load, independently of projects', async () => {
    client.GET.mockRejectedValueOnce(new Error('Offline'))
    await open()
    expect(wrapper.get('[role="alert"]').text()).toContain('could not be loaded')
    await wrapper.get('button').trigger('click')
    await flushPromises()
    expect(wrapper.find('li').exists()).toBe(true)
  })

  it('hides the section when no invitations are pending', async () => {
    client.GET.mockResolvedValue({ data: [] } as never)
    await open()
    expect(wrapper.find('section').exists()).toBe(false)
  })
})
