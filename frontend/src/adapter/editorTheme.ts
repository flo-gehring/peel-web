import type * as Monaco from 'monaco-editor'

export function definePeelEditorTheme(monaco: typeof Monaco): void {
  monaco.editor.defineTheme('peel-dark', {
    base: 'vs-dark',
    inherit: true,
    rules: [
      { token: 'keyword.peel', foreground: 'c792ea' },
      { token: 'constant.peel', foreground: '82aaff' },
      { token: 'number.peel', foreground: 'f78c6c' },
      { token: 'string.peel', foreground: 'c3e88d' },
      { token: 'string.escape.peel', foreground: '89ddff' },
      { token: 'string.escape.invalid.peel', foreground: 'ff5370' },
      { token: 'comment.peel', foreground: '858e94', fontStyle: 'italic' },
      { token: 'operator.peel', foreground: '89ddff' },
      { token: 'function', foreground: 'ffcf32' },
      { token: 'parameter', foreground: '82aaff' },
      { token: 'variable', foreground: 'f3f1e9' },
      { token: 'variable.readonly', foreground: 'c3e88d' },
    ],
    colors: {
      'editor.background': '#1b1e20',
      'editor.foreground': '#f3f1e9',
      'editorLineNumber.foreground': '#858e94',
      'editorLineNumber.activeForeground': '#ffcf32',
      'editor.lineHighlightBackground': '#24282b',
      'editor.selectionBackground': '#ffcf3233',
      'editorCursor.foreground': '#ffcf32',
      'editorWidget.background': '#24282b',
      'editorWidget.border': '#3b4145',
      'editorIndentGuide.background1': '#343a3e',
    },
  })
}
