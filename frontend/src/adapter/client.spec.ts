import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

describe('API CSRF integration', () => {
  beforeEach(() => vi.resetModules())
  afterEach(() => vi.unstubAllGlobals())

  it('attaches CSRF to JSON and multipart requests and refreshes after logout', async () => {
    const requests: Request[] = []
    let token = 0
    vi.stubGlobal(
      'fetch',
      vi.fn(async (request: Request) => {
        requests.push(request)
        if (new URL(request.url).pathname === '/api/auth/csrf') {
          token += 1
          return Response.json({ token: `token-${token}`, headerName: 'X-CSRF-TOKEN' })
        }
        return new Response(null, { status: 204 })
      }),
    )
    const { api } = await import('./client')
    await api.POST('/api/auth/link', { body: { email: 'one@example.test' } })
    await api.PUT('/api/print-jobs/{id}/data', {
      params: { path: { id: 'one' } },
      body: { file: 'ignored' },
      bodySerializer: () => {
        const data = new FormData()
        data.append('file', new Blob(['a,b'], { type: 'text/csv' }), 'test.csv')
        return data
      },
    })
    await api.POST('/api/auth/logout')
    await api.POST('/api/auth/link', { body: { email: 'two@example.test' } })
    const writes = requests.filter((request) => request.method !== 'GET')
    expect(writes.map((request) => request.headers.get('X-CSRF-TOKEN'))).toEqual([
      'token-1',
      'token-1',
      'token-1',
      'token-2',
    ])
    expect(writes[1]?.headers.get('Content-Type')).toContain('multipart/form-data')
  })
})
