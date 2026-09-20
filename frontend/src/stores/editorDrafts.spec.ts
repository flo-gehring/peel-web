import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useEditorDraftStore } from './editorDrafts'

describe('editor drafts', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('does not overwrite an existing draft while loading', () => {
    const store = useEditorDraftStore()

    store.setDraft('script-1', 'unsaved content')
    store.markLoaded('script-1', 'server content')

    expect(store.getDraft('script-1')).toBe('unsaved content')
  })

  it('moves a panel draft to its saved document id', () => {
    const store = useEditorDraftStore()

    store.setDraft('editor-script-new', 'draft content')
    store.bindPanelToDocument('editor-script-new', 'script-1')

    expect(store.getDraft('editor-script-new')).toBeUndefined()
    expect(store.getDraft('script-1')).toBe('draft content')
    expect(store.getDocumentIdForPanel('editor-script-new')).toBe('script-1')
  })

  it('removes only the deleted document and related panel mappings', () => {
    const store = useEditorDraftStore()

    store.setDraft('script-1', 'first')
    store.setDraft('script-2', 'second')
    store.bindPanelToDocument('editor-script-1', 'script-1')
    store.bindPanelToDocument('editor-script-2', 'script-2')
    store.removeDocument('script-1')

    expect(store.getDraft('script-1')).toBeUndefined()
    expect(store.getDocumentIdForPanel('editor-script-1')).toBeUndefined()
    expect(store.getDraft('script-2')).toBe('second')
    expect(store.getDocumentIdForPanel('editor-script-2')).toBe('script-2')
  })
})
