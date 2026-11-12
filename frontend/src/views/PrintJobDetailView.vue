<script setup lang="ts">
import { formatDateTime, t } from '@/i18n'
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import {
  ArrowLeft,
  ChevronRight,
  Download,
  FileSpreadsheet,
  FileText,
  Printer,
  RefreshCw,
  Upload,
} from '@lucide/vue'
import { apiErrorMessage } from '@/adapter/apiError'
import { api } from '@/adapter/client'
import type { components } from '@/types/api'
import { useProjectStore } from '@/stores/projects'

type PrintJobSummary = components['schemas']['PrintJobSummary']
type Details = components['schemas']['ProjectPrintJobDetails']

const route = useRoute()
const projectStore = useProjectStore()
const printJob = ref<PrintJobSummary>()
const details = ref<Details | null>(null)
const projectId = computed(() => String(route.params.projectId))
const isLoading = ref(true)
const isUploading = ref(false)
const isStarting = ref(false)
const errorMessage = ref('')
const printJobId = computed(() => String(route.params.id))
const readOnly = computed(() => details.value?.projectState !== 'ACTIVE')
const documentAvailable = computed(() => !!details.value?.currentDocumentName)
const documentName = computed(
  () =>
    details.value?.currentDocumentName ?? printJob.value?.documentName ?? t('documentUnavailable'),
)
const canStart = computed(
  () =>
    !readOnly.value &&
    documentAvailable.value &&
    printJob.value?.status === 'CREATED' &&
    !!printJob.value.fileName,
)

function statusLabel(status: PrintJobSummary['status']) {
  return {
    CREATED: t('created'),
    CALCULATING: t('processing'),
    PRINTING: t('printing'),
    COMPLETED: t('completed'),
    FAILED: t('failed'),
  }[status ?? 'CREATED']
}

async function loadPrintJob() {
  const requestedProject = projectId.value,
    requestedId = printJobId.value
  const isCurrent = () => projectId.value === requestedProject && printJobId.value === requestedId
  isLoading.value = true
  errorMessage.value = ''
  try {
    const [{ data, error }] = await Promise.all([
      api.GET('/api/projects/{projectId}/print-jobs/{id}', {
        params: { path: { projectId: requestedProject, id: requestedId } },
      }),
      projectStore.loadProject(requestedProject),
    ])
    if (error || !data?.job) throw new Error(apiErrorMessage(error, t('printJobNotFound')))
    if (!isCurrent()) return
    details.value = data
    printJob.value = data.job
  } catch (error) {
    if (isCurrent())
      errorMessage.value = error instanceof Error ? error.message : t('thePrintJobCouldNotBeLoaded')
  } finally {
    if (isCurrent()) isLoading.value = false
  }
}

async function startPrintJob() {
  if (!canStart.value || isStarting.value) return
  const requestedId = printJobId.value
  isStarting.value = true
  errorMessage.value = ''
  try {
    const { data, error } = await api.POST('/api/print-jobs/{id}/run', {
      params: { path: { id: printJobId.value } },
    })
    if (error || !data || !printJob.value)
      throw new Error(apiErrorMessage(error, t('thePrintJobCouldNotBeStarted')))
    if (printJobId.value === requestedId) printJob.value = { ...printJob.value, status: data }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t('theServerCouldNotBeReached')
  } finally {
    isStarting.value = false
  }
}

async function uploadFile(event: Event) {
  if (readOnly.value || !documentAvailable.value || isUploading.value) return
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  if (!file.name.toLowerCase().endsWith('.csv')) {
    errorMessage.value = t('pleaseSelectACsvFile')
    return
  }

  isUploading.value = true
  errorMessage.value = ''
  const csvFile = new File([file], file.name, { type: 'text/csv' })
  try {
    const { error } = await api.PUT('/api/print-jobs/{id}/data', {
      params: { path: { id: printJobId.value } },
      body: { file: csvFile as unknown as string },
      bodySerializer: (body) => {
        if (!body?.file) throw new Error(t('aCsvFileIsRequired'))
        const formData = new FormData()
        formData.append('file', body.file as unknown as File)
        return formData
      },
    })

    if (error) throw new Error(apiErrorMessage(error, t('theFileCouldNotBeUploaded')))

    await loadPrintJob()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t('theServerCouldNotBeReached')
  } finally {
    isUploading.value = false
  }
}

async function downloadFile() {
  if (!printJob.value?.fileName) return

  await download('/api/print-jobs/{id}/data', printJob.value.fileName)
}

async function downloadResultFile(fileId: string) {
  await download('/api/print-jobs/{id}/file/{fileId}', fileId, fileId)
}

async function download(
  path: '/api/print-jobs/{id}/data' | '/api/print-jobs/{id}/file/{fileId}',
  fileName: string,
  fileId?: string,
) {
  errorMessage.value = ''
  const response =
    path === '/api/print-jobs/{id}/data'
      ? await api.GET(path, { params: { path: { id: printJobId.value } }, parseAs: 'blob' })
      : await api.GET(path, {
          params: { path: { id: printJobId.value, fileId: fileId! } },
          parseAs: 'blob',
        })
  const { data, error } = response
  if (error || !data) {
    errorMessage.value = t('theFileCouldNotBeDownloaded')
    return
  }

  const downloadUrl = URL.createObjectURL(data as unknown as Blob)
  const link = document.createElement('a')
  link.href = downloadUrl
  link.download = fileName
  link.click()
  URL.revokeObjectURL(downloadUrl)
}

