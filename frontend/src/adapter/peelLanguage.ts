import type * as Monaco from 'monaco-editor'
import { api } from '@/adapter/client'
import { t } from '@/i18n'
import type { components } from '@/types/api'
import type { ExpectedBinding } from '@/stores/projectFiles'

type Analysis = components['schemas']['AnalysisResult']
type Symbol = NonNullable<Analysis['symbols']>[number]
type Context = { projectId: string; expectedBindings: ExpectedBinding[] }
type Session = {
  model: Monaco.editor.ITextModel
  context: () => Context
  analysis?: Analysis
  runtime: Symbol[]
  refresh: () => void
}
const sessions = new Map<string, Session>()
type MonacoApi = Pick<typeof Monaco, 'languages' | 'editor' | 'MarkerSeverity'>
const registered = new WeakSet<MonacoApi>()
const semanticListeners = new Set<() => void>()
const changed = () => semanticListeners.forEach((listener) => listener())
export const peelKeywords = ['var', 'fun', 'if', 'else', 'while', 'for', 'in', 'return']
export const peelLanguage: Monaco.languages.IMonarchLanguage = {
  tokenPostfix: '.peel',
  keywords: peelKeywords,
  booleans: ['True', 'False'],
  tokenizer: {
    root: [
      [/[a-zA-Z_][a-zA-Z0-9_]*(?=\s*:\s*\{)/, 'type.identifier'],
      [/\/\/.*$/, 'comment'],
      [
        /"/,
        {
          cases: { '@eos': 'string.quote', '@default': { token: 'string.quote', next: '@string' } },
        },
      ],
      [
        /[a-zA-Z_][a-zA-Z0-9_]*/,
        { cases: { '@keywords': 'keyword', '@booleans': 'constant', '@default': 'identifier' } },
      ],
      [/[0-9]+\.[0-9]+/, 'number.float'],
      [/[0-9]+/, 'number'],
      [/\*\*|==|!=|&&|\|\||[+\-*/%=!^?:]/, 'operator'],
      [/[{}()[\]]/, '@brackets'],
      [/[;,]/, 'delimiter'],
      [/[ \t]+/, 'white'],
    ],
    string: [
      [
        /\\["\\nrt]/,
        {
          cases: { '@eos': { token: 'string.escape', next: '@pop' }, '@default': 'string.escape' },
        },
      ],
      [
        /\\./,
        {
          cases: {
            '@eos': { token: 'string.escape.invalid', next: '@pop' },
            '@default': 'string.escape.invalid',
          },
        },
      ],
      [/\\$/, { token: 'string.escape.invalid', next: '@pop' }],
      [/"/, { token: 'string.quote', next: '@pop' }],
      [/[^"\\]+/, { cases: { '@eos': { token: 'string', next: '@pop' }, '@default': 'string' } }],
    ],
  },
}

export function visibleSymbols(analysis: Analysis, offset: number): Symbol[] {
  const scopes = analysis.scopes ?? []
  const active = scopes.filter(
    (scope) =>
      (scope.start ?? 0) <= offset &&
      (offset < (scope.end ?? 0) || (scope.openEnded && offset === scope.end)),
  )
  const ids = new Set(active.map((scope) => scope.id))
  const result = new Map<string, Symbol>()
  for (const symbol of [...(analysis.symbols ?? [])].reverse()) {
    if (
      ids.has(symbol.scopeId) &&
      (symbol.visibleFrom ?? 0) <= offset &&
      symbol.name &&
      !result.has(symbol.name)
    )
      result.set(symbol.name, symbol)
  }
  return [...result.values()]
}

function range(monacoModel: Monaco.editor.ITextModel, start = 0, end = start) {
  const a = monacoModel.getPositionAt(start),
    b = monacoModel.getPositionAt(end)
  return {
    startLineNumber: a.lineNumber,
    startColumn: a.column,
    endLineNumber: b.lineNumber,
    endColumn: b.column,
  }
}
function inText(
  monaco: MonacoApi,
  model: Monaco.editor.ITextModel,
  position: Monaco.Position,
): boolean {
  const line = model.getLineContent(position.lineNumber)
  const tokens = monaco.editor.tokenize(line, 'peel')[0] ?? []
  const offset = Math.max(0, position.column - 2)
  return tokens.some(
    (token, i) =>
      token.offset <= offset &&
      (tokens[i + 1]?.offset ?? line.length + 1) > offset &&
      (token.type.startsWith('string') || token.type.startsWith('comment')),
  )
}

export function registerPeelLanguage(monaco: MonacoApi): void {
  if (registered.has(monaco)) return
  registered.add(monaco)
  monaco.languages.register({ id: 'peel', extensions: ['.peel'], aliases: ['PEEL'] })
  monaco.languages.setMonarchTokensProvider('peel', peelLanguage)
  monaco.languages.setLanguageConfiguration('peel', {
    comments: { lineComment: '//' },
    brackets: [
      ['{', '}'],
      ['[', ']'],
      ['(', ')'],
    ],
    autoClosingPairs: [
      { open: '{', close: '}' },
      { open: '[', close: ']' },
      { open: '(', close: ')' },
      { open: '"', close: '"', notIn: ['string', 'comment'] },
    ],
    surroundingPairs: [
      { open: '{', close: '}' },
      { open: '[', close: ']' },
      { open: '(', close: ')' },
      { open: '"', close: '"' },
    ],
    indentationRules: { increaseIndentPattern: /\{\s*$/, decreaseIndentPattern: /^\s*\}/ },
    wordPattern: /[a-zA-Z_][a-zA-Z0-9_]*/,
  })
  monaco.languages.registerCompletionItemProvider('peel', {
    provideCompletionItems(model, position) {
      const session = sessions.get(model.uri.toString())
      if (!session || inText(monaco, model, position)) return { suggestions: [] }
      const word = model.getWordUntilPosition(position)
      const replace = {
        startLineNumber: position.lineNumber,
        endLineNumber: position.lineNumber,
        startColumn: word.startColumn,
        endColumn: word.endColumn,
      }
      const symbols = session.analysis
        ? visibleSymbols(session.analysis, model.getOffsetAt(position))
        : [
            ...session.context().expectedBindings.map((binding, i) => ({
              id: -i - 1,
              name: binding.name,
              description: binding.description,
              kind: 'VARIABLE',
              origin: 'EXTERNAL',
              arity: -1,
            })),
            ...session.runtime,
          ]
      const suggestions: Monaco.languages.CompletionItem[] = symbols
        .filter((symbol) => !!symbol.name)
        .map((symbol) => ({
          label: symbol.name!,
          insertText: symbol.name!,
          range: replace,
          kind:
            symbol.kind === 'FUNCTION'
              ? monaco.languages.CompletionItemKind.Function
              : monaco.languages.CompletionItemKind.Variable,
          detail:
            symbol.origin === 'EXTERNAL'
              ? t('externalBinding')
              : symbol.kind === 'FUNCTION'
                ? symbol.name +
                  '(' +
                  Array.from(
                    { length: Math.max(0, symbol.arity ?? 0) },
                    (_, i) => 'arg' + (i + 1),
                  ).join(', ') +
                  ')'
                : symbol.kind,
          documentation: symbol.description ?? '',
          sortText: (symbol.origin === 'LOCAL' ? '0' : '1') + symbol.name,
        }))
      suggestions.push(
        ...peelKeywords.concat(['True', 'False']).map((keyword) => ({
          label: keyword,
          insertText: keyword,
          kind: monaco.languages.CompletionItemKind.Keyword,
          range: replace,
          sortText: '2' + keyword,
        })),
      )
      return { suggestions }
    },
  })
  monaco.languages.registerHoverProvider('peel', {
    provideHover(model, position) {
      const analysis = sessions.get(model.uri.toString())?.analysis
      const offset = model.getOffsetAt(position)
      const occurrence = analysis?.occurrences?.find(
        (item) => (item.start ?? 0) <= offset && offset < (item.end ?? 0),
      )
      const symbol = analysis?.symbols?.find((item) => item.id === occurrence?.symbolId)
      if (!occurrence || !symbol) return null
      return {
        range: range(model, occurrence.start, occurrence.end),
        contents: [
          { value: symbol.name ?? '' },
          { value: symbol.origin === 'EXTERNAL' ? t('externalBinding') : (symbol.kind ?? '') },
          { value: symbol.description ?? '' },
        ],
      }
    },
  })
  monaco.languages.registerDocumentSemanticTokensProvider('peel', {
    getLegend: () => ({
      tokenTypes: ['variable', 'function', 'parameter'],
      tokenModifiers: ['declaration', 'readonly'],
    }),
    onDidChange: (listener) => {
      semanticListeners.add(listener)
      return { dispose: () => semanticListeners.delete(listener) }
    },
    provideDocumentSemanticTokens(model) {
      const analysis = sessions.get(model.uri.toString())?.analysis
      const data: number[] = []
      let line = 0,
        column = 0
      for (const item of [...(analysis?.occurrences ?? [])].sort(
        (a, b) => (a.start ?? 0) - (b.start ?? 0),
      )) {
        const symbol = analysis?.symbols?.find((symbol) => symbol.id === item.symbolId)
        if (!symbol) continue
        const position = model.getPositionAt(item.start ?? 0)
        const nextLine = position.lineNumber - 1,
          nextColumn = position.column - 1
        const type = symbol.kind === 'FUNCTION' ? 1 : symbol.kind === 'PARAMETER' ? 2 : 0
        data.push(
          nextLine - line,
          nextLine === line ? nextColumn - column : nextColumn,
          (item.end ?? 0) - (item.start ?? 0),
          type,
          (item.role === 'DECLARATION' ? 1 : 0) | (symbol.origin === 'EXTERNAL' ? 2 : 0),
        )
        line = nextLine
        column = nextColumn
      }
      return { data: new Uint32Array(data) }
    },
    releaseDocumentSemanticTokens() {},
  })
}

export function bindPeelModel(
  monaco: MonacoApi,
  model: Monaco.editor.ITextModel,
  context: () => Context,
  availability: (available: boolean) => void,
) {
  const key = model.uri.toString()
  let generation = 0
  let timer: ReturnType<typeof setTimeout> | undefined
  let controller: AbortController | undefined
  let disposed = false
  const session: Session = { model, context, runtime: [], refresh }
  sessions.set(key, session)
  function refresh(): void {
    if (disposed) return
    const requestGeneration = ++generation
    clearTimeout(timer)
    controller?.abort()
    session.analysis = undefined
    monaco.editor.setModelMarkers(model, 'peel-analysis', [])
    changed()
    const snapshot = context()
    const expectedBindings = snapshot.expectedBindings.map((binding) => ({ ...binding }))
    const script = model.getValue()
    timer = setTimeout(async () => {
      controller = new AbortController()
      try {
        const { data, error } = await api.POST('/api/scripts/analyze', {
          body: { projectId: { id: snapshot.projectId }, script, expectedBindings },
          signal: controller.signal,
        })
        if (disposed || generation !== requestGeneration || model.isDisposed()) return
        if (error || !data) {
          availability(false)
          return
        }
        session.analysis = data
        session.runtime = (data.symbols ?? []).filter((symbol) => symbol.origin === 'RUNTIME')
        monaco.editor.setModelMarkers(
          model,
          'peel-analysis',
          (data.diagnostics ?? []).map((item) => ({
            ...range(model, item.start, item.end),
            severity:
              item.severity === 'WARNING'
                ? monaco.MarkerSeverity.Warning
                : monaco.MarkerSeverity.Error,
            message: item.message ?? '',
            code: item.code,
          })),
        )
        availability(true)
        changed()
      } catch {
        if (!disposed && generation === requestGeneration) availability(false)
      }
    }, 300)
  }
  const contentListener = model.onDidChangeContent(refresh)
  refresh()
  return {
    refresh,
    dispose(): void {
      disposed = true
      generation++
      clearTimeout(timer)
      controller?.abort()
      contentListener.dispose()
      if (sessions.get(key) === session) sessions.delete(key)
      if (!model.isDisposed()) monaco.editor.setModelMarkers(model, 'peel-analysis', [])
      changed()
    },
  }
}
