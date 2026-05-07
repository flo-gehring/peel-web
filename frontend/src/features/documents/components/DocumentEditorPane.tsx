import clsx from 'clsx'
import {
  Bold,
  ChevronDown,
  Heading1,
  Heading2,
  Heading3,
  Highlighter,
  Italic,
  Link as LinkIcon,
  List,
  ListChecks,
  ListOrdered,
  Pilcrow,
  Redo,
  RemoveFormatting,
  Subscript,
  Superscript,
  Undo,
} from 'lucide-react'
import {
  $createParagraphNode,
  $createTextNode,
  $getSelection,
  $isRangeSelection,
  CAN_REDO_COMMAND,
  CAN_UNDO_COMMAND,
  COMMAND_PRIORITY_CRITICAL,
  COMMAND_PRIORITY_EDITOR,
  FORMAT_ELEMENT_COMMAND,
  FORMAT_TEXT_COMMAND,
  INDENT_CONTENT_COMMAND,
  OUTDENT_CONTENT_COMMAND,
  REDO_COMMAND,
  SELECTION_CHANGE_COMMAND,
  UNDO_COMMAND,
  type LexicalEditor,
  type TextNode,
} from 'lexical'
import { type ReactNode, useEffect, useMemo, useRef, useState } from 'react'

import { AutoFocusPlugin } from '@lexical/react/LexicalAutoFocusPlugin'
import { LexicalComposer } from '@lexical/react/LexicalComposer'
import { useLexicalComposerContext } from '@lexical/react/LexicalComposerContext'
import { ContentEditable } from '@lexical/react/LexicalContentEditable'
import { LexicalErrorBoundary } from '@lexical/react/LexicalErrorBoundary'
import { HistoryPlugin } from '@lexical/react/LexicalHistoryPlugin'
import { LinkPlugin } from '@lexical/react/LexicalLinkPlugin'
import { ListPlugin } from '@lexical/react/LexicalListPlugin'
import { OnChangePlugin } from '@lexical/react/LexicalOnChangePlugin'
import { RichTextPlugin } from '@lexical/react/LexicalRichTextPlugin'
import { CheckListPlugin } from '@lexical/react/LexicalCheckListPlugin'
import { $isLinkNode, LinkNode, TOGGLE_LINK_COMMAND } from '@lexical/link'
import {
  ListItemNode,
  ListNode,
  $isListNode,
  INSERT_CHECK_LIST_COMMAND,
  INSERT_ORDERED_LIST_COMMAND,
  INSERT_UNORDERED_LIST_COMMAND,
  REMOVE_LIST_COMMAND,
} from '@lexical/list'
import { $createHeadingNode, $createQuoteNode, HeadingNode, QuoteNode } from '@lexical/rich-text'
import { $setBlocksType } from '@lexical/selection'
import { registerLexicalTextEntity } from '@lexical/text'
import { mergeRegister } from '@lexical/utils'

import type { JsonObject } from '../../../lib/api/types'
import { $createScriptRefNode, $isScriptRefNode, ScriptRefNode } from '../editor/ScriptRefNode'
import { INSERT_SCRIPT_REF_COMMAND } from '../editor/constants'
import { parseDocumentContent } from '../lib/content'

type DocumentEditorPaneProps = {
  content: JsonObject
  onContentChange: (content: JsonObject) => void
}

type PageSize = 'a4' | 'letter'

type ToolbarState = {
  canUndo: boolean
  canRedo: boolean
  isBold: boolean
  isItalic: boolean
  isUnderline: boolean
  isStrikethrough: boolean
  isCode: boolean
  isSubscript: boolean
  isSuperscript: boolean
  isHighlight: boolean
  isLink: boolean
  blockType: 'paragraph' | 'h1' | 'h2' | 'h3' | 'quote' | 'code' | 'bullet' | 'number' | 'check'
  chipScriptId: string | null
}

const TOKEN_REGEX = /\{\{script:([A-Za-z0-9-]+)}}/

const EMPTY_TOOLBAR_STATE: ToolbarState = {
  canUndo: false,
  canRedo: false,
  isBold: false,
  isItalic: false,
  isUnderline: false,
  isStrikethrough: false,
  isCode: false,
  isSubscript: false,
  isSuperscript: false,
  isHighlight: false,
  isLink: false,
  blockType: 'paragraph',
  chipScriptId: null,
}

