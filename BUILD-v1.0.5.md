# BUILD v1.0.5 — SONARA 1.0.5 (versionCode 6)

Batch 3 celah MixMusic: PODCASTS + SPEED UI + SLEEP-TIMER EXTRAS.
Dibangun hanya dari jalur data yang terbukti live/signed-out dari VM
(fixture respons di harness150). Semua string UI Bahasa Inggris.
Tidak ada izin baru. Tidak ada perubahan: tema/token (14 varian),
Jam sync, lirik (v1.0.4 + rumus pemusatan + LyricsVersionPolicy),
komentar, feed, mesin Go, sinkron, notifikasi/widget, kode
background-playback v1.0.2, fitur v1.0.3.

## Bukti probe live (25 Nov 2026, signed-out, dari VM)

- Halaman acara: browseId `MPSPPLPNW_gerXa4Pc8S2qoUQc5e8Ir97RLuVW`
  (Huberman Lab) → `musicResponsiveHeaderRenderer` (judul, host
  "Andrew Huberman", deskripsi, artwork kotak) + shelf 20 episode
  `musicMultiRowListItemRenderer` (judul, subtitle
  "509 rb x ditonton • 1 h lalu", durasi di `playbackProgress`,
  videoId, `MUSIC_VIDEO_TYPE_PODCAST_EPISODE`, deskripsi per episode).
  Fixture: `harness150/fixtures/show_page_huberman.json`.
- Paginasi: continuation shelf → 100 episode lagi + token berikutnya.
  Fixture: `harness150/fixtures/show_page_continuation.json`.
- Search: baris acara `musicResponsiveListItemRenderer`
  ("Huberman Lab" / "Podcast • Andrew Huberman" + browseId MPSP…);
  baris episode ("Episode • <tanggal> • <Acara>" + videoId + browseId
  MPED…). Jalur Go engine TIDAK membawa baris acara — acara diambil
  lewat panggilan Kotlin terpisah yang digabung di SearchScreen saja.
  Fixture: `harness150/fixtures/search_huberman.json`.
- Durasi berbahasa Indonesia: ` • 35 mnt`, ` • 2 j, 6 mnt`,
  ` • 1 j, 39 mnt` ("j" = jam, "mnt" = menit). Parser menangani
  bentuk itu + `dtk` + `H:MM:SS`/`M:SS` + bentuk Inggris defensif.
- Chapters TERBUKTI: `/next` (client WEB, body/header sama dengan
  `nextRaw` CommentsApi) → `engagementPanels[4]`
  `engagement-panel-macro-markers-description-chapters` →
  `macroMarkersListRenderer`, 23 marker (judul + `timeDescription`
  teks "0:00"/"1:38"/"9:26"; tanpa field millis — teks di-parse).
  Episode: videoId `brzkTni-nTQ`.
  Fixture: `harness150/fixtures/next_episode_panels.json`.
- Transcript TIDAK dibangun: panel
  `engagement-panel-searchable-transcript` hanya membawa continuation
  `get_transcript`, yang mengembalikan HTTP 400 "Precondition check
  failed" untuk kedua varian param, signed-out. Membangunnya berarti
  mengarang data. Harness150 R9 menegaskan tidak ada pemanggilan
  `get_transcript` di kode (baris komentar/KDoc yang mendokumentasikan
  hasil probe dikecualikan — koreksi checker, dicatat jujur di bawah).
  (v1.0.4 sudah membuktikan timedtext kosong + tanpa captionTracks.)
- `/player` WEB untuk episode mengembalikan LOGIN_REQUIRED — chapters
  diambil dari `/next`, bukan `/player`.
- Chip halaman acara ("Terbaru / Belum selesai / …") memakai
  continuation reload berbasis status akun — TIDAK dibangun; Keep
  listening lokal + mark-played menutup kebutuhan itu. Tombol header
  Share + Save-bookmark (library akun) tidak dipakai signed-out;
  Follow aplikasi bersifat lokal. "Temukan di acara" (cari di dalam
  acara) dilewati untuk membatasi cakupan.
