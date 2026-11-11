import type * as Monaco from 'monaco-editor'

export function definePeelEditorTheme(monaco: typeof Monaco): void {
  monaco.editor.defineTheme('peel-dark', {
    base: 'vs-dark',
    inherit: true,
    rules: [],
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
