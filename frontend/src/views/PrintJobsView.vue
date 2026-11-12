<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ChevronRight, FileText, Plus, RefreshCw } from '@lucide/vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '@/adapter/client'
import { apiErrorMessage } from '@/adapter/apiError'
import type { components } from '@/types/api'
import { useProjectStore } from '@/stores/projects'
import CreatePrintJobDialog from '@/components/CreatePrintJobDialog.vue'

type Overview = components['schemas']['ProjectPrintJobs']
type Group = components['schemas']['DocumentJobs']
const route = useRoute(),
  router = useRouter()
const projects = useProjectStore()
const projectId = computed(() => String(route.params.projectId))
const overview = ref<Overview | null>(null)
const isLoading = ref(false)
const errorMessage = ref('')
const selected = ref<Group | null>(null)
const readOnly = computed(() => overview.value?.state !== 'ACTIVE')
const groups = computed(() => [
  ...(overview.value?.documents ?? []).map((group) => ({ ...group, deleted: false })),
  ...(overview.value?.deletedDocuments ?? []).map((group) => ({ ...group, deleted: true })),
])
function statusLabel(status: string | undefined): string {
  return (
    (
      {
        CREATED: 'Erstellt',
        CALCULATING: 'Wird verarbeitet',
        PRINTING: 'Wird gedruckt',
        COMPLETED: 'Abgeschlossen',
        FAILED: 'Fehlgeschlagen',
      } as Record<string, string>
    )[status ?? 'CREATED'] ??
    status ??
    ''
  )
}
async function load(): Promise<void> {
  const id = projectId.value
  isLoading.value = true
  errorMessage.value = ''
  try {
    const [{ data, error }] = await Promise.all([
      api.GET('/api/projects/{projectId}/print-jobs', { params: { path: { projectId: id } } }),
      projects.loadProject(id),
    ])
    if (error || !data)
      throw new Error(apiErrorMessage(error, 'Druckaufträge konnten nicht geladen werden.'))
    if (projectId.value === id) overview.value = data
  } catch (error) {
    if (projectId.value === id)
      errorMessage.value =
        error instanceof Error ? error.message : 'Der Server konnte nicht erreicht werden.'
  } finally {
    if (projectId.value === id) isLoading.value = false
  }
}
function created(id: string): void {
  selected.value = null
  void router.push({ name: 'print-job-detail', params: { projectId: projectId.value, id } })
}
watch(
  projectId,
  () => {
    overview.value = null
    selected.value = null
    void load()
  },
  { immediate: true },
)
</script>
<template>
  <main class="page">
    <div class="page-content">
      <header class="page-header">
        <div class="page-intro">
          <h1>Druckaufträge</h1>
          <p>{{ overview?.projectName }} · Nach Dokumentvorlage gruppiert</p>
        </div>
        <button
          title="Druckaufträge aktualisieren"
          aria-label="Druckaufträge aktualisieren"
          :disabled="isLoading"
          @click="load"
        >
          <RefreshCw :size="18" />
        </button>
      </header>
      <p v-if="overview && readOnly" class="status-message">
        Archiviertes Projekt: Aufträge und Ergebnisse bleiben lesbar. Neue Aufträge, Uploads und
        Starts sind gesperrt.
      </p>
      <p v-if="errorMessage" class="error-message" role="alert">
        {{ errorMessage }} <button @click="load">Erneut versuchen</button>
      </p>
      <p v-if="isLoading" class="status-message" role="status">Druckaufträge werden geladen…</p>
      <p v-if="overview && !groups.length" class="status-message">
        Noch keine Dokumentvorlagen vorhanden. Lege im Workspace eine Dokumentvorlage an.
      </p>
      <template v-for="(group, index) in groups" :key="group.documentId">
        <h2 v-if="group.deleted && !groups[index - 1]?.deleted" class="deleted-heading">
          Gelöschte Dokumentvorlagen
        </h2>
        <section class="document-jobs" :class="{ deleted: group.deleted }">
          <details open>
            <summary>
              <ChevronRight :size="16" class="chevron" aria-hidden="true" /><FileText
                :size="18"
                aria-hidden="true"
              /><strong>{{ group.name }}</strong
              ><span>{{ group.jobs?.length ?? 0 }} Aufträge</span>
            </summary>
            <div class="print-job-list">
              <p v-if="!group.jobs?.length" class="empty-group">Noch keine Druckaufträge.</p>
              <RouterLink
                v-for="job in group.jobs"
                :key="job.printJobId?.id"
                class="print-job-row"
                :to="{ name: 'print-job-detail', params: { projectId, id: job.printJobId?.id } }"
              >
                <span class="job-info"
                  ><strong>{{ job.name || 'Unbenannter Druckauftrag' }}</strong
                  ><time>{{
                    job.createdAt
                      ? new Date(job.createdAt).toLocaleString()
                      : 'Zeitpunkt nicht erfasst'
                  }}</time></span
                >
                <span class="job-file">{{ job.fileName || 'Keine CSV-Datei' }}</span>
                <span>{{ job.fileIds?.length ?? 0 }} PDFs</span>
                <span class="status-badge" :class="`status-${job.status?.toLowerCase()}`">{{
                  statusLabel(job.status)
                }}</span>
              </RouterLink>
            </div>
          </details>
          <button
            v-if="!group.deleted"
            class="create-job"
            :disabled="readOnly"
            :title="`Druckauftrag für ${group.name} anlegen`"
            @click="selected = group"
          >
            <Plus :size="16" aria-hidden="true" /> Neuer Druckauftrag
          </button>
        </section>
      </template>
    </div>
    <CreatePrintJobDialog
      v-if="selected?.documentId"
      :project-id="projectId"
      :document-id="selected.documentId"
      :document-name="selected.name ?? ''"
      @cancel="selected = null"
      @created="created"
    />
  </main>
