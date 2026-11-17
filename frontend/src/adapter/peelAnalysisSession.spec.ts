import { afterEach, describe, expect, it, vi } from 'vitest'
import { api } from '@/adapter/client'
import { bindPeelModel } from './peelLanguage'
import type * as Monaco from 'monaco-editor'

vi.mock('@/adapter/client', () => ({ api: { POST: vi.fn() } }))
let id = 0
function harness() {
  const markers = vi.fn(),
    availability = vi.fn()
  let changed = () => {}
  let value = 'first;'
  const key = 'peel://session/' + id++
  const model = {
    uri: { toString: () => key },
    getValue: () => value,
    isDisposed: () => false,
    getPositionAt: (offset: number) => ({ lineNumber: 1, column: offset + 1 }),
    onDidChangeContent: (listener: () => void) => {
      changed = listener
      return { dispose: vi.fn() }
    },
  }
  const monaco = { editor: { setModelMarkers: markers }, MarkerSeverity: { Warning: 4, Error: 8 } }
  const session = bindPeelModel(
    monaco as unknown as typeof Monaco,
    model as unknown as Monaco.editor.ITextModel,
    () => ({ projectId: 'project', expectedBindings: [{ name: 'input', description: 'Input' }] }),
    availability,
  )
  return {
    session,
    markers,
    availability,
    change: () => {
      value = 'second;'
      changed()
    },
  }
}
describe('analysis session lifecycle', () => {
  afterEach(() => {
    vi.useRealTimers()
    vi.resetAllMocks()
  })
  it('ignores old responses and analyzes the current draft with its expected bindings', async () => {
    vi.useFakeTimers()
    let resolveOld!: (value: unknown) => void
    vi.mocked(api.POST)
      .mockReturnValueOnce(
        new Promise((resolve) => {
          resolveOld = resolve
        }) as never,
      )
      .mockResolvedValueOnce({ data: { symbols: [], diagnostics: [] } } as never)
    const h = harness()
    await vi.advanceTimersByTimeAsync(300)
    h.change()
    resolveOld({
      data: { diagnostics: [{ message: 'old error', severity: 'error', start: 0, end: 2 }] },
    })
    await vi.advanceTimersByTimeAsync(300)
    expect(
      h.markers.mock.calls.every((call) =>
        call[2].every((marker: { message: string }) => marker.message !== 'old error'),
      ),
    ).toBe(true)
    const lastRequest = vi.mocked(api.POST).mock.calls.at(-1) as unknown as [
      string,
      { body: unknown },
    ]
    expect(lastRequest[1].body).toEqual({
      projectId: { id: 'project' },
      script: 'second;',
      expectedBindings: [{ name: 'input', description: 'Input' }],
    })
    h.session.dispose()
  })
  it('keeps network failures out of syntax markers and cancels scheduled requests on disposal', async () => {
    vi.useFakeTimers()
    vi.mocked(api.POST).mockRejectedValueOnce(new TypeError('offline'))
    const h = harness()
    await vi.advanceTimersByTimeAsync(300)
    expect(h.availability).toHaveBeenCalledWith(false)
    expect(h.markers.mock.calls.at(-1)?.[2]).toEqual([])
    h.change()
    h.session.dispose()
    await vi.advanceTimersByTimeAsync(300)
    expect(api.POST).toHaveBeenCalledTimes(1)
  })
})
