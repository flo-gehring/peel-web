import clsx from 'clsx'
import { useEffect, useState } from 'react'

import { DocumentJsonEditorPane } from './DocumentJsonEditorPane'
import { DocumentPreviewPane } from './DocumentPreviewPane'
import { DocumentsHeader } from './DocumentsHeader'
import { DocumentsSidebar } from './DocumentsSidebar'
import { useDocumentDraftState } from '../hooks/useDocumentDraftState'
import { useDocumentsApi } from '../hooks/useDocumentsApi'
import { parseBindings } from '../../workbench/lib/bindings'
import { formatError } from '../../workbench/lib/errors'

const SIDEBAR_STATE_STORAGE_KEY = 'peel-documents-sidebar-open'

export function DocumentsMode() {
  const {
    name,
    setName,
    script,
    setScript,
    template,
    setTemplate,
    bindingsText,
    setBindingsText,
    selectedDocumentId,
    setSelectedDocumentId,
    resetToDefault,
  } = useDocumentDraftState()

  const bindingsState = parseBindings(bindingsText)

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

  const {
    documentsQuery,
    loadDocumentMutation,
    saveDocumentMutation,
    deleteDocumentMutation,
    previewDocumentMutation,
  } = useDocumentsApi({
    onDocumentLoaded: (document) => {
      setSelectedDocumentId(document.id)
      setName(document.name)
      setScript(document.script)
      setTemplate(document.template)
      setBindingsText(JSON.stringify(document.exampleBindings, null, 2))
    },
    onDocumentSaved: (document) => {
      setSelectedDocumentId(document.id)
      setName(document.name)
      setScript(document.script)
      setTemplate(document.template)
    },
    onDocumentDeleted: (deletedId) => {
      if (selectedDocumentId === deletedId) {
        resetToDefault()
      }
    },
  })

  const documents = documentsQuery.data ?? []

  const saveDisabled =
    saveDocumentMutation.isPending ||
    name.trim().length === 0 ||
    script.trim().length === 0 ||
    template.trim().length === 0 ||
    bindingsState.value === null

  const previewDisabled =
    previewPending || script.trim().length === 0 || template.trim().length === 0 || bindingsState.value === null

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
    if (!bindingsState.value) {
      return
    }
    saveDocumentMutation.mutate({
      id: selectedDocumentId ?? undefined,
      name: name.trim(),
      script,
      template,
      exampleBindings: bindingsState.value,
    })
  }

  async function handlePreview() {
    if (!bindingsState.value) {
      return
    }

    setPreviewPending(true)
    setPreviewRenderError(null)
    setIsPreviewOpen(true)

    try {
      const previewResponse = await previewDocumentMutation.mutateAsync({
        script,
        bindings: bindingsState.value,
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
    const nextScript = ''
    const nextTemplate = '<ul>\n{% for statement in statements %}\n  <li>{{ statement | renderTraceExpression }}</li>\n{% endfor %}\n</ul>'
    const nextBindings = {}

    setSelectedDocumentId(null)
    setName(nextName)
    setScript(nextScript)
    setTemplate(nextTemplate)
    setBindingsText(JSON.stringify(nextBindings, null, 2))

    saveDocumentMutation.mutate({
      name: nextName,
      script: nextScript,
      template: nextTemplate,
      exampleBindings: nextBindings,
    })
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
        onSelectDocument={(id) => loadDocumentMutation.mutate(id)}
        onCreateDocument={handleCreateDocument}
        onDeleteSelected={handleDeleteSelected}
        deleteDisabled={deleteDisabled}
      />

      <section className="grid min-h-0 gap-4 overflow-hidden">
        <DocumentsHeader
          name={name}
          onNameChange={setName}
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

        <div className="grid min-h-0 gap-4 lg:grid-cols-[1fr_1fr_0.9fr]">
          <DocumentJsonEditorPane
            title="Script (Peel)"
            language="peel"
            value={script}
            onValueChange={setScript}
          />
          <DocumentJsonEditorPane
            title="Template (Pebble)"
            language="twig"
            value={template}
            onValueChange={setTemplate}
          />
          <DocumentJsonEditorPane
            title="Bindings (JSON)"
            language="json"
            value={bindingsText}
            onValueChange={setBindingsText}
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
