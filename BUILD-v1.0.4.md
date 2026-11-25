# BUILD v1.0.4 (versionCode 5) — SONARA

Tanggal: 25 Nov 2026 (WIB). Batch 2 dari perintah user "bikin semua"
(09:45 WIB): celah MixMusic — lirik. Setiap jalur data di-probe
live/signed-out dari VM DULU; hanya yang terbukti dibawa datanya
yang dibangun. Tidak ada angka, label, terjemahan, atau lirik
karangan.

## 0. Ringkasan fitur

1. Cari di lirik: kolom cari di tab LYRICS; baris hasil disorot
   (hasil saat ini lebih kuat), tombol naik/turun melingkar,
   Enter = lompat; lompat memakai rumus pemusatan yang sama dengan
   follow (bukan scroll mentah).
2. Edit lirik: satu field per baris, timestamp dipertahankan,
   baris yang dikosongkan dibuang, timing per-kata dibuang hanya
   pada baris yang teksnya berubah. Tersimpan LOKAL per lagu
   (maks 100 lagu), tidak disinkron; "Reset to fetched"
   mengembalikan hasil fetch; penanda "Edited" di panel + caption.
3. Offset timing: per-lagu ±10 detik (langkah 0,5 detik, dialog
   di panel) + global ±10 detik (slider Settings → Playback).
   Posisi efektif = posisi putar + global + per-lagu; hanya
   menggeser baris aktif + highlight kata, tidak mengubah pemutar.
4. Pemilih sumber lirik: Auto (LRCLIB sinkron → YTM → LRCLIB
   polos, persis urutan lama), LRCLIB saja, atau YouTube Music
   saja (polos). Pilihan eksplisit tidak jatuh ke sumber lain.
5. Lirik fullscreen: daftar sendiri dengan follow memakai rumus
   pemusatan yang sama (centerScrollOffset + isNearCenter, satu
   koreksi), gaya Standard (22/28sp) / Large (30/38sp), warna
   token stage detail yang sama di semua 14 varian tema.
6. TIDAK DIBANGUN (jujur, lihat §1): terjemahan per-baris,
   romaji, lirik-dari-caption YouTube. Highlight per-kata SUDAH
   ada sejak v1.6 (render baris aktif dari timing kata ASLI
   lyricsfile LRCLIB) dan tidak diubah.

Tidak berubah: tema/token (14 varian, byte-identical), rumus
pemusatan + kebijakan versi lirik (tidak disentuh), Jam, sleep
timer, komentar, feed, mesin Go, sinkron, notifikasi/widget, kode
background-playback v1.0.2, fitur v1.0.3. Tidak ada izin baru.
Tidak ada jalur Spotify, tidak ada sign-in YTM.

## 1. Bukti probe live (25 Nov 2026, dari VM; fixture di harness149/fixtures/)

- LRCLIB `/api/get` (Blinding Lights, durasi 200): 200, field
  `hasWordSync: true`, `lyricsfile` terisi, `syncedLyrics` 37
  baris. Union key: albumName, artistName, duration, hasWordSync,
  id, instrumental, lyricsfile, name, plainLyrics, syncedLyrics,
  trackName — TIDAK ADA `translatedLyrics`/`romanizedLyrics`.
- LRCLIB `/api/search` (Lemon 20 entri, Spring Day 20,
  Mencintaimu 19, Idol 20 = 79 entri + 1 get = 80): union key
  IDENTIK, tanpa field terjemahan/romaji apa pun, untuk lagu
  EN/JP/KR/ID. `hasWordSync: true` untuk Lemon, Spring Day, dan
  sebagian Idol; Mencintaimu `hasWordSync: false` (tetap punya
  `lyricsfile` — jalur app hanya memakainya bila hasWordSync,
  perilaku lama dipertahankan).
- YouTube timedtext: `GET /api/timedtext?v=<id>&lang=<en|ja>&kind=asr&fmt=json3`
  untuk 4 videoId hasil search WEB_REMIX → HTTP 200, body KOSONG
  (0 byte) untuk semua bahasa; varian `tlang=` → HTTP 429.
- Respons player InnerTube (WEB, Blinding Lights official video,
  12.451 byte): TIDAK mengandung `captionTracks`.
- Kesimpulan: tidak ada sumber signed-out yang membawa terjemahan,
  romaji, atau caption untuk konten ini. Membangun ketiganya
  berarti mengarang — dilarang aturan proyek. Dicatat di sini,
  bukan disembunyikan.

## 2. Perubahan kode

Baru:

