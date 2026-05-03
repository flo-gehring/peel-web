import clsx from 'clsx'

import type { ScriptSummary } from '../../../lib/api/types'

type ScriptListItemProps = {
  script: ScriptSummary
  selected: boolean
  onClick: () => void
}

export function ScriptListItem({ script, selected, onClick }: ScriptListItemProps) {
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
