<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { NodeViewWrapper, NodeViewContent, nodeViewProps } from '@tiptap/vue-3'
import { api } from '@/adapter/client'
import { apiErrorMessage } from '@/adapter/apiError'
import { useProjectStore } from '@/stores/projects'
import { t } from '@/i18n'

const props = defineProps(nodeViewProps)
const projects = useProjectStore()
const blocks = ref<string[]>([])
const error = ref('')
const loading = ref(false)
let generation = 0
const repeat = computed(() => props.node.type.name === 'peelRepeat')
const inline = computed(() => props.node.type.name === 'peelValue')
const title = computed(() =>
  t(repeat.value ? 'traceRepeat' : inline.value ? 'traceValue' : 'traceCalculation'),
)
const editable = computed(() => props.editor.isEditable && !projects.isReadOnly)
const scripts = computed(() => projects.activeProject?.scripts ?? [])
const inRepeat = computed(() => {
  const pos = props.getPos()
  if (pos === undefined) return false
  const resolved = props.editor.state.doc.resolve(pos)
  for (let depth = resolved.depth; depth > 0; depth--) {
    if (resolved.node(depth).type.name === 'peelRepeat') return true
  }
  return false
})
const sourceScriptId = computed(() => {
  if (props.node.attrs.scope === 'script') return props.node.attrs.scriptId as string
  const pos = props.getPos()
  if (pos === undefined) return ''
  const resolved = props.editor.state.doc.resolve(pos)
  for (let depth = resolved.depth; depth > 0; depth--) {
    const ancestor = resolved.node(depth)
    if (ancestor.type.name === 'peelRepeat' && ancestor.attrs.scope === 'script')
      return ancestor.attrs.scriptId as string
  }
  return ''
})

async function loadBlocks() {
  const request = ++generation
  blocks.value = []
  error.value = ''
  loading.value = false
  const projectId = projects.activeProject?.projectIdApi?.id
  const scriptId = sourceScriptId.value
  if (!projectId || !scriptId) return
  loading.value = true
  try {
    const { data, error: failure } = await api.GET(
      '/api/projects/{projectId}/files/{fileId}/blocks',
      {
        params: { path: { projectId, fileId: scriptId } },
      },
    )
    if (request !== generation) return
    if (failure) error.value = apiErrorMessage(failure, t('traceBlocksFailed'))
    else blocks.value = data ?? []
  } catch (failure) {
    if (request === generation)
      error.value = failure instanceof Error ? failure.message : t('traceBlocksFailed')
  } finally {
    if (request === generation) loading.value = false
  }
}
watch(() => [projects.activeProject?.projectIdApi?.id, sourceScriptId.value], loadBlocks, {
  immediate: true,
})
function update(name: string, event: Event) {
  if (!editable.value) return
  const value = (event.target as HTMLInputElement).value
  props.updateAttributes({
    [name]: name === 'decimals' ? (value === '' ? null : Number(value)) : value,
  })
}
function changeScript(event: Event) {
  if (editable.value)
    props.updateAttributes({ scriptId: (event.target as HTMLSelectElement).value, block: '' })
}
</script>

