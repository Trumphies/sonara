# BUILD v1.0 — Rename total MIXTUNE → SONARA + reset versi

Tanggal: 25 November 2026. Di atas tree v1.10.44 (vc55). Perintah
user (24 Nov 2026 24:17 WIB, via redirect): "coba ganti identitas
app gua jadi SONARA" — lalu scope diubah dari rebrand display-only
menjadi TOTAL rename + reset versi. Brief awal (display-only)
DIGANTI SELURUHNYA oleh redirect; yang berlaku adalah redirect
ini. Semua string tetap bahasa Inggris. Tidak ada permission,
dependency, backend, atau billing baru.

CATATAN NAMA DOKUMEN: file BUILD-v1.0.md SUDAH ADA sebelumnya
(dokumen build v1.0 asli, 21 Nov 2026). File lama TIDAK ditimpa:
dipindah ke BUILD-v1.0-legacy.md di tempat yang sama, utuh.
Dokumen ini adalah catatan build v1.0 (SONARA) yang baru.

## 1. Yang diubah

### 1.1 Identitas baru

- `applicationId`/package: `com.mixtune.app` → **`com.sonara.app`**
  (direktori `java/com/mixtune/app` → `java/com/sonara/app`,
  semua `package` + `import`, manifest, `R.java`, build script).
- `versionName` **"1.0"**, `versionCode` **1** (reset; bukan
  lanjutan 1.10.44/vc55). About menampilkan "SONARA v1.0 (1)"
  lewat label runtime yang sama (perbaikan 10.35).
- Nama file APK: `sonara-debug.apk` (output build) +
  `sonara-debug-v1.0.apk` (salinan hash-identik untuk upload).

### 1.2 Nama kelas/fungsi/identifier (nol `mixtune` tersisa)

`SonaraApplication`, `SonaraApp` (composable root),
`SonaraState`, `SonaraTheme`, `SonaraWidget` /
`SonaraWidgetProvider`, `SonaraHttp`, `sonaraTextButtonColors` /
`sonaraTextFieldColors`, file `SonaraApplication.kt`,
`SonaraApp.kt`, `SonaraWidget.kt`, resource
`widget_sonara_small/wide` + `sonara_widget_info_small/wide`,
`gen_lib_r.py` (path `R.java`), `build.sh` (injeksi package,
path `R.java`, nama output APK). Otoritas FileProvider:
`com.sonara.app.fileprovider` (WAJIB berubah — otoritas lama
yang sama dengan app MIXTUNE yang masih terinstall akan
membuat install gagal `INSTALL_FAILED_CONFLICTING_PROVIDER`).
ID internal: channel notifikasi `sonara_playback`, prefs
`sonara` / `sonara_secure` / `sonara_crash` (+ key alias),
`window.sonaraPlay/Pause/Seek/Volume` (bridge JS WebEmbed),
User-Agent LRCLIB `SONARA/1.0`, action string
`com.sonara.app.action.*`.

### 1.3 String user-visible → SONARA

Launcher label, label widget (`SONARA` / `SONARA Wide`), About
(`SONARA v…`, fallback, contentDescription logo, bullet
"YouTube Music", label ClipData), header laporan crash
(`SONARA — Crash Report`), mark kartu lirik (`SONARA`), nama
channel notifikasi (`SONARA Playback`), tag MediaSession,
judul fallback notifikasi, teks credits/history, teks share
`(via SONARA)`, teks share Jam (`Join my Jam on SONARA!`).

### 1.4 Deep link

- Primer: **`sonara://jam/<KODE>`** (`JAM_DEEP_LINK_SCHEME =
  "sonara"`, intent-filter baru di manifest, `jamDeepLinkUrl`
  menghasilkan bentuk ini).
- Fallback legacy: **`mixtune://` tetap diterima** — intent-filter
  tambahan di manifest + `JAM_DEEP_LINK_SCHEME_LEGACY =
  "mixtune"` + `isJamDeepLinkUri` menerima kedua skema. Link
  lama yang sudah dishare tetap membuka app.
- Bridge https **TIDAK berubah**:
  `https://moloqo.github.io/mixtune/jam/<KODE>` (repo/URL sama;
  konstanta `JAM_BRIDGE_PATH_PREFIX = "/mixtune/jam/"` dan
  parsing segmen path dipertahankan). Update file bridge
  (`assetlinks.json` perlu mencantumkan package baru) adalah
  tugas parent lewat browser task — BUKAN bagian build ini
  (lihat §5).

### 1.5 Firebase — project sama, keystore sama

