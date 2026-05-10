import clsx from 'clsx'
import { LoaderCircle, Save, Sparkles } from 'lucide-react'
import { useMemo, useState } from 'react'

import type { RenderConfigurationSummary } from '../../../lib/api/types'

type DocumentsHeaderProps = {
  name: string
  onNameChange: (value: string) => void
  renderConfigurationName: string
  onRenderConfigurationNameChange: (value: string) => void
  renderConfigurationOptions: RenderConfigurationSummary[]
  renderConfigurationsLoading: boolean
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
  renderConfigurationName,
  onRenderConfigurationNameChange,
  renderConfigurationOptions,
  renderConfigurationsLoading,
  onSave,
  saveDisabled,
  savePending,
  onPreview,
  previewDisabled,
  previewPending,
}: DocumentsHeaderProps) {
  const [isMenuOpen, setIsMenuOpen] = useState(false)

  const filteredOptions = useMemo(() => {
    const query = renderConfigurationName.trim().toLowerCase()
    if (query.length === 0) {
      return renderConfigurationOptions.slice(0, 20)
    }
    return renderConfigurationOptions
      .filter((option) => {
        const nameMatches = option.name.toLowerCase().includes(query)
        const idMatches = option.id.toLowerCase().includes(query)
        return nameMatches || idMatches
      })
      .slice(0, 20)
  }, [renderConfigurationName, renderConfigurationOptions])

  return (
    <div className="flex flex-wrap items-center gap-3 border border-slate-800 bg-slate-900/70 px-3 py-2">
      <input
        value={name}
        onChange={(event) => onNameChange(event.target.value)}
        className="min-w-56 cursor-text rounded border border-slate-700 bg-slate-950 px-3 py-2 text-sm font-semibold text-slate-100 outline-none placeholder:text-slate-500 hover:border-slate-500 focus:border-teal-500 lg:w-72"
        placeholder="Document name"
        title="Editable document title"
      />
      <div className="relative min-w-56 grow">
        <input
          value={renderConfigurationName}
          onChange={(event) => {
            onRenderConfigurationNameChange(event.target.value)
            setIsMenuOpen(true)
          }}
          onFocus={() => setIsMenuOpen(true)}
          onBlur={() => window.setTimeout(() => setIsMenuOpen(false), 100)}
          className="w-full cursor-text rounded border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100 outline-none placeholder:text-slate-500 hover:border-slate-500 focus:border-teal-500"
          placeholder={renderConfigurationsLoading ? 'Loading render configs...' : 'Search render config by name or id'}
          title="Editable render configuration selector"
        />
        {isMenuOpen && !renderConfigurationsLoading ? (
          <div className="absolute left-0 right-0 top-[calc(100%+4px)] z-20 max-h-64 overflow-auto rounded border border-slate-700 bg-slate-950 shadow-xl">
            {filteredOptions.length > 0 ? (
              filteredOptions.map((option) => (
                <button
                  key={option.id}
                  type="button"
                  className="flex w-full items-start justify-between gap-3 border-b border-slate-800 px-3 py-2 text-left text-sm last:border-b-0 hover:bg-slate-800"
                  onMouseDown={(event) => {
                    event.preventDefault()
                    onRenderConfigurationNameChange(option.name)
                    setIsMenuOpen(false)
                  }}
                >
                  <span className="text-slate-100">{option.name}</span>
                  <span className="text-xs text-slate-500">{option.id}</span>
                </button>
              ))
            ) : (
              <p className="px-3 py-2 text-sm text-slate-500">No matching render configurations.</p>
            )}
          </div>
        ) : null}
      </div>
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
