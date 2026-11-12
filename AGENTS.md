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
- The client base URL is empty; call generated paths including `/api`, e.g. `api.GET('/api/projects/{projectId}/files', ...)`.
- Use generated endpoint types. When a backend OpenAPI endpoint is added, update/regenerate `frontend/src/types/api.ts` before using it; the root `src/types/api.ts` may already contain the new definition.

### Workspace Documents

- `frontend/src/adapter/ClientTypeDefinition.ts` exports `PeelWorkspaceDocument`, currently a discriminated union.
- The existing file tree contains only `Extract<PeelWorkspaceDocument, { kind: 'peel' }>` entries with `id`, `name`, and `icon`.
- Add render-config and document behavior as new union variants; do not overload the peel variant with unrelated data.

### Dockview

- `ProjectView.vue` owns the `DockviewApi`, panel registry, initial layout, and editor group id.
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

### File Lifecycle

- Scripts, render configurations and document templates use the common `project-files` collection and `/api/projects/{projectId}/files` API; see `peelserver/docs/FILE_MANAGEMENT.md`.
- Use `projectFiles.ts` for loading, creating, saving and deleting files. It retains the acknowledged server version; never fetch a newer version implicitly before saving an existing draft.
- Create using `POST /files` with a client-generated UUID, name, description and typed `content`. Retain the ID and exact payload for retries. Creation never updates an existing file.
- Update with `PUT /files/{fileId}` and `expectedVersion`. Identical target states succeed without writing; other stale updates return `409`. Keep drafts and their baseline on failure.
- Delete after confirmation with the last known version. Referenced files return blocking usages; remove explorer items, drafts and tabs only after success.
- Lists contain metadata and derived `type`; details contain `content.type` (`script`, `render-config`, `document`). Common name/description editing is in the explorer.
- `ProjectView.vue` owns Dockview and initial loads. Cached drafts survive tab changes. Explicit reload confirms discarding that file's draft before loading the server version.

### Running Scripts

- The shared `FileToolbar.vue` below each file tab saves all file types, runs script drafts, and triggers document previews. Settings are in `FileSettings.vue`; Ctrl+S and Ctrl+Enter work within the editor.
- Call `PUT /api/scripts/run` with `{ projectId: { id }, script, bindings }`.
- Bindings come from the global bindings store and must be valid JSON with an object top level; each value is currently required to be an object, matching generated `RunRequest` typing.
- Send run state to `runOutput.ts`; `OutputPanel.vue` renders request, error, trace, and result as read-only formatted JSON.
- Keep OutputPanel scrollable for large results.

## UI Conventions

- Preserve the current dark Dockview/Monaco visual language.
- Use Monaco (`@guolao/vue-monaco-editor`) for script and JSON editing.
- Explorer sections are collapsible and each has its own plus button. Creation opens the matching script, render-configuration or document form without a type selector. Refresh is an icon in the Project files header.
- Destructive explorer actions require confirmation and should stop click propagation so they do not also select/open the file.

## Editing and Validation

- Prefer small direct edits and existing stores/components over new frameworks or event buses.
- Use `apply_patch` for code changes.
- Run `git diff --check` after edits.
- Do not introduce Bun lockfiles or switch package managers; this project uses npm.
- When type-checking is blocked by the current Vue-module issue, still inspect relevant imports, generated API types, and `git diff --check`.

### Compact editor layout

- `EditorPanel.vue` and `DocumentPanel.vue` embed the same `FileToolbar.vue`; do not add save/run controls to Dockview headers.
- Document script links and render configuration have dedicated Banana/Palette toolbar buttons and separate side panels. The gear opens only file metadata. Edit/Preview share the document area; keep the Tiptap instance mounted when switching.
- Document previews are keyed by file ID in `documentPreview.ts`. Ignore superseded/cleared requests and revoke old object URLs.
- Opening a document hides Output; the toolbar can reopen it via `workspaceSelection.requestOutput()`. Script output is restored when returning to a script.
- Metadata drafts live in `projectFiles.ts` and count as unsaved changes for tabs, navigation and logout.
