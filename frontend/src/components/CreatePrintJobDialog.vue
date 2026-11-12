<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { api } from '@/adapter/client'
import { apiErrorMessage } from '@/adapter/apiError'

const props = defineProps<{
  projectId: string
  documentId: string
  documentName: string
  saveRequired?: boolean
  prepare?: () => Promise<void>
}>()
const emit = defineEmits<{ cancel: []; created: [id: string] }>()
const name = ref('')
const busy = ref(false)
const error = ref('')
const nameInput = ref<HTMLInputElement | null>(null)
let alive = true
const submitLabel = computed(() =>
  busy.value
    ? 'Wird angelegt…'
    : props.saveRequired
      ? 'Speichern und Auftrag anlegen'
      : 'Auftrag anlegen',
)
onMounted(async () => {
  await nextTick()
  nameInput.value?.focus()
})
onUnmounted(() => {
  alive = false
})
function cancel(): void {
  if (!busy.value) emit('cancel')
}
async function create(): Promise<void> {
  if (busy.value || !name.value.trim()) return
  const projectId = props.projectId,
    documentId = props.documentId,
    jobName = name.value.trim()
  busy.value = true
  error.value = ''
  try {
    await props.prepare?.()
    if (!alive) return
    const { data, error: failure } = await api.POST('/api/projects/{projectId}/print-jobs', {
      params: { path: { projectId } },
      body: { name: jobName, documentId: { id: documentId } },
    })
    if (failure || !data?.id)
      throw new Error(apiErrorMessage(failure, 'Druckauftrag konnte nicht angelegt werden.'))
    if (alive) emit('created', data.id)
  } catch (failure) {
    error.value =
      failure instanceof Error ? failure.message : 'Der Server konnte nicht erreicht werden.'
  } finally {
    busy.value = false
  }
}
function trapFocus(event: KeyboardEvent): void {
  if (event.key !== 'Tab') return
  const form = event.currentTarget as HTMLFormElement
  const controls = Array.from(
    form.querySelectorAll<HTMLElement>(
      'button:not(:disabled), input:not(:disabled), [tabindex="0"]',
    ),
  )
  const first = controls[0],
    last = controls.at(-1)
  if (!first) {
    event.preventDefault()
    form.focus()
    return
  }
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault()
    last?.focus()
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault()
    first.focus()
  }
}
</script>
<template>
  <Teleport to="body">
    <div class="print-job-backdrop" @click.self="cancel">
      <form
        class="print-job-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="create-print-job-title"
        tabindex="-1"
        @submit.prevent="create"
        @keydown.esc.stop.prevent="cancel"
        @keydown="trapFocus"
      >
        <h2 id="create-print-job-title">Druckauftrag anlegen</h2>
        <p>
          Dokumentvorlage: <strong>{{ documentName }}</strong>
        </p>
        <p v-if="saveRequired" class="save-notice">
          Die Dokumentvorlage hat ungespeicherte Änderungen. Sie wird vor dem Anlegen gespeichert.
        </p>
        <p v-if="error" role="alert" class="dialog-error">{{ error }}</p>
        <label for="print-job-name">Name des Druckauftrags</label>
        <input id="print-job-name" ref="nameInput" v-model="name" required :disabled="busy" />
        <p class="hint">
          Der Auftrag verwendet die beim Start gespeicherte Vorlage. Auf der nächsten Seite lädst du
          die CSV-Datei hoch und startest den Auftrag.
        </p>
        <div class="dialog-actions">
          <button type="button" :disabled="busy" @click="cancel">Abbrechen</button
          ><button class="primary-button" :disabled="busy || !name.trim()">
            {{ submitLabel }}
          </button>
        </div>
      </form>
    </div>
  </Teleport>
</template>
<style scoped>
.print-job-backdrop {
  position: fixed;
  inset: 0;
  z-index: 150;
  display: grid;
  place-items: center;
  padding: 20px;
  background: #000a;
}
.print-job-dialog {
  width: min(460px, 100%);
  max-height: calc(100dvh - 40px);
  overflow: auto;
  display: grid;
  gap: 14px;
  padding: 24px;
  border: 1px solid var(--peel-border);
  border-radius: 12px;
  background: var(--peel-surface);
  color: var(--peel-text);
}
h2 {
  margin: 0;
  font-size: 21px;
}
p {
  margin: 0;
  overflow-wrap: anywhere;
}
.hint {
  color: var(--peel-muted);
  font-size: 12px;
}
.save-notice {
  color: var(--peel-accent);
}
.dialog-error {
  color: var(--peel-error);
  white-space: pre-wrap;
}
.dialog-actions {
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
