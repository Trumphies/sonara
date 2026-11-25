# BUILD v1.0.3 (versionCode 4) — SONARA

Tanggal: 25 Nov 2026 (WIB). Batch 1 dari perintah user "bikin semua"
(09:45 WIB): celah MixMusic — pencarian, antrean/radio, tidak suka,
info lagu. Setiap jalur data di-probe live/signed-out dari VM DULU;
hanya yang terbukti dibawa datanya yang dibangun. Tidak ada angka,
label, atau lirik karangan.

## 0. Ringkasan fitur

1. Saran pencarian saat mengetik (jalur suggest YTM, signed-out);
   ketuk saran lagu = langsung putar. Riwayat pencarian (maks 8) tetap.
2. Urutan hasil cari: Best match / Most played / Newest + hitungan
   putar/tonton di hasil, hanya bila respons benar-benar membawanya.
3. Tempel link YouTube/YTM di kolom cari + Enter = putar/buka.
   Link layanan lain = toast jujur, tanpa pencarian teks sampah.
4. Start radio dari menu lagu: lagu radio disimpan TERPISAH dari
   antrean user (seksi "Radio" di tab UP NEXT), lagu user selalu
   menang, radio mengalir saat antrean user habis + isi ulang
   otomatis. NON-Jam saja: saat sesi Jam aktif, radio dibersihkan
   dan aksinya disembunyikan (antrean milik sesi).
5. Dislike yang benar-benar melompati: set tidak-suka persisten
   lokal; perpindahan (otomatis + tombol Next) melewati lagu yang
   tidak disukai; Library mendapat daftar "Disliked songs"
   (terbaru dulu, cari + urut, Restore = batal tidak suka).
6. Label "made with AI": TIDAK DIBANGUN — tidak tersedia signed-out
   (lihat §5, jujur).
7. Sheet "Track info": sumber, videoId, format/bitrate stream nyata
   dari jalur resolve, dan jejak langkah resolve apa adanya. Lagu
   yang gagal putar: tombol "Details" di kotak error membuka dialog
   yang sama (apa yang dicoba, kenapa tiap percobaan gagal).
8. SponsorBlock: lewati bagian sponsor/intro/outro + penanda di
   seek bar + chip Undo 6 detik + toggle di Settings → Playback.
   Dibangun dari bentuk respons API yang terdokumentasi; belum ada
   payload segmen sungguhan yang berhasil diambil dari VM (semua
   videoId yang di-probe mengembalikan 404 = tidak ada segmen) —
   lihat §5. HP adalah pembuktian akhirnya.
9. Siaran live: kanal live diputar lewat HLS + label "LIVE" ganti
   durasi + seek dimatikan (terbukti lewat jalur resolve yang ada).

Tidak berubah: tema/token (14 varian), Jam, sleep timer, lirik,
komentar, feed, mesin Go, sinkron, notifikasi/widget, kode
background-playback v1.0.2. Tidak ada izin baru. Tidak ada jalur
Spotify, tidak ada sign-in YTM, Video mode tetap dihapus.

## 1. Bukti probe live (25 Nov 2026, dari VM; fixture di harness148/fixtures/)

- Saran: POST music/get_search_suggestions (WEB_REMIX, gl=ID/hl=id)
  → 200, 6 saran untuk "coldplay". Ketiga input yang dicoba
  ("coldplay", "yellow", "coldplay yellow", "espresso sabrina")
  → SEMUA saran bertipe query (searchEndpoint); NOL saran entitas
  (watchEndpoint) dalam keadaan signed-out. Parser tetap mendukung
  watchEndpoint secara defensif (ketuk = langsung putar bila YTM
  suatu saat mengembalikannya).
- Cari: WEB_REMIX search → 200, 31 musicResponsiveListItemRenderer.
  Ada 9 chip filter di respons (searchEndpoint) — filter TIDAK
  dibangun di batch ini (di luar scope). Tidak ada kontrol sort
  server → urutan non-best-match dihitung sisi klien dari data
  respons. Subtitle TERBUKTI membawa hitungan + tahun: "48 jt x
  ditonton • 4.32", "2,4 M pemutaran", "418 rb pemutaran",
  "1,1 jt x ditonton", "Single • Coldplay • 2003". Artis membawa
  "460 jt audiens bulanan" — BUKAN hitungan putar, parser
  mengabaikannya. NOL badge musicInlineBadgeRenderer; NOL token
  "synthetic"/"altered".
