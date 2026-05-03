import clsx from 'clsx'
import { useMemo, useState } from 'react'

import { BindingsEditorPane } from './BindingsEditorPane'
import { ResultView } from './ResultView'
import { ScriptEditorPane } from './ScriptEditorPane'
import { ScriptsSidebar } from './ScriptsSidebar'
import { useDraftState } from '../hooks/useDraftState'
import { useRunScript } from '../hooks/useRunScript'
import { useScriptsApi } from '../hooks/useScriptsApi'
import { parseBindings } from '../lib/bindings'
import { formatError } from '../lib/errors'

const SIDEBAR_STATE_STORAGE_KEY = 'peel-workbench-sidebar-open'
const TEST_DATA_STATE_STORAGE_KEY = 'peel-workbench-test-data-open'
const RESULT_VALUE_STATE_STORAGE_KEY = 'peel-workbench-result-open'
const DETAIL_STATE_STORAGE_KEY = 'peel-workbench-detail-open'

export function WorkbenchMode() {
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
  const [isSidebarOpen, setIsSidebarOpen] = useState<boolean>(() => {
    const raw = localStorage.getItem(SIDEBAR_STATE_STORAGE_KEY)
    if (raw === null) {
      return true
    }
    return raw === 'true'
  })
  const [isTestDataOpen, setIsTestDataOpen] = useState<boolean>(() => {
    const raw = localStorage.getItem(TEST_DATA_STATE_STORAGE_KEY)
    if (raw === null) {
      return true
    }
    return raw === 'true'
  })
  const [isResultOpen, setIsResultOpen] = useState<boolean>(() => {
    const raw = localStorage.getItem(RESULT_VALUE_STATE_STORAGE_KEY)
    if (raw === null) {
      return true
    }
    return raw === 'true'
  })
  const [isDetailOpen, setIsDetailOpen] = useState<boolean>(() => {
    const raw = localStorage.getItem(DETAIL_STATE_STORAGE_KEY)
    if (raw === null) {
      return false
    }
    return raw === 'true'
  })

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

  function toggleSidebar() {
    const nextState = !isSidebarOpen
    setIsSidebarOpen(nextState)
    localStorage.setItem(SIDEBAR_STATE_STORAGE_KEY, String(nextState))
  }

  function toggleTestDataPanel() {
    const nextState = !isTestDataOpen
    setIsTestDataOpen(nextState)
    localStorage.setItem(TEST_DATA_STATE_STORAGE_KEY, String(nextState))
  }

  function toggleResultPanel() {
    const nextState = !isResultOpen
    setIsResultOpen(nextState)
    localStorage.setItem(RESULT_VALUE_STATE_STORAGE_KEY, String(nextState))
  }

  function toggleDetailPanel() {
    const nextState = !isDetailOpen
    setIsDetailOpen(nextState)
    localStorage.setItem(DETAIL_STATE_STORAGE_KEY, String(nextState))
  }

  return (
    <div
      className={clsx(
        'grid flex-1 gap-4 overflow-hidden',
        isSidebarOpen ? 'lg:grid-cols-[260px_1fr]' : 'lg:grid-cols-[56px_1fr]',
      )}
    >
      <ScriptsSidebar
        scriptsQuery={scriptsQuery}
        scripts={scripts}
        selectedScriptId={selectedScriptId}
        isOpen={isSidebarOpen}
        onToggle={toggleSidebar}
        onSelectScript={(id) => loadScriptMutation.mutate(id)}
      />

      <section className="grid min-h-0 gap-4 overflow-hidden lg:grid-cols-[1.2fr_1fr]">
        <ScriptEditorPane
          scriptName={scriptName}
          onScriptNameChange={setScriptName}
          script={script}
          onScriptChange={setScript}
          onRunShortcut={handleRun}
          onNew={resetToDefault}
          onDuplicate={duplicate}
          onSave={handleSave}
          onRun={handleRun}
          saveDisabled={saveDisabled}
          runDisabled={runDisabled}
          savePending={saveScriptMutation.isPending}
          runPending={runMutation.isPending}
        />

        <article className="flex min-h-0 flex-col gap-4 overflow-hidden">
          <div
            className={clsx(
              'min-h-0 overflow-hidden',
              isResultOpen ? 'basis-0 grow' : 'shrink-0',
            )}
          >
            <ResultView
              runData={runData}
              runErrorMessage={runErrorMessage}
              saveErrorMessage={saveErrorMessage}
              isResultOpen={isResultOpen}
              onToggleResultOpen={toggleResultPanel}
              isDetailOpen={isDetailOpen}
              onToggleDetailOpen={toggleDetailPanel}
            />
          </div>

          <div
            className={clsx(
              'min-h-0 overflow-hidden',
              isTestDataOpen ? 'basis-0 grow' : 'shrink-0',
            )}
          >
            <BindingsEditorPane
              bindingsText={bindingsText}
              onBindingsTextChange={setBindingsText}
              isOpen={isTestDataOpen}
              onToggleOpen={toggleTestDataPanel}
            />
          </div>
        </article>
      </section>
    </div>
  )
}
