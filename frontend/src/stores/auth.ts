import { ref } from 'vue'
import { defineStore } from 'pinia'
import { api, clearCsrf, cancelWorkspaceRequests } from '@/adapter/client'
import { useEditorDraftStore } from './editorDrafts'
import { useProjectFileStore } from './projectFiles'
import { useDocumentDraftStore } from './documentDrafts'
import { useProjectStore } from './projects'
import { useWorkspaceSelectionStore } from './workspaceSelection'
import { useRunBindingsStore } from './runBindings'
import { useRunOutputStore } from './runOutput'
import { useDocumentPreviewStore } from './documentPreview'

export const useAuthStore = defineStore('auth', () => {
  const isLoaded = ref(false)
  const isAuthenticated = ref(false)
  const email = ref<string | null>(null)
  const userId = ref<string | null>(null)
  const statusError = ref<string | null>(null)
  const sessionExpired = ref(false)
  const workspaceVersion = ref(0)
  const linkEmail = ref('')
  const linkRetryAt = ref(0)
  let previousUserId: string | null = null
  let pendingRefresh: Promise<boolean> | null = null
  let authVersion = 0

  function clearWorkspace(): void {
    cancelWorkspaceRequests()
    useProjectFileStore().clear()
    useEditorDraftStore().clear()
    useDocumentDraftStore().clear()
    useProjectStore().clear()
    useWorkspaceSelectionStore().clear()
    useRunBindingsStore().setRawJson('{}')
    useRunOutputStore().clearRun()
    useDocumentPreviewStore().clear()
    workspaceVersion.value += 1
  }

  function expireSession(): void {
    clearCsrf()
    if (email.value) linkEmail.value = email.value
    if (previousUserId) sessionExpired.value = true
    isAuthenticated.value = false
    email.value = null
    userId.value = null
  }

  async function refresh(): Promise<boolean> {
    if (pendingRefresh) return pendingRefresh
    const version = authVersion
    pendingRefresh = (async () => {
      try {
        const { data, error } = await api.GET('/api/whoami')
        if (version !== authVersion) return false
        if (error || !data) throw new Error('Session status unavailable')
        statusError.value = null
        isLoaded.value = true
        if (!data.authenticated) {
          expireSession()
          return true
        }
        if (!isAuthenticated.value || userId.value !== data.userId) clearCsrf()
        if (previousUserId && previousUserId !== data.userId) clearWorkspace()
        isAuthenticated.value = true
        email.value = data.email ?? null
        if (email.value) linkEmail.value = email.value
        userId.value = data.userId ?? null
        previousUserId = userId.value
        sessionExpired.value = false
        return true
      } catch {
        if (version !== authVersion) return false
        statusError.value =
          'Your sign-in status could not be checked. Check your connection and try again.'
        return false
      }
    })().finally(() => {
      pendingRefresh = null
    })
    return pendingRefresh
  }

  async function logout(): Promise<void> {
    const { error, response } = await api.POST('/api/auth/logout')
    if (error && response.status !== 401) throw new Error('Sign-out failed. Please try again.')
    authVersion += 1
    clearCsrf()
    clearWorkspace()
    previousUserId = null
    isAuthenticated.value = false
    email.value = null
    userId.value = null
    sessionExpired.value = false
    statusError.value = null
    linkEmail.value = ''
    linkRetryAt.value = 0
    isLoaded.value = true
  }

  return {
    isLoaded,
    isAuthenticated,
    email,
    userId,
    statusError,
    sessionExpired,
    workspaceVersion,
    linkEmail,
    linkRetryAt,
    refresh,
    expireSession,
    logout,
  }
})
