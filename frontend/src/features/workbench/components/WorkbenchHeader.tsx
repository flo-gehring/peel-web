import clsx from 'clsx'

type WorkbenchHeaderProps = {
  onNew: () => void
  onDuplicate: () => void
  onSave: () => void
  onRun: () => void
  saveDisabled: boolean
  runDisabled: boolean
  savePending: boolean
  runPending: boolean
}

export function WorkbenchHeader({
  onNew,
  onDuplicate,
  onSave,
  onRun,
  saveDisabled,
  runDisabled,
  savePending,
  runPending,
}: WorkbenchHeaderProps) {
  return (
    <header className="mb-4 flex flex-wrap items-center justify-between gap-3 border border-slate-800/80 bg-slate-900/80 px-4 py-3">
      <div>
        <p className="text-xs uppercase tracking-[0.24em] text-cyan-300/80">Peel Workbench</p>
        <h1 className="font-serif text-2xl text-slate-50">Script Runner</h1>
      </div>

      <div className="flex items-center gap-2">
        <button
          type="button"
          className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm hover:border-cyan-500"
          onClick={onNew}
        >
          New
        </button>
        <button
          type="button"
          className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm hover:border-cyan-500"
          onClick={onDuplicate}
        >
          Duplicate
        </button>
        <button
          type="button"
          className={clsx(
            'rounded-lg border px-3 py-2 text-sm font-medium transition',
            saveDisabled
              ? 'cursor-not-allowed border-slate-800 bg-slate-900 text-slate-500'
              : 'border-emerald-500/70 bg-emerald-500/10 text-emerald-200 hover:bg-emerald-500/20',
          )}
          onClick={onSave}
          disabled={saveDisabled}
        >
          {savePending ? 'Saving...' : 'Save'}
        </button>
        <button
          type="button"
          className={clsx(
            'rounded-lg border px-4 py-2 text-sm font-semibold transition',
            runDisabled
              ? 'cursor-not-allowed border-slate-800 bg-slate-900 text-slate-500'
              : 'border-cyan-400 bg-cyan-400/10 text-cyan-100 hover:bg-cyan-400/20',
          )}
          onClick={onRun}
          disabled={runDisabled}
        >
          {runPending ? 'Running...' : 'Run (Editor Ctrl/Cmd+Enter)'}
        </button>
      </div>
    </header>
  )
}
