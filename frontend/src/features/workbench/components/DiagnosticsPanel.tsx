import type { ValidationDiagnostic } from '../../../lib/api/types'

type DiagnosticsPanelProps = {
  diagnostics: ValidationDiagnostic[]
}

export function DiagnosticsPanel({ diagnostics }: DiagnosticsPanelProps) {
  if (diagnostics.length === 0) {
    return (
      <div className="border-t border-slate-800 bg-slate-950/40 px-3 py-2 text-xs text-emerald-300">
        Validation passed.
      </div>
    )
  }

  return (
    <div className="max-h-28 overflow-auto border-t border-rose-900/70 bg-rose-950/30 px-3 py-2 text-xs text-rose-100">
      {diagnostics.map((diagnostic, index) => (
        <div key={`${diagnostic.line}-${diagnostic.column}-${index}`}>
          Ln {diagnostic.line}, Col {diagnostic.column}: {diagnostic.message}
        </div>
      ))}
    </div>
  )
}
