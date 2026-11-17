# PEEL language support

Scripts store their input contract alongside their source:

```json
{
  "type": "script",
  "source": "var gross = amount * 1.19;",
  "expectedBindings": [
    { "name": "amount", "description": "Net amount" }
  ]
}
```

Every expected input is required. Names must be unique PEEL identifiers and
cannot be reserved words. Descriptions can be empty. A key with JSON null counts
as present and retains the existing conversion to None. Extra values are allowed.
There are no optional inputs, types, defaults, object-field schemas or migrations.

## Editor and drafts

File settings contain the expected-input form. Inputs, source, name and
description save together with the last acknowledged file version. Contract
drafts participate in tab status, navigation, project switching, logout, reload
and deletion. Edits during a save stay dirty. Run uses the current source/contract
draft without saving; document previews and jobs use stored referenced scripts.

Script tabs always use PEEL regardless of filename; render configurations retain
JSON. A local Monarch tokenizer gives immediate colors and bracket/comment support.
The PEEL core analyzer supplies semantic colors, scopes, references and diagnostics.
Functions, parameters and variables are distinguished; external inputs are readonly
global values. Variables holding functions remain variables without type inference.

Named blocks retain their interpreter scopes. Labels such as `report: {` are
highlighted separately and do not become variables or completion suggestions.
Duplicate block names are diagnosed across the complete script, including
unexecuted branches and functions.

Parsing does not build a block catalog. Consumers needing block definitions or
possible descendants explicitly call `BlockCatalog.analyze(PeelGrammar.parse(source))`.
The block endpoint and document rendering request this analysis; ordinary script
execution only parses and runs the program.

Ctrl+Space offers visible local names, expected inputs, runtime symbols and
keywords. Descriptions appear in completion and hover. Expected inputs require no
test values. Analysis runs after 300 ms of inactivity, and old/cancelled responses
cannot replace newer results. Without current analysis, suggestions use the current
expected inputs and previously discovered runtime symbols. Network errors do not
become syntax diagnostics or disable local syntax colors.

## HTTP API

`POST /api/scripts/analyze` replaces GET validation. The body contains
`projectId: { id }`, `script` and `expectedBindings`. Analysis requires project
access and accepts empty or unfinished source. It returns symbols, occurrences,
scopes and diagnostics. All ranges are zero-based UTF-16 offsets with exclusive
ends. Unknown reads are warnings. Analysis executes no script or host function.
The flat analysis fields use enum names in JSON: kind is VARIABLE, PARAMETER or
FUNCTION; origin is LOCAL, EXTERNAL or RUNTIME; role is DECLARATION, REFERENCE,
ASSIGNMENT or CALL. Diagnostic severity is WARNING or ERROR; diagnostic codes
such as UNKNOWN_NAME and SYNTAX_ERROR are enums as well. OpenAPI lists their
allowed values, and the generated TypeScript definitions expose string unions.
Runtime names/arity come from the same RuntimeBuilder configuration used to
register the functions.

`PUT /api/scripts/run` additionally receives `expectedBindings` for the source
draft being run. Missing values produce HTTP 400 with `missing_bindings` and
`error.details.missingBindings`. Document preview validates all referenced
contracts before executing any script.

## Print jobs

Before CALCULATING and scheduling, the job checks its CSV against the union of
expected names from the scripts resolved at start. Header matching is exact and
case-sensitive. Extra columns and empty cells are allowed; duplicate/unnamed
headers are invalid. Validation and execution share one CSV format.

Missing columns return HTTP 400 with code `missing_csv_columns`, a
`missingColumns` list and a `scripts` map assigning missing inputs to script
names in `error.details`. Structured document references are resolved by file ID;
their contracts are checked together before any referenced script executes.
The job stays CREATED so a corrected upload can start.
CSV replacement is restricted to CREATED. The worker uses the same resolved
scripts/contracts checked before scheduling; later file edits do not change them.

Inputs used only by document templates, value types, nested properties, column
mapping and historical snapshots at job creation are outside this version.

## Verification and local checkouts

- Frontend: `npm run test`, `npm run type-check`, `npm run build`.
- Backend: `./gradlew.bat test` with Java 25 and Docker.
- Core: `./gradlew.bat test` with its Java 22 toolchain.
- `LanguageContractTest` exports the full API to `build/openapi.json`; use
  openapi-typescript to synchronize both API type files.
- A browser smoke check used the real EditorPanel/Monaco with a mocked analysis
  transport. It verified Ctrl+Space, semantic colors and the input form; separate
  backend tests exercise the real analyzer and request handling.

Both repositories are needed. The existing Gradle composite build uses the
adjacent PEEL checkout. Core changes must be integrated along with the web changes.
