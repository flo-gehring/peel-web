import { z } from 'zod'

import type {
  ApiErrorPayload,
  JsonValue,
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

export function getScript(id: string): Promise<ScriptDetail> {
  return request(`/api/scripts/${id}`, { method: 'GET' }, scriptDetailSchema)
}

export function saveScript(payload: ScriptSaveRequest): Promise<ScriptDetail> {
  return request(
    '/api/scripts',
    { method: 'POST', body: JSON.stringify(payload) },
    scriptDetailSchema,
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
