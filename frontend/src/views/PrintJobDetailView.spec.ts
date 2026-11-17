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
  it('shows missing columns and their scripts without starting the job, then locks uploads after a valid start', async () => {
    const wrapper = await open(false)
    mockApi.POST.mockResolvedValueOnce({
      error: {
        error: {
          code: 'missing_csv_columns',
          details: { missingColumns: ['amount'], scripts: { calculation: ['amount'] } },
        },
      },
    } as never)
    await wrapper.get('.start-button').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('Missing CSV columns: amount')
    expect(wrapper.text()).toContain('Required by scripts: calculation')
    expect(wrapper.find('.start-button').exists()).toBe(true)
    expect(wrapper.get('input[type="file"]').attributes('disabled')).toBeUndefined()
    expect(wrapper.text()).toContain('Replace CSV file')
    mockApi.PUT.mockResolvedValueOnce({} as never)
    const input = wrapper.get('input[type="file"]')
    Object.defineProperty(input.element, 'files', {
      value: [new File(['amount\n5'], 'corrected.csv', { type: 'text/csv' })],
    })
    await input.trigger('change')
    await flushPromises()
    expect(mockApi.PUT).toHaveBeenCalledWith(
      '/api/print-jobs/{id}/data',
      expect.objectContaining({
        params: { path: { id: 'job' } },
        body: { file: expect.any(File) },
      }),
    )
    mockApi.POST.mockResolvedValueOnce({ data: 'CALCULATING' })
    await wrapper.get('.start-button').trigger('click')
    await flushPromises()
    expect(wrapper.get('input[type="file"]').attributes('disabled')).toBeDefined()
    wrapper.unmount()
  })
  it('keeps results accessible after the source document was deleted and hides Start', async () => {
    const wrapper = await open(true)
    expect(wrapper.text()).toContain('document template was deleted')
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
    expect(wrapper.text()).toContain('Processing')
    wrapper.unmount()
    const archived = await open(false, 'ARCHIVED')
    expect(archived.find('.start-button').exists()).toBe(false)
    expect(archived.text()).toContain('result.pdf')
    archived.unmount()
  })
})
