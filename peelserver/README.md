# 🍌 PEEL - Web Server
This is the web server counterpart for the [PEEL Project](https://github.com/flo-gehring/peel). 
It is basically an example application for the use cases and capabilities of PEEL.
## What is it?
A web app to generate dynamic documents based computational results. The User can supply PEEL Scripts,
input data and a document template, from which documents (PDF) will be generated.
PEEL provides not only the result but also an execution trace, how the result was computed.
This execution trace can be formatted and displayed in the document. This allows for transparent calculations for 
official documents.

## Local development: email sign-in

Use Java 25 and the adjacent `../peel` checkout. Start Docker Desktop, then run from the repository root:

```powershell
docker compose -f peelserver/compose.yaml up -d
cd peelserver
./gradlew.bat bootRun --args="--spring.profiles.active=dev"
```

Keep the existing MongoDB and MinIO connection configuration.

When launching `PeelserverApplication` from IntelliJ, set **Active profiles** to `dev` in the Spring Boot run
configuration (or set `SPRING_PROFILES_ACTIVE=dev`). Without this profile, configure SMTP and the public origin
explicitly; otherwise Spring cannot create the mail sender required by the sign-in flow.

Start the frontend in another terminal:

```powershell
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173/login`, request a link for any test email address, and open the message at
`http://localhost:8025`. Mailpit captures local SMTP mail; it does not deliver it to a real inbox.
Open the link, then confirm sign-in. Links expire after 15 minutes and are single-use.
Refreshing the confirmation page after the token has been removed from the URL requires reopening the email link.

The `dev` profile uses SMTP at `localhost:11025`, sender `noreply@peel.test`, and public frontend origin
`http://localhost:5173`. Override with `SMTP_HOST`, `SMTP_PORT`, `PEEL_MAIL_FROM`, and `PEEL_PUBLIC_BASE_URL`.
The origin in emails must be reachable by the browser, regardless of where the backend runs.
If the backend runs in a container, connect it to the Compose network and use `SMTP_HOST=mailpit` and `SMTP_PORT=1025`.
The separate devcontainer does not join that network automatically. Vite currently forwards `/api` to
`host.docker.internal:8080`, so the default development arrangement runs the backend on the host.

Only the `dev` profile allows an HTTP session cookie. Other environments default to secure cookies and require
`PEEL_PUBLIC_BASE_URL`, `PEEL_MAIL_FROM`, and Spring's SMTP configuration (`SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`,
and credentials/TLS options as appropriate). Serve the SPA and `/api` on the same public origin. The web server must
fall back to the frontend entry point for `/login`, `/login/sent`, `/login/confirm`, and other frontend routes.

Token storage and resend limits are in memory. Restarting the backend invalidates outstanding links and sessions.
Resends are limited to one per address per 60 seconds and 20 per socket IP per hour, configurable through
`peel.auth.resend-interval` and `peel.auth.requests-per-ip-per-hour`. Behind an unconfigured proxy the IP limit is shared.
Repeated requests create separate links; requesting a new link does not invalidate older unexpired ones.
Accounts are created after successful token verification. Email addresses are trimmed and lowercased and have a unique index.

API authentication uses a server session that expires after eight hours of inactivity. Fetch `/api/auth/csrf` and send its token in the returned header on all
mutating requests, including multipart uploads. Fetch a fresh token after login/logout. API failures use JSON
with `error.code` and `error.message`; unauthenticated requests return 401 instead of redirects.
An expired session leaves drafts and the last authenticated email address in memory. The session dialog prefills
that address, sends a sign-in email on request, and accepts the code from the email without leaving the current tab.
The email link and code use the same single-use token, valid for 15 minutes. Neither is stored in browser storage.
After successful sign-in the dialog closes and drafts remain available for the same account. Explicit logout discards
workspace state after confirmation if there are unsaved changes. Resource-level project authorization is implemented;
see [the authorization concept](docs/AUTHORIZATION.md) for rules and implementation details. Members and owners have
full access to project contents; document references must stay within one project. Print jobs store their own project ID.

## Invited beta

Run the backend with `SPRING_PROFILES_ACTIVE=prod` and set
`PEEL_AUTH_ALLOWLIST_FILE` to the file path as seen by the backend. For example, keep
`/etc/peel/auth/allowed-emails.txt` on the VPS and mount its directory read-only
into the container at `/run/peel-auth`. Then use
`PEEL_AUTH_ALLOWLIST_FILE=/run/peel-auth/allowed-emails.txt`. Mount the directory
so an editor’s atomic replacement of the file is visible inside the container.
The backend needs read access and the VPS operator needs write access.

