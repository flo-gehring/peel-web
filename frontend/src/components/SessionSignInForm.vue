<script setup lang="ts">
import { ref } from 'vue'
import { api } from '@/adapter/client'
import { useAuthStore } from '@/stores/auth'
import SignInForm from './SignInForm.vue'

defineProps<{ redirect: string }>()
const auth = useAuthStore()
const token = ref('')
const busy = ref(false)
const error = ref<string | null>(null)
const completed = ref(false)

async function confirm(): Promise<void> {
  if (busy.value || !token.value.trim()) return
  busy.value = true
  error.value = null
  try {
    // A sign-in in another tab may already have restored this session.
    if (!(await auth.refresh())) return
    if (auth.isAuthenticated) return
    const { error: requestError, response } = await api.POST('/api/auth/login', {
      body: { token: token.value.trim() },
      bodySerializer: (body) => new URLSearchParams(body).toString(),
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    })
    if (requestError) {
      error.value =
        requestError.error?.message ?? 'Sign-in failed. Request a new email and try again.'
      if (response.status === 401) token.value = ''
      return
    }
    token.value = ''
    completed.value = true
    await auth.refresh()
  } catch {
    error.value = 'The server could not be reached. Check your connection and try again.'
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="session-sign-in">
    <SignInForm :redirect="redirect" />
    <form v-if="!completed" class="login-card code-form" @submit.prevent="confirm">
      <label for="session-sign-in-code">Sign-in code from your email</label>
      <input
        id="session-sign-in-code"
        v-model="token"
        name="token"
        autocomplete="one-time-code"
        autocapitalize="off"
        :spellcheck="false"
        :disabled="busy"
        required
      />
      <button type="submit" :disabled="busy || !token.trim()">
        {{ busy ? 'Signing in…' : 'Continue in this tab' }}
      </button>
    </form>
    <p v-else role="status">Sign-in succeeded. Check sign-in below to restore your session.</p>
    <p v-if="error" role="alert" class="auth-error">{{ error }}</p>
  </div>
</template>

<style scoped>
.session-sign-in .login-card {
  width: 100%;
}
.code-form {
  display: grid;
  gap: 12px;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid var(--peel-border);
}
.code-form input {
  width: 100%;
  min-width: 0;
}
</style>
