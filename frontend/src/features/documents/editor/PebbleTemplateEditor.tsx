import { LexicalComposer } from '@lexical/react/LexicalComposer'
import { ContentEditable } from '@lexical/react/LexicalContentEditable'
import { HistoryPlugin } from '@lexical/react/LexicalHistoryPlugin'
import { OnChangePlugin } from '@lexical/react/LexicalOnChangePlugin'
import { RichTextPlugin } from '@lexical/react/LexicalRichTextPlugin'
import { ListPlugin } from '@lexical/react/LexicalListPlugin'
import { LinkPlugin } from '@lexical/react/LexicalLinkPlugin'
import { TablePlugin } from '@lexical/react/LexicalTablePlugin'
import { useLexicalComposerContext } from '@lexical/react/LexicalComposerContext'
import { $generateHtmlFromNodes, $generateNodesFromDOM } from '@lexical/html'
import { $setBlocksType } from '@lexical/selection'
import { $createHeadingNode, HeadingNode, QuoteNode } from '@lexical/rich-text'
import { INSERT_ORDERED_LIST_COMMAND, INSERT_UNORDERED_LIST_COMMAND, ListItemNode, ListNode, REMOVE_LIST_COMMAND } from '@lexical/list'
import { $createLinkNode, LinkNode } from '@lexical/link'
import {
  $createLineBreakNode,
  $createParagraphNode,
  $createTextNode,
  $getRoot,
  $getSelection,
  $insertNodes,
  $isNodeSelection,
  $isRangeSelection,
  $isTextNode,
  COMMAND_PRIORITY_EDITOR,
  FORMAT_TEXT_COMMAND,
  type EditorState,
  type LexicalEditor,
  type LexicalNode,
} from 'lexical'
import {
  INSERT_TABLE_COMMAND,
  $isTableNode,
  $isTableSelection,
  TableNode,
  TableCellNode,
  TableRowNode,
} from '@lexical/table'
import { useEffect, useMemo, useState } from 'react'

import { $createPebbleBlockNode, PebbleBlockNode } from './PebbleBlockNode'
import { $createPebbleInlineNode, PebbleInlineNode } from './PebbleInlineNode'
import { INSERT_PEBBLE_BLOCK_COMMAND, INSERT_PEBBLE_INLINE_COMMAND } from './constants'

type PebbleTemplateEditorProps = {
  initialHtml: string
  initialEditorStateJson: string
  onTemplateChange: (next: {
    templatePebble: string
    templateHtml: string
    editorStateJson: string
  }) => void
}

const FONT_FAMILY_OPTIONS = [
  { label: 'Sans', value: "'Space Grotesk', 'Segoe UI', sans-serif" },
  { label: 'Serif', value: "'Georgia', 'Times New Roman', serif" },
  { label: 'Mono', value: "'IBM Plex Mono', ui-monospace, monospace" },
]

const FONT_SIZE_OPTIONS = ['12px', '14px', '16px', '18px', '22px', '28px']

