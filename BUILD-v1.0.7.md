# BUILD v1.0.7 — SONARA 1.0.7 (versionCode 8)

Batch 5 (TERAKHIR) celah MixMusic: LOCAL FILES EXTRAS +
CAST/REMOTE/OUTPUT + THEMES/CUSTOMIZATION. Dibangun hanya dari
data lokal / API framework yang terbukti ada. Semua string UI
Bahasa Inggris. Tidak ada izin baru. Tidak ada perubahan: tabel
token tema 7 tema (byte-identik), Jam sync, lirik (v1.0.4 +
rumus pemusatan + LyricsVersionPolicy), komentar, feed, mesin
Go, sinkron, notifikasi/widget, kode background-playback v1.0.2,
fitur v1.0.3 + v1.0.5 + v1.0.6.

## Inventaris sebelum membangun (jujur)

- File lokal SUDAH ada: `data/LocalMusic.kt` — scan MediaStore
  saja. Yang belum ada: folder pilihan user, edit tag, gabung
  duplikat, penandaan berkas hilang.
- `data/SafFolderScanner.kt` SUDAH ada (scan tree SAF + cek
  keberadaan) tetapi TIDAK PERNAH terkompilasi di build penuh:
  `buildChildDocumentsUriUsingTree(parentUri)` — arity salah.
  Diperbaiki di build ini (dua argumen: treeUri + document id).
- Tiga API yang direncanakan sesi sebelumnya ternyata BELUM
  PERNAH ditulis: `LocalFilesPolicy.scanAll`,
  `LocalFolderEntry`, dan pemetaan framework→model output audio
  (`sinkDevices`/`matchingRoute`/`deviceDetail` tidak ada —
  API nyatanya `listDevices`/`matchRoute`/`selectedRoute` +
  `deviceLabel`). Semuanya ditulis/diselaraskan di build ini;
  tidak ada yang diasumsikan dari ringkasan.
- Chromecast: TIDAK ada Cast SDK / MediaRouter di cache Gradle;
  Maven Central terjangkau via proxy tetapi
  `play-services-cast-framework/21.5.0` mengembalikan 404.
  Menambah SDK berarti rantai transitif besar ke build manual
  tanpa Gradle + jalur remote-playback paralel yang bertabrakan
  dengan arsitektur ExoPlayer/Jam. Keputusan builder: TIDAK
  dibangun; dicatat jujur, tanpa tombol Cast palsu.
- LAN remote control (pairing 6 digit): butuh protokol
  pairing/trust + UI controller yang tidak bisa dibuktikan aman
  dari VM; soket kontrol-playback tanpa autentikasi di LAN
  adalah lubang keamanan. Keputusan builder: TIDAK dibangun;
  dicatat jujur sebagai tidak tersedia + langkah lanjut.
- Output picker TERBUKTI bisa dibangun: `AudioManager
  .getDevices(GET_DEVICES_OUTPUTS)` + `AudioDeviceInfo` +
  framework `MediaRouter` (javap atas android.jar API 35).
  `Settings.Panel` API 35 TIDAK punya aksi media-output — dialog
  harus menampilkan perangkat/rute nyata sendiri.
- Theme/plugin marketplace: TIDAK ada backend — tidak dibangun
  (toko palsu dilarang).
- Integrasi (Last.fm, Discord, MCP): probe saja — semuanya
  butuh akun/kunci yang belum ada; tidak ada alur sign-in
  yang dibangun (lihat "Yang TIDAK dibangun").

## Yang dibangun

