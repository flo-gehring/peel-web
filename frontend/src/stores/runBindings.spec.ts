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
})
