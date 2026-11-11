<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRunOutputStore } from '@/stores/runOutput'

const output = useRunOutputStore()
const activeTab = ref<'Result' | 'Request' | 'Trace'>('Result')
const tabs = ['Result', 'Request', 'Trace'] as const
const statusLabel = computed(() =>
  output.isRunning
    ? 'Running…'
    : output.error
      ? 'Run failed'
      : output.response
        ? 'Run completed'
        : 'Ready to run',
)
const formattedContent = computed(() => {
  if (activeTab.value === 'Request') return JSON.stringify(output.request ?? {}, null, 2)
  if (activeTab.value === 'Trace') return JSON.stringify(output.response?.trace ?? null, null, 2)
  return JSON.stringify(output.response?.result ?? null, null, 2)
})
const lastRun = computed(() =>
  output.lastRunAt ? new Date(output.lastRunAt).toLocaleTimeString() : '',
)
</script>

<template>
  <section class="output-panel" aria-label="Run output">
    <div class="output-toolbar">
      <div class="output-tabs" role="group" aria-label="Output view">
        <button
          v-for="tab in tabs"
          :key="tab"
          :aria-pressed="activeTab === tab"
          :class="{ selected: activeTab === tab }"
          @click="activeTab = tab"
        >
          {{ tab }}
        </button>
      </div>
      <span
        class="output-status"
        :class="{
          failed: !!output.error,
          complete: !!output.response && !output.error && !output.isRunning,
        }"
        role="status"
        ><span aria-hidden="true" class="status-dot"></span>{{ statusLabel }}</span
      >
      <time v-if="lastRun" class="run-time">{{ lastRun }}</time>
    </div>
    <div class="output-content">
      <pre v-if="output.error" class="output-error" role="alert">{{ output.error }}</pre>
      <p v-if="!output.request && !output.response" class="output-empty">
        Run a script to see its result here.
      </p>
      <pre v-else class="json-block">{{ formattedContent }}</pre>
    </div>
  </section>
</template>

<style scoped>
.output-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: var(--peel-background);
  color: var(--peel-text);
}
.output-toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  min-height: 40px;
  flex-wrap: wrap;
  border-bottom: 1px solid var(--peel-border);
  background: var(--peel-surface);
  padding-right: 14px;
}
.output-tabs {
  display: flex;
  align-self: stretch;
  margin-right: auto;
}
.output-tabs button {
  border: 0;
  border-radius: 0;
  background: transparent;
  color: var(--peel-muted);
  font-size: 12px;
  padding: 10px 16px;
}
.output-tabs button.selected {
  color: var(--peel-text);
  box-shadow: inset 0 -2px var(--peel-accent);
}
.output-status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--peel-muted);
}
.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: currentColor;
}
.complete {
  color: #a6e3a1;
}
.failed,
.output-error {
  color: var(--peel-error);
}
.run-time {
  font-size: 11px;
  color: var(--peel-muted);
}
.output-content {
  flex: 1;
  overflow: auto;
  min-height: 0;
  padding: 16px 20px;
}
.json-block,
.output-error {
  margin: 0;
  font:
    13px/1.7 'Cascadia Code',
    Consolas,
    monospace;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
.output-error {
  padding-bottom: 16px;
}
.output-empty {
  color: var(--peel-muted);
  font-size: 13px;
}
</style>