1. File lokal (`data/LocalFilesPolicy.kt` murni +
   `data/LocalFilesScan.kt` + `data/SafFolderScanner.kt`):
   - Folder SAF pilihan user (pemilih dokumen sistem,
     izin baca persistable, maks 8 folder, tersimpan lokal di
     `local_folders_v1` sebagai daftar URI; nama chip
     diturunkan dari URI tree).
   - Scan gabungan MediaStore + folder SAF (`LocalFilesScan
     .scanAll`, wajib thread IO): override tag diterapkan
     saat scan, jadi tampilan detail ikut berubah.
   - Tag editing: judul/artis/album per file, disimpan lokal
     (`local_tag_overrides_v1`); kosong = kembali ke asli;
     file TIDAK PERNAH ditulis. Dialog "Clear saved tags".
   - Duplikat: judul+artis+durasi ternormalisasi (case/spasi
     tidak sensitif) digabung; chip "N copies" membuka
     pemilih salinan; pilihan tersimpan
     (`local_preferred_sources_v1`, kunci = kunci grup);
     salinan pilihan yang hilang digantikan salinan yang ada.
   - Berkas hilang: cek keberadaan per salinan (MediaStore
     query / SAF document query; gagal cek = tidak diketahui,
     BUKAN hilang). Grup hilang bila semua salinannya hilang:
     baris meredup (alpha 0.45) + label + dialog penjelasan
     (tombol "Hide missing files"); toggle Hide missing +
     hitungan jujur di header.
   - Watch-for-changes = scan ulang saat Library dibuka +
     tombol Rescan; TIDAK ada background watcher/service.
2. Output audio (Settings → Playback → "Audio output"):
   dialog perangkat nyata dari AudioManager (sink saja,
   dedupe) + rute framework MediaRouter (best-effort).
   Label/dedupe/pencocokan di `AudioOutputPolicy` (murni +
   fixture): Bluetooth klasik selalu "Bluetooth" (nama produk
   sering alamat); pencocokan rute HANYA dari nama produk
   (label generik tidak dipakai mencocokkan). Tap perangkat
   dengan rute cocok → `selectRoute`; tanpa rute → toast
   jujur + tombol "Bluetooth settings". Catatan Cast di
   dialog (tidak dibangun — lihat atas).
3. Layout (Settings → Appearance → "Layout", tersimpan
   `home_layout`): EDITORIAL (rel seperti semula), COMPACT
   (rak jadi daftar rapat), GRID (ubin 2 kolom). Dipakai
   HomePosterRail/HomeWideRail + seksi "Your playlists"
   (baris rapat / ubin cover + pin). Baris lagu, MixCard,
   Keep listening, dan feed TIDAK berubah bentuk.
4. Tema kustom (Settings → Appearance → "Custom accent",
   tersimpan `custom_theme_v1`, melapis DI ATAS palet dasar
   — `data/AppTheme.kt` tidak disentuh): 8 swatch aksen +
   radius SHARP/BALANCED/ROUND. `CustomThemePolicy.resolve`
   menegakkan WCAG 4.5:1 (aksen vs background; onAccent vs
   accentFill, diturunkan/dinaikkan bertahap, fallback ke
   pasangan dasar bila gagal); pratinjau menampilkan warna
   HASIL resolve. ROUND memaksa pill = 999 dan tidak pernah
   mengecilkan sudut dasar; SHARP memendekkan sudut (pill
   999 → 8). Struktur tema (font/border/treatment) tetap.
5. Command palette (layar Search): 12 perintah
   (`data/CommandPalette.kt`, registry tetap): Play/Pause,
   Next, Previous, Playback speed, Sleep timer, Audio
   output, Go to Home/Library, Jam Together, Go to
   Settings, Your stats, Switch light/dark. Query kosong =
   daftar lengkap (di antara Recent searches dan Explore,
   aturan sembunyi-Browse v1.10.38 dipertahankan); query
   terisi = chip perintah yang cocok di atas hasil lagu
   (query tidak diubah). Aksi playback lewat gerbang Jam
   yang sama dengan layar detail (tamu terkunci → toast
   host-only; kontrol bersama nyala → request ke host);
   speed/sleep/output membuka Settings halaman Playback
   (parameter `startPage`); navigasi via `Screen`; flip mode
   via callback tema.

## Yang TIDAK dibangun (jujur)

- Chromecast / tombol Cast (alasan di atas; SDK 404 + jalur
  remote-playback paralel tidak dibangun).
- LAN remote control / pairing 6 digit (butuh protokol trust
  + UI controller; soket tanpa autentikasi = lubang).
- Theme/plugin marketplace (tidak ada backend).
- Sign-in Last.fm / Discord presence / kontrol MCP AI
  (butuh akun/kunci user — menunggu keputusannya; langkah
  yang ia perlu ambil: buat akun + berikan kunci/token
  resmi, lalu integrasi dibangun di atas API resminya).
- Background watcher file lokal (scan ulang manual + saat
  buka Library saja; murah dan jujur).
