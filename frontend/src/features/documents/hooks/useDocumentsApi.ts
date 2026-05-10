import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import {
  deleteDocument,
  listDocuments,
  previewDocument,
  saveDocument,
} from '../../../lib/api/client'
import type {
  DocumentPreviewRequest,
  DocumentSaveRequest,
  DocumentSaveResponse,
  DocumentSummary,
} from '../../../lib/api/types'

type UseDocumentsApiOptions = {
  onDocumentSaved: (document: DocumentSaveResponse, payload: DocumentSaveRequest) => void
  onDocumentDeleted: (deletedId: string) => void
}

type SaveDocumentMutationContext = {
  previousDocuments: DocumentSummary[]
  optimisticId?: string
}

export function useDocumentsApi(options: UseDocumentsApiOptions) {
  const queryClient = useQueryClient()

  const documentsQuery = useQuery({
    queryKey: ['documents'],
    queryFn: listDocuments,
  })

  const saveDocumentMutation = useMutation({
    mutationFn: (payload: DocumentSaveRequest) => saveDocument(payload),
    onMutate: async (payload): Promise<SaveDocumentMutationContext> => {
      await queryClient.cancelQueries({ queryKey: ['documents'] })

      const previousDocuments = queryClient.getQueryData<DocumentSummary[]>(['documents']) ?? []

      if (payload.id.trim().length === 0) {
        const optimisticId = `temp-${Date.now()}`
        const optimisticSummary: DocumentSummary = {
          id: optimisticId,
          name: payload.name,
          updatedAt: new Date().toISOString(),
        }

        queryClient.setQueryData<DocumentSummary[]>(['documents'], (current) => {
          const existing = current ?? []
          return [optimisticSummary, ...existing]
        })

        return { previousDocuments, optimisticId }
      }

      queryClient.setQueryData<DocumentSummary[]>(['documents'], (current) => {
        const existing = current ?? []
        return existing.map((documentSummary) => {
          if (documentSummary.id !== payload.id) {
            return documentSummary
          }
          return {
            ...documentSummary,
            name: payload.name,
            updatedAt: new Date().toISOString(),
          }
        })
      })

      return { previousDocuments }
    },
    onSuccess: (savedDocument, payload, context) => {
      queryClient.setQueryData<DocumentSummary[]>(['documents'], (current) => {
        const nextSummary: DocumentSummary = {
          id: savedDocument.id,
          name: payload.name,
          updatedAt: savedDocument.updatedAt,
        }

        const existing = current ?? []
        const withoutCurrent = existing.filter((documentSummary) => documentSummary.id !== savedDocument.id)
        const withoutOptimistic = context?.optimisticId
          ? withoutCurrent.filter((documentSummary) => documentSummary.id !== context.optimisticId)
          : withoutCurrent
        const next = [nextSummary, ...withoutOptimistic]
        next.sort((left, right) => right.updatedAt.localeCompare(left.updatedAt))
        return next
      })
      options.onDocumentSaved(savedDocument, payload)
    },
    onError: (_error, _payload, context: SaveDocumentMutationContext | undefined) => {
      queryClient.setQueryData(['documents'], context?.previousDocuments ?? [])
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ['documents'] })
    },
  })

  const deleteDocumentMutation = useMutation({
    mutationFn: async (id: string) => {
      await deleteDocument(id)
      return id
    },
    onSuccess: (deletedId) => {
      queryClient.invalidateQueries({ queryKey: ['documents'] })
      queryClient.removeQueries({ queryKey: ['document', deletedId] })
      options.onDocumentDeleted(deletedId)
    },
  })

  const previewDocumentMutation = useMutation({
    mutationFn: (payload: DocumentPreviewRequest) => previewDocument(payload),
  })

  return {
    documentsQuery,
    saveDocumentMutation,
    deleteDocumentMutation,
    previewDocumentMutation,
  }
}