- Radio: POST /next (client WEB) dengan playlistId "RD"+videoId DAN
  "RDAMVM"+videoId → keduanya 200, 25–26
  playlistPanelVideoRenderer. Dipilih RD (artis bervariasi: Goo Goo
  Dolls, Hoobastank, Harry Styles setelah seed; RDAMVM semuanya
  Coldplay = artist mix). Panel membawa videoId, judul,
  longBylineText (artis), lengthText ("4.26"), satu thumbnail
  i.ytimg. Continuation TIDAK dipakai — isi ulang memakai seed lagu
  radio terakhir. Panel pertama = seed itu sendiri (disaring).
- SponsorBlock: GET skipSegments → HTTP 404 bersih ("Not Found" =
  tidak ada segmen) untuk yKNxeF4KMsY, dQw4w9WgXcQ, 9bZkp7q19f0,
  kJQP7kiw5Fk. API terjangkau; tidak ada payload segmen yang bisa
  dipakai sebagai ground truth dari VM.
- Live: dengan visitorData, 5yx6BWlEVcY → status OK +
  isLiveContent: true + hlsManifestUrl + 8 format adaptif.
  (jfKfPfyJRdk UNPLAYABLE "live stream recording is not available";
  DWcJFNfaw9c LOGIN_REQUIRED bot check.) Jalur resolve yang ada
  sudah menangani isLive → HLS; yang baru hanya plumbing UI.
- Label AI: NOL token badge/synthetic/altered di respons search
  MAUPUN player → fitur 6 dilewati dengan jujur (lihat §5).

## 2. Perbaikan per fitur

### 2.1 Saran ketikan
- `YoutubeMusicApi.searchSuggestions()` (POST get_search_suggestions,
  WEB_REMIX) + parser murni `SearchSuggestParser.kt`
  (searchSuggestionRenderer di seluruh respons; query vs videoId).
  Gagal/kosong = daftar kosong, bukan error.
- `SearchScreen`: debounce 300ms terpisah dari debounce cari 500ms;
  hasil basi dibuang (query sudah berubah); daftar di bawah kolom
  cari, maks 6; sembunyi saat submit/clear/link. Ketuk saran video
  → pola pending-link yang sama dengan MainActivity
  (`pendingYtmVideoId` → SonaraApp me-resolve + memutar + membuka
  detail). Ketuk saran query → cari query itu + catat riwayat.

