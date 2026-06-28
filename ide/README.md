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
