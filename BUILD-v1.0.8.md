# BUILD v1.0.8 — SONARA 1.0.8 (versionCode 9)

Penghapusan command palette atas permintaan user (25 Nov 13:06 WIB,
setelah melihat layar Commands di HP-nya: "hilangkan aja"). Command
palette adalah fitur v1.0.7; build ini menghapusnya TOTAL dari UI
dan kode, tanpa menyentuh fitur lain. Semua string UI tetap Bahasa
Inggris. Tidak ada izin baru.

## Yang dihapus

- `data/CommandPalette.kt` DIHAPUS (seluruh file): `CommandPalette`
  (registry 12 perintah), `PaletteCommand`, `CommandAction`,
  `filter()`. Tidak ada referensi lain ke file ini di luar
  `HomeSearch.kt` — diverifikasi dengan grep sebelum dihapus.
- `ui/screens/HomeSearch.kt`:
  - Composable `CommandsSection` (header "Commands" + deskripsi
    "Jump to a screen or control playback..." + 12 baris perintah)
    DIHAPUS seluruhnya.
  - Blok chip perintah saat mengetik (`CommandPalette.filter(query)`
    + LazyRow chip "Commands") DIHAPUS seluruhnya.
  - Blok `if (!searching) { item { CommandsSection(...) } }` DIHAPUS.
  - Tiga parameter `SearchScreen` khusus palette DIHAPUS:
    `onNavigate`, `onToggleThemeMode`, `onOpenPlaybackSettings`.
  - Helper `runCommand` + `guardedCommandPlayback` + state
    `commandGuestLocked` / `commandIsJamGuest` /
    `commandCurrentTrack` DIHAPUS.
  - Import yang menganggur DIHAPUS: `CommandPalette`,
    `CommandAction`, `JamControlAction`, `JamRole`, `Screen`,
    `Icons.AutoMirrored.Filled.KeyboardArrowRight`. (`JamControlAction`
    tetap dipakai di `SettingsAbout.kt`, `SonaraApp.kt`,
    `JamSessionManager.kt` — hanya import di HomeSearch yang mati.)
- `ui/SonaraApp.kt`: tiga argumen palette di call site `SearchScreen`
  DIHAPUS (`onNavigate`, `onToggleThemeMode`,
  `onOpenPlaybackSettings`). `navigateTo`, `applyThemeSelection`, dan
  `pendingSettingsPage` tetap dipakai di tempat lain — diverifikasi
  (19 / 3 / 4 kemunculan) — tidak ada yang menganggur.
- Versi: `build-manual/build.sh` `--version-code 8 --version-name
  1.0.7` → `--version-code 9 --version-name 1.0.8`. Label About
  membaca dari PackageManager (perbaikan v1.10.35), jadi ikut
  berubah tanpa edit string.

## Yang TIDAK berubah (perilaku Search setelah penghapusan)

- Query kosong → seksi Browse tampil persis seperti sebelum v1.0.7:
  Recent searches (bila ada) → ExploreTilesRow → header "Browse" +
  grid genre (v1.10.33) → daftar Browse. Aturan sembunyi-Browse
  v1.10.38 (`if (!searching)`) dipertahankan; tidak ada celah kosong
  peninggalan palette.
- Mengetik → hasil lagu + saran (v1.0.3), tanpa chip perintah.
- Playback speed / sleep timer / audio output tetap bisa dijangkau
  dari tempat aslinya: tombol "Speed:" + dialog sleep di layar
  detail, baris TrackSheet, dan Settings → Playback. Hanya daftar
  jalan pintas palette yang hilang.
- Utuh: file lokal (folder SAF, edit tag, duplikat, berkas hilang),
  layout Editorial/Compact/Grid, editor tema kustom, dialog audio
  output, tema/token (byte-identik), Jam, lirik + rumus pemusatan,
  komentar, feed, mesin Go, sinkron, notifikasi/widget,
  background-playback v1.0.2, fitur v1.0.3 + v1.0.5 + v1.0.6.

## Verifikasi

### Harness153 (reproduce-first, `harness153/`)