export function DocumentEditorPane({ content, onContentChange }: DocumentEditorPaneProps) {
  const initialState = useMemo(() => JSON.stringify(parseDocumentContent(content)), [content])
  const [pageSize, setPageSize] = useState<PageSize>('a4')

  return (
    <div className="flex h-full min-h-0 flex-col border border-slate-800 bg-slate-900/70">
      <LexicalComposer
        initialConfig={{
          namespace: 'peel-documents-editor',
          editorState: initialState,
          nodes: [ScriptRefNode, LinkNode, ListNode, ListItemNode, HeadingNode, QuoteNode],
          onError(error) {
            throw error
          },
          theme: {
            paragraph: 'peel-editor-paragraph',
            text: {
              bold: 'font-semibold',
              italic: 'italic',
              underline: 'underline',
              strikethrough: 'line-through',
              code: 'rounded bg-slate-200 px-1 py-0.5 font-mono text-[0.9em] text-slate-900',
              subscript: 'align-sub text-[0.75em]',
              superscript: 'align-super text-[0.75em]',
              highlight: 'rounded bg-amber-200 px-0.5',
            },
            heading: {
              h1: 'mt-5 mb-2 text-2xl font-semibold text-slate-900',
              h2: 'mt-4 mb-2 text-xl font-semibold text-slate-900',
              h3: 'mt-3 mb-2 text-lg font-semibold text-slate-900',
            },
            quote: 'my-3 border-l-4 border-cyan-500/60 pl-4 text-slate-700 italic',
            list: {
              ul: 'my-3 list-disc pl-6',
              ol: 'my-3 list-decimal pl-6',
              listitem: 'my-1',
              nested: {
                listitem: 'my-1',
              },
              listitemChecked:
                'relative list-none pl-6 before:absolute before:left-0 before:top-1 before:h-3 before:w-3 before:rounded before:border before:border-emerald-500 before:bg-emerald-500/25',
              listitemUnchecked:
                'relative list-none pl-6 before:absolute before:left-0 before:top-1 before:h-3 before:w-3 before:rounded before:border before:border-slate-500',
            },
            link: 'text-cyan-700 underline underline-offset-2',
          },
        }}
      >
        <ToolbarPlugin pageSize={pageSize} onPageSizeChange={setPageSize} />
        <RichTextPlugin
          contentEditable={
            <ContentEditable
              className={clsx(
                'peel-editor-content',
                pageSize === 'a4' ? 'peel-editor-page-a4' : 'peel-editor-page-letter',
              )}
              aria-placeholder="Start writing your document"
              placeholder={<div className="peel-editor-placeholder">Start writing your document...</div>}
            />
          }
          placeholder={null}
          ErrorBoundary={LexicalErrorBoundary}
        />
        <HistoryPlugin />
        <ListPlugin />
        <CheckListPlugin />
        <LinkPlugin />
        <ScriptRefEntityPlugin />
        <EditorCommandsPlugin />
        <ExternalStateSyncPlugin serializedContent={initialState} />
        <AutoFocusPlugin />
        <OnChangePlugin
          ignoreSelectionChange
          onChange={(nextState) => {
            const asJson = nextState.toJSON() as unknown as JsonObject
            onContentChange(asJson)
          }}
        />
      </LexicalComposer>
    </div>
  )
}

function ExternalStateSyncPlugin({ serializedContent }: { serializedContent: string }) {
  const [editor] = useLexicalComposerContext()
  const lastAppliedRef = useRef(serializedContent)

  useEffect(() => {
    if (lastAppliedRef.current === serializedContent) {
      return
    }

    const currentSerialized = JSON.stringify(editor.getEditorState().toJSON())
    if (currentSerialized === serializedContent) {
      lastAppliedRef.current = serializedContent
      return
    }

    editor.setEditorState(editor.parseEditorState(serializedContent))
    lastAppliedRef.current = serializedContent
  }, [editor, serializedContent])

  return null
}

function EditorCommandsPlugin() {
  const [editor] = useLexicalComposerContext()

  useEffect(() => {
    return editor.registerCommand<string>(
      INSERT_SCRIPT_REF_COMMAND,
      (scriptId) => {
        editor.update(() => {
          const selection = $getSelection()
          if (!$isRangeSelection(selection)) {
            return
          }
          selection.insertNodes([$createScriptRefNode(scriptId), $createTextNode(' ')])
        })
        return true
      },
      COMMAND_PRIORITY_EDITOR,
    )
  }, [editor])

  return null
}