### 2.2 Urutan + hitungan
- `MediaTextParse.kt` (murni): parse hitungan dari bagian subtitle
  ("rb"/"jt"/"K"/"M"/"B", desimal koma; mengabaikan "audiens
  bulanan"), parse tahun (4 digit 1900–2100 di bagian non-durasi),
  `formatPlayCount` ("2.4M plays" / "48M views" / "418K plays").
- `Track` + `YtmResult` mendapat `playCount`/`countKind`/`year`
  (0/"" = tidak dibawa respons). `parseItem` mengisi dari subtitle
  yang sama; `search()` + `trackFromResult()` memetakan.
- `SearchSortPolicy.kt` (murni, stabil): BEST_MATCH = urutan server
  apa adanya; MOST_PLAYED/NEWEST = urutan sisi klien, yang tanpa
  data kunci tetap di urutan server di belakang. Urutan non-best
  memaksa jalur Kotlin (`forceKotlin=true`, preseden v1.10.25)
  karena jalur Go tidak membawa subtitle hitungan.
- UI: chip urutan hanya saat sedang mencari; ganti urutan
  menjalankan ulang pencarian yang sama. `TrackRow.showPlayCount`
  (hanya SearchScreen) menampilkan baris hitungan di bawah subjudul
  bila hitungannya nyata.

### 2.3 Tempel link
- `doSearch()` memeriksa link DULU: video → `pendingYtmVideoId`
  (pola yang sama, tanpa duplikasi resolve); playlist saja → toast
  jujur; URL layanan lain → toast jujur ("paste a YouTube Music
  link"), tanpa pencarian teks. Teks link yang sedang diketik
  TIDAK dicari sebagai teks dan TIDAK auto-play — menunggu Enter
  (`onSearchSubmit` menjalankan `doSearch()`). `recordSearch`
  tidak mencatat teks link.

### 2.4 Radio
- `YoutubeMusicApi.fetchRadio()` (POST /next + RD…) +
  `RadioParser.kt` (panel → `RadioItem`) + `RadioPolicy.kt`
  (saring seed/tidak-suka/duplikat, cap 25, ambang isi ulang ≤3).
- `PlayerManager`: state radio TERPISAH (`_radioQueue`,
  `_radioIndex`, `_radioActive`, `_radioSeed`) — tidak pernah masuk
  `_queue`, tidak pernah disimpan `saveQueue`, tidak menyentuh
  matematika antrean/Jam. `next()`: lagu user berikutnya yang boleh
  diputar (lewati tidak-suka) → lagu radio berikutnya → Repeat ALL
  mengulang dari lagu user pertama yang boleh diputar → berhenti.
  Selama Jam, `next()` berperilaku persis seperti sebelumnya
  (tanpa lompat-tidak-suka, tanpa radio).
- `startRadio(seed)`: fetch RD, saring, simpan; seed diputar via
  `playTrack` bila belum sedang diputar (tidak me-restart lagu yang
  sedang bunyi). `playRadioAt(i)` untuk ketuk baris radio.
  `maybeRefillRadio()`: seed = lagu radio terakhir, saring dari
  yang sudah ada + tidak-suka; gagal = radio habis tanpa error.
  `playQueue()` (antrean eksplisit baru) membersihkan radio basi.
  `clearRadio()` dipanggil dari SATU-SATUNYA titik aktivasi Jam
  (`JamSessionManager.startSession()`) + aksi "Stop radio" di UI.
- `replaceCurrentTrack()` kini hanya menulis ke `_queue` bila lagu
  di indeks itu memang lagu yang sedang diputar (guard id) —
  sebelumnya resolve lagu radio akan menimpa lagu user di posisi
  terakhir (korupsi antrean; guard aman untuk jalur lama karena
  playAt selalu menunjuk lagu yang sedang diputar).
- UI: aksi "Start radio" di sheet lagu (di samping "Start mix" —
  semantik berbeda, keduanya dipertahankan), disembunyikan selama
  Jam dan tanpa videoId. Seksi "Radio" di tab UP NEXT (di luar Jam
  saja): header + baris (tanpa reorder/swipe) + titik penanda lagu
  yang sedang diputar + "Stop radio".

### 2.5 Tidak suka yang melompati
- `LocalStore`: `dislikedTracks()`/`setDislikedTrack()` — cermin
  `likedTracks()`, kunci `disliked_tracks`, terbaru dulu, LOKAL
  SAJA (tidak disinkron; sengaja tidak memicu `onLibraryChanged`
  supaya SyncManager tidak mengenal koleksi ini). Set id
  (`disliked_ids`) yang sudah ada tetap dipakai pill.
- `QueueMath.nextPlayableIndex()` (murni): indeks berikutnya yang
  boleh diputar, melewati yang tidak disukai; -1 bila tidak ada.
  Dipakai `next()` (otomatis + manual Next). Ketuk eksplisit
  (`playAt`) tetap menang — niat user di atas daftar.
- `PlayerManager.onTrackDisliked(trackId)`: bila lagu itu sedang
  diputar di luar Jam → `next()`. Selama Jam: no-op.
- Pill Like/Dislike di layar detail kini juga menulis
  `setDislikedTrack` (suka menghapus dari daftar tidak-suka;
  tidak-suka menyimpan track lengkapnya) + memanggil
  `onTrackDisliked` saat baru tidak disukai.
- Library: `CollectionView.Disliked` + baris "Disliked songs" di
  bawah grid 2×2 (jumlah segar dari store) + tampilan detail
  (cari + urut Newest/Title/Artist + tombol "Restore" per baris,
  cermin tombol "Delete" di Offline) + "Play all" memainkan daftar
  yang sedang tampil (niat eksplisit).

### 2.6 Info lagu + rincian kegagalan
- `ResolveTrace.kt` (murni): `ResolveAttempt` (langkah + hasil)
  + `toDisplayLines()`.
- `YoutubeMusicApi.resolveStreamDetailed()`: rantai yang SAMA
  persis (Go → Kotlin client chain → retry visitor baru), tiap
  langkah dicatat apa adanya (pesan gagal asli, format/itag/bitrate
  saat sukses, "New visitor ID" saat retry). `resolveStream()`
  menjadi pembungkus `.result` — pemanggil lama tidak berubah.
  `YtmStream` mendapat `bitrate` (0 = tidak dilaporkan, tidak
  pernah dikarang). `lookupVideo`/`VideoMeta` mendapat `isLive`
  (dari `isLiveContent`).
- `PlayerManager`: single-flight kini memakai
  `sharedResolveDetailed` (map Deferred<ResolveOutcome>);
  `sharedResolve` mendelegasikan ke `.result` (prefetch Jam tidak
  berubah). `_resolveTrace` + `_currentStream` di-reset tiap
  `startTrack`, diisi saat resolve selesai (sukses/gagal). Stream
  sukses menandai `isLive` pada track hasil resolve.
- UI: dialog `TrackInfoDialog` (dipakai aksi "Track info" di sheet
  DAN tombol "Details" di kotak error layar detail): sumber,
  videoId, stream (mime/itag/bitrate, "Live stream (HLS)", atau
  "No direct stream — playing via the YouTube player"), dan
  "How the stream was found" / "What was tried, in order" dari
  jejak. Lagu yang belum diputar: "Stream details appear once
  this song plays." — bukan karangan.

### 2.7 SponsorBlock
- `SponsorBlockApi` (GET sponsor.ajay.app, timeout 6 dtk): 404
  atau gagal apa pun = daftar kosong (best-effort, tidak pernah
  memblokir putar). `SponsorBlockPolicy.kt` (murni): parse bentuk
  respons yang didokumentasikan, kategori yang dipakai hanya
  sponsor/intro/outro/selfpromo/music_offtopic/interaction,
  `decideSkip` (posisi di dalam segmen → ujung segmen; tidak
  pernah mundur).
- `PlayerManager`: fetch per lagu (paralel dengan resolve, hanya
  bila toggle nyala + ada videoId); cek di loop polling ExoPlayer
  100ms; lompatan menyimpan `SkipUndo` (posisi sebelum lompat,
  kedaluwarsa 6 detik via handler); `undoLastSkip()` kembali ke
  posisi sebelum lompatan; `segments` diekspos untuk penanda wave.
- UI: toggle "Skip sponsored parts" di Settings → Playback
  (default nyala, cermin baris Gapless; `LocalStore`
  `skipSponsoredSegments`). `ArusWaveProgress` mendapat param
  opsional `markers` (fraksi 0f..1f) — tick digambar SETELAH
  clipRect isian, batas progress tidak pernah bergeser. Chip Undo
  sementara di layar detail ("Skipped a sponsored part" + "Undo").

### 2.8 Live
- `Track.isLive` (dari resolve `stream.isLive` atau
  `lookupVideo.isLiveContent`). Layar detail: label kanan baris
  waktu = "LIVE" (mono) bila live; `onSeek = null` mematikan
  gesture wave (siaran tidak bisa di-seek); posisi elapsed tetap
  tampil. Tidak ada perubahan jalur putar (HLS sudah ditangani
  `playExo` sejak lama).

## 3. Verifikasi

- Harness148 (reproduce-first, skill reproduce_first_verification):
  baseline di tree v1.0.2 = 21 FAIL + guard lolos (reproduksi);
  setelah implementasi = 26/26 PASS (R1–R22 + G1–G4), termasuk
  45 tes Kotlin murni (T01–T45) dari fixture respons live.
  Satu koreksi checker yang jujur: R8 awalnya memeriksa kunci
  panel di YoutubeMusicApi — parsing memang tinggal di
  RadioParser.kt; checker diperbaiki, bukan kodenya.
- G1: tabel token tema byte-identik dengan baseline v1.0.2
  (MD5 2480bca8703269751d32bfa3688cc488). G2: nol string
  "spotify" di tree. G3: tidak ada label tab "Video". G4: bentuk
  START_STICKY PlaybackService utuh.
- Regresi spot-check: h147 (background) ALL SOURCE CHECKS PASS; h146
  15/15 PASS; h130 79 checks, 0 failures. h118 dijalankan sekali di
  tree v1.0.3: mode dormant 5 failures (F1/F2/F3×2/F4 — sesuai
  desain, watchdog dorman HARUS gagal); mode normal 1 failure:
  F4 "buffering-stall recovers" (calls=2 base=1 buffering=false) —
  F4 adalah flake yang dikenal (di v1.0.2: gagal di loop, lulus di
  isolated re-run); F1/F2/F3/F5/F6 lulus di run ini. F2 TIDAK
  dinyatakan fixed — investigasi watchdog h118 tetap terbuka.
  TIDAK ada sentuhan watchdog. Satu penyesuaian harness: stub
  FakePlayerManager h118 mendapat `clearRadio()` no-op (cermin
  metode PlayerManager baru yang dipanggil startSession) supaya
  harness tetap terkompilasi; perilaku Jam harness tidak berubah.
- Build bersih via build-manual/build.sh: 0 error. Peringatan
  Kotlin: hanya peringatan tooling yang sudah ada sebelumnya
  (kotlin-stdlib/-reflect/-script-runtime tidak ditemukan di
  Kotlin home + peringatan Jansi/Unsafe) — 0 peringatan BARU dari
  kode v1.0.3. aapt2: com.sonara.app vc4/1.0.3. apksigner: v2+v3,
  1 signer, sertifikat debug yang sama.
- Backup v1.0.2: disalin hash-terverifikasi SEBELUM build
  (MD5 10a19270a11a2ab3aefd8587eb809d35) ke
  backups/sonara-debug-v1.0.2-vc3.apk (MD5 sama, terverifikasi).

## 4. Artefak

- APK: sonara-debug-v1.0.3.apk — ukuran: 64.864.221 byte,
  MD5: efb6e1aea8ed5dfd8e6862aa57ab4cca,
  SHA-256: e8eb1b8de1630572bc7c035496bf46382ac67131b26634131ed309f3c76de1bf.
  aapt2: com.sonara.app vc4/1.0.3. apksigner: v2+v3, 1 signer,
  sertifikat debug sama (SHA-256 e83b1d93…c5da06).
- Harness: harness148/ (check_148.py, Harness148.kt, run.sh,
  fixtures/ dari respons live 25 Nov 2026).

## 5. Batas jujur + yang TIDAK dibangun

- Label "made with AI": UNAVAILABLE signed-out. Respons search dan
  player yang di-probe dari VM tidak membawa badge, field, atau
  token apa pun yang menandai konten buatan AI (nol
  musicInlineBadgeRenderer, nol "synthetic"/"altered"). Membangun
  fitur ini berarti mengarang label — tidak dibangun. Bila YTM
  signed-out suatu saat membawa labelnya, parser + UI-nya tinggal
  ditambah; tidak ada kode pura-pura di tree ini.
- SponsorBlock: API terjangkau, tetapi TIDAK ADA payload segmen
  sungguhan yang berhasil diambil dari VM (semua videoId yang
  di-probe → 404 "Not Found"). Perilaku skip/undo/marker dibuktikan
  harness dari bentuk respons yang didokumentasikan + tes kebijakan
  murni; perilaku di HP (video yang benar-benar punya segmen)
  adalah pembuktian akhirnya. Bila API tidak terjangkau dari HP,
  fitur ini diam-diam tidak melakukan apa-apa (daftar kosong).
- Saran entitas (ketuk = langsung putar lagu spesifik): tidak ada
  satupun yang dikembalikan signed-out di probe; dukungannya ada
  di parser secara defensif, tetapi belum terbukti live.
- Filter chip hasil cari (9 chip di respons): di luar scope batch
  ini; tidak dibangun.
- Highlight "jump-to-highlight": tidak ada endpoint/highlight
  sungguhan yang terbukti di probe; yang dibangun adalah penanda
  segmen SponsorBlock di seek bar + lompatan skip — bukan klaim
  highlight.
- Semua angka hitungan/tahun di UI berasal dari respons; tanpa
  data = tidak tampil (bukan 0, bukan tebakan).

## 6. Aturan yang dikunci (untuk batch berikutnya)

- Radio TIDAK PERNAH jalan selama Jam: `clearRadio()` di
  `JamSessionManager.startSession()` (satu-satunya titik aktivasi)
  + aksi radio disembunyikan selama sesi + `next()` bercabang
  Jam-vs-bukan. Batch berikutnya (lirik, podcast, dst.) tidak
  boleh mengendurkan gerbang ini tanpa perintah eksplisit.
- Lagu user SELALU menang atas radio; radio TIDAK PERNAH masuk
  `_queue` atau `saveQueue`. Antrean radio bersifat ephemeral.
- Daftar tidak disukai bersifat LOKAL SAJA (tidak disinkron);
  `setDislikedTrack` sengaja tidak memicu `onLibraryChanged`.
- Jejak resolve TIDAK PERNAH mengarang diagnostik: langkah yang
  tidak dijalankan tidak dicatat; pesan gagal memakai pesan asli
  jalurnya.

## 7. Langkah cek di HP
0) About menunjukkan SONARA v1.0.3 (4).
1) Saran muncul saat mengetik; ketuk saran lagu langsung putar.
2) Urutan mengubah urutan hasil; hitungan putar terlihat bila
   datanya nyata.
3) Link YTM yang ditempel + Enter langsung putar/buka.
4) Start radio: lagu-lagu mirip mengalir, ditandai terpisah di
   seksi Radio; lagu antrean user tetap menang.
5) Dislike lagu → dilewati saat maju + terdaftar di Library →
   Disliked; Restore mengembalikannya.
6) Track info menampilkan format/sumber nyata; lagu yang gagal
   menjelaskan apa yang dicoba (tombol Details di kotak error).
7) Jam/lirik/tema/sinkron tidak berubah; background playback
   v1.0.2 tidak berubah.
