<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '@/adapter/client'
import type { components } from '@/types/api'

type PrintJobSummary = components['schemas']['PrintJobSummary']
type DocumentSummary = components['schemas']['DocumentSummaryResponse']

const router = useRouter()
const printJobs = ref<PrintJobSummary[]>([])
const documents = ref<DocumentSummary[]>([])
const isLoading = ref(true)
const isDialogOpen = ref(false)
const isCreating = ref(false)
const errorMessage = ref('')
const name = ref('')
const documentId = ref('')

async function loadPrintJobs() {
  isLoading.value = true
  errorMessage.value = ''
  const { data, error } = await api.GET('/print-jobs/list')
  isLoading.value = false

  if (error) {
    errorMessage.value = 'Druckaufträge konnten nicht geladen werden.'
    return
  }

  printJobs.value = data ?? []
}

async function openCreateDialog() {
  errorMessage.value = ''
  const { data, error } = await api.GET('/documents')
  if (error) {
    errorMessage.value = 'Dokumente konnten nicht geladen werden.'
    return
  }

  documents.value = data ?? []
  name.value = ''
  documentId.value = ''
  isDialogOpen.value = true
}

async function createPrintJob() {
  if (!name.value.trim() || !documentId.value) return

  isCreating.value = true
  errorMessage.value = ''
  const { data, error } = await api.PUT('/print-jobs/init', {
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
    <header class="page-header">
      <div>
        <RouterLink class="back-link" to="/">Peel</RouterLink>
        <h1>Druckaufträge</h1>
      </div>
      <button class="primary-button" type="button" @click="openCreateDialog">+ Neuer Auftrag</button>
    </header>

    <p v-if="errorMessage" class="error-message" role="alert">{{ errorMessage }}</p>
    <p v-else-if="isLoading" class="status-message">Druckaufträge werden geladen...</p>
    <p v-else-if="printJobs.length === 0" class="status-message">Noch keine Druckaufträge vorhanden.</p>

    <section v-else class="print-job-list" aria-label="Druckaufträge">
      <button
        v-for="printJob in printJobs"
        :key="printJob.printJobId?.id"
        class="print-job-row"
        type="button"
        @click="openPrintJob(printJob)"
      >
        <strong>{{ printJob.name || 'Unbenannter Druckauftrag' }}</strong>
        <span>{{ printJob.documentName || 'Dokument nicht verfügbar' }}</span>
        <span :class="printJob.fileName ? 'file-ready' : 'file-missing'">
          {{ printJob.fileName || 'Keine Datei hochgeladen' }}
        </span>
      </button>
    </section>

    <div v-if="isDialogOpen" class="dialog-backdrop" @click.self="isDialogOpen = false">
      <form class="dialog" @submit.prevent="createPrintJob">
        <h2>Druckauftrag anlegen</h2>
        <label>
          Name
          <input v-model="name" required autofocus />
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
          <button class="primary-button" type="submit" :disabled="isCreating || documents.length === 0">
            {{ isCreating ? 'Wird angelegt...' : 'Anlegen' }}
          </button>
        </div>
      </form>
    </div>
  </main>
</template>

<style scoped>
.page { min-height: 100vh; padding: 40px max(24px, calc((100vw - 980px) / 2)); background: #17191d; color: #edf1f5; }
.page-header { display: flex; align-items: end; justify-content: space-between; gap: 24px; margin-bottom: 38px; }
.back-link { color: #89c6ff; font-family: ui-monospace, monospace; font-size: 0.8rem; text-decoration: none; text-transform: uppercase; }
h1 { margin-top: 5px; font-size: clamp(2.2rem, 6vw, 4rem); letter-spacing: -0.055em; }
button, input, select { font: inherit; }
button { cursor: pointer; }
.primary-button { border: 1px solid #89c6ff; padding: 10px 15px; background: #89c6ff; color: #102231; font-weight: 700; }
.primary-button:disabled { cursor: wait; opacity: 0.55; }
.print-job-list { border-top: 1px solid #3a4049; }
.print-job-row { display: grid; grid-template-columns: 1.2fr 1fr 1fr; width: 100%; padding: 18px 12px; border: 0; border-bottom: 1px solid #3a4049; background: transparent; color: inherit; text-align: left; }
.print-job-row:hover, .print-job-row:focus-visible { background: #252a31; }
.print-job-row span { color: #afb8c4; }
.file-ready { color: #9ee3b1 !important; }.file-missing { color: #e7b76b !important; }
.status-message, .error-message { padding: 22px; border: 1px solid #3a4049; color: #afb8c4; }.error-message { border-color: #c97777; color: #ffabab; }
.dialog-backdrop { position: fixed; inset: 0; display: grid; padding: 24px; place-items: center; background: rgb(0 0 0 / 65%); }
.dialog { display: grid; width: min(440px, 100%); gap: 18px; padding: 24px; border: 1px solid #4d5865; background: #22272e; }
.dialog h2 { font-size: 1.4rem; }.dialog label { display: grid; gap: 6px; color: #c9d1da; }
input, select { width: 100%; border: 1px solid #4d5865; padding: 9px; background: #17191d; color: inherit; }.hint { color: #e7b76b; }
.dialog-actions { display: flex; justify-content: end; gap: 10px; }.dialog-actions button:not(.primary-button) { border: 1px solid #4d5865; padding: 10px 15px; background: transparent; color: inherit; }
@media (max-width: 650px) { .page-header { align-items: start; flex-direction: column; }.print-job-row { grid-template-columns: 1fr; gap: 4px; } }
</style>
