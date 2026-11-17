import { describe, expect, it, vi } from 'vitest'
import { visibleSymbols, registerPeelLanguage } from './peelLanguage'
import type * as Monaco from 'monaco-editor'
import type { components } from '@/types/api'

vi.mock('@/adapter/client', () => ({ api: { POST: vi.fn() } }))
type Analysis = components['schemas']['AnalysisResult']

describe('PEEL completion scopes', () => {
  it('uses the nearest declaration and excludes future and unrelated names', () => {
    const analysis: Analysis = {
      scopes: [
        { id: 0, parentId: -1, start: 0, end: 40, openEnded: true },
        { id: 1, parentId: 0, start: 5, end: 25, openEnded: false },
        { id: 2, parentId: 1, start: 10, end: 20, openEnded: false },
      ],
      symbols: [
        { id: 0, name: 'input', kind: 'VARIABLE', origin: 'EXTERNAL', scopeId: 0, visibleFrom: 0 },
        { id: 1, name: 'x', kind: 'VARIABLE', origin: 'LOCAL', scopeId: 1, visibleFrom: 8 },
        { id: 2, name: 'x', kind: 'PARAMETER', origin: 'LOCAL', scopeId: 2, visibleFrom: 10 },
        { id: 3, name: 'later', kind: 'VARIABLE', origin: 'LOCAL', scopeId: 1, visibleFrom: 22 },
      ],
    }
    expect(visibleSymbols(analysis, 15).map((symbol) => symbol.id)).toEqual([2, 0])
    expect(visibleSymbols(analysis, 21).map((symbol) => symbol.id)).toEqual([1, 0])
    expect(visibleSymbols(analysis, 25).map((symbol) => symbol.id)).toEqual([0])
    expect(visibleSymbols(analysis, 40).map((symbol) => symbol.id)).toEqual([0])
  })
})

describe('PEEL language registration', () => {
  it('registers providers once per Monaco instance', () => {
    const monaco = {
      languages: Object.fromEntries(
        [
          'register',
          'setMonarchTokensProvider',
          'setLanguageConfiguration',
          'registerCompletionItemProvider',
          'registerHoverProvider',
          'registerDocumentSemanticTokensProvider',
        ].map((name) => [name, vi.fn()]),
      ),
    }
    registerPeelLanguage(monaco as unknown as typeof Monaco)
    registerPeelLanguage(monaco as unknown as typeof Monaco)
    expect(monaco.languages.register).toHaveBeenCalledTimes(1)
    expect(monaco.languages.registerCompletionItemProvider).toHaveBeenCalledTimes(1)
  })
})
