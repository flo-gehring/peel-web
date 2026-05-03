import { X } from 'lucide-react'

type DocumentPreviewPaneProps = {
  isOpen: boolean
  onClose: () => void
  previewPending: boolean
  pdfUrl: string | null
  previewError: string | null
  parseError: string | null
}

export function DocumentPreviewPane({
  isOpen,
  onClose,
  previewPending,
  pdfUrl,
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

          {pdfUrl ? (
            <iframe
              src={pdfUrl}
              title="PDF Preview"
              className="h-full min-h-[70vh] w-full rounded border border-slate-700 bg-white"
            />
          ) : (
            <p className="text-slate-400">Run preview to inspect the rendered PDF.</p>
          )}
        </div>
      </div>
    </div>
  )
}