- Sleep timer: daftar menit TERKUNCI `listOf(0, 15, 30, 60)` di
  `SLEEP_TIMER_CHOICES_MINUTES` — fixture harness144 menegaskan
  kesamaan persis, daftar TIDAK diubah. Fade-out saat timer berbunyi
  SUDAH ada (`SleepTimer { pause() }` → `pauseEngines()` →
  ExoPlayer `fadeOutThenPause()`) — tidak ada perubahan kode, hanya
  didokumentasikan. `WebEmbedPlayer` TIDAK punya dukungan
  playback-rate — kecepatan hanya ExoPlayer; fallback embed jalan
  1.0× (didokumentasikan jujur).
- Kecepatan: `setPlaybackRate` tetap satu-satunya kontrol; drift Jam
  menulis 1.0/0.98/1.02 langsung; `startTrack` sebelumnya me-reset
  ke 1.0f. Model baru: di luar Jam → rate pengguna (per-episode bila
  tersimpan untuk konten panjang, else default); saat Jam → 1.0f,
  drift yang memiliki rate; UI kecepatan disembunyikan saat Jam.

## Yang dibangun

1. Halaman acara podcast (`PodcastShowScreen`, `Screen.PODCAST_SHOW`):
   header (artwork, judul, host, deskripsi expandable, tombol Follow
   lokal), daftar episode (baris Track dari `episodeToTrack`, status
   resume "32m left", status played, toggle mark-played per baris),
   "Load more episodes" dari continuation token (dedupe videoId),
   empty/error state jujur + Try again. Navigasi: baris acara di hasil
   Cari (maks 3, di depan), seksi "Podcasts you follow" di Library,
   ketuk nama artis di detail saat episode (bukan profil artis).
   Baris acara tidak pernah masuk antrean/seleksi/sheet lagu
   (id `show:<browseId>`, `youtubeVideoId = null`, dicegat sebelum
   jalur putar). Episode di hasil Cari ditandai `isPodcastEpisode`
   (subtitle "Episode • …" → artis = nama acara, bukan tanggal).
2. Keep listening (Beranda): snapshot posisi-lanjut (cap 20, terbaru
   dulu, played dikeluarkan, maks 10 baris), baris = Track dari
   snapshot, ketuk = putar (seek resume otomatis) + buka detail,
   trailing "32m left". Snapshot kini membawa `showBrowseId` agar
   tombol artis di detail bisa membuka halaman acara.
3. Mark played: per episode, lokal (`played_episodes_v1`); mark juga
   menghapus posisi resume; un-mark TIDAK menghidupkan posisi
   (mulai dari 0, posisi baru tersimpan saat didengarkan lagi).
   Tersedia di halaman acara, TrackSheet (episode/konten ≥ 10 mnt),
   dan (implisit) Keep listening mengecualikan yang played.
4. Chapters: tab CHAPTERS ke-4 di detail (hanya bila chapters
   termuat; `DetailTabsRow` digeneralisasi ke tabCount 3/4, rumus
   indikator `tabIndicatorIndex` tidak berubah; indeks efektif
   dijepit 0..2 bila tab tidak ada). Daftar: waktu mulai
   (M:SS/H:MM:SS) + judul, chapter aktif ditebalkan dari posisi
   putar, ketuk = seek lewat guard Jam yang sama. Fetch: latar saat
   lagu berganti, gate episode ATAU durasi metadata ≥ 10 mnt, hanya
   jalur YTM. Rumus pemusatan lirik + LyricsVersionPolicy TIDAK
   disentuh (md5 terverifikasi).
5. Kecepatan putar: `SpeedDialog` bersama (detail + TrackSheet +
   Settings); pilihan 0.5/0.75/1.0/1.25/1.5/1.75/2.0; tombol detail
   "Speed: 1.0×" di bawah kontrol (disembunyikan saat Jam);
   `setUserPlaybackRate` menyimpan default (0.5–2.0, ter-coerce) +
   per-episode bila lagu saat ini konten panjang (durasi efektif
   setelah resolve); `startTrack` memakai `effectiveStartRate`
   (Jam → 1.0f); `playExo` menerapkan ulang rate per-episode saat
   durasi baru diketahui. Perubahan kecepatan saat lagu panjang
   diputar memperbarui entri per-episode DAN default.