Use one email address per line in UTF-8. Blank lines and lines starting with
`#` are ignored. SSH into the VPS, edit a copy, and rename it over the original
so requests always read a complete file:

```sh
cd /etc/peel/auth
sudo cp -p allowed-emails.txt allowed-emails.txt.new
sudoedit allowed-emails.txt.new
sudo mv allowed-emails.txt.new allowed-emails.txt
```

Changes take effect without a restart. Removing an address blocks outstanding links and signs that account
out on its next API request. An empty file denies all sign-ins. A missing,
unreadable or invalid list blocks sign-in and session access. The production
profile checks the file at startup and requires an HTTPS origin and secure cookies.

Set `SPRING_MAIL_HOST`, `SPRING_MAIL_USERNAME`, and `SPRING_MAIL_PASSWORD`
for the SMTP relay. Production defaults to port 587 with required STARTTLS.
Login links and project mail share an hourly budget of 100 send attempts.
Set `PEEL_AUTH_MAIL_REQUESTS_PER_HOUR` to suit the relay and beta size.
Set `PEEL_TRUSTED_PROXY_REGEX` to a regular expression matching only Caddy's
private connection IP (for example, `^172[.]30[.]0[.]2$` with a fixed Compose
address). Keep the backend port private. The default trusts no forwarded client
IPs, so behind an unconfigured proxy the per-IP login limit is shared.
`GET /health/ready` reports whether the backend can reach MongoDB through a
status code. Use the full login and print workflow to check SMTP and storage.

Build against the published Peel Core with
`./gradlew bootJar -PpeelVersion=<core-version> -PreleaseVersion=<app-version>`.
Add `-PpeelRepositoryUrl=<maven-repository>` if the artifact is hosted outside
Maven Central. Local development can keep using the adjacent checkout by omitting
`peelVersion`. The build records both versions on print job execution.

To verify the versioned build locally, run
`./gradlew publishPeelPublicationToStagingRepository -PreleaseVersion=0.1.0-beta.1`
in Peel Core. Then build Peel Web with `-PpeelVersion=0.1.0-beta.1` and
`-PpeelRepositoryUrl=file:///.../peel/build/maven-repository/`. Use an immutable
artifact from the release Maven repository in CI.

To release Core to GitHub Packages, push a version tag in the Core repository.
The publish workflow runs the Core tests and publishes a Maven package with that
version. Peel Web Actions is pinned to `de.flo-gehring:peel:0.1.0-beta` via
`PEEL_CORE_VERSION=0.1.0-beta` and
`PEEL_CORE_REPOSITORY_URL=https://maven.pkg.github.com/flo-gehring/peel`.
Peel Web CI tries its repository `GITHUB_TOKEN` with `packages: read`.
If GitHub denies cross-repository access, set `PEEL_CORE_READ_USERNAME` to an
account with read access to Peel Core and set the Actions secret
`PEEL_CORE_READ_TOKEN` to that account's classic token with `read:packages`.
Pull requests from forks receive no repository secrets, so the token fallback
is only available to trusted workflow runs. To resolve the package locally,
set the Gradle properties `peelCoreUsername` and `peelCorePassword` through
environment variables prefixed with `ORG_GRADLE_PROJECT_`.

The production backend records its expected MongoDB schema revision in the
`schema_revision` collection and checks it at startup. Roll back only to an app
release that supports the stored revision. Back up the database and object store
before a schema change.

## Script, analysis and rendering limits

Script runs and scripts used by document previews and print jobs run in a separate
JVM. The server terminates the worker if it exceeds `PEEL_EXECUTION_TIMEOUT`
(default `5s`, including JVM startup and trace serialization). The limit applies
per script execution. The evaluator has 80% of that time (4 seconds by default).
A timeout produces `script_timeout`. Exceeding the evaluator's other limits, the
128 MiB worker heap or the serialized trace limit returns `script_resource_limit`.
Both errors fail the current print job when they occur during a batch.

| Environment variable | Default |
| --- | ---: |
| `PEEL_EXECUTION_MAX_STEPS` | 100,000 |
| `PEEL_EXECUTION_MAX_DEPTH` | 64 |
| `PEEL_EXECUTION_MAX_TRACE_UNITS` | 500,000 |
| `PEEL_EXECUTION_MAX_VALUE_SIZE` | 65,536 |

