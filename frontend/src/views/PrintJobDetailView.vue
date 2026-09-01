<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '@/adapter/client'
import type { components } from '@/types/api'

type PrintJobSummary = components['schemas']['PrintJobSummary']

const route = useRoute()
const printJob = ref<PrintJobSummary>()
const isLoading = ref(true)
const isUploading = ref(false)
const errorMessage = ref('')
const printJobId = computed(() => String(route.params.id))

async function loadPrintJob() {
  isLoading.value = true
  errorMessage.value = ''
  const { data, error } = await api.GET('/print-jobs/list')
  isLoading.value = false
  if (error) {
    errorMessage.value = 'Druckauftrag konnte nicht geladen werden.'
    return
  }

  printJob.value = data?.find((job) => job.printJobId?.id === printJobId.value)
  if (!printJob.value) errorMessage.value = 'Druckauftrag wurde nicht gefunden.'
}

async function uploadFile(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return

  isUploading.value = true
  errorMessage.value = ''
  const { error } = await api.PUT('/print-jobs/{id}/data', {
    params: { path: { id: printJobId.value } },
    body: { file: file as unknown as string },
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
      <p class="document-name">{{ printJob.documentName || 'Dokument nicht verfügbar' }}</p>
      <div class="file-area">
        <template v-if="printJob.fileName">
          <p>Datei</p>
          <button class="file-link" type="button" @click="downloadFile">{{ printJob.fileName }}</button>
        </template>
        <template v-else>
          <p>Noch keine Datei hochgeladen.</p>
          <label class="upload-button">
            {{ isUploading ? 'Wird hochgeladen...' : 'Datei hochladen' }}
            <input type="file" :disabled="isUploading" @change="uploadFile" />
          </label>
        </template>
      </div>
    </section>
  </main>
</template>

<style scoped>
.page { min-height: 100vh; padding: 40px max(24px, calc((100vw - 760px) / 2)); background: #17191d; color: #edf1f5; }.back-link, .file-link { color: #89c6ff; font-family: ui-monospace, monospace; font-size: 0.8rem; text-decoration: none; text-transform: uppercase; }.detail-card { margin-top: 52px; padding: 30px; border: 1px solid #3a4049; background: #20242a; }.eyebrow { color: #89c6ff; font-family: ui-monospace, monospace; font-size: 0.75rem; letter-spacing: 0.12em; text-transform: uppercase; }h1 { margin: 5px 0 2px; font-size: clamp(2rem, 6vw, 3.6rem); letter-spacing: -0.055em; }.document-name { color: #afb8c4; }.file-area { display: grid; margin-top: 38px; gap: 10px; padding-top: 20px; border-top: 1px solid #3a4049; }.file-link { width: fit-content; border: 0; padding: 0; background: transparent; text-align: left; }.file-link:hover { text-decoration: underline; }.upload-button { width: fit-content; border: 1px solid #89c6ff; padding: 10px 15px; background: #89c6ff; color: #102231; cursor: pointer; font-weight: 700; }.upload-button input { position: absolute; width: 1px; height: 1px; opacity: 0; }.status-message, .error-message { margin-top: 50px; padding: 22px; border: 1px solid #3a4049; color: #afb8c4; }.error-message { border-color: #c97777; color: #ffabab; }
</style>
