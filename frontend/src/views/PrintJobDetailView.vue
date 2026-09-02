<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '@/adapter/client'
import type { components } from '@/types/api'

type PrintJobSummary = components['schemas']['PrintJobSummary']
type DocumentSummary = components['schemas']['DocumentSummaryResponse']

const route = useRoute()
const printJob = ref<PrintJobSummary>()
const documents = ref<DocumentSummary[]>([])
const isLoading = ref(true)
const isUploading = ref(false)
const isStarting = ref(false)
const errorMessage = ref('')
const printJobId = computed(() => String(route.params.id))
const documentName = computed(() => {
  const documentId = printJob.value?.documentId?.id
  return documents.value.find((document) => document.id === documentId)?.name ?? 'Dokument nicht verfügbar'
})
const canStart = computed(() => printJob.value?.status === 'CREATED' && Boolean(printJob.value.fileName))

function statusLabel(status: PrintJobSummary['status']) {
  return {
    CREATED: 'Erstellt',
    CALCULATING: 'Wird verarbeitet',
    PRINTING: 'Wird gedruckt',
    COMPLETED: 'Abgeschlossen',
    FAILED: 'Fehlgeschlagen',
  }[status ?? 'CREATED']
}

async function loadPrintJob() {
  isLoading.value = true
  errorMessage.value = ''
  const [{ data: printJobData, error: printJobError }, { data: documentData, error: documentError }] = await Promise.all([
    api.GET('/print-jobs/list'),
    api.GET('/documents'),
  ])
  isLoading.value = false
  if (printJobError || documentError) {
    errorMessage.value = 'Druckauftrag konnte nicht geladen werden.'
    return
  }

  documents.value = documentData ?? []
  printJob.value = printJobData?.find((job) => job.printJobId?.id === printJobId.value)
  if (!printJob.value) errorMessage.value = 'Druckauftrag wurde nicht gefunden.'
}

async function startPrintJob() {
  isStarting.value = true
  errorMessage.value = ''
  const { data, error } = await api.POST('/print-jobs/{id}/run', {
    params: { path: { id: printJobId.value } },
  })
  isStarting.value = false

  if (error || !data || !printJob.value) {
    errorMessage.value = 'Druckauftrag konnte nicht gestartet werden.'
    return
  }

  printJob.value = { ...printJob.value, status: data }
}

async function uploadFile(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  if (!file.name.toLowerCase().endsWith('.csv')) {
    errorMessage.value = 'Bitte eine CSV-Datei auswählen.'
    return
  }

  isUploading.value = true
  errorMessage.value = ''
  const csvFile = new File([file], file.name, { type: 'text/csv' })
  const { error } = await api.PUT('/print-jobs/{id}/data', {
    params: { path: { id: printJobId.value } },
    body: { file: csvFile as unknown as string },
    bodySerializer: (body) => {
      const formData = new FormData()
      formData.append('file', body.file as unknown as File)
      return formData
    },
  })
  isUploading.value = false

  if (error) {
    errorMessage.value = 'Datei konnte nicht hochgeladen werden.'
    return
  }

  await loadPrintJob()
}

async function downloadFile() {
  if (!printJob.value?.fileName) return

  errorMessage.value = ''
  const { data, error } = await api.GET('/print-jobs/{id}/data', {
    params: { path: { id: printJobId.value } },
    parseAs: 'blob',
  })
  if (error || !data) {
    errorMessage.value = 'Datei konnte nicht heruntergeladen werden.'
    return
  }

  const downloadUrl = URL.createObjectURL(data as unknown as Blob)
  const link = document.createElement('a')
  link.href = downloadUrl
  link.download = printJob.value.fileName
  link.click()
  URL.revokeObjectURL(downloadUrl)
}

onMounted(loadPrintJob)
</script>

<template>
  <main class="page">
    <RouterLink class="back-link" to="/print-jobs">Zurück zu Druckaufträgen</RouterLink>
    <p v-if="errorMessage" class="error-message" role="alert">{{ errorMessage }}</p>
    <p v-else-if="isLoading" class="status-message">Druckauftrag wird geladen...</p>
    <section v-else-if="printJob" class="detail-card">
      <p class="eyebrow">Druckauftrag</p>
      <h1>{{ printJob.name || 'Unbenannter Druckauftrag' }}</h1>
      <p class="document-name">{{ documentName }}</p>
      <p class="status-badge" :class="`status-${printJob.status?.toLowerCase() ?? 'created'}`">
        {{ statusLabel(printJob.status) }}
      </p>
      <div class="file-area">
        <template v-if="printJob.fileName">
          <p>Datei</p>
          <button class="file-link" type="button" @click="downloadFile">{{ printJob.fileName }}</button>
        </template>
        <template v-else>
          <p>Noch keine Datei hochgeladen.</p>
          <label class="upload-button">
            {{ isUploading ? 'Wird hochgeladen...' : 'Datei hochladen' }}
            <input type="file" accept=".csv,text/csv" :disabled="isUploading" @change="uploadFile" />
          </label>
        </template>
      </div>
      <button v-if="canStart" class="start-button" type="button" :disabled="isStarting" @click="startPrintJob">
        {{ isStarting ? 'Wird gestartet...' : 'Druckauftrag starten' }}
      </button>
    </section>
  </main>
</template>

<style scoped>
.page { min-height: 100vh; padding: 40px max(24px, calc((100vw - 760px) / 2)); background: #17191d; color: #edf1f5; }.back-link, .file-link { color: #89c6ff; font-family: ui-monospace, monospace; font-size: 0.8rem; text-decoration: none; text-transform: uppercase; }.detail-card { margin-top: 52px; padding: 30px; border: 1px solid #3a4049; background: #20242a; }.eyebrow { color: #89c6ff; font-family: ui-monospace, monospace; font-size: 0.75rem; letter-spacing: 0.12em; text-transform: uppercase; }h1 { margin: 5px 0 2px; font-size: clamp(2rem, 6vw, 3.6rem); letter-spacing: -0.055em; }.document-name { color: #afb8c4; }.status-badge { width: fit-content; margin-top: 16px; padding: 2px 7px; border: 1px solid #4d5865; color: #c9d1da; font-family: ui-monospace, monospace; font-size: 0.72rem; text-transform: uppercase; }.status-completed { border-color: #4f9d67; color: #9ee3b1; }.status-failed { border-color: #c97777; color: #ffabab; }.status-calculating, .status-printing { border-color: #b5894c; color: #e7b76b; }.file-area { display: grid; margin-top: 38px; gap: 10px; padding-top: 20px; border-top: 1px solid #3a4049; }.file-link { width: fit-content; border: 0; padding: 0; background: transparent; text-align: left; }.file-link:hover { text-decoration: underline; }.upload-button, .start-button { width: fit-content; border: 1px solid #89c6ff; padding: 10px 15px; background: #89c6ff; color: #102231; cursor: pointer; font-weight: 700; }.upload-button input { position: absolute; width: 1px; height: 1px; opacity: 0; }.start-button { margin-top: 22px; }.start-button:disabled { cursor: wait; opacity: 0.55; }.status-message, .error-message { margin-top: 50px; padding: 22px; border: 1px solid #3a4049; color: #afb8c4; }.error-message { border-color: #c97777; color: #ffabab; }
</style>
