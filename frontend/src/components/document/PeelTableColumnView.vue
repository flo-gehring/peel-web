<script setup lang="ts">
import { computed, ref, onBeforeUnmount } from 'vue'
import { NodeViewWrapper, NodeViewContent, nodeViewProps } from '@tiptap/vue-3'
import { ArrowLeft, ArrowRight, Trash2 } from '@lucide/vue'
import { TextSelection } from '@tiptap/pm/state'
import { useProjectStore } from '@/stores/projects'
import { t } from '@/i18n'

const props = defineProps(nodeViewProps)
const projects = useProjectStore()
const revision = ref(0)
const onTransaction = () => revision.value++
props.editor.on('transaction', onTransaction)
onBeforeUnmount(() => props.editor.off('transaction', onTransaction))
const editable = computed(() => props.editor.isEditable && !projects.isReadOnly)
const position = computed(() => {
  void revision.value
  const pos = props.getPos()
  if (pos === undefined) return null
  const resolved = props.editor.state.doc.resolve(pos)
  if (resolved.parent.type.name !== 'peelTable') return null
  return { pos, table: resolved.parent, index: resolved.index() }
})
function update(name: string, event: Event) {
  if (editable.value) props.updateAttributes({ [name]: (event.target as HTMLInputElement).value })
}
function move(direction: -1 | 1) {
  const current = position.value
  if (!editable.value || !current) return
  const next = current.index + direction
  if (next < 0 || next >= current.table.childCount) return
  const target =
    current.pos +
    (direction < 0 ? -current.table.child(next).nodeSize : current.table.child(next).nodeSize)
  props.editor
    .chain()
    .focus()
    .command(({ tr }) => {
      const node = tr.doc.nodeAt(current.pos)!
      tr.delete(current.pos, current.pos + node.nodeSize).insert(target, node)
      tr.setSelection(TextSelection.near(tr.doc.resolve(target + 1)))
      return true
    })
    .run()
}
function remove() {
  const current = position.value
  if (editable.value && current && current.table.childCount > 1)
    props.editor.commands.deleteRange({ from: current.pos, to: current.pos + props.node.nodeSize })
}
</script>

<template>
  <NodeViewWrapper class="peel-table-column" :style="{ textAlign: node.attrs.align }">
    <div class="column-settings" contenteditable="false" @mousedown.stop @keydown.stop>
      <div class="column-actions">
        <button
          type="button"
          :title="t('traceMoveColumnLeft')"
          :aria-label="t('traceMoveColumnLeft')"
          :disabled="!editable || !position || position.index === 0"
          @click="move(-1)"
        >
          <ArrowLeft :size="14" aria-hidden="true" />
        </button>
        <button
          type="button"
          :title="t('traceMoveColumnRight')"
          :aria-label="t('traceMoveColumnRight')"
          :disabled="!editable || !position || position.index === position.table.childCount - 1"
          @click="move(1)"
        >
          <ArrowRight :size="14" aria-hidden="true" />
        </button>
        <button
          type="button"
          :title="t('traceRemoveColumn')"
          :aria-label="t('traceRemoveColumn')"
          :disabled="!editable || !position || position.table.childCount <= 1"
          @click="remove"
        >
          <Trash2 :size="14" aria-hidden="true" />
        </button>
      </div>
      <label
        >{{ t('traceColumnTitle')
        }}<input :value="node.attrs.title" :disabled="!editable" @change="update('title', $event)"
      /></label>
      <label
        >{{ t('traceColumnAlign')
        }}<select :value="node.attrs.align" :disabled="!editable" @change="update('align', $event)">
          <option value="left">{{ t('traceAlignLeft') }}</option>
          <option value="center">{{ t('traceAlignCenter') }}</option>
          <option value="right">{{ t('traceAlignRight') }}</option>
        </select></label
      >
    </div>
    <NodeViewContent class="peel-table-cell" />
  </NodeViewWrapper>
</template>

<style scoped>
.peel-table-column {
  flex: 1 0 180px;
  min-width: 0;
  border: 1px solid #b8b8b8;
}
.column-settings {
  padding: 6px;
  border-bottom: 1px dashed #b8b8b8;
  text-align: left;
}
.column-actions {
  display: flex;
  justify-content: flex-end;
  gap: 2px;
}
button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px;
  color: inherit;
  background: transparent;
  border: 0;
  cursor: pointer;
}
button:hover:not(:disabled) {
  background: rgba(128, 128, 128, 0.15);
}
button:disabled {
  opacity: 0.4;
  cursor: default;
}
label {
  display: flex;
  flex-direction: column;
  gap: 3px;
  margin-top: 5px;
  font-size: 11px;
}
input,
select {
  box-sizing: border-box;
  width: 100%;
  min-width: 0;
  padding: 5px;
  color: #ddd;
  background: #202226;
  border: 1px solid #484b50;
  border-radius: 3px;
}
.peel-table-cell {
  padding: 8px;
  min-height: 50px;
}
</style>
