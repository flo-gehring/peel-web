<script setup lang="ts">
import { t } from '@/i18n'
</script>

<template>
  <div class="editor-welcome">
    <div>
      <span class="welcome-symbol" aria-hidden="true">{ }</span>
      <h1>{{ t('readyWhenYouAre') }}</h1>
      <p>{{ t('openAFileInTheExplorerToStartEditing') }}</p>
      <p>{{ t('createAScriptDocumentOrRenderConfigurationWithSymbol', { symbol: '＋' }) }}</p>
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
