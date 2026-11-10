import { describe, expect, it } from 'vitest'
import { safeAuthRedirect } from './authRedirect'

describe('safeAuthRedirect', () => {
  it('keeps application destinations, including query and fragment', () => {
    expect(safeAuthRedirect('/projects/123?tab=editor#draft')).toBe(
      '/projects/123?tab=editor#draft',
    )
    expect(safeAuthRedirect('/print-jobs')).toBe('/print-jobs')
    expect(safeAuthRedirect('/print-jobs/123')).toBe('/print-jobs/123')
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