watch(
  () => [projectId.value, printJobId.value],
  () => {
    details.value = null
    printJob.value = undefined
    void loadPrintJob()
  },
  { immediate: true },
)
</script>

<template>
  <main class="page">
    <div class="page-content">
      <RouterLink class="back-link" :to="`/projects/${projectId}/print-jobs`"
        ><ArrowLeft :size="16" aria-hidden="true" />{{ t('backToPrintJobs') }}</RouterLink
      >
      <header class="page-header">
        <div class="page-intro">
          <p class="eyebrow">{{ t('printJob') }}</p>
          <h1>{{ printJob?.name || t('printJob') }}</h1>
          <p v-if="details" class="document-name">{{ details.projectName }} / {{ documentName }}</p>
        </div>
        <div class="header-actions">
          <span
            v-if="printJob"
            class="status-badge"
            :class="`status-${printJob.status?.toLowerCase() ?? 'created'}`"
            ><span class="status-dot" aria-hidden="true"></span
            >{{ statusLabel(printJob.status) }}</span
          >
          <button
            class="icon-button"
            type="button"
            :title="t('refreshStatus')"
            :aria-label="t('refreshStatus')"
            :disabled="isLoading"
            @click="loadPrintJob"
          >
            <RefreshCw :size="18" aria-hidden="true" />
          </button>
          <button
            v-if="canStart"
            class="primary-button start-button"
            type="button"
            :disabled="isStarting"
            @click="startPrintJob"
          >
            <Printer :size="17" aria-hidden="true" />{{
              isStarting ? t('starting') : t('startPrintJob')
            }}
          </button>
        </div>
      </header>
      <p v-if="errorMessage" class="error-message" role="alert">{{ errorMessage }}</p>
      <p v-if="isLoading && !printJob" class="status-message" role="status">
        {{ t('loadingPrintJob') }}
      </p>
      <template v-if="printJob">
        <p v-if="!documentAvailable" class="notice deleted-notice">
          {{ t('thisDocumentTemplateWasDeletedResultsRemainAvailableThe') }}
        </p>
        <p v-if="readOnly" class="notice">
          {{ t('archivedProjectUploadsAndJobStartsAreDisabled') }}
        </p>
        <section class="detail-card metadata-card" :aria-label="t('jobDetails')">
          <dl>
            <div>
              <dt>{{ t('documentTemplate2') }}</dt>
              <dd>{{ documentName }}</dd>
            </div>
            <div>
              <dt>{{ t('created') }}</dt>
              <dd>
                {{ printJob.createdAt ? formatDateTime(printJob.createdAt) : t('timeNotRecorded') }}
              </dd>
            </div>
            <div>
              <dt>{{ t('templateWhenCreated') }}</dt>
              <dd>{{ printJob.documentName || t('nameNotRecorded') }}</dd>
            </div>
          </dl>
        </section>
        <div class="file-grid">
          <section class="detail-card file-area" aria-labelledby="input-heading">
            <h2 id="input-heading" class="card-heading">
              <FileSpreadsheet :size="20" aria-hidden="true" />{{ t('inputFile') }}
            </h2>
            <div class="card-body">
              <template v-if="printJob.fileName">
                <button
                  class="file-link"
                  type="button"
                  :title="printJob.fileName"
                  @click="downloadFile"
                >
                  <FileSpreadsheet :size="18" aria-hidden="true" /><span class="filename">{{
                    printJob.fileName
                  }}</span
                  ><Download :size="16" aria-hidden="true" />
                </button>
                <p class="hint">{{ t('csvFileForThisPrintJob') }}</p>
              </template>
              <template v-else>
                <p class="empty-message">{{ t('noCsvFileUploadedYet') }}</p>
                <label
                  class="primary-button upload-button"
                  :class="{ disabled: isUploading || readOnly || !documentAvailable }"
                  ><Upload :size="17" aria-hidden="true" />{{
                    isUploading ? t('uploading') : t('uploadCsvFile')
                  }}<input
                    class="sr-only"
                    type="file"
                    accept=".csv,text/csv"
                    :disabled="isUploading || readOnly || !documentAvailable"
                    @change="uploadFile"
                /></label>
                <p class="hint">{{ t('uploadTheInputDataThenStartThePrintJob') }}</p>
              </template>
            </div>
          </section>
          <details class="detail-card result-files" open>
            <summary class="card-heading">
              <FileText :size="20" aria-hidden="true" /><span>{{ t('results') }}</span
              ><span class="result-count">{{ printJob.fileIds?.length ?? 0 }} {{ t('pdfs') }}</span
              ><ChevronRight :size="16" class="disclosure-icon" aria-hidden="true" />
            </summary>
            <div class="card-body">
              <ul v-if="printJob.fileIds?.length">
                <li v-for="fileId in printJob.fileIds" :key="fileId">
                  <button
                    class="file-link"
                    type="button"
                    :title="fileId"
                    @click="downloadResultFile(fileId)"
                  >
                    <FileText :size="18" aria-hidden="true" /><span class="filename">{{
                      fileId
                    }}</span
                    ><Download :size="16" aria-hidden="true" />
                  </button>
                </li>
              </ul>
              <p v-else class="empty-message">
                {{
                  printJob.status === 'FAILED'
                    ? t('noResultFilesAreAvailableForThisJob')
                    : t('generatedPdfFilesWillAppearHereAfterProcessing')
                }}
              </p>
            </div>
          </details>
        </div>
      </template>
    </div>
  </main>
