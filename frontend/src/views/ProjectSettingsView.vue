<script setup lang="ts">
import { LogOut } from '@lucide/vue'
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '@/adapter/client'
import { apiErrorMessage } from '@/adapter/apiError'
import { useAuthStore } from '@/stores/auth'
import { useProjectStore } from '@/stores/projects'
import type { components } from '@/types/api'

type Settings = components['schemas']['ProjectSettings']
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const projects = useProjectStore()
const id = computed(() => String(route.params.projectId))
const settings = ref<Settings | null>(null)
const name = ref('')
const description = ref('')
const inviteEmail = ref('')
const targetEmail = ref('')
const confirmedEmail = ref('')
const transferConfirmation = ref('')
const deleteConfirmation = ref('')
const busy = ref(false)
const loading = ref(true)
const error = ref<string | null>(null)
const notice = ref<string | null>(null)
const owner = computed(() => settings.value?.role === 'OWNER')
const admin = computed(() => owner.value || settings.value?.role === 'ADMIN')
const active = computed(() => settings.value?.state === 'ACTIVE')
const otherMembers = computed(
  () => settings.value?.members?.filter((member) => member.role !== 'OWNER') ?? [],
)

async function load(): Promise<void> {
  loading.value = true
  error.value = null
  try {
    const { data, error: failure } = await api.GET('/api/projects/{id}/settings', {
      params: { path: { id: id.value } },
    })
    if (failure || !data)
      throw new Error(apiErrorMessage(failure, 'Project settings could not be loaded.'))
    const keepDraft =
      settings.value &&
      (name.value !== settings.value.name || description.value !== settings.value.description)
    settings.value = data
    if (!keepDraft) {
      name.value = data.name ?? ''
      description.value = data.description ?? ''
    }
  } catch (failure) {
    error.value =
      failure instanceof Error ? failure.message : 'Project settings could not be loaded.'
  } finally {
    loading.value = false
  }
}
watch(
  id,
  () => {
    settings.value = null
    error.value = null
    void load()
  },
  { immediate: true },
)

