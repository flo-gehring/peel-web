import { useMutation } from '@tanstack/react-query'

import { runScript } from '../../../lib/api/client'
import type { JsonObject } from '../../../lib/api/types'

type UseRunScriptOptions = {
  onSuccess?: () => void
}

export function useRunScript(options: UseRunScriptOptions = {}) {
  const runMutation = useMutation({
    mutationFn: runScript,
    onSuccess: () => {
      options.onSuccess?.()
    },
  })

  const run = (script: string, bindings: JsonObject) => {
    runMutation.mutate({ script, bindings })
  }

  return {
    run,
    runMutation,
    runData: runMutation.data ?? null,
  }
}
