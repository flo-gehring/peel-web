<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '@/adapter/client'
import { useAuthStore } from '@/stores/auth'
import { safeAuthRedirect } from '@/adapter/authRedirect'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const token = ref(typeof route.query.token === 'string' ? route.query.token : '')
const redirect = safeAuthRedirect(route.query.redirect)
const busy = ref(false)
const error = ref<string | null>(
  token.value ? null : 'This sign-in link is incomplete. Request a new one.',
)
const completed = ref(false)
const canConfirm = computed(() => !!token.value && !completed.value && !busy.value)
// Keep the credential only in memory, never in history or a referrer.
void router.replace({ name: 'confirm-login', query: { redirect } })

async function confirm(): Promise<void> {
  busy.value = true
  error.value = null
  try {
    const checked = await auth.refresh()
    if (!checked) {
      error.value = auth.statusError
      return
    }
    if (auth.isAuthenticated) {
      error.value = 'You are already signed in. Sign out before using another account.'
      return
    }
    const { error: apiError, response } = await api.POST('/api/auth/login', {
      body: { token: token.value },
      bodySerializer: (body) => new URLSearchParams(body).toString(),
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    })
    if (apiError) {
      error.value = apiError.error?.message ?? 'Sign-in failed. Please request a new link.'
      if (response.status === 401) token.value = ''
      return
    }
    token.value = ''
    completed.value = true
    if (await auth.refresh()) await router.replace(redirect)
  } catch {
    error.value = 'The server could not be reached. Check your connection and try again.'
  } finally {
    busy.value = false
  }
}

async function continueAfterLogin(): Promise<void> {
  if (await auth.refresh()) await router.replace(redirect)
}
</script>

<template>
  <main class="auth-page">
    <section class="login-card">
      <p class="eyebrow">PEEL WEB</p>
      <h1>Confirm sign-in</h1>
      <template v-if="auth.isAuthenticated">
        <p>You are signed in as {{ auth.email }}.</p>
        <RouterLink :to="redirect">Continue to PEEL WEB</RouterLink>
        <p>To use a different account, sign out first.</p>
      </template>
      <template v-else-if="completed">
        <p>Your sign-in succeeded. Check your connection to continue.</p>
        <button :disabled="busy" @click="continueAfterLogin">Continue</button>
      </template>
      <template v-else>
        <p>Confirm to sign in with the email address that received this link.</p>
        <button :disabled="!canConfirm" @click="confirm">
          {{ busy ? 'Signing in…' : 'Sign in to PEEL WEB' }}
        </button>
      </template>
      <p v-if="error || auth.statusError" role="alert" class="auth-error">
        {{ error || auth.statusError }}
      </p>
      <RouterLink :to="{ name: 'login', query: { redirect } }"
        >Request a new sign-in link</RouterLink
      >
    </section>
  </main>
</template>