13 cek sumber; di tree v1.0.7 yang belum disentuh: 8 cek-R GAGAL
(reproduksi: file palette ada, token palette ada, CommandsSection
ada, string header ada, parameter palette ada, argumen call site
ada, helper dispatch ada, import mati ada) + 2 cek bertahan (R8/R9)
+ 3 guard LULUS. Setelah penghapusan: **13/13 LULUS**. Tidak ada
fixture Kotlin baru — ini penghapusan; kompilasi dibuktikan oleh
clean rebuild penuh. Guard byte-identik: AppTheme.kt
`2480bca8703269751d32bfa3688cc488`, LyricsFollow.kt
`cccee070d2178bde1b69c09e61f63607`, LyricsApi.kt
`3f82d64bdb38ea09d483a76a406747d9`.

### Bukti artefak

- `resources.arsc` APK: 0 kemunculan "Jump to a screen", 0
  "commandpalette" (case-insensitive), 0 "Commands".
- Grep `app/src/main/java`: 0 kemunculan `CommandPalette`,
  `CommandAction`, `PaletteCommand`, `CommandsSection`, `runCommand`,
  `onNavigate`, `onToggleThemeMode`, `onOpenPlaybackSettings`.
- Himpunan warning build byte-identik dengan v1.0.7 (65 baris,
  diff kosong) — 0 warning baru, 0 error. `BUILD OK`.

### Regresi

- h151: 21/21 cek sumber + fixture Kotlin ALL PASS; satu-satunya
  FAIL adalah R10 yang mengunci `build.sh` di vc7/1.0.6 — basi
  karena bump versi (bukan regresi).
- h150: 39/39 + fixture ALL PASS. h130: 79 cek / 0 gagal + guard OK.
- h152: cek sumber 21 lulus / 3 gagal — ketiganya adalah cek yang
  sengaja disupersede oleh penghapusan ini (R6 file palette ada,
  R17 Search punya seksi Commands, R21 build.sh vc8/1.0.7). Fase
  fixture Kotlin h152 TIDAK bisa jalan di tree ini: 5 cek F17a–F17e
  menguji `CommandPalette.filter` — API yang dihapus; kompilasi
  fixture gagal di referensi itu, sesuai harapan. 57 cek fixture
  lain (file lokal/output/layout/tema kustom) tidak berubah dan
  tercakup oleh cek sumber h152 yang lulus + clean rebuild. Harness
  h152 TIDAK dimodifikasi; kompilasi fixture dicoba lewat salinan
  `/tmp` run.sh tanpa baris CommandPalette.kt.
- h118 (satu loop, TIDAK ada sentuhan watchdog): fase dormant 5
  gagal sesuai desain; fase live F1–F7 SEMUA LULUS; satu-satunya
  kegagalan adalah assertion setup "guest follows host" di skenario
  pertama — flake setup yang sama seperti di v1.0.4. **F2 TIDAK
  dinyatakan fixed** — suite tidak stabil antar-run; investigasi
  watchdog khusus tetap terbuka.

### Build

- Backup v1.0.7 terverifikasi hash (MD5
  `2af61a8960764168a3808b0b13afed5a`) di
  `backups/sonara-debug-v1.0.7-vc8.apk` SEBELUM overwrite.
- Clean rebuild `BUILD_EXIT=0`. aapt2: `com.sonara.app` vc9 / 1.0.8.
  apksigner v2+v3, 1 signer, sertifikat sama (SHA-256
  `e83b1d93…c5da06`, SHA-1 `4942789c…02b0`).
- APK: 65.552.349 byte, MD5
  `d3ef6c2603b17c4e1c21cbbbd73372bc`, SHA-256
  `092b76a7cc897ecd4fb7d7a474046495bf3df270886462e60ba89fb1472d74b7`.

## Langkah cek HP (About: SONARA v1.0.8 (9))

1. Tab Search, query kosong: Browse tampil penuh (Recent searches,
   tile Explore, header Browse + grid genre) — TIDAK ada seksi
   Commands, tidak ada celah kosong.
2. Ketik query: hasil lagu + saran tampil, tanpa chip perintah.
3. Speed/sleep/output masih bisa dibuka dari layar detail +
   Settings → Playback.
4. Jam / lirik / podcast / statistik / unduhan / layout / tema
   kustom / file lokal / sinkron / background tidak berubah.
