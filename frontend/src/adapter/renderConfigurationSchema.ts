import { z } from 'zod'

const traceExpressionKinds = [
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
] as const

const traceExpressionKindSet = new Set<string>(traceExpressionKinds)

const renderTemplatesSchema = z.record(z.string(), z.string()).superRefine((templates, context) => {
  for (const key of Object.keys(templates)) {
    if (!traceExpressionKindSet.has(key)) {
      context.addIssue({
        code: 'custom',
        path: [key],
        message: `Unknown trace expression kind: ${key}`,
      })
    }
  }
})

export const renderConfigurationSchema = z
  .object({
    renderConfigurations: renderTemplatesSchema,
    namedOverrides: z.record(z.string(), renderTemplatesSchema),
  })
  .strict()

export type RenderConfiguration = z.infer<typeof renderConfigurationSchema>
