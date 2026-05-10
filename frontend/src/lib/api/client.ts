import { z } from 'zod'

import type {
  ApiErrorPayload,
  DocumentDetail,
  DocumentPreviewRequest,
  DocumentPreviewResponse,
  DocumentSaveRequest,
  DocumentSaveResponse,
  DocumentSummary,
  JsonValue,
  RenderConfigurationCreateResponse,
  RenderConfigurationDetail,
  RenderConfigurationDto,
  RenderConfigurationSummary,
  RunRequest,
  RunResponse,
  ScriptDetail,
  ScriptSaveRequest,
  ScriptSummary,
  ValidationRequest,
  ValidationResponse,
} from './types'

const scriptSummarySchema = z.object({
  id: z.string(),
  name: z.string(),
})

const scriptDetailSchema = z.object({
  id: z.string(),
  name: z.string(),
  script: z.string(),
})

const documentSummarySchema = z.object({
  id: z.string(),
  name: z.string(),
  updatedAt: z.string(),
})

const validationDiagnosticSchema = z.object({
  line: z.number(),
  column: z.number(),
  endLine: z.number(),
  endColumn: z.number(),
  severity: z.enum(['error', 'warning', 'info']),
  message: z.string(),
})

const validationResponseSchema = z.object({
  diagnostics: z.array(validationDiagnosticSchema),
})

const jsonValueSchema: z.ZodType<JsonValue> = z.lazy(() =>
  z.union([z.string(), z.number(), z.boolean(), z.null(), z.array(jsonValueSchema), z.record(z.string(), jsonValueSchema)]),
)

const documentDetailSchema = z.object({
  id: z.string(),
  createdAt: z.string(),
  updatedAt: z.string(),
})

const documentSaveResponseSchema = z.object({
  id: z.string(),
  createdAt: z.string(),
  updatedAt: z.string(),
  type: z.enum(['CREATED', 'UPDATED']),
})

const documentPreviewResponseSchema = z.object({
  html: z.string(),
})

const runResponseSchema = z.object({
  trace: z.record(z.string(), jsonValueSchema),
  result: z.record(z.string(), jsonValueSchema),
})

const apiErrorSchema = z.object({
  error: z.object({
    code: z.string(),
    message: z.string(),
    details: z.record(z.string(), z.unknown()).optional(),
  }),
})

const traceExpressionKindSchema = z.enum([
  'LITERAL',
  'BINARY_OPERATOR',
  'UNARY_PREFIX_OPERATOR',
  'VARIABLE_NAME',
  'FUNCTION_CALL',
  'RETURN_EXPR',
  'ASSIGNMENT',
  'IF_STATEMENT',
  'WHILE_LOOP',
  'FOR_EACH_LOOP',
  'LIST_LITERAL',
  'MAP_LITERAL',
  'SELECTOR',
  'BLOCK',
])

const renderConfigurationDtoSchema = z.object({
  renderConfigurations: z.partialRecord(traceExpressionKindSchema, z.string()),
})

const renderConfigurationSummarySchema = z.object({
  id: z.string(),
  name: z.string(),
})

const renderConfigurationDetailSchema = z.object({
  name: z.string(),
  renderConfigurationDto: renderConfigurationDtoSchema,
})

const renderConfigurationCreateResponseSchema = z.object({
  id: z.string(),
})

export class ApiClientError extends Error {
  readonly code: string
  readonly status: number
  readonly details?: Record<string, unknown>

  constructor(status: number, payload: ApiErrorPayload) {
    super(payload.error.message)
    this.name = 'ApiClientError'
    this.code = payload.error.code
    this.status = status
    this.details = payload.error.details
  }
}

async function request<T>(
  path: string,
  init: RequestInit,
  schema: z.ZodType<T>,
): Promise<T> {
  const response = await fetch(path, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(init.headers ?? {}),
    },
  })

  const text = await response.text()
  const rawBody = text.length > 0 ? JSON.parse(text) : {}

  if (!response.ok) {
    const payload = apiErrorSchema.safeParse(rawBody)
    if (payload.success) {
      throw new ApiClientError(response.status, payload.data)
    }
    throw new Error(`Request failed with status ${response.status}`)
  }

  return schema.parse(rawBody)
}

export function listScripts(): Promise<ScriptSummary[]> {
  return request('/api/scripts', { method: 'GET' }, z.array(scriptSummarySchema))
}

export function listDocuments(): Promise<DocumentSummary[]> {
  return request('/api/documents', { method: 'GET' }, z.array(documentSummarySchema))
}

export function getScript(id: string): Promise<ScriptDetail> {
  return request(`/api/scripts/${id}`, { method: 'GET' }, scriptDetailSchema)
}

export function getDocument(id: string): Promise<DocumentDetail> {
  return request(`/api/documents/${id}`, { method: 'GET' }, documentDetailSchema)
}

export function saveScript(payload: ScriptSaveRequest): Promise<ScriptDetail> {
  return request(
    '/api/scripts',
    { method: 'POST', body: JSON.stringify(payload) },
    scriptDetailSchema,
  )
}

export function saveDocument(payload: DocumentSaveRequest): Promise<DocumentSaveResponse> {
  return request(
    '/api/documents',
    { method: 'POST', body: JSON.stringify(payload) },
    documentSaveResponseSchema,
  )
}

export async function deleteDocument(id: string): Promise<void> {
  await request(
    `/api/documents/${id}`,
    { method: 'DELETE' },
    z.object({}).passthrough(),
  )
}

export function previewDocument(payload: DocumentPreviewRequest): Promise<DocumentPreviewResponse> {
  return request(
    '/api/documents/preview',
    { method: 'POST', body: JSON.stringify(payload) },
    documentPreviewResponseSchema,
  )
}

export function runScript(payload: RunRequest): Promise<RunResponse> {
  return request('/api/run', { method: 'POST', body: JSON.stringify(payload) }, runResponseSchema)
}

export function validateScript(payload: ValidationRequest): Promise<ValidationResponse> {
  return request(
    '/api/validate',
    { method: 'POST', body: JSON.stringify(payload) },
    validationResponseSchema,
  )
}

export function listRenderConfigurations(): Promise<RenderConfigurationSummary[]> {
  return request('/api/render-config/list-ids', { method: 'GET' }, z.array(renderConfigurationSummarySchema))
}

export function getRenderConfiguration(id: string): Promise<RenderConfigurationDetail> {
  return request(`/api/render-config/${id}`, { method: 'GET' }, renderConfigurationDetailSchema)
}

export function getDefaultRenderConfiguration(): Promise<RenderConfigurationDto> {
  return request('/api/render-config/default', { method: 'GET' }, renderConfigurationDtoSchema)
}

export function createRenderConfiguration(payload: RenderConfigurationDetail): Promise<RenderConfigurationCreateResponse> {
  return request(
    '/api/render-config/save',
    { method: 'POST', body: JSON.stringify(payload) },
    renderConfigurationCreateResponseSchema,
  )
}

export async function updateRenderConfiguration(id: string, payload: RenderConfigurationDetail): Promise<void> {
  await request(
    `/api/render-config/update/${id}`,
    { method: 'PUT', body: JSON.stringify(payload) },
    z.object({}).passthrough(),
  )
}
