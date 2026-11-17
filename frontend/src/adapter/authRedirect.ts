export function safeAuthRedirect(value: unknown): string {
  if (typeof value !== 'string' || value.length > 2048 || /[\\%\u0000-\u001f\u007f]/.test(value))
    return '/'
  return /^\/(?:projects\/[^/?#]+(?:\/(?:settings|print-jobs(?:\/[^/?#]+)?))?|invitations\/[A-Za-z0-9_-]{43})(?:[?#].*)?$/.test(
    value,
  )
    ? value
    : '/'
}
