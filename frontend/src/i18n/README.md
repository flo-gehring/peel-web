# UI translations

PEEL uses Vue I18n with German (`de`) and English (`en`) catalogs. The initial
language comes from the first supported browser language, defaulting to English.
The language selector stores an explicit choice under `peel.locale` in localStorage.

Import `t` from `@/i18n` in components and stores. Add each key to both `en.json`
and `de.json`. Translate complete sentences and pass variable values as named
parameters, for example `t('deleteName', { name: file.name })`. Keep translated
labels in computed values when storing them in component state. Dockview titles
are updated explicitly when the locale changes. Use `formatDateTime` for UI dates.

Project transfer deliberately requires the language-independent API token
`TRANSFER <exact project name>` in both languages. Only the surrounding instruction
is translated. The displayed phrase, submit validation and API payload must agree;
never translate or normalize the token or project name. Project deletion similarly
requires the exact, untranslated project name.

API errors with stable codes are translated in `adapter/apiError.ts`. Known project
validation messages share broad codes and are matched against the English `api*`
catalog entries. Unknown script/server diagnostics remain verbatim.

`index.spec.ts` checks catalog parity, placeholders, formatting and live language
switching. Project settings tests exercise transfer validation and submission in
both languages.