- Folder bertingkat untuk playlist, tag/folder tersinkron,
  statistik dari akun (tetap seperti v1.0.6 — lokal saja).
- Video mode (dihapus di 10.18 — tidak dibangkitkan),
  Spotify, sign-in akun YTM, data karangan apa pun.

## Verifikasi

- harness152 (reproduce-first): pada tree v1.0.6 yang belum
  disentuh = 19 FAIL + compile fail (reproduksi
  terkonfirmasi lewat source checks). Catatan jujur: compile
  fail tree bersih sebagian disebabkan harness sendiri —
  `SubtitleArtist.kt` (tempat `displayArtist`) tidak masuk
  daftar kompilasi fixture; diperbaiki dengan menambahkannya
  ke `harness152/run.sh` (harness, bukan kode app). Setelah
  build: 24/24 source checks PASS (R1–R21 + guard G1–G3) +
  SEMUA fixture Kotlin PASS (F1 override, F2 JSON, F3 grup
  duplikat, F4 hilang, F5 klasifikasi SAF, F6 folder, F7
  label perangkat, F8 sink/dedupe, F9 pencocokan rute, F10
  layout, F11 rail mode, F12 kontras aksen, F13 onAccent,
  F14 radius, F15 JSON tema, F16 registry, F17 filter).
  Tiga koreksi IMPLEMENTASI ke kontrak fixture (bukan
  sebaliknya): `groupDuplicates` menerima kunci preferensi
  bentuk kunci-grup ATAU id salinan-pertama; `markMissing`
  menjadi per-track (satu entri per track masukan);
  `deviceLabel` Bluetooth klasik selalu generik +
  `matchRoute` hanya dari nama produk; `applyRadius` ROUND
  memaksa pill 999 + tidak mengecilkan sudut dasar.
- Guard md5: AppTheme.kt `2480bca8703269751d32bfa3688cc488`
  (byte-identik), LyricsFollow.kt
  `cccee070d2178bde1b69c09e61f63607`, LyricsApi.kt
  `3f82d64bdb38ea09d483a76a406747d9` — ketiganya tidak
  berubah.
- Clean rebuild: 0 error, 0 peringatan BARU — set peringatan
  sama persis dengan v1.0.6 (dibandingkan per-pesan).
  Perjalanan build: putaran 1 gagal (8 error: arity SAF,
  `SegmentBg`/`OnAccent` belum diimpor, urutan deklarasi
  state lokal di QueueLibrary, API `RailMode` bersarang,
  nama aksi `CommandPalette`, API `AudioOutputPolicy`,
  `scanAll`/`LocalFolderEntry` belum ditulis); putaran 2
  gagal (1 error: param String SAF + 1 instans peringatan
  deprecated dari ikon yang kupakai — diganti ke
  AutoMirrored); putaran 3 sukses.
- aapt2: package `com.sonara.app`, versionCode 8,
  versionName 1.0.7.
- apksigner: v2+v3 true, v1/v3.1 false, 1 signer,
  sertifikat SHA-256
  `e83b1d93926c1ebbfebd8d0f95391ff5b4fc83b978ebbef1fd88f451c2c5da06`
  — SAMA dengan backup v1.0.6 / registrasi Firebase.
- Backup v1.0.6 terverifikasi hash (MD5
  `2dd00c6ed2b569209da992c8ae62c82b`) di
  `backups/sonara-debug-v1.0.6-vc7.apk` SEBELUM overwrite.
  Salinan v1.0.7 juga disimpan hash-terverifikasi di
  `backups/sonara-debug-v1.0.7-vc8.apk`.

## Regresi (spot-check)

- h151: KOTLIN ALL PASS. h150: ALL PASS. h149: ALL PASS
  (termasuk guard rumus pemusatan lirik). h130: 79 checks,
  0 failures + GUARDS OK.
- h118: SATU putaran pada tree v1.0.7 — mode dormant 5 FAIL
  yang diharapkan (perilaku pra-perbaikan); mode normal
  ALL PASS (56 PASS, F1–F6 lolos termasuk F2). F2 TIDAK
  dinyatakan fixed — suite tidak stabil antar putaran dan
  investigasi watchdog khusus masih terbuka; tidak ada jalur
  watchdog yang disentuh build ini.
