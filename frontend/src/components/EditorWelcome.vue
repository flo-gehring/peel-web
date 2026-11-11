<template>
  <div class="editor-welcome">
    <div>
      <span class="welcome-symbol" aria-hidden="true">{ }</span>
      <h1>Ready when you are</h1>
      <p>Open a file in the explorer to start editing.</p>
      <p>Create a script, document or render configuration with <strong>＋</strong>.</p>
    </div>
  </div>
</template>

<style scoped>
.editor-welcome {
  display: grid;
  place-items: center;
  height: 100%;
  padding: 32px;
  color: var(--peel-muted);
  background: var(--peel-background);
  text-align: center;
}
.welcome-symbol {
  display: block;
  margin-bottom: 20px;
  font:
    36px ui-monospace,
    monospace;
  color: var(--peel-accent);
}
h1 {
  color: var(--peel-text);
  font-size: 22px;
  font-weight: 600;
  margin-bottom: 12px;
}
p {
  margin-top: 8px;
  max-width: 380px;
}
</style>
