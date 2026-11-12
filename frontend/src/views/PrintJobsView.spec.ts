import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import PrintJobsView from './PrintJobsView.vue'
import { api } from '@/adapter/client'

vi.mock('@/adapter/client', () => ({ api: { GET: vi.fn() } }))
const get = vi.mocked(api.GET)
beforeEach(() => vi.resetAllMocks())
async function open(state = 'ACTIVE') {
  get.mockImplementation(
    async (path) =>
      ({
        data:
          path === '/api/projects/{projectId}/print-jobs'
            ? {
                projectId: { id: 'project' },
                projectName: 'Invoices',
                state,
                documents: [
                  { documentId: 'empty', name: 'Empty', jobs: [] },
                  {
                    documentId: 'document',
                    name: 'Invoice',
                    jobs: [
                      {
                        printJobId: { id: 'job' },
                        name: 'November',
                        createdAt: '2026-11-12T12:00:00Z',
                        status: 'COMPLETED',
                        fileIds: ['pdf'],
                      },
                    ],
                  },
                ],
                deletedDocuments: [
                  {
                    documentId: 'old-document',
                    name: 'Old Invoice',
                    jobs: [
                      {
                        printJobId: { id: 'old-job' },
                        name: 'October',
                        status: 'COMPLETED',
                        fileIds: [],
                      },
                    ],
                  },
                ],
              }
            : { projectIdApi: { id: 'project' }, name: 'Invoices', state },
      }) as never,
  )
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/projects/:projectId/print-jobs', component: PrintJobsView },
      {
        path: '/projects/:projectId/print-jobs/:id',
        name: 'print-job-detail',
        component: { template: '<div />' },
      },
    ],
  })
  await router.push('/projects/project/print-jobs')
  const wrapper = mount(PrintJobsView, {
    global: {
      plugins: [createPinia(), router],
      stubs: {
        CreatePrintJobDialog: {
          props: ['documentId'],
          template: '<div class="test-create">{{ documentId }}</div>',
        },
      },
    },
  })
  await flushPromises()
  return { wrapper, router }
}
describe('project print jobs', () => {
  it('lists empty and populated document groups, with deleted groups below and no creation there', async () => {
    const { wrapper } = await open()
    expect(wrapper.findAll('.document-jobs')).toHaveLength(3)
    expect(wrapper.get('.empty-group').text()).toContain('Noch keine')
    expect(wrapper.get('.deleted-heading').text()).toBe('Gelöschte Dokumentvorlagen')
    expect(wrapper.get('.document-jobs.deleted').text()).toContain('October')
    expect(wrapper.get('.document-jobs.deleted').find('.create-job').exists()).toBe(false)
    expect(wrapper.findAll('.create-job')).toHaveLength(2)
    expect(wrapper.get('a.print-job-row').attributes('href')).toBe(
      '/projects/project/print-jobs/job',
    )
    await wrapper.findAll('.create-job')[1]!.trigger('click')
    expect(wrapper.get('.test-create').text()).toBe('document')
    wrapper.unmount()
  })
  it('keeps archived groups readable but disables creation', async () => {
    const { wrapper } = await open('ARCHIVED')
    expect(wrapper.text()).toContain('Archiviertes Projekt')
    expect(wrapper.get('.create-job').attributes('disabled')).toBeDefined()
    expect(wrapper.findAll('a.print-job-row')).toHaveLength(2)
    wrapper.unmount()
  })
})
