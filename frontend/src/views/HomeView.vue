<template>
  <main class="home-view">
    <section>
      <p class="eyebrow">Peel</p>
      <h1>Arbeitsbereiche</h1>
      <nav aria-label="Arbeitsbereiche">
        <RouterLink class="workspace-card" to="/project">
          <span class="card-number">01</span>
          <strong>Project</strong>
          <span>Dokumente, Skripte und Vorschauen bearbeiten</span>
        </RouterLink>
        <RouterLink class="workspace-card" to="/print-jobs">
          <span class="card-number">02</span>
          <strong>Druckaufträge</strong>
          <span>Druckdateien zu Dokumenten verwalten</span>
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
