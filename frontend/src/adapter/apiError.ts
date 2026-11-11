export function apiErrorMessage(
  error: unknown,
  fallback = 'The request failed. Please try again.',
): string {
  if (error && typeof error === 'object' && 'error' in error) {
    const details = error.error
    if (
      details &&
      typeof details === 'object' &&
      'message' in details &&
      typeof details.message === 'string'
    )
      return details.message
  }
  return fallback
}
