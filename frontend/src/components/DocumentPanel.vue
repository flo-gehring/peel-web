<script setup lang="ts">
import { computed, ref } from 'vue'
import type { JSONContent } from '@tiptap/core'
import { EditorContent, useEditor } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import { api } from '@/adapter/client'
import type { DocumentDraft } from '@/stores/documentDrafts'
import { useDocumentDraftStore } from '@/stores/documentDrafts'
import { useDocumentPreviewStore } from '@/stores/documentPreview'
import { useRunBindingsStore } from '@/stores/runBindings'

const props = defineProps<{
  params?: {
    documentId?: string
    params?: {
      documentId?: string
      draft?: DocumentDraft
    }
  }
}>()

const documentId = props.params?.params?.documentId || props.params?.documentId || ''
const initialDraft = props.params?.params?.draft
const draftStore = useDocumentDraftStore()
const previewStore = useDocumentPreviewStore()
const bindingsStore = useRunBindingsStore()
const draft = computed(() => draftStore.getDraft(documentId) ?? initialDraft)
const isSaving = ref(false)
const saveError = ref<string | null>(null)
const isPreviewing = ref(false)

const editor = useEditor({
  extensions: [StarterKit],
  content: parseEditorState(draft.value?.editorStateJson),
  onUpdate: ({ editor }) => {
    draftStore.updateDraft(documentId, {
      editorStateJson: JSON.stringify(editor.getJSON()),
      templateHtml: editor.getHTML(),
    })
  },
})

function toggleHeading(level: 1 | 2 | 3): void {
  editor.value?.chain().focus().toggleHeading({ level }).run()
}

function toggleBold(): void {
  editor.value?.chain().focus().toggleBold().run()
}

function toggleItalic(): void {
  editor.value?.chain().focus().toggleItalic().run()
}

function toggleBulletList(): void {
  editor.value?.chain().focus().toggleBulletList().run()
}

function toggleOrderedList(): void {
  editor.value?.chain().focus().toggleOrderedList().run()
}

function toggleBlockquote(): void {
  editor.value?.chain().focus().toggleBlockquote().run()
}

function parseEditorState(editorStateJson: string | undefined): JSONContent {
  if (!editorStateJson) return { type: 'doc', content: [{ type: 'paragraph' }] }
  try {
    return JSON.parse(editorStateJson) as JSONContent
  } catch {
    return { type: 'doc', content: [{ type: 'paragraph' }] }
  }
}

function addScriptLink(): void {
  const current = draft.value
  if (!current) return
  draftStore.updateDraft(documentId, {
    scriptNameTags: { ...current.scriptNameTags, [`script-${Date.now()}`]: '' },
  })
}

function updateScriptLink(previousTag: string, tag: string, scriptId: string): void {
  const current = draft.value
  if (!current) return
  const scriptNameTags = { ...current.scriptNameTags }
  delete scriptNameTags[previousTag]
  scriptNameTags[tag] = scriptId
  draftStore.updateDraft(documentId, { scriptNameTags })
}

function removeScriptLink(tag: string): void {
  const current = draft.value
  if (!current) return
  const scriptNameTags = { ...current.scriptNameTags }
  delete scriptNameTags[tag]
  draftStore.updateDraft(documentId, { scriptNameTags })
}

async function saveDocument(): Promise<void> {
  const current = draft.value
  if (!current || isSaving.value) return

  isSaving.value = true
  saveError.value = null
  try {
    const { error } = await api.POST('/documents', {
      body: {
        id: documentId,
        name: current.name,
        scriptNameTags: current.scriptNameTags,
        templateHtml: current.templateHtml,
        editorStateJson: current.editorStateJson,
        renderConfigurationId: current.renderConfigurationId,
      },
    })
    if (error) {
      saveError.value = 'The document could not be saved. Ensure a render configuration is selected.'
      return
    }
    draftStore.markSaved(documentId)
  } catch {
    saveError.value = 'An unexpected error occurred while saving.'
  } finally {
    isSaving.value = false
  }
}

