import { ref } from 'vue'
import { defineStore } from 'pinia'

type Preview = {
  pdfUrl: string | null
  error: string | null
  isLoading: boolean
  name: string
  signature: string
  renderedAt: number | null
  requestId: number
}
export const useDocumentPreviewStore = defineStore('documentPreview', () => {
  const previews = ref<Record<string, Preview>>({})
  let sequence = 0
  function start(id: string, name: string, signature: string): number {
    const previous = previews.value[id]
    if (previous?.pdfUrl) URL.revokeObjectURL(previous.pdfUrl)
    const requestId = ++sequence
    previews.value[id] = {
      pdfUrl: null,
      error: null,
      isLoading: true,
      name,
      signature,
      renderedAt: null,
      requestId,
    }
    return requestId
  }
  function setPreview(id: string, requestId: number, pdfData: string): void {
    const preview = previews.value[id]
    if (preview?.requestId !== requestId) return
    const bytes = Uint8Array.from(atob(pdfData), (character) => character.charCodeAt(0))
    preview.pdfUrl = URL.createObjectURL(new Blob([bytes], { type: 'application/pdf' }))
    preview.isLoading = false
    preview.error = null
    preview.renderedAt = Date.now()
  }
  function setError(id: string, requestId: number, error: string): void {
    const preview = previews.value[id]
    if (preview?.requestId !== requestId) return
    preview.error = error
    preview.isLoading = false
  }
  function remove(id: string): void {
    const preview = previews.value[id]
    if (preview?.pdfUrl) URL.revokeObjectURL(preview.pdfUrl)
    delete previews.value[id]
  }
  function clear(): void {
    Object.keys(previews.value).forEach(remove)
  }
  return { previews, start, setPreview, setError, remove, clear }
})
