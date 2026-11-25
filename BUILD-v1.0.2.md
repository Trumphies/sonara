# BUILD v1.0.2 (versionCode 3) — SONARA

Tanggal: 25 Nov 2026 (WIB). Satu scope atas perintah user (09:32 WIB):
"buat biar bisa run di background" — pemutaran harus sungguhan
bertahan di background (Home / layar kunci / swipe dari recents).

## 1. Audit: empat celah, semua terbukti dari kode sebelum disentuh

PlaybackService SUDAH ada (foreground service, MediaSession,
notifikasi, START_STICKY, foregroundServiceType mediaPlayback,
semua izin dideklarasi). Yang rusak adalah celah-celah di
sekitarnya. Harness147 membuktikan keempatnya dari tree v1.0.1
(14/14 FAIL + compile fixtures gagal karena AudioFocusPolicy.kt
belum ada).

### Gap 1 — TIDAK ADA wake mode / atribut audio (TERBUKTI)
`PlayerManager.kt:221` (pre-fix): `ExoPlayer.Builder(appContext).build()`
tanpa `setWakeMode`, tanpa `setAudioAttributes`. Izin WAKE_LOCK
dideklarasi di manifest tapi tidak ada yang memegang wake lock —
CPU boleh tidur saat layar mati. Tersangka utama, terutama di OEM
agresif (HP user Infinix/XOS).

### Gap 2 — SILENT SWALLOW + flag `started` menipu (TERBUKTI)
`PlayerManager.kt:1085` (pre-fix): `startService()` menelan SEMUA
exception dari `startForegroundService` tanpa jejak. `PlaybackService.kt:162`
(pre-fix): `started = true` disetel SEBELUM `try { startForeground() }`
— sekali startForeground gagal (mis. pembatasan start FGS Android 12+
dari background), service jalan TANPA status foreground dan tidak
pernah mencoba lagi, tanpa diagnostik apa pun. Start dari background
relevan di sini: auto-play tamu Jam / autoplay bisa terpicu saat app
di-background-kan.

