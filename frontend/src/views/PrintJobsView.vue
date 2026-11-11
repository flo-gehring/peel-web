<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'
import { ArrowRight, FileSpreadsheet, Plus, Printer } from '@lucide/vue'
import { useRouter } from 'vue-router'
import { api } from '@/adapter/client'
import type { components } from '@/types/api'
import { useProjectStore } from '@/stores/projects'

type PrintJobSummary = components['schemas']['PrintJobSummary']
type DocumentSummary = components['schemas']['DocumentTemplateSummaryResponse']

const router = useRouter()
const projectStore = useProjectStore()
const printJobs = ref<PrintJobSummary[]>([])
const documents = ref<DocumentSummary[]>([])
const isLoading = ref(true)
const isDialogOpen = ref(false)
const isCreating = ref(false)
const errorMessage = ref('')
const name = ref('')
const documentId = ref('')
const nameInput = ref<HTMLInputElement | null>(null)

function documentName(documentId: string | undefined) {
  return (
    documents.value.find((document) => document.id === documentId)?.name ??
    'Dokument nicht verfügbar'
  )
}

function statusLabel(status: PrintJobSummary['status']) {
  return {
    CREATED: 'Erstellt',
    CALCULATING: 'Wird verarbeitet',
    PRINTING: 'Wird gedruckt',
    COMPLETED: 'Abgeschlossen',
    FAILED: 'Fehlgeschlagen',
  }[status ?? 'CREATED']
}

async function loadPrintJobs() {
  isLoading.value = true
  errorMessage.value = ''
  try {
    const [{ data: printJobData, error: printJobError }, documentData] = await Promise.all([
      api.GET('/api/print-jobs/list'),
      projectStore.loadDocuments(),
    ])
    if (printJobError) throw printJobError
    printJobs.value = printJobData ?? []
    documents.value = documentData
  } catch {
    errorMessage.value = 'Druckaufträge konnten nicht geladen werden.'
  } finally {
    isLoading.value = false
  }
}

async function openCreateDialog() {
  errorMessage.value = ''
  try {
    documents.value = await projectStore.loadDocuments()
  } catch {
    errorMessage.value = 'Dokumente konnten nicht geladen werden.'
    return
  }
  name.value = ''
  documentId.value = ''
  isDialogOpen.value = true
  await nextTick()
  nameInput.value?.focus()
}

async function createPrintJob() {
  if (!name.value.trim() || !documentId.value) return

  isCreating.value = true
  errorMessage.value = ''
  const { data, error } = await api.POST('/api/print-jobs', {
    body: {
      name: name.value.trim(),
      documentId: { id: documentId.value },
    },
  })
  isCreating.value = false

  if (error || !data?.id) {
    errorMessage.value = 'Druckauftrag konnte nicht angelegt werden.'
    return
  }

  await router.push({ name: 'print-job-detail', params: { id: data.id } })
}

function openPrintJob(printJob: PrintJobSummary) {
  const id = printJob.printJobId?.id
  if (id) router.push({ name: 'print-job-detail', params: { id } })
}

onMounted(loadPrintJobs)
</script>

<template>
  <main class="page">
    <div class="page-content">
      <header class="page-header">
        <div class="page-intro">
          <h1>Druckaufträge</h1>
          <p>Aufträge anlegen, Dateien bereitstellen und den Status verfolgen.</p>
        </div>
        <button class="primary-button" type="button" @click="openCreateDialog">
          <Plus :size="18" aria-hidden="true" /> Neuer Auftrag
        </button>
      </header>

      <p v-if="errorMessage && !isDialogOpen" class="error-message" role="alert">
        {{ errorMessage }}
      </p>
      <p v-else-if="isLoading" class="status-message" role="status">
        Druckaufträge werden geladen…
      </p>
      <p v-else-if="printJobs.length === 0" class="status-message">
        Noch keine Druckaufträge vorhanden.
      </p>

      <section v-else class="print-job-list" aria-label="Druckaufträge">
        <button
          v-for="printJob in printJobs"
          :key="printJob.printJobId?.id"
          class="print-job-row"
          type="button"
          @click="openPrintJob(printJob)"
        >
          <Printer class="job-icon" :size="24" aria-hidden="true" />
          <span class="job-info">
            <strong>{{ printJob.name || 'Unbenannter Druckauftrag' }}</strong>
            <span class="document-name">{{ documentName(printJob.documentId?.id) }}</span>
          </span>
          <span class="job-file" :class="{ 'file-missing': !printJob.fileName }">
            <FileSpreadsheet :size="16" aria-hidden="true" />
            {{ printJob.fileName || 'Keine Datei hochgeladen' }}
          </span>
          <span
            class="status-badge"
            :class="`status-${printJob.status?.toLowerCase() ?? 'created'}`"
          >
            {{ statusLabel(printJob.status) }}
          </span>
          <ArrowRight class="open-arrow" :size="20" aria-hidden="true" />
        </button>
      </section>
    </div>

    <div v-if="isDialogOpen" class="dialog-backdrop" @click.self="isDialogOpen = false">
      <form
        class="dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="create-print-job-title"
        @submit.prevent="createPrintJob"
        @keydown.esc.prevent="isDialogOpen = false"
      >
        <h2 id="create-print-job-title">Druckauftrag anlegen</h2>
        <p v-if="errorMessage" class="dialog-error" role="alert">{{ errorMessage }}</p>
        <label>
          Name
          <input ref="nameInput" v-model="name" required placeholder="Name des Druckauftrags" />
        </label>
        <label>
          Dokument
          <select v-model="documentId" required>
            <option disabled value="">Dokument auswählen</option>
            <option v-for="document in documents" :key="document.id" :value="document.id">
              {{ document.name || 'Unbenanntes Dokument' }}
            </option>
          </select>
        </label>
        <p v-if="documents.length === 0" class="hint">Es sind keine Dokumente verfügbar.</p>
        <div class="dialog-actions">
          <button type="button" @click="isDialogOpen = false">Abbrechen</button>
          <button
            class="primary-button"
            type="submit"
            :disabled="isCreating || documents.length === 0"
          >
            {{ isCreating ? 'Wird angelegt...' : 'Anlegen' }}
          </button>
        </div>
      </form>
    </div>
  </main>
