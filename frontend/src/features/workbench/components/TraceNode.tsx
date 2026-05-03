import clsx from 'clsx'

type TraceNodeProps = {
  label: string
  value: unknown
  path: string
  selectedPath: string
  onSelect: (path: string) => void
  depth: number
}

export function TraceNode({ label, value, path, selectedPath, onSelect, depth }: TraceNodeProps) {
  const isObject = value !== null && typeof value === 'object'
  const isArray = Array.isArray(value)

  const entries = isObject
    ? isArray
      ? value.map((item, index) => [String(index), item] as const)
      : Object.entries(value as Record<string, unknown>)
    : []

  return (
    <div>
      <button
        type="button"
        onClick={() => onSelect(path)}
        className={clsx(
          'mb-1 w-full rounded px-2 py-1 text-left text-xs transition',
          selectedPath === path
            ? 'bg-cyan-500/20 text-cyan-100'
            : 'text-slate-200 hover:bg-slate-800/70',
        )}
        style={{ paddingLeft: `${8 + depth * 14}px` }}
      >
        <span className="text-slate-400">{label}</span>
        <span className="ml-2 text-slate-200">{renderPreview(value)}</span>
      </button>
      {entries.map(([childLabel, childValue]) => (
        <TraceNode
          key={`${path}.${childLabel}`}
          label={childLabel}
          value={childValue}
          path={`${path}.${childLabel}`}
          selectedPath={selectedPath}
          onSelect={onSelect}
          depth={depth + 1}
        />
      ))}
    </div>
  )
}

function renderPreview(value: unknown): string {
  if (value === null) {
    return 'null'
  }

  if (Array.isArray(value)) {
    return `[${value.length}]`
  }

  if (typeof value === 'object') {
    const typeValue = (value as Record<string, unknown>).type
    if (typeof typeValue === 'string') {
      return `{${typeValue}}`
    }
    return '{...}'
  }

  if (typeof value === 'string') {
    return `"${value}"`
  }

  return String(value)
}
