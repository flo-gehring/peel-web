import clsx from 'clsx'
import { useMemo, useState } from 'react'

import type { RunResponse } from '../../../lib/api/types'
import { resolveNodeByPath } from '../lib/trace'
import { TraceNode } from './TraceNode'

type ResultViewProps = {
  runData: RunResponse | null
  runErrorMessage: string | null
  saveErrorMessage: string | null
}

type ResultTab = 'trace' | 'result' | 'json'

export function ResultView({ runData, runErrorMessage, saveErrorMessage }: ResultViewProps) {
  const [activeTab, setActiveTab] = useState<ResultTab>('trace')
  const [selectedNodePath, setSelectedNodePath] = useState<string>('trace')

  const selectedNodeValue = useMemo(() => {
    if (!runData) {
      return null
    }
    return resolveNodeByPath(runData.trace, selectedNodePath)
  }, [runData, selectedNodePath])

  return (
    <div className="flex min-h-0 flex-col border border-slate-800 bg-slate-900/70">
      <div className="flex items-center justify-between border-b border-slate-800 px-3 py-2">
        <div className="flex gap-2">
          <TabButton label="Trace" active={activeTab === 'trace'} onClick={() => setActiveTab('trace')} />
          <TabButton
            label="Result"
            active={activeTab === 'result'}
            onClick={() => setActiveTab('result')}
          />
          <TabButton label="Raw JSON" active={activeTab === 'json'} onClick={() => setActiveTab('json')} />
        </div>
        {runErrorMessage ? <p className="text-xs text-rose-300">{runErrorMessage}</p> : null}
        {saveErrorMessage ? <p className="text-xs text-rose-300">{saveErrorMessage}</p> : null}
      </div>

      <div className="min-h-0 flex-1 overflow-hidden">
        {activeTab === 'trace' ? (
          <TracePane
            runData={runData}
            selectedPath={selectedNodePath}
            onSelect={setSelectedNodePath}
            selectedNode={selectedNodeValue}
          />
        ) : null}
        {activeTab === 'result' ? <ResultPane runData={runData} /> : null}
        {activeTab === 'json' ? <JsonPane runData={runData} /> : null}
      </div>
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
  return <div className="flex h-full items-center justify-center px-4 text-sm text-slate-400">{message}</div>
}
