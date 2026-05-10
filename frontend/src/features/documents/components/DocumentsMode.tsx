import clsx from 'clsx'
import { useEffect, useState } from 'react'

import { DocumentJsonEditorPane } from './DocumentJsonEditorPane'
import { DocumentPreviewPane } from './DocumentPreviewPane'
import { DocumentsHeader } from './DocumentsHeader'
import { DocumentsSidebar } from './DocumentsSidebar'
import { useDocumentDraftState } from '../hooks/useDocumentDraftState'
import { useDocumentsApi } from '../hooks/useDocumentsApi'
import { listRenderConfigurations } from '../../../lib/api/client'
import { parseBindings } from '../../workbench/lib/bindings'
import { formatError } from '../../workbench/lib/errors'
import type { RenderConfigurationDto } from '../../../lib/api/types'
import { useQuery } from '@tanstack/react-query'

const SIDEBAR_STATE_STORAGE_KEY = 'peel-documents-sidebar-open'
const LOCAL_DOCUMENT_STATE_STORAGE_KEY = 'peel-documents-local-state-v1'
const DEFAULT_SCRIPT_NAME_TAGS_TEXT = '{\n  "calc": "SCRIPT_ID"\n}'
const DEFAULT_TEMPLATE =
  '<ul>\n{% for statement in calc.statements %}\n  <li>{{ statement | renderTraceExpression }}</li>\n{% endfor %}\n</ul>'
const DEFAULT_LOCAL_OVERRIDES_TEXT = '{\n  "renderConfigurations": {}\n}'
const DEFAULT_RENDER_CONFIGURATION_NAME = 'default'

