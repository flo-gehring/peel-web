<script setup lang="ts">
import { computed, ref, watch, onBeforeUnmount } from 'vue'
import { NodeViewWrapper, NodeViewContent, nodeViewProps } from '@tiptap/vue-3'
import { RefreshCw, Trash2 } from '@lucide/vue'
import { api } from '@/adapter/client'
import { apiErrorMessage } from '@/adapter/apiError'
import { useProjectStore } from '@/stores/projects'
import { t } from '@/i18n'
import type { components } from '@/types/api'

const props = defineProps(nodeViewProps)
const projects = useProjectStore()
const catalog = ref<components['schemas']['BlockDefinition'][]>([])
const transaction = ref(0)
const onTransaction = () => transaction.value++
props.editor.on('transaction', onTransaction)
onBeforeUnmount(() => props.editor.off('transaction', onTransaction))
const error = ref('')
const loading = ref(false)
let generation = 0
const table = computed(() => props.node.type.name === 'peelTable')
const repeat = computed(() => props.node.type.name === 'peelRepeat' || table.value)
const inline = computed(() => props.node.type.name === 'peelValue')
const title = computed(() =>
  t(
    table.value
      ? 'traceTable'
      : repeat.value
        ? 'traceRepeat'
        : inline.value
          ? 'traceValue'
          : 'traceCalculation',
  ),
)
const editable = computed(() => props.editor.isEditable && !projects.isReadOnly)
const scripts = computed(() => projects.activeProject?.scripts ?? [])
const repeatContext = computed(() => {
  void transaction.value
  const pos = props.getPos()
  if (pos === undefined) return null
  const resolved = props.editor.state.doc.resolve(pos)
  const context = { scriptId: '', block: '' }
  let found = false
  for (let depth = 1; depth <= resolved.depth; depth++) {
    const ancestor = resolved.node(depth)
    if (!['peelRepeat', 'peelTable'].includes(ancestor.type.name)) continue
    found = true
    if (ancestor.attrs.scope === 'script') {
      context.scriptId = ancestor.attrs.scriptId
      context.block = ancestor.attrs.block
    } else if (ancestor.attrs.block) {
      context.block = ancestor.attrs.block
    }
  }
  return found ? context : null
})
const inRepeat = computed(() => repeatContext.value !== null)
const sourceScriptId = computed(() => {
  if (props.node.attrs.scope === 'script') return props.node.attrs.scriptId as string
  return repeatContext.value?.scriptId ?? ''
})
const blocks = computed(() => {
  if (props.node.attrs.scope !== 'current' || !repeatContext.value?.block)
    return catalog.value.map((block) => block.name)
  const parent = catalog.value.find((block) => block.name === repeatContext.value?.block)
  return parent?.reachableBlocks ?? []
})

async function loadBlocks() {
  const request = ++generation
  catalog.value = []
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
    else catalog.value = data ?? []
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
function addColumn() {
  const pos = props.getPos()
  if (!editable.value || pos === undefined) return
  props.editor.commands.insertContentAt(pos + props.node.nodeSize - 1, {
    type: 'peelTableColumn',
    attrs: { title: t('traceColumnTitle'), align: 'left' },
    content: [{ type: 'paragraph' }],
  })
}
</script>

<template>
  <NodeViewWrapper
    :as="inline ? 'span' : 'div'"
    class="peel-node"
    :class="{ 'peel-inline': inline, selected }"
  >
    <button
      type="button"
      class="node-remove"
      contenteditable="false"
      :title="t('remove')"
      :aria-label="t('remove')"
      :disabled="!editable"
      @mousedown.stop
      @keydown.stop
      @click.stop="deleteNode"
    >
      <Trash2 :size="14" aria-hidden="true" />
    </button>
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
        <div class="block-field">
          <label
            >{{ t('traceBlock') }}
            <select :value="node.attrs.block" @change="update('block', $event)">
              <option value="">
                {{ t(node.attrs.scope === 'current' ? 'traceCurrentBlock' : 'traceWholeScript') }}
              </option>
              <option
                v-if="node.attrs.block && !blocks.includes(node.attrs.block)"
                :value="node.attrs.block"
                disabled
              >
                {{ node.attrs.block }}
              </option>
              <option v-for="block in blocks" :key="block" :value="block">{{ block }}</option>
            </select>
          </label>
          <button
            type="button"
            class="block-refresh"
            :title="t('traceRefreshBlocks')"
            :aria-label="t('traceRefreshBlocks')"
            :disabled="loading || !sourceScriptId"
            @click="loadBlocks"
          >
            <RefreshCw :size="14" aria-hidden="true" />
          </button>
        </div>
        <label v-if="!inline && !repeat"
          >{{ t('traceDisplay') }}
          <select :value="node.attrs.mode" @change="update('mode', $event)">
            <option value="detailed">{{ t('traceDetailed') }}</option>
            <option value="compact">{{ t('traceCompact') }}</option>
            <option value="substituted">{{ t('traceSubstituted') }}</option>
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
      </fieldset>
      <p v-if="error" role="alert" class="trace-error">{{ error }}</p>
      <p class="trace-hint">
        {{
          t(
            table
              ? 'traceTableHint'
              : inline
                ? 'traceVariableHint'
                : repeat
                  ? 'traceRepeatHint'
                  : 'tracePreviewHint',
          )
        }}
      </p>
      <button v-if="table" type="button" :disabled="!editable" @click="addColumn">
        {{ t('traceAddColumn') }}
      </button>
    </details>
    <div v-if="table" class="peel-table-scroll"><NodeViewContent class="peel-table-columns" /></div>
    <NodeViewContent v-else-if="repeat" class="peel-repeat-content" />
  </NodeViewWrapper>
</template>

<style scoped>
.peel-node {
  position: relative;
  display: block;
  margin: 12px 0;
  border: 1px solid #53513d;
  border-radius: 5px;
  background: transparent;
  color: inherit;
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
  border-radius: 4px;
  background: transparent;
  color: inherit;
}
summary {
  padding-right: 26px;
  cursor: pointer;
  color: inherit;
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
  color: inherit;
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
.block-field {
  display: flex;
  align-items: end;
  gap: 4px;
}
.block-refresh,
.node-remove {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  padding: 4px;
  color: inherit;
  background: transparent;
  border-color: transparent;
}
.node-remove {
  position: absolute;
  top: 4px;
  right: 4px;
}
.block-refresh:hover:not(:disabled),
.node-remove:hover:not(:disabled) {
  background: rgba(128, 128, 128, 0.15);
}
.block-refresh:disabled,
.node-remove:disabled {
  opacity: 0.4;
  cursor: default;
}
.trace-hint {
  margin: 8px 0 0;
  color: inherit;
  opacity: 0.7;
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
.peel-table-scroll {
  overflow-x: auto;
  padding: 8px;
}
.peel-table-columns {
  display: flex;
}
</style>
