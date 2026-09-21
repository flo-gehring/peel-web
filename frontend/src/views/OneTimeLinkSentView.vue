<template>
  <main class="sent-view">
    <section>
      <p class="eyebrow">Peel</p>
      <h1>Check the server log</h1>
      <p>Your one-time sign-in link has been generated. Copy it from the backend log to continue.</p>
      <RouterLink to="/login">Use another email</RouterLink>
    </section>
  </main>
</template>

<style scoped>
.sent-view { display: grid; min-height: 100vh; place-items: center; padding: 24px; background: #17191d; color: #f2f4f6; }
section { display: grid; width: min(420px, 100%); gap: 16px; padding: 28px; border: 1px solid #3a4049; background: #20242a; }
.eyebrow { margin: 0; color: #89c6ff; font-family: ui-monospace, monospace; font-size: .78rem; letter-spacing: .12em; text-transform: uppercase; }
h1, p { margin: 0; }
a { color: #89c6ff; }
</style>
