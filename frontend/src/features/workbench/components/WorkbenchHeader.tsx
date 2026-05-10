import clsx from 'clsx'
import { Banana } from 'lucide-react'

export type AppMode = 'workbench' | 'documents' | 'render-configs' | 'batch'

type WorkbenchHeaderProps = {
  mode: AppMode
  onModeChange: (mode: AppMode) => void
}

export function WorkbenchHeader({ mode, onModeChange }: WorkbenchHeaderProps) {
  return (
    <header className="mb-4 flex flex-wrap items-center justify-between gap-3 border border-slate-800/80 bg-slate-900/80 px-4 py-3">
      <div>
        <div className="flex flex-wrap items-center gap-2">
          <Banana color="yellow" />
          <span className="font-semibold tracking-wide">PEEL</span>
        </div>
        <p className="text-xs uppercase tracking-[0.24em] text-cyan-300/80">Script + Documents Platform</p>
      </div>

      <nav className="flex items-center gap-2 rounded-lg border border-slate-700 bg-slate-950/40 p-1">
        <ModeButton
          active={mode === 'workbench'}
          label="Workbench"
          onClick={() => onModeChange('workbench')}
        />
        <ModeButton
          active={mode === 'documents'}
          label="Documents"
          onClick={() => onModeChange('documents')}
        />
        <ModeButton
          active={mode === 'render-configs'}
          label="Render Configs"
          onClick={() => onModeChange('render-configs')}
        />
        <ModeButton
          active={mode === 'batch'}
          label="Batch"
          onClick={() => onModeChange('batch')}
        />
      </nav>
    </header>
  )
}

function ModeButton({
  active,
  label,
  onClick,
}: {
  active: boolean
  label: string
  onClick: () => void
}) {
  return (
    <button
      type="button"
      className={clsx(
        'rounded-md px-3 py-1.5 text-sm transition',
        active
          ? 'bg-cyan-500/20 text-cyan-100 border border-cyan-500/60'
          : 'text-slate-300 border border-transparent hover:border-slate-700 hover:bg-slate-800/60',
      )}
      onClick={onClick}
    >
      {label}
    </button>
  )
}
