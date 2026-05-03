export function resolveNodeByPath(root: Record<string, unknown>, path: string): unknown {
  const parts = path.split('.').filter(Boolean)
  let cursor: unknown = root

  for (const part of parts.slice(1)) {
    if (Array.isArray(cursor)) {
      const index = Number(part)
      cursor = cursor[index]
      continue
    }

    if (cursor !== null && typeof cursor === 'object') {
      cursor = (cursor as Record<string, unknown>)[part]
      continue
    }

    return null
  }

  return cursor
}