type LocalDocumentState = {
  name: string
  scriptNameTagsText: string
  template: string
  bindingsText: string
  renderConfigurationName: string
  localOverridesText: string
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
    bindingsText,
    setBindingsText,
    renderConfigurationId,
    setRenderConfigurationId,
    localOverridesText,
    setLocalOverridesText,
    selectedDocumentId,
    setSelectedDocumentId,
    resetToDefault,
  } = useDocumentDraftState()

  const bindingsState = parseBindings(bindingsText)
  const scriptNameTagsState = parseScriptNameTags(scriptNameTagsText)
  const localOverridesState = parseLocalOverrides(localOverridesText)

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
  const [documentLoadNotice, setDocumentLoadNotice] = useState<string | null>(null)
  const [localDocumentStateMap, setLocalDocumentStateMap] = useState<LocalDocumentStateMap>(() => loadLocalDocumentStateMap())

  const renderConfigurationsQuery = useQuery({
    queryKey: ['render-configs'],
    queryFn: listRenderConfigurations,
  })

  const renderConfigurationName = resolveRenderConfigurationName(
    renderConfigurationId,
    renderConfigurationsQuery.data ?? [],
  )

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
      setRenderConfigurationId(payload.renderConfigurationId)
      setLocalOverridesText(JSON.stringify(payload.localOverrides, null, 2))
      rememberLocalDocumentState(document.id, {
        name: payload.name,
        scriptNameTagsText: JSON.stringify(payload.scriptNameTags, null, 2),
        template: payload.template,
        bindingsText,
        renderConfigurationName: resolveRenderConfigurationName(payload.renderConfigurationId, renderConfigurationsQuery.data ?? []),
        localOverridesText: JSON.stringify(payload.localOverrides, null, 2),
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
    renderConfigurationId.trim().length === 0 ||
    localOverridesState.value === null

  const previewDisabled =
    previewPending ||
    scriptNameTagsState.value === null ||
    template.trim().length === 0 ||
    bindingsState.value === null ||
    renderConfigurationId.trim().length === 0 ||
    localOverridesState.value === null

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
    if (!bindingsState.value || !scriptNameTagsState.value || !localOverridesState.value) {
      return
    }

    saveDocumentMutation.mutate({
      id: selectedDocumentId ?? '',
      name: name.trim(),
      scriptNameTags: scriptNameTagsState.value,
      template,
      renderConfigurationId: renderConfigurationId.trim(),
      localOverrides: localOverridesState.value,
    })
  }

  async function handlePreview() {
    if (!bindingsState.value || !scriptNameTagsState.value || !localOverridesState.value) {
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
        scripTags: previewScriptTags,
        bindings: bindingsState.value,
        renderConfigId: renderConfigurationId.trim(),
        localOverrides: localOverridesState.value,
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
    const nextBindings = {}
    const nextRenderConfigurationId = 'default'
    const nextRenderConfigurationName = DEFAULT_RENDER_CONFIGURATION_NAME
    const nextLocalOverridesText = DEFAULT_LOCAL_OVERRIDES_TEXT

    setSelectedDocumentId(null)
    setName(nextName)
    setScriptNameTagsText(nextScriptNameTagsText)
    setTemplate(nextTemplate)
    setBindingsText(JSON.stringify(nextBindings, null, 2))
    setRenderConfigurationId(nextRenderConfigurationId)
    setLocalOverridesText(nextLocalOverridesText)
    setDocumentLoadNotice(null)

    const nextScriptNameTags = parseScriptNameTags(nextScriptNameTagsText)
    const nextLocalOverrides = parseLocalOverrides(nextLocalOverridesText)
    if (!nextScriptNameTags.value || !nextLocalOverrides.value) {
      return
    }

    saveDocumentMutation.mutate({
      id: '',
      name: nextName,
      scriptNameTags: nextScriptNameTags.value,
      template: nextTemplate,
      renderConfigurationId: nextRenderConfigurationId,
      localOverrides: nextLocalOverrides.value,
    })

    if (selectedDocumentId) {
      rememberLocalDocumentState(selectedDocumentId, {
        name: nextName,
        scriptNameTagsText: nextScriptNameTagsText,
        template: nextTemplate,
        bindingsText: JSON.stringify(nextBindings, null, 2),
        renderConfigurationName: nextRenderConfigurationName,
        localOverridesText: nextLocalOverridesText,
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
      setBindingsText('{}')
      setRenderConfigurationId('default')
      setLocalOverridesText(DEFAULT_LOCAL_OVERRIDES_TEXT)
      setDocumentLoadNotice('No local editor data found for this document. Using local defaults.')
      return
    }

    setName(localState.name)
    setScriptNameTagsText(localState.scriptNameTagsText)
    setTemplate(localState.template)
    setBindingsText(localState.bindingsText)
    const matched = (renderConfigurationsQuery.data ?? []).find((config) => config.name === localState.renderConfigurationName)
    setRenderConfigurationId(matched?.id ?? 'default')
    setLocalOverridesText(localState.localOverridesText)
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
        {localOverridesState.error ? (
          <p className="rounded border border-rose-800/70 bg-rose-950/40 px-3 py-2 text-sm text-rose-200">
            {localOverridesState.error}
          </p>
        ) : null}

        <div className="grid min-h-0 gap-4 lg:grid-cols-[1fr_1fr]">
          <DocumentJsonEditorPane
            title="Script Name Tags (JSON)"
            language="json"
            value={scriptNameTagsText}
            onValueChange={setScriptNameTagsText}
          />
          <DocumentJsonEditorPane
            title="Template (Pebble)"
            language="twig"
            value={template}
            onValueChange={setTemplate}
          />
        </div>

        <div className="grid min-h-0 gap-4 lg:grid-cols-[1fr_1fr]">
          <DocumentJsonEditorPane
            title="Bindings (JSON)"
            language="json"
            value={bindingsText}
            onValueChange={setBindingsText}
          />
          <DocumentJsonEditorPane
            title="Local Overrides (JSON)"
            language="json"
            value={localOverridesText}
            onValueChange={setLocalOverridesText}
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

function parseLocalOverrides(value: string): { value: RenderConfigurationDto | null; error: string | null } {
  try {
    const parsed = JSON.parse(value) as unknown
    if (parsed === null || Array.isArray(parsed) || typeof parsed !== 'object') {
      throw new Error('Local overrides must be a JSON object with a renderConfigurations object.')
    }

    const renderConfigurations = (parsed as Record<string, unknown>).renderConfigurations
    if (
      renderConfigurations === null ||
      renderConfigurations === undefined ||
      Array.isArray(renderConfigurations) ||
      typeof renderConfigurations !== 'object'
    ) {
      throw new Error('Local overrides require renderConfigurations to be an object.')
    }

    const nextRenderConfigurations: Record<string, string> = {}
    for (const [expressionKind, template] of Object.entries(renderConfigurations)) {
      if (typeof template !== 'string') {
        throw new Error('Each renderConfigurations value must be a string template.')
      }
      nextRenderConfigurations[expressionKind] = template
    }

    return {
      value: {
        renderConfigurations: nextRenderConfigurations,
      },
      error: null,
    }
  } catch (error) {
    return {
      value: null,
      error: error instanceof Error ? error.message : 'Local overrides must be valid JSON.',
    }
  }
}

function nextUntitledDocumentName(existingNames: string[]): string {
  const normalizedNames = new Set(existingNames.map((name) => name.trim().toLowerCase()))
  if (!normalizedNames.has('untitled document')) {
    return 'Untitled document'
  }

  let index = 2
  while (normalizedNames.has(`untitled document ${index}`)) {
    index += 1
  }
  return `Untitled document ${index}`
}
