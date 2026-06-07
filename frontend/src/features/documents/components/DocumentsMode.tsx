import clsx from 'clsx'
import { useQuery } from '@tanstack/react-query'
import { useEffect, useMemo, useState } from 'react'

import { DocumentJsonEditorPane } from './DocumentJsonEditorPane'
import { DocumentPreviewPane } from './DocumentPreviewPane'
import { DocumentsHeader } from './DocumentsHeader'
import { DocumentsSidebar } from './DocumentsSidebar'
import { PebbleTemplateEditor } from '../editor/PebbleTemplateEditor'
import { getRenderConfiguration, listRenderConfigurations } from '../../../lib/api/client'
import { useDocumentDraftState } from '../hooks/useDocumentDraftState'
import { useDocumentsApi } from '../hooks/useDocumentsApi'
import { parseBindings } from '../../workbench/lib/bindings'
import { formatError } from '../../workbench/lib/errors'

const SIDEBAR_STATE_STORAGE_KEY = 'peel-documents-sidebar-open'
const LOCAL_DOCUMENT_STATE_STORAGE_KEY = 'peel-documents-local-state-v2'
const DEFAULT_SCRIPT_NAME_TAGS_TEXT = '{\n  "calc": "SCRIPT_ID"\n}'
const DEFAULT_TEMPLATE =
  '<ul>\n{% for statement in calc.statements %}\n  <li>{{ statement | renderTraceExpression }}</li>\n{% endfor %}\n</ul>'
const DEFAULT_RENDER_CONFIGURATION_NAME = 'default'

type LocalDocumentState = {
  name: string
  scriptNameTagsText: string
  template: string
  templateHtml: string
  editorStateJson: string
  bindingsText: string
  renderConfigurationName: string
}

type LocalDocumentStateMap = Record<string, LocalDocumentState>

