import { describe, expect, it } from 'vitest'
import { safeAuthRedirect } from './authRedirect'

describe('safeAuthRedirect', () => {
  it('keeps application destinations, including query and fragment', () => {
    expect(safeAuthRedirect('/projects/123?tab=editor#draft')).toBe(
      '/projects/123?tab=editor#draft',
    )
    expect(safeAuthRedirect('/projects/project/print-jobs')).toBe('/projects/project/print-jobs')
    expect(safeAuthRedirect('/projects/project/print-jobs/123')).toBe(
      '/projects/project/print-jobs/123',
    )
    expect(safeAuthRedirect('/projects/123/settings')).toBe('/projects/123/settings')
    const invitation = '/invitations/' + 'a'.repeat(43)
    expect(safeAuthRedirect(invitation)).toBe(invitation)
  })
  it('rejects external URLs, encoded paths and login loops', () => {
    for (const value of [
      'https://evil.test',
      '//evil.test',
      '/\\evil.test',
      '/login',
      '/projects/%2f%2fevil',
      '/api/auth/logout',
      null,
    ]) {
      expect(safeAuthRedirect(value)).toBe('/')
    }
  })
})
