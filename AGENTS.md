# Peel Web

## Scope and Principles

- This is a pre-production application. Do not add compatibility or data-migration work unless explicitly requested.
- Build the smallest maintainable solution. Avoid speculative abstractions and features.
- Ask when a requirement is ambiguous; do not silently infer product behavior.
- Existing unrelated worktree changes may belong to the user. Do not revert them.

## Repository Layout

- `frontend/`: Vue 3, TypeScript, Vite application.
- `peelserver/`: Spring Boot 4 / Java 25 backend with MongoDB.
- `src/types/api.ts`: backend-generated OpenAPI TypeScript definitions.
- `frontend/src/types/api.ts`: copy consumed by `openapi-fetch`; keep it synchronized with the generated backend type file after API changes.
- `peelserver/AGENTS.md`: backend-specific product and coding guidance. Read it before backend changes.

## Frontend Commands

Run commands from `frontend/` using npm, the project package manager:

- `npm run dev`
- `npm run type-check`
- `npm run build`
- `npm run lint`
- `npm run format`

The Vite alias `@/` resolves to `frontend/src/`. The Vite dev server proxies `/api` to `http://host.docker.internal:8080`.

### Current Verification Caveat

`npm run type-check` may currently fail before checking feature code with unresolved `*.vue` imports such as `./App.vue` and views. This is a project TypeScript/Vue module-resolution configuration issue, not necessarily a feature failure. Do not mask it with unrelated changes; report it if encountered.

## Frontend Architecture

### API Client

- Use `api` from `frontend/src/adapter/client.ts`.
- The client base URL is `/api`; call paths without the prefix, e.g. `api.GET('/scripts')`, `api.POST('/run')`.
- Use generated endpoint types. When a backend OpenAPI endpoint is added, update/regenerate `frontend/src/types/api.ts` before using it; the root `src/types/api.ts` may already contain the new definition.

### Workspace Documents

- `frontend/src/adapter/ClientTypeDefinition.ts` exports `PeelWorkspaceDocument`, currently a discriminated union.
- The existing file tree contains only `Extract<PeelWorkspaceDocument, { kind: 'peel' }>` entries with `id`, `name`, and `icon`.
- Add render-config and document behavior as new union variants; do not overload the peel variant with unrelated data.

### Dockview

- `App.vue` owns the `DockviewApi`, panel registry, initial layout, and editor group id.
- Script editor panel ids use `editor-script-${scriptId}`. Use script id, not name, as identity.
- New script tabs must be added to `editor-group`, not the active group.
- The output panel id is `output-console`; the global bindings panel id is `bindings-json`.
- Dockview wraps custom panel parameters. In panel components, application data is generally at `props.params?.params`, while Dockview API/context is at `props.params`.
- Do not pass function callbacks via Dockview panel `params`; they are not reliable through Dockview updates/serialization. Use Pinia for cross-panel communication instead.

### Pinia Stores

- `workspaceSelection.ts`: file-selection events and script-list refresh/delete notifications.
- `editorDrafts.ts`: in-memory editor drafts. Drafts are keyed by document id when available, otherwise panel id. Bind a panel to the returned document id after a create/save. Clear draft state when its script is deleted.
- `runBindings.ts`: global JSON bindings for all scripts. Validate JSON before execution.
- `runOutput.ts`: latest run request, status, error, and response shared with `OutputPanel`.
- Pinia state is intentionally session-only; no persistence is implemented.

### Script Lifecycle

- File tree fetches `GET /scripts`; clicking a script selects it through `workspaceSelection`.
- `App.vue` watches selection changes, loads `GET /scripts/{id}` only for first tab open, and uses cached drafts on re-focus/re-open.
- Creating a script should call `POST /scripts` immediately with initial name and empty content, then add/select the returned script so its tab is bound to the returned id.
- Saving calls `POST /scripts` with `{ id?, name, script }`. A missing id means create; a present id means update.
- Deleting calls `DELETE /scripts/{id}` only after confirmation. On success remove the explorer item, clear the related draft, clear selection if needed, and close `editor-script-${id}` if open.

### Running Scripts

- The ▶ button in `GroupActions.vue` runs the currently active editor's latest draft.
- Call `POST /run` with `{ script, bindings }`.
- Bindings come from the global bindings store and must be valid JSON with an object top level; each value is currently required to be an object, matching generated `RunRequest` typing.
- Send run state to `runOutput.ts`; `OutputPanel.vue` renders request, error, trace, and result as read-only formatted JSON.
- Keep OutputPanel scrollable for large results.

## UI Conventions

- Preserve the current dark Dockview/Monaco visual language.
- Use Monaco (`@guolao/vue-monaco-editor`) for script and JSON editing.
- Explorer creation uses the plus button and a prompt with a type selector. Only `PEEL Script` is enabled now; retain the selector structure for future render configs/documents.
- Destructive explorer actions require confirmation and should stop click propagation so they do not also select/open the file.

## Editing and Validation

- Prefer small direct edits and existing stores/components over new frameworks or event buses.
- Use `apply_patch` for code changes.
- Run `git diff --check` after edits.
- Do not introduce Bun lockfiles or switch package managers; this project uses npm.
- When type-checking is blocked by the current Vue-module issue, still inspect relevant imports, generated API types, and `git diff --check`.
