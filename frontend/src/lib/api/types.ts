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
  createdAt: string
  updatedAt: string
}

export type TraceExpressionKind =
  | 'LITERAL'
  | 'BINARY_OPERATOR'
  | 'UNARY_PREFIX_OPERATOR'
  | 'VARIABLE_NAME'
  | 'FUNCTION_CALL'
  | 'RETURN_EXPR'
  | 'ASSIGNMENT'
  | 'IF_STATEMENT'
  | 'WHILE_LOOP'
  | 'FOR_EACH_LOOP'
  | 'LIST_LITERAL'
  | 'MAP_LITERAL'
  | 'SELECTOR'
  | 'BLOCK'

export interface RenderConfigurationDto {
  renderConfigurations: Partial<Record<TraceExpressionKind, string>>
}

export interface RenderConfigurationSummary {
  id: string
  name: string
}

export interface RenderConfigurationDetail {
  name: string
  renderConfigurationDto: RenderConfigurationDto
}

export interface RenderConfigurationCreateResponse {
  id: string
}

export interface ScriptSaveRequest {
  id?: string
  name: string
  script: string
}

export interface DocumentSaveRequest {
  id: string
  name: string
  scriptNameTags: Record<string, string>
  template: string
  renderConfigurationId: string
  localOverrides: RenderConfigurationDto
}

export interface DocumentSaveResponse {
  id: string
  createdAt: string
  updatedAt: string
  type: 'CREATED' | 'UPDATED'
}

export interface DocumentPreviewRequest {
  scripTags: Record<string, { id: string }>
  bindings: JsonObject
  renderConfigId: string
  localOverrides: RenderConfigurationDto
  template: string
}

export interface DocumentPreviewResponse {
  html: string
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
