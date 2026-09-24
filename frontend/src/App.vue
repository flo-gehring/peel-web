<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

async function refreshSession(): Promise<void> {
  await authStore.refresh()
  if (!authStore.isAuthenticated && router.currentRoute.value.meta.requiresAuth) {
    await router.replace({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
  }
}

onMounted(refreshSession)
</script>

<template>
  <RouterView />
  <RouterLink class="account-button" :to="authStore.isAuthenticated ? '/' : '/login'">
    {{ authStore.isAuthenticated ? authStore.email : 'Login' }}
  </RouterLink>
</template>

<style>
.account-button { position: fixed; top: 16px; right: 18px; z-index: 200; padding: 7px 10px; border: 1px solid #4d5865; background: #20242a; color: #edf1f5; font: 12px ui-monospace, monospace; text-decoration: none; }
</style>
