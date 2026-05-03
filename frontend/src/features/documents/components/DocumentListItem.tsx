import clsx from 'clsx'

import type { DocumentSummary } from '../../../lib/api/types'

type DocumentListItemProps = {
  document: DocumentSummary
  selected: boolean
  onClick: () => void
}

export function DocumentListItem({ document, selected, onClick }: DocumentListItemProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={clsx(
        'w-full rounded-lg border px-2 py-2 text-left text-sm transition',
        selected
          ? 'border-teal-500 bg-teal-500/10 text-teal-100'
          : 'border-transparent bg-slate-900 text-slate-300 hover:border-slate-700 hover:bg-slate-800/70',
      )}
    >
      <p className="truncate">{document.name}</p>
      <p className="mt-1 text-xs text-slate-500">Updated {new Date(document.updatedAt).toLocaleString()}</p>
    </button>
  )
}
