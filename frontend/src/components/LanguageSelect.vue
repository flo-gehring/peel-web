<script setup lang="ts">
import { locale, setLocale, t } from '@/i18n'
</script>

<template>
  <select
    class="language-select"
    :value="locale"
    :aria-label="t('language')"
    :title="t('language')"
    @change="setLocale(($event.target as HTMLSelectElement).value as 'de' | 'en')"
  >
    <option value="de" lang="de">Deutsch</option>
    <option value="en" lang="en">English</option>
  </select>
</template>

<style scoped>
.language-select {
  width: auto;
  flex-shrink: 0;
  padding: 6px 8px;
  color: var(--peel-text);
  background: var(--peel-surface);
  border: 1px solid var(--peel-border);
  border-radius: 6px;
  font-size: 12px;
}
</style>
