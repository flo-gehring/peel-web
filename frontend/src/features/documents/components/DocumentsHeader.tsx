import clsx from 'clsx'
import { LoaderCircle, Save, Sparkles } from 'lucide-react'

type DocumentsHeaderProps = {
  name: string
  onNameChange: (value: string) => void
  onSave: () => void
  saveDisabled: boolean
  savePending: boolean
  onPreview: () => void
  previewDisabled: boolean
  previewPending: boolean
}

export function DocumentsHeader({
  name,
  onNameChange,
  onSave,
  saveDisabled,
  savePending,
  onPreview,
  previewDisabled,
  previewPending,
}: DocumentsHeaderProps) {
  return (
    <div className="flex flex-wrap items-center gap-3 border border-slate-800 bg-slate-900/70 px-3 py-2">
      <input
        value={name}
        onChange={(event) => onNameChange(event.target.value)}
        className="min-w-56 grow bg-transparent text-sm font-semibold text-slate-100 outline-none placeholder:text-slate-500"
        placeholder="Document name"
      />
      <div className="flex items-center gap-2">
        <button
          type="button"
          className={clsx(
            'inline-flex items-center gap-2 rounded-lg border px-3 py-2 text-sm font-medium transition',
            saveDisabled
              ? 'cursor-not-allowed border-slate-800 bg-slate-900 text-slate-500'
              : 'border-emerald-500/70 bg-emerald-500/10 text-emerald-200 hover:bg-emerald-500/20',
          )}
          onClick={onSave}
          disabled={saveDisabled}
        >
          {savePending ? <LoaderCircle size={14} className="animate-spin" /> : <Save size={14} />}
          Save
        </button>

        <button
          type="button"
          className={clsx(
            'inline-flex items-center gap-2 rounded-lg border px-3 py-2 text-sm font-semibold transition',
            previewDisabled
              ? 'cursor-not-allowed border-slate-800 bg-slate-900 text-slate-500'
              : 'border-teal-400 bg-teal-400/10 text-teal-100 hover:bg-teal-400/20',
          )}
          onClick={onPreview}
          disabled={previewDisabled}
        >
          {previewPending ? <LoaderCircle size={14} className="animate-spin" /> : <Sparkles size={14} />}
          Preview
        </button>
      </div>
    </div>
  )
}
