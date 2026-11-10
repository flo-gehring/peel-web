<script setup lang="ts">
import { computed, onUnmounted, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { api } from '@/adapter/client'
import { safeAuthRedirect } from '@/adapter/authRedirect'

const props = defineProps<{ redirect?: unknown; initiallySent?: boolean }>()
const emit = defineEmits<{ sent: [] }>()
const auth = useAuthStore()
const email = ref(auth.linkEmail)
const sent = ref(props.initiallySent ?? false)
const busy = ref(false)
const error = ref<string | null>(null)
const now = ref(Date.now())
const timer = window.setInterval(() => {
  now.value = Date.now()
}, 1000)
onUnmounted(() => window.clearInterval(timer))
const waitSeconds = computed(() => Math.max(0, Math.ceil((auth.linkRetryAt - now.value) / 1000)))

function useAnotherEmail(): void {
  sent.value = false
  error.value = null
}

async function send(): Promise<void> {
  if (busy.value || waitSeconds.value > 0) return
  busy.value = true
  error.value = null
  try {
    const { error: apiError, response } = await api.POST('/api/auth/link', {
      body: { email: email.value.trim(), redirect: safeAuthRedirect(props.redirect) },
    })
    if (apiError) {
      if (response.status === 429) {
        auth.linkRetryAt = Date.now() + Number(response.headers.get('Retry-After') || 60) * 1000
      }
      error.value =
        apiError.error?.message ?? 'The sign-in email could not be sent. Please try again.'
      return
    }
    auth.linkEmail = email.value.trim()
    auth.linkRetryAt = Date.now() + 60_000
    sent.value = true
    emit('sent')
  } catch {
    error.value = 'The server could not be reached. Check your connection and try again.'
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <section class="login-card">
    <p class="eyebrow">PEEL WEB</p>
    <template v-if="sent">
      <h1>Check your inbox</h1>
      <p v-if="auth.linkEmail">A sign-in link has been sent to {{ auth.linkEmail }}.</p>
      <p v-else>Open your sign-in email, or request another link.</p>
      <p>
        The link is valid for 15 minutes and can be used once. Check your spam folder if it does not
        arrive.
      </p>
    </template>
    <template v-else>
      <h1>Sign in</h1>
      <p>
        Enter your email address to receive a one-time sign-in link. Your first sign-in creates your
        account.
      </p>
    </template>
    <form v-if="!sent || !auth.linkEmail" @submit.prevent="send">
      <label
        >Email
        <input
          v-model="email"
          name="email"
          type="email"
          autocomplete="email"
          maxlength="254"
          required
          :disabled="busy"
        />
      </label>
      <button type="submit" :disabled="busy || waitSeconds > 0">
        {{ busy ? 'Sending…' : 'Send sign-in link' }}
      </button>
    </form>
    <template v-else>
      <button :disabled="busy || waitSeconds > 0" @click="send">
        {{ busy ? 'Sending…' : 'Send another link' }}
      </button>
      <button
        class="secondary"
        :disabled="busy"
        @click="useAnotherEmail"
      >
        Use another email
      </button>
    </template>
    <p v-if="waitSeconds > 0">You can request another link in {{ waitSeconds }} seconds.</p>
    <p v-if="error" role="alert" class="auth-error">{{ error }}</p>
  </section>
</template>
