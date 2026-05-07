import clsx from 'clsx'
import { Copy } from 'lucide-react'
import { type KeyboardEvent, type MouseEvent, useState } from 'react'

import type { ScriptSummary } from '../../../lib/api/types'

type ScriptListItemProps = {
  script: ScriptSummary
  selected: boolean
  onClick: () => void
}

export function ScriptListItem({ script, selected, onClick }: ScriptListItemProps) {
  const [copySuccess, setCopySuccess] = useState(false)

  async function handleCopyClick(event: MouseEvent<HTMLButtonElement>) {
    event.stopPropagation()
    try {
      await navigator.clipboard.writeText(script.id)
      setCopySuccess(true)
      window.setTimeout(() => setCopySuccess(false), 1200)
    } catch {
      setCopySuccess(false)
    }
  }

  function handleKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault()
      onClick()
    }
  }

  return (
    <div
      role="button"
      tabIndex={0}
      onClick={onClick}
      onKeyDown={handleKeyDown}
      className={clsx(
        'w-full rounded-lg border px-2 py-2 text-sm transition',
        selected
          ? 'border-cyan-500 bg-cyan-500/10 text-cyan-100'
          : 'border-transparent bg-slate-900 text-slate-300 hover:border-slate-700 hover:bg-slate-800/70',
      )}
    >
      <div className="flex items-center justify-between gap-2">
        <span className="truncate text-left" title={script.name}>{script.name}</span>
        <button
          type="button"
          onClick={handleCopyClick}
          className="inline-flex h-6 w-6 items-center justify-center rounded border border-slate-700 text-slate-300 hover:border-cyan-500"
          aria-label={`Copy script id for ${script.name}`}
          title={copySuccess ? 'Copied' : 'Copy script id'}
        >
          <Copy size={12} />
        </button>
      </div>
      <p className="mt-1 truncate text-[11px] text-slate-500">{script.id}</p>
    </div>
  )
}
