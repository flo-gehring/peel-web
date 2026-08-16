<script setup lang="ts">
import { computed } from 'vue'
import { useRunOutputStore } from '@/stores/runOutput'

const runOutputStore = useRunOutputStore()

const statusLabel = computed(() => {
  if (runOutputStore.isRunning) return 'Running...'
  if (runOutputStore.error) return 'Error'
  if (runOutputStore.response) return 'Success'
  return 'Idle'
})

const formattedResult = computed(() => {
  const payload = {
    trace: runOutputStore.response?.trace ?? null,
    result: runOutputStore.response?.result ?? null,
  }
  return JSON.stringify(payload, null, 2)
})

const formattedRequest = computed(() => {
  if (!runOutputStore.request) {
    return '{}'
  }

  return JSON.stringify(
    {
      name: runOutputStore.request.name,
      panelId: runOutputStore.request.panelId,
      script: runOutputStore.request.script,
      bindings: runOutputStore.request.bindings,
    },
    null,
    2,
  )
})

const formattedRunAt = computed(() => {
  if (!runOutputStore.lastRunAt) {
    return '-'
  }

  return new Date(runOutputStore.lastRunAt).toLocaleTimeString()
})
</script>

<template>
  <div class="output-panel">
    <div class="output-header">CONSOLE OUTPUT</div>
    <div class="output-logs">
      <p><span class="label">Status:</span> <span class="value">{{ statusLabel }}</span></p>
      <p><span class="label">Last Run:</span> <span class="value">{{ formattedRunAt }}</span></p>

      <p class="section-title">Request</p>
      <pre class="json-block">{{ formattedRequest }}</pre>

      <p v-if="runOutputStore.error" class="section-title error-title">Error</p>
      <pre v-if="runOutputStore.error" class="json-block error-block">{{ runOutputStore.error }}</pre>

      <p class="section-title">Result</p>
      <pre class="json-block">{{ formattedResult }}</pre>
    </div>
  </div>
</template>

<style scoped>
.output-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  min-height: 0;
  background-color: #181818;
  color: #cccccc;
  font-family: monospace;
  font-size: 12px;
}
.output-header {
  background: #252526;
  padding: 4px 10px;
  font-weight: bold;
  font-size: 11px;
  color: #888888;
}
.output-logs {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 8px 12px;
}

.label {
  color: #888;
}

.value {
  color: #d4d4d4;
}

.section-title {
  margin: 10px 0 6px;
  color: #9cdcfe;
}

.error-title {
  color: #f48771;
}

.json-block {
  margin: 0;
  padding: 8px;
  border: 1px solid #2b2b2b;
  border-radius: 4px;
  background: #1f1f1f;
  color: #d4d4d4;
  white-space: pre-wrap;
  word-break: break-word;
}

.error-block {
  border-color: #6a2a2a;
  color: #ffb4a9;
}
</style>