### Gap 3 — WebView embed milik activity (TERBUKTI)
Satu-satunya situs pembuatan WebView: `SonaraApp.kt:933` (pre-fix),
`WebView(ctx)` di dalam composition (activity context).
`WebEmbedPlayer.attach()` mempertahankan WebView PERTAMA selamanya.
Activity dihancurkan (swipe dari recents) = WebView mati = lagu
jalur embed berhenti; activity dibuat ulang = WebView basi dari
activity yang sudah mati tetap dipakai. Komentar lama ("dibuat di
Application context bisa merusak dialog/input") tidak berlaku:
WebView ini audio-only IFrame, tanpa dialog/input/file chooser.

### Gap 4 — TIDAK ADA audio focus / becoming-noisy (TERBUKTI)
Grep seluruh tree: nol `AudioFocus`, `AUDIOFOCUS`, `becomingNoisy`,
`AudioManager`, `OnAudioFocusChangeListener`. Tidak ada penanganan
sama sekali — telepon / app lain / headphone dicabut tidak pernah
menjeda atau meredupkan pemutaran.

## 2. Perbaikan per celah

### 2.1 Wake mode + atribut audio (PlayerManager, builder ExoPlayer)
- `setWakeMode(C.WAKE_MODE_LOCAL)` — ExoPlayer memegang
  PARTIAL_WAKE_LOCK sendiri selama putar.
- `setAudioAttributes(USAGE_MEDIA + CONTENT_TYPE_MUSIC, handleAudioFocus=false)`.
  `handleAudioFocus=false` disengaja: fokus dikelola PlayerManager
  sendiri supaya perlakuannya IDENTIK untuk jalur ExoPlayer dan
  jalur embed — fokus bawaan ExoPlayer hanya menutup jalur ExoPlayer,
  dan ducking internalnya akan dilawan `applyVolume()` yang menulis
  volume tiap 100ms (satu-satunya penulis volume).
- `setHandleAudioBecomingNoisy(false)` — perilaku noisy dimiliki
  PlayerManager lewat satu receiver (butir 2.3), bukan dua pemilik.

### 2.2 Foreground service: gagal = coba lagi + tercatat
- `PlaybackService`: `started = true` HANYA setelah `startForeground`
  sukses. Gagal = flag tetap false + `Log.w` ke logcat. Start
  berikutnya (setiap ganti lagu / resume memanggil `startService`)
  mencoba lagi. Notifikasi tetap diposting lewat `nm.notify`
  (`updateNotification`) supaya kontrol tetap terlihat walau
  foreground belum berhasil. MediaSession dibuat di `onCreate`
  SEBELUM startForeground — susunan yang didokumentasikan untuk
  pengecualian start FGS media dari background.
- `PlayerManager.startService()`: kegagalan dicatat `Log.w`, tidak
  lagi hilang diam-diam.
- `resume()` kini juga memanggil `startService()` (sebelumnya hanya
  playExo/playEmbed): resume dari notifikasi lockscreen setelah jeda
  panjang tidak lagi jalan tanpa FGS.

### 2.3 Fokus audio + becoming-noisy (PlayerManager, satu pemilik)
- `AudioFocusPolicy.kt` BARU (murni, tanpa import Android, teruji
  harness): pemetaan perubahan fokus ke aksi mengikuti konvensi
  platform — LOSS permanen = jeda + lepas fokus (gain berikutnya
  TIDAK me-resume sendiri); TRANSIENT = jeda, gain boleh resume;
  CAN_DUCK = volume ×0.2; GAIN tanpa kehilangan = abaikan (tidak
  pernah meng-override jeda user); nilai tak dikenal = abaikan.
- `AudioFocusRequest` (GAIN, atribut media, delayed-gain diterima)
  diminta di `playExo`/`playEmbed`/`resume`, dilepas di `pause`/
  `stop`. Request gagal = tetap putar seperti dulu + `Log.w`
  (best-effort; jaring pengaman sesungguhnya adalah penanganan
  kehilangan fokus).
- Duck: `focusDuckScale` dikalikan di `applyVolume()` (satu-satunya
  penulis volume ExoPlayer; volume embed ikut dihitung dari skala
  yang sama). `pause()` user membersihkan `pausedByFocusLoss`;
  jeda karena fokus memakai `pauseEngines()` terpisah supaya gain
  bisa me-resume.
- Receiver `ACTION_AUDIO_BECOMING_NOISY` didaftarkan saat putar
  mulai, dilepas saat jeda/stop (via `ContextCompat.registerReceiver`
  + `RECEIVER_NOT_EXPORTED`; tidak ada izin baru). Headphone dicabut
  = jeda, kedua engine.

### 2.4 WebView embed milik Application (WebEmbedPlayer + SonaraApp)
- `WebEmbedPlayer.ownedView()`: SATU WebView dari `applicationContext`,
  dibuat malas di thread Main, dikonfigurasi sekali (pengaturan yang
  sama persis dengan attach() lama). Tema application context sudah
  AppTheme (`android:theme` di `<application>`), jadi pembuatan
  dengan application context aman.
- View dilepas dari parent lamanya sebelum dikembalikan, supaya
  bisa di-host ulang oleh composition baru setelah activity dibuat
  ulang. Selama UI ada, composition tetap me-host view yang sama
  lewat `AndroidView` 1dp (perilaku lama dipertahankan); saat
  activity hancur, objeknya bertahan di Application.
- `attach()` dipertahankan sebagai kompatibilitas dan kini
  mengabaikan view asing (activity context) — kepemilikan ada di
  `ownedView()`. `destroy()` menghancurkan owned view dan meng-nol-kannya.
- `SonaraApp.kt`: factory `AndroidView` kini mengembalikan
  `app.embedPlayer.ownedView()`; import WebView yang menganggur
  dihapus.

## 3. Yang TIDAK disentuh
Perilaku notifikasi/MediaSession/widget (hanya diperluas di titik
yang dibutuhkan celah di atas), sinkron Jam, sleep timer, lirik,
komentar, feed, tema, mesin Go, string UI, pembacaan label versi.
Tidak ada izin baru. Tidak ada kerja repo publik (tugas terpisah
yang tertunda — tidak disentuh).

## 4. Verifikasi

### Harness147 (reproduce-first)
- Tree v1.0.1: **14/14 FAIL** (W1, W2, W3, S1a, S1b, S2, V1, V2, V3,
  F1, F2, F3, F4, P1) + compile fixtures gagal (AudioFocusPolicy.kt
  belum ada).
- Sesudah perbaikan: **14/14 PASS** + fixtures Kotlin **15/15 PASS**.
- Koreksi checker yang dicatat jujur (kode tidak dibengkokkan):
  F4 awal menghitung definisi `private fun startService()` sebagai
  call site (lolos palsu di tree lama) — diperbaiki menghitung
  call site telanjang saja; S1b awal mencocokkan signature catch
  lama `catch (_: Exception)` — diperbaiki mencocokkan `catch (`
  apa pun. Kedua checker yang diperbaiki tetap FAIL di tree lama
  (diverifikasi dari isi kode lama: tidak ada Log.w, hanya 2 call
  site).

### Build bersih
- Backup: `sonara-debug-v1.0.1.apk` → `backups/sonara-debug-v1.0.1-vc2.apk`,
  hash terverifikasi MD5 `c83a9d17ec9a1d037dce812f65d03da8` SEBELUM
  overwrite.
- `build-manual/build.sh` BUILD_EXIT=0, 0 error. aapt2:
  `com.sonara.app` versionCode 3 / versionName 1.0.2.
  apksigner v2+v3, 1 signer, sertifikat debug SAMA dengan v1.0
  (SHA-256 `e83b1d93…c5da06`, SHA-1 `4942789c…02b0`).
- Bukti dex: string `AudioFocusPolicy`, `startForeground failed`,
  `Audio focus request returned` ada di APK.
- Peringatan Kotlin: 56 baris `: warning: `, SEMUANYA di file yang
  tidak disentuh build ini (CommentsApi 20, TrackSheet/SettingsAbout/
  GenreApi/ArtistApi 5, SyncManager 4, YoutubeMusicApi/LyricsApi 3,
  QueueLibrary/HomeSearch 2, UiCommon/JamSessionManager 1 — set
  "Java type mismatch" yang sudah dikenal); NOL di file yang diubah
  (PlayerManager, WebEmbedPlayer, SonaraApp, PlaybackService,
  AudioFocusPolicy). 0 peringatan BARU.

### Regresi spot-check
- harness146 (disc pairs): ALL PASS (fixtures fails=0).
- harness130 (cluster): 79/79 + guards OK.
- harness118 (Jam watchdog): loop run 1 kegagalan — F4
  (`calls=2 base=1 buffering=false`); F1/F2/F3 PASS di run yang
  sama. Isolated re-run: ALL PASS (exit 0), termasuk F2 dan F4.
  Build ini TIDAK menyentuh jalur watchdog apa pun (tidak ada file
  Jam yang diubah). Suite tetap tidak stabil antar-run: F2 TIDAK
  dinyatakan fixed; investigasi instrumented khusus tetap TERBUKA
  dan terpisah.
- h118b fixture-blocked + h139 measure_text fixture-blocked:
  pre-existing, tidak berubah, dicatat apa adanya.

## 5. File yang diubah
- `app/src/main/java/com/sonara/app/player/PlayerManager.kt`
  (builder wake/attrs/noisy-off, focus manager, noisy receiver,
  pauseEngines, duck scale, resume/startService, logging)
- `app/src/main/java/com/sonara/app/player/AudioFocusPolicy.kt`
  (BARU, murni)
- `app/src/main/java/com/sonara/app/player/WebEmbedPlayer.kt`
  (owned app-context WebView)
- `app/src/main/java/com/sonara/app/service/PlaybackService.kt`
  (started-setelah-sukses + Log.w)
- `app/src/main/java/com/sonara/app/ui/SonaraApp.kt`
  (AndroidView meng-host owned view; import WebView dihapus)
- `build-manual/build.sh` (`--version-code 3 --version-name 1.0.2`)
- `harness147/` (check_147.py, Harness147.kt, run.sh, log regresi)
- `DESIGN-SPEC.md` §5.61 (appendix)
- `BUILD-v1.0.2.md` (dokumen ini)

## 6. Batas jujur
- Pembuktian sungguhan (musik tetap jalan 5 menit layar terkunci,
  tetap jalan setelah swipe dari recents) HANYA bisa dari HP user.
  Harness membuktikan celah kode tertutup, bukan kelangsungan hidup
  proses di perangkat — terutama Infinix/XOS yang punya setelan
  baterai/autostart sendiri di luar jangkauan kode. Bila setelah
  perbaikan ini HP masih membunuh pemutaran, langkah berikutnya
  adalah memeriksa setelan tersebut, bukan menebak-nebak kode lagi.
- Start FGS dari background (Android 12+) tetap bisa ditolak sistem
  pada percobaan pertama; perbaikannya adalah retry + diagnostik,
  bukan jaminan. MediaSession aktif adalah jalur pengecualian yang
  didokumentasikan, dan susunannya sudah seperti itu.
- Jalur embed dengan WebView yang tidak ter-host (activity hancur)
  mengandalkan perilaku background-tab Chromium untuk audio; ini
  tidak bisa dibuktikan dari VM — langkah HP no. 3 yang
  membuktikannya.
- Fokus audio bersifat best-effort saat request ditolak; perilaku
  kehilangan-fokus (jeda/duck) adalah jaring pengamannya.

## 7. Langkah cek di HP
0) About menunjukkan SONARA v1.0.2 (3).
1) Jalur Exo: putar lagu → Home → kunci layar, 5 menit, musik
   tetap jalan tanpa putus; kontrol notifikasi (play/pause/next/
   stop) bekerja dari lock screen.
2) Swipe app dari recents saat sedang putar → musik lanjut,
   notifikasi tetap; Stop mengakhirinya.
3) Lagu jalur fallback embed (hanya bila ada yang kena fallback):
   tes background yang sama.
4) HP tamu Jam yang di-background-kan selama sesi tetap mengikuti
   host.
5) Bila Infinix tetap membunuh: cek setelan baterai/autostart —
   tetapi hanya SETELAH perbaikan kode ini dites.