function ScriptRefEntityPlugin() {
  const [editor] = useLexicalComposerContext()

  useEffect(() => {
    const getMatch = (text: string) => {
      const match = TOKEN_REGEX.exec(text)
      if (!match) {
        return null
      }
      return {
        start: match.index,
        end: match.index + match[0].length,
      }
    }

    return mergeRegister(
      ...registerLexicalTextEntity(
        editor,
        getMatch,
        ScriptRefNode,
        (textNode: TextNode) => {
          const match = TOKEN_REGEX.exec(textNode.getTextContent())
          const scriptId = match?.[1] ?? 'SCRIPT_ID'
          return $createScriptRefNode(scriptId)
        },
      ),
    )
  }, [editor])

  return null
}

function ToolbarPlugin({
  pageSize,
  onPageSizeChange,
}: {
  pageSize: PageSize
  onPageSizeChange: (next: PageSize) => void
}) {
  const [editor] = useLexicalComposerContext()
  const [toolbar, setToolbar] = useState<ToolbarState>(EMPTY_TOOLBAR_STATE)
  const [blockOpen, setBlockOpen] = useState(false)
  const [advancedOpen, setAdvancedOpen] = useState(false)
  const [insertOpen, setInsertOpen] = useState(false)
  const [pageSizeOpen, setPageSizeOpen] = useState(false)
  const dropdownRootRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    return mergeRegister(
      editor.registerCommand(
        SELECTION_CHANGE_COMMAND,
        () => {
          editor.getEditorState().read(() => setToolbar(readToolbarState()))
          return false
        },
        COMMAND_PRIORITY_EDITOR,
      ),
      editor.registerUpdateListener(({ editorState }) => {
        editorState.read(() => setToolbar(readToolbarState()))
      }),
      editor.registerCommand<boolean>(
        CAN_UNDO_COMMAND,
        (payload) => {
          setToolbar((value) => ({ ...value, canUndo: payload }))
          return false
        },
        COMMAND_PRIORITY_CRITICAL,
      ),
      editor.registerCommand<boolean>(
        CAN_REDO_COMMAND,
        (payload) => {
          setToolbar((value) => ({ ...value, canRedo: payload }))
          return false
        },
        COMMAND_PRIORITY_CRITICAL,
      ),
    )
  }, [editor])

  useEffect(() => {
    const onWindowClick = (event: MouseEvent) => {
      if (!dropdownRootRef.current) {
        return
      }

      if (event.target instanceof Node && dropdownRootRef.current.contains(event.target)) {
        return
      }

      setBlockOpen(false)
      setAdvancedOpen(false)
      setInsertOpen(false)
      setPageSizeOpen(false)
    }

    window.addEventListener('mousedown', onWindowClick)
    return () => window.removeEventListener('mousedown', onWindowClick)
  }, [])

  return (
    <div ref={dropdownRootRef} className="flex flex-wrap items-center gap-2 border-b border-slate-800 px-3 py-2">
      <ToolbarButton
        label="Undo"
        active={false}
        onClick={() => editor.dispatchCommand(UNDO_COMMAND, undefined)}
        disabled={!toolbar.canUndo}
      >
        <Undo size={14} />
      </ToolbarButton>
      <ToolbarButton
        label="Redo"
        active={false}
        onClick={() => editor.dispatchCommand(REDO_COMMAND, undefined)}
        disabled={!toolbar.canRedo}
      >
        <Redo size={14} />
      </ToolbarButton>

      <DropdownButton
        label={`Page: ${pageSize === 'a4' ? 'A4' : 'Letter'}`}
        icon={<ChevronDown size={14} />}
        open={pageSizeOpen}
        onToggle={() => setPageSizeOpen((value) => !value)}
      >
        <DropdownItem
          label="A4"
          onClick={() => {
            onPageSizeChange('a4')
            setPageSizeOpen(false)
          }}
        >
          <span className="text-xs font-semibold">A4</span>
        </DropdownItem>
        <DropdownItem
          label="Letter"
          onClick={() => {
            onPageSizeChange('letter')
            setPageSizeOpen(false)
          }}
        >
          <span className="text-xs font-semibold">LT</span>
        </DropdownItem>
      </DropdownButton>

      <DropdownButton
        label={blockLabel(toolbar.blockType)}
        icon={blockIcon(toolbar.blockType)}
        open={blockOpen}
        onToggle={() => setBlockOpen((value) => !value)}
      >
        <DropdownItem label="Paragraph" onClick={() => applyParagraph(editor, () => setBlockOpen(false))}>
          <Pilcrow size={14} />
        </DropdownItem>
        <DropdownItem label="Heading 1" onClick={() => applyHeading(editor, 'h1', () => setBlockOpen(false))}>
          <Heading1 size={14} />
        </DropdownItem>
        <DropdownItem label="Heading 2" onClick={() => applyHeading(editor, 'h2', () => setBlockOpen(false))}>
          <Heading2 size={14} />
        </DropdownItem>
        <DropdownItem label="Heading 3" onClick={() => applyHeading(editor, 'h3', () => setBlockOpen(false))}>
          <Heading3 size={14} />
        </DropdownItem>
        <DropdownItem label="Quote" onClick={() => applyQuote(editor, () => setBlockOpen(false))}>
          <Pilcrow size={14} />
        </DropdownItem>
      </DropdownButton>

      <ToolbarButton
        label="Bold"
        active={toolbar.isBold}
        onClick={() => editor.dispatchCommand(FORMAT_TEXT_COMMAND, 'bold')}
      >
        <Bold size={14} />
      </ToolbarButton>
      <ToolbarButton
        label="Italic"
        active={toolbar.isItalic}
        onClick={() => editor.dispatchCommand(FORMAT_TEXT_COMMAND, 'italic')}
      >
        <Italic size={14} />
      </ToolbarButton>
      <ToolbarButton
        label="Underline"
        active={toolbar.isUnderline}
        onClick={() => editor.dispatchCommand(FORMAT_TEXT_COMMAND, 'underline')}
      >
        <span className="text-xs font-semibold underline">U</span>
      </ToolbarButton>
      <ToolbarButton
        label="Strikethrough"
        active={toolbar.isStrikethrough}
        onClick={() => editor.dispatchCommand(FORMAT_TEXT_COMMAND, 'strikethrough')}
      >
        <span className="text-xs font-semibold line-through">S</span>
      </ToolbarButton>

      <ToolbarButton
        label="Bullet list"
        active={toolbar.blockType === 'bullet'}
        onClick={() => toggleList(editor, 'bullet', toolbar.blockType)}
      >
        <List size={14} />
      </ToolbarButton>
      <ToolbarButton
        label="Numbered list"
        active={toolbar.blockType === 'number'}
        onClick={() => toggleList(editor, 'number', toolbar.blockType)}
      >
        <ListOrdered size={14} />
      </ToolbarButton>
      <ToolbarButton
        label="Checklist"
        active={toolbar.blockType === 'check'}
        onClick={() => toggleList(editor, 'check', toolbar.blockType)}
      >
        <ListChecks size={14} />
      </ToolbarButton>

      <ToolbarButton
        label="Link"
        active={toolbar.isLink}
        onClick={() => toggleLink(editor, toolbar.isLink)}
      >
        <LinkIcon size={14} />
      </ToolbarButton>

      <DropdownButton
        label="Insert"
        icon={<span className="text-base leading-none">+</span>}
        open={insertOpen}
        onToggle={() => setInsertOpen((value) => !value)}
      >
        <DropdownItem
          label="Script chip"
          onClick={() => {
            const value = window.prompt('Script id', toolbar.chipScriptId ?? 'SCRIPT_ID')
            if (!value) {
              return
            }
            editor.dispatchCommand(INSERT_SCRIPT_REF_COMMAND, value)
            setInsertOpen(false)
          }}
        >
          <span className="font-mono text-[11px]">{'{{...}}'}</span>
        </DropdownItem>
      </DropdownButton>

      <ToolbarButton
        label="Align left"
        active={false}
        onClick={() => editor.dispatchCommand(FORMAT_ELEMENT_COMMAND, 'left')}
      >
        <span className="text-xs font-semibold">L</span>
      </ToolbarButton>
      <ToolbarButton
        label="Align center"
        active={false}
        onClick={() => editor.dispatchCommand(FORMAT_ELEMENT_COMMAND, 'center')}
      >
        <span className="text-xs font-semibold">C</span>
      </ToolbarButton>
      <ToolbarButton
        label="Align right"
        active={false}
        onClick={() => editor.dispatchCommand(FORMAT_ELEMENT_COMMAND, 'right')}
      >
        <span className="text-xs font-semibold">R</span>
      </ToolbarButton>
      <ToolbarButton
        label="Justify"
        active={false}
        onClick={() => editor.dispatchCommand(FORMAT_ELEMENT_COMMAND, 'justify')}
      >
        <span className="text-xs font-semibold">J</span>
      </ToolbarButton>
      <ToolbarButton
        label="Outdent"
        active={false}
        onClick={() => editor.dispatchCommand(OUTDENT_CONTENT_COMMAND, undefined)}
      >
        <span className="text-xs font-semibold">&lt;</span>
      </ToolbarButton>
      <ToolbarButton
        label="Indent"
        active={false}
        onClick={() => editor.dispatchCommand(INDENT_CONTENT_COMMAND, undefined)}
      >
        <span className="text-xs font-semibold">&gt;</span>
      </ToolbarButton>

      <DropdownButton
        label="Advanced"
        icon={<ChevronDown size={14} />}
        open={advancedOpen}
        onToggle={() => setAdvancedOpen((value) => !value)}
      >
        <DropdownItem
          label="Inline code"
          onClick={() => {
            editor.dispatchCommand(FORMAT_TEXT_COMMAND, 'code')
            setAdvancedOpen(false)
          }}
        >
          <span className="font-mono text-[11px]">{'</>'}</span>
        </DropdownItem>
        <DropdownItem
          label="Subscript"
          onClick={() => {
            editor.dispatchCommand(FORMAT_TEXT_COMMAND, 'subscript')
            setAdvancedOpen(false)
          }}
        >
          <Subscript size={14} />
        </DropdownItem>
        <DropdownItem
          label="Superscript"
          onClick={() => {
            editor.dispatchCommand(FORMAT_TEXT_COMMAND, 'superscript')
            setAdvancedOpen(false)
          }}
        >
          <Superscript size={14} />
        </DropdownItem>
        <DropdownItem
          label="Highlight"
          onClick={() => {
            editor.dispatchCommand(FORMAT_TEXT_COMMAND, 'highlight')
            setAdvancedOpen(false)
          }}
        >
          <Highlighter size={14} />
        </DropdownItem>
        <DropdownItem
          label="Clear formatting"
          onClick={() => {
            clearFormatting(editor)
            setAdvancedOpen(false)
          }}
        >
          <RemoveFormatting size={14} />
        </DropdownItem>
      </DropdownButton>
    </div>
  )
}