6. Sleep-timer extras (mesin yang SAMA, `setSleepTimer`): mode
   "End of track" — berhenti di akhir lagu/episode saat ini, tanpa
   advance/repeat/autoplay, sekali saja, flag dibersihkan, nonaktif
   saat Jam. Chip di dialog sleep layar detail + baris chip di
   Settings → Playback (status ikut dibaca). Memilih menit
   membersihkan flag, dan sebaliknya. Fade-out sudah ada (lihat
   probe di atas). Daftar menit TIDAK diubah.
7. Follow acara: lokal (`followed_shows_v1`, cap tidak diubah),
   seksi Library "Podcasts you follow", versi `podcastFollowVersion`
   di SonaraState (pola yang sama dengan `followedVersion` artis).

## Yang TIDAK dibangun (jujur)

- Transcript / terjemahan / romaji untuk episode (probe: 400 +
  timedtext kosong + tanpa captionTracks — lihat probe di atas).
- Chip filter halaman acara, cari-di-dalam-acara, tombol Share/Save
  akun (status akun, tidak tersedia signed-out).
- Label AI / SponsorBlock untuk podcast (nol token label signed-out;
  SponsorBlock umum dari v1.0.3 tetap berlaku untuk segmen video).
- Kecepatan untuk WebEmbedPlayer (tidak ada dukungan rate di embed;
  1.0× saat fallback embed).
- Perubahan daftar menit sleep timer (terkunci harness144).

## Verifikasi

- harness150: reproduksi pada tree v1.0.4 yang belum disentuh =
  33 FAIL / 6 PASS. Setelah build: 39/39 source checks PASS +
  seluruh fixture Kotlin PASS (F1–F9: parse acara, durasi Indonesia,
  continuation, search acara, 23 chapters, parse waktu chapter,
  policy kecepatan, keep-listening, episode→Track).
  Koreksi harness yang jujur: (a) R9 kini mengecualikan baris
  komentar/KDoc — dua penyebutan `get_transcript` ada di KDoc
  PodcastApi/ChaptersApi sebagai dokumentasi hasil probe, bukan
  pemanggilan; (b) `Harness150.kt` F1 meneruskan browseId seperti
  pemanggil produksi; (c) `run.sh` mengompilasi `ResumePosition.kt`
  (sumber konstanta `RESUME_MIN_DURATION_MS` yang dipakai
  PlaybackSpeedPolicy). Tidak ada ekspektasi fixture yang diubah.
- Guard md5: AppTheme.kt `2480bca8…` (byte-identik),
  LyricsFollow.kt `cccee070d2178bde1b69c09e61f63607`,
  LyricsApi.kt `3f82d64bdb38ea09d483a76a406747d9` — ketiganya
  tidak berubah.
- Clean rebuild: 0 error. Peringatan: hanya peringatan
  kotlin-home yang sudah ada sebelumnya (stdlib/reflet — bawaan
  build script, bukan dari kode baru); 0 peringatan BARU dari
  file-file v1.0.5 (grep `warning: *.kt` pada log build = 0).
- aapt2: package `com.sonara.app`, versionCode 6, versionName 1.0.5.
- apksigner: v2+v3 true, v1/v3.1/v4 false, 1 signer, sertifikat
  SHA-256 `e83b1d93926c1ebbfebd8d0f95391ff5b4fc83b978ebbef1fd88f451c2c5da06`
  — SAMA dengan backup v1.0.4.
- Backup v1.0.4 terverifikasi hash (MD5
  `ab5e0b3722ba8d67e51ccb9e68f6e563`) di
  `backups/sonara-debug-v1.0.4-vc5.apk` SEBELUM overwrite.

