import { ref } from 'vue'
import { defineStore } from 'pinia'
import { api } from '@/adapter/client'

export const useAuthStore = defineStore('auth', () => {
  const isLoaded = ref(false)
  const isAuthenticated = ref(false)
  const email = ref<string | null>(null)
  const userId = ref<string | null>(null)

  async function refresh(): Promise<void> {
    const { data } = await api.GET('/api/whoami')
    isAuthenticated.value = data?.authenticated === true
    email.value = data?.email ?? null
    userId.value = data?.userId ?? null
    isLoaded.value = true
  }

  return { isLoaded, isAuthenticated, email, userId, refresh }
})