function readToolbarState(): ToolbarState {
  const selection = $getSelection()
  if (!$isRangeSelection(selection)) {
    return EMPTY_TOOLBAR_STATE
  }

  const anchorNode = selection.anchor.getNode()
  const element = anchorNode.getTopLevelElementOrThrow()
  const elementType = element.getType()

  let blockType: ToolbarState['blockType'] = 'paragraph'
  if ($isListNode(element)) {
    const listType = element.getListType()
    blockType = listType === 'number' ? 'number' : listType === 'check' ? 'check' : 'bullet'
  } else if (elementType === 'heading') {
    const headingTag = (element as unknown as { getTag: () => string }).getTag()
    blockType = headingTag === 'h3' ? 'h3' : headingTag === 'h2' ? 'h2' : 'h1'
  } else if (elementType === 'quote') {
    blockType = 'quote'
  } else if (elementType === 'code') {
    blockType = 'code'
  }

  const selectedNode = selection.anchor.getNode()
  const parent = selectedNode.getParent()
  const chipScriptId = $isScriptRefNode(selectedNode)
    ? selectedNode.getScriptId()
    : $isScriptRefNode(parent)
      ? parent.getScriptId()
      : null

  return {
    ...EMPTY_TOOLBAR_STATE,
    isBold: selection.hasFormat('bold'),
    isItalic: selection.hasFormat('italic'),
    isUnderline: selection.hasFormat('underline'),
    isStrikethrough: selection.hasFormat('strikethrough'),
    isCode: selection.hasFormat('code'),
    isSubscript: selection.hasFormat('subscript'),
    isSuperscript: selection.hasFormat('superscript'),
    isHighlight: selection.hasFormat('highlight'),
    isLink: $isLinkNode(parent) || $isLinkNode(selectedNode),
    blockType,
    chipScriptId,
  }
}

