<script setup lang="ts">
import {
  Banana,
  Palette,
  FileText,
  Check,
  Copy,
  Trash2,
  Pencil,
  RotateCcw,
  RefreshCw,
  Plus,
  ChevronRight,
} from '@lucide/vue'
import { computed, ref, watch } from 'vue'

import { useProjectFileStore, type FileState } from '@/stores/projectFiles'
import { api } from '@/adapter/client'
import { renderConfigurationSchema } from '@/adapter/renderConfigurationSchema'
import type { PeelWorkspaceDocument } from '@/adapter/ClientTypeDefinition'
import { useWorkspaceSelectionStore } from '@/stores/workspaceSelection'
import { useProjectStore } from '@/stores/projects'

type PeelScriptDocument = Extract<PeelWorkspaceDocument, { kind: 'peel' }>
type RenderConfigDocument = Extract<PeelWorkspaceDocument, { kind: 'renderConfig' }>
type DocumentWorkspaceItem = Extract<PeelWorkspaceDocument, { kind: 'document' }>
type CreatableDocumentKind = 'peel' | 'renderConfig' | 'document'

// Dockview passes panel props automatically to registered components
defineProps<{ params?: Record<string, never> }>()

const scripts = ref<PeelScriptDocument[]>([])
const renderConfigs = ref<RenderConfigDocument[]>([])
const documents = ref<DocumentWorkspaceItem[]>([])
const selectionStore = useWorkspaceSelectionStore()
const projectStore = useProjectStore()
const files = useProjectFileStore()
const versions = ref<Record<string, number>>({})
const errorMessage = ref<string | null>(null)
const pendingCreate = ref<{ id: string; state: FileState } | null>(null)
const newDescription = ref('')
const editingId = ref<string | null>(null)
const editName = ref('')
const editDescription = ref('')
const isEditing = ref(false)
const errorText = (error: unknown) =>
  error instanceof Error ? error.message : 'The request failed.'
const projectId = computed(() => projectStore.activeProject?.projectIdApi?.id ?? '')

const isLoading = ref(false)
const activeFileId = ref<string | null>(null)
const isCreateDialogOpen = ref(false)
const newDocumentName = ref('')
const newDocumentKind = ref<CreatableDocumentKind>('peel')
const expanded = ref<Record<CreatableDocumentKind, boolean>>({
  peel: true,
  renderConfig: true,
  document: true,
})
const createTitles: Record<CreatableDocumentKind, string> = {
  peel: 'New script',
  renderConfig: 'New render configuration',
  document: 'New document',
}
const newDocumentRenderConfigurationId = ref('')
const isCreating = ref(false)
const deletingScriptId = ref<string | null>(null)
const deletingRenderConfigId = ref<string | null>(null)
const deletingDocumentId = ref<string | null>(null)
const copiedId = ref<string | null>(null)

async function fetchFiles() {
  isLoading.value = true
  try {
    if (!projectId.value) return
    await projectStore.loadProject(projectId.value)
    const summaries = await files.list(projectId.value)
    versions.value = Object.fromEntries(summaries.map((file) => [file.id ?? '', file.version ?? 0]))
    scripts.value = summaries
      .filter((file) => file.type === 'script')
      .map((file) => ({ kind: 'peel', id: file.id ?? '', name: file.name ?? '', icon: '🍌' }))
    renderConfigs.value = summaries
      .filter((file) => file.type === 'render-config')
      .map((file) => ({
        kind: 'renderConfig',
        id: file.id ?? '',
        name: file.name ?? '',
        icon: '{}',
      }))
    documents.value = summaries
      .filter((file) => file.type === 'document')
      .map((file) => ({ kind: 'document', id: file.id ?? '', name: file.name ?? '', icon: 'DOC' }))
  } catch (error) {
    errorMessage.value = errorText(error)
  } finally {
    isLoading.value = false
  }
}

function selectDocument(file: PeelScriptDocument | RenderConfigDocument | DocumentWorkspaceItem) {
  activeFileId.value = `${file.kind}-${file.id}`
  selectionStore.selectDocument(file)
}

