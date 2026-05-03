import type { UseQueryResult } from '@tanstack/react-query'
import { ChevronLeft, ChevronRight, FilePlus2, Trash2 } from 'lucide-react'

import type { DocumentSummary } from '../../../lib/api/types'
import { formatError } from '../../workbench/lib/errors'
import { DocumentListItem } from './DocumentListItem'

type DocumentsSidebarProps = {
  documentsQuery: UseQueryResult<DocumentSummary[], Error>
  documents: DocumentSummary[]
  selectedDocumentId: string | null
  isOpen: boolean
  onToggle: () => void
  onSelectDocument: (id: string) => void
  onCreateDocument: () => void
  onDeleteSelected: () => void
  deleteDisabled: boolean
}

export function DocumentsSidebar({
  documentsQuery,
  documents,
  selectedDocumentId,
  isOpen,
  onToggle,
  onSelectDocument,
  onCreateDocument,
  onDeleteSelected,
  deleteDisabled,
}: DocumentsSidebarProps) {
  if (!isOpen) {
    return (
      <aside className="flex min-h-0 flex-col items-center gap-2 border border-slate-800 bg-slate-900/70 p-2">
        <button
          type="button"
          className="flex h-9 w-9 items-center justify-center rounded-md border border-slate-700 bg-slate-900 text-slate-200 hover:border-teal-500"
          onClick={onToggle}
          aria-label="Expand documents sidebar"
        >
          <ChevronRight size={16} />
        </button>
      </aside>
    )
  }

  return (
    <aside className="flex min-h-0 flex-col border border-slate-800 bg-slate-900/70 p-3">
      <div className="mb-2 flex items-center justify-between gap-2">
        <h2 className="text-sm font-semibold tracking-wide text-slate-300">Documents</h2>
        <div className="flex items-center gap-2">
          <button
            type="button"
            className="flex h-8 w-8 items-center justify-center rounded-md border border-slate-700 bg-slate-900 text-slate-300 hover:border-teal-500"
            onClick={onCreateDocument}
            aria-label="Create new document"
          >
            <FilePlus2 size={16} />
          </button>
          <button
            type="button"
            className="flex h-8 w-8 items-center justify-center rounded-md border border-slate-700 bg-slate-900 text-slate-300 hover:border-rose-500 disabled:cursor-not-allowed disabled:text-slate-600"
            onClick={onDeleteSelected}
            disabled={deleteDisabled}
            aria-label="Delete selected document"
          >
            <Trash2 size={16} />
          </button>
          <button
            type="button"
            className="flex h-8 w-8 items-center justify-center rounded-md border border-slate-700 bg-slate-900 text-slate-300 hover:border-teal-500"
            onClick={onToggle}
            aria-label="Collapse documents sidebar"
          >
            <ChevronLeft size={16} />
          </button>
        </div>
      </div>

      {documentsQuery.isLoading && <p className="text-sm text-slate-400">Loading documents...</p>}
      {documentsQuery.isError && (
        <p className="text-sm text-rose-300">
          {formatError(documentsQuery.error) ?? 'Failed to load documents'}
        </p>
      )}

      <div className="space-y-1 overflow-y-auto pr-1">
        {documents.map((documentSummary) => (
          <DocumentListItem
            key={documentSummary.id}
            document={documentSummary}
            selected={selectedDocumentId === documentSummary.id}
            onClick={() => onSelectDocument(documentSummary.id)}
          />
        ))}
        {documents.length === 0 && !documentsQuery.isLoading ? (
          <p className="text-sm text-slate-500">No saved documents yet.</p>
        ) : null}
      </div>
    </aside>
  )
}
