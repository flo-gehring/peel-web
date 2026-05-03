import clsx from 'clsx'
import { useMemo, useState } from 'react'

import { DocumentBindingsPane } from './DocumentBindingsPane'
import { DocumentJsonEditorPane } from './DocumentJsonEditorPane'
import { DocumentPreviewPane } from './DocumentPreviewPane'
import { DocumentsHeader } from './DocumentsHeader'
import { DocumentsSidebar } from './DocumentsSidebar'
import { useDocumentDraftState } from '../hooks/useDocumentDraftState'
import { useDocumentsApi } from '../hooks/useDocumentsApi'
import {
  defaultDocumentBindings,
  defaultDocumentContent,
  parseDocumentContent,
} from '../lib/content'
import { parseBindings } from '../../workbench/lib/bindings'
import { formatError } from '../../workbench/lib/errors'

const SIDEBAR_STATE_STORAGE_KEY = 'peel-documents-sidebar-open'

export function DocumentsMode() {
  const {
    name,
    setName,
    contentText,
    setContentText,
    bindingsText,
    setBindingsText,
    selectedDocumentId,
    setSelectedDocumentId,
    resetToDefault,
  } = useDocumentDraftState()

  const contentState = useMemo(() => parseDocumentContent(contentText), [contentText])
  const bindingsState = useMemo(() => parseBindings(bindingsText), [bindingsText])

  const [isSidebarOpen, setIsSidebarOpen] = useState<boolean>(() => {
    const raw = localStorage.getItem(SIDEBAR_STATE_STORAGE_KEY)
    if (raw === null) {
      return true
    }
    return raw === 'true'
  })
  const [isPreviewOpen, setIsPreviewOpen] = useState(false)

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
      setContentText(JSON.stringify(document.content, null, 2))
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
    contentState.value === null ||
    bindingsState.value === null

  const previewDisabled =
    previewDocumentMutation.isPending ||
    contentState.value === null ||
    bindingsState.value === null

  const deleteDisabled = deleteDocumentMutation.isPending || selectedDocumentId === null

  function toggleSidebar() {
    const nextState = !isSidebarOpen
    setIsSidebarOpen(nextState)
    localStorage.setItem(SIDEBAR_STATE_STORAGE_KEY, String(nextState))
  }

  function handleSave() {
    if (!contentState.value || !bindingsState.value) {
      return
    }
    saveDocumentMutation.mutate({
      id: selectedDocumentId ?? undefined,
      name: name.trim(),
      content: contentState.value,
      exampleBindings: bindingsState.value,
    })
  }

  function handlePreview() {
    if (!contentState.value || !bindingsState.value) {
      return
    }
    setIsPreviewOpen(true)
    previewDocumentMutation.mutate({
      content: contentState.value,
      exampleBindings: bindingsState.value,
    })
  }

  function handleCreateDocument() {
    const nextName = nextUntitledDocumentName(documents.map((documentSummary) => documentSummary.name))
    const nextContent = defaultDocumentContent()
    const nextBindings = defaultDocumentBindings()

    setSelectedDocumentId(null)
    setName(nextName)
    setContentText(JSON.stringify(nextContent, null, 2))
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

  const previewError = formatError(previewDocumentMutation.error)
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
          previewPending={previewDocumentMutation.isPending}
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

        <div className="grid min-h-0 gap-4 lg:grid-cols-[1fr_1fr]">
          <DocumentJsonEditorPane
            contentText={contentText}
            onContentTextChange={setContentText}
          />
          <DocumentBindingsPane
            bindingsText={bindingsText}
            onBindingsTextChange={setBindingsText}
          />
        </div>

        <DocumentPreviewPane
          isOpen={isPreviewOpen}
          onClose={() => setIsPreviewOpen(false)}
          previewPending={previewDocumentMutation.isPending}
          previewData={previewDocumentMutation.data ?? null}
          previewError={previewError}
          parseError={contentState.error ?? bindingsState.error}
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
