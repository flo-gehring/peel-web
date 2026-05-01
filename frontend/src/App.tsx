import Editor, { type BeforeMount, type OnMount } from '@monaco-editor/react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import clsx from 'clsx'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'

import {
  ApiClientError,
  getScript,
  listScripts,
  runScript,
  saveScript,
  validateScript,
} from './lib/api/client'
import type {
  JsonObject,
  RunResponse,
  ScriptSummary,
  ValidationDiagnostic,
} from './lib/api/types'
import { setupPeelLanguage } from './lib/monaco/peelLanguage'

type MonacoEditor = Parameters<OnMount>[0]
type MonacoApi = Parameters<OnMount>[1]

const DEFAULT_SCRIPT = `var income = gross;
var cost = expenses;

if (income > cost) {
  income - cost;
} else {
  0;
}`

const DEFAULT_BINDINGS = {
  gross: 125000,
  expenses: 83000,
}

const STORAGE_KEY = 'peel-workbench-draft-v1'

type DraftState = {
  script: string
  bindingsText: string
  selectedScriptId: string | null
  scriptName: string
}

function loadDraft(): DraftState {
  const fallback: DraftState = {
    script: DEFAULT_SCRIPT,
    bindingsText: JSON.stringify(DEFAULT_BINDINGS, null, 2),
    selectedScriptId: null,
    scriptName: 'Profit example',
  }

  const rawDraft = localStorage.getItem(STORAGE_KEY)
  if (!rawDraft) {
    return fallback
  }

  try {
    const parsed = JSON.parse(rawDraft) as Partial<DraftState>
    return {
      script: typeof parsed.script === 'string' ? parsed.script : fallback.script,
      bindingsText:
        typeof parsed.bindingsText === 'string' ? parsed.bindingsText : fallback.bindingsText,
      selectedScriptId:
        typeof parsed.selectedScriptId === 'string' ? parsed.selectedScriptId : fallback.selectedScriptId,
      scriptName:
        typeof parsed.scriptName === 'string' && parsed.scriptName.trim().length > 0
          ? parsed.scriptName
          : fallback.scriptName,
    }
  } catch {
    localStorage.removeItem(STORAGE_KEY)
    return fallback
  }
}

