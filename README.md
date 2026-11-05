# Electruck

Electruck is a private, offline-first automotive wiring-diagram project archive. The Android starter now supports local vehicle projects, local image/PDF attachments, expert-entered codes and notes, and manual map verification. The separate desktop prototype is under `desktop/`.

## Android prototype

Open the repository root in Android Studio with JDK 17+ and Android SDK 35 installed, sync Gradle, and run the `app` configuration on a real Android phone or emulator. Minimum Android version is API 26. The app declares no internet permission and disables Android cloud backup; its project metadata is stored in private app preferences on-device. Imported source files remain in the Android document provider selected by the owner, with a persisted read grant. Uninstalling the app or deleting/moving the source file can make project attachments unavailable, so keep originals backed up separately.

### Implemented in this first offline step

- Persian RTL home, project archive, map workspace, and settings.
- Create/search/open/delete vehicle projects; persist metadata across app restarts.
- Attach a local image or PDF using Android's document picker.
- View images with pinch zoom; PDF is opened in an installed PDF reader.
- Manually enter/remove circuit codes and save notes.
- Explicit expert-controlled verification checkbox; newly attached maps start unverified.
- Light/dark preference saved on device.

### Not implemented yet

No OCR, automatic symbol/wire analysis, 3D simulation, verified diagram library, account/cloud sync, user registration, AI assistant, speech, or JSON export. The app does not ship vehicle-specific wiring information; only the vehicle/project facts entered by the owner are stored. The code and notes screen is a manual annotation starter, not a diagram editor overlay. No APK was built or runtime-tested here because Java and Android SDK are unavailable in this environment.

## Desktop prototype

The Electron implementation in `desktop/` provides a Persian RTL desktop shell and local project UI. On a development machine with Node.js 20+:

```sh
cd desktop
npm install
npm start
```

To make the Windows NSIS installer on a Windows computer, run `npm run dist:win`; output is under `desktop/release/`. The repository does not currently contain a built installer.

## Safety and next steps

This is a starter, not a production diagnostics product. Verify every imported diagram against its source, vehicle model, year, trim, and system. Never treat a displayed sample or manually entered value as validated by the app. The next Android steps should be robust diagram metadata and backup/export, in-app PDF preview, annotation overlays, tests, then owner-approved OCR suggestions with review. Any future sharing/AI service must remain opt-in and preserve owner-controlled access.
