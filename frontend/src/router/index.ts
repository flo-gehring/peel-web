import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import LoginView from '../views/LoginView.vue'
import OneTimeLinkSentView from '../views/OneTimeLinkSentView.vue'
import ProjectView from '../views/ProjectView.vue'
import ConfirmLoginView from '../views/ConfirmLoginView.vue'
import { safeAuthRedirect } from '@/adapter/authRedirect'
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
      path: '/login/sent',
      name: 'one-time-link-sent',
      component: OneTimeLinkSentView,
    },
    {
      path: '/login/confirm',
      name: 'confirm-login',
      component: ConfirmLoginView,
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
      path: '/projects/:projectId/settings',
      name: 'project-settings',
      component: () => import('../views/ProjectSettingsView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/invitations/:token',
      name: 'invitation',
      component: () => import('../views/InvitationView.vue'),
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
  if (to.meta.requiresAuth && !authStore.isLoaded)
    return { name: 'login', query: { redirect: to.fullPath } }
  if (to.meta.requiresAuth && authStore.sessionExpired) return false
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if ((to.name === 'login' || to.name === 'one-time-link-sent') && authStore.isAuthenticated)
    return safeAuthRedirect(to.query.redirect)
})

export default router
