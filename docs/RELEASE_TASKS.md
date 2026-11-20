# Peel Web private beta release tasks

Start with invite-only admission, then bound script and document execution. These are the two most important app changes before putting the beta on the internet. This checklist assumes one backend on an EU VPS, with local MongoDB and private object storage, plus an external SMTP relay. See the [deployment plan](DEPLOYMENT_PLAN.md) for infrastructure details.

## App changes in implementation order

- [x] **Enforce invite-only access on the server.** The production profile requires a file with one email per line. Edits apply without restart. Removing an address blocks outstanding links and invalidates its session on the next API request. See the [operator instructions](../peelserver/README.md#invited-beta).
- [ ] **Bound PEEL analysis, execution and rendering.** Choose initial caps using representative scripts, document previews and CSV batches. Limit request sizes, interpreter steps and recursion, trace and PDF size, and CSV rows and columns. Stop work when a budget is exhausted and surface a clear error. Verify that an over-limit run stops consuming resources and that a normal request still works afterward. Review template permissions and disable PDF resource loading from local files or unapproved URLs.
- [ ] **Complete print and storage verification.** The single background worker, busy response, startup recovery and upload byte lengths are implemented. Run the Docker-backed recovery and storage tests. Rehearse stopping the backend during a job and verify that it becomes failed, partial PDFs remain unavailable, and a new job with the same CSV can complete. Verify the chosen S3 server with Peel’s actual SDK. Stream CSV and PDF downloads and confirm authorization and large-file behavior.
- [ ] **Harden production login and configuration.** Require an HTTPS public origin, secure cookies and production credentials. Trust forwarded client IPs only through Caddy and test that a client cannot spoof them. Bound login and invitation email sending. Verify CSRF, token expiry and single use, and that credentials and document contents stay out of logs. Keep detailed health and API documentation private or disabled in production.
- [ ] **Make builds reproducible and verify the API contract.** Publish and pin a Peel Core artifact so a clean build needs no sibling checkout. Check the Spring Boot and springdoc pairing, regenerate and synchronize the API types if the contract changes, and get the backend and frontend checks passing in CI, including Vue type-checking.
- [ ] **Record release and database versions.** Establish a baseline schema revision, record the app and Peel Core versions used for completed print jobs, and document when an app rollback is compatible with the stored data. Decide whether historical reruns must reproduce the original output before adding job input snapshots.
- [ ] **Add basic health and error reporting.** Provide a minimal readiness check and logs with request or job IDs. Include useful diagnostics for storage, SMTP and job failures without logging sensitive input. Verify that the operator can distinguish a full worker, a dependency outage and a failed render.

## Implementation progress

- Script source length, multipart uploads, CSV rows and columns, and generated PDF pages and bytes have initial limits. The script worker enforces Core steps, depth, value size and trace limits inside a configurable five-second process deadline. In-process analysis and rendering have separate concurrency and input or expansion limits. Analysis and PDF layout do not have hard deadlines. Recheck limits with representative data on the VPS.
- CSV and PDF download endpoints now stream from the object store. The API types describe both downloads as binary. The Docker-backed suite exercises the existing Java S3 client against SeaweedFS 4.53 and includes MongoDB recovery and project authorization. The VPS restore rehearsal still needs to cover both stores.
- The `prod` profile requires the allowlist, TLS mail settings and secure session configuration, and disables Swagger. Login and project mail share a configurable hourly budget. Check trusted-proxy behavior on the Compose deployment.
- The Core publishing workflow released `0.1.0-beta` to GitHub Packages. Peel Web Actions is pinned to that version through `PEEL_CORE_VERSION` and `PEEL_CORE_REPOSITORY_URL`. Verify the backend job from a clean checkout once the release workflow is in the default branch. See the [GitHub Packages setup](../peelserver/README.md#invited-beta) for cross-repository credentials.
- Production checks MongoDB schema revision 1 at startup. New print executions capture the app and Core build versions. Verify the revision guard and document the rollback procedure during the restore rehearsal.
- Production readiness reports MongoDB reachability. Unexpected request failures have an incident ID and print-job errors log their job ID. The deployed smoke test still needs to check SMTP and storage.

## Checks before sending invitations

- [ ] From a fresh browser on the deployed HTTPS origin, request a real email link, sign in, create a project, save a script and document, preview a PDF, print a CSV batch, and download its outputs.
- [ ] With two accounts, verify project isolation, membership removal, archived project restrictions, and stale-save conflicts.
- [ ] Rehearse one backend restart, one release rollback, and one restore of MongoDB and CSV/PDF data from an off-host backup. Verify the restored files through Peel.

Keep shared sessions, automatic job resumption, and full revision history outside this checklist unless they become beta requirements.