async function perform(
  action: () => Promise<{ error?: unknown }>,
  success: string,
  navigateHome = false,
): Promise<void> {
  if (busy.value) return
  busy.value = true
  error.value = null
  notice.value = null
  try {
    const result = await action()
    if (result.error) throw new Error(apiErrorMessage(result.error))
    notice.value = success
    if (navigateHome) {
      projects.clearActiveProject()
      await router.push('/')
    } else await load()
  } catch (failure) {
    error.value = failure instanceof Error ? failure.message : 'The server could not be reached.'
  } finally {
    busy.value = false
  }
}
function save(): void {
  void perform(
    () =>
      api.PUT('/api/projects/{id}/settings', {
        params: { path: { id: id.value } },
        body: { name: name.value.trim(), description: description.value },
      }),
    'Project details saved.',
  )
}
function invite(): void {
  void perform(
    () =>
      api.POST('/api/projects/{id}/invitations', {
        params: { path: { id: id.value } },
        body: { email: inviteEmail.value.trim() },
      }),
    'Invitation email sent. The recipient must explicitly accept it.',
  )
}
function setAdmin(memberId: string, enabled: boolean): void {
  void perform(
    () =>
      api.PUT('/api/projects/{id}/members/{memberId}/admin', {
        params: { path: { id: id.value, memberId } },
        body: { admin: enabled },
      }),
    'Member role updated.',
  )
}
function remove(memberId: string): void {
  if (!window.confirm('Remove this member from the project?')) return
  void perform(
    () =>
      api.DELETE('/api/projects/{id}/members/{memberId}', {
        params: { path: { id: id.value, memberId } },
      }),
    'Member removed.',
  )
}
function leave(): void {
  if (!window.confirm('Leave this project? You will lose access.')) return
  void perform(
    () => api.POST('/api/projects/{id}/leave', { params: { path: { id: id.value } } }),
    '',
    true,
  )
}
function lifecycle(archive: boolean): void {
  if (
    archive &&
    !window.confirm(
      'Archive this project? Existing print jobs may finish; further changes and runs will be blocked.',
    )
  )
    return
  void perform(
    () =>
      archive
        ? api.POST('/api/projects/{id}/archive', { params: { path: { id: id.value } } })
        : api.POST('/api/projects/{id}/restore', { params: { path: { id: id.value } } }),
    archive ? 'Project archived.' : 'Project restored.',
  )
}
function revoke(invitationId: string): void {
  void perform(
    () =>
      api.DELETE('/api/projects/{id}/invitations/{invitationId}', {
        params: { path: { id: id.value, invitationId } },
      }),
    'Invitation revoked.',
  )
}
function resend(invitationId: string): void {
  void perform(
    () =>
      api.POST('/api/projects/{id}/invitations/{invitationId}/resend', {
        params: { path: { id: id.value, invitationId } },
      }),
    'Invitation sent again. The previous link is no longer valid.',
  )
}
function requestTransfer(): void {
  void perform(
    () =>
      api.POST('/api/projects/{id}/transfer', {
        params: { path: { id: id.value } },
        body: { email: confirmedEmail.value.trim(), confirmation: transferConfirmation.value },
      }),
    'Transfer requested. You remain owner until the recipient accepts.',
  )
}
function answerTransfer(answer: 'accept' | 'decline' | 'cancel'): void {
  const requestId = settings.value?.transfer?.id
  if (!requestId) return
  const params = { path: { id: id.value, requestId } }
  void perform(
    () =>
      answer === 'accept'
        ? api.POST('/api/projects/{id}/transfer/{requestId}/accept', { params })
        : answer === 'decline'
          ? api.POST('/api/projects/{id}/transfer/{requestId}/decline', { params })
          : api.DELETE('/api/projects/{id}/transfer/{requestId}', { params }),
    answer === 'accept'
      ? 'Ownership transferred. The previous owner is now an admin.'
      : 'Transfer request closed.',
  )
}
function deleteProject(): void {
  void perform(
    () =>
      api.DELETE('/api/projects/{id}', {
        params: { path: { id: id.value } },
        body: { confirmation: deleteConfirmation.value },
      }),
    '',
    true,
  )
}
</script>

