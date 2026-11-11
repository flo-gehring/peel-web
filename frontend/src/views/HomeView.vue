<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useProjectStore } from '@/stores/projects'
import peelLogo from '@/assets/peel-logo.png'

const router = useRouter()
const projectStore = useProjectStore()
const newProjectName = ref('')
const nameInput = ref<HTMLInputElement | null>(null)
const createButton = ref<HTMLButtonElement | null>(null)
const isCreating = ref(false)
const isLoading = ref(true)
const showCreate = ref(false)
const loadError = ref<string | null>(null)
const createError = ref<string | null>(null)
const projectFilter = ref('active')
const visibleProjects = computed(() =>
  projectStore.projects.filter((project) =>
    projectFilter.value === 'archived'
      ? project.state === 'ARCHIVED'
      : project.state !== 'ARCHIVED',
  ),
)

async function loadProjects(): Promise<void> {
  isLoading.value = true
  loadError.value = null
  try {
    await projectStore.loadProjects()
  } catch {
    loadError.value = 'Projects could not be loaded. Check your connection and try again.'
  } finally {
    isLoading.value = false
  }
}
async function openCreate(): Promise<void> {
  showCreate.value = true
  await nextTick()
  nameInput.value?.focus()
}
async function cancelCreate(): Promise<void> {
  if (isCreating.value) return
  showCreate.value = false
  newProjectName.value = ''
  createError.value = null
  await nextTick()
  createButton.value?.focus()
}
async function createProject(): Promise<void> {
  const name = newProjectName.value.trim()
  if (!name || isCreating.value) return
  isCreating.value = true
  createError.value = null
  try {
    const project = await projectStore.createProject(name)
    const id = project.projectIdApi?.id
    if (!id) throw new Error('Missing project ID')
    await router.push({ name: 'project', params: { projectId: id } })
  } catch {
    createError.value = 'The project could not be created. Please try again.'
  } finally {
    isCreating.value = false
  }
}
onMounted(loadProjects)
</script>

<template>
  <main class="home-view">
    <section class="projects-section" aria-labelledby="projects-title">
      <div class="projects-heading">
        <img :src="peelLogo" alt="Peel banana logo" class="welcome-logo" width="112" height="96" />
        <div class="projects-intro">
          <h1 id="projects-title">Projects</h1>
          <p>Your documents, scripts and render configurations.</p>
        </div>
        <button
          ref="createButton"
          class="primary-button"
          :aria-expanded="showCreate"
          aria-controls="new-project-form"
          @click="openCreate"
        >
          <span aria-hidden="true">＋</span> New project
        </button>
      </div>
      <div class="project-collection" :aria-busy="isLoading">
        <label class="project-filter"
          >Show
          <select v-model="projectFilter" aria-label="Project status">
            <option value="active">Active projects</option>
            <option value="archived">Archived projects</option>
          </select></label
        >
        <form
          v-if="showCreate"
          id="new-project-form"
          class="create-project"
          @submit.prevent="createProject"
          @keydown.esc.prevent="cancelCreate"
        >
          <span class="new-project-icon" aria-hidden="true">＋</span>
          <label class="sr-only" for="new-project-name">New project name</label>
          <input
            id="new-project-name"
            ref="nameInput"
            v-model="newProjectName"
            :disabled="isCreating"
            placeholder="New project name"
            required
            :aria-invalid="!!createError"
            :aria-describedby="createError ? 'create-project-error' : undefined"
          />
          <div class="create-actions">
            <button
              class="primary-button"
              type="submit"
              :disabled="!newProjectName.trim() || isCreating"
            >
              {{ isCreating ? 'Creating…' : 'Create' }}
            </button>
            <button type="button" class="quiet-button" :disabled="isCreating" @click="cancelCreate">
              Cancel
            </button>
          </div>
          <p v-if="createError" id="create-project-error" role="alert" class="project-error">
            {{ createError }}
          </p>
        </form>
        <div v-if="isLoading" class="collection-message" role="status">Loading projects…</div>
        <div v-else-if="loadError" class="collection-message">
          <p role="alert" class="project-error">{{ loadError }}</p>
          <button class="quiet-button" @click="loadProjects">Try again</button>
        </div>
        <div v-else-if="!visibleProjects.length" class="collection-message">
          <p>No projects yet. Create your first project to get started.</p>
        </div>
        <nav v-else aria-label="Projects" class="project-list">
          <div
            v-for="project in visibleProjects"
            :key="project.projectIdApi?.id"
            class="project-entry"
          >
            <RouterLink
              :key="project.projectIdApi?.id"
              class="project-row"
              :to="{ name: 'project', params: { projectId: project.projectIdApi?.id } }"
            >
              <svg
                class="folder-icon"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="1.7"
                aria-hidden="true"
              >
                <path
                  d="M3 7V5a1 1 0 0 1 1-1h5l2 3h9a1 1 0 0 1 1 1v11a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1Z"
                />
              </svg>
              <strong>{{ project.name }}</strong
              ><span class="project-description"
                >{{ project.description || 'Documents, scripts & render configurations'
                }}{{ project.state === 'DELETING' ? ' — Cleanup incomplete' : '' }}</span
              ><span class="open-arrow" aria-hidden="true">→</span>
            </RouterLink>
            <RouterLink
              class="manage-project"
              :to="`/projects/${project.projectIdApi?.id}/settings`"
              >Manage</RouterLink
            >
          </div>
        </nav>
      </div>
    </section>
  </main>
