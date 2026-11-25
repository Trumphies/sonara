# Third-Party Notices — SONARA

This file lists the open-source components bundled in the SONARA app
(APK). It is a project record only; it is not shown in the app's UI.
The app's own code is original to this project. Network data comes
from YouTube Music (signed-out web API) and LRCLIB — those are
services, not bundled code, and are not listed here. Firebase is used
via REST only; no Firebase SDK is bundled.

## MixMusic — MIT License

- Component: the YouTube Music Go engine, bound via gomobile
  (`mixmusic.aar`: `libgojni.so` + the `mobileapi` binding), used for
  YouTube Music search and direct stream resolution. The Kotlin UI and
  the Kotlin fallback implementation are original to this project.
- Source: fzn0x/mixmusic — https://github.com/fzn0x/mixmusic
- License: MIT. The MIT permission notice:

  > MIT License
  >
  > Permission is hereby granted, free of charge, to any person
  > obtaining a copy of this software and associated documentation
  > files (the "Software"), to deal in the Software without
  > restriction, including without limitation the rights to use, copy,
  > modify, merge, publish, distribute, sublicense, and/or sell copies
  > of the Software, and to permit persons to whom the Software is
  > furnished to do so, subject to the following conditions:
  >
  > The above copyright notice and this permission notice shall be
  > included in all copies or substantial portions of the Software.
  >
  > THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
  > EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
  > MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
  > NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
  > BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
  > ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
  > CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  > SOFTWARE.

## Apache License 2.0 components

The following are bundled under the Apache License, Version 2.0
(full texts at https://www.apache.org/licenses/LICENSE-2.0):

- AndroidX libraries (appcompat, core, activity, fragment, lifecycle,
  browser, customview, drawerlayout, emoji2, exifinterface,
  vectordrawable, and related artifacts)
- Jetpack Compose UI, Foundation, Material 3, Material Icons
- ExoPlayer / Jetpack Media3 1.4.1 (playback)
- Google Play Services Auth 21.2.0 (Google sign-in)
- Google Tink + androidx.security:security-crypto (encrypted storage)
- Coil 3 (image loading) and OkHttp + Okio (networking)
- Kotlin standard library and kotlinx-coroutines
- Guava (listenable futures, via Play Services dependencies)

## SIL Open Font License 1.1 — bundled fonts

The following fonts are bundled under the SIL Open Font License,
Version 1.1 (full text at https://openfontlicense.org):

- Plus Jakarta Sans (Regular, Bold)
- Bricolage Grotesque (ExtraBold)
- Archivo (Bold)
- JetBrains Mono (Regular, Bold)
- Space Grotesk (Regular, Medium, Bold)
- Anton (Regular)
- Fraunces (Light, Black)
- Inter (Light, Regular, SemiBold, Bold)
- IBM Plex Mono (Regular, Bold)
