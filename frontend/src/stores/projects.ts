import { ref } from 'vue'
import { defineStore } from 'pinia'
import { api } from '@/adapter/client'
import type { components } from '@/types/api'

type ProjectSummary = components['schemas']['ProjectSummary']
type ProjectData = components['schemas']['ProjectData']
type DocumentSummary = components['schemas']['DocumentTemplateSummaryResponse']

export const useProjectStore = defineStore('projects', () => {
  const projects = ref<ProjectSummary[]>([])
  const activeProject = ref<ProjectData | null>(null)
  const isLoading = ref(false)

  async function loadProjects(): Promise<void> {
    const { data, error } = await api.GET('/api/projects/list')
    if (error) throw error
    projects.value = data ?? []
  }

  async function loadProject(projectId: string): Promise<ProjectData> {
    isLoading.value = true
    try {
      const { data, error } = await api.GET('/api/projects/{projectId}', {
        params: { path: { projectId } },
      })
      if (error || !data) throw error ?? new Error('Project not found.')
      activeProject.value = data
      return data
    } finally {
      isLoading.value = false
    }
  }

  async function createProject(name: string): Promise<ProjectSummary> {
    const { data, error } = await api.POST('/api/projects/{projectId}', {
      params: { path: { projectId: name } },
    })
    if (error || !data) throw error ?? new Error('Project could not be created.')
    projects.value.push(data)
    return data
  }

  async function loadDocuments(): Promise<DocumentSummary[]> {
    await loadProjects()
    const projectData = await Promise.all(
      projects.value
        .map((project) => project.projectIdApi?.id)
        .filter((projectId): projectId is string => !!projectId)
        .map(async (projectId) => {
          const { data, error } = await api.GET('/api/projects/{projectId}', {
            params: { path: { projectId } },
          })
          if (error || !data) throw error ?? new Error('Project not found.')
          return data
        }),
    )
    return projectData.flatMap((project) => project.documentTemplates ?? [])
  }

  function clearActiveProject(): void {
    activeProject.value = null
  }

  function clear(): void {
    projects.value = []
    activeProject.value = null
    isLoading.value = false
  }

  return {
    projects,
    activeProject,
    isLoading,
    loadProjects,
    loadProject,
    createProject,
    loadDocuments,
    clearActiveProject,
    clear,
  }
})
