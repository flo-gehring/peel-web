import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import PrintJobDetailView from './PrintJobDetailView.vue'
import { api } from '@/adapter/client'
vi.mock('@/adapter/client', () => ({ api: { GET: vi.fn(), POST: vi.fn(), PUT: vi.fn() } }))
const mockApi = vi.mocked(api)
beforeEach(() => vi.resetAllMocks())
async function open(deleted: boolean, state = 'ACTIVE') {
  mockApi.GET.mockImplementation(
    async (path) =>
      ({
        data:
          path === '/api/projects/{projectId}/print-jobs/{id}'
            ? {
                projectName: 'Project',
                projectState: state,
                currentDocumentName: deleted ? null : 'Invoice',
                job: {
                  printJobId: { id: 'job' },
                  projectId: { id: 'project' },
                  documentId: { id: 'document' },
                  documentName: 'Invoice',
                  name: 'Batch',
                  status: 'CREATED',
                  fileName: 'input.csv',
                  fileIds: ['result.pdf'],
                },
              }
            : { projectIdApi: { id: 'project' }, name: 'Project', state },
      }) as never,
  )
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/projects/:projectId/print-jobs/:id', component: PrintJobDetailView }],
  })
  await router.push('/projects/project/print-jobs/job')
  const wrapper = mount(PrintJobDetailView, { global: { plugins: [createPinia(), router] } })
  await flushPromises()
  return wrapper
}
describe('project print job details', () => {
  it('keeps results accessible after the source document was deleted and hides Start', async () => {
    const wrapper = await open(true)
    expect(wrapper.text()).toContain('Dokumentvorlage wurde gelöscht')
    expect(wrapper.find('.start-button').exists()).toBe(false)
    expect(wrapper.text()).toContain('result.pdf')
    expect(wrapper.get('.back-link').attributes('href')).toBe('/projects/project/print-jobs')
    wrapper.unmount()
  })
  it('starts only a job with an available document in an active project', async () => {
    const wrapper = await open(false)
    mockApi.POST.mockResolvedValueOnce({ data: 'CALCULATING' })
    await wrapper.get('.start-button').trigger('click')
    await flushPromises()
    expect(mockApi.POST).toHaveBeenCalledWith('/api/print-jobs/{id}/run', {
      params: { path: { id: 'job' } },
    })
    expect(wrapper.text()).toContain('Wird verarbeitet')
    wrapper.unmount()
    const archived = await open(false, 'ARCHIVED')
    expect(archived.find('.start-button').exists()).toBe(false)
    expect(archived.text()).toContain('result.pdf')
    archived.unmount()
  })
})
