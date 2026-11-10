import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useAuthStore } from './auth'
import { useEditorDraftStore } from './editorDrafts'
import { useProjectStore } from './projects'
import { useRunBindingsStore } from './runBindings'
import { useDocumentPreviewStore } from './documentPreview'

const { get, post } = vi.hoisted(() => ({ get: vi.fn(), post: vi.fn() }))
vi.mock('@/adapter/client', () => ({
  api: { GET: get, POST: post },
  clearCsrf: vi.fn(),
  cancelWorkspaceRequests: vi.fn(),
}))

describe('authentication state', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  async function signedIn() {
    const auth = useAuthStore()
    get.mockResolvedValueOnce({
      data: { authenticated: true, userId: 'one', email: 'one@example.test' },
    })
    await auth.refresh()
    return auth
  }

  it('preserves drafts on expiration and recovery with the same account', async () => {
    const auth = await signedIn()
    const drafts = useEditorDraftStore()
    drafts.setDraft('script', 'unsaved work')
    get.mockResolvedValueOnce({ data: { authenticated: false } })
    await auth.refresh()
    expect(auth.sessionExpired).toBe(true)
    expect(drafts.getDraft('script')).toBe('unsaved work')
    get.mockResolvedValueOnce({
      data: { authenticated: true, userId: 'one', email: 'one@example.test' },
    })
    await auth.refresh()
    expect(auth.sessionExpired).toBe(false)
    expect(drafts.getDraft('script')).toBe('unsaved work')
  })

  it('does not interpret a network failure as logout', async () => {
    const auth = await signedIn()
    get.mockRejectedValueOnce(new TypeError('offline'))
    expect(await auth.refresh()).toBe(false)
    expect(auth.isAuthenticated).toBe(true)
    expect(auth.statusError).toBeTruthy()
  })

  it('clears the old account workspace when another account is detected', async () => {
    const auth = await signedIn()
    useEditorDraftStore().setDraft('private', 'old user')
    useRunBindingsStore().setRawJson('{"private":42}')
    get.mockResolvedValueOnce({
      data: { authenticated: true, userId: 'two', email: 'two@example.test' },
    })
    await auth.refresh()
    expect(useEditorDraftStore().getDraft('private')).toBeUndefined()
    expect(useRunBindingsStore().rawJson).toBe('{}')
    expect(auth.workspaceVersion).toBe(1)
  })

  it('clears account and workspace only after successful logout', async () => {
    const auth = await signedIn()
    useEditorDraftStore().setDraft('private', 'draft')
    useProjectStore().projects = [{ name: 'private' }]
    post.mockRejectedValueOnce(new TypeError('offline'))
    await expect(auth.logout()).rejects.toThrow()
    expect(auth.isAuthenticated).toBe(true)
    expect(useEditorDraftStore().getDraft('private')).toBe('draft')
    post.mockResolvedValueOnce({ response: { status: 204 } })
    await auth.logout()
    expect(auth.isAuthenticated).toBe(false)
    expect(useEditorDraftStore().draftsById).toEqual({})
    expect(useProjectStore().projects).toEqual([])
    expect(useDocumentPreviewStore().pdfUrl).toBeNull()
  })

  it('ignores a session check that finishes after logout', async () => {
    const auth = await signedIn()
    let resolveSession!: (value: unknown) => void
    get.mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          resolveSession = resolve
        }),
    )
    const refresh = auth.refresh()
    post.mockResolvedValueOnce({ response: { status: 204 } })
    await auth.logout()
    resolveSession({ data: { authenticated: true, userId: 'one', email: 'one@example.test' } })
    await refresh
    expect(auth.isAuthenticated).toBe(false)
  })
})