- Project **tetap `mixtune-9f676`** (project id, RTDB URL,
  API key, Web OAuth client id di `SyncConfig.kt` TIDAK
  berubah). Keystore debug **sama** (`debug.keystore`, MD5
  `289c4d7ff84aa71d5ee0e14a6a531c80`) — sidik jari sertifikat
  APK identik dengan 10.44 (SHA-256
  `e83b1d93…c5da06`, SHA-1 `4942789c…02b0`).
- `google-services.json` (baru di root proyek): HANYA dua
  `package_name` diubah ke `com.sonara.app` (client_info +
  android_info); api key, project id, mobilesdk_app_id,
  kedua OAuth client id, certificate_hash byte-identik dengan
  file asli unduhan Firebase console (MD5 asli
  `95d557337af1c7ed7b83126822263bfd` → baru
  `e855bc04c36cad1bf9b9fd72a80dfa63`; diff = 2 baris).
  TIDAK ada OAuth client id yang dikarang.
- **LANGKAH PARENT (belum dilakukan):** registrasi app Android
  baru (`com.sonara.app`, SHA-1 yang sama) di Firebase console
  lewat browser task. Sampai langkah itu selesai, perilaku
  Google Sign-In di HP BELUM terverifikasi — cek login/sinkron
  di HP dilakukan SETELAH registrasi (lihat §5). APK tetap
  compile + install bersih dengan config ini.

### 1.6 Yang TIDAK berubah (disengaja, dicatat)

- Logo vector `ic_logo_ripples` ("Dua riak, satu sumber") —
  tanpa teks, cocok dengan cerita gelombang suara SONARA.
  Hanya komentar XML-nya ikut rename; geometri mark identik
  (dibuktikan harness145 G1).
- `Garis Arus`, ketujuh nama tema (ARUS, Senja Gelap, Blok,
  GETAR, RASI, PASAR, SENYAP), seluruh UI Inggris, tabel
  token tema (byte-identik kecuali baris package — harness145
  G2), atribusi mesin `MixMusic (fzn0x/mixmusic, MIT)`.
- Nama direktori workspace `~/workspace/mixtune-app` (internal
  saja), nama repo GitHub Pages + bridge, node RTDB, nama file
  APK lama + BUILD doc lama (riwayat), isi `README.md` /
  bagian lama DESIGN-SPEC (riwayat).

### 1.7 Konsekuensi fresh install (jujur, sampaikan ke user)

`versionCode 1` + package baru = app TERPISAH: app MIXTUNE
lama tetap terinstall sampai user uninstall manual. Data
lokal-saja TIDAK pindah: cover playlist, `play_counts`,
follow lokal, posisi resume podcast. Data cloud (playlist
buatan, liked, history) sinkron kembali setelah login (setelah
langkah registrasi Firebase §1.5). Preferensi tema + antrean
lokal mulai dari nol. Widget lama (MIXTUNE) tetap menunjuk app
lama; widget SONARA ditambahkan terpisah dari picker.

## 2. Verifikasi

### 2.1 Harness145 (baru, reproduce-first)

Baseline (snapshot tree 10.44 pra-rename): **27 REPRO FAIL**
+ guard lolos → REPRODUCTION CONFIRMED (ambang blind-checker
≥10 terpenuhi). Tree baru: **38/38 PASS, 0 FAIL, 0 SKIP**.
Mencakup: semua `package` = `com.sonara.app*`; nol `mixtune`
di code/config di luar allowlist; komentar yang menyebut
mixtune hanya catatan rename/legacy/bridge; seluruh string
user-visible SONARA; skema primer + legacy; versi 1/1.0; fit
(SONARA 6 < MIXTUNE 7 huruf, mark kartu center-measured,
tanpa fixed-width); `google-services.json` (2 package_name,
project id sama, key/id/hash identik, 0 client karangan);
nol nama file/dir mengandung mixtune; guard logo, token tema,
Garis Arus + 7 tema, permission (8, sama). Koreksi checker
(dicatat jujur): G1 membandingkan XML drawable tanpa komentar
(komentar ikut rename, geometri tidak); G2 membandingkan
AppTheme tanpa baris package + trailing whitespace (artefak
stripper); R4 memeriksa BLOK komentar, bukan per baris
(komentar URL bridge multi-baris + project id dikecualikan
sebagai substring allowlist).

### 2.2 Bukti level artefak (independen dari source)

- `aapt2 dump badging`: `package com.sonara.app,
  versionCode 1, versionName 1.0`.
