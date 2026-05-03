import Editor, { type OnMount } from '@monaco-editor/react'

type DocumentJsonEditorPaneProps = {
  contentText: string
  onContentTextChange: (value: string) => void
}

export function DocumentJsonEditorPane({
  contentText,
  onContentTextChange,
}: DocumentJsonEditorPaneProps) {
  const onMount: OnMount = (editor) => {
    editor.updateOptions({ tabSize: 2, fontSize: 14, minimap: { enabled: false } })
  }

  return (
    <div className="flex h-full min-h-0 flex-col border border-slate-800 bg-slate-900/70">
      <div className="border-b border-slate-800 px-3 py-2">
        <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-300">Document Content (JSON)</p>
      </div>
      <div className="min-h-0 flex-1 overflow-hidden">
        <Editor
          defaultLanguage="json"
          language="json"
          value={contentText}
          onMount={onMount}
          theme="vs-dark"
          onChange={(next) => onContentTextChange(next ?? '{}')}
          options={{ automaticLayout: true, scrollBeyondLastLine: false }}
        />
      </div>
    </div>
  )
}
