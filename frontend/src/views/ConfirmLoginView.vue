<script setup lang="ts">
import { t } from '@/i18n'
import { apiErrorMessage } from '@/adapter/apiError'
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
const error = ref<string | null>(token.value ? null : t('thisSignInLinkIsIncompleteRequestANew'))
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
      error.value = t('youAreAlreadySignedInSignOutBeforeUsing')
      return
    }
    const { error: apiError, response } = await api.POST('/api/auth/login', {
      body: { token: token.value },
      bodySerializer: (body) => new URLSearchParams(body).toString(),
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    })
    if (apiError) {
      error.value = apiErrorMessage(apiError, t('signInFailedPleaseRequestANewLink'))
      if (response.status === 401) token.value = ''
      return
    }
    token.value = ''
    completed.value = true
    if (await auth.refresh()) await router.replace(redirect)
  } catch {
    error.value = t('theServerCouldNotBeReachedCheckYourConnection')
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
      <h1>{{ t('confirmSignIn') }}</h1>
      <template v-if="auth.isAuthenticated">
        <p>{{ t('youAreSignedInAsEmail', { email: auth.email }) }}.</p>
        <RouterLink :to="redirect">{{ t('continueToPeelWeb') }}</RouterLink>
        <p>{{ t('toUseADifferentAccountSignOutFirst') }}</p>
      </template>
      <template v-else-if="completed">
        <p>{{ t('yourSignInSucceededCheckYourConnectionToContinue') }}</p>
        <button :disabled="busy" @click="continueAfterLogin">{{ t('continue') }}</button>
      </template>
      <template v-else>
        <p>{{ t('confirmToSignInWithTheEmailAddressThat') }}</p>
        <button :disabled="!canConfirm" @click="confirm">
          {{ busy ? t('signingIn') : t('signInToPeelWeb') }}
        </button>
      </template>
      <p v-if="error || auth.statusError" role="alert" class="auth-error">
        {{ error || auth.statusError }}
      </p>
      <RouterLink :to="{ name: 'login', query: { redirect } }">{{
        t('requestANewSignInLink')
      }}</RouterLink>
    </section>
  </main>
</template>
