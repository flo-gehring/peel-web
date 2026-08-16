import { ref } from 'vue'
import { defineStore } from 'pinia'

type RunRequestInfo = {
  panelId: string
  name: string
  script: string
}

type RunResponseInfo = {
  trace?: unknown
  result?: unknown
}

export const useRunOutputStore = defineStore('runOutput', () => {
  const isRunning = ref(false)
  const lastRunAt = ref<number | null>(null)
  const request = ref<RunRequestInfo | null>(null)
  const response = ref<RunResponseInfo | null>(null)
  const error = ref<string | null>(null)

  function startRun(nextRequest: RunRequestInfo): void {
    isRunning.value = true
    request.value = nextRequest
    error.value = null
  }

  function setRunSuccess(nextResponse: RunResponseInfo): void {
    isRunning.value = false
    response.value = nextResponse
    lastRunAt.value = Date.now()
    error.value = null
  }

  function setRunError(message: string): void {
    isRunning.value = false
    error.value = message
    lastRunAt.value = Date.now()
  }

  function clearRun(): void {
    isRunning.value = false
    request.value = null
    response.value = null
    error.value = null
    lastRunAt.value = null
  }

  return {
    isRunning,
    lastRunAt,
    request,
    response,
    error,
    startRun,
    setRunSuccess,
    setRunError,
    clearRun,
  }
})
