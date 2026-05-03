import { ChevronDown, ChevronUp } from 'lucide-react'
import { useMemo, useState } from 'react'

import type { RunResponse } from '../../../lib/api/types'
import { resolveNodeByPath } from '../lib/trace'
import { TraceNode } from './TraceNode'

type ResultViewProps = {
  runData: RunResponse | null
  runErrorMessage: string | null
  saveErrorMessage: string | null
  isResultOpen: boolean
  onToggleResultOpen: () => void
  isDetailOpen: boolean
  onToggleDetailOpen: () => void
}

type DetailMode = 'trace' | 'raw'

export function ResultView({
  runData,
  runErrorMessage,
  saveErrorMessage,
  isResultOpen,
  onToggleResultOpen,
  isDetailOpen,
  onToggleDetailOpen,
}: ResultViewProps) {
  const [detailMode, setDetailMode] = useState<DetailMode>('trace')
  const [selectedNodePath, setSelectedNodePath] = useState<string>('trace')

  const selectedNodeValue = useMemo(() => {
    if (!runData) {
      return null
    }
    return resolveNodeByPath(runData.trace, selectedNodePath)
  }, [runData, selectedNodePath])

  return (
    <div className="flex h-full min-h-0 flex-col overflow-hidden border border-slate-800 bg-slate-900/70">
      <div className="flex items-center justify-between border-b border-slate-800 px-3 py-2">
        <span>Result Value</span>
        <button
          type="button"
          className="flex h-7 w-7 items-center justify-center rounded-md border border-slate-700 text-slate-300 hover:border-cyan-500"
          onClick={onToggleResultOpen}
        >
          {isResultOpen ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
        </button>

      </div>

      {isResultOpen ? (
        <div className="min-h-0 flex-1 overflow-auto px-3 py-3">
          <div className="flex flex-col items-end">
            {runErrorMessage ? <p className="text-xs text-rose-300">{runErrorMessage}</p> : null}
            {saveErrorMessage ? <p className="text-xs text-rose-300">{saveErrorMessage}</p> : null}
          </div>
          <ResultPane runData={runData} />
          <div className="mt-3 rounded-lg border border-slate-800 bg-slate-950/40">
            <div className="flex items-center justify-between gap-2 px-3 py-2">
              <div className="flex items-center gap-2">
                <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-300">Detail</p>
                <div className="rounded-md border border-slate-700 bg-slate-900 p-0.5">
                  <button
                    type="button"
                    className={
                      detailMode === 'trace'
                        ? 'rounded px-2 py-1 text-xs font-semibold text-cyan-100 bg-cyan-500/20'
                        : 'rounded px-2 py-1 text-xs text-slate-400 hover:text-slate-200'
                    }
                    onClick={() => setDetailMode('trace')}
                  >
                    Trace
                  </button>
                  <button
                    type="button"
                    className={
                      detailMode === 'raw'
                        ? 'rounded px-2 py-1 text-xs font-semibold text-cyan-100 bg-cyan-500/20'
                        : 'rounded px-2 py-1 text-xs text-slate-400 hover:text-slate-200'
                    }
                    onClick={() => setDetailMode('raw')}
                  >
                    Raw
                  </button>
                </div>
              </div>

              <button
                type="button"
                className="flex h-7 w-7 items-center justify-center rounded-md border border-slate-700 text-slate-300 hover:border-cyan-500"
                onClick={onToggleDetailOpen}
                aria-label={isDetailOpen ? 'Collapse detail pane' : 'Expand detail pane'}
              >
                {isDetailOpen ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
              </button>
            </div>

            {isDetailOpen ? (
              <div className="h-64 border-t border-slate-800">
                {detailMode === 'trace' ? (
                  <TracePane
                    runData={runData}
                    selectedPath={selectedNodePath}
                    onSelect={setSelectedNodePath}
                    selectedNode={selectedNodeValue}
                  />
                ) : (
                  <JsonPane runData={runData} />
                )}
              </div>
            ) : (
              <CollapsedMessage message="Detail hidden" />
            )}
          </div>
        </div>
      ) : null}
    </div>
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
    <div className="rounded-lg border border-emerald-800/40 bg-emerald-950/20 p-3">
      <pre className="overflow-auto text-xs text-emerald-100">{JSON.stringify(runData.result, null, 2)}</pre>
    </div>
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

function CollapsedMessage({ message }: { message: string }) {
  return <div className="px-3 py-2 text-xs text-slate-500">{message}</div>
}