## Regresi (spot-check)

- h149: ALL PASS (Kotlin F1–F12, termasuk guard rumus pemusatan lirik).
- h148: 45 PASS, 0 FAIL — dijalankan dengan path jar org.json yang
  diperbaiki (salinan run.sh di /tmp; harness148 TIDAK dimodifikasi;
  run.sh apa adanya gagal kompilasi di VM ini karena path jar kosong
  di transforms cache — kerusakan env yang sudah ada sebelumnya).
- h147: ALL PASS (K1–K15).
- h130: 79 checks, 0 failures + GUARDS OK.
- h118: satu putaran pada tree v1.0.5 — fase dormant (diharapkan
  gagal) F1/F2/F3/F4 gagal sesuai desain; fase live: 55 PASS, 1 FAIL
  (F4 buffering-stall recovers, `calls=2 base=1 buffering=false`).
  F1/F2/F3 lolos pada putaran ini. F2 TIDAK dinyatakan fixed —
  suite tidak stabil antar putaran (riwayat: gagal deterministik
  3× di 10.41, lolos di 10.42/10.44/v1.0/v1.0.1, F4 gagal di
  v1.0.2/v1.0.3) dan investigasi watchdog khusus masih terbuka;
  tidak ada jalur watchdog yang disentuh build ini.

## Artefak

- APK: `sonara-debug-v1.0.5.apk` — ukuran 65.126.365 byte,
  MD5 `feb084db6a9ff42da28f3a248e14440e`,
  SHA-256 `bedb3e7ea2c7812747ab874d11b9cf11c3712ce5de3694880e44ae31ec0b94ce`.
  Link: https://muse.ai/files/1439248322600143/1722978209440894/gg2puovn0044pzz189qzdkqs/sonara-debug-v1.0.5.apk
  (kedaluwarsa 27 Nov 2026 11:40 WIB / 04:40 UTC).
- File baru: `data/PodcastApi.kt`, `data/ChaptersApi.kt`,
  `data/PlaybackSpeedPolicy.kt`, `data/KeepListeningPolicy.kt`,
  `ui/screens/PodcastShowScreen.kt`, `ui/SpeedDialog.kt`.
- File diubah: `data/Models.kt` (3 field podcast di Track),
  `data/LocalStore.kt` (snapshot + 5 store baru),
  `player/PlayerManager.kt` (rate + end-of-track + snapshot),
  `data/YoutubeMusicApi.kt` (`browseContinuation`, `searchShows`,
  `fetchChapters`, deteksi episode di `parseItem`),
  `ui/SonaraApp.kt` (Screen + target + follow acara),
  `ui/screens/HomeSearch.kt` (acara di Cari + rak Keep listening),
  `ui/screens/SettingsAbout.kt` (tab CHAPTERS + Speed + sleep
  end-of-track + Settings Playback),
  `ui/screens/TrackSheet.kt` (speed + mark played),
  `ui/screens/QueueLibrary.kt` (seksi acara diikuti),
  `build-manual/build.sh` (vc6/1.0.5).

## Langkah cek di HP

About: SONARA v1.0.5 (6). (1) Cari "Huberman" → baris acara di
atas hasil → halaman acara menampilkan episode nyata + Load more.
(2) Follow/unfollow → muncul/hilang di Library "Podcasts you
follow". (3) Putar episode, jeda di tengah → rak "Keep listening"
di Beranda menampilkan episode + "left", ketuk = lanjut dari
posisi. (4) Mark as played di halaman acara/sheet → hilang dari
Keep listening + label "Played"; un-mark mengembalikan (posisi
dari 0). (5) Episode panjang → tab CHAPTERS di detail, ketuk
chapter melompat. (6) Tombol "Speed:" di detail mengubah
kecepatan; putar episode lain yang sama = kecepatan diingat;
lagu baru = default terakhir. (7) Sleep "End of track" berhenti
di akhir lagu tanpa lanjut. (8) Jam/lyrics/tema/sync/background
tidak berubah.