<template>
  <main class="settings-page">
    <header class="settings-heading">
      <div>
        <RouterLink to="/">← Projects</RouterLink>
        <h1>Project settings</h1>
        <p>{{ settings?.name }}</p>
      </div>
      <RouterLink
        v-if="settings && settings.state !== 'DELETING'"
        class="quiet-button"
        :to="{ name: 'project', params: { projectId: id } }"
        >Open workspace</RouterLink
      >
    </header>
    <p v-if="error" role="alert" class="settings-error">{{ error }}</p>
    <p v-if="notice" role="status" class="settings-notice">{{ notice }}</p>
    <p v-if="loading && !settings" role="status">Loading project…</p>
    <button v-if="!settings && !loading" class="quiet-button" @click="load">Try again</button>
    <template v-if="settings">
      <p v-if="!active" class="state-banner">
        {{
          settings.state === 'DELETING'
            ? 'Deletion is incomplete. The owner can retry cleanup below.'
            : 'Archived project: contents are read-only. Existing results remain available.'
        }}
      </p>
      <section class="settings-card">
        <h2>General</h2>
        <form @submit.prevent="save">
          <fieldset :disabled="busy || !active">
            <label for="project-name">Project name</label
            ><input id="project-name" v-model="name" required maxlength="200" />
            <label for="project-description">Description</label
            ><textarea id="project-description" v-model="description" rows="4" maxlength="10000" />
            <button class="primary-button" :disabled="!name.trim()">Save details</button>
          </fieldset>
        </form>
      </section>
      <section class="settings-card">
        <h2>Members</h2>
        <ul class="settings-list">
          <li v-for="member in settings.members" :key="member.id">
            <span
              >{{ member.email }} <small>{{ member.role }}</small></span
            >
            <div v-if="owner && member.role !== 'OWNER' && active" class="button-row">
              <button
                :disabled="busy"
                class="quiet-button"
                @click="setAdmin(member.id!, member.role !== 'ADMIN')"
              >
                {{ member.role === 'ADMIN' ? 'Remove admin rights' : 'Make admin' }}
              </button>
              <button :disabled="busy" class="quiet-button" @click="remove(member.id!)">
                Remove member
              </button>
            </div>
          </li>
        </ul>
        <div v-if="!owner && active" class="leave-project-actions">
          <button :disabled="busy" class="danger-button leave-project-button" @click="leave">
            <LogOut :size="18" aria-hidden="true" />
            Leave project
          </button>
        </div>
      </section>
      <section v-if="admin && settings.state !== 'DELETING'" class="settings-card">
        <h2>Invitations</h2>
        <p>
          Invite by email. Recipients can register if needed and must accept before joining. Links
          expire after 7 days.
        </p>
        <form @submit.prevent="invite">
          <fieldset :disabled="busy || !active">
            <label for="invite-email">Email address</label>
            <div class="button-row">
              <input
                id="invite-email"
                v-model="inviteEmail"
                type="email"
                required
                maxlength="254"
              /><button class="primary-button">Send invitation</button>
            </div>
          </fieldset>
        </form>
        <ul class="settings-list">
          <li v-for="invitation in settings.invitations" :key="invitation.id">
            <span
              >{{ invitation.email
              }}<small
                >Expires
                {{
                  invitation.expiresAt ? new Date(invitation.expiresAt).toLocaleString() : ''
                }}</small
              ></span
            >
            <div class="button-row">
              <button
                class="quiet-button"
                :disabled="busy || !active"
                @click="resend(invitation.id!)"
              >
                Resend</button
              ><button class="quiet-button" :disabled="busy" @click="revoke(invitation.id!)">
                Revoke
              </button>
            </div>
          </li>
        </ul>
      </section>
      <section v-if="settings.transfer" class="settings-card">
        <h2>Ownership transfer pending</h2>
        <p>
          {{ settings.transfer.email }} has been asked to become the owner. Roles remain unchanged
          until acceptance.
        </p>
        <div class="button-row">
          <template v-if="settings.transfer.recipientId === auth.userId"
            ><button
              class="primary-button"
              :disabled="busy || !active"
              @click="answerTransfer('accept')"
            >
              Accept ownership</button
            ><button class="quiet-button" :disabled="busy" @click="answerTransfer('decline')">
              Decline
            </button></template
          >
          <button
            v-if="owner"
            class="quiet-button"
            :disabled="busy"
            @click="answerTransfer('cancel')"
          >
            Withdraw request
          </button>
        </div>
      </section>
      <section v-if="owner && active && !settings.transfer" class="settings-card">
        <h2>Transfer ownership</h2>
        <p>
          The recipient must already be a member and explicitly accept. You will then become an
          admin.
        </p>
        <form @submit.prevent="requestTransfer">
          <fieldset :disabled="busy">
            <label for="transfer-target">New owner</label
            ><select id="transfer-target" v-model="targetEmail" required>
              <option value="">Select a member</option>
              <option v-for="member in otherMembers" :key="member.id" :value="member.email">
                {{ member.email }}
              </option>
            </select>
            <label for="transfer-email">Type the new owner's email</label
            ><input
              id="transfer-email"
              v-model="confirmedEmail"
              type="email"
              autocomplete="off"
              required
            />
            <label for="transfer-confirmation">Type TRANSFER {{ settings.name }}</label
            ><input
              id="transfer-confirmation"
              v-model="transferConfirmation"
              autocomplete="off"
              required
            />
            <button
              class="primary-button"
              :disabled="
                !targetEmail ||
                confirmedEmail.trim().toLowerCase() !== targetEmail.toLowerCase() ||
                transferConfirmation !== `TRANSFER ${settings.name}`
              "
            >
              Request transfer
            </button>
          </fieldset>
        </form>
      </section>
      <section v-if="owner" class="settings-card danger-card">
        <h2>Project lifecycle</h2>
        <button v-if="active" class="quiet-button" :disabled="busy" @click="lifecycle(true)">
          Archive project
        </button>
        <template v-else>
          <button
            v-if="settings.state === 'ARCHIVED'"
            class="quiet-button"
            :disabled="busy"
            @click="lifecycle(false)"
          >
            Restore project
          </button>
          <p>
            Permanently delete this project, scripts, templates, render configurations, invitations,
            print jobs, uploaded CSVs and generated PDFs. This cannot be undone. Running jobs must
            finish first.
          </p>
          <form @submit.prevent="deleteProject">
            <fieldset :disabled="busy">
              <label for="delete-confirmation">Type {{ settings.name }} to confirm deletion</label
              ><input
                id="delete-confirmation"
                v-model="deleteConfirmation"
                autocomplete="off"
                required
              />
              <button class="danger-button" :disabled="deleteConfirmation !== settings.name">
                {{
                  settings.state === 'DELETING'
                    ? 'Retry permanent deletion'
                    : 'Permanently delete project'
                }}
              </button>
            </fieldset>
          </form>
        </template>
      </section>
    </template>
  </main>
