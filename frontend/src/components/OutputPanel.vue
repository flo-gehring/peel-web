<script setup lang="ts"></script>

<template>
  <div class="output-panel">
    <div class="output-header">CONSOLE OUTPUT</div>
    <div class="output-logs">
      <p>
        <span class="time">[10:00:01]</span> Connected to Spring Boot REST API at
        http://localhost:8080
      </p>
      <p><span class="time">[10:00:02]</span> Initialized Dockview layout engine.</p>
    </div>
  </div>
</template>

<style scoped>
.output-panel {
  height: 100%;
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
  padding: 8px 12px;
}
.time {
  color: #569cd6;
}
</style>
