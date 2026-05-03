import { useMemo } from 'react'

import { BindingsEditorPane } from './features/workbench/components/BindingsEditorPane'
import { ResultView } from './features/workbench/components/ResultView'
import { ScriptEditorPane } from './features/workbench/components/ScriptEditorPane'
import { ScriptsSidebar } from './features/workbench/components/ScriptsSidebar'
import { WorkbenchHeader } from './features/workbench/components/WorkbenchHeader'
import { useDraftState } from './features/workbench/hooks/useDraftState'
import { useRunScript } from './features/workbench/hooks/useRunScript'
import { useScriptsApi } from './features/workbench/hooks/useScriptsApi'
import { parseBindings } from './features/workbench/lib/bindings'
import { formatError } from './features/workbench/lib/errors'

function App() {
  const {
    script,
    setScript,
    scriptName,
    setScriptName,
    bindingsText,
    setBindingsText,
    selectedScriptId,
    setSelectedScriptId,
    resetToDefault,
    duplicate,
  } = useDraftState()

  const { run, runMutation, runData } = useRunScript()

  const { scriptsQuery, loadScriptMutation, saveScriptMutation } = useScriptsApi({
    onScriptLoaded: (loadedScript) => {
      setSelectedScriptId(loadedScript.id)
      setScript(loadedScript.script)
      setScriptName(loadedScript.name)
    },
    onScriptSaved: (savedScript) => {
      setSelectedScriptId(savedScript.id)
      setScriptName(savedScript.name)
    },
  })

  const parsedBindingsState = useMemo(() => parseBindings(bindingsText), [bindingsText])

  const saveDisabled = saveScriptMutation.isPending || script.trim().length === 0
  const runDisabled =
    runMutation.isPending || script.trim().length === 0 || parsedBindingsState.value === null

  const runErrorMessage = formatError(runMutation.error)
  const saveErrorMessage = formatError(saveScriptMutation.error)

  const scripts = scriptsQuery.data ?? []

  function handleSave() {
    saveScriptMutation.mutate({
      id: selectedScriptId ?? undefined,
      name: scriptName.trim().length > 0 ? scriptName.trim() : 'Untitled script',
      script,
    })
  }

  function handleRun() {
    if (!parsedBindingsState.value) {
      return
    }
    run(script, parsedBindingsState.value)
  }

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      <div className="mx-auto flex h-screen max-w-[1800px] flex-col px-4 py-4 sm:px-6">
        <WorkbenchHeader
          onNew={resetToDefault}
          onDuplicate={duplicate}
          onSave={handleSave}
          onRun={handleRun}
          saveDisabled={saveDisabled}
          runDisabled={runDisabled}
          savePending={saveScriptMutation.isPending}
          runPending={runMutation.isPending}
        />

        <div className="grid flex-1 gap-4 lg:grid-cols-[260px_1fr]">
          <ScriptsSidebar
            scriptsQuery={scriptsQuery}
            scripts={scripts}
            selectedScriptId={selectedScriptId}
            onSelectScript={(id) => loadScriptMutation.mutate(id)}
          />

          <section className="grid min-h-0 gap-4 lg:grid-cols-[1.2fr_1fr]">
            <ScriptEditorPane
              scriptName={scriptName}
              onScriptNameChange={setScriptName}
              script={script}
              onScriptChange={setScript}
              onRunShortcut={handleRun}
            />

            <article className="grid min-h-0 grid-rows-[0.9fr_1.1fr] gap-4">
              <BindingsEditorPane
                bindingsText={bindingsText}
                onBindingsTextChange={setBindingsText}
              />

              <ResultView
                runData={runData}
                runErrorMessage={runErrorMessage}
                saveErrorMessage={saveErrorMessage}
              />
            </article>
          </section>
        </div>
      </div>
    </div>
  )
}

export default App
