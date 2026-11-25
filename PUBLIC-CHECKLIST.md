# SONARA — pre-publication checklist (prepared 25 Nov 2026, NOT published; staging refreshed 25 Nov 2026 ~14:10 WIB to the v1.0.8 tree)

Staging copy at `~/workspace/sonara-public-staging/` (3.9 MB, 148 files, verified 25 Nov 2026).
Original project untouched at `~/workspace/mixtune-app/` (5.3 GB incl. backups/APKs).
Nothing has been pushed, no repo created, no release uploaded.

## Verified in this staging copy

- Size: 3.9 MB total (source `app/` 3.6 MB — 97 .kt + 18 .ttf + 16 .xml — + docs + build script + config).
- Zero `*.apk`, `*.keystore`, `*.aar`, `*.so`, `*.idsig`, `*.log` files.
- Text-file scan (`.kt/.xml/.md/.sh/.json`): no phone-like numbers, no private-key material. The one phone number found in the original tree (`BUILD-v1.10.7.md`) was excluded entirely — that file and all other MIXTUNE-era BUILD docs were left out; only `BUILD-v1.0.md` and `BUILD-v1.0.1.md` … `BUILD-v1.0.8.md` are included. `google-services.json` is byte-identical to the live tree's (client config only, already exposed in every shared APK — per the 25 Nov audit).
- No `CommandPalette` references anywhere (v1.0.8 removal confirmed). Remaining `mixtune` mentions in code are comments documenting the v1.0 rename + the legacy deep-link/bridge fallback only.
- Old MIXTUNE README was NOT copied (it described v1.10.21 / `com.mixtune.app` and would mislead). A fresh draft `README.md` was written for SONARA and updated to v1.0.8.
- `.gitignore` covers APKs, keystores, AARs, `.so` files, build outputs, backups and harness dirs.

## Still open before publishing (owner decisions / console checks)

1. **Go engine optional — NOT DONE.** `build-manual/build.sh` fails without the
   local `mixmusic.aar`, and Kotlin code references Go classes. Needs a
   verified Go-free build variant (stub/conditional compile) before claiming
   a fresh clone builds. This is builder work in the live tree, not a script tweak.
2. **Firebase rules — must be checked in the Firebase console** (rules live
   only there, not in any repo): confirm Firestore is owner-only, and harden
   RTDB `/jam/*` (reported `auth != null` — any signed-in Google user could
   read/write Jam nodes and burn the free Spark quota). Consider API-key
   restrictions and App Check before public.
3. **APK distribution:** latest SONARA APK is `sonara-debug-v1.0.8.apk`, 65,552,349 bytes, MD5 `d3ef6c2603b17c4e1c21cbbbd73372bc` (verified in the live tree; link expires 27 Nov 2026 13:19 WIB). Recommended: GitHub Release asset, not a repo file. Do not publish the APK as a release publicly until the user has phone-verified it.
4. **Repo name / account / licence:** not chosen. Licence file not included.
   Add one before publishing (MIT for the app code; note the Go engine's own
   MIT provenance in `THIRD-PARTY-NOTICES.md`).

## If the user says "gas"

1. Re-check Firebase rules in console (item 2) — blocker.
2. Decide on item 1: publish as source-only with the honest README caveat, or
   wait for the Go-free variant.
3. `git init` in the staging copy, commit, create the public repo, push.
   Add the latest APK (only after phone verification) as a Release asset.
