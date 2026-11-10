<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useProjectStore } from '@/stores/projects'

const router = useRouter()
const projectStore = useProjectStore()
const newProjectName = ref('')
const error = ref<string | null>(null)

async function loadProjects(): Promise<void> {
  try {
    await projectStore.loadProjects()
  } catch {
    error.value = 'Projects could not be loaded. Check your connection and try again.'
  }
}

async function createProject(): Promise<void> {
  const name = newProjectName.value.trim()
  if (!name) return
  error.value = null
  try {
    const project = await projectStore.createProject(name)
    const id = project.projectIdApi?.id
    if (id) await router.push({ name: 'project', params: { projectId: id } })
  } catch {
    error.value = 'The project could not be created.'
  }
}

onMounted(loadProjects)
</script>

<template>
  <main class="home-view">
    <section>
      <p class="eyebrow">Peel</p>
      <h1>Projects</h1>
      <form class="create-project" @submit.prevent="createProject">
        <input v-model="newProjectName" aria-label="New project name" placeholder="New project name" />
        <button type="submit">Create project</button>
      </form>
      <p v-if="error" class="error">{{ error }}</p>
      <nav aria-label="Projects">
        <RouterLink
          v-for="project in projectStore.projects"
          :key="project.projectIdApi?.id"
          class="workspace-card"
          :to="{ name: 'project', params: { projectId: project.projectIdApi?.id } }"
        >
          <span class="card-number">Project</span>
          <strong>{{ project.name }}</strong>
          <span>Documents, scripts, and render configurations</span>
        </RouterLink>
      </nav>
    </section>
  </main>
</template>

<style scoped>
.home-view {
  display: grid;
  min-height: 100vh;
  padding: 48px 24px;
  place-items: center;
  background: #17191d;
  color: #f2f4f6;
}

section {
  width: min(860px, 100%);
}

.eyebrow,
.card-number {
  color: #89c6ff;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 0.78rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

h1 {
  margin: 4px 0 34px;
  font-size: clamp(2.4rem, 8vw, 5rem);
  letter-spacing: -0.06em;
}

nav {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.create-project { display: flex; gap: 8px; margin: -18px 0 24px; }
.create-project input, .create-project button { padding: 9px 10px; border: 1px solid #3a4049; background: #20242a; color: inherit; }
.create-project button { cursor: pointer; }
.error { color: #f48771; }

.workspace-card {
  display: grid;
  min-height: 230px;
  padding: 22px;
  align-content: start;
  gap: 18px;
  border: 1px solid #3a4049;
  background: #20242a;
  color: inherit;
  text-decoration: none;
  transition: border-color 160ms ease, background 160ms ease, transform 160ms ease;
}

.workspace-card:hover,
.workspace-card:focus-visible {
  border-color: #89c6ff;
  background: #272d35;
  transform: translateY(-3px);
}

.workspace-card strong {
  font-size: 1.65rem;
}

.workspace-card span:last-child {
  color: #b2bbc7;
}

@media (max-width: 600px) {
  nav {
    grid-template-columns: 1fr;
  }
}
</style>