Trace units count recorded work and value snapshots. Value size measures text
characters, decimal digits or collection entries.

`PEEL_EXECUTION_MAX_WORKERS` defaults to two shared slots per backend. When both
are occupied, interactive runs and previews receive HTTP 503. A print job that
cannot acquire a slot fails. Each run starts a fresh JVM, so large print batches
take longer. The worker runs under the backend's OS identity.

Editor analysis and document rendering have separate pools of two slots and
return HTTP 503 when busy. Analysis limits source and expected-binding metadata.
Rendering limits each document to eight linked scripts, 20,000 render operations
and 1,048,576 characters of generated body HTML, plus 100 PDF pages and 10 MiB of
PDF output. Expression templates have their own fragment output and recursion
limits. References to external Pebble templates are disabled. The PDF renderer
loads its bundled stylesheet and skips image resources. Analysis and PDF layout
still run in the server JVM and have no process deadline.

## Project files

Scripts, render configurations and document templates share one typed file model,
MongoDB collection and CRUD API under `/api/projects/{projectId}/files`. Creation
uses a client-generated UUID and starts at version 0; it never overwrites an
existing file. Updates and deletion require the last acknowledged version.
An identical update succeeds without writing even when its expected version is
stale. Otherwise concurrent changes return `409` and drafts remain available.

The explorer supports common name/description editing, creation retries, protected
deletion and explicit reloading of the server version. Run, preview and print
operations resolve references from the same collection. Old CRUD endpoints and
collections are no longer used. There is no migration: recreate development
contents as needed; this change does not automatically delete old data.
See [FILE_MANAGEMENT.md](docs/FILE_MANAGEMENT.md) for request semantics, concurrency
scope, indexes and verification, and [DATA_MODEL.md](docs/DATA_MODEL.md).

## Project management

Print jobs now live inside their project under `/projects/{projectId}/print-jobs`,
grouped by document template. Jobs for deleted templates remain separately available.
Create a job from a document's Printer button or its group in the project overview;
unsaved document changes must be saved successfully first. Upload CSV and start the
job on its detail page. See [PRINT_JOBS.md](docs/PRINT_JOBS.md) for routes, grouping,
historical names, missing metadata in older jobs, and execution semantics.

Open the **Settings** gear on a project in the overview or beside its name in the workspace header. Owners and members can edit
the name and description of active projects. Owners can grant/revoke admin rights, remove members, archive/restore,
and permanently delete archived projects. Admins can invite members, resend invitations, and revoke invitations;
they cannot appoint admins. Invitations work for registered and unregistered email addresses and require explicit
acceptance after email sign-in. The inviter receives no registration-status information.
Pending invitations also appear on the start page with the project name, inviter and expiry date. Recipients can
explicitly accept or decline there; accepting refreshes their project list. Email invitation links remain available.

Ownership transfers require typing the recipient's email and `TRANSFER <project name>`. The recipient must already
be a member and must explicitly accept; they may decline, and the owner may withdraw. On acceptance the former owner
becomes an admin. Invitations and transfers expire after seven days by default.

Archived projects remain readable and downloadable; edits, uploads, previews and new runs are blocked. Use the
overview filter to find archived projects. Deletion requires typing the project name and waits for running jobs.
Failed object cleanup can be retried from settings. See [PROJECT_MANAGEMENT.md](docs/PROJECT_MANAGEMENT.md) for the
single-backend-instance concurrency scope, persistent cleanup state, API routes and test coverage.

### Authentication checks and generated types

```powershell
# From peelserver; authentication tests without Docker
./gradlew.bat test --tests "*AuthFlowTest" --tests "*AuthContractTest" --tests "*AuthUserRegistrationTest"
# SMTP integration test against the Mailpit image (requires Docker)
./gradlew.bat test --tests "*AuthSmtpTest"
# Account persistence and unique email index (requires Docker)
./gradlew.bat test --tests "*AuthPersistenceTest"
# From frontend, after the backend contract test has run
npm run gen:auth-api
npm run test -- src/stores/auth.spec.ts src/components/SignInForm.spec.ts src/views/ConfirmLoginView.spec.ts src/adapter/authRedirect.spec.ts src/adapter/client.spec.ts
```

`AuthOpenApiConfiguration` documents the controller and Spring Security filter endpoints. Its contract test exports
`build/auth-openapi.json`; `gen:auth-api` generates `frontend/src/types/auth-api.ts`, consumed alongside the main API
types by the existing client. The full running-server OpenAPI endpoint also includes these authentication endpoints.

