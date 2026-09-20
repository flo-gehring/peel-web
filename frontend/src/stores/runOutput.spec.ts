import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useRunOutputStore } from './runOutput'

describe('run output', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-01-01T00:00:00Z'))
  })

  it('tracks successful execution state', () => {
    const store = useRunOutputStore()
    const request = { panelId: 'editor-script-1', name: 'Example', script: 'return 1', bindings: {} }

    store.startRun(request)
    store.setRunSuccess({ trace: { step: 1 }, result: { value: 1 } })

    expect(store.isRunning).toBe(false)
    expect(store.request).toEqual(request)
    expect(store.response).toEqual({ trace: { step: 1 }, result: { value: 1 } })
    expect(store.error).toBeNull()
    expect(store.lastRunAt).toBe(Date.now())
  })

  it('clears all state', () => {
    const store = useRunOutputStore()

    store.startRun({ panelId: 'editor-script-1', name: 'Example', script: 'return 1', bindings: {} })
    store.setRunError('Execution failed')
    store.clearRun()

    expect(store.isRunning).toBe(false)
    expect(store.request).toBeNull()
    expect(store.response).toBeNull()
    expect(store.error).toBeNull()
    expect(store.lastRunAt).toBeNull()
  })
})