export function PebbleTemplateEditor({ initialHtml, initialEditorStateJson, onTemplateChange }: PebbleTemplateEditorProps) {
  const [editor, setEditor] = useState<LexicalEditor | null>(null)
  const [inlineMarkupDraft, setInlineMarkupDraft] = useState('{{ calc.result | renderTraceExpression }}')
  const [blockMarkupDraft, setBlockMarkupDraft] = useState('{% if calc.result %}\n{{ calc.result | renderTraceExpression }}\n{% endif %}')
  const [isInlinePopoverOpen, setIsInlinePopoverOpen] = useState(false)
  const [isBlockPopoverOpen, setIsBlockPopoverOpen] = useState(false)
  const [isLinkPopoverOpen, setIsLinkPopoverOpen] = useState(false)
  const [linkDraft, setLinkDraft] = useState('https://')

  const initialConfig = useMemo(
    () => ({
      namespace: 'peel-doc-template-editor',
      onError: (error: Error) => {
        throw error
      },
      nodes: [
        HeadingNode,
        QuoteNode,
        ListNode,
        ListItemNode,
        LinkNode,
        TableNode,
        TableRowNode,
        TableCellNode,
        PebbleInlineNode,
        PebbleBlockNode,
      ],
      theme: {
        paragraph: 'peel-editor-paragraph',
        heading: {
          h1: 'peel-editor-h1',
          h2: 'peel-editor-h2',
        },
        table: 'peel-editor-table',
        tableRow: 'peel-editor-table-row',
        tableCell: 'peel-editor-table-cell',
        tableCellHeader: 'peel-editor-table-cell-header',
      },
      editorState: (lexicalEditor: LexicalEditor) => {
        if (initialEditorStateJson.trim().length > 0) {
          lexicalEditor.setEditorState(lexicalEditor.parseEditorState(initialEditorStateJson))
          return
        }

        lexicalEditor.update(() => {
          const root = $getRoot()
          root.clear()
          if (initialHtml.trim().length > 0) {
            const parser = new DOMParser()
            const document = parser.parseFromString(initialHtml, 'text/html')
            const nodes = $generateNodesFromDOM(lexicalEditor, document)
            root.append(...nodes)
          } else {
            const paragraph = $createParagraphNode()
            paragraph.append($createTextNode('Start writing your document here...'))
            root.append(paragraph)
          }
        })
      },
    }),
    [initialEditorStateJson, initialHtml],
  )

  useEffect(() => {
    if (!editor) {
      return
    }
    return editor.registerCommand(
      INSERT_PEBBLE_INLINE_COMMAND,
      (payload) => {
        editor.update(() => {
          const selection = $getSelection()
          if (!$isRangeSelection(selection)) {
            return
          }
          selection.insertNodes([$createPebbleInlineNode(payload)])
        })
        return true
      },
      COMMAND_PRIORITY_EDITOR,
    )
  }, [editor])

  useEffect(() => {
    if (!editor) {
      return
    }
    return editor.registerCommand(
      INSERT_PEBBLE_BLOCK_COMMAND,
      (payload) => {
        editor.update(() => {
          $insertNodes([$createPebbleBlockNode(payload), $createParagraphNode()])
        })
        return true
      },
      COMMAND_PRIORITY_EDITOR,
    )
  }, [editor])

  function handleChange(nextEditorState: EditorState, lexicalEditor: LexicalEditor) {
    nextEditorState.read(() => {
      const exportedHtml = $generateHtmlFromNodes(lexicalEditor, null)
      const templatePebble = compilePebbleTemplateFromEditor(lexicalEditor)
      const editorStateJson = JSON.stringify(nextEditorState.toJSON())
      onTemplateChange({ templatePebble, templateHtml: exportedHtml, editorStateJson })
    })
  }

  function insertInlineMarkup() {
    if (!editor) {
      return
    }
    editor.dispatchCommand(INSERT_PEBBLE_INLINE_COMMAND, inlineMarkupDraft)
    setIsInlinePopoverOpen(false)
  }

  function insertBlockMarkup() {
    if (!editor) {
      return
    }

    editor.dispatchCommand(INSERT_PEBBLE_BLOCK_COMMAND, blockMarkupDraft)
    setIsBlockPopoverOpen(false)
  }

  function removeTableAtSelection() {
    if (!editor) {
      return
    }

    editor.update(() => {
      const selection = $getSelection()
      if (!selection) {
        return
      }

      if (!$isRangeSelection(selection) && !$isNodeSelection(selection) && !$isTableSelection(selection)) {
        return
      }

      let anchorNode: LexicalNode | null = null
      if ($isNodeSelection(selection)) {
        anchorNode = selection.getNodes()[0] ?? null
      } else {
        anchorNode = selection.anchor.getNode()
      }

      const tableNode = findAncestorTableNode(anchorNode)
      if (tableNode) {
        tableNode.remove()
      }
    })
  }

  function setFontStyle(property: 'font-family' | 'font-size', value: string) {
    if (!editor) {
      return
    }
    editor.update(() => {
      const selection = $getSelection()
      if (!$isRangeSelection(selection)) {
        return
      }
      const nodes = selection.getNodes()
      for (const node of nodes) {
        if ($isTextNode(node)) {
          const currentStyle = node.getStyle()
          node.setStyle(mergeCssStyle(currentStyle, property, value))
        }
      }
    })
  }

  function insertLink() {
    if (!editor) {
      return
    }
    const nextLink = linkDraft.trim()
    if (nextLink.length === 0) {
      return
    }
    editor.update(() => {
      const selection = $getSelection()
      if (!$isRangeSelection(selection)) {
        return
      }
      const selectedText = selection.getTextContent()
      const linkNode = $createLinkNode(nextLink)
      linkNode.append($createTextNode(selectedText.trim().length > 0 ? selectedText : nextLink))
      selection.insertNodes([linkNode])
    })
    setIsLinkPopoverOpen(false)
  }

  function insertSimpleTable(rows: number, columns: number) {
    if (!editor) {
      return
    }
    editor.dispatchCommand(INSERT_TABLE_COMMAND, {
      rows: String(rows),
      columns: String(columns),
      includeHeaders: true,
    })
  }

  return (
    <div className="flex h-full min-h-0 flex-col overflow-visible border border-slate-800 bg-slate-900/70">
      <div className="relative z-30 flex flex-wrap items-center gap-2 overflow-visible border-b border-slate-800 px-3 py-2">
        <ToolbarButton label="Bold" onClick={() => editor?.dispatchCommand(FORMAT_TEXT_COMMAND, 'bold')} />
        <ToolbarButton label="Italic" onClick={() => editor?.dispatchCommand(FORMAT_TEXT_COMMAND, 'italic')} />
        <ToolbarButton
          label="H1"
          onClick={() =>
            editor?.update(() => {
              const selection = $getSelection()
              if ($isRangeSelection(selection)) {
                $setBlocksType(selection, () => $createHeadingNode('h1'))
              }
            })
          }
        />
        <ToolbarButton
          label="H2"
          onClick={() =>
            editor?.update(() => {
              const selection = $getSelection()
              if ($isRangeSelection(selection)) {
                $setBlocksType(selection, () => $createHeadingNode('h2'))
              }
            })
          }
        />
        <ToolbarButton
          label="Paragraph"
          onClick={() =>
            editor?.update(() => {
              const selection = $getSelection()
              if ($isRangeSelection(selection)) {
                $setBlocksType(selection, () => $createParagraphNode())
              }
            })
          }
        />
        <ToolbarButton label="Bulleted" onClick={() => editor?.dispatchCommand(INSERT_UNORDERED_LIST_COMMAND, undefined)} />
        <ToolbarButton label="Numbered" onClick={() => editor?.dispatchCommand(INSERT_ORDERED_LIST_COMMAND, undefined)} />
        <ToolbarButton label="Remove List" onClick={() => editor?.dispatchCommand(REMOVE_LIST_COMMAND, undefined)} />
        <ToolbarButton label="Line Break" onClick={() => editor?.update(() => $insertNodes([$createLineBreakNode()]))} />

        <select
          className="rounded border border-slate-700 bg-slate-950 px-2 py-1 text-xs text-slate-100"
          defaultValue=""
          onChange={(event) => {
            const nextValue = event.target.value
            if (nextValue) {
              setFontStyle('font-family', nextValue)
            }
          }}
        >
          <option value="">Font</option>
          {FONT_FAMILY_OPTIONS.map((option) => (
            <option key={option.label} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>

        <select
          className="rounded border border-slate-700 bg-slate-950 px-2 py-1 text-xs text-slate-100"
          defaultValue=""
          onChange={(event) => {
            const nextValue = event.target.value
            if (nextValue) {
              setFontStyle('font-size', nextValue)
            }
          }}
        >
          <option value="">Size</option>
          {FONT_SIZE_OPTIONS.map((size) => (
            <option key={size} value={size}>
              {size}
            </option>
          ))}
        </select>

        <div className="relative">
          <ToolbarButton label="Link" onClick={() => setIsLinkPopoverOpen((open) => !open)} />
          {isLinkPopoverOpen ? (
            <div className="absolute left-0 top-[calc(100%+4px)] z-20 w-80 rounded border border-slate-700 bg-slate-950 p-2 shadow-xl">
              <input
                value={linkDraft}
                onChange={(event) => setLinkDraft(event.target.value)}
                className="w-full rounded border border-slate-700 bg-slate-900 px-2 py-1 text-sm text-slate-100"
                placeholder="https://example.com"
              />
              <div className="mt-2 flex justify-end">
                <button
                  type="button"
                  onClick={insertLink}
                  className="rounded border border-teal-500/60 bg-teal-500/10 px-2 py-1 text-xs text-teal-100"
                >
                  Insert Link
                </button>
              </div>
            </div>
          ) : null}
        </div>

        <ToolbarButton label="Table 3x3" onClick={() => insertSimpleTable(3, 3)} />
        <ToolbarButton label="Delete Table" onClick={removeTableAtSelection} />

        <div className="relative">
          <ToolbarButton label="Inline Markup" onClick={() => setIsInlinePopoverOpen((open) => !open)} />
          {isInlinePopoverOpen ? (
            <div className="absolute left-0 top-[calc(100%+4px)] z-20 w-96 rounded border border-slate-700 bg-slate-950 p-2 shadow-xl">
              <input
                value={inlineMarkupDraft}
                onChange={(event) => setInlineMarkupDraft(event.target.value)}
                className="w-full rounded border border-slate-700 bg-slate-900 px-2 py-1 text-sm text-slate-100"
                placeholder="{{ calc.result | renderTraceExpression }}"
              />
              <div className="mt-2 flex justify-end">
                <button
                  type="button"
                  onClick={insertInlineMarkup}
                  className="rounded border border-cyan-500/60 bg-cyan-500/10 px-2 py-1 text-xs text-cyan-100"
                >
                  Insert Inline
                </button>
              </div>
            </div>
          ) : null}
        </div>

        <div className="relative">
          <ToolbarButton label="Block Markup" onClick={() => setIsBlockPopoverOpen((open) => !open)} />
          {isBlockPopoverOpen ? (
            <div className="absolute right-0 top-[calc(100%+4px)] z-50 w-[32rem] max-w-[calc(100vw-5rem)] rounded border border-slate-700 bg-slate-950 p-2 shadow-xl">
              <textarea
                value={blockMarkupDraft}
                onChange={(event) => setBlockMarkupDraft(event.target.value)}
                className="h-28 w-full rounded border border-slate-700 bg-slate-900 px-2 py-1 text-sm text-slate-100"
                placeholder="{% if calc.result %}\n{{ calc.result | renderTraceExpression }}\n{% endif %}"
                spellCheck={false}
              />
              <div className="mt-2 flex justify-end">
                <button
                  type="button"
                  onClick={insertBlockMarkup}
                  className="rounded border border-cyan-500/60 bg-cyan-500/10 px-2 py-1 text-xs text-cyan-100"
                >
                  Insert Block
                </button>
              </div>
            </div>
          ) : null}
        </div>
      </div>

      <div className="min-h-0 flex-1 overflow-auto bg-slate-950/60 p-4">
        <LexicalComposer initialConfig={initialConfig}>
          <EditorBridgePlugin onEditorReady={setEditor} />
          <RichTextPlugin
            contentEditable={
              <ContentEditable
                className="peel-editor-content peel-editor-page-a4"
                aria-placeholder="Write rich text and insert inline/block Pebble markup..."
                placeholder={
                  <div className="peel-editor-placeholder">
                    Write rich text and insert inline/block Pebble markup...
                  </div>
                }
              />
            }
            placeholder={null}
            ErrorBoundary={({ children }) => <>{children}</>}
          />
          <HistoryPlugin />
          <ListPlugin />
          <LinkPlugin />
          <TablePlugin hasCellMerge={false} hasCellBackgroundColor={false} />
          <OnChangePlugin onChange={handleChange} ignoreSelectionChange />
        </LexicalComposer>
      </div>
    </div>
  )
}

function EditorBridgePlugin({ onEditorReady }: { onEditorReady: (editor: LexicalEditor) => void }) {
  const [editor] = useLexicalComposerContext()

  useEffect(() => {
    onEditorReady(editor)
  }, [editor, onEditorReady])

  return null
}

function ToolbarButton({ label, onClick }: { label: string; onClick: () => void }) {
  return (
    <button
      type="button"
      className="rounded border border-slate-700 bg-slate-950 px-2 py-1 text-xs text-slate-100 hover:border-teal-500"
      onMouseDown={(event) => event.preventDefault()}
      onClick={onClick}
    >
      {label}
    </button>
  )
}

function mergeCssStyle(existingStyle: string, property: string, value: string): string {
  const styleMap = new Map<string, string>()
  for (const rule of existingStyle.split(';')) {
    const trimmed = rule.trim()
    if (!trimmed) {
      continue
    }
    const [key, ...rest] = trimmed.split(':')
    if (!key || rest.length === 0) {
      continue
    }
    styleMap.set(key.trim(), rest.join(':').trim())
  }
  styleMap.set(property, value)
  return Array.from(styleMap.entries())
    .map(([key, nextValue]) => `${key}: ${nextValue}`)
    .join('; ')
}

function compilePebbleTemplateFromEditor(editor: LexicalEditor): string {
  const html = $generateHtmlFromNodes(editor, null)
  const parser = new DOMParser()
  const document = parser.parseFromString(html, 'text/html')
  const replacements = new Map<string, string>()
  let replacementIndex = 0

  const createToken = (value: string): string => {
    const token = `__PEEL_MARKUP_TOKEN_${replacementIndex}__`
    replacementIndex += 1
    replacements.set(token, value)
    return token
  }

  const inlineElements = document.querySelectorAll('[data-peel-inline]')
  inlineElements.forEach((element) => {
    const expression = element.getAttribute('data-peel-inline') ?? ''
    element.replaceWith(document.createTextNode(createToken(expression)))
  })

  const blockElements = document.querySelectorAll('[data-peel-block]')
  blockElements.forEach((element) => {
    const markup = element.getAttribute('data-peel-block') ?? ''
    element.replaceWith(document.createTextNode(createToken(markup)))
  })

  let compiledTemplate = document.body.innerHTML
  for (const [token, rawMarkup] of replacements.entries()) {
    compiledTemplate = compiledTemplate.replaceAll(token, rawMarkup)
  }

  return compiledTemplate
}

function findAncestorTableNode(node: LexicalNode | null): TableNode | null {
  let current: LexicalNode | null = node
  while (current) {
    if ($isTableNode(current)) {
      return current
    }
    current = current.getParent()
  }
  return null
}
