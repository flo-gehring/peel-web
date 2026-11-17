<script setup lang="ts">
import { useRoute } from 'vue-router'
import SignInForm from '@/components/SignInForm.vue'
const route = useRoute()
</script>

<template>
  <main class="auth-page"><SignInForm initially-sent :redirect="route.query.redirect" /></main>
</template>
