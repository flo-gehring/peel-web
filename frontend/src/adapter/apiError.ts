import { t, type MessageKey } from '@/i18n'
import en from '@/i18n/en.json'

const errorKeys: Record<string, MessageKey> = {
  invalid_email: 'apiInvalidEmail',
  mail_unavailable: 'apiMailUnavailable',
  rate_limited: 'apiRateLimited',
  already_authenticated: 'apiAlreadyAuthenticated',
  unauthenticated: 'apiUnauthenticated',
  csrf_invalid: 'apiCsrfInvalid',
  forbidden: 'apiForbidden',
  auth_unavailable: 'apiAuthUnavailable',
  invalid_link: 'apiInvalidLink',
  internal_error: 'apiInternalError',
  file_in_use: 'apiFileInUse',
  file_already_exists: 'apiFileAlreadyExists',
  file_version_conflict: 'apiFileVersionConflict',
  file_name_conflict: 'apiFileNameConflict',
}
const messageKeys = new Map<string, MessageKey>(
  (Object.keys(en) as MessageKey[])
    .filter((key) => key.startsWith('api'))
    .map((key) => [en[key], key]),
)
export function apiErrorMessage(
  error: unknown,
  fallback = t('theRequestFailedPleaseTryAgain'),
): string {
  if (error && typeof error === 'object' && 'error' in error) {
    const details = error.error
    if (
      details &&
      typeof details === 'object' &&
      'code' in details &&
      typeof details.code === 'string'
    ) {
      const key = errorKeys[details.code]
      if (key) return t(key)
    }
    if (
      details &&
      typeof details === 'object' &&
      'message' in details &&
      typeof details.message === 'string'
    ) {
      // Project validation shares broad codes. Keep unknown script diagnostics verbatim.
      const key = messageKeys.get(details.message)
      return key ? t(key) : details.message
    }
  }
  return fallback
}