- `data/LyricsTools.kt` (murni): `LyricsOffsetPolicy`
  (effectivePositionMs, clamp ±10 dtk, langkah 0,5 dtk,
  offsetLabel), `LyricsFindPolicy` (findMatches substring tak
  peduli kapital, nextMatch melingkar), `LyricsEdit` +
  `LyricsEditCodec` (JSON round-trip termasuk timing kata +
  endMs; `applyEdit` mempertahankan timestamp, membuang baris
  kosong, membuang timing kata hanya pada baris yang berubah),
  `LyricsOffsetsCodec` (peta {trackId: ms}), `LyricsSourceOrder`
  (urutan coba per preferensi), `LyricsStyle`
  (STANDARD 22/28, LARGE 30/38), `hasWordTimings`.
- `harness149/` (check_149.py + Harness149.kt + run.sh +
  fixtures/): 23 source check + 35 fixture Kotlin.

Diubah:

- `data/LyricsApi.kt`: enum `LyricsSource` (AUTO/LRCLIB/
  YOUTUBE_MUSIC); `fetchLyrics(..., source = AUTO)` dispatcher;
  rantai LRCLIB dipindah ke `fetchFromLrclib` (perilaku identik:
  sinkron → polos); YTM dipindah ke `fetchFromYtm`. AUTO =
  LRCLIB sinkron → YTM → LRCLIB polos (urutan lama, dibuktikan
  dari kode lama sebelum refactor). `parseLyricsFile`/`parseLrc`
  tidak disentuh.
- `data/LocalStore.kt`: `lyricsEdit/saveLyricsEdit/
  clearLyricsEdit` (key `lyrics_edits_v1`, cap 100),
  `lyricsOffsetMs/setLyricsOffsetMs` (key `lyrics_offsets_v1`),
  `globalLyricsOffsetMs` (key `lyrics_offset_global_ms`),
  `lyricsSourcePref` (key `lyrics_source_pref`, default AUTO).
- `ui/screens/SettingsAbout.kt`:
  - Detail: state preferensi sumber + `lyricsFetchedSource`
    (ganti sumber = ambil-ulang); override edit lokal menang
    atas fetch; posisi efektif (global + per-lagu) dipakai
    baris aktif + highlight kata; `LyricsPanel` menerima
    track/store/preferensi/callback; dialog fullscreen
    dipanggil dari state layar.
  - `LyricsPanel`: tombol Find/Edit/Fullscreen/Offset + tombol
    kartu lirik (tetap); baris sumber ("Source: …") + penanda
    "Edited" + label offset; baris cari (hitung hasil,
    naik/turun, IME Search); sorot hasil cari; caption footer
    menyebut status edit; 3 dialog + fullscreen dipanggil dari
    panel. Render baris, follow-hold, dan worker follow TIDAK
    disentuh.
  - Dialog baru: `LyricsEditDialog` (field per baris + label
    timestamp, Reset to fetched bila sudah diedit),
    `LyricsOffsetDialog` (slider ±10 dtk langkah 0,5 dtk +
    konteks offset global), `LyricsSourceDialog` (3 radio +
    penjelasan), `LyricsFullscreenDialog` (daftar sendiri,
    follow rumus sama + 1 koreksi, gaya Standard/Large,
    BackHandler menutup).
  - Settings → Playback: slider "Lyrics timing offset" global
    (±10 dtk, langkah 0,5 dtk) setelah baris SponsorBlock.
- `build-manual/build.sh`: `--version-code 5 --version-name 1.0.4`.
- `DESIGN-SPEC.md`: §5.63.

Tidak disentuh: `data/LyricsFollow.kt` (md5 sama),
`data/AppTheme.kt` (md5 sama), `LyricsVersionPolicy`,
`AndroidManifest.xml` (8 uses-permission, sama).

Koreksi checker selama build (jujur, dicatat):

- Harness149 F6e: ekspektasi awal memakai posisi match 3 untuk
  daftar 3 match (di luar jangkauan) — diperbaiki ke posisi
  terakhir yang valid (2). Bukan cacat kode.
- Harness149 F7d/F7f: ekspektasi awal salah membaca applyEdit
  (baris kosong dibuang → ukuran 1, bukan 2) — diperbaiki;
  ditambah cek baris tak berubah mempertahankan timing kata.
- Harness149 F7a: codec awal tidak menyimpan timing kata —
  DITEMUKAN oleh harness; codec diperbaiki menyimpan words +
  endMs sebelum build (fidelity edit, bukan kosmetik).
- Harness149 run.sh: path jar org.json dari transforms cache
  kosong di VM ini (cache dipindah) — dialihkan ke path
  modules-2, pola harness139. Bukan perubahan perilaku.
- Build: 2 warning baru sempat muncul (optString null di
  LocalStore, `t != null` redundan di fullscreen) — keduanya
  diperbaiki di kode, build final kembali ke 59 warning
  (baseline v1.0.3, semua pre-existing).

## 3. Verifikasi

