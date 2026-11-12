<script setup lang="ts">
import { t } from '@/i18n'
import { computed, onUnmounted, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { api } from '@/adapter/client'
import { safeAuthRedirect } from '@/adapter/authRedirect'
import { apiErrorMessage } from '@/adapter/apiError'

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
      error.value = apiErrorMessage(apiError, t('theSignInEmailCouldNotBeSentPlease'))
      return
    }
    auth.linkEmail = email.value.trim()
    auth.linkRetryAt = Date.now() + 60_000
    sent.value = true
    emit('sent')
  } catch {
    error.value = t('theServerCouldNotBeReachedCheckYourConnection')
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <section class="login-card">
    <p class="eyebrow">PEEL WEB</p>
    <template v-if="sent">
      <h1>{{ t('checkYourInbox') }}</h1>
      <p v-if="auth.linkEmail">
        {{ t('aSignInLinkHasBeenSentToEmail', { email: auth.linkEmail }) }}
      </p>
      <p v-else>{{ t('openYourSignInEmailOrRequestAnotherLink') }}</p>
      <p>{{ t('theLinkIsValidFor15MinutesAndCan') }}</p>
    </template>
    <template v-else>
      <h1>{{ t('signIn') }}</h1>
      <p>{{ t('enterYourEmailAddressToReceiveAOneTime') }}</p>
    </template>
    <form v-if="!sent || !auth.linkEmail" @submit.prevent="send">
      <label
        >{{ t('email')
        }}<input
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
        {{ busy ? t('sending') : t('sendSignInLink') }}
      </button>
    </form>
    <template v-else>
      <button :disabled="busy || waitSeconds > 0" @click="send">
        {{ busy ? t('sending') : t('sendAnotherLink') }}
      </button>
      <button class="secondary" :disabled="busy" @click="useAnotherEmail">
        {{ t('useAnotherEmail') }}
      </button>
    </template>
    <p v-if="waitSeconds > 0">
      {{ t('youCanRequestAnotherLinkInSecondsSeconds', { seconds: waitSeconds }) }}
    </p>
    <p v-if="error" role="alert" class="auth-error">{{ error }}</p>
  </section>
</template>
