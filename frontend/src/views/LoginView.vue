<script setup lang="ts">
import { ref } from 'vue'

const email = ref('')
</script>

<template>
  <main class="login-view">
    <form class="login-card" method="post" action="/ott/generate">
      <p class="eyebrow">Peel</p>
      <h1>Sign in</h1>
      <p>Enter your email address and we will send you a one-time sign-in link.</p>
      <label>
        Email
        <input v-model="email" name="username" type="email" autocomplete="email" required />
      </label>
      <button type="submit">Send sign-in link</button>
    </form>
  </main>
</template>

<style scoped>
.login-view { display: grid; min-height: 100vh; place-items: center; padding: 24px; background: #17191d; color: #f2f4f6; }
.login-card { display: grid; width: min(420px, 100%); gap: 16px; padding: 28px; border: 1px solid #3a4049; background: #20242a; }
.eyebrow { margin: 0; color: #89c6ff; font-family: ui-monospace, monospace; font-size: 0.78rem; letter-spacing: 0.12em; text-transform: uppercase; }
h1, p { margin: 0; }
label { display: grid; gap: 6px; }
input, button { padding: 10px; border: 1px solid #3a4049; background: #17191d; color: inherit; }
button { cursor: pointer; background: #276da3; border-color: #3d8dcc; }
</style>
