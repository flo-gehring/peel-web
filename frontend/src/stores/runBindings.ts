import { t } from '@/i18n'
import { ref } from 'vue'
import { defineStore } from 'pinia'

type BindingsMap = Record<string, unknown>

export const useRunBindingsStore = defineStore('runBindings', () => {
  const rawJson = ref('{}')
  const parseError = ref<string | null>(null)
  const lastValidBindings = ref<BindingsMap>({})

  function parseBindings(): BindingsMap | null {
    try {
      const parsed = JSON.parse(rawJson.value) as unknown

      if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
        parseError.value = t('bindingsJsonMustBeAnObjectAtTheTop')
        return null
      }

      const normalized = parsed as Record<string, unknown>
      const bindings = normalized as BindingsMap
      lastValidBindings.value = bindings
      parseError.value = null
      return bindings
    } catch (error) {
      parseError.value = error instanceof Error ? error.message : t('invalidJson2')
      return null
    }
  }

  function setRawJson(value: string): void {
    rawJson.value = value
    parseBindings()
  }

  return {
    rawJson,
    parseError,
    lastValidBindings,
    setRawJson,
    parseBindings,
  }
})
