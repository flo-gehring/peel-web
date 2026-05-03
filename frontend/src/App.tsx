import { useState } from 'react'

import { DocumentsMode } from './features/documents/components/DocumentsMode'
import { WorkbenchHeader } from './features/workbench/components/WorkbenchHeader'
import type { AppMode } from './features/workbench/components/WorkbenchHeader'
import { WorkbenchMode } from './features/workbench/components/WorkbenchMode'

function App() {
  const [mode, setMode] = useState<AppMode>('workbench')

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      <div className="mx-auto flex h-screen max-w-[1800px] flex-col px-4 py-4 sm:px-6">
        <WorkbenchHeader mode={mode} onModeChange={setMode} />

        {mode === 'workbench' ? <WorkbenchMode /> : null}
        {mode === 'documents' ? <DocumentsMode /> : null}
        {mode === 'batch' ? (
          <section className="flex flex-1 items-center justify-center border border-dashed border-slate-700 bg-slate-900/40">
            <p className="text-sm text-slate-400">Batch mode will be implemented after document workflows are complete.</p>
          </section>
        ) : null}
      </div>
    </div>
  )
}

export default App
