import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import LoginView from '../views/LoginView.vue'
import OneTimeLinkSentView from '../views/OneTimeLinkSentView.vue'
import ProjectView from '../views/ProjectView.vue'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
      meta: { requiresAuth: true },
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView,
    },
    {
      path: '/one-time-link-sent',
      name: 'one-time-link-sent',
      component: OneTimeLinkSentView,
    },
    {
      path: '/project',
      redirect: '/',
    },
    {
      path: '/projects/:projectId',
      name: 'project',
      component: ProjectView,
      meta: { requiresAuth: true },
    },
    {
      path: '/print-jobs',
      name: 'print-jobs',
      component: () => import('../views/PrintJobsView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/print-jobs/:id',
      name: 'print-job-detail',
      component: () => import('../views/PrintJobDetailView.vue'),
      meta: { requiresAuth: true },
    },
  ],
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()
  if (!authStore.isLoaded) await authStore.refresh()
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'login' && authStore.isAuthenticated) return { name: 'home' }
})

export default router