function App() {
  const queryClient = useQueryClient()
  const [draftSeed] = useState<DraftState>(() => loadDraft())
  const [script, setScript] = useState<string>(draftSeed.script)
  const [scriptName, setScriptName] = useState<string>(draftSeed.scriptName)
  const [selectedScriptId, setSelectedScriptId] = useState<string | null>(draftSeed.selectedScriptId)
  const [bindingsText, setBindingsText] = useState<string>(draftSeed.bindingsText)
  const [activeTab, setActiveTab] = useState<'trace' | 'result' | 'json'>('trace')
  const [selectedNodePath, setSelectedNodePath] = useState<string>('trace.result')

  const [scriptDiagnostics, setScriptDiagnostics] = useState<ValidationDiagnostic[]>([])

  const scriptEditorRef = useRef<MonacoEditor | null>(null)
  const monacoRef = useRef<MonacoApi | null>(null)
  const validationTimerRef = useRef<number | null>(null)

  const scriptsQuery = useQuery({
    queryKey: ['scripts'],
    queryFn: listScripts,
  })

  useEffect(() => {
    const payload: DraftState = {
      script,
      bindingsText,
      selectedScriptId,
      scriptName,
    }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(payload))
  }, [bindingsText, script, scriptName, selectedScriptId])

  const loadScriptMutation = useMutation({
    mutationFn: getScript,
    onMutate: (id) => {
      setSelectedScriptId(id)
    },
    onSuccess: (loadedScript) => {
      setScript(loadedScript.script)
      setScriptName(loadedScript.name)
      setSelectedNodePath('trace.result')
      queryClient.setQueryData(['script', loadedScript.id], loadedScript)
    },
  })

  const runMutation = useMutation({
    mutationFn: runScript,
    onSuccess: () => {
      setSelectedNodePath('trace.result')
    },
  })

  const saveMutation = useMutation({
    mutationFn: saveScript,
    onSuccess: (saved) => {
      setSelectedScriptId(saved.id)
      setScriptName(saved.name)
      queryClient.invalidateQueries({ queryKey: ['scripts'] })
      queryClient.setQueryData(['script', saved.id], saved)
    },
  })

  const parsedBindingsState = useMemo(() => {
    try {
      const parsed = JSON.parse(bindingsText) as unknown
      if (parsed === null || Array.isArray(parsed) || typeof parsed !== 'object') {
        throw new Error('Bindings JSON must be an object at the top level.')
      }
      return {
        value: parsed as JsonObject,
        error: null as string | null,
      }
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Invalid JSON'
      return {
        value: null,
        error: message,
      }
    }
  }, [bindingsText])

  const parsedBindings = parsedBindingsState.value
  const bindingsError = parsedBindingsState.error

  const runData = runMutation.data
  const selectedNodeValue = useMemo(() => {
    if (!runData) {
      return null
    }
    return resolveNodeByPath(runData.trace, selectedNodePath)
  }, [runData, selectedNodePath])

  const saveDisabled = saveMutation.isPending || script.trim().length === 0
  const runDisabled =
    runMutation.isPending || script.trim().length === 0 || parsedBindings === null

  const runErrorMessage = formatError(runMutation.error)
  const saveErrorMessage = formatError(saveMutation.error)

  const onScriptEditorBeforeMount: BeforeMount = (monaco) => {
    setupPeelLanguage(monaco)
  }

  const onScriptEditorMount: OnMount = (editor, monaco) => {
    scriptEditorRef.current = editor
    monacoRef.current = monaco
    editor.updateOptions({ tabSize: 2, fontSize: 14, minimap: { enabled: false } })
    triggerValidation(editor.getValue())
  }

  const onBindingsEditorMount: OnMount = (editor) => {
    editor.updateOptions({ tabSize: 2, fontSize: 14, minimap: { enabled: false } })
  }

  function triggerValidation(nextScript: string) {
    if (validationTimerRef.current !== null) {
      window.clearTimeout(validationTimerRef.current)
    }
    validationTimerRef.current = window.setTimeout(async () => {
      if (!nextScript.trim()) {
        setScriptDiagnostics([])
        applyMarkers([])
        return
      }
      try {
        const response = await validateScript({ script: nextScript })
        setScriptDiagnostics(response.diagnostics)
        applyMarkers(response.diagnostics)
      } catch (error) {
        const fallbackDiagnostic: ValidationDiagnostic = {
          line: 1,
          column: 1,
          endLine: 1,
          endColumn: 2,
          severity: 'error',
          message: formatError(error) ?? 'Validation request failed',
        }
        setScriptDiagnostics([fallbackDiagnostic])
        applyMarkers([fallbackDiagnostic])
      }
    }, 300)
  }

  function applyMarkers(diagnostics: ValidationDiagnostic[]) {
    if (!scriptEditorRef.current || !monacoRef.current) {
      return
    }
    const model = scriptEditorRef.current.getModel()
    if (!model) {
      return
    }
    monacoRef.current.editor.setModelMarkers(
      model,
      'peel-validation',
      diagnostics.map((diagnostic) => ({
        startLineNumber: Math.max(1, diagnostic.line),
        startColumn: Math.max(1, diagnostic.column),
        endLineNumber: Math.max(diagnostic.line, diagnostic.endLine),
        endColumn: Math.max(2, diagnostic.endColumn),
        message: diagnostic.message,
        severity: mapSeverity(monacoRef.current!, diagnostic.severity),
      })),
    )
  }

  const handleRun = useCallback(() => {
    if (!parsedBindings) {
      return
    }
    runMutation.mutate({ script, bindings: parsedBindings })
  }, [parsedBindings, runMutation, script])

  function handleSave() {
    saveMutation.mutate({
      id: selectedScriptId ?? undefined,
      name: scriptName.trim().length > 0 ? scriptName.trim() : 'Untitled script',
      script,
    })
  }

  function handleNewScript() {
    setSelectedScriptId(null)
    setScriptName('Untitled script')
    setScript(DEFAULT_SCRIPT)
  }

  function handleDuplicate() {
    setSelectedScriptId(null)
    setScriptName(`${scriptName} copy`)
  }

  const handleSelectScript = useCallback(
    (id: string) => {
      loadScriptMutation.mutate(id)
    },
    [loadScriptMutation],
  )

  useEffect(() => {
    return () => {
      if (validationTimerRef.current !== null) {
        window.clearTimeout(validationTimerRef.current)
      }
    }
  }, [])

  useEffect(() => {
    const listener = (event: KeyboardEvent) => {
      if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
        event.preventDefault()
        if (!runDisabled) {
          handleRun()
        }
      }
    }
    window.addEventListener('keydown', listener)
    return () => window.removeEventListener('keydown', listener)
  }, [handleRun, runDisabled])

  const scripts = scriptsQuery.data ?? []

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      <div className="mx-auto flex h-screen max-w-[1800px] flex-col px-4 py-4 sm:px-6">
        <header className="mb-4 flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-slate-800/80 bg-slate-900/80 px-4 py-3 shadow-[0_12px_40px_-18px_rgba(56,189,248,0.55)] backdrop-blur">
          <div>
            <p className="text-xs uppercase tracking-[0.24em] text-cyan-300/80">Peel Workbench</p>
            <h1 className="font-serif text-2xl text-slate-50">Script Runner</h1>
          </div>
          <div className="flex items-center gap-2">
            <button
              type="button"
              className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm hover:border-cyan-500"
              onClick={handleNewScript}
            >
              New
            </button>
            <button
              type="button"
              className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm hover:border-cyan-500"
              onClick={handleDuplicate}
            >
              Duplicate
            </button>
            <button
              type="button"
              className={clsx(
                'rounded-lg border px-3 py-2 text-sm font-medium transition',
                saveDisabled
                  ? 'cursor-not-allowed border-slate-800 bg-slate-900 text-slate-500'
                  : 'border-emerald-500/70 bg-emerald-500/10 text-emerald-200 hover:bg-emerald-500/20',
              )}
              onClick={handleSave}
              disabled={saveDisabled}
            >
              {saveMutation.isPending ? 'Saving...' : 'Save'}
            </button>
            <button
              type="button"
              className={clsx(
                'rounded-lg border px-4 py-2 text-sm font-semibold transition',
                runDisabled
                  ? 'cursor-not-allowed border-slate-800 bg-slate-900 text-slate-500'
                  : 'border-cyan-400 bg-cyan-400/10 text-cyan-100 hover:bg-cyan-400/20',
              )}
              onClick={handleRun}
              disabled={runDisabled}
            >
              {runMutation.isPending ? 'Running...' : 'Run (Ctrl/Cmd+Enter)'}
            </button>
          </div>
        </header>

        <div className="grid flex-1 gap-4 lg:grid-cols-[260px_1fr]">
          <aside className="flex min-h-0 flex-col rounded-2xl border border-slate-800 bg-slate-900/70 p-3">
            <h2 className="mb-2 text-sm font-semibold tracking-wide text-slate-300">Scripts</h2>
            {scriptsQuery.isLoading && <p className="text-sm text-slate-400">Loading scripts...</p>}
            {scriptsQuery.isError && (
              <p className="text-sm text-rose-300">{formatError(scriptsQuery.error) ?? 'Failed to load scripts'}</p>
            )}
            <div className="space-y-1 overflow-y-auto pr-1">
              {scripts.map((scriptSummary) => (
                <ScriptListItem
                  key={scriptSummary.id}
                  script={scriptSummary}
                  selected={selectedScriptId === scriptSummary.id}
                  onClick={() => handleSelectScript(scriptSummary.id)}
                />
              ))}
              {scripts.length === 0 && !scriptsQuery.isLoading ? (
                <p className="text-sm text-slate-500">No saved scripts yet.</p>
              ) : null}
            </div>
          </aside>

          <section className="grid min-h-0 gap-4 lg:grid-cols-[1.2fr_1fr]">
            <article className="flex min-h-0 flex-col rounded-2xl border border-slate-800 bg-slate-900/70">
              <div className="flex items-center justify-between border-b border-slate-800 px-3 py-2">
                <input
                  value={scriptName}
                  onChange={(event) => setScriptName(event.target.value)}
                  className="w-full bg-transparent text-sm font-semibold text-slate-100 outline-none placeholder:text-slate-500"
                  placeholder="Script name"
                />
              </div>
              <div className="min-h-0 flex-1">
                <Editor
                  beforeMount={onScriptEditorBeforeMount}
                  defaultLanguage="peel"
                  language="peel"
                  value={script}
                  onMount={onScriptEditorMount}
                  theme="vs-dark"
                  onChange={(next) => {
                    const value = next ?? ''
                    setScript(value)
                    triggerValidation(value)
                  }}
                  options={{ automaticLayout: true, scrollBeyondLastLine: false }}
                />
              </div>
              <DiagnosticsPanel diagnostics={scriptDiagnostics} />
            </article>

            <article className="grid min-h-0 grid-rows-[0.9fr_1.1fr] gap-4">
              <div className="flex min-h-0 flex-col rounded-2xl border border-slate-800 bg-slate-900/70">
                <div className="border-b border-slate-800 px-3 py-2 text-sm font-semibold text-slate-300">
                  Test Data (JSON bindings)
                </div>
                <div className="min-h-0 flex-1">
                  <Editor
                    defaultLanguage="json"
                    language="json"
                    value={bindingsText}
                    onMount={onBindingsEditorMount}
                    theme="vs-dark"
                    onChange={(next) => setBindingsText(next ?? '{}')}
                    options={{ automaticLayout: true, scrollBeyondLastLine: false }}
                  />
                </div>
                {bindingsError ? (
                  <div className="border-t border-rose-800/70 bg-rose-950/40 px-3 py-2 text-xs text-rose-200">
                    {bindingsError}
                  </div>
                ) : null}
              </div>

              <div className="flex min-h-0 flex-col rounded-2xl border border-slate-800 bg-slate-900/70">
                <div className="flex items-center justify-between border-b border-slate-800 px-3 py-2">
                  <div className="flex gap-2">
                    <TabButton
                      label="Trace"
                      active={activeTab === 'trace'}
                      onClick={() => setActiveTab('trace')}
                    />
                    <TabButton
                      label="Result"
                      active={activeTab === 'result'}
                      onClick={() => setActiveTab('result')}
                    />
                    <TabButton
                      label="Raw JSON"
                      active={activeTab === 'json'}
                      onClick={() => setActiveTab('json')}
                    />
                  </div>
                  {runErrorMessage ? <p className="text-xs text-rose-300">{runErrorMessage}</p> : null}
                  {saveErrorMessage ? <p className="text-xs text-rose-300">{saveErrorMessage}</p> : null}
                </div>
                <div className="min-h-0 flex-1 overflow-hidden">
                  {activeTab === 'trace' ? (
                    <TracePane
                      runData={runData ?? null}
                      selectedPath={selectedNodePath}
                      onSelect={setSelectedNodePath}
                      selectedNode={selectedNodeValue}
                    />
                  ) : null}
                  {activeTab === 'result' ? <ResultPane runData={runData ?? null} /> : null}
                  {activeTab === 'json' ? <JsonPane runData={runData ?? null} /> : null}
                </div>
              </div>
            </article>
          </section>
        </div>
      </div>
    </div>
  )
}

