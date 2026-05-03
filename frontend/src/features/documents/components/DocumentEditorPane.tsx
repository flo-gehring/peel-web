import { EditorContent, useEditor } from '@tiptap/react'
import StarterKit from '@tiptap/starter-kit'
import clsx from 'clsx'
import {
  Bold,
  Heading1,
  Heading2,
  Italic,
  List,
  ListOrdered,
  Pilcrow,
  Redo,
  Undo,
} from 'lucide-react'
import { type ReactNode, useEffect } from 'react'

import type { JsonObject } from '../../../lib/api/types'

type DocumentEditorPaneProps = {
  content: JsonObject
  onContentChange: (content: JsonObject) => void
}

export function DocumentEditorPane({ content, onContentChange }: DocumentEditorPaneProps) {
  const editor = useEditor({
    extensions: [StarterKit],
    content,
    editorProps: {
      attributes: {
        class:
          'prose prose-invert prose-slate min-h-full max-w-none px-4 py-4 text-slate-100 outline-none',
      },
    },
    onUpdate: ({ editor: currentEditor }) => {
      onContentChange(currentEditor.getJSON() as JsonObject)
    },
  })

  useEffect(() => {
    if (!editor) {
      return
    }
    const current = editor.getJSON()
    if (JSON.stringify(current) === JSON.stringify(content)) {
      return
    }
    editor.commands.setContent(content, { emitUpdate: false })
  }, [content, editor])

  return (
    <div className="flex h-full min-h-0 flex-col border border-slate-800 bg-slate-900/70">
      <div className="flex flex-wrap items-center gap-2 border-b border-slate-800 px-3 py-2">
        <ToolbarButton
          label="Paragraph"
          active={editor?.isActive('paragraph') ?? false}
          onClick={() => editor?.chain().focus().setParagraph().run()}
        >
          <Pilcrow size={14} />
        </ToolbarButton>
        <ToolbarButton
          label="Heading 1"
          active={editor?.isActive('heading', { level: 1 }) ?? false}
          onClick={() => editor?.chain().focus().toggleHeading({ level: 1 }).run()}
        >
          <Heading1 size={14} />
        </ToolbarButton>
        <ToolbarButton
          label="Heading 2"
          active={editor?.isActive('heading', { level: 2 }) ?? false}
          onClick={() => editor?.chain().focus().toggleHeading({ level: 2 }).run()}
        >
          <Heading2 size={14} />
        </ToolbarButton>
        <ToolbarButton
          label="Bold"
          active={editor?.isActive('bold') ?? false}
          onClick={() => editor?.chain().focus().toggleBold().run()}
        >
          <Bold size={14} />
        </ToolbarButton>
        <ToolbarButton
          label="Italic"
          active={editor?.isActive('italic') ?? false}
          onClick={() => editor?.chain().focus().toggleItalic().run()}
        >
          <Italic size={14} />
        </ToolbarButton>
        <ToolbarButton
          label="Bulleted list"
          active={editor?.isActive('bulletList') ?? false}
          onClick={() => editor?.chain().focus().toggleBulletList().run()}
        >
          <List size={14} />
        </ToolbarButton>
        <ToolbarButton
          label="Numbered list"
          active={editor?.isActive('orderedList') ?? false}
          onClick={() => editor?.chain().focus().toggleOrderedList().run()}
        >
          <ListOrdered size={14} />
        </ToolbarButton>
        <ToolbarButton
          label="Undo"
          active={false}
          onClick={() => editor?.chain().focus().undo().run()}
          disabled={!(editor?.can().undo() ?? false)}
        >
          <Undo size={14} />
        </ToolbarButton>
        <ToolbarButton
          label="Redo"
          active={false}
          onClick={() => editor?.chain().focus().redo().run()}
          disabled={!(editor?.can().redo() ?? false)}
        >
          <Redo size={14} />
        </ToolbarButton>
      </div>

      <div className="min-h-0 flex-1 overflow-auto">
        <EditorContent editor={editor} />
      </div>
    </div>
  )
}

function ToolbarButton({
  label,
  active,
  onClick,
  disabled = false,
  children,
}: {
  label: string
  active: boolean
  onClick: () => void
  disabled?: boolean
  children: ReactNode
}) {
  return (
    <button
      type="button"
      aria-label={label}
      title={label}
      disabled={disabled}
      onClick={onClick}
      className={clsx(
        'inline-flex h-8 w-8 items-center justify-center rounded-md border transition',
        active
          ? 'border-cyan-500 bg-cyan-500/20 text-cyan-100'
          : 'border-slate-700 bg-slate-900 text-slate-300 hover:border-cyan-500',
        disabled ? 'cursor-not-allowed opacity-40 hover:border-slate-700' : null,
      )}
    >
      {children}
    </button>
  )
}
