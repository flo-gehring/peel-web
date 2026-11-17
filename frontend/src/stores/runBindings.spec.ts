import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useRunBindingsStore } from './runBindings'

describe('run bindings', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('accepts an object containing arbitrary JSON values', () => {
    const store = useRunBindingsStore()

    store.setRawJson('{"number":1,"list":[true],"nested":{"value":null}}')

    expect(store.parseBindings()).toEqual({ number: 1, list: [true], nested: { value: null } })
    expect(store.parseError).toBeNull()
  })

  it.each(['invalid json', '[]', 'null', '42', '"text"'])('rejects %s as bindings', (rawJson) => {
    const store = useRunBindingsStore()

    store.setRawJson(rawJson)

    expect(store.parseBindings()).toBeNull()
    expect(store.parseError).toBeTruthy()
  })

  it('keeps the last valid bindings after invalid input', () => {
    const store = useRunBindingsStore()

    store.setRawJson('{"valid":true}')
    store.setRawJson('not json')

    expect(store.lastValidBindings).toEqual({ valid: true })
  })

  it('adds missing expected inputs without overwriting any existing values', () => {
    const store = useRunBindingsStore()
    store.setRawJson('{"a":1,"c":2,"nullable":null,"disabled":false,"text":"","nested":{"x":3}}')
    store.fillExpectedBindings(['a', 'b', 'nullable', 'disabled', 'text', 'nested', 'b', ''])
    expect(store.parseBindings()).toEqual({
      a: 1,
      c: 2,
      nullable: null,
      disabled: false,
      text: '',
      nested: { x: 3 },
      b: 0,
    })
    const filled = store.rawJson
    store.fillExpectedBindings(['a', 'b'])
    expect(store.rawJson).toBe(filled)
  })

  it('preserves invalid JSON instead of replacing it with the last valid bindings', () => {
    const store = useRunBindingsStore()
    store.setRawJson('{"a":1}')
    store.setRawJson('{"a":')
    store.fillExpectedBindings(['b'])
    expect(store.rawJson).toBe('{"a":')
    expect(store.parseError).toBeTruthy()
  })

  it('handles property names inherited from Object.prototype as missing inputs', () => {
    const store = useRunBindingsStore()
    store.fillExpectedBindings(['constructor', '__proto__'])
    expect(store.parseBindings()).toEqual(JSON.parse('{"constructor":0,"__proto__":0}'))
  })
})
