import type * as Monaco from 'monaco-editor'

let registered = false

const KEYWORDS = [
  'var',
  'fun',
  'if',
  'else',
  'while',
  'for',
  'in',
  'return',
  'True',
  'False',
]

export function setupPeelLanguage(monaco: typeof Monaco): void {
  if (registered) {
    return
  }

  monaco.languages.register({ id: 'peel' })
  monaco.languages.setMonarchTokensProvider('peel', {
    keywords: KEYWORDS,
    operators: ['+', '-', '*', '/', '%', '**', '==', '!=', '&&', '||', '^', '?', ':', '='],
    tokenizer: {
      root: [
        [/[a-zA-Z_][a-zA-Z0-9_]*/, {
          cases: {
            '@keywords': 'keyword',
            '@default': 'identifier',
          },
        }],
        [/\d+\.\d+/, 'number.float'],
        [/\d+/, 'number'],
        [/"([^"\\]|\\.)*$/, 'string.invalid'],
        [/"/, { token: 'string.quote', next: '@string' }],
        [/\/\/.*$/, 'comment'],
        [/[{}()[\]]/, '@brackets'],
        [/[;,.]/, 'delimiter'],
        [/(\*\*|==|!=|&&|\|\||[+\-*/%^?:=])/, 'operator'],
      ],
      string: [
        [/[^\\"]+/, 'string'],
        [/\\./, 'string.escape'],
        [/"/, { token: 'string.quote', next: '@pop' }],
      ],
    },
  })

  monaco.languages.setLanguageConfiguration('peel', {
    comments: { lineComment: '//' },
    brackets: [
      ['{', '}'],
      ['(', ')'],
      ['[', ']'],
    ],
    autoClosingPairs: [
      { open: '{', close: '}' },
      { open: '(', close: ')' },
      { open: '[', close: ']' },
      { open: '"', close: '"', notIn: ['string'] },
    ],
    surroundingPairs: [
      { open: '{', close: '}' },
      { open: '(', close: ')' },
      { open: '[', close: ']' },
      { open: '"', close: '"' },
    ],
  })

  registered = true
}