function applyParagraph(editor: LexicalEditor, onDone: () => void): void {
  editor.update(() => {
    const selection = $getSelection()
    if (!$isRangeSelection(selection)) {
      return
    }
    $setBlocksType(selection, () => $createParagraphNode())
  })
  onDone()
}

function applyHeading(editor: LexicalEditor, heading: 'h1' | 'h2' | 'h3', onDone: () => void): void {
  editor.update(() => {
    const selection = $getSelection()
    if (!$isRangeSelection(selection)) {
      return
    }
    $setBlocksType(selection, () => $createHeadingNode(heading))
  })
  onDone()
}

function applyQuote(editor: LexicalEditor, onDone: () => void): void {
  editor.update(() => {
    const selection = $getSelection()
    if (!$isRangeSelection(selection)) {
      return
    }
    $setBlocksType(selection, () => $createQuoteNode())
  })
  onDone()
}

function toggleList(
  editor: LexicalEditor,
  kind: 'bullet' | 'number' | 'check',
  currentBlockType: ToolbarState['blockType'],
): void {
  if (currentBlockType === kind) {
    editor.dispatchCommand(REMOVE_LIST_COMMAND, undefined)
    return
  }

  const command =
    kind === 'number'
      ? INSERT_ORDERED_LIST_COMMAND
      : kind === 'check'
        ? INSERT_CHECK_LIST_COMMAND
        : INSERT_UNORDERED_LIST_COMMAND
  editor.dispatchCommand(command, undefined)
}

