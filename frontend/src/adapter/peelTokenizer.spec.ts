import { describe, expect, it } from 'vitest'
import * as monaco from 'monaco-editor/editor/editor.api.js'
import { registerPeelLanguage } from './peelLanguage'

registerPeelLanguage(monaco)
describe('PEEL Monarch tokenizer', () => {
  it('recognizes named block labels separately from variables and map keys', () => {
    const tokens = monaco.editor.tokenize('report: { var value = {"key": 1}; }', 'peel')[0] ?? []
    expect(tokens[0]?.type).toBe('type.identifier.peel')
    expect(tokens.some((token) => token.type === 'string.peel')).toBe(true)
    expect(monaco.editor.tokenize('report:\n{ 1; }', 'peel')[0]?.[0]?.type).toBe('identifier.peel')
  })
  it('keeps comment markers inside strings and recognizes real comments and operators', () => {
    const lines = monaco.editor.tokenize(
      'var url = "https://peel"; // comment\nTrue && 2 ** 3;',
      'peel',
    )
    expect(lines[0]?.filter((token) => token.type === 'comment.peel')).toHaveLength(1)
    expect(lines[0]?.find((token) => token.type === 'comment.peel')?.offset).toBe(26)
    expect(lines[1]?.some((token) => token.type === 'constant.peel')).toBe(true)
    expect(lines[1]?.some((token) => token.type === 'operator.peel')).toBe(true)
  })
  it('recovers after an unfinished string and identifies unsupported escapes', () => {
    const lines = monaco.editor.tokenize('"unfinished\nvar x = 1;\n"bad\\q";', 'peel')
    expect(lines[1]?.[0]?.type).toBe('keyword.peel')
    expect(lines[2]?.some((token) => token.type === 'string.escape.invalid.peel')).toBe(true)
  })
})
