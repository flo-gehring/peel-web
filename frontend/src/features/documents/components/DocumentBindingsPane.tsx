import Editor, { type OnMount } from '@monaco-editor/react'

type DocumentBindingsPaneProps = {
  bindingsText: string
  onBindingsTextChange: (value: string) => void
}

export function DocumentBindingsPane({
  bindingsText,
  onBindingsTextChange,
}: DocumentBindingsPaneProps) {
  const onMount: OnMount = (editor) => {
    editor.updateOptions({ tabSize: 2, fontSize: 14, minimap: { enabled: false } })
  }

  return (
    <div className="flex h-full min-h-0 flex-col border border-slate-800 bg-slate-900/70">
      <div className="border-b border-slate-800 px-3 py-2">
        <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-300">Example Data (JSON)</p>
      </div>
      <div className="min-h-0 flex-1 overflow-hidden">
        <Editor
          defaultLanguage="json"
          language="json"
          value={bindingsText}
          onMount={onMount}
          theme="vs-dark"
          onChange={(next) => onBindingsTextChange(next ?? '{}')}
          options={{ automaticLayout: true, scrollBeyondLastLine: false }}
        />
      </div>
    </div>
  )
}
