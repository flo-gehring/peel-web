import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import {
  deleteDocument,
  getDocument,
  listDocuments,
  saveDocument,
} from '../../../lib/api/client'
import type {
  DocumentDetail,
  DocumentSaveRequest,
  DocumentSummary,
} from '../../../lib/api/types'

type UseDocumentsApiOptions = {
  onDocumentLoaded: (document: DocumentDetail) => void
  onDocumentSaved: (document: DocumentDetail) => void
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

  const loadDocumentMutation = useMutation({
    mutationFn: getDocument,
    onSuccess: (loadedDocument) => {
      queryClient.setQueryData(['document', loadedDocument.id], loadedDocument)
      options.onDocumentLoaded(loadedDocument)
    },
  })

  const saveDocumentMutation = useMutation({
    mutationFn: (payload: DocumentSaveRequest) => saveDocument(payload),
    onMutate: async (payload): Promise<SaveDocumentMutationContext> => {
      await queryClient.cancelQueries({ queryKey: ['documents'] })

      const previousDocuments = queryClient.getQueryData<DocumentSummary[]>(['documents']) ?? []

      if (!payload.id) {
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
    onSuccess: (savedDocument, _payload, context) => {
      queryClient.setQueryData<DocumentSummary[]>(['documents'], (current) => {
        const nextSummary: DocumentSummary = {
          id: savedDocument.id,
          name: savedDocument.name,
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
      queryClient.setQueryData(['document', savedDocument.id], savedDocument)
      options.onDocumentSaved(savedDocument)
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

  return {
    documentsQuery,
    loadDocumentMutation,
    saveDocumentMutation,
    deleteDocumentMutation,
  }
}
