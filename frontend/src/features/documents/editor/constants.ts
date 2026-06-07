import { createCommand } from 'lexical'

export const INSERT_SCRIPT_REF_COMMAND = createCommand<string>('INSERT_SCRIPT_REF_COMMAND')
export const INSERT_PEBBLE_INLINE_COMMAND = createCommand<string>('INSERT_PEBBLE_INLINE_COMMAND')
export const INSERT_PEBBLE_BLOCK_COMMAND = createCommand<string>('INSERT_PEBBLE_BLOCK_COMMAND')
