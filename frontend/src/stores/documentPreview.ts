import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useDocumentPreviewStore = defineStore('documentPreview', () => {
  const html = ref<string | null>(null)
  const error = ref<string | null>(null)
  const isLoading = ref(false)
  const previewVersion = ref(0)

  function start(): void {
    isLoading.value = true
    error.value = null
  }

  function setPreview(value: string): void {
    html.value = value
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

  return { html, error, isLoading, previewVersion, start, setPreview, setError, requestOpen }
})
