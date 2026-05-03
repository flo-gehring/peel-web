import Editor, { type BeforeMount, type OnMount } from '@monaco-editor/react'
import { useCallback, useEffect, useRef } from 'react'
import type * as Monaco from 'monaco-editor'

import { setupPeelLanguage } from '../../../lib/monaco/peelLanguage'
import { usePeelValidation } from '../hooks/usePeelValidation'
import { DiagnosticsPanel } from './DiagnosticsPanel'

type ScriptEditorPaneProps = {
  scriptName: string
  onScriptNameChange: (value: string) => void
  script: string
  onScriptChange: (value: string) => void
  onRunShortcut: () => void
}

type MonacoEditor = Parameters<OnMount>[0]
type MonacoApi = Parameters<OnMount>[1]

export function ScriptEditorPane({
  scriptName,
  onScriptNameChange,
  script,
  onScriptChange,
  onRunShortcut,
}: ScriptEditorPaneProps) {
  const editorRef = useRef<MonacoEditor | null>(null)
  const monacoRef = useRef<MonacoApi | null>(null)
  const { diagnostics, scheduleValidation, cancelValidation } = usePeelValidation()

  const applyMarkers = useCallback((markers: Monaco.editor.IMarkerData[]) => {
    if (!editorRef.current || !monacoRef.current) {
      return
    }
    const model = editorRef.current.getModel()
    if (!model) {
      return
    }
    monacoRef.current.editor.setModelMarkers(model, 'peel-validation', markers)
  }, [])

  const onBeforeMount: BeforeMount = useCallback((monaco) => {
    setupPeelLanguage(monaco)
  }, [])

  const onMount: OnMount = useCallback((editor, monaco) => {
    editorRef.current = editor
    monacoRef.current = monaco

    editor.updateOptions({ tabSize: 2, fontSize: 14, minimap: { enabled: false } })
    scheduleValidation(editor.getValue(), applyMarkers)

    editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyCode.Enter, () => {
      onRunShortcut()
    })
  }, [applyMarkers, onRunShortcut, scheduleValidation])

  useEffect(() => {
    return () => {
      cancelValidation()
    }
  }, [cancelValidation])

  return (
    <article className="flex min-h-0 flex-col border border-slate-800 bg-slate-900/70">
      <div className="flex items-center justify-between border-b border-slate-800 px-3 py-2">
        <input
          value={scriptName}
          onChange={(event) => onScriptNameChange(event.target.value)}
          className="w-full bg-transparent text-sm font-semibold text-slate-100 outline-none placeholder:text-slate-500"
          placeholder="Script name"
        />
      </div>
      <div className="min-h-0 flex-1">
        <Editor
          beforeMount={onBeforeMount}
          defaultLanguage="peel"
          language="peel"
          value={script}
          onMount={onMount}
          theme="vs-dark"
          onChange={(next) => {
            const value = next ?? ''
            onScriptChange(value)
            scheduleValidation(value, applyMarkers)
          }}
          options={{ automaticLayout: true, scrollBeyondLastLine: false }}
        />
      </div>
      <DiagnosticsPanel diagnostics={diagnostics} />
    </article>
  )
}
