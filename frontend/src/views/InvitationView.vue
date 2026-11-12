<script setup lang="ts">
import { t } from '@/i18n'
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
        error.value = t('thisInvitationIsExpiredRevokedOrUnavailableForYour')
        return
      }
      invitation.value = data
    } catch {
      error.value = t('theInvitationCouldNotBeLoadedCheckYourConnection')
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
    result.value = accept ? t('youHaveJoinedTheProject') : t('invitationDeclined')
    invitation.value = null
  } catch {
    error.value = t('theServerCouldNotBeReachedPleaseTryAgain')
  } finally {
    busy.value = false
  }
}
</script>
<template>
  <main class="auth-page" :class="{ 'with-header': auth.isAuthenticated }">
    <section class="login-card">
      <p class="eyebrow">PEEL WEB</p>
      <h1>{{ t('projectInvitation') }}</h1>
      <p v-if="auth.isAuthenticated">
        {{ t('signedInAsEmailUseTheSignOutButton', { email: auth.email }) }}
      </p>
      <template v-if="!auth.isAuthenticated"
        ><p>{{ t('signInOrCreateAnAccountWithTheEmail') }}</p>
        <RouterLink :to="{ name: 'login', query: { redirect: route.fullPath } }">{{
          t('signInOrRegister')
        }}</RouterLink>
      </template>
      <template v-else-if="invitation"
        ><p>
          {{
            t('youAreInvitedToProjectAsEmail', {
              project: invitation.projectName,
              email: invitation.email,
            })
          }}.
        </p>
        <p>{{ t('acceptingGivesYouAccessToProjectContents') }}</p>
        <button :disabled="busy" @click="answer(true)">{{ t('acceptInvitation') }}</button
        ><button class="secondary" :disabled="busy" @click="answer(false)">
          {{ t('declineInvitation') }}
        </button>
      </template>
      <p v-else-if="result" role="status">{{ result }}</p>
      <p v-else-if="!error" role="status">{{ t('loadingInvitation') }}</p>
      <p v-if="error" role="alert" class="auth-error">{{ error }}</p>
      <RouterLink v-if="auth.isAuthenticated" to="/">{{ t('goToProjects') }}</RouterLink>
    </section>
  </main>
</template>
<style scoped>
.with-header {
  min-height: calc(100dvh - var(--app-header-height));
}
</style>