- `aapt2 dump strings`: `SONARA` 2 hit (app_name,
  widget_empty), `MIXTUNE` 0, nol baris `mixtune` di string
  pool resources.arsc.
- `aapt2 dump xmltree` manifest: label `SONARA` / `SONARA
  Wide`, scheme `sonara` + `mixtune` (legacy) + `https`.
- Dex (3 file): byte `MIXTUNE` = 0 di semua dex; `SONARA` 7
  hit; `mixtune` lowercase HANYA di: URL bridge
  (`moloqo.github.io/mixtune/jam/`), path bridge
  (`/mixtune/jam`), URL RTDB + project id (`mixtune-9f676`),
  skema legacy (`mixtune`, `mixtune://jam`) — semua item
  allowlist yang didokumentasi.
- Grep scope code/config/resources (`app/src`,
  `build-manual/build.sh`, `build-manual/gen_lib_r.py`):
  15 hit tersisa, SEMUANYA allowlist — komentar rename/
  legacy/bridge + konstanta legacy + konstanta path bridge di
  `JamModel.kt`, komentar rename + filter legacy di manifest.
  Bukti lengkap di log harness145 (`check145_fixed.log`).

### 2.3 Regresi 115–145

Loop `harness145/regress145.sh` + re-run terarah. Hijau
SEMUA kecuali item yang dicatat di bawah. Tiga koreksi
checker akibat rename (kode produk TIDAK dibengkokkan):

1. **h118/h119 — stub harness**: sed rename mengubah path
   stub di `run.sh` (`stubs/com/sonara/app/...`) sementara
   direktori stub masih `com/mixtune` → compile gagal
   (unresolved reference berantai). Perbaikan: direktori
   stub diganti namanya; re-run h118 ALL PASS, h119 ALL PASS.
   (Bukan bug produk: tree app tidak disentuh perbaikan ini.)
2. **h140f — checker crash**: `check_140f.py` masih mencari
   `mixtuneTextFieldColors` (lowercase, lolos dari sed
   `Mixtune`→`Sonara`) sehingga F7 gagal → mengeksekusi
   f-string pola regex `r'OutlinedTextField\\('` (backslash
   ganda, bug checker pre-existing yang tidak pernah
   tereksekusi karena F7 selalu lolos) → `re.error`.
   Perbaikan: nama helper → `sonaraText*` + pola regex
   diperbaiki. Re-run h140: ALL CHECKS PASS (6 sub-check).
3. **h141 G4 / h142 G1 — guard byte-identical**: membandingkan
   file dengan snapshot baseline 10.40/10.41 byte-per-byte;
   rename mengubah setiap file secara sah. Dibuktikan dulu:
   `TrackSheet.kt` vs baseline 10.40 = RENAME-ONLY, dan
   ke-17 file h142 vs baseline 10.41 = RENAME-ONLY (transform
   rename yang sama persis dengan §1). Guard diubah ke
   perbandingan rename-normalized. Re-run: h141 33/33,
   h142 37/37 ALL GREEN.

Tally lain (sesuai status pra-rename yang didokumentasi):
- **h118 watchdog: loop run GAGAL COMPILE** (butir 1 di atas
  — kerusakan harness, bukan perilaku watchdog); **re-run
  terisolasi: ALL PASS**. Satu lari lolos BUKAN bukti F2
  "diperbaiki" — suite ini tidak stabil antar-lari dan build
  ini tidak menyentuh jalur watchdog. **Investigasi khusus
  berinstrumen tetap terbuka dan terpisah; jangan tutup.**
- h118b: fixture-blocked (`/tmp/search_tory.json` tidak ada)
  — sama seperti sebelumnya.
- h139: CHECK139 + HARNESS139 ALL PASS; `measure_text`
  fixture-blocked (font Roboto /tmp hilang) — sama seperti
  10.42–10.44.
- h127: 1 kegagalan pre-existing (outline RASI/LIGHT 1.23).
  h128: 11 kegagalan identitas tema pre-existing (tabel token
  byte-identik).
- Harness pra-115 (109–112, stub `com/mixtune`) TIDAK
  disentuh — di luar scope loop, historis.

### 2.4 Build

- `build-manual/build.sh`: selalu clean dulu; hanya baris
  versi/package/path/output yang berubah. `BUILD_EXIT=0`,
  0 error. Warning Kotlin: pola yang sama dengan build
  sebelumnya (39× `Nothing?`/String, ikon deprecated,
  GoogleSignIn deprecated, dst.) — 0 warning terkait rename.