</template>

<style scoped>
.project-filter {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 28px;
  border-bottom: 1px solid var(--peel-border);
  color: var(--peel-muted);
}
.project-entry {
  display: flex;
  align-items: center;
  border-bottom: 1px solid var(--peel-border);
}
.project-entry .project-row {
  flex: 1;
  min-width: 0;
  border-bottom: 0;
}
.manage-project {
  margin-right: 24px;
  color: var(--peel-accent);
}
.home-view {
  min-height: calc(100dvh - var(--app-header-height));
  padding: 56px 32px;
  background: var(--peel-background);
}
.projects-section {
  max-width: 1120px;
  margin: 0 auto;
}
.projects-heading {
  display: flex;
  align-items: center;
  gap: 22px;
  margin-bottom: 28px;
}
.welcome-logo {
  object-fit: contain;
  flex-shrink: 0;
}
.projects-intro {
  flex: 1;
}
h1 {
  font-size: clamp(28px, 4vw, 40px);
  font-weight: 700;
  letter-spacing: -0.04em;
  line-height: 1.2;
  margin-bottom: 8px;
}
.projects-intro p {
  color: var(--peel-muted);
}
.project-collection {
  border: 1px solid var(--peel-border);
  border-radius: 12px;
  overflow: hidden;
  background: var(--peel-surface);
}
.project-row {
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr) minmax(0, 1.25fr) 24px;
  gap: 20px;
  align-items: center;
  min-height: 88px;
  padding: 18px 28px;
  color: var(--peel-text);
  text-decoration: none;
  border-bottom: 1px solid var(--peel-border);
  transition: background 140ms;
}
.project-row:last-child {
  border-bottom: 0;
}
.project-row:hover,
.project-row:focus-visible {
  background: var(--peel-raised);
}
.project-row strong {
  font-size: 16px;
  font-weight: 600;
  overflow-wrap: anywhere;
}
.folder-icon {
  width: 28px;
  height: 28px;
  color: var(--peel-accent);
}
.project-description {
  color: var(--peel-muted);
  font-size: 13px;
}
.open-arrow {
  font-size: 25px;
  color: var(--peel-muted);
}
.project-row:hover .open-arrow {
  color: var(--peel-accent);
}
.create-project {
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr) auto;
  align-items: center;
  gap: 20px;
  padding: 18px 28px;
  border-bottom: 1px solid var(--peel-border);
  background: var(--peel-raised);
}
.new-project-icon {
  font-size: 28px;
  color: var(--peel-muted);
}
.create-project input {
  width: min(100%, 420px);
}
.create-actions {
  display: flex;
  gap: 10px;
}
.project-error {
  color: var(--peel-error);
  grid-column: 2 / -1;
}
.collection-message {
  padding: 32px 28px;
  color: var(--peel-muted);
  display: flex;
  gap: 16px;
  align-items: center;
  flex-wrap: wrap;
}
@media (max-width: 680px) {
  .home-view {
    padding: 28px 16px;
  }
  .projects-heading {
    flex-wrap: wrap;
    gap: 12px;
  }
  .welcome-logo {
    width: 72px;
    height: 72px;
  }
  .projects-intro {
    min-width: 180px;
  }
  .projects-intro p {
    font-size: 13px;
  }
  .project-description {
    display: none;
  }
  .project-row {
    grid-template-columns: 28px minmax(0, 1fr) 24px;
    padding: 18px;
    gap: 14px;
    min-height: 76px;
  }
  .create-project {
    padding: 18px;
    gap: 12px;
    grid-template-columns: 28px minmax(0, 1fr);
  }
  .create-actions {
    grid-column: 2;
  }
  .project-error {
    grid-column: 1 / -1;
  }
}
</style>