</template>

<style scoped>
.page {
  min-height: calc(100dvh - var(--app-header-height));
  padding: 56px 32px;
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
  gap: 24px;
  margin-bottom: 28px;
}
.page-intro p {
  color: var(--peel-muted);
  margin-top: 8px;
}
h1 {
  font-size: clamp(28px, 4vw, 40px);
  font-weight: 700;
  letter-spacing: -0.04em;
  line-height: 1.2;
}
.primary-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 15px;
}
.print-job-list {
  border: 1px solid var(--peel-border);
  border-radius: 12px;
  overflow: hidden;
  background: var(--peel-surface);
}
.print-job-row {
  display: grid;
  grid-template-columns: 28px minmax(0, 1.2fr) minmax(0, 1fr) auto 20px;
  align-items: center;
  gap: 20px;
  min-height: 88px;
  width: 100%;
  padding: 18px 28px;
  border: 0;
  border-bottom: 1px solid var(--peel-border);
  border-radius: 0;
  background: transparent;
  color: inherit;
  text-align: left;
}
.print-job-row:last-child {
  border-bottom: 0;
}
.print-job-row:hover,
.print-job-row:focus-visible {
  background: var(--peel-raised);
  filter: none;
}
.print-job-row strong {
  font-size: 16px;
  font-weight: 600;
}
.job-info {
  display: grid;
  gap: 4px;
  min-width: 0;
  overflow-wrap: anywhere;
}
.document-name {
  font-size: 13px;
  color: var(--peel-muted);
}
.job-file {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  font-size: 13px;
  color: var(--peel-muted);
  overflow-wrap: anywhere;
}
.job-file svg {
  flex-shrink: 0;
}
.job-icon {
  color: var(--peel-accent);
}
.open-arrow {
  color: var(--peel-muted);
}
.print-job-row:hover .open-arrow {
  color: var(--peel-accent);
}
.file-missing {
  color: #e7b76b;
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
  padding: 32px 28px;
  border: 1px solid var(--peel-border);
  border-radius: 12px;
  background: var(--peel-surface);
  color: var(--peel-muted);
}
.error-message {
  border-color: #c97777;
  color: var(--peel-error);
}
.dialog-backdrop {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: grid;
  padding: 24px;
  place-items: center;
  background: rgb(0 0 0 / 65%);
}
.dialog {
  display: grid;
  width: min(440px, 100%);
  max-height: calc(100dvh - 48px);
  overflow-y: auto;
  gap: 18px;
  padding: 24px;
  border: 1px solid var(--peel-border);
  border-radius: 16px;
  background: var(--peel-surface);
}
.dialog h2 {
  font-size: 22px;
  font-weight: 600;
}
.dialog-error {
  color: var(--peel-error);
}
.dialog label {
  display: grid;
  gap: 6px;
  color: var(--peel-text);
}
input,
select {
  width: 100%;
}
.hint {
  color: #e7b76b;
}
.dialog-actions {
  display: flex;
  justify-content: end;
  gap: 10px;
}
.dialog-actions button:not(.primary-button) {
  background: transparent;
}
@media (max-width: 680px) {
  .page {
    padding: 28px 16px;
  }
  .page-header {
    align-items: start;
    flex-direction: column;
    gap: 16px;
  }
  .print-job-row {
    grid-template-columns: 24px minmax(0, 1fr) 20px;
    gap: 8px 14px;
    padding: 18px;
  }
  .job-icon {
    grid-column: 1;
    grid-row: 1;
  }
  .job-info {
    grid-column: 2;
    grid-row: 1;
  }
  .job-file {
    grid-column: 2;
    grid-row: 2;
  }
  .status-badge {
    grid-column: 2;
    grid-row: 3;
  }
  .open-arrow {
    grid-column: 3;
    grid-row: 1;
  }
}
</style>
