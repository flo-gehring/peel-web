import type { UseQueryResult } from '@tanstack/react-query'
import { ChevronLeft, ChevronRight } from 'lucide-react'

import type { ScriptSummary } from '../../../lib/api/types'
import { formatError } from '../lib/errors'
import { ScriptListItem } from './ScriptListItem'

type ScriptsSidebarProps = {
  scriptsQuery: UseQueryResult<ScriptSummary[], Error>
  scripts: ScriptSummary[]
  selectedScriptId: string | null
  isOpen: boolean
  onToggle: () => void
  onSelectScript: (id: string) => void
}

export function ScriptsSidebar({
  scriptsQuery,
  scripts,
  selectedScriptId,
  isOpen,
  onToggle,
  onSelectScript,
}: ScriptsSidebarProps) {
  if (!isOpen) {
    return (
      <aside className="flex min-h-0 flex-col items-center gap-2 border border-slate-800 bg-slate-900/70 p-2">
        <button
          type="button"
          className="flex h-9 w-9 items-center justify-center rounded-md border border-slate-700 bg-slate-900 text-slate-200 hover:border-cyan-500"
          onClick={onToggle}
          aria-label="Expand scripts sidebar"
        >
          <ChevronRight size={16} />
        </button>
      </aside>
    )
  }

  return (
    <aside className="flex min-h-0 flex-col border border-slate-800 bg-slate-900/70 p-3">
      <div className="mb-2 flex items-center justify-between gap-2">
        <h2 className="text-sm font-semibold tracking-wide text-slate-300">Scripts</h2>
        <button
          type="button"
          className="flex h-8 w-8 items-center justify-center rounded-md border border-slate-700 bg-slate-900 text-slate-300 hover:border-cyan-500"
          onClick={onToggle}
          aria-label="Collapse scripts sidebar"
        >
          <ChevronLeft size={16} />
        </button>
      </div>
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
            onClick={() => onSelectScript(scriptSummary.id)}
          />
        ))}
        {scripts.length === 0 && !scriptsQuery.isLoading ? (
          <p className="text-sm text-slate-500">No saved scripts yet.</p>
        ) : null}
      </div>
    </aside>
  )
}