- h118b/h139 fixture-blocked + h127/h128 kegagalan lama:
  tidak dijalankan ulang (blok env yang sudah ada
  sebelumnya, bukan regresi build ini).

## Artefak

- APK: `sonara-debug-v1.0.7.apk` — ukuran 65.568.733 byte,
  MD5 `2af61a8960764168a3808b0b13afed5a`,
  SHA-256 `0386e28a582ebd8c50a33b0755a7d08a3621d5679f64abb7e8c58ac77027cd00`.
  Link: https://muse.ai/files/1439248322600143/996698733497652/re8nru3hyze7zxu7d09qb4b5/sonara-debug-v1.0.7.apk
  (kedaluwarsa 27 Nov 2026 12:51 WIB / 05:51 UTC).
- File baru: `data/LocalFilesScan.kt`,
  `harness152/check_152.py`, `harness152/Harness152.kt`,
  `harness152/run.sh`. (File murni `LocalFilesPolicy.kt`,
  `AudioOutputPolicy.kt`, `LayoutPolicy.kt`,
  `CustomThemePolicy.kt`, `CommandPalette.kt`,
  `SafFolderScanner.kt` sudah ada dari sesi sebelumnya —
  diperbaiki/dilengkapi di build ini, lihat atas.)
- File diubah: `data/LocalStore.kt` (prefs file lokal +
  layout + tema kustom), `data/LocalFilesPolicy.kt`
  (LocalFolderEntry/LocalTrackGroup, fallback preferensi,
  markMissing per-track, copyLabel, folderDisplayName),
  `data/AudioOutputPolicy.kt` (label BT + matchRoute),
  `data/CustomThemePolicy.kt` (ROUND pill 999),
  `data/SafFolderScanner.kt` (arity API),
  `ui/theme/Theme.kt` (param `customOverride` — tabel token
  tidak berubah), `ui/SonaraApp.kt` (state tema kustom +
  wiring palette + `startPage`), `ui/screens/SettingsAbout.kt`
  (layout picker + editor tema kustom + dialog output),
  `ui/screens/HomeSearch.kt` (layout rail + command palette),
  `ui/screens/QueueLibrary.kt` (seksi lokal + layout
  playlist + 3 dialog), `harness152/run.sh` (tambah
  SubtitleArtist.kt), `build-manual/build.sh` (vc8/1.0.7).

## Langkah cek di HP

About: SONARA v1.0.7 (8). (1) Library → Local music: Add
folder → pilih folder berisi lagu → lagunya muncul; Rescan
memperbarui; hapus folder (X di chip) → lagunya hilang.
(2) Edit (ikon pensil di baris lokal) → ubah judul/artis →
tampil berubah, file asli tidak berubah; Clear saved tags →
kembali ke asli. (3) Lagu yang sama di 2 tempat → chip
"2 copies" → pilih salinan → yang dipilih yang diputar.
(4) Hapus/pindahkan satu file lokal di luar app → Rescan →
barisnya meredup + "File missing"; tap → dialog; "Hide
missing files" menyembunyikannya; matikan toggle-nya →
muncul lagi. (5) Settings → Appearance → Layout: Compact →
rak Beranda jadi daftar rapat; Grid → ubin 2 kolom;
koleksi "Your playlists" ikut berubah; kembali ke Editorial
→ seperti semula. (6) Custom accent: nyalakan → pilih
swatch → aksen berubah di seluruh app + pratinjau jujur;
radius Sharp/Round mengubah sudut; matikan → warna tema
asli kembali; semua kombinasi tetap terbaca. (7) Search →
"Commands" tampil saat query kosong; ketik "sleep" → chip
Sleep timer; jalankan Play/Pause, Next, Go to Library,
Switch light/dark — semuanya bekerja; selama Jam sebagai
tamu, Play/Pause/Next mengikuti aturan host (toast bila
terkunci). (8) Settings → Playback → Audio output: daftar
perangkat nyata (speaker/headphone/Bluetooth yang
terhubung); tap perangkat dengan rute → sistem berpindah;
tanpa Cast (jujur tidak ada). (9) Jam/lirik/podcast/stat/
unduhan/tema/sync/background tidak berubah.