export function DocumentsMode() {
  const {
    name,
    setName,
    scriptNameTagsText,
    setScriptNameTagsText,
    template,
    setTemplate,
    templateHtml,
    setTemplateHtml,
    editorStateJson,
    setEditorStateJson,
    bindingsText,
    setBindingsText,
    renderConfigurationId,
    setRenderConfigurationId,
    selectedDocumentId,
    setSelectedDocumentId,
    resetToDefault,
  } = useDocumentDraftState()

  const bindingsState = parseBindings(bindingsText)
  const scriptNameTagsState = parseScriptNameTags(scriptNameTagsText)

  const [isSidebarOpen, setIsSidebarOpen] = useState<boolean>(() => {
    const raw = localStorage.getItem(SIDEBAR_STATE_STORAGE_KEY)
    if (raw === null) {
      return true
    }
    return raw === 'true'
  })
  const [isPreviewOpen, setIsPreviewOpen] = useState(false)
  const [previewPending, setPreviewPending] = useState(false)
  const [previewHtml, setPreviewHtml] = useState<string | null>(null)
  const [previewRenderError, setPreviewRenderError] = useState<string | null>(null)
  const [leftEditorTab, setLeftEditorTab] = useState<'scriptTags' | 'bindings'>('scriptTags')
  const [documentLoadNotice, setDocumentLoadNotice] = useState<string | null>(null)
  const [localDocumentStateMap, setLocalDocumentStateMap] = useState<LocalDocumentStateMap>(() =>
    loadLocalDocumentStateMap(),
  )

  const renderConfigurationsQuery = useQuery({
    queryKey: ['render-configs'],
    queryFn: listRenderConfigurations,
  })

  const selectedRenderConfigurationDetailQuery = useQuery({
    queryKey: ['render-config', renderConfigurationId],
    queryFn: () => getRenderConfiguration(renderConfigurationId),
    enabled: renderConfigurationId.trim().length > 0,
  })

  const renderConfigurationName = resolveRenderConfigurationName(
    renderConfigurationId,
    renderConfigurationsQuery.data ?? [],
  )

  const availableTemplateFilters = useMemo(() => {
    const detail = selectedRenderConfigurationDetailQuery.data
    const namedOverrides = detail?.renderConfigurationDto.namedOverrides ?? {}
    const overrideNames = Object.keys(namedOverrides).sort((left, right) => left.localeCompare(right))
    return ['renderTraceExpression', ...overrideNames]
  }, [selectedRenderConfigurationDetailQuery.data])

  useEffect(() => {
    localStorage.setItem(LOCAL_DOCUMENT_STATE_STORAGE_KEY, JSON.stringify(localDocumentStateMap))
  }, [localDocumentStateMap])

  function rememberLocalDocumentState(documentId: string, state: LocalDocumentState) {
    setLocalDocumentStateMap((current) => ({
      ...current,
      [documentId]: state,
    }))
  }

  const {
    documentsQuery,
    saveDocumentMutation,
    deleteDocumentMutation,
    previewDocumentMutation,
  } = useDocumentsApi({
    onDocumentSaved: (document, payload) => {
      setSelectedDocumentId(document.id)
      setName(payload.name)
      setScriptNameTagsText(JSON.stringify(payload.scriptNameTags, null, 2))
      setTemplate(payload.template)
      setTemplateHtml(payload.templateHtml)
      setEditorStateJson(payload.editorStateJson)
      setRenderConfigurationId(payload.renderConfigurationId)
      rememberLocalDocumentState(document.id, {
        name: payload.name,
        scriptNameTagsText: JSON.stringify(payload.scriptNameTags, null, 2),
        template: payload.template,
        templateHtml: payload.templateHtml,
        editorStateJson: payload.editorStateJson,
        bindingsText,
        renderConfigurationName: resolveRenderConfigurationName(
          payload.renderConfigurationId,
          renderConfigurationsQuery.data ?? [],
        ),
      })
      setDocumentLoadNotice(null)
    },
    onDocumentDeleted: (deletedId) => {
      setLocalDocumentStateMap((current) => {
        if (!(deletedId in current)) {
          return current
        }
        const next = { ...current }
        delete next[deletedId]
        return next
      })
      if (selectedDocumentId === deletedId) {
        resetToDefault()
        setDocumentLoadNotice(null)
      }
    },
  })

  const documents = documentsQuery.data ?? []

  const saveDisabled =
    saveDocumentMutation.isPending ||
    name.trim().length === 0 ||
    scriptNameTagsState.value === null ||
    template.trim().length === 0 ||
    bindingsState.value === null ||
    renderConfigurationId.trim().length === 0

  const previewDisabled =
    previewPending ||
    scriptNameTagsState.value === null ||
    template.trim().length === 0 ||
    bindingsState.value === null ||
    renderConfigurationId.trim().length === 0

  const deleteDisabled = deleteDocumentMutation.isPending || selectedDocumentId === null

  useEffect(() => {
    return () => {
      setPreviewHtml(null)
    }
  }, [])

  function toggleSidebar() {
    const nextState = !isSidebarOpen
    setIsSidebarOpen(nextState)
    localStorage.setItem(SIDEBAR_STATE_STORAGE_KEY, String(nextState))
  }

  function handleSave() {
    if (!bindingsState.value || !scriptNameTagsState.value) {
      return
    }

    saveDocumentMutation.mutate({
      id: selectedDocumentId ?? '',
      name: name.trim(),
      scriptNameTags: scriptNameTagsState.value,
      template,
      templateHtml,
      editorStateJson,
      renderConfigurationId: renderConfigurationId.trim(),
    })
  }

  async function handlePreview() {
    if (!bindingsState.value || !scriptNameTagsState.value) {
      return
    }

    setPreviewPending(true)
    setPreviewRenderError(null)
    setIsPreviewOpen(true)

    try {
      const previewScriptTags = Object.fromEntries(
        Object.entries(scriptNameTagsState.value).map(([nameTag, scriptId]) => [nameTag, { id: scriptId }]),
      )
      const previewResponse = await previewDocumentMutation.mutateAsync({
        scriptTags: previewScriptTags,
        bindings: bindingsState.value,
        renderConfigId: renderConfigurationId.trim(),
        template,
      })
      setPreviewHtml(previewResponse.html)
    } catch (error) {
      setPreviewHtml(null)
      setPreviewRenderError(formatError(error) ?? 'Could not render preview.')
    } finally {
      setPreviewPending(false)
    }
  }

  function handleCreateDocument() {
    const nextName = nextUntitledDocumentName(documents.map((documentSummary) => documentSummary.name))
    const nextScriptNameTagsText = DEFAULT_SCRIPT_NAME_TAGS_TEXT
    const nextTemplate = DEFAULT_TEMPLATE
    const nextTemplateHtml = '<p>Result: <span data-peel-inline="{{ calc.result | renderTraceExpression }}"></span></p>'
    const nextEditorStateJson = ''
    const nextBindings = {}
    const nextRenderConfigurationId = 'default'
    const nextRenderConfigurationName = DEFAULT_RENDER_CONFIGURATION_NAME

    setSelectedDocumentId(null)
    setName(nextName)
    setScriptNameTagsText(nextScriptNameTagsText)
    setTemplate(nextTemplate)
    setTemplateHtml(nextTemplateHtml)
    setEditorStateJson(nextEditorStateJson)
    setBindingsText(JSON.stringify(nextBindings, null, 2))
    setRenderConfigurationId(nextRenderConfigurationId)
    setDocumentLoadNotice(null)

    const nextScriptNameTags = parseScriptNameTags(nextScriptNameTagsText)
    if (!nextScriptNameTags.value) {
      return
    }

    saveDocumentMutation.mutate({
      id: '',
      name: nextName,
      scriptNameTags: nextScriptNameTags.value,
      template: nextTemplate,
      templateHtml: nextTemplateHtml,
      editorStateJson: nextEditorStateJson,
      renderConfigurationId: nextRenderConfigurationId,
    })

    if (selectedDocumentId) {
      rememberLocalDocumentState(selectedDocumentId, {
        name: nextName,
        scriptNameTagsText: nextScriptNameTagsText,
        template: nextTemplate,
        templateHtml: nextTemplateHtml,
        editorStateJson: nextEditorStateJson,
        bindingsText: JSON.stringify(nextBindings, null, 2),
        renderConfigurationName: nextRenderConfigurationName,
      })
    }
  }

  function handleSelectDocument(id: string) {
    setSelectedDocumentId(id)
    const localState = localDocumentStateMap[id]
    if (!localState) {
      const summary = documents.find((documentSummary) => documentSummary.id === id)
      setName(summary?.name ?? 'Untitled document')
      setScriptNameTagsText(DEFAULT_SCRIPT_NAME_TAGS_TEXT)
      setTemplate(DEFAULT_TEMPLATE)
      setTemplateHtml('<p>Result: <span data-peel-inline="{{ calc.result | renderTraceExpression }}"></span></p>')
      setEditorStateJson('')
      setBindingsText('{}')
      setRenderConfigurationId('default')
      setDocumentLoadNotice('No local editor data found for this document. Using local defaults.')
      return
    }

    setName(localState.name)
    setScriptNameTagsText(localState.scriptNameTagsText)
    setTemplate(localState.template)
    setTemplateHtml(localState.templateHtml)
    setEditorStateJson(localState.editorStateJson)
    setBindingsText(localState.bindingsText)
    const matched = (renderConfigurationsQuery.data ?? []).find(
      (config) => config.name === localState.renderConfigurationName,
    )
    setRenderConfigurationId(matched?.id ?? 'default')
    setDocumentLoadNotice(null)
  }

  function handleRenderConfigurationNameChange(nextName: string) {
    const matched = (renderConfigurationsQuery.data ?? []).find((config) => config.name === nextName)
    if (matched) {
      setRenderConfigurationId(matched.id)
      if (selectedDocumentId) {
        setLocalDocumentStateMap((current) => {
          const previous = current[selectedDocumentId]
          if (!previous) {
            return current
          }
          return {
            ...current,
            [selectedDocumentId]: {
              ...previous,
              renderConfigurationName: matched.name,
            },
          }
        })
      }
      return
    }
    setRenderConfigurationId(nextName)
  }

  function handleDeleteSelected() {
    if (!selectedDocumentId) {
      return
    }
    deleteDocumentMutation.mutate(selectedDocumentId)
  }

  const saveError = formatError(saveDocumentMutation.error)
  const deleteError = formatError(deleteDocumentMutation.error)
  const previewError = previewRenderError ?? formatError(previewDocumentMutation.error)
  const renderConfigError = formatError(selectedRenderConfigurationDetailQuery.error)

  return (
    <div
      className={clsx(
        'grid flex-1 gap-4 overflow-hidden',
        isSidebarOpen ? 'lg:grid-cols-[300px_1fr]' : 'lg:grid-cols-[56px_1fr]',
      )}
    >
      <DocumentsSidebar
        documentsQuery={documentsQuery}
        documents={documents}
        selectedDocumentId={selectedDocumentId}
        isOpen={isSidebarOpen}
        onToggle={toggleSidebar}
        onSelectDocument={handleSelectDocument}
        onCreateDocument={handleCreateDocument}
        onDeleteSelected={handleDeleteSelected}
        deleteDisabled={deleteDisabled}
      />

      <section className="grid min-h-0 gap-4 overflow-hidden">
        <DocumentsHeader
          name={name}
          onNameChange={setName}
          renderConfigurationName={renderConfigurationName}
          onRenderConfigurationNameChange={handleRenderConfigurationNameChange}
          renderConfigurationOptions={renderConfigurationsQuery.data ?? []}
          renderConfigurationsLoading={renderConfigurationsQuery.isLoading}
          onSave={handleSave}
          saveDisabled={saveDisabled}
          savePending={saveDocumentMutation.isPending}
          onPreview={handlePreview}
          previewDisabled={previewDisabled}
          previewPending={previewPending}
        />

        {saveError ? (
          <p className="rounded border border-rose-800/70 bg-rose-950/40 px-3 py-2 text-sm text-rose-200">
            {saveError}
          </p>
        ) : null}
        {deleteError ? (
          <p className="rounded border border-rose-800/70 bg-rose-950/40 px-3 py-2 text-sm text-rose-200">
            {deleteError}
          </p>
        ) : null}
        {documentLoadNotice ? (
          <p className="rounded border border-amber-700/70 bg-amber-950/40 px-3 py-2 text-sm text-amber-100">
            {documentLoadNotice}
          </p>
        ) : null}
        {scriptNameTagsState.error ? (
          <p className="rounded border border-rose-800/70 bg-rose-950/40 px-3 py-2 text-sm text-rose-200">
            {scriptNameTagsState.error}
          </p>
        ) : null}
        {renderConfigError ? (
          <p className="rounded border border-rose-800/70 bg-rose-950/40 px-3 py-2 text-sm text-rose-200">
            {renderConfigError}
          </p>
        ) : null}

        <div className="rounded border border-slate-800 bg-slate-900/60 px-3 py-2 text-xs text-slate-300">
          <p className="mb-2 font-semibold text-slate-200">Available template filters for selected render configuration:</p>
          <div className="flex flex-wrap gap-2">
            {availableTemplateFilters.map((filterName) => (
              <span key={filterName} className="rounded border border-slate-700 bg-slate-950 px-2 py-1 text-[11px]">
                {`{{ expression | ${filterName} }}`}
              </span>
            ))}
          </div>
        </div>

        <div className="grid min-h-0 gap-4 overflow-visible lg:grid-cols-[360px_1fr]">
          <div className="grid min-h-0 gap-2">
            <div className="inline-flex w-fit items-center gap-1 rounded border border-slate-700 bg-slate-950/70 p-1">
              <button
                type="button"
                onClick={() => setLeftEditorTab('scriptTags')}
                className={clsx(
                  'rounded px-2.5 py-1 text-xs font-medium transition',
                  leftEditorTab === 'scriptTags'
                    ? 'bg-cyan-500/20 text-cyan-100 border border-cyan-500/60'
                    : 'text-slate-300 border border-transparent hover:border-slate-700 hover:bg-slate-800/60',
                )}
              >
                Script Name Tags
              </button>
              <button
                type="button"
                onClick={() => setLeftEditorTab('bindings')}
                className={clsx(
                  'rounded px-2.5 py-1 text-xs font-medium transition',
                  leftEditorTab === 'bindings'
                    ? 'bg-cyan-500/20 text-cyan-100 border border-cyan-500/60'
                    : 'text-slate-300 border border-transparent hover:border-slate-700 hover:bg-slate-800/60',
                )}
              >
                Bindings
              </button>
            </div>

            {leftEditorTab === 'scriptTags' ? (
              <DocumentJsonEditorPane
                title="Script Name Tags (JSON)"
                language="json"
                value={scriptNameTagsText}
                onValueChange={setScriptNameTagsText}
              />
            ) : (
              <DocumentJsonEditorPane
                title="Bindings (JSON)"
                language="json"
                value={bindingsText}
                onValueChange={setBindingsText}
              />
            )}
          </div>

          <PebbleTemplateEditor
            initialHtml={templateHtml}
            initialEditorStateJson={editorStateJson}
            onTemplateChange={(next) => {
              setTemplate(next.templatePebble)
              setTemplateHtml(next.templateHtml)
              setEditorStateJson(next.editorStateJson)
            }}
          />
        </div>

        <DocumentPreviewPane
          isOpen={isPreviewOpen}
          onClose={() => {
            setIsPreviewOpen(false)
          }}
          previewPending={previewPending}
          html={previewHtml}
          previewError={previewError}
          parseError={bindingsState.error}
        />
      </section>
    </div>
  )
}

