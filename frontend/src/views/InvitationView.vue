<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '@/adapter/client'
import { apiErrorMessage } from '@/adapter/apiError'
import { useAuthStore } from '@/stores/auth'
import type { components } from '@/types/api'

const route = useRoute()
const auth = useAuthStore()
const invitation = ref<components['schemas']['InvitationDetails'] | null>(null)
const error = ref<string | null>(null)
const busy = ref(false)
const result = ref<string | null>(null)
const token = computed(() => String(route.params.token))
watch(
  [token, () => auth.userId],
  async () => {
    invitation.value = null
    error.value = null
    result.value = null
    if (!auth.isAuthenticated) return
    try {
      const { data, error: failure } = await api.GET('/api/invitations/{token}', {
        params: { path: { token: token.value } },
      })
      if (failure || !data) {
        error.value =
          'This invitation is expired, revoked or unavailable for your signed-in email address.'
        return
      }
      invitation.value = data
    } catch {
      error.value = 'The invitation could not be loaded. Check your connection and try again.'
    }
  },
  { immediate: true },
)
async function answer(accept: boolean): Promise<void> {
  if (busy.value) return
  busy.value = true
  error.value = null
  try {
    const params = { path: { token: token.value } }
    const response = accept
      ? await api.POST('/api/invitations/{token}/accept', { params })
      : await api.POST('/api/invitations/{token}/decline', { params })
    const { error: failure } = response
    if (failure) {
      error.value = apiErrorMessage(failure)
      return
    }
    result.value = accept ? 'You have joined the project.' : 'Invitation declined.'
    invitation.value = null
  } catch {
    error.value = 'The server could not be reached. Please try again.'
  } finally {
    busy.value = false
  }
}
</script>
<template>
  <main class="auth-page" :class="{ 'with-header': auth.isAuthenticated }">
    <section class="login-card">
      <p class="eyebrow">PEEL WEB</p>
      <h1>Project invitation</h1>
      <p v-if="auth.isAuthenticated">
        Signed in as {{ auth.email }}. Use the sign-out button to switch accounts.
      </p>
      <template v-if="!auth.isAuthenticated"
        ><p>
          Sign in or create an account with the email address that received this invitation. Joining
          requires your explicit acceptance after sign-in.
        </p>
        <RouterLink :to="{ name: 'login', query: { redirect: route.fullPath } }"
          >Sign in or register</RouterLink
        >
      </template>
      <template v-else-if="invitation"
        ><p>You are invited to {{ invitation.projectName }} as {{ invitation.email }}.</p>
        <p>Accepting gives you access to project contents.</p>
        <button :disabled="busy" @click="answer(true)">Accept invitation</button
        ><button class="secondary" :disabled="busy" @click="answer(false)">
          Decline invitation
        </button>
      </template>
      <p v-else-if="result" role="status">{{ result }}</p>
      <p v-else-if="!error" role="status">Loading invitation…</p>
      <p v-if="error" role="alert" class="auth-error">{{ error }}</p>
      <RouterLink v-if="auth.isAuthenticated" to="/">Go to projects</RouterLink>
    </section>
  </main>
</template>
<style scoped>
.with-header {
  min-height: calc(100dvh - var(--app-header-height));
}
</style>
