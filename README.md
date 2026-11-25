# SONARA

Android music player — YouTube Music only. (DRAFT README, prepared 25 Nov 2026; not yet published.)

- **Package:** `com.sonara.app` (arm64-v8a only)
- **Version:** v1.0.8 (versionCode 9) is the latest build in this tree's history — see `BUILD-v1.0.8.md`. (History: v1.0–v1.0.8; the v1.0.7 command palette was removed again in v1.0.8.)
- **Identity:** ARUS theme (warm paper, Garis Arus), 7 structural themes × light/dark, Jamming Together (shared listening over Firebase Realtime Database), synced lyrics, real YTM comments (read-only).
- **Build:** manual, no Gradle — `bash build-manual/build.sh` (kotlinc + aapt2 + d8, as used in this workspace). The script expects the Android SDK and a Gradle dependency cache; see the script header.

## Before this repo can honestly be called "buildable from a fresh clone"

1. **Go engine is NOT yet optional.** The build script hard-fails if the local
   `mixmusic.aar` (Go/MixMusic engine, `libgojni.so`) is absent, and Kotlin
   sources reference its classes. The app has a Kotlin fallback path at
   runtime, but a fresh clone cannot compile without the `.aar` or a stub.
   Do not publish claiming a fresh clone builds until a Go-free build variant
   (stub or conditional compilation) is verified. See `PUBLIC-CHECKLIST.md`.
2. **Firebase rules must be checked in the console first.** Rules are not in
   this repo. Verify Firestore owner-only rules and harden Realtime Database
   `/jam/*` rules before going public; the client config in
   `google-services.json` is already exposed in every shared APK and is not
   a server secret.
3. **Signing:** never commit a keystore. The build script generates a fresh
   debug keystore if none exists — a clone's signature will differ from the
   author's, which also affects Google Sign-In / App Links registration.

## Attribution

Third-party notices: `THIRD-PARTY-NOTICES.md`. The Go engine remains in use
at runtime; its provenance is recorded there, not in the app's About screen.
