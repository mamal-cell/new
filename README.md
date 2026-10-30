# Electruck

Electruck is a professional automotive electrical-diagram workspace. The repository currently contains two independent prototypes: a native Android starter and a Windows/macOS/Linux desktop shell built with Electron.

## Desktop prototype

The desktop implementation is in `desktop/` and includes a Persian RTL workbench, project creation/search stored locally in the browser profile, local image/PDF selection, schematic demo, dark mode, browser speech output, and a scripted (not AI-backed) demo chat.

### Run on a development computer

1. Install Node.js 20 or newer.
2. Open a terminal in `desktop/` and run `npm install`.
3. Run `npm start` to launch the desktop app.

### Build the Windows installer

On Windows, from `desktop/`, run `npm install` and then `npm run dist:win`. The installer is written to `desktop/release/Electruck-Setup-0.1.0.exe`. Building a Windows installer on Linux may require Wine and Windows signing is not configured. This repository does not currently contain a built installer.

## Android prototype

Open the repository root in Android Studio, install JDK 17+ and Android SDK 35, sync Gradle, and run the `app` configuration. Android minimum version is API 26; application ID is `com.madarban.app`.

## Current scope and limitations

These are prototypes, not a production-ready diagnostics product. Data in starter screens and the circuit illustration are examples. Local project entries are stored on the current computer; use the JSON export for a manual backup. Image files can be opened locally, but OCR, symbol recognition, real wiring simulation, vehicle-specific technical data, Google sign-in, cloud sync, user invitations, connected AI, and production speech workflows have not been implemented. The chat responses are scripted examples and must not be used as repair instructions. Never infer a fuse rating or wiring procedure from the illustrative diagram.

## Next engineering steps

1. Define and validate the vehicle, diagram, circuit, annotation, and permission data model.
2. Implement file import, OCR, and symbol recognition with expert review/correction.
3. Add secure authentication and owner-controlled access through a backend.
4. Add source-attributed technical references and an AI service behind a protected API.
5. Add automated tests, signed release packaging, crash reporting, and update delivery.

Owner approval and project privacy should be enforced server-side before inviting ordinary users. Vehicle electrical work can cause injury or damage; product guidance must clearly state uncertainty and safety precautions.
