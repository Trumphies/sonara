# SONARA

SONARA is a native Android music player for YouTube Music. It needs no
account sign-in for playback, and pairs an editorial, theme-driven
interface with shared listening, synced lyrics, downloads, podcasts,
and local files.

## Features

- **Playback** — streaming with background playback, lock-screen controls,
  queue management, a sleep timer, playback speed, and SponsorBlock
  segment skipping.
- **Seven structural themes** — ARUS, Senja Gelap, Blok, GETAR, RASI,
  PASAR, and SENYAP, each with light and dark variants. Themes change
  layout, typography, density, and motion, not just color.
- **Jamming Together** — listen in sync with friends over Firebase
  Realtime Database: a shared queue, votes, chat, reactions, and host
  controls.
- **Lyrics** — synced line-by-line lyrics with word-level highlighting,
  in-lyrics search, local editing, and per-song timing offsets.
- **Discovery** — search with suggestions and sorting, radio stations,
  genre pages, charts, artist profiles, and podcast show pages with
  chapters.
- **Library** — playlists with folders, tags, and pins; listening
  statistics with a shareable yearly recap; download management with
  pause and resume; local audio files.
- **Comments** — read-only comments from YouTube Music, including replies.

## Building

SONARA is built with a manual script — no Gradle wrapper is included in
this repository:

```sh
bash build-manual/build.sh
```

See the header of `build-manual/build.sh` for the exact Android SDK and
dependency-cache requirements. The script produces a debug APK signed
with a debug keystore; never commit a keystore to this repository.

A fresh clone does not compile out of the box: the build requires a
prebuilt native engine artifact that is not included here (see the
script header for details). Until that artifact is published or the
engine is made optional, treat this repository as source for review
rather than a one-command build.

## Configuration

Firebase is used for Google sign-in, cloud sync, and Jamming Together.
The client configuration in `google-services.json` is already exposed in
every shared APK and is not a server secret. Database security rules are
managed in the Firebase console and are not part of this repository —
verify them there before deploying or publishing a build.

## Project structure

- `app/src/main/java/com/sonara/app/` — Kotlin sources (`data`,
  `player`, `service`, `ui`, `widget`)
- `app/src/main/res/` — resources, including 18 OFL fonts
- `build-manual/build.sh` — the build script
- `BUILD-v1.0.md` … `BUILD-v1.0.8.md` — per-release build notes
- `DESIGN-SPEC.md` — design specification
- `docs/` — UI analysis notes

## Third-party notices

See `THIRD-PARTY-NOTICES.md`.

## License

MIT — see `LICENSE`.
