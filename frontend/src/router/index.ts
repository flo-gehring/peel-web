import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import LoginView from '../views/LoginView.vue'
import OneTimeLinkSentView from '../views/OneTimeLinkSentView.vue'
import ProjectView from '../views/ProjectView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView,
    },
    {
      path: '/ott/sent',
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
    },
    {
      path: '/print-jobs',
      name: 'print-jobs',
      component: () => import('../views/PrintJobsView.vue'),
    },
    {
      path: '/print-jobs/:id',
      name: 'print-job-detail',
      component: () => import('../views/PrintJobDetailView.vue'),
    },
  ],
})

export default router