</template>

<style scoped>
.page {
  min-height: calc(100dvh - var(--app-header-height));
  padding: 36px 24px 64px;
  background: var(--peel-background);
  color: var(--peel-text);
}
.page-content {
  max-width: 1120px;
  margin: 0 auto;
}
.back-link {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  margin-bottom: 24px;
  color: var(--peel-muted);
  text-decoration: none;
  font-size: 13px;
}
.back-link:hover {
  color: var(--peel-text);
}
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 20px;
  margin-bottom: 24px;
}
.page-intro {
  min-width: 0;
}
.eyebrow {
  margin-bottom: 6px;
  font-size: 12px;
  color: var(--peel-muted);
}
h1 {
  font-size: clamp(28px, 4vw, 40px);
  line-height: 1.2;
  font-weight: 650;
  letter-spacing: -0.03em;
  overflow-wrap: anywhere;
}
.document-name {
  margin-top: 8px;
  color: var(--peel-muted);
  overflow-wrap: anywhere;
}
.header-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}
.icon-button {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  padding: 0;
  background: transparent;
}
.start-button,
.upload-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 14px;
}
.detail-card {
  border: 1px solid var(--peel-border);
  border-radius: 12px;
  background: var(--peel-surface);
  min-width: 0;
}
.metadata-card {
  padding: 20px 24px;
  margin: 20px 0;
}
dl {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
}
dt {
  color: var(--peel-muted);
  font-size: 12px;
  margin-bottom: 5px;
}
dd {
  margin: 0;
  overflow-wrap: anywhere;
}
.file-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.3fr);
  align-items: start;
  gap: 20px;
}
.card-heading {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 18px 20px;
  font-size: 15px;
  font-weight: 600;
}
.card-heading > svg:first-child {
  color: var(--peel-accent);
  flex-shrink: 0;
}
.card-body {
  padding: 18px 20px 20px;
  border-top: 1px solid var(--peel-border);
}
.file-link {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 11px 12px;
  text-align: left;
  background: var(--peel-background);
  color: var(--peel-text);
  border: 1px solid var(--peel-border);
  border-radius: 8px;
}
.file-link svg {
  flex-shrink: 0;
  color: var(--peel-muted);
}
.file-link svg:last-child {
  margin-left: auto;
}
.filename {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}
.hint,
.empty-message {
  color: var(--peel-muted);
  font-size: 13px;
}
.hint {
  margin-top: 12px;
}
.empty-message + .upload-button {
  margin-top: 18px;
}
.upload-button {
  position: relative;
  cursor: pointer;
  border: 1px solid var(--peel-accent);
  border-radius: 8px;
  width: fit-content;
}
.upload-button.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.upload-button:focus-within {
  outline: 2px solid var(--peel-accent);
  outline-offset: 3px;
}
.result-files summary {
  cursor: pointer;
  list-style: none;
}
.result-files summary::-webkit-details-marker {
  display: none;
}
.result-count {
  margin-left: auto;
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--peel-raised);
  color: var(--peel-muted);
  font-size: 12px;
  font-weight: normal;
  white-space: nowrap;
}
.result-files[open] .disclosure-icon {
  transform: rotate(90deg);
}
ul {
  display: grid;
  gap: 8px;
  padding: 0;
  margin: 0;
  list-style: none;
  max-height: 440px;
  overflow: auto;
}
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  width: fit-content;
  padding: 4px 10px;
  border: 1px solid var(--peel-border);
  border-radius: 999px;
  color: var(--peel-muted);
  font-size: 12px;
  white-space: nowrap;
}
.status-dot {
  width: 6px;
  height: 6px;
  background: currentColor;
  border-radius: 50%;
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
.notice,
.status-message,
.error-message {
  padding: 14px 18px;
  border: 1px solid var(--peel-border);
  border-radius: 10px;
  color: var(--peel-muted);
  margin: 16px 0;
  background: var(--peel-surface);
}
.error-message {
  border-color: var(--peel-error);
  color: var(--peel-error);
}
@media (max-width: 760px) {
  .page {
    padding: 24px 16px 48px;
  }
  .file-grid {
    grid-template-columns: 1fr;
  }
  dl {
    grid-template-columns: 1fr;
    gap: 14px;
  }
  .metadata-card {
    padding: 18px 20px;
  }
  .header-actions {
    width: 100%;
  }
  .start-button {
    margin-left: auto;
  }
}
</style>