<template>
  <NodeViewWrapper
    :as="inline ? 'span' : 'div'"
    class="peel-node"
    :class="{ 'peel-inline': inline, selected }"
  >
    <details :open="!inline" contenteditable="false" class="peel-node-settings">
      <summary data-drag-handle>
        {{ title }} <span v-if="node.attrs.block">· {{ node.attrs.block }}</span>
      </summary>
      <fieldset :disabled="!editable" @mousedown.stop @keydown.stop>
        <label
          >{{ t('traceScope') }}
          <select :value="node.attrs.scope" @change="update('scope', $event)">
            <option value="script">{{ t('traceScriptScope') }}</option>
            <option v-if="inRepeat || node.attrs.scope === 'current'" value="current">
              {{ t('traceCurrentScope') }}
            </option>
          </select>
        </label>
        <label v-if="node.attrs.scope === 'script'"
          >{{ t('script') }}
          <select :value="node.attrs.scriptId" @change="changeScript">
            <option value="">{{ t('traceChooseScript') }}</option>
            <option
              v-if="
                node.attrs.scriptId && !scripts.some((script) => script.id === node.attrs.scriptId)
              "
              :value="node.attrs.scriptId"
            >
              {{ node.attrs.scriptId }}
            </option>
            <option v-for="script in scripts" :key="script.id" :value="script.id">
              {{ script.name }}
            </option>
          </select>
        </label>
        <label
          >{{ t('traceBlock') }}
          <select :value="node.attrs.block" @change="update('block', $event)">
            <option value="">
              {{ t(node.attrs.scope === 'current' ? 'traceCurrentBlock' : 'traceWholeScript') }}
            </option>
            <option
              v-if="node.attrs.block && !blocks.includes(node.attrs.block)"
              :value="node.attrs.block"
            >
              {{ node.attrs.block }}
            </option>
            <option v-for="block in blocks" :key="block" :value="block">{{ block }}</option>
          </select>
        </label>
        <label v-if="!inline && !repeat"
          >{{ t('traceDisplay') }}
          <select :value="node.attrs.mode" @change="update('mode', $event)">
            <option value="detailed">{{ t('traceDetailed') }}</option>
            <option value="compact">{{ t('traceCompact') }}</option>
            <option value="result">{{ t('traceResult') }}</option>
          </select>
        </label>
        <label v-if="inline"
          >{{ t('traceBinding') }}
          <input
            :value="node.attrs.binding"
            :placeholder="t('traceBindingPlaceholder')"
            @change="update('binding', $event)"
          />
        </label>
        <label v-if="!repeat"
          >{{ t('traceDecimals') }}
          <input
            type="number"
            min="0"
            max="12"
            :value="node.attrs.decimals"
            :placeholder="t('traceAutomatic')"
            @change="update('decimals', $event)"
          />
        </label>
        <button type="button" :disabled="loading || !sourceScriptId" @click="loadBlocks">
          {{ t('refreshFiles') }}
        </button>
        <button type="button" @click="deleteNode">{{ t('remove') }}</button>
      </fieldset>
      <p v-if="error" role="alert" class="trace-error">{{ error }}</p>
      <p class="trace-hint">{{ t(repeat ? 'traceRepeatHint' : 'tracePreviewHint') }}</p>
    </details>
    <NodeViewContent v-if="repeat" class="peel-repeat-content" />
  </NodeViewWrapper>
</template>

<style scoped>
.peel-node {
  display: block;
  margin: 12px 0;
  border: 1px solid #53513d;
  border-radius: 5px;
  background: #292a25;
}
.peel-node.selected {
  outline: 1px solid var(--peel-accent);
}
.peel-inline {
  display: inline-block;
  vertical-align: baseline;
  margin: 0 3px;
  font-size: 12px;
}
.peel-node-settings {
  padding: 8px;
}
summary {
  cursor: pointer;
  color: var(--peel-accent);
  font-size: 12px;
}
fieldset {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  border: 0;
  margin: 8px 0 0;
  padding: 0;
}
label {
  display: flex;
  flex-direction: column;
  gap: 3px;
  font-size: 11px;
  color: #bbb;
}
input,
select,
button {
  color: #ddd;
  background: #202226;
  border: 1px solid #484b50;
  padding: 5px;
  border-radius: 3px;
  max-width: 240px;
}
button {
  align-self: end;
  cursor: pointer;
}
.trace-hint {
  margin: 8px 0 0;
  color: #999;
  font-size: 11px;
}
.trace-error {
  color: #f48771;
  font-size: 12px;
}
.peel-repeat-content {
  padding: 8px 12px;
  border-top: 1px dashed #53513d;
}
</style>