async function previewDocument(): Promise<void> {
  const current = draft.value
  if (!current || isPreviewing.value) return

  const bindings = bindingsStore.parseBindings()
  if (!bindings) {
    saveError.value = bindingsStore.parseError || 'Invalid bindings JSON.'
    return
  }
  if (!current.renderConfigurationId) {
    saveError.value = 'Select a render configuration before previewing.'
    return
  }

  isPreviewing.value = true
  saveError.value = null
  previewStore.start()
  try {
    const { data, error } = await api.POST('/documents/preview', {
      body: {
        scriptTags: Object.fromEntries(
          Object.entries(current.scriptNameTags).map(([tag, id]) => [tag, { id }]),
        ),
        bindings,
        renderConfigId: current.renderConfigurationId,
        template: current.templateHtml,
      },
    })
    if (error) {
      previewStore.setError('The document could not be rendered. Check script links and template content.')
    } else {
      previewStore.setPreview(data?.html ?? '')
    }
  } catch {
    previewStore.setError('An unexpected error occurred while rendering the document.')
  } finally {
    isPreviewing.value = false
  }
  previewStore.requestOpen()
}

</script>

<template>
  <div class="document-panel">
    <div v-if="draft" class="document-links">
      <div class="links-heading">
        <strong>Script Links</strong>
        <button @click="addScriptLink">Add Script</button>
      </div>
      <div v-for="(scriptId, tag) in draft.scriptNameTags" :key="tag" class="script-link-row">
        <input
          :value="tag"
          aria-label="Template tag name"
          placeholder="Template tag"
          @change="updateScriptLink(tag, ($event.target as HTMLInputElement).value, scriptId)"
        />
        <input
          :value="scriptId"
          aria-label="PEEL script ID"
          placeholder="PEEL script ID"
          @input="updateScriptLink(tag, tag, ($event.target as HTMLInputElement).value)"
        />
        <button title="Remove script link" @click="removeScriptLink(tag)">Remove</button>
      </div>
      <label class="render-config-field">
        Render Config
        <select
          :value="draft.renderConfigurationId"
          @change="draftStore.updateDraft(documentId, { renderConfigurationId: ($event.target as HTMLSelectElement).value })"
        >
          <option value="">Select a render config</option>
          <option v-for="config in draft.renderConfigurations" :key="config.id" :value="config.id">
            {{ config.name }}
          </option>
        </select>
      </label>
      <div class="document-actions">
        <button :disabled="isSaving" @click="saveDocument">{{ isSaving ? 'Saving...' : 'Save' }}</button>
        <button :disabled="isPreviewing" @click="previewDocument">{{ isPreviewing ? 'Rendering...' : 'Render Preview' }}</button>
      </div>
      <p v-if="saveError" class="save-error">{{ saveError }}</p>
    </div>
    <div class="editor-toolbar" role="toolbar" aria-label="Document formatting">
      <button :class="{ active: editor?.isActive('paragraph') }" title="Paragraph" @click="editor?.chain().focus().setParagraph().run()">P</button>
      <button :class="{ active: editor?.isActive('heading', { level: 1 }) }" title="Heading 1" @click="toggleHeading(1)">H1</button>
      <button :class="{ active: editor?.isActive('heading', { level: 2 }) }" title="Heading 2" @click="toggleHeading(2)">H2</button>
      <span class="toolbar-divider"></span>
      <button :class="{ active: editor?.isActive('bold') }" title="Bold" @click="toggleBold"><strong>B</strong></button>
      <button :class="{ active: editor?.isActive('italic') }" title="Italic" @click="toggleItalic"><em>I</em></button>
      <button :class="{ active: editor?.isActive('strike') }" title="Strikethrough" @click="editor?.chain().focus().toggleStrike().run()"><s>S</s></button>
      <span class="toolbar-divider"></span>
      <button :class="{ active: editor?.isActive('bulletList') }" title="Bullet list" @click="toggleBulletList">List</button>
      <button :class="{ active: editor?.isActive('orderedList') }" title="Numbered list" @click="toggleOrderedList">1.</button>
      <button :class="{ active: editor?.isActive('blockquote') }" title="Quote" @click="toggleBlockquote">Quote</button>
      <span class="toolbar-divider"></span>
      <button title="Undo" :disabled="!editor?.can().undo()" @click="editor?.chain().focus().undo().run()">Undo</button>
      <button title="Redo" :disabled="!editor?.can().redo()" @click="editor?.chain().focus().redo().run()">Redo</button>
    </div>
    <div class="document-canvas">
      <article class="a4-page">
        <EditorContent :editor="editor" class="tiptap-editor" />
      </article>
    </div>
  </div>