- APK: `sonara-debug-v1.0.apk` — 64.733.153 byte, MD5
  `d035bd7df0454610a2be83e29f7cb7f7`, SHA-256
  `7ef3e591c936c3aa1b7a715c6bb3d4e416f88ee8bd65278625130a189ccdfb3a`;
  aapt2 package `com.sonara.app`, versionCode 1, versionName
  1.0; apksigner v2+v3 true, 1 signer, sertifikat debug yang
  sama (SHA-256 `e83b1d93…c5da06`, SHA-1 `4942789c…02b0` —
  cocok dengan certificate_hash di google-services.json).
- Backup v1.10.44: `backups/mixtune-debug-v1.10.44.apk`,
  hash terverifikasi SEBELUM overwrite (MD5
  `25b0652c08491cfbbbd36b42f36555c4`, cocok dengan
  BUILD-v1.10.44.md). Keystore tidak berubah (MD5
  `289c4d7ff84aa71d5ee0e14a6a531c80`).
- Upload: https://muse.ai/files/1439248322600143/988078260592962/zytxhicpjs8mjg2z9qbjkh82/sonara-debug-v1.0.apk
  — kedaluwarsa 27 Nov 2026 01:48 WIB (48 jam).

## 3. Batasan jujur

- Nama channel notifikasi `sonara_playback` baru: di install
  segar tampil "SONARA Playback"; tidak ada install lama
  yang terpengaruh (package baru).
- Link https bridge (`moloqo.github.io/mixtune/jam/...`) baru
  auto-open app SONARA SETELAH parent memperbarui
  `assetlinks.json` (cantumkan package baru + sidik jari
  yang sama); sebelum itu, bridge menampilkan tombol buka
  app seperti sebelumnya. Link skema `sonara://` dan
  `mixtune://` bekerja langsung dari app (intent-filter).
- Login Google + sinkron HANYA bisa dibuktikan di HP setelah
  registrasi Firebase (§1.5). Jangan klaim berfungsi sebelum
  langkah itu.
- Verifikasi visual akhir (14 varian tema, fit wordmark)
  tetap di HP user — tidak ada perubahan geometri (SONARA
  lebih pendek dari MIXTUNE), tapi mata user adalah
  kebenaran final.

## 4. Langkah cek di HP

0. Fresh install (app lama MIXTUNE boleh tetap terinstall):
   label launcher **SONARA**; About menunjukkan
   **SONARA v1.0 (1)**.
1. Jelajahi UI (min. 1 tema terang + 1 gelap): tidak ada
   tulisan "MIXTUNE" di mana pun — header, Settings, sheet,
   kartu share lirik, widget, notifikasi, dialog crash.
2. Login Google + sinkron — HANYA setelah parent mengonfirmasi
   registrasi Firebase selesai (lihat §1.5): playlist/liked/
   history dari cloud kembali; "Synced" tampil.
3. Jam 2 HP: buat sesi → share link → link `sonara://`
   membuka app + join; link lama `mixtune://` juga membuka
   app + join; link https bridge dicoba setelah parent
   mengonfirmasi update assetlinks.
4. Jam/lirik/komentar/feed/tema: tidak berubah dari v1.10.44.
5. (Opsional) Uninstall app MIXTUNE lama bila sudah yakin
   data cloud kembali — data lokal lamanya tidak pindah.

## 5. Addendum — config rebuild dengan google-services.json asli (unduhan Firebase)

Tanggal: 25 November 2026, 01:52 WIB. Setelah registrasi app
Android baru (`com.sonara.app`, SHA-1 yang sama) selesai di
Firebase console, file `google-services.json` asli diunduh
ulang. Build ini HANYA mengganti file config itu — nol
perubahan source code, nol version bump (tetap versionCode 1 /
versionName 1.0, keystore debug yang sama).

### 5.1 Diff: file hand-edited vs unduhan Firebase (verbatim)

File lama (hand-edited, MD5
`e855bc04c36cad1bf9b9fd72a80dfa63`, dibackup ke
`backups/google-services-handedited.json`): SATU client,
`package_name` diganti ke `com.sonara.app` di 2 situs, tapi
`mobilesdk_app_id` + OAuth client Android masih milik
`com.mixtune.app` yang lama.

File baru (unduhan Firebase, MD5
`88ae6c14cec344e43a0a137b6e8cdb1f`, disalin ke
`google-services.json` proyek VERBATIM — `cmp` byte-identik,
tanpa satu nilai pun yang diedit tangan):

