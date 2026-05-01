export type JsonPrimitive = string | number | boolean | null

export type JsonValue = JsonPrimitive | JsonObject | JsonValue[]

export interface JsonObject {
  [key: string]: JsonValue
}

export interface ScriptSummary {
  id: string
  name: string
}

export interface ScriptDetail {
  id: string
  name: string
  script: string
}

export interface ScriptSaveRequest {
  id?: string
  name: string
  script: string
}

export interface RunRequest {
  script: string
  bindings: JsonObject
}

export interface RunResponse {
  trace: JsonObject
  result: JsonObject
}

export interface ValidationRequest {
  script: string
}

export interface ValidationDiagnostic {
  line: number
  column: number
  endLine: number
  endColumn: number
  severity: 'error' | 'warning' | 'info'
  message: string
}

export interface ValidationResponse {
  diagnostics: ValidationDiagnostic[]
}

export interface ApiErrorPayload {
  error: {
    code: string
    message: string
    details?: Record<string, unknown>
  }
}
