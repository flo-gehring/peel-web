<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import SignInForm from '@/components/SignInForm.vue'
import { safeAuthRedirect } from '@/adapter/authRedirect'
const route = useRoute()
const router = useRouter()
</script>

<template>
  <main class="auth-page">
    <SignInForm
      :redirect="route.query.redirect"
      @sent="
        router.replace({
          name: 'one-time-link-sent',
          query: { redirect: safeAuthRedirect(route.query.redirect) },
        })
      "
    />
  </main>
</template>
