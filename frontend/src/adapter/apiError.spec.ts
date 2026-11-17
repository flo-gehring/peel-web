import { afterEach, describe, expect, it } from 'vitest'
import { setLocale } from '@/i18n'
import { apiErrorMessage } from './apiError'

afterEach(() => setLocale('en'))

describe('localized API errors', () => {
  it('translates stable error codes and known project validation messages', () => {
    setLocale('de')
    expect(
      apiErrorMessage({
        error: { code: 'invalid_email', message: 'Enter a valid email address.' },
      }),
    ).toBe('Gib eine gültige E-Mail-Adresse ein.')
    expect(
      apiErrorMessage({
        error: {
          code: 'bad_request',
          message: 'Type TRANSFER followed by the exact project name.',
        },
      }),
    ).toBe('Gib TRANSFER gefolgt vom exakten Projektnamen ein.')
  })

  it('preserves unknown diagnostics and uses a translated fallback for missing details', () => {
    setLocale('de')
    expect(
      apiErrorMessage({ error: { code: 'peel_error', message: 'Unknown variable: invoice' } }),
    ).toBe('Unknown variable: invoice')
    expect(apiErrorMessage(null)).toBe('Die Anfrage ist fehlgeschlagen. Bitte versuche es erneut.')
  })
})
