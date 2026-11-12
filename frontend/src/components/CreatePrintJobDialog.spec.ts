import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import CreatePrintJobDialog from './CreatePrintJobDialog.vue'
import { api } from '@/adapter/client'

vi.mock('@/adapter/client', () => ({ api: { POST: vi.fn() } }))
const post = vi.mocked(api.POST)
beforeEach(() => vi.resetAllMocks())
const props = { projectId: 'project', documentId: 'document', documentName: 'Invoice' }
const options = { global: { stubs: { Teleport: true } } }
describe('create project print job', () => {
  it('prepares the document before creating and emits the returned job ID', async () => {
    const prepare = vi.fn(async () => {
      expect(post).not.toHaveBeenCalled()
    })
    post.mockResolvedValueOnce({ data: { id: 'job' } })
    const wrapper = mount(CreatePrintJobDialog, {
      ...options,
      props: { ...props, saveRequired: true, prepare },
    })
    expect(wrapper.text()).toContain('Speichern und Auftrag anlegen')
    await wrapper.get('input').setValue(' November ')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(prepare).toHaveBeenCalledOnce()
    expect(post).toHaveBeenCalledWith('/api/projects/{projectId}/print-jobs', {
      params: { path: { projectId: 'project' } },
      body: { name: 'November', documentId: { id: 'document' } },
    })
    expect(wrapper.emitted('created')).toEqual([['job']])
    wrapper.unmount()
  })
  it('does not create a job when saving fails and retains the entered name', async () => {
    const wrapper = mount(CreatePrintJobDialog, {
      ...options,
      props: {
        ...props,
        prepare: async () => {
          throw new Error('Version conflict')
        },
      },
    })
    await wrapper.get('input').setValue('My job')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(post).not.toHaveBeenCalled()
    expect(wrapper.get('[role="alert"]').text()).toBe('Version conflict')
    expect(wrapper.get('input').element.value).toBe('My job')
    wrapper.unmount()
  })
  it('reports creation failures and allows a retry without an automatic second request', async () => {
    post.mockRejectedValueOnce(new TypeError('Connection lost'))
    const wrapper = mount(CreatePrintJobDialog, { ...options, props })
    await wrapper.get('input').setValue('My job')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(post).toHaveBeenCalledOnce()
    expect(wrapper.get('[role="alert"]').text()).toContain('Connection lost')
    expect(wrapper.get('button.primary-button').attributes('disabled')).toBeUndefined()
    wrapper.unmount()
  })
})
