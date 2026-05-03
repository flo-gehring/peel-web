import {Banana} from 'lucide-react'

export function WorkbenchHeader() {
    return (
        <header
            className="mb-4 flex flex-wrap items-center justify-between gap-3 border border-slate-800/80 bg-slate-900/80 px-4 py-3">
            <div>
                <div className="flex flex-wrap items-center gap-2">
                    <Banana color="yellow"/> PEEL
                </div>
                <p className="text-xs uppercase tracking-[0.24em] text-cyan-300/80"> Workbench</p>
            </div>

        </header>
    )
}
