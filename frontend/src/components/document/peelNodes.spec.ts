import { describe, expect, it } from 'vitest'
import { getSchema, generateHTML, generateJSON } from '@tiptap/core'
import StarterKit from '@tiptap/starter-kit'
import { peelNodes } from './peelNodes'
import { documentFromEditor } from '@/adapter/documentModel'

describe('structured calculation documents', () => {
  it('preserves nested repetitions, inline bindings and calculation references through editor serialization', () => {
    const schema = getSchema([StarterKit, ...peelNodes])
    const source = {
      type: 'doc',
      content: [
        {
          type: 'peelRepeat',
          attrs: { scope: 'script', scriptId: 's', block: 'parent' },
          content: [
            {
              type: 'paragraph',
              content: [
                { type: 'text', text: 'Bauteil ' },
                {
                  type: 'peelValue',
                  attrs: { scope: 'current', binding: 'bauteil.name', decimals: 2 },
                },
              ],
            },
            {
              type: 'peelRepeat',
              attrs: { scope: 'current', block: 'child' },
              content: [
                {
                  type: 'peelCalculation',
                  attrs: { scope: 'current', mode: 'substituted' },
                },
              ],
            },
          ],
        },
      ],
    }
    const document = schema.nodeFromJSON(source)
    document.check()
    const saved = documentFromEditor(document.toJSON())
    const reopened = schema.nodeFromJSON(JSON.parse(JSON.stringify(saved)))
    reopened.check()
    expect(reopened.eq(document)).toBe(true)
    expect(saved.content?.[0]?.content?.[0]?.content?.[1]?.attrs).toMatchObject({
      binding: 'bauteil.name',
      scope: 'current',
      decimals: 2,
    })
    expect(saved.content?.[0]?.content?.[1]?.content?.[0]?.attrs).toMatchObject({
      scope: 'current',
      mode: 'substituted',
    })
  })

  it('supports a whole-script calculation with no named blocks', () => {
    const schema = getSchema([StarterKit, ...peelNodes])
    const document = schema.nodeFromJSON({
      type: 'doc',
      content: [{ type: 'peelCalculation', attrs: { scriptId: 's' } }],
    })
    document.check()
    expect(document.firstChild?.attrs).toMatchObject({
      scope: 'script',
      block: '',
      mode: 'detailed',
    })
  })

  it('preserves numeric formatting and references when copying blocks as HTML', () => {
    const extensions = [StarterKit, ...peelNodes]
    const source = {
      type: 'doc',
      content: [
        {
          type: 'peelCalculation',
          attrs: { scriptId: 's', block: 'report', decimals: 2, mode: 'substituted' },
        },
      ],
    }
    const reopened = getSchema(extensions).nodeFromJSON(
      generateJSON(generateHTML(source, extensions), extensions),
    )
    reopened.check()
    expect(reopened.firstChild?.attrs).toMatchObject({
      scriptId: 's',
      block: 'report',
      decimals: 2,
      mode: 'substituted',
    })
  })
})
