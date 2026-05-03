import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { getScript, listScripts, saveScript } from '../../../lib/api/client'
import type { ScriptDetail, ScriptSaveRequest } from '../../../lib/api/types'

type UseScriptsApiOptions = {
  onScriptLoaded: (script: ScriptDetail) => void
  onScriptSaved: (script: ScriptDetail) => void
}

export function useScriptsApi(options: UseScriptsApiOptions) {
  const queryClient = useQueryClient()

  const scriptsQuery = useQuery({
    queryKey: ['scripts'],
    queryFn: listScripts,
  })

  const loadScriptMutation = useMutation({
    mutationFn: getScript,
    onSuccess: (loadedScript) => {
      queryClient.setQueryData(['script', loadedScript.id], loadedScript)
      options.onScriptLoaded(loadedScript)
    },
  })

  const saveScriptMutation = useMutation({
    mutationFn: (payload: ScriptSaveRequest) => saveScript(payload),
    onSuccess: (savedScript) => {
      queryClient.invalidateQueries({ queryKey: ['scripts'] })
      queryClient.setQueryData(['script', savedScript.id], savedScript)
      options.onScriptSaved(savedScript)
    },
  })

  return {
    scriptsQuery,
    loadScriptMutation,
    saveScriptMutation,
  }
}