function openCreateDialog(kind: CreatableDocumentKind) {
  if (projectStore.isReadOnly) return
  pendingCreate.value = null
  errorMessage.value = null
  newDescription.value = ''
  newDocumentName.value = ''
  newDocumentKind.value = kind
  expanded.value[kind] = true
  newDocumentRenderConfigurationId.value = ''
  isCreateDialogOpen.value = true
}

async function createDocument() {
  if (projectStore.isReadOnly) return
  const name = newDocumentName.value.trim()
  if (!name || isCreating.value) {
    return
  }

  isCreating.value = true

  errorMessage.value = null
  try {
    if (!pendingCreate.value) {
      let content: FileState['content']
      if (newDocumentKind.value === 'peel') content = { type: 'script', source: '' }
      else if (newDocumentKind.value === 'document') {
        if (!newDocumentRenderConfigurationId.value)
          throw new Error('Select a render configuration.')
        content = {
          type: 'document',
          scriptNameTags: {},
          templateHtml: '<p></p>',
          editorStateJson: '{"type":"doc","content":[{"type":"paragraph"}]}',
          renderConfigurationId: newDocumentRenderConfigurationId.value,
        }
      } else {
        const { data, error } = await api.GET('/api/render-config/default')
        if (error || !data) throw new Error('The default configuration could not be loaded.')
        content = { type: 'render-config', ...renderConfigurationSchema.parse(data) }
      }
      pendingCreate.value = {
        id: crypto.randomUUID(),
        state: { name, description: newDescription.value, content },
      }
    }
    const created = await files.create(
      projectId.value,
      pendingCreate.value.id,
      pendingCreate.value.state,
    )
    pendingCreate.value = null
    isCreateDialogOpen.value = false
    await fetchFiles()
    selectDocument({
      kind:
        created.content?.type === 'script'
          ? 'peel'
          : created.content?.type === 'render-config'
            ? 'renderConfig'
            : 'document',
      id: created.id ?? '',
      name: created.name ?? name,
      icon: '',
    })
  } catch (error) {
    errorMessage.value = errorText(error)
  } finally {
    isCreating.value = false
  }
}

async function copyId(file: PeelWorkspaceDocument): Promise<void> {
  try {
    await navigator.clipboard.writeText(file.id)
    copiedId.value = `${file.kind}-${file.id}`
    window.setTimeout(() => {
      if (copiedId.value === `${file.kind}-${file.id}`) copiedId.value = null
    }, 1500)
  } catch (error) {
    console.error('Failed to copy workspace item ID:', error)
  }
}

async function deleteFile(file: PeelWorkspaceDocument, busy: { value: string | null }) {
  if (projectStore.isReadOnly || busy.value || !window.confirm(`Delete "${file.name}"?`)) return
  busy.value = file.id
  errorMessage.value = null
  try {
    const version = Math.max(versions.value[file.id] ?? 0, files.loaded[file.id]?.version ?? 0)
    await files.remove(projectId.value, file.id, version)
    scripts.value = scripts.value.filter((item) => item.id !== file.id)
    renderConfigs.value = renderConfigs.value.filter((item) => item.id !== file.id)
    documents.value = documents.value.filter((item) => item.id !== file.id)
    selectionStore.deleteDocument(file)
    await projectStore.loadProject(projectId.value)
  } catch (error) {
    errorMessage.value = errorText(error)
  } finally {
    busy.value = null
  }
}
const deletePeelScript = (file: PeelScriptDocument) => deleteFile(file, deletingScriptId)
const deleteRenderConfig = (file: RenderConfigDocument) => deleteFile(file, deletingRenderConfigId)
const deleteDocument = (file: DocumentWorkspaceItem) => deleteFile(file, deletingDocumentId)
function reloadFile(file: PeelWorkspaceDocument) {
  if (window.confirm('Discard local edits to this file and load the latest server version?'))
    selectionStore.reloadDocument(file)
}
async function editMetadata(file: PeelWorkspaceDocument) {
  errorMessage.value = null
  try {
    const baseline = await files.load(projectId.value, file.id)
    editingId.value = file.id
    editName.value = baseline.name ?? ''
    editDescription.value = baseline.description ?? ''
  } catch (error) {
    errorMessage.value = errorText(error)
  }
}
async function saveMetadata() {
  const id = editingId.value
  if (!id || isEditing.value || projectStore.isReadOnly) return
  isEditing.value = true
  errorMessage.value = null
  try {
    const baseline = files.loaded[id]
    if (!baseline?.content) throw new Error('Reopen the file before saving.')
    await files.save(projectId.value, id, {
      name: editName.value,
      description: editDescription.value,
      content: baseline.content,
    })
    editingId.value = null
    await fetchFiles()
    const updated = [...scripts.value, ...renderConfigs.value, ...documents.value].find(
      (file) => file.id === id,
    )
    if (updated) selectDocument(updated)
  } catch (error) {
    errorMessage.value = errorText(error)
  } finally {
    isEditing.value = false
  }
}