- Harness149 reproduce-first: di tree v1.0.3 yang belum diubah,
  17 R-check FAIL + compile fixture gagal (LyricsTools tidak
  ada). Setelah perbaikan: **23/23 source check + 35/35
  fixture Kotlin ALL PASS** (termasuk parse lyricsfile fixture
  asli → 37 baris + timing kata naik; 80 entri LRCLIB tanpa
  field terjemahan/romaji; player tanpa captionTracks).
- Regressions spot-check di tree v1.0.4:
  - h147 ALL PASS, h146 ALL PASS, h130 guards OK.
  - h148: source check 26/26 + fixture Kotlin 45/45 PASS —
    dengan CATATAN: `harness148/run.sh` apa adanya GAGAL compile
    di VM ini karena path jar org.json-nya (transforms cache)
    kosong; ini kerusakan harness-env yang pre-existing (bukan
    dari tree ini — tidak satu pun file yang dikompilasi h148
    disentuh build ini). Fixture dijalankan ulang dengan path
    jar yang diperbaiki (pola harness139) dan lulus penuh.
    harness148 TIDAK diubah.
  - h118 (satu loop run, tanpa sentuhan jalur watchdog):
    fase dormant 5 FAIL as designed (F1–F4 + setup, sesuai
    desain harness); fase live: F1/F2/F3/F4/F5/F6/F7 PASS,
    satu-satunya FAIL adalah setup "guest follows host"
    (guest=vidA playing=false) — BUKAN kasus watchdog. F2
    lulus di run ini, tetapi F2 TIDAK dinyatakan fixed:
    suite ini tidak stabil antar-run dan investigasi
    instrumented khusus tetap terbuka.
- Build: clean rebuild exit 0, 0 error, 59 warning (baseline
  v1.0.3, nol warning baru di file yang diubah). aapt2
  `com.sonara.app` vc5/1.0.4; apksigner v2+v3, 1 signer,
  sertifikat debug sama (SHA-256
  `E8:3B:1D:93:…:C5:DA:06`); string pool resources.arsc nol
  "mixmusic" (atribusi hanya di THIRD-PARTY-NOTICES.md +
  konstanta internal mesin Go, seperti sebelumnya).
- Backup v1.0.3 hash-verified sebelum overwrite
  (MD5 `efb6e1aea8ed5dfd8e6862aa57ab4cca` →
  `backups/sonara-debug-v1.0.3-vc4.apk`).

## 4. Artefak

- APK: `sonara-debug-v1.0.4.apk`, 64.995.293 byte,
  MD5 `ab5e0b3722ba8d67e51ccb9e68f6e563`,
  SHA-256 `9766b00f9faf0b03adc5d92b13cd17eeff62979b7bc1e8327f35e9029dfd74b8`.
- Dokumen ini + DESIGN-SPEC.md §5.63 + `harness149/`.

## 5. Batas jujur / yang harus dibuktikan di HP

- Terjemahan/romaji/caption memang tidak ada di build ini —
  itu keputusan berbasis probe, bukan kelalaian; bila user
  menemukan sumber signed-out yang membawanya, itu batch
  berikutnya.
- Offset + edit + sumber + fullscreen hanya dibuktikan
  harness + compile; perilaku visual (follow fullscreen,
  dialog edit di layar kecil, slider) hanya bisa dibuktikan
  di HP.
- Edit yang disimpan untuk lagu yang hasil fetch-nya berubah
  (ambil-ulang terverifikasi) tetap menang sampai di-reset —
  itu desain (edit lokal = otoritas), bukan bug.
- Lirik polos (kind PLAIN) tidak punya baris aktif — offset
  tidak berpengaruh visual padanya; itu jujur, bukan cacat.

## 6. Langkah cek di HP

0. About menunjukkan SONARA v1.0.4 (5).
1. Terjemahan/romaji TIDAK ADA di UI (tidak ada toggle-nya) —
   sesuai keputusan probe; jangan dicari.
2. Cari di lirik: ketik kata → hasil disorot, naik/turun
   melingkar, Enter melompat ke tengah.
3. Edit: ubah satu baris → tampil berubah + penanda "Edited";
   restart app → edit tetap; Reset to fetched → kembali ke
   lirik asli.
4. Offset: geser +2 detik untuk satu lagu → baris aktif
   bergeser; lagu lain tidak terpengaruh; offset global di
   Settings menggeser semua lagu.
5. Sumber: pilih "YouTube Music" → lirik jadi polos (atau
   kosong jujur bila tidak ada); kembali ke Auto → sinkron
   lagi.
6. Fullscreen: ikon fullscreen di tab LYRICS → layar penuh,
   baris aktif mengikuti + tetap di tengah, gaya Large
   membesar, Back menutup.
7. Jam/lirik-follow centering/tema/sinkron/background
   playback tidak berubah.