- `client`: 1 → **2 client**.
- `client[0]`: `com.mixtune.app` KEMBALI seperti aslinya,
  `mobilesdk_app_id`
  `1:77242001591:android:6e8f49063f8accbabac532`; OAuth
  client-nya identik dengan file asli (Android type 1 +
  Web type 3, certificate_hash sama).
- `client[1]` (BARU): `com.sonara.app`,
  `mobilesdk_app_id`
  `1:77242001591:android:a956c7bc14601d10bac532`; OAuth
  client Android type 1 BARU (client id baru, package
  `com.sonara.app`, certificate_hash = SHA-1 yang sama
  `4942789c…02b0`); OAuth client Web type 3 IDENTIK dengan
  sebelumnya (nilai di `SyncConfig.kt` cocok dengan client
  type 3 di file ini — diverifikasi, bukan diasumsikan).
- `project_info` mendapat `firebase_url` (URL RTDB) yang
  tidak ada di file lama.
- Selain itu IDENTIK: `project_id`/`project_number`/
  `storage_bucket`, API key, Web OAuth client id.

Nilai secret (API key, OAuth client id) TIDAK ditulis di
dokumen ini — yang dicatat hanya struktur dan identitas
non-secret di atas.

### 5.2 Catatan jujur: file config tidak masuk ke APK

Build ini manual (aapt2/kotlinc, tanpa plugin Gradle):
`google-services.json` TIDAK dibaca `build.sh`, tidak ada
di `res/raw`/`assets`, dan tidak ter-package ke APK.
Konfigurasi runtime di-wire dari `SyncConfig.kt` (Web OAuth
client id — tidak berubah). Karena itu APK hasil rebuild
ini praktis identik dengan sebelumnya (4 byte lebih kecil,
selisih non-determinisme build biasa) — nilai rebuild ini
adalah: (a) config proyek kini sama persis dengan kebenaran
Firebase, (b) sisi server kini punya Android OAuth client
yang terdaftar untuk `com.sonara.app`, sehingga Google
Sign-In punya prasyarat server-side yang benar. Bukti di HP
tetap lewat langkah cek §4 butir 2.

### 5.3 Build + verifikasi rebuild

- `build-manual/build.sh` (selalu clean dulu):
  `BUILD_EXIT=0`, 0 error, log `build-v1.0-configrebuild.log`.
- APK `sonara-debug-v1.0.apk` (salinan hash-identik dari
  `sonara-debug.apk`): 64.733.149 byte, MD5
  `0934b8fbc38d63204017a3c3ef9229bf`, SHA-256
  `f4b7ef69a47e9bb0da5a6c388a83279e68c47c3b9269df559f26bdb4c2765606`.
  APK lama (config hand-edited) dibackup dulu ke
  `backups/sonara-debug-v1.0-handedited-config.apk` (MD5
  `d035bd7df0454610a2be83e29f7cb7f7`, cocok dengan §2.4).
- `aapt2 dump badging`: package `com.sonara.app`,
  versionCode 1, versionName 1.0. apksigner v2+v3, 1 signer,
  sertifikat IDENTIK dengan build sebelumnya (SHA-256
  `e83b1d93…c5da06`, SHA-1 `4942789c…02b0`).
- Spot-check artefak: string pool `SONARA` hadir (app_name +
  teks widget kosong), `MIXTUNE` 0; dex: byte `MIXTUNE` 0,
  `mixtune` lowercase HANYA item allowlist §2.2 (URL/path
  bridge, URL RTDB + project id, skema legacy).
- **Harness145: 38/38 PASS, 0 FAIL.** Satu koreksi checker
  (kode produk TIDAK dibengkokkan): R9b lama menegaskan
  "semua package_name == com.sonara.app" — ekspektasi file
  hand-edited interim; pada unduhan Firebase yang benar
  check itu GAGAL (37/38) karena file asli memuat kedua
  client. R9b diperbarui menegaskan pasangan yang benar:
  client mixtune asli + client sonara terdaftar, tiap
  client dengan situs package_name-nya sendiri. Baseline
  run tidak diulang (di luar scope config-only); loop
  regresi penuh TIDAK dijalankan sesuai brief. Jalur
  watchdog TIDAK disentuh — **investigasi h118 tetap
  terbuka dan terpisah; jangan tutup.**
- Upload: https://muse.ai/files/1439248322600143/1477470441185288/b7rdra7eqwtmdfilq95vl7y6/sonara-debug-v1.0.apk
  — kedaluwarsa 27 Nov 2026 02:01 WIB (48 jam).
