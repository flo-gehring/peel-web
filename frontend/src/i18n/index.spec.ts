import { afterEach, describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { formatDateTime, locale, resolveLocale, setLocale, t, type MessageKey } from './index'
import en from './en.json'
import de from './de.json'
import LanguageSelect from '@/components/LanguageSelect.vue'
import EditorWelcome from '@/components/EditorWelcome.vue'

afterEach(() => {
  setLocale('en')
  localStorage.removeItem('peel.locale')
})

describe('internationalization', () => {
  it('uses a saved choice, then supported browser languages, then English', () => {
    expect(resolveLocale('en', ['de-DE'])).toBe('en')
    expect(resolveLocale(null, ['fr-FR', 'de-AT'])).toBe('de')
    expect(resolveLocale(null, ['en-US', 'de-DE'])).toBe('en')
    expect(resolveLocale('invalid', ['de-DE'])).toBe('de')
    expect(resolveLocale(null, ['fr-FR'])).toBe('en')
    expect(resolveLocale(null, [])).toBe('en')
  })

  it('provides matching, compilable catalogs with matching placeholders', () => {
    expect(Object.keys(de).sort()).toEqual(Object.keys(en).sort())
    for (const key of Object.keys(en) as MessageKey[]) {
      const placeholders = (value: string) =>
        [...value.matchAll(/\{(\w+)\}/g)].map((m) => m[1]).sort()
      expect(placeholders(de[key]), key).toEqual(placeholders(en[key]))
      for (const language of ['de', 'en'] as const) {
        locale.value = language
        const values: Record<string, string | number> = {
          confirmation: 'TRANSFER Äpfel',
          name: 'Äpfel',
          names: 'A, B',
          email: 'a@example.test',
          seconds: 3,
          project: 'Äpfel',
          symbol: '＋',
          kind: 'EXPR',
        }
        const expected = (language === 'de' ? de : en)[key].replace(/\{(\w+)\}/g, (_, name) =>
          String(values[name]),
        )
        expect(t(key, values), `${language}: ${key}`).toBe(expected)
      }
    }
  })

  it('switches mounted UI, stores the selection and updates document language', async () => {
    setLocale('en')
    const select = mount(LanguageSelect)
    const welcome = mount(EditorWelcome)
    expect(welcome.text()).toContain('Ready when you are')
    await select.get('select').setValue('de')
    await nextTick()
    expect(welcome.text()).toContain('Bereit, wenn du es bist')
    expect(document.documentElement.lang).toBe('de')
    expect(localStorage.getItem('peel.locale')).toBe('de')
    select.unmount()
    welcome.unmount()
  })

  it('formats dates with the selected language', () => {
    setLocale('de')
    const german = formatDateTime('2026-11-12T12:00:00Z')
    setLocale('en')
    expect(formatDateTime('2026-11-12T12:00:00Z')).not.toBe(german)
  })
})
