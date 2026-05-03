import Editor, { type OnMount } from '@monaco-editor/react'
import { ChevronDown, ChevronUp } from 'lucide-react'
import { useMemo } from 'react'

import { parseBindings, type ParsedBindings } from '../lib/bindings'

type BindingsEditorPaneProps = {
  bindingsText: string
  onBindingsTextChange: (value: string) => void
  isOpen: boolean
  onToggleOpen: () => void
}

export function BindingsEditorPane({
  bindingsText,
  onBindingsTextChange,
  isOpen,
  onToggleOpen,
}: BindingsEditorPaneProps) {
  const parsedBindings = useMemo<ParsedBindings>(() => parseBindings(bindingsText), [bindingsText])

  const onMount: OnMount = (editor) => {
    editor.updateOptions({ tabSize: 2, fontSize: 14, minimap: { enabled: false } })
  }

  return (
    <div className="flex h-full min-h-0 flex-col border border-slate-800 bg-slate-900/70">
      <div className="border-b border-slate-800">
        <button
          type="button"
          className="flex w-full items-center justify-between px-3 py-2 text-sm font-semibold text-slate-300"
          onClick={onToggleOpen}
        >
          <span>Test Data (JSON bindings)</span>
          {isOpen ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
        </button>
      </div>
      {isOpen ? (
        <>
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
          {parsedBindings.error ? (
            <div className="border-t border-rose-800/70 bg-rose-950/40 px-3 py-2 text-xs text-rose-200">
              {parsedBindings.error}
            </div>
          ) : null}
        </>
      ) : null}
    </div>
  )
}
