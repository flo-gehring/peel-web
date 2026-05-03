export type JsonPrimitive = string | number | boolean | null

export type JsonValue = JsonPrimitive | JsonObject | JsonValue[]

export interface JsonObject {
  [key: string]: JsonValue
}

export interface ScriptSummary {
  id: string
  name: string
}

export interface DocumentSummary {
  id: string
  name: string
  updatedAt: string
}

export interface ScriptDetail {
  id: string
  name: string
  script: string
}

export interface DocumentDetail {
  id: string
  name: string
  content: JsonObject
  exampleBindings: JsonObject
  createdAt: string
  updatedAt: string
}

export interface ScriptSaveRequest {
  id?: string
  name: string
  script: string
}

export interface DocumentSaveRequest {
  id?: string
  name: string
  content: JsonObject
  exampleBindings: JsonObject
}

export interface DocumentPreviewRequest {
  content: JsonObject
  exampleBindings: JsonObject
}

export interface DocumentPreviewReferenceStatus {
  refId: string
  scriptId: string
  status: 'ok' | 'error'
}

export interface DocumentPreviewDiagnostic {
  refId: string
  code: string
  message: string
}

export interface DocumentPreviewResponse {
  renderedContent: JsonObject
  references: DocumentPreviewReferenceStatus[]
  diagnostics: DocumentPreviewDiagnostic[]
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
