import Editor, { type OnMount } from '@monaco-editor/react'

type DocumentJsonEditorPaneProps = {
  title: string
  language: string
  value: string
  onValueChange: (value: string) => void
}

export function DocumentJsonEditorPane({
  title,
  language,
  value,
  onValueChange,
}: DocumentJsonEditorPaneProps) {
  const onMount: OnMount = (editor) => {
    editor.updateOptions({ tabSize: 2, fontSize: 14, minimap: { enabled: false } })
  }

  return (
    <div className="flex h-full min-h-0 flex-col border border-slate-800 bg-slate-900/70">
      <div className="border-b border-slate-800 px-3 py-2">
        <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-300">{title}</p>
      </div>
      <div className="min-h-0 flex-1 overflow-hidden">
        <Editor
          defaultLanguage={language}
          language={language}
          value={value}
          onMount={onMount}
          theme="vs-dark"
          onChange={(next) => onValueChange(next ?? '')}
          options={{ automaticLayout: true, scrollBeyondLastLine: false }}
        />
      </div>
    </div>
  )
}
