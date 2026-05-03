import type { JsonObject } from '../../../lib/api/types'

export type ParsedBindings = {
  value: JsonObject | null
  error: string | null
}

export function parseBindings(bindingsText: string): ParsedBindings {
  try {
    const parsed = JSON.parse(bindingsText) as unknown
    if (parsed === null || Array.isArray(parsed) || typeof parsed !== 'object') {
      throw new Error('Bindings JSON must be an object at the top level.')
    }
    return {
      value: parsed as JsonObject,
      error: null,
    }
  } catch (error) {
    return {
      value: null,
      error: error instanceof Error ? error.message : 'Invalid JSON',
    }
  }
}
