import { X } from 'lucide-react'

import type { DocumentPreviewResponse } from '../../../lib/api/types'

type DocumentPreviewPaneProps = {
  isOpen: boolean
  onClose: () => void
  previewPending: boolean
  previewData: DocumentPreviewResponse | null
  previewError: string | null
  parseError: string | null
}

export function DocumentPreviewPane({
  isOpen,
  onClose,
  previewPending,
  previewData,
  previewError,
  parseError,
}: DocumentPreviewPaneProps) {
  if (!isOpen) {
    return null
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/80 p-4">
      <div className="flex h-[85vh] w-full max-w-6xl min-w-0 flex-col border border-slate-700 bg-slate-900 shadow-2xl">
        <div className="flex items-center justify-between border-b border-slate-800 px-4 py-3">
          <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-300">Preview</p>
          <button
            type="button"
            className="inline-flex h-8 w-8 items-center justify-center rounded-md border border-slate-700 text-slate-300 hover:border-cyan-500"
            onClick={onClose}
            aria-label="Close preview"
          >
            <X size={16} />
          </button>
        </div>

        <div className="min-h-0 flex-1 overflow-auto p-4 text-xs">
          {parseError ? (
            <p className="mb-3 rounded border border-rose-800/70 bg-rose-950/40 px-3 py-2 text-rose-200">
              {parseError}
            </p>
          ) : null}
          {previewError ? (
            <p className="mb-3 rounded border border-rose-800/70 bg-rose-950/40 px-3 py-2 text-rose-200">
              {previewError}
            </p>
          ) : null}

          {previewPending ? <p className="mb-3 text-slate-400">Rendering preview...</p> : null}

          {previewData ? (
            <>
              <p className="mb-2 text-[11px] uppercase tracking-[0.15em] text-slate-400">Rendered content</p>
              <pre className="mb-3 overflow-auto rounded-lg border border-slate-800 bg-slate-950/60 p-2 text-slate-200">
                {JSON.stringify(previewData.renderedContent, null, 2)}
              </pre>

              <p className="mb-2 text-[11px] uppercase tracking-[0.15em] text-slate-400">References</p>
              <pre className="mb-3 overflow-auto rounded-lg border border-slate-800 bg-slate-950/60 p-2 text-slate-200">
                {JSON.stringify(previewData.references, null, 2)}
              </pre>

              <p className="mb-2 text-[11px] uppercase tracking-[0.15em] text-slate-400">Diagnostics</p>
              <pre className="overflow-auto rounded-lg border border-slate-800 bg-slate-950/60 p-2 text-slate-200">
                {JSON.stringify(previewData.diagnostics, null, 2)}
              </pre>
            </>
          ) : (
            <p className="text-slate-400">Run preview to inspect rendered document output.</p>
          )}
        </div>
      </div>
    </div>
  )
}
