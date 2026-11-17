import { watch } from 'vue'
import { createI18n } from 'vue-i18n'
import en from './en.json'
import de from './de.json'

export type Locale = 'de' | 'en'
const storageKey = 'peel.locale'

export function resolveLocale(saved: string | null, languages: readonly string[]): Locale {
  if (saved === 'de' || saved === 'en') return saved
  for (const language of languages) {
    const base = language.toLowerCase().split('-')[0]
    if (base === 'de' || base === 'en') return base
  }
  return 'en'
}

function initialLocale(): Locale {
  let saved: string | null = null
  try {
    saved = localStorage.getItem(storageKey)
  } catch {
    // Language selection also works when browser storage is unavailable.
  }
  return resolveLocale(saved, typeof navigator === 'undefined' ? [] : navigator.languages)
}

export const i18n = createI18n({
  legacy: false,
  locale: initialLocale(),
  fallbackLocale: 'en',
  messages: { en, de },
})

export const locale = i18n.global.locale
export type MessageKey = keyof typeof en
export function t(key: MessageKey, values: Record<string, unknown> = {}): string {
  return i18n.global.t(key, values)
}

export function setLocale(value: Locale): void {
  locale.value = value
  try {
    localStorage.setItem(storageKey, value)
  } catch {
    // Keep the in-memory selection even if storage is blocked.
  }
}

export function formatDateTime(value: string | number, timeOnly = false): string {
  const options: Intl.DateTimeFormatOptions = timeOnly
    ? { hour: '2-digit', minute: '2-digit', second: '2-digit' }
    : { dateStyle: 'short', timeStyle: 'medium' }
  return new Intl.DateTimeFormat(locale.value === 'de' ? 'de-DE' : 'en-GB', options).format(
    new Date(value),
  )
}

watch(
  locale,
  (value) => {
    if (typeof document !== 'undefined') document.documentElement.lang = value
  },
  { immediate: true },
)
