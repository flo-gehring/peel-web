<script setup lang="ts">
import { formatDateTime, t } from '@/i18n'
import { ChevronRight, Crown, LogOut, User, UserRoundStar } from '@lucide/vue'
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
// This is an API confirmation token, intentionally identical in every language.
const transferPhrase = computed(() => `TRANSFER ${settings.value?.name ?? ''}`)
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
      throw new Error(apiErrorMessage(failure, t('projectSettingsCouldNotBeLoaded')))
    const keepDraft =
      settings.value &&
      (name.value !== settings.value.name || description.value !== settings.value.description)
    settings.value = data
    if (!keepDraft) {
      name.value = data.name ?? ''
      description.value = data.description ?? ''
    }
  } catch (failure) {
    error.value = failure instanceof Error ? failure.message : t('projectSettingsCouldNotBeLoaded')
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
    error.value = failure instanceof Error ? failure.message : t('theServerCouldNotBeReached')
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
    t('projectDetailsSaved'),
  )
}
function invite(): void {
  void perform(
    () =>
      api.POST('/api/projects/{id}/invitations', {
        params: { path: { id: id.value } },
        body: { email: inviteEmail.value.trim() },
      }),
    t('invitationEmailSentTheRecipientMustExplicitlyAcceptIt'),
  )
}
function setAdmin(memberId: string, enabled: boolean): void {
  void perform(
    () =>
      api.PUT('/api/projects/{id}/members/{memberId}/admin', {
        params: { path: { id: id.value, memberId } },
        body: { admin: enabled },
      }),
    t('memberRoleUpdated'),
  )
}
function remove(memberId: string): void {
  if (!window.confirm(t('removeThisMemberFromTheProject'))) return
  void perform(
    () =>
      api.DELETE('/api/projects/{id}/members/{memberId}', {
        params: { path: { id: id.value, memberId } },
      }),
    t('memberRemoved'),
  )
}
function leave(): void {
  if (!window.confirm(t('leaveThisProjectYouWillLoseAccess'))) return
  void perform(
    () => api.POST('/api/projects/{id}/leave', { params: { path: { id: id.value } } }),
    '',
    true,
  )
}
function lifecycle(archive: boolean): void {
  if (archive && !window.confirm(t('archiveThisProjectExistingPrintJobsMayFinishFurther'))) return
  void perform(
    () =>
      archive
        ? api.POST('/api/projects/{id}/archive', { params: { path: { id: id.value } } })
        : api.POST('/api/projects/{id}/restore', { params: { path: { id: id.value } } }),
    archive ? t('projectArchived') : t('projectRestored'),
  )
}
function revoke(invitationId: string): void {
  void perform(
    () =>
      api.DELETE('/api/projects/{id}/invitations/{invitationId}', {
        params: { path: { id: id.value, invitationId } },
      }),
    t('invitationRevoked'),
  )
}
function resend(invitationId: string): void {
  void perform(
    () =>
      api.POST('/api/projects/{id}/invitations/{invitationId}/resend', {
        params: { path: { id: id.value, invitationId } },
      }),
    t('invitationSentAgainThePreviousLinkIsNoLonger'),
  )
}
function requestTransfer(): void {
  if (
    !targetEmail.value ||
    confirmedEmail.value.trim().toLowerCase() !== targetEmail.value.toLowerCase() ||
    transferConfirmation.value !== transferPhrase.value
  )
    return
  void perform(
    () =>
      api.POST('/api/projects/{id}/transfer', {
        params: { path: { id: id.value } },
        body: { email: confirmedEmail.value.trim(), confirmation: transferConfirmation.value },
      }),
    t('transferRequestedYouRemainOwnerUntilTheRecipientAccepts'),
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
      ? t('ownershipTransferredThePreviousOwnerIsNowAnAdmin')
      : t('transferRequestClosed'),
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
        <RouterLink to="/">{{ t('projects2') }}</RouterLink>
        <h1>{{ t('projectSettings') }}</h1>
        <p>{{ settings?.name }}</p>
      </div>
      <RouterLink
        v-if="settings && settings.state !== 'DELETING'"
        class="quiet-button"
        :to="{ name: 'project', params: { projectId: id } }"
        >{{ t('openWorkspace') }}</RouterLink
      >
    </header>
    <p v-if="error" role="alert" class="settings-error">{{ error }}</p>
    <p v-if="notice" role="status" class="settings-notice">{{ notice }}</p>
    <p v-if="loading && !settings" role="status">{{ t('loadingProject') }}</p>
    <button v-if="!settings && !loading" class="quiet-button" @click="load">
      {{ t('tryAgain') }}
    </button>
    <template v-if="settings">
      <p v-if="!active" class="state-banner">
        {{
          settings.state === 'DELETING'
            ? t('deletionIsIncompleteTheOwnerCanRetryCleanupBelow')
            : t('archivedProjectContentsAreReadOnlyExistingResultsRemain')
        }}
      </p>
      <section class="settings-card">
        <h2>{{ t('general') }}</h2>
        <form @submit.prevent="save">
          <fieldset :disabled="busy || !active">
            <label for="project-name">{{ t('projectName') }}</label
            ><input id="project-name" v-model="name" required maxlength="200" />
            <label for="project-description">{{ t('description') }}</label
            ><textarea id="project-description" v-model="description" rows="4" maxlength="10000" />
            <button class="primary-button" :disabled="!name.trim()">{{ t('saveDetails') }}</button>
          </fieldset>
        </form>
      </section>
      <section class="settings-card">
        <h2>{{ t('members') }}</h2>
        <ul class="settings-list">
          <li v-for="member in settings.members" :key="member.id">
            <div class="member-identity">
              <span class="member-email">{{ member.email }}</span>
              <span class="role-badge" :class="`role-${member.role?.toLowerCase()}`">
                <Crown v-if="member.role === 'OWNER'" :size="14" aria-hidden="true" />
                <UserRoundStar v-else-if="member.role === 'ADMIN'" :size="14" aria-hidden="true" />
                <User v-else :size="14" aria-hidden="true" />
                {{
                  member.role === 'OWNER'
                    ? t('owner')
                    : member.role === 'ADMIN'
                      ? t('admin')
                      : t('member')
                }}
              </span>
            </div>
            <div v-if="owner && member.role !== 'OWNER' && active" class="button-row">
              <button
                :disabled="busy"
                class="quiet-button"
                @click="setAdmin(member.id!, member.role !== 'ADMIN')"
              >
                {{ member.role === 'ADMIN' ? t('removeAdminRights') : t('makeAdmin') }}
              </button>
              <button :disabled="busy" class="quiet-button" @click="remove(member.id!)">
                {{ t('removeMember') }}
              </button>
            </div>
          </li>
        </ul>
        <div v-if="!owner && active" class="leave-project-actions">
          <button :disabled="busy" class="danger-button leave-project-button" @click="leave">
            <LogOut :size="18" aria-hidden="true" />{{ t('leaveProject') }}
          </button>
        </div>
      </section>
      <section v-if="admin && settings.state !== 'DELETING'" class="settings-card">
        <h2>{{ t('invitations') }}</h2>
        <p>{{ t('inviteByEmailRecipientsCanRegisterIfNeededAnd') }}</p>
        <form @submit.prevent="invite">
          <fieldset :disabled="busy || !active">
            <label for="invite-email">{{ t('emailAddress') }}</label>
            <div class="button-row">
              <input
                id="invite-email"
                v-model="inviteEmail"
                type="email"
                required
                maxlength="254"
              /><button class="primary-button">{{ t('sendInvitation') }}</button>
            </div>
          </fieldset>
        </form>
        <ul class="settings-list">
          <li v-for="invitation in settings.invitations" :key="invitation.id">
            <span
              >{{ invitation.email
              }}<small
                >{{ t('expires') }}
                {{ invitation.expiresAt ? formatDateTime(invitation.expiresAt) : '' }}</small
              ></span
            >
            <div class="button-row">
              <button
                class="quiet-button"
                :disabled="busy || !active"
                @click="resend(invitation.id!)"
              >
                {{ t('resend') }}</button
              ><button class="quiet-button" :disabled="busy" @click="revoke(invitation.id!)">
                {{ t('revoke') }}
              </button>
            </div>
          </li>
        </ul>
      </section>
      <section v-if="settings.transfer" class="settings-card">
        <h2>{{ t('ownershipTransferPending') }}</h2>
        <p>
          {{ t('emailHasBeenAskedToBecomeTheOwnerRoles', { email: settings.transfer.email }) }}
        </p>
        <div class="button-row">
          <template v-if="settings.transfer.recipientId === auth.userId"
            ><button
              class="primary-button"
              :disabled="busy || !active"
              @click="answerTransfer('accept')"
            >
              {{ t('acceptOwnership') }}</button
            ><button class="quiet-button" :disabled="busy" @click="answerTransfer('decline')">
              {{ t('decline') }}
            </button></template
          >
          <button
            v-if="owner"
            class="quiet-button"
            :disabled="busy"
            @click="answerTransfer('cancel')"
          >
            {{ t('withdrawRequest') }}
          </button>
        </div>
      </section>
      <details v-if="owner" class="danger-zone">
        <summary>
          <ChevronRight :size="18" aria-hidden="true" /><span>{{ t('dangerZone') }}</span>
        </summary>
        <div class="danger-zone-content">
          <section v-if="active && !settings.transfer" class="settings-card">
            <h2>{{ t('transferOwnership') }}</h2>
            <p>{{ t('theRecipientMustAlreadyBeAMemberAndExplicitly') }}</p>
            <form @submit.prevent="requestTransfer">
              <fieldset :disabled="busy">
                <label for="transfer-target">{{ t('newOwner') }}</label
                ><select id="transfer-target" v-model="targetEmail" required>
                  <option value="">{{ t('selectAMember') }}</option>
                  <option v-for="member in otherMembers" :key="member.id" :value="member.email">
                    {{ member.email }}
                  </option>
                </select>
                <label for="transfer-email">{{ t('typeTheNewOwnerSEmail') }}</label
                ><input
                  id="transfer-email"
                  v-model="confirmedEmail"
                  type="email"
                  autocomplete="off"
                  required
                />
                <label for="transfer-confirmation">{{
                  t('typeConfirmation', { confirmation: transferPhrase })
                }}</label
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
                    transferConfirmation !== transferPhrase
                  "
                >
                  {{ t('requestTransfer') }}
                </button>
              </fieldset>
            </form>
          </section>
          <section class="settings-card">
            <h2>{{ t('projectLifecycle') }}</h2>
            <button
              v-if="active"
              class="danger-button archive-project-button"
              :disabled="busy"
              @click="lifecycle(true)"
            >
              {{ t('archiveProject') }}
            </button>
            <template v-else>
              <button
                v-if="settings.state === 'ARCHIVED'"
                class="quiet-button"
                :disabled="busy"
                @click="lifecycle(false)"
              >
                {{ t('restoreProject') }}
              </button>
              <p>
                {{
                  t('permanentlyDeleteThisProjectScriptsTemplatesRenderConfigurationsInvitations')
                }}
              </p>
              <form @submit.prevent="deleteProject">
                <fieldset :disabled="busy">
                  <label for="delete-confirmation">{{
                    t('typeNameToConfirmDeletion', { name: settings.name })
                  }}</label
                  ><input
                    id="delete-confirmation"
                    v-model="deleteConfirmation"
                    autocomplete="off"
                    required
                  />
                  <button class="danger-button" :disabled="deleteConfirmation !== settings.name">
                    {{
                      settings.state === 'DELETING'
                        ? t('retryPermanentDeletion')
                        : t('permanentlyDeleteProject')
                    }}
                  </button>
                </fieldset>
              </form>
            </template>
          </section>
        </div>
      </details>
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
.member-identity {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 0;
}
.member-email {
  overflow-wrap: anywhere;
}
.role-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  flex-shrink: 0;
  padding: 3px 8px;
  border: 1px solid currentColor;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  line-height: 1.4;
  white-space: nowrap;
}
.role-owner {
  color: #e5bd52;
  background: #e5bd5218;
}
.role-admin {
  color: #83ce98;
  background: #83ce9818;
}
.role-member {
  color: #a5abb3;
  background: #a5abb318;
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
.danger-zone {
  margin: 20px 0;
  border: 1px solid var(--peel-error);
  border-radius: 12px;
  background: var(--peel-surface);
}
.danger-zone > summary {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 24px;
  color: var(--peel-error);
  font-size: 20px;
  cursor: pointer;
  list-style: none;
}
.danger-zone > summary::-webkit-details-marker {
  display: none;
}
.danger-zone > summary:focus-visible {
  outline: 2px solid var(--peel-error);
  outline-offset: 3px;
  border-radius: 12px;
}
.danger-zone[open] > summary svg {
  transform: rotate(90deg);
}
.danger-zone-content {
  border-top: 1px solid var(--peel-border);
}
.danger-zone-content > .settings-card {
  margin: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
}
.danger-zone-content > .settings-card + .settings-card {
  border-top: 1px solid var(--peel-border);
}
.leave-project-actions {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid var(--peel-border);
}
.danger-button.leave-project-button,
.danger-button.archive-project-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: color-mix(in srgb, var(--peel-error) 12%, transparent);
}
.leave-project-button:hover:not(:disabled),
.archive-project-button:hover:not(:disabled) {
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
