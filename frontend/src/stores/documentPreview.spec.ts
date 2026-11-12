import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useDocumentPreviewStore } from './documentPreview'

describe('document preview ownership', () => {
  afterEach(() => vi.restoreAllMocks())
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:preview')
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {})
  })
  it('keeps results attached to their document when requests finish out of order', () => {
    const store = useDocumentPreviewStore()
    const first = store.start('first', 'First', 'input-a')
    const second = store.start('second', 'Second', 'input-b')
    store.setPreview('second', second, btoa('second-pdf'))
    store.setError('first', first, 'First failed')
    expect(store.previews.second?.pdfUrl).toBe('blob:preview')
    expect(store.previews.second?.name).toBe('Second')
    expect(store.previews.first?.error).toBe('First failed')
  })
  it('ignores superseded responses and revokes URLs when clearing the workspace', () => {
    const store = useDocumentPreviewStore()
    const old = store.start('file', 'Old', 'old')
    const current = store.start('file', 'Current', 'current')
    store.setPreview('file', old, btoa('old-pdf'))
    expect(store.previews.file?.pdfUrl).toBeNull()
    store.setPreview('file', current, btoa('new-pdf'))
    store.clear()
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:preview')
    store.setPreview('file', current, btoa('late-pdf'))
    expect(store.previews).toEqual({})
  })
})