</template>
<style scoped>
.page {
  min-height: calc(100dvh - var(--app-header-height));
  padding: 40px 24px;
  background: var(--peel-background);
  color: var(--peel-text);
}
.page-content {
  max-width: 1120px;
  margin: 0 auto;
}
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 28px;
}
h1 {
  font-size: clamp(28px, 4vw, 40px);
  line-height: 1.2;
}
.page-intro p {
  margin-top: 8px;
  color: var(--peel-muted);
}
.document-jobs {
  position: relative;
  margin: 16px 0;
  border: 1px solid var(--peel-border);
  border-radius: 12px;
  background: var(--peel-surface);
}
.document-jobs summary {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 18px 220px 18px 16px;
  cursor: pointer;
  list-style: none;
}
.document-jobs summary::-webkit-details-marker {
  display: none;
}
.document-jobs summary strong {
  overflow-wrap: anywhere;
}
.document-jobs summary span {
  color: var(--peel-muted);
  font-size: 12px;
  white-space: nowrap;
}
.document-jobs summary svg {
  flex-shrink: 0;
}
.document-jobs details[open] > summary .chevron {
  transform: rotate(90deg);
}
.print-job-list {
  border-top: 1px solid var(--peel-border);
  border-radius: 0 0 12px 12px;
  overflow: hidden;
}
.print-job-row {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 16px;
  padding: 18px 24px;
  border-bottom: 1px solid var(--peel-border);
  color: inherit;
  text-decoration: none;
}
.print-job-row:last-child {
  border-bottom: 0;
}
.print-job-row:hover,
.print-job-row:focus-visible {
  background: var(--peel-raised);
}
.job-info {
  display: grid;
  gap: 4px;
  min-width: 0;
  overflow-wrap: anywhere;
}
.job-file,
time {
  color: var(--peel-muted);
  font-size: 12px;
  overflow-wrap: anywhere;
}
.status-badge {
  width: fit-content;
  padding: 4px 10px;
  border: 1px solid var(--peel-border);
  border-radius: 20px;
  color: var(--peel-muted);
  font-size: 12px;
  white-space: nowrap;
}
.status-completed {
  border-color: #4f9d67;
  color: #9ee3b1;
  background: #9ee3b10d;
}
.status-failed {
  border-color: #c97777;
  color: var(--peel-error);
  background: #ffaca00d;
}
.status-calculating,
.status-printing {
  border-color: #b5894c;
  color: #e7b76b;
  background: #e7b76b0d;
}
.status-message,
.error-message {
  padding: 24px;
  border: 1px solid var(--peel-border);
  border-radius: 12px;
  background: var(--peel-surface);
  color: var(--peel-muted);
}
.error-message {
  color: var(--peel-error);
}
.create-job {
  position: absolute;
  top: 12px;
  right: 12px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
}
.empty-group {
  padding: 20px;
  color: var(--peel-muted);
}
.deleted-heading {
  margin-top: 36px;
  font-size: 20px;
}
.deleted summary {
  padding-right: 16px;
  color: var(--peel-muted);
}
@media (max-width: 680px) {
  .page {
    padding: 28px 16px;
  }
  .document-jobs summary {
    padding-right: 16px;
    padding-bottom: 52px;
    flex-wrap: wrap;
  }
  .create-job {
    top: 47px;
    left: 16px;
    right: auto;
  }
  .deleted summary {
    padding-bottom: 18px;
  }
  .print-job-row {
    display: flex;
    flex-wrap: wrap;
  }
  .job-info {
    flex: 1 1 100%;
  }
}
</style>
