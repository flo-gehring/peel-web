<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useEditorDraftStore } from '@/stores/editorDrafts'
import { useDocumentDraftStore } from '@/stores/documentDrafts'
import SignInForm from '@/components/SignInForm.vue'

const router = useRouter()
const authStore = useAuthStore()
const drafts = useEditorDraftStore()
const documents = useDocumentDraftStore()
const signingOut = ref(false)
const logoutError = ref<string | null>(null)

async function refreshSession(): Promise<void> {
  const checked = await authStore.refresh()
  if (
    checked &&
    !authStore.sessionExpired &&
    !authStore.isAuthenticated &&
    router.currentRoute.value.meta.requiresAuth
  ) {
    await router.replace({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
  }
}

async function logout(): Promise<void> {
  const dirty =
    Object.values(drafts.draftsById).some((draft) => draft.dirty) ||
    Object.values(documents.draftsById).some((draft) => draft.dirty)
  if (dirty && !window.confirm('Discard unsaved changes and sign out?')) return
  signingOut.value = true
  logoutError.value = null
  try {
    await authStore.logout()
    await router.replace('/login')
  } catch {
    logoutError.value = 'Sign-out failed. Check your connection and try again.'
  } finally {
    signingOut.value = false
  }
}

onMounted(() => {
  void refreshSession()
  window.addEventListener('focus', refreshSession)
})
onUnmounted(() => window.removeEventListener('focus', refreshSession))
</script>

<template>
  <div
    :inert="authStore.sessionExpired && !!router.currentRoute.value.meta.requiresAuth"
    class="workspace-shell"
  >
    <RouterView :key="authStore.workspaceVersion" />
    <div class="account-button">
      <RouterLink :to="authStore.isAuthenticated ? '/' : '/login'">{{
        authStore.email || 'Sign in'
      }}</RouterLink>
      <button
        v-if="authStore.isAuthenticated || authStore.sessionExpired"
        :disabled="signingOut"
        @click="logout"
      >
        {{ signingOut ? 'Signing out…' : 'Sign out' }}
      </button>
      <p v-if="logoutError" role="alert" class="auth-error">{{ logoutError }}</p>
    </div>
  </div>
  <div
    v-if="authStore.sessionExpired && router.currentRoute.value.meta.requiresAuth"
    class="session-overlay"
    role="dialog"
    aria-modal="true"
    aria-labelledby="session-title"
  >
    <div class="session-content">
      <h2 id="session-title">Your session has ended</h2>
      <p>Your drafts are still here. Sign in using the email link, then return to this tab.</p>
      <SignInForm :redirect="router.currentRoute.value.fullPath" />
      <button @click="refreshSession">Check sign-in</button>
      <button :disabled="signingOut" @click="logout">Discard drafts and sign out</button>
      <p v-if="authStore.statusError || logoutError" role="alert" class="auth-error">
        {{ authStore.statusError || logoutError }}
      </p>
    </div>
  </div>
  <div v-else-if="authStore.statusError" class="session-status" role="alert">
    {{ authStore.statusError }} <button @click="refreshSession">Try again</button>
  </div>
</template>

<style>
.workspace-shell {
  display: contents;
}
.account-button {
  position: fixed;
  top: 16px;
  right: 18px;
  z-index: 200;
  padding: 7px 10px;
  border: 1px solid #4d5865;
  background: #20242a;
  color: #edf1f5;
  font:
    12px ui-monospace,
    monospace;
  text-decoration: none;
}
.account-button a {
  color: inherit;
  text-decoration: none;
}
.account-button button {
  margin-left: 12px;
}
.session-overlay {
  position: fixed;
  inset: 0;
  z-index: 300;
  display: grid;
  place-items: center;
  background: rgba(0, 0, 0, 0.85);
  color: #edf1f5;
  overflow: auto;
  padding: 24px;
}
.session-content {
  display: grid;
  gap: 16px;
  max-width: 480px;
}
.session-content .login-card {
  width: 100%;
}
.session-status {
  position: fixed;
  bottom: 16px;
  left: 16px;
  right: 16px;
  z-index: 250;
  padding: 12px;
  border: 1px solid #a86a39;
  background: #20242a;
  color: #edf1f5;
}
</style>
