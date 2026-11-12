<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { LogOut, Settings } from '@lucide/vue'
import { useRoute, useRouter } from 'vue-router'
import { useProjectStore } from '@/stores/projects'
import peelLogo from '@/assets/peel-logo.png'
import { useAuthStore } from '@/stores/auth'
import { useEditorDraftStore } from '@/stores/editorDrafts'
import { useDocumentDraftStore } from '@/stores/documentDrafts'
import { useProjectFileStore } from '@/stores/projectFiles'
import SessionSignInForm from '@/components/SessionSignInForm.vue'

const router = useRouter()
const route = useRoute()
const projects = useProjectStore()
const inWorkspace = computed(() => route.name === 'project')
const projectName = computed(() =>
  projects.activeProject?.projectIdApi?.id === route.params.projectId
    ? projects.activeProject?.name
    : 'Loading project…',
)
const authStore = useAuthStore()
const drafts = useEditorDraftStore()
const documents = useDocumentDraftStore()
const projectFiles = useProjectFileStore()
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
    Object.keys(projectFiles.metadataDrafts).some((id) => projectFiles.metadataDirty(id)) ||
    Object.values(drafts.draftsById).some((draft) => draft.dirty) ||
    Object.values(documents.draftsById).some((draft) => draft.dirty)
  if (dirty && !window.confirm('Discard unsaved changes and sign out?')) return
  signingOut.value = true
  logoutError.value = null
  try {
    const invitationReturn = route.name === 'invitation' ? route.fullPath : null
    await authStore.logout()
    await router.replace(
      invitationReturn ? { name: 'login', query: { redirect: invitationReturn } } : '/login',
    )
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
    <header
      v-if="route.meta.requiresAuth || (route.name === 'invitation' && authStore.isAuthenticated)"
      class="app-header"
    >
      <RouterLink to="/" class="app-brand" aria-label="Peel projects">
        <img :src="peelLogo" alt="" width="38" height="32" /><span>peel</span>
      </RouterLink>
      <nav class="app-navigation" aria-label="Main navigation">
        <RouterLink to="/" :aria-current="route.name === 'home' ? 'page' : undefined"
          >Projects</RouterLink
        >
        <template v-if="inWorkspace"
          ><span class="breadcrumb-divider" aria-hidden="true">/</span
          ><RouterLink to="/print-jobs" class="header-print-jobs">Print jobs</RouterLink
          ><RouterLink
            :to="`/projects/${route.params.projectId}/settings`"
            class="settings-link"
            title="Settings"
            aria-label="Settings"
            ><Settings :size="18" aria-hidden="true" /></RouterLink
          ><strong class="project-breadcrumb">{{ projectName }}</strong></template
        >
        <RouterLink v-if="!inWorkspace" to="/print-jobs">Print jobs</RouterLink>
      </nav>
      <button
        class="logout-button"
        title="logout"
        aria-label="logout"
        :disabled="signingOut"
        :aria-busy="signingOut"
        @click="logout"
      >
        <LogOut :size="18" aria-hidden="true" />
      </button>
    </header>
    <div class="app-content">
      <RouterView :key="authStore.workspaceVersion" />
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
      <p>
        Your drafts are still here. Request an email and paste its sign-in code below to continue.
      </p>
      <SessionSignInForm :redirect="router.currentRoute.value.fullPath" />
      <button @click="refreshSession">Check sign-in</button>
      <button :disabled="signingOut" @click="logout">Discard drafts and sign out</button>
      <p v-if="authStore.statusError || logoutError" role="alert" class="auth-error">
        {{ authStore.statusError || logoutError }}
      </p>
    </div>
  </div>
  <div v-else-if="authStore.statusError || logoutError" class="session-status" role="alert">
    {{ authStore.statusError || logoutError }}
    <button v-if="authStore.statusError" @click="refreshSession">Try again</button>
    <button v-else :disabled="signingOut" @click="logout">Try again</button>
  </div>
</template>

<style>
.workspace-shell {
  display: flex;
  flex-direction: column;
  min-height: 100dvh;
}
.app-content {
  flex: 1;
  min-width: 0;
}
.app-header {
  height: var(--app-header-height);
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 0 24px;
  background: var(--peel-surface);
  border-bottom: 1px solid var(--peel-border);
  position: relative;
  z-index: 20;
}
.app-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--peel-text);
  text-decoration: none;
}
.app-brand img {
  object-fit: contain;
}
.app-brand span {
  font-size: 24px;
  font-weight: 750;
  letter-spacing: -1px;
}
.app-navigation {
  display: flex;
  align-items: center;
  gap: 20px;
  flex: 1;
  min-width: 0;
  padding-left: 24px;
  border-left: 1px solid var(--peel-border);
}
.app-navigation a,
.header-print-jobs {
  color: var(--peel-muted);
  text-decoration: none;
  white-space: nowrap;
}
.app-navigation a:hover,
.app-navigation a[aria-current='page'],
.app-navigation .router-link-active,
.header-print-jobs:hover {
  color: var(--peel-text);
}
.project-breadcrumb {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  font-weight: 600;
}
.settings-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  border-radius: 8px;
}
.settings-link:hover {
  background: var(--peel-raised);
}
.breadcrumb-divider {
  color: var(--peel-muted);
}
.logout-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 32px;
  width: 32px;
  height: 32px;
  padding: 0;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--peel-muted);
}
.logout-button:hover {
  background: var(--peel-raised);
  color: var(--peel-text);
}
@media (max-width: 640px) {
  .app-header {
    gap: 12px;
    padding: 0 12px;
  }
  .app-brand span {
    display: none;
  }
  .app-brand img {
    width: 36px;
  }
  .app-navigation {
    gap: 10px;
    padding-left: 12px;
  }
  .header-print-jobs {
    display: none;
  }
  .project-breadcrumb {
    max-width: 130px;
  }
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
