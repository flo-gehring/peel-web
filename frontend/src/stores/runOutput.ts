import { ref } from 'vue'
import { defineStore } from 'pinia'

type RunRequestInfo = {
  panelId: string
  name: string
  script: string
  bindings: Record<string, unknown>
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

  let requestId = 0
  function startRun(nextRequest: RunRequestInfo): number {
    requestId += 1
    isRunning.value = true
    lastRunAt.value = null
    response.value = null
    request.value = nextRequest
    error.value = null
    return requestId
  }

  function setRunSuccess(nextResponse: RunResponseInfo, expectedRequestId = requestId): void {
    if (expectedRequestId !== requestId) return
    isRunning.value = false
    response.value = nextResponse
    lastRunAt.value = Date.now()
    error.value = null
  }

  function setRunError(message: string, expectedRequestId = requestId): void {
    if (expectedRequestId !== requestId) return
    isRunning.value = false
    error.value = message
    lastRunAt.value = Date.now()
  }

  function clearRun(): void {
    requestId += 1
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