function ScriptListItem({
  script,
  selected,
  onClick,
}: {
  script: ScriptSummary
  selected: boolean
  onClick: () => void
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={clsx(
        'w-full rounded-lg border px-2 py-2 text-left text-sm transition',
        selected
          ? 'border-cyan-500 bg-cyan-500/10 text-cyan-100'
          : 'border-transparent bg-slate-900 text-slate-300 hover:border-slate-700 hover:bg-slate-800/70',
      )}
    >
      {script.name}
    </button>
  )
}

function DiagnosticsPanel({ diagnostics }: { diagnostics: ValidationDiagnostic[] }) {
  if (diagnostics.length === 0) {
    return (
      <div className="border-t border-slate-800 bg-slate-950/40 px-3 py-2 text-xs text-emerald-300">
        Validation passed.
      </div>
    )
  }

  return (
    <div className="max-h-28 overflow-auto border-t border-rose-900/70 bg-rose-950/30 px-3 py-2 text-xs text-rose-100">
      {diagnostics.map((diagnostic, index) => (
        <div key={`${diagnostic.line}-${diagnostic.column}-${index}`}>
          Ln {diagnostic.line}, Col {diagnostic.column}: {diagnostic.message}
        </div>
      ))}
    </div>
  )
}

function TabButton({
  label,
  active,
  onClick,
}: {
  label: string
  active: boolean
  onClick: () => void
}) {
  return (
    <button
      type="button"
      className={clsx(
        'rounded-md px-2 py-1 text-xs font-medium transition',
        active
          ? 'bg-cyan-500/20 text-cyan-100'
          : 'bg-slate-900 text-slate-400 hover:bg-slate-800 hover:text-slate-200',
      )}
      onClick={onClick}
    >
      {label}
    </button>
  )
}

