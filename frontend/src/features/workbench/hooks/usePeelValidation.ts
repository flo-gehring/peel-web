import { useCallback, useMemo, useRef, useState } from 'react'
import type { editor as MonacoEditorApi } from 'monaco-editor'
import { MarkerSeverity } from 'monaco-editor'

import { validateScript } from '../../../lib/api/client'
import type { ValidationDiagnostic } from '../../../lib/api/types'
import { formatError } from '../lib/errors'

type UsePeelValidationOptions = {
  delayMs?: number
}

export function usePeelValidation(options: UsePeelValidationOptions = {}) {
  const delayMs = options.delayMs ?? 300
  const timerRef = useRef<number | null>(null)
  const [diagnostics, setDiagnostics] = useState<ValidationDiagnostic[]>([])

  const cancelValidation = useCallback(() => {
    if (timerRef.current !== null) {
      window.clearTimeout(timerRef.current)
      timerRef.current = null
    }
  }, [])

  const parseToMarkers = useCallback((nextDiagnostics: ValidationDiagnostic[]): MonacoEditorApi.IMarkerData[] => {
    return nextDiagnostics.map((diagnostic) => ({
      startLineNumber: Math.max(1, diagnostic.line),
      startColumn: Math.max(1, diagnostic.column),
      endLineNumber: Math.max(diagnostic.line, diagnostic.endLine),
      endColumn: Math.max(2, diagnostic.endColumn),
      message: diagnostic.message,
      severity: severityToMarker(diagnostic.severity),
    }))
  }, [])

  const validate = useCallback(async (script: string): Promise<MonacoEditorApi.IMarkerData[]> => {
    if (!script.trim()) {
      setDiagnostics([])
      return []
    }

    try {
      const response = await validateScript({ script })
      setDiagnostics(response.diagnostics)
      return parseToMarkers(response.diagnostics)
    } catch (error) {
      const fallbackDiagnostic: ValidationDiagnostic = {
        line: 1,
        column: 1,
        endLine: 1,
        endColumn: 2,
        severity: 'error',
        message: formatError(error) ?? 'Validation request failed',
      }
      setDiagnostics([fallbackDiagnostic])
      return parseToMarkers([fallbackDiagnostic])
    }
  }, [parseToMarkers])

  const scheduleValidation = useCallback(
    async (
      script: string,
      applyMarkers: (markers: MonacoEditorApi.IMarkerData[]) => void,
    ) => {
      cancelValidation()

      timerRef.current = window.setTimeout(async () => {
        const markers = await validate(script)
        applyMarkers(markers)
      }, delayMs)
    },
    [cancelValidation, delayMs, validate],
  )

  return useMemo(
    () => ({
      diagnostics,
      scheduleValidation,
      cancelValidation,
    }),
    [cancelValidation, diagnostics, scheduleValidation],
  )
}

function severityToMarker(severity: ValidationDiagnostic['severity']): MarkerSeverity {
  switch (severity) {
    case 'info':
      return MarkerSeverity.Info
    case 'warning':
      return MarkerSeverity.Warning
    default:
      return MarkerSeverity.Error
  }
}
