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

API authentication uses a server session. Fetch `/api/auth/csrf` and send its token in the returned header on all
mutating requests, including multipart uploads. Fetch a fresh token after login/logout. API failures use JSON
with `error.code` and `error.message`; unauthenticated requests return 401 instead of redirects.
An expired session leaves drafts in memory while the user signs in in another tab. Explicit logout discards
workspace state after confirmation if there are unsaved changes. Resource-level project authorization is implemented;
see [the authorization concept](docs/AUTHORIZATION.md) for rules and implementation details. Members and owners have
full access to project contents; document references must stay within one project. Print jobs store their own project ID.

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