watch(projectId, () => void fetchFiles(), { immediate: true })
watch(
  () => Object.values(files.loaded).map((file) => [file.id, file.name]),
  () => {
    for (const items of [scripts.value, renderConfigs.value, documents.value]) {
      for (const item of items) item.name = files.loaded[item.id]?.name ?? item.name
    }
  },
)
</script>
<template>
  <div class="file-tree-container">
    <div class="panel-header">
      <span>Project files</span>
      <button
        class="header-icon-button"
        title="Refresh files"
        aria-label="Refresh files"
        :disabled="isLoading"
        @click="fetchFiles"
      >
        <RefreshCw :size="16" aria-hidden="true" />
      </button>
    </div>
    <p v-if="errorMessage" role="alert" class="file-error">{{ errorMessage }}</p>
    <div v-if="isLoading" class="loading">Loading...</div>
    <section class="document-section">
      <div class="section-header">
        <h2>
          <button
            class="section-toggle"
            :aria-expanded="expanded.peel"
            aria-controls="files-peel"
            @click="expanded.peel = !expanded.peel"
            title="Scripts"
          >
            <ChevronRight
              :size="14"
              :class="{ expanded: expanded.peel }"
              aria-hidden="true"
            /><Banana :size="16" aria-hidden="true" /><span>Scripts</span>
          </button>
        </h2>
        <button
          class="header-icon-button"
          title="New script"
          aria-label="New script"
          :disabled="projectStore.isReadOnly"
          @click="openCreateDialog('peel')"
        >
          <Plus :size="16" aria-hidden="true" />
        </button>
      </div>
      <ul v-show="expanded.peel" id="files-peel" class="file-list">
        <li
          v-for="file in scripts"
          :key="`peel-${file.id}`"
          :class="{ active: activeFileId === `peel-${file.id}` }"
          @click="selectDocument(file)"
        >
          <button class="file-name" @click.stop="selectDocument(file)">{{ file.name }}</button>
          <button
            class="copy-id-button"
            title="Reload file"
            aria-label="Reload file"
            @click.stop="reloadFile(file)"
          >
            <RotateCcw :size="14" />
          </button>
          <button
            class="copy-id-button"
            title="Edit name and description"
            aria-label="Edit name and description"
            :disabled="projectStore.isReadOnly"
            @click.stop="editMetadata(file)"
          >
            <Pencil :size="14" />
          </button>
          <button
            class="delete-button"
            :disabled="projectStore.isReadOnly || deletingScriptId === file.id"
            title="Delete"
            aria-label="Delete"
            :aria-busy="deletingScriptId === file.id"
            @click.stop="deletePeelScript(file)"
          >
            <Trash2 :size="16" aria-hidden="true" />
          </button>
          <button
            class="copy-id-button"
            :title="copiedId === `peel-${file.id}` ? 'ID copied' : 'Copy ID'"
            aria-label="Copy ID"
            @click.stop="copyId(file)"
          >
            <Check
              v-if="copiedId === `peel-${file.id}`"
              :size="16"
              class="copied-icon"
              aria-hidden="true"
            />
            <Copy v-else :size="16" aria-hidden="true" />
          </button>
        </li>
      </ul>
    </section>
    <section class="document-section">
      <div class="section-header">
        <h2>
          <button
            class="section-toggle"
            :aria-expanded="expanded.renderConfig"
            aria-controls="files-renderConfig"
            @click="expanded.renderConfig = !expanded.renderConfig"
            title="Render configurations"
          >
            <ChevronRight
              :size="14"
              :class="{ expanded: expanded.renderConfig }"
              aria-hidden="true"
            /><Palette :size="16" aria-hidden="true" /><span>Render configurations</span>
          </button>
        </h2>
        <button
          class="header-icon-button"
          title="New render configuration"
          aria-label="New render configuration"
          :disabled="projectStore.isReadOnly"
          @click="openCreateDialog('renderConfig')"
        >
          <Plus :size="16" aria-hidden="true" />
        </button>
      </div>
      <ul v-show="expanded.renderConfig" id="files-renderConfig" class="file-list">
        <li
          v-for="file in renderConfigs"
          :key="`renderConfig-${file.id}`"
          :class="{ active: activeFileId === `renderConfig-${file.id}` }"
          @click="selectDocument(file)"
        >
          <button class="file-name" @click.stop="selectDocument(file)">{{ file.name }}</button>
          <button
            class="copy-id-button"
            title="Reload file"
            aria-label="Reload file"
            @click.stop="reloadFile(file)"
          >
            <RotateCcw :size="14" />
          </button>
          <button
            class="copy-id-button"
            title="Edit name and description"
            aria-label="Edit name and description"
            :disabled="projectStore.isReadOnly"
            @click.stop="editMetadata(file)"
          >
            <Pencil :size="14" />
          </button>
          <button
            class="delete-button"
            :disabled="projectStore.isReadOnly || deletingRenderConfigId === file.id"
            title="Delete"
            aria-label="Delete"
            :aria-busy="deletingRenderConfigId === file.id"
            @click.stop="deleteRenderConfig(file)"
          >
            <Trash2 :size="16" aria-hidden="true" />
          </button>
          <button
            class="copy-id-button"
            :title="copiedId === `renderConfig-${file.id}` ? 'ID copied' : 'Copy ID'"
            aria-label="Copy ID"
            @click.stop="copyId(file)"
          >
            <Check
              v-if="copiedId === `renderConfig-${file.id}`"
              :size="16"
              class="copied-icon"
              aria-hidden="true"
            />
            <Copy v-else :size="16" aria-hidden="true" />
          </button>
        </li>
      </ul>
    </section>
    <section class="document-section">
      <div class="section-header">
        <h2>
          <button
            class="section-toggle"
            :aria-expanded="expanded.document"
            aria-controls="files-document"
            @click="expanded.document = !expanded.document"
            title="Documents"
          >
            <ChevronRight
              :size="14"
              :class="{ expanded: expanded.document }"
              aria-hidden="true"
            /><FileText :size="16" aria-hidden="true" /><span>Documents</span>
          </button>
        </h2>
        <button
          class="header-icon-button"
          title="New document"
          aria-label="New document"
          :disabled="projectStore.isReadOnly"
          @click="openCreateDialog('document')"
        >
          <Plus :size="16" aria-hidden="true" />
        </button>
      </div>
      <ul v-show="expanded.document" id="files-document" class="file-list">
        <li
          v-for="file in documents"
          :key="`document-${file.id}`"
          :class="{ active: activeFileId === `document-${file.id}` }"
          @click="selectDocument(file)"
        >
          <button class="file-name" @click.stop="selectDocument(file)">{{ file.name }}</button>
          <button
            class="copy-id-button"
            :title="copiedId === `document-${file.id}` ? 'ID copied' : 'Copy ID'"
            aria-label="Copy ID"
            @click.stop="copyId(file)"
          >
            <Check
              v-if="copiedId === `document-${file.id}`"
              :size="16"
              class="copied-icon"
              aria-hidden="true"
            />
            <Copy v-else :size="16" aria-hidden="true" />
          </button>
          <button
            class="copy-id-button"
            title="Reload file"
            aria-label="Reload file"
            @click.stop="reloadFile(file)"
          >
            <RotateCcw :size="14" />
          </button>
          <button
            class="copy-id-button"
            title="Edit name and description"
            aria-label="Edit name and description"
            :disabled="projectStore.isReadOnly"
            @click.stop="editMetadata(file)"
          >
            <Pencil :size="14" />
          </button>
          <button
            class="delete-button"
            :disabled="projectStore.isReadOnly || deletingDocumentId === file.id"
            title="Delete document"
            aria-label="Delete document"
            @click.stop="deleteDocument(file)"
          >
            <Trash2 :size="16" aria-hidden="true" />
          </button>
        </li>
      </ul>
    </section>

    <span class="sr-only" role="status">{{ copiedId ? 'ID copied to clipboard.' : '' }}</span>

    <div
      v-if="isCreateDialogOpen"
      class="create-dialog-backdrop"
      @click.self="!isCreating && (isCreateDialogOpen = false)"
    >
      <form class="create-dialog" @submit.prevent="createDocument">
        <h2>{{ createTitles[newDocumentKind] }}</h2>
        <p v-if="errorMessage" role="alert" class="file-error">{{ errorMessage }}</p>
        <p v-if="pendingCreate">
          Retry sends the same file. Cancel and refresh to check its status.
        </p>
        <label for="new-document-name">Name</label>
        <input
          id="new-document-name"
          v-model="newDocumentName"
          :disabled="!!pendingCreate || isCreating"
          autofocus
          placeholder="Name"
        />
        <label for="new-description">Description</label
        ><input
          id="new-description"
          v-model="newDescription"
          :disabled="!!pendingCreate || isCreating"
        />
        <label v-if="newDocumentKind === 'document'" for="new-document-render-config"
          >Render Config</label
        >
        <select
          v-if="newDocumentKind === 'document'"
          id="new-document-render-config"
          v-model="newDocumentRenderConfigurationId"
          :disabled="!!pendingCreate || isCreating"
        >
          <option value="">Select a render config</option>
          <option v-for="config in renderConfigs" :key="config.id" :value="config.id">
            {{ config.name }}
          </option>
        </select>
        <div class="dialog-actions">
          <button type="button" :disabled="isCreating" @click="isCreateDialogOpen = false">
            Cancel
          </button>
          <button
            type="submit"
            :disabled="
              !newDocumentName.trim() ||
              isCreating ||
              (newDocumentKind === 'document' && !newDocumentRenderConfigurationId)
            "
          >
            {{ isCreating ? 'Creating...' : pendingCreate ? 'Retry' : 'Create' }}
          </button>
        </div>
      </form>
    </div>
    <div v-if="editingId" class="create-dialog-backdrop">
      <form class="create-dialog" @submit.prevent="saveMetadata">
        <h2>File details</h2>
        <p v-if="errorMessage" role="alert" class="file-error">{{ errorMessage }}</p>
        <label for="edit-name">Name</label
        ><input id="edit-name" v-model="editName" required :disabled="isEditing" />
        <label for="edit-description">Description</label
        ><input id="edit-description" v-model="editDescription" :disabled="isEditing" />
        <div class="dialog-actions">
          <button type="button" :disabled="isEditing" @click="editingId = null">Cancel</button
          ><button :disabled="isEditing || !editName.trim()">Save</button>
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
.file-error {
  color: #f48771;
  padding: 0 12px;
  white-space: pre-wrap;
}
.file-tree-container {
  height: 100%;
  background-color: var(--peel-surface);
  color: var(--peel-text);
  overflow-y: auto;
  font-family: system-ui, sans-serif;
  font-size: 13px;
  user-select: none;
}
.panel-header {
  padding: 8px 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
  font-size: 12px;
  color: var(--peel-muted);
  border-bottom: 1px solid var(--peel-border);
}
.header-icon-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 26px;
  width: 26px;
  height: 26px;
  padding: 0;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: var(--peel-muted);
  cursor: pointer;
}
.header-icon-button:hover:not(:disabled) {
  background: var(--peel-raised);
  color: var(--peel-text);
}
.header-icon-button:disabled {
  opacity: 0.5;
  cursor: default;
}
.section-header {
  display: flex;
  align-items: center;
  gap: 4px;
  margin: 12px 8px 2px;
}
.section-header h2 {
  flex: 1;
  min-width: 0;
  margin: 0;
  font-size: 12px;
}
.section-toggle {
  display: flex;
  align-items: center;
  gap: 5px;
  width: 100%;
  min-width: 0;
  padding: 6px 2px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: var(--peel-muted);
  font: inherit;
  font-weight: bold;
  text-align: left;
  cursor: pointer;
}
.section-toggle:hover {
  color: var(--peel-text);
}
.section-toggle span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.section-toggle svg {
  flex-shrink: 0;
}
.section-toggle svg.expanded {
  transform: rotate(90deg);
}
.header-icon-button:focus-visible,
.section-toggle:focus-visible {
  outline: 2px solid var(--peel-accent);
  outline-offset: 2px;
}
.file-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.loading {
  padding: 8px 12px;
  color: #888888;
}
.file-list li {
  padding: 9px 10px;
  margin: 2px 6px;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
}
.file-name {
  flex: 1;
  min-width: 0;
  padding: 0;
  background: transparent;
  border: 0;
  border-radius: 0;
  text-align: left;
  color: inherit;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.delete-button {
  display: none;
  flex: 0 0 24px;
  width: 24px;
  height: 24px;
  padding: 0;
  align-items: center;
  justify-content: center;
  border: 0;
  background: transparent;
  color: #cccccc;
  cursor: pointer;
  font-size: 18px;
  line-height: 16px;
}
.copy-id-button {
  display: none;
  flex: 0 0 24px;
  width: 24px;
  height: 24px;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
  border: 0;
  background: transparent;
  color: #cccccc;
  cursor: pointer;
  font-size: 11px;
  padding: 0;
}
.copied-icon {
  color: #a6e3a1;
}
.file-list li:hover .delete-button,
.file-list li.active .delete-button,
.file-list li:hover .copy-id-button,
.file-list li:focus-within .delete-button,
.file-list li:focus-within .copy-id-button,
.file-list li.active .copy-id-button {
  display: inline-flex;
}
.delete-button:hover {
  color: #f48771;
}
.delete-button:disabled {
  cursor: default;
  opacity: 0.5;
}
.file-list li:hover {
  background-color: var(--peel-raised);
}
.file-list li.active {
  background-color: #ffcf3226;
  color: var(--peel-text);
}
.create-dialog-backdrop {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: grid;
  place-items: center;
  background: rgba(0, 0, 0, 0.45);
}
.create-dialog {
  width: min(320px, calc(100vw - 32px));
  display: grid;
  gap: 12px;
  padding: 16px;
  border: 1px solid #3c3c3c;
  border-radius: 12px;
  background: var(--peel-surface);
}
.create-dialog h2 {
  margin: 0;
  font-size: 15px;
}
.create-dialog select,
.create-dialog input[type='text'],
.create-dialog input:not([type]) {
  width: 100%;
  box-sizing: border-box;
  padding: 7px;
  border: 1px solid #3c3c3c;
  background: #1e1e1e;
  color: #ffffff;
  border-radius: 8px;
}
.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
.dialog-actions button {
  padding: 5px 10px;
  border: 1px solid #3c3c3c;
  border-radius: 3px;
  background: #333333;
  color: #ffffff;
  cursor: pointer;
}
.dialog-actions button:disabled {
  cursor: default;
  opacity: 0.5;
}
</style>