function TracePane({
  runData,
  selectedPath,
  onSelect,
  selectedNode,
}: {
  runData: RunResponse | null
  selectedPath: string
  onSelect: (path: string) => void
  selectedNode: unknown
}) {
  if (!runData) {
    return <EmptyPane message="Run a script to inspect the execution trace." />
  }

  return (
    <div className="grid h-full grid-cols-[1.1fr_0.9fr]">
      <div className="overflow-auto border-r border-slate-800 px-3 py-3">
        <TraceNode
          label="trace"
          value={runData.trace}
          path="trace"
          selectedPath={selectedPath}
          onSelect={onSelect}
          depth={0}
        />
      </div>
      <div className="overflow-auto px-3 py-3 text-xs">
        <p className="mb-2 text-[11px] uppercase tracking-[0.15em] text-slate-400">Node details</p>
        <pre className="whitespace-pre-wrap break-words rounded-lg border border-slate-800 bg-slate-950/60 p-2 text-slate-200">
          {JSON.stringify(selectedNode ?? null, null, 2)}
        </pre>
      </div>
    </div>
  )
}

function ResultPane({ runData }: { runData: RunResponse | null }) {
  if (!runData) {
    return <EmptyPane message="Run a script to view the computed result." />
  }
  return (
    <pre className="h-full overflow-auto p-3 text-xs text-slate-200">
      {JSON.stringify(runData.result, null, 2)}
    </pre>
  )
}

