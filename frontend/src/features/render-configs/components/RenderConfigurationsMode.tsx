import clsx from 'clsx'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ChevronLeft, ChevronRight, FilePlus2, LoaderCircle, Save } from 'lucide-react'
import { useMemo, useState } from 'react'

import {
  createRenderConfiguration,
  getDefaultRenderConfiguration,
  getRenderConfiguration,
  listRenderConfigurations,
  updateRenderConfiguration,
} from '../../../lib/api/client'
import type { RenderConfigurationDetail, TraceExpressionKind } from '../../../lib/api/types'
import { formatError } from '../../workbench/lib/errors'
import { TRACE_EXPRESSION_KINDS } from '../constants'

const SIDEBAR_STATE_STORAGE_KEY = 'peel-render-configs-sidebar-open'

export function RenderConfigurationsMode() {
  const queryClient = useQueryClient()
  const [selectedRenderConfigId, setSelectedRenderConfigId] = useState<string | null>(null)
  const [name, setName] = useState('Untitled render config')
  const [templatesByKind, setTemplatesByKind] = useState<Partial<Record<TraceExpressionKind, string>>>({})
  const [isSidebarOpen, setIsSidebarOpen] = useState<boolean>(() => {
    const raw = localStorage.getItem(SIDEBAR_STATE_STORAGE_KEY)
    if (raw === null) {
      return true
    }
    return raw === 'true'
  })

  const renderConfigsQuery = useQuery({
    queryKey: ['render-configs'],
    queryFn: listRenderConfigurations,
  })

  const loadRenderConfigMutation = useMutation({
    mutationFn: getRenderConfiguration,
    onSuccess: (loadedConfig, id) => {
      setSelectedRenderConfigId(id)
      setName(loadedConfig.name)
      setTemplatesByKind(loadedConfig.renderConfigurationDto.renderConfigurations)
    },
  })

  const getDefaultMutation = useMutation({
    mutationFn: getDefaultRenderConfiguration,
    onSuccess: (defaultConfig) => {
      setSelectedRenderConfigId(null)
      setName('Untitled render config')
      setTemplatesByKind(defaultConfig.renderConfigurations)
    },
  })

  const createRenderConfigMutation = useMutation({
    mutationFn: (payload: RenderConfigurationDetail) => createRenderConfiguration(payload),
    onSuccess: (created) => {
      setSelectedRenderConfigId(created.id)
      queryClient.invalidateQueries({ queryKey: ['render-configs'] })
    },
  })

  const updateRenderConfigMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: RenderConfigurationDetail }) =>
      updateRenderConfiguration(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['render-configs'] })
    },
  })

  const savePending = createRenderConfigMutation.isPending || updateRenderConfigMutation.isPending
  const saveDisabled = savePending || name.trim().length === 0

  const saveError = formatError(createRenderConfigMutation.error) ?? formatError(updateRenderConfigMutation.error)
  const loadError = formatError(loadRenderConfigMutation.error)
  const defaultError = formatError(getDefaultMutation.error)

  const sortedKinds = useMemo(() => TRACE_EXPRESSION_KINDS, [])

  function toggleSidebar() {
    const nextState = !isSidebarOpen
    setIsSidebarOpen(nextState)
    localStorage.setItem(SIDEBAR_STATE_STORAGE_KEY, String(nextState))
  }

  function handleNewFromDefault() {
    getDefaultMutation.mutate()
  }

  function handleSave() {
    const payload: RenderConfigurationDetail = {
      name: name.trim(),
      renderConfigurationDto: {
        renderConfigurations: templatesByKind,
      },
    }

    if (!selectedRenderConfigId) {
      createRenderConfigMutation.mutate(payload)
      return
    }

    updateRenderConfigMutation.mutate({ id: selectedRenderConfigId, payload })
  }

  return (
    <div
      className={clsx(
        'grid flex-1 gap-4 overflow-hidden',
        isSidebarOpen ? 'lg:grid-cols-[300px_1fr]' : 'lg:grid-cols-[56px_1fr]',
      )}
    >
      {!isSidebarOpen ? (
        <aside className="flex min-h-0 flex-col items-center gap-2 border border-slate-800 bg-slate-900/70 p-2">
          <button
            type="button"
            className="flex h-9 w-9 items-center justify-center rounded-md border border-slate-700 bg-slate-900 text-slate-200 hover:border-teal-500"
            onClick={toggleSidebar}
            aria-label="Expand render config sidebar"
          >
            <ChevronRight size={16} />
          </button>
        </aside>
      ) : (
        <aside className="flex min-h-0 flex-col border border-slate-800 bg-slate-900/70 p-3">
          <div className="mb-2 flex items-center justify-between gap-2">
            <h2 className="text-sm font-semibold tracking-wide text-slate-300">Render Configs</h2>
            <div className="flex items-center gap-2">
              <button
                type="button"
                className="flex h-8 w-8 items-center justify-center rounded-md border border-slate-700 bg-slate-900 text-slate-300 hover:border-teal-500"
                onClick={handleNewFromDefault}
                aria-label="Create new from default"
                disabled={getDefaultMutation.isPending}
              >
                {getDefaultMutation.isPending ? <LoaderCircle size={16} className="animate-spin" /> : <FilePlus2 size={16} />}
              </button>
              <button
                type="button"
                className="flex h-8 w-8 items-center justify-center rounded-md border border-slate-700 bg-slate-900 text-slate-300 hover:border-teal-500"
                onClick={toggleSidebar}
                aria-label="Collapse render config sidebar"
              >
                <ChevronLeft size={16} />
              </button>
            </div>
          </div>

          {renderConfigsQuery.isLoading ? <p className="text-sm text-slate-400">Loading render configs...</p> : null}
          {renderConfigsQuery.isError ? (
            <p className="text-sm text-rose-300">{formatError(renderConfigsQuery.error) ?? 'Failed to load render configs.'}</p>
          ) : null}

          <div className="space-y-1 overflow-y-auto pr-1">
            {(renderConfigsQuery.data ?? []).map((summary) => (
              <button
                key={summary.id}
                type="button"
                className={clsx(
                  'w-full rounded-lg border px-2 py-2 text-left text-sm transition',
                  selectedRenderConfigId === summary.id
                    ? 'border-teal-500 bg-teal-500/10 text-teal-100'
                    : 'border-transparent bg-slate-900 text-slate-300 hover:border-slate-700 hover:bg-slate-800/70',
                )}
                onClick={() => loadRenderConfigMutation.mutate(summary.id)}
              >
                <p className="truncate">{summary.name}</p>
                <p className="mt-1 text-xs text-slate-500">{summary.id}</p>
              </button>
            ))}
          </div>
        </aside>
      )}

      <section className="flex min-h-0 flex-col gap-4 overflow-hidden">
        <div className="flex flex-wrap items-center gap-3 border border-slate-800 bg-slate-900/70 px-3 py-2">
          <input
            value={name}
            onChange={(event) => setName(event.target.value)}
            className="min-w-56 grow cursor-text rounded border border-slate-700 bg-slate-950 px-3 py-2 text-sm font-semibold text-slate-100 outline-none placeholder:text-slate-500 hover:border-slate-500 focus:border-teal-500"
            placeholder="Render configuration name"
            title="Editable render configuration title"
          />
          <p className="text-xs text-slate-400">Edit templates per TraceExpressionKind and save.</p>
          <button
            type="button"
            className={clsx(
              'inline-flex items-center gap-2 rounded-lg border px-3 py-2 text-sm font-medium transition',
              saveDisabled
                ? 'cursor-not-allowed border-slate-800 bg-slate-900 text-slate-500'
                : 'border-emerald-500/70 bg-emerald-500/10 text-emerald-200 hover:bg-emerald-500/20',
            )}
            onClick={handleSave}
            disabled={saveDisabled}
          >
            {savePending ? <LoaderCircle size={14} className="animate-spin" /> : <Save size={14} />}
            Save
          </button>
        </div>

        {loadError ? (
          <p className="rounded border border-rose-800/70 bg-rose-950/40 px-3 py-2 text-sm text-rose-200">{loadError}</p>
        ) : null}
        {defaultError ? (
          <p className="rounded border border-rose-800/70 bg-rose-950/40 px-3 py-2 text-sm text-rose-200">{defaultError}</p>
        ) : null}
        {saveError ? (
          <p className="rounded border border-rose-800/70 bg-rose-950/40 px-3 py-2 text-sm text-rose-200">{saveError}</p>
        ) : null}

        <div className="min-h-0 overflow-auto border border-slate-800 bg-slate-900/60 p-3">
          <div className="space-y-2">
            {sortedKinds.map((kind) => {
              const currentValue = templatesByKind[kind] ?? ''
              return (
                <details key={kind} className="rounded border border-slate-800 bg-slate-950/40" open={currentValue.length > 0}>
                  <summary className="cursor-pointer select-none px-3 py-2 text-sm font-medium text-slate-200">
                    {kind}
                    <span className="ml-2 text-xs text-slate-500">{currentValue.trim().length > 0 ? 'configured' : 'empty'}</span>
                  </summary>
                  <div className="border-t border-slate-800 p-3">
                    <textarea
                      value={currentValue}
                      onChange={(event) => {
                        const nextValue = event.target.value
                        setTemplatesByKind((current) => ({
                          ...current,
                          [kind]: nextValue,
                        }))
                      }}
                      className="h-28 w-full resize-y rounded border border-slate-700 bg-slate-950 px-3 py-2 text-xs text-slate-100 outline-none focus:border-teal-500"
                      spellCheck={false}
                    />
                  </div>
                </details>
              )
            })}
          </div>
        </div>
      </section>
    </div>
  )
}
