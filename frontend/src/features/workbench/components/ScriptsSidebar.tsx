import type { UseQueryResult } from '@tanstack/react-query'

import type { ScriptSummary } from '../../../lib/api/types'
import { formatError } from '../lib/errors'
import { ScriptListItem } from './ScriptListItem'

type ScriptsSidebarProps = {
  scriptsQuery: UseQueryResult<ScriptSummary[], Error>
  scripts: ScriptSummary[]
  selectedScriptId: string | null
  onSelectScript: (id: string) => void
}

export function ScriptsSidebar({
  scriptsQuery,
  scripts,
  selectedScriptId,
  onSelectScript,
}: ScriptsSidebarProps) {
  return (
    <aside className="flex min-h-0 flex-col border border-slate-800 bg-slate-900/70 p-3">
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