function toggleLink(editor: LexicalEditor, isActive: boolean): void {
  if (isActive) {
    editor.dispatchCommand(TOGGLE_LINK_COMMAND, null)
    return
  }

  const url = window.prompt('Link URL', 'https://')
  if (!url) {
    return
  }
  editor.dispatchCommand(TOGGLE_LINK_COMMAND, url)
}

function clearFormatting(editor: LexicalEditor): void {
  const formats: Array<
    'bold' | 'italic' | 'underline' | 'strikethrough' | 'code' | 'subscript' | 'superscript' | 'highlight'
  > = ['bold', 'italic', 'underline', 'strikethrough', 'code', 'subscript', 'superscript', 'highlight']

  editor.update(() => {
    const selection = $getSelection()
    if (!$isRangeSelection(selection)) {
      return
    }

    for (const format of formats) {
      if (selection.hasFormat(format)) {
        selection.formatText(format)
      }
    }
  })
}

function blockLabel(type: ToolbarState['blockType']): string {
  if (type === 'h1') return 'Heading 1'
  if (type === 'h2') return 'Heading 2'
  if (type === 'h3') return 'Heading 3'
  if (type === 'quote') return 'Quote'
  if (type === 'code') return 'Code block'
  if (type === 'bullet') return 'Bullet list'
  if (type === 'number') return 'Number list'
  if (type === 'check') return 'Checklist'
  return 'Paragraph'
}

function blockIcon(type: ToolbarState['blockType']): ReactNode {
  if (type === 'h1') return <Heading1 size={14} />
  if (type === 'h2') return <Heading2 size={14} />
  if (type === 'h3') return <Heading3 size={14} />
  if (type === 'bullet') return <List size={14} />
  if (type === 'number') return <ListOrdered size={14} />
  if (type === 'check') return <ListChecks size={14} />
  return <Pilcrow size={14} />
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
        'inline-flex h-8 min-w-8 items-center justify-center rounded-md border px-2 transition',
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

function DropdownButton({
  label,
  icon,
  open,
  onToggle,
  children,
}: {
  label: string
  icon: ReactNode
  open: boolean
  onToggle: () => void
  children: ReactNode
}) {
  return (
    <div className="relative">
      <button
        type="button"
        onClick={onToggle}
        className={clsx(
          'inline-flex h-8 items-center gap-1 rounded-md border px-2 text-sm transition',
          open
            ? 'border-cyan-500 bg-cyan-500/20 text-cyan-100'
            : 'border-slate-700 bg-slate-900 text-slate-300 hover:border-cyan-500',
        )}
      >
        {icon}
        <span>{label}</span>
      </button>
      {open ? (
        <div className="absolute left-0 top-10 z-20 min-w-44 rounded-md border border-slate-700 bg-slate-950 p-1 shadow-xl">
          {children}
        </div>
      ) : null}
    </div>
  )
}

function DropdownItem({
  label,
  onClick,
  children,
}: {
  label: string
  onClick: () => void
  children: ReactNode
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="flex w-full items-center gap-2 rounded px-2 py-1.5 text-left text-sm text-slate-200 hover:bg-slate-800"
    >
      {children}
      <span>{label}</span>
    </button>
  )
}
