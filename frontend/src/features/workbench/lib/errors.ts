import { ApiClientError } from '../../../lib/api/client'

export function formatError(error: unknown): string | null {
  if (!error) {
    return null
  }

  if (error instanceof ApiClientError) {
    return error.message
  }

  if (error instanceof Error) {
    return error.message
  }

  return 'Unexpected error'
}