function resolveRenderConfigurationName(
  renderConfigurationId: string,
  options: { id: string; name: string }[],
): string {
  const matched = options.find((option) => option.id === renderConfigurationId)
  return matched?.name ?? renderConfigurationId
}

function loadLocalDocumentStateMap(): LocalDocumentStateMap {
  const raw = localStorage.getItem(LOCAL_DOCUMENT_STATE_STORAGE_KEY)
  if (!raw) {
    return {}
  }

  try {
    const parsed = JSON.parse(raw) as unknown
    if (parsed === null || Array.isArray(parsed) || typeof parsed !== 'object') {
      return {}
    }
    return parsed as LocalDocumentStateMap
  } catch {
    localStorage.removeItem(LOCAL_DOCUMENT_STATE_STORAGE_KEY)
    return {}
  }
}

function parseScriptNameTags(value: string): { value: Record<string, string> | null; error: string | null } {
  try {
    const parsed = JSON.parse(value) as unknown
    if (parsed === null || Array.isArray(parsed) || typeof parsed !== 'object') {
      throw new Error('Script name tags must be a JSON object where each value is a script id string.')
    }

    const next: Record<string, string> = {}
    for (const [nameTag, scriptId] of Object.entries(parsed)) {
      if (typeof scriptId !== 'string' || scriptId.trim().length === 0) {
        throw new Error('Every script name tag must map to a non-empty script id string.')
      }
      next[nameTag] = scriptId.trim()
    }

    return { value: next, error: null }
  } catch (error) {
    return {
      value: null,
      error: error instanceof Error ? error.message : 'Script name tags must be valid JSON.',
    }
  }
}

function nextUntitledDocumentName(existingNames: string[]): string {
  const normalizedNames = new Set(existingNames.map((existingName) => existingName.trim().toLowerCase()))
  if (!normalizedNames.has('untitled document')) {
    return 'Untitled document'
  }

  let index = 2
  while (normalizedNames.has(`untitled document ${index}`)) {
    index += 1
  }
  return `Untitled document ${index}`
}
