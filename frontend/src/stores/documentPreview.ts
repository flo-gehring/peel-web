import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useDocumentPreviewStore = defineStore('documentPreview', () => {
  const pdfUrl = ref<string | null>(null)
  const error = ref<string | null>(null)
  const isLoading = ref(false)
  const previewVersion = ref(0)

  function start(): void {
    isLoading.value = true
    error.value = null
  }

  function setPreview(pdfData: string): void {
    if (pdfUrl.value) URL.revokeObjectURL(pdfUrl.value)
    const binary = atob(pdfData)
    const bytes = Uint8Array.from(binary, (character) => character.charCodeAt(0))
    pdfUrl.value = URL.createObjectURL(new Blob([bytes], { type: 'application/pdf' }))
    error.value = null
    isLoading.value = false
  }

  function setError(message: string): void {
    error.value = message
    isLoading.value = false
  }

  function requestOpen(): void {
    previewVersion.value += 1
  }

  return { pdfUrl, error, isLoading, previewVersion, start, setPreview, setError, requestOpen }
})
