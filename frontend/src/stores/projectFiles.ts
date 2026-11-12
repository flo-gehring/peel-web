import { ref } from 'vue'
import { defineStore } from 'pinia'
import { api } from '@/adapter/client'
import { apiErrorMessage } from '@/adapter/apiError'
import type { components } from '@/types/api'

export type ProjectFile = components['schemas']['ProjectFile']
export type FileContent = components['schemas']['FileContent']
export type FileSummary = components['schemas']['FileSummary']
export type FileState = { name: string; description: string; content: FileContent }

export function fileError(error: unknown): Error {
  let message = apiErrorMessage(error)
  const usages = (error as { error?: { details?: { usages?: FileSummary[] } } })?.error?.details
    ?.usages
  if (usages?.length) message += ` Used by: ${usages.map((file) => file.name).join(', ')}.`
  return new Error(message)
}

export const useProjectFileStore = defineStore('projectFiles', () => {
  // These are acknowledged server states, never refreshed implicitly during a save.
  const loaded = ref<Record<string, ProjectFile>>({})
  const metadataDrafts = ref<Record<string, { name: string; description: string }>>({})
  function metadata(fileId: string): { name: string; description: string } {
    return (
      metadataDrafts.value[fileId] ?? {
        name: loaded.value[fileId]?.name ?? '',
        description: loaded.value[fileId]?.description ?? '',
      }
    )
  }
  function setMetadata(
    fileId: string,
    update: Partial<{ name: string; description: string }>,
  ): void {
    metadataDrafts.value[fileId] = { ...metadata(fileId), ...update }
  }
  function metadataDirty(fileId: string): boolean {
    const draft = metadataDrafts.value[fileId]
    const saved = loaded.value[fileId]
    return !!draft && (draft.name !== saved?.name || draft.description !== saved?.description)
  }
  const saving = new Set<string>()
  const pendingLoads = new Map<string, Promise<ProjectFile>>()
  let generation = 0

  async function list(projectId: string): Promise<FileSummary[]> {
    const { data, error } = await api.GET('/api/projects/{projectId}/files', {
      params: { path: { projectId } },
    })
    if (error || !data) throw fileError(error)
    return data
  }

  async function load(projectId: string, fileId: string, reload = false): Promise<ProjectFile> {
    const cached = loaded.value[fileId]
    if (!reload && cached?.projectId?.id === projectId) return cached
    const key = `${projectId}/${fileId}`
    const pending = pendingLoads.get(key)
    if (pending) return pending
    if (saving.has(fileId)) throw new Error('Wait for the current save before reloading.')
    const started = generation
    const request = (async () => {
      const { data, error } = await api.GET('/api/projects/{projectId}/files/{fileId}', {
        params: { path: { projectId, fileId } },
      })
      if (started !== generation) throw new Error('The workspace changed.')
      if (error || !data) throw fileError(error)
      loaded.value[fileId] = data
      if (reload) delete metadataDrafts.value[fileId]
      return data
    })()
    pendingLoads.set(key, request)
    try {
      return await request
    } finally {
      if (pendingLoads.get(key) === request) pendingLoads.delete(key)
    }
  }

  async function create(projectId: string, id: string, state: FileState): Promise<ProjectFile> {
    const started = generation
    const { data, error } = await api.POST('/api/projects/{projectId}/files', {
      params: { path: { projectId } },
      body: { id, ...state },
    })
    if (started !== generation) throw new Error('The workspace changed.')
    if (error || !data) throw fileError(error)
    loaded.value[id] = data
    return data
  }

  async function save(projectId: string, fileId: string, state: FileState): Promise<ProjectFile> {
    const baseline = loaded.value[fileId]
    if (!baseline || baseline.projectId?.id !== projectId || baseline.version === undefined)
      throw new Error('Reopen the file before saving. Its original version is unavailable.')
    if (saving.has(fileId)) throw new Error('This file is already being saved. Please wait.')
    if (pendingLoads.has(`${projectId}/${fileId}`))
      throw new Error('Wait for the file to finish loading.')
    const started = generation
    saving.add(fileId)
    try {
      const { data, error } = await api.PUT('/api/projects/{projectId}/files/{fileId}', {
        params: { path: { projectId, fileId } },
        body: { expectedVersion: baseline.version, ...state },
      })
      if (started !== generation) throw new Error('The workspace changed.')
      if (error || !data) throw fileError(error)
      loaded.value[fileId] = data
      const draft = metadataDrafts.value[fileId]
      if (draft?.name === state.name && draft.description === state.description)
        delete metadataDrafts.value[fileId]
      return data
    } finally {
      saving.delete(fileId)
    }
  }

  async function remove(projectId: string, fileId: string, expectedVersion: number): Promise<void> {
    const { error } = await api.DELETE('/api/projects/{projectId}/files/{fileId}', {
      params: { path: { projectId, fileId }, query: { expectedVersion } },
    })
    if (error) throw fileError(error)
    delete loaded.value[fileId]
    delete metadataDrafts.value[fileId]
  }

  function clear(): void {
    generation += 1
    pendingLoads.clear()
    loaded.value = {}
    metadataDrafts.value = {}
  }
  return {
    loaded,
    metadataDrafts,
    metadata,
    setMetadata,
    metadataDirty,
    list,
    load,
    create,
    save,
    remove,
    clear,
  }
})