</template>

<style scoped>
.document-panel { height: 100%; overflow: auto; background: #17191c; color: #ccc; }
.document-links { display: grid; gap: 8px; padding: 12px; border-bottom: 1px solid #3c3c3c; }
.links-heading, .script-link-row { display: flex; gap: 8px; align-items: center; }
.script-link-row input, .render-config-field select { flex: 1; min-width: 0; padding: 6px; border: 1px solid #3c3c3c; background: #252526; color: #fff; }
.render-config-field { display: grid; gap: 4px; }
button { width: fit-content; padding: 5px 10px; border: 1px solid #3c3c3c; background: #333; color: #fff; cursor: pointer; }
button:disabled { cursor: default; opacity: 0.45; }
.save-error { margin: 0; color: #f48771; }
.document-actions { display: flex; gap: 8px; }
.editor-toolbar { position: sticky; top: 0; z-index: 2; display: flex; flex-wrap: wrap; gap: 5px; align-items: center; padding: 8px 14px; border-bottom: 1px solid #34373c; background: #25272b; box-shadow: 0 3px 8px rgba(0, 0, 0, 0.22); }
.editor-toolbar button { min-width: 31px; padding: 4px 7px; border-color: transparent; background: transparent; font-size: 12px; }
.editor-toolbar button:hover, .editor-toolbar button.active { background: #3d4148; }
.toolbar-divider { width: 1px; height: 20px; margin: 0 3px; background: #4a4d52; }
.document-canvas { min-height: 100%; padding: 34px 20px 80px; background: radial-gradient(circle at top, #30343a 0, #202328 52%, #17191c 100%); }
.a4-page { width: min(794px, 100%); min-height: 1123px; box-sizing: border-box; margin: 0 auto; padding: 76px 76px 92px; background: #fbfbfa; color: #202124; box-shadow: 0 8px 28px rgba(0, 0, 0, 0.45); }
.tiptap-editor :deep(.ProseMirror) { min-height: 955px; outline: none; font-family: Georgia, 'Times New Roman', serif; font-size: 16px; line-height: 1.65; }
.tiptap-editor :deep(h1) { margin: 0 0 0.7em; font-size: 2.1em; line-height: 1.15; }
.tiptap-editor :deep(h2) { margin: 1.2em 0 0.55em; font-size: 1.55em; line-height: 1.2; }
.tiptap-editor :deep(h3) { margin: 1em 0 0.45em; font-size: 1.2em; }
.tiptap-editor :deep(p) { margin: 0 0 0.9em; }
.tiptap-editor :deep(ul), .tiptap-editor :deep(ol) { padding-left: 1.5em; margin: 0 0 0.9em; }
.tiptap-editor :deep(blockquote) { margin: 1em 0; padding-left: 1em; border-left: 3px solid #9aa4b1; color: #50545a; }
@media (max-width: 700px) { .document-canvas { padding: 16px 0 40px; } .a4-page { min-height: calc(100vh - 250px); padding: 36px 24px 60px; box-shadow: none; } }
</style>
