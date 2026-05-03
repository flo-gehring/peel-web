import clsx from 'clsx'
import { useEffect, useMemo, useState } from 'react'

import { DocumentBindingsPane } from './DocumentBindingsPane'
import { DocumentEditorPane } from './DocumentEditorPane'
import { DocumentPreviewPane } from './DocumentPreviewPane'
import { DocumentsHeader } from './DocumentsHeader'
import { DocumentsSidebar } from './DocumentsSidebar'
import { useDocumentDraftState } from '../hooks/useDocumentDraftState'
import { useDocumentsApi } from '../hooks/useDocumentsApi'
import {
  defaultDocumentBindings,
  defaultDocumentContent,
  documentContentToPlainText,
  normalizeDocumentContent,
} from '../lib/content'
import { renderDocumentPdf } from '../lib/pdf'
import { parseBindings } from '../../workbench/lib/bindings'
import { formatError } from '../../workbench/lib/errors'

const SIDEBAR_STATE_STORAGE_KEY = 'peel-documents-sidebar-open'

export function DocumentsMode() {
  const {
    name,
    setName,
    content,
    setContent,
    bindingsText,
    setBindingsText,
    selectedDocumentId,
    setSelectedDocumentId,
    resetToDefault,
  } = useDocumentDraftState()

  const bindingsState = useMemo(() => parseBindings(bindingsText), [bindingsText])

  const [isSidebarOpen, setIsSidebarOpen] = useState<boolean>(() => {
    const raw = localStorage.getItem(SIDEBAR_STATE_STORAGE_KEY)
    if (raw === null) {
      return true
    }
    return raw === 'true'
  })
  const [isPreviewOpen, setIsPreviewOpen] = useState(false)
  const [previewPending, setPreviewPending] = useState(false)
  const [previewPdfUrl, setPreviewPdfUrl] = useState<string | null>(null)
  const [previewRenderError, setPreviewRenderError] = useState<string | null>(null)

  const {
    documentsQuery,
    loadDocumentMutation,
    saveDocumentMutation,
    deleteDocumentMutation,
  } = useDocumentsApi({
    onDocumentLoaded: (document) => {
      setSelectedDocumentId(document.id)
      setName(document.name)
      setContent(normalizeDocumentContent(document.content))
      setBindingsText(JSON.stringify(document.exampleBindings, null, 2))
    },
    onDocumentSaved: (document) => {
      setSelectedDocumentId(document.id)
      setName(document.name)
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
    bindingsState.value === null

  const previewDisabled =
    previewPending

  const deleteDisabled = deleteDocumentMutation.isPending || selectedDocumentId === null

  useEffect(() => {
    return () => {
      if (previewPdfUrl) {
        URL.revokeObjectURL(previewPdfUrl)
      }
    }
  }, [previewPdfUrl])

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
      content,
      exampleBindings: bindingsState.value,
    })
  }

  async function handlePreview() {
    setPreviewPending(true)
    setPreviewRenderError(null)
    setIsPreviewOpen(true)
    if (previewPdfUrl) {
      URL.revokeObjectURL(previewPdfUrl)
    }

    try {
      const plainText = documentContentToPlainText(content)
      const pdfUrl = await renderDocumentPdf({
        title: name,
        body: plainText,
      })
      setPreviewPdfUrl(pdfUrl)
    } catch (error) {
      setPreviewPdfUrl(null)
      setPreviewRenderError(formatError(error) ?? 'Could not render PDF preview.')
    } finally {
      setPreviewPending(false)
    }
  }

  function handleCreateDocument() {
    const nextName = nextUntitledDocumentName(documents.map((documentSummary) => documentSummary.name))
    const nextContent = defaultDocumentContent()
    const nextBindings = defaultDocumentBindings()

    setSelectedDocumentId(null)
    setName(nextName)
    setContent(nextContent)
    setBindingsText(JSON.stringify(nextBindings, null, 2))

    saveDocumentMutation.mutate({
      name: nextName,
      content: nextContent,
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

        <div className="grid min-h-0 gap-4 lg:grid-cols-[6fr_3fr]">
            <DocumentEditorPane content={content} onContentChange={setContent} />
            <DocumentBindingsPane
            bindingsText={bindingsText}
            onBindingsTextChange={setBindingsText}
          />
        </div>

        <DocumentPreviewPane
          isOpen={isPreviewOpen}
          onClose={() => {
            setIsPreviewOpen(false)
          }}
          previewPending={previewPending}
          pdfUrl={previewPdfUrl}
          previewError={previewRenderError}
          parseError={null}
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