function JsonPane({ runData }: { runData: RunResponse | null }) {
  if (!runData) {
    return <EmptyPane message="Run a script to inspect raw JSON payloads." />
  }
  return (
    <pre className="h-full overflow-auto p-3 text-xs text-slate-200">
      {JSON.stringify(runData, null, 2)}
    </pre>
  )
}

function EmptyPane({ message }: { message: string }) {
  return (
    <div className="flex h-full items-center justify-center px-4 text-sm text-slate-400">{message}</div>
  )
}

function TraceNode({
  label,
  value,
  path,
  selectedPath,
  onSelect,
  depth,
}: {
  label: string
  value: unknown
  path: string
  selectedPath: string
  onSelect: (path: string) => void
  depth: number
}) {
  const isObject = value !== null && typeof value === 'object'
  const isArray = Array.isArray(value)
  const entries = isObject
    ? isArray
      ? value.map((item, index) => [String(index), item] as const)
      : Object.entries(value as Record<string, unknown>)
    : []

  const preview = renderPreview(value)

  return (
    <div>
      <button
        type="button"
        onClick={() => onSelect(path)}
        className={clsx(
          'mb-1 w-full rounded px-2 py-1 text-left text-xs transition',
          selectedPath === path
            ? 'bg-cyan-500/20 text-cyan-100'
            : 'text-slate-200 hover:bg-slate-800/70',
        )}
        style={{ paddingLeft: `${8 + depth * 14}px` }}
      >
        <span className="text-slate-400">{label}</span>
        <span className="ml-2 text-slate-200">{preview}</span>
      </button>
      {entries.map(([childLabel, childValue]) => (
        <TraceNode
          key={`${path}.${childLabel}`}
          label={childLabel}
          value={childValue}
          path={`${path}.${childLabel}`}
          selectedPath={selectedPath}
          onSelect={onSelect}
          depth={depth + 1}
        />
      ))}
    </div>
  )
}

function renderPreview(value: unknown): string {
  if (value === null) {
    return 'null'
  }
  if (Array.isArray(value)) {
    return `[${value.length}]`
  }
  if (typeof value === 'object') {
    const typeValue = (value as Record<string, unknown>).type
    if (typeof typeValue === 'string') {
      return `{${typeValue}}`
    }
    return '{...}'
  }
  if (typeof value === 'string') {
    return `"${value}"`
  }
  return String(value)
}

function resolveNodeByPath(root: Record<string, unknown>, path: string): unknown {
  const parts = path.split('.').filter(Boolean)
  let cursor: unknown = root
  for (const part of parts.slice(1)) {
    if (Array.isArray(cursor)) {
      const index = Number(part)
      cursor = cursor[index]
      continue
    }
    if (cursor !== null && typeof cursor === 'object') {
      cursor = (cursor as Record<string, unknown>)[part]
      continue
    }
    return null
  }
  return cursor
}

function mapSeverity(monaco: MonacoApi, severity: ValidationDiagnostic['severity']): number {
  switch (severity) {
    case 'info':
      return monaco.MarkerSeverity.Info
    case 'warning':
      return monaco.MarkerSeverity.Warning
    default:
      return monaco.MarkerSeverity.Error
  }
}

function formatError(error: unknown): string | null {
  if (!error) {
    return null
  }
  if (error instanceof ApiClientError) {
    return error.message
  }
  if (error instanceof Error) {
    return error.message
  }
  return 'Unexpected error'
}

export default App
