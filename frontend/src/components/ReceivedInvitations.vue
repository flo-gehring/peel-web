<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api } from '@/adapter/client'
import { apiErrorMessage } from '@/adapter/apiError'
import { useProjectStore } from '@/stores/projects'
import type { components } from '@/types/api'

const projects = useProjectStore()
const invitations = ref<components['schemas']['ReceivedInvitation'][]>([])
const loading = ref(true)
const busy = ref(false)
const error = ref<string | null>(null)
const message = ref('')

async function load(): Promise<void> {
  loading.value = true
  error.value = null
  try {
    const { data, error: requestError } = await api.GET('/api/invitations/received')
    if (requestError) throw requestError
    invitations.value = data ?? []
  } catch {
    error.value = 'Invitations could not be loaded. Please try again.'
  } finally {
    loading.value = false
  }
}

async function answer(id: string, accept: boolean): Promise<void> {
  if (busy.value) return
  busy.value = true
  error.value = null
  message.value = ''
  try {
    const { error: requestError } = await api.POST(
      accept ? '/api/invitations/received/{id}/accept' : '/api/invitations/received/{id}/decline',
      { params: { path: { id } } },
    )
    if (requestError) throw requestError
    invitations.value = invitations.value.filter((invitation) => invitation.id !== id)
    message.value = accept ? 'Invitation accepted.' : 'Invitation declined.'
    if (accept) {
      try {
        await projects.loadProjects()
      } catch {
        error.value = 'Invitation accepted, but projects could not be refreshed. Reload the page.'
      }
    }
  } catch (cause) {
    error.value = apiErrorMessage(
      cause,
      'The invitation could not be updated. It may have expired or been revoked. Refresh invitations and try again.',
    )
  } finally {
    busy.value = false
  }
}

onMounted(load)
</script>

<template>
  <section
    v-if="loading || invitations.length || error || message"
    class="invitations"
    aria-labelledby="invitations-title"
    :aria-busy="loading || busy"
  >
    <h2 id="invitations-title">Project invitations</h2>
    <p v-if="loading" role="status">Loading invitations…</p>
    <p v-if="message" role="status">{{ message }}</p>
    <div v-if="error" class="invitation-error">
      <p role="alert">{{ error }}</p>
      <button class="quiet-button" :disabled="loading || busy" @click="load">
        Refresh invitations
      </button>
    </div>
    <ul v-if="!loading && invitations.length">
      <li v-for="invitation in invitations" :key="invitation.id">
        <div class="invitation-details">
          <strong>{{ invitation.projectName }}</strong>
          <span>Invited by {{ invitation.invitedByEmail }}</span>
          <span>Expires {{ new Date(invitation.expiresAt!).toLocaleString() }}</span>
        </div>
        <div class="invitation-actions">
          <button class="primary-button" :disabled="busy" @click="answer(invitation.id!, true)">
            Accept
          </button>
          <button class="quiet-button" :disabled="busy" @click="answer(invitation.id!, false)">
            Decline
          </button>
        </div>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.invitations {
  margin-bottom: 28px;
  padding: 24px 28px;
  border: 1px solid var(--peel-border);
  border-radius: 12px;
  background: var(--peel-surface);
}
h2 {
  margin-bottom: 16px;
}
ul {
  list-style: none;
  padding: 0;
  margin: 0;
}
li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 0;
}
li + li {
  border-top: 1px solid var(--peel-border);
}
.invitation-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
  overflow-wrap: anywhere;
  min-width: 0;
}
.invitation-details span {
  color: var(--peel-muted);
  font-size: 13px;
}
.invitation-actions {
  display: flex;
  gap: 10px;
}
.invitation-error {
  color: var(--peel-error);
}
@media (max-width: 680px) {
  .invitations {
    padding: 18px;
  }
  li {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
