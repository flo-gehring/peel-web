# PEEL Theia App

This folder contains the Theia-based IDE application and custom Theia extension for PEEL.

## Structure

- `browser-app/`: Theia browser application composition.
- `extensions/peel-ide/`: custom PEEL extension with commands, tree widget, and frontend/backend contributions.

## Quick start

1. Install dependencies:

   ```bash
   npm install
   ```

2. Build and start browser app:

   ```bash
   npm run build:browser
   npm run start:browser
   ```

3. Open `http://localhost:3000`.

By default, the extension calls the Spring backend at `http://localhost:8080`.

## Devcontainer (Phase 1)

The recommended setup for IDE development is the repository devcontainer.

- The Theia app runs inside Linux (no Windows native build toolchain issues).
- The Spring backend stays on your host machine in phase 1.
- The container uses `PEEL_API_BASE_URL=http://host.docker.internal:8080`.

### Start

1. Open the repository in VS Code.
2. Run **Dev Containers: Reopen in Container**.
3. Wait for the post-create install to finish.
4. In the container terminal:

   ```bash
   cd ide
   npm run build:browser
   npm run start:browser
   ```

5. Open `http://localhost:3000`.

If backend requests fail, verify the backend is running on your host at `http://localhost:8080`.