</template>

<style scoped>
.settings-page {
  max-width: 1000px;
  margin: auto;
  padding: 36px 24px 80px;
}
.settings-page a {
  color: var(--peel-accent);
  text-decoration: none;
}
.settings-page a:hover {
  text-decoration: underline;
}
.settings-page textarea {
  padding: 10px 12px;
  border: 1px solid var(--peel-border);
  border-radius: 8px;
  background: var(--peel-background);
  color: var(--peel-text);
  resize: vertical;
}
.settings-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 24px;
}
.settings-heading h1 {
  margin: 14px 0 6px;
  font-size: 30px;
}
.settings-card {
  padding: 24px;
  border: 1px solid var(--peel-border);
  border-radius: 12px;
  background: var(--peel-surface);
  margin: 20px 0;
}
.settings-card h2 {
  font-size: 20px;
  margin-bottom: 16px;
}
.settings-card p {
  color: var(--peel-muted);
  margin: 12px 0;
}
fieldset {
  display: grid;
  gap: 12px;
  border: 0;
  padding: 0;
  margin-top: 16px;
}
fieldset:disabled {
  opacity: 0.65;
}
input,
textarea,
select {
  width: 100%;
}
fieldset > button {
  justify-self: start;
}
.settings-list {
  list-style: none;
  padding: 0;
}
.settings-list li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  border-bottom: 1px solid var(--peel-border);
  padding: 16px 0;
}
.settings-list span {
  overflow-wrap: anywhere;
}
small {
  display: block;
  color: var(--peel-muted);
  margin-top: 4px;
}
.button-row {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}
.button-row input {
  flex: 1;
  min-width: 180px;
}
.settings-error {
  color: var(--peel-error);
  padding: 12px 0;
}
.settings-notice {
  color: var(--peel-accent);
}
.state-banner {
  padding: 16px;
  border: 1px solid var(--peel-border);
  border-radius: 8px;
}
.danger-card {
  border-color: var(--peel-error);
}
.leave-project-actions {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid var(--peel-border);
}
.danger-button.leave-project-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: color-mix(in srgb, var(--peel-error) 12%, transparent);
}
.leave-project-button:hover:not(:disabled) {
  background: color-mix(in srgb, var(--peel-error) 22%, transparent);
}
.danger-button {
  padding: 10px 16px;
  border: 1px solid var(--peel-error);
  color: var(--peel-error);
  border-radius: 7px;
  background: transparent;
  cursor: pointer;
}
button:disabled {
  cursor: default;
  opacity: 0.5;
}
</style>
