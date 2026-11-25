# BUILD v1.0.1 (versionCode 2) — SONARA

Tanggal: 25 Nov 2026 (WIB). Satu batch, tiga scope atas perintah user
("no separate v1.0.2; previous queue plan cancelled"):

1. Tombol play/pause mini player hilang di HP.
2. Paragraf MixMusic dihapus sepenuhnya dari About (mesin Go dibiarkan).
3. Artwork HD untuk SEMUA artwork lagu + audit 14 varian (7 tema ×
   Light/Dark) untuk scope 1.

## 1. Ground truth: screenshot user

File: `~/workspace/user/files/1000950303_43_6h0n.jpg` — Home, ARUS
Light, lagu "Teh Hijau" (Tulus) di mini player, lingkaran merah user
di sekitar area prev/next.

Pengukuran piksel (25 Nov 02:31 WIB):

- Pita kapsul mini player: y ≈ 1730–1875, warna #1B1813 (StageBg
  ARUS Light — cocok dengan tabel token, lihat §3).
- Ikon prev (SkipPrevious), next (SkipNext), dan heart tampil putih.
- Celah x ≈ 600–775 seragam #1B1813 — TIDAK ada lingkaran, TIDAK
  ada ikon. Ruang 44dp untuk tombol ADA, isinya hilang total.
- Sensus warna 2D pita kapsul: hanya warna pita + ikon putih +
  goresan merah user — NOL piksel oranye (#FF4E24) di area tombol.

## 2. MiniPlayer: struktur benar, token benar, penyebab tak terbukti

Kode pra-perbaikan (`SonaraApp.kt` ~1545, `MiniPlayer`): Row → cover +
judul/artis (weighted) + Like + Previous + `BouncyBox`
(`ArusMotion.kt:154`, scale 1.0 saat diam) ukuran
`BottomCluster.playerAction` (44dp), `CircleShape`, background
`AccentFill`, ikon PlayArrow/Pause tint `OnAccent` 24dp + Next.
Struktur terverifikasi benar; tidak ada shadowing (satu definisi
`AccentFill`, import ikon Material asli, satu `BouncyBox`); build
membersihkan classes (`build.sh:39`) dan source mendahului APK
terkirim, jadi build terkirim cocok dengan source.

Teori token DIBANTAH oleh dump Kotlin dari `paletteFor` ASLI untuk
ke-14 varian (harness146): ARUS Light accentFill #FF4E24, onAccent
#1B1813, stage #1B1813 (background screenshot #FFF9ED + pita #1B1813
cocok persis ARUS Light). Kontras cakram-vs-pita 5.37, ikon-vs-cakram
5.37 — lolos. Dump ini BUKAN dari formula perbaikan: implementasi
Python independen dari tabel yang di-dump.

Eksperimen kontrol di screenshot YANG SAMA: cakram play Community
Picks (`HomeSearch.kt:892`, konstruksi Box + clip + background +
ikon yang identik) tampil #FF4E24 dengan segitiga gelap. `BigPlayButton`
detail (`UiCommon.kt:1219`, pola BouncyBox + clip + background yang
sama) tampil di detail screen menurut riwayat HP user. Nav `BouncyBox`
tampil normal.

Satu-satunya elemen yang belum terbukti: pembungkus BouncyBox
(scale-graphicsLayer) yang menaruh clip + background di dalam
graphicsLayer, di dalam Row yang juga punya pointerInput drag +
clickable + clip + background StageBg. Tidak ada reproduksi on-device
(tanpa emulator/KVM). PENYEBAB AKAR TIDAK TERBUKTI — dinyatakan
jujur; cek HP adalah arbiter final.

Perbaikan (scope tepat, tidak lebih):

- BouncyBox dilepas, diganti `Box` biasa + ukuran/clip/background/
  ikon + `clickable` yang sama (onClickLabel Play/Pause, getar tap
  via `rememberHaptic()`, lalu `onToggle()`). 44dp/24dp/warna/
  posisi/urutan Row sama. Umpan balik tekan berubah bounce → ripple
  (dicatat jujur). `MorphPlayPauseIcon` detail (`UiCommon.kt:1162`)
  TIDAK disentuh.
- Warna cakram kini dari pasangan [playDiscPair] (lihat §3).

## 3. Pasangan warna cakram + audit 14 varian

Audit harness146 menemukan DUA cacat kelas yang sama (cakram di atas
kapsul GELAP harus kontras terhadap kapsul, bukan terhadap
background tema):

- RASI/LIGHT: accentFill #3157D5 vs stage #101A2E = 2.86 (< 3.0).
  Ikon-vs-cakram 6.08 OK.
- SENYAP/LIGHT + SENYAP/DARK: accentFill == warna kapsul persis
  (Light #111110 == text; Dark #FAF8F3 == text, karena kapsul
  flatNoCards = `text`) → kontras 1.00. Ikon 17.80 tapi cakramnya
  tak terlihat.
- PASAR/LIGHT 3.19 lolos tapi tipis — dibiarkan (aturan baru
  memperbaikinya juga, lihat bawah).

Aturan `playDiscPair(p: AppThemePalette)` di `AppTheme.kt` (murni,
dari token yang ada, tanpa warna baru), disimpan sebagai
`playDiscFill`/`playDiscTint` di `SonaraTokens` via `toTokens()`:

- `flatNoCards` (SENYAP): cakram = `background`, ikon = `text`.
- Bila `accentOnDark != accentFill` (RASI Light, PASAR Light):
  cakram = `accentOnDark` (aksen untuk permukaan gelap).
- Ikon = `onAccent`, kecuali kontrasnya terhadap cakram < 3.0
  (RASI Light: putih di #8AB4FF hanya 2.09) → pakai `text`.
- Sisanya: (accentFill, onAccent) — tidak berubah.

Hasil harness146 pasca-perbaikan (kontras WCAG, implementasi Python
independen; ukuran 44/24/54 di semua varian):

| Varian | cakram-vs-pita | ikon-vs-cakram | discFill | discTint |
|---|---|---|---|---|
| ARUS/LIGHT | 5.37 | 5.37 | #FF4E24 | #1B1813 |
| ARUS/DARK | 4.84 | 5.37 | #FF4E24 | #1B1813 |
| SENJA/LIGHT | 10.32 | 10.59 | #FFB347 | #171006 |
| SENJA/DARK | 8.35 | 10.59 | #FFB347 | #171006 |
| BLOK/LIGHT | 5.41 | 5.41 | #FF3D68 | #131313 |
| BLOK/DARK | 4.70 | 5.41 | #FF3D68 | #131313 |
| GETAR/LIGHT | 8.40 | 8.40 | #FF8A7A | #0E0E14 |
| GETAR/DARK | 7.74 | 8.40 | #FF8A7A | #0E0E14 |
| RASI/LIGHT | 8.31 | 8.31 | #8AB4FF | #101A2E |
| RASI/DARK | 8.13 | 9.21 | #8AB4FF | #070D24 |
| PASAR/LIGHT | 5.17 | 3.55 | #FF3B30 | #FFFFFF |
| PASAR/DARK | 4.45 | 4.99 | #FF3B30 | #1A1816 |
| SENYAP/LIGHT | 17.80 | 17.80 | #FAF8F3 | #111110 |
| SENYAP/DARK | 17.80 | 17.80 | #111110 | #FAF8F3 |

Hanya 4 varian berubah (RASI Light, PASAR Light isi saja, SENYAP
Light/Dark); 10 varian lain pasangan == (accentFill, onAccent)
identik — ditegaskan harness (S2b).

## 4. About: MixMusic dihapus dari UI, mesin Go dibiarkan

Perintah user 25 Nov 02:30 WIB: "hapus paragraph mixmusic di about
sepenuhnya biarin mesin Go-nya". Bukti mesin memang ter-bundle
(sebelum menjawab user): `mixmusic.aar` → `libgojni.so`
(32.857.240 byte, arm64), `GoEngine.kt` memanggilnya PERTAMA untuk
search + stream, build script GAGAL tanpa aar
(`build.sh:365`). User tetap memerintahkan hapus dari UI setelah
diberi tahu — keputusan dicatat apa adanya.

Dihapus dari `SettingsAbout.kt`:

- (a) bullet "• YouTube Music engine and streams: original MixMusic
  Go code (fzn0x/mixmusic, MIT license) ported via gomobile; the UI
  stays Kotlin. If the Go engine fails to load, the app automatically
  uses the Kotlin path." — seluruh bullet.
- (b) baris `Text("MixMusic (fzn0x/mixmusic, MIT)")` di bawah label
  versi.

TIDAK disentuh: `GoEngine.kt`, `YoutubeMusicApi.kt`, build script,
`libgojni.so`, komentar kode pendokumentasi asal mesin. Bullet About
lain byte-identik. Atribusi dipindah ke file proyek
`THIRD-PARTY-NOTICES.md` (MixMusic MIT + komponen Apache-2.0 + font
OFL; REST-only Firebase) — tanpa UI user-visible.

Verifikasi artefak: `resources.arsc` NOL "mixmusic" (case-insensitive).
Dex: 1 hit di `classes3.dex` — konstanta internal binding gomobile
`mixmusic-engine 0.5.12 (fzn0x/mixmusic, MIT)`, BUKAN string
user-visible, tidak dibaca kode UI manapun; menghapusnya butuh
menyentuh mesin (dilarang perintah user). Komentar kode Kotlin
(`GoEngine.kt:8`, `YoutubeMusicApi.kt:20`) dikecualikan sesuai brief.

## 5. Artwork HD

### 5.1 Audit sumber (sebelum perubahan)

| Sumber | Seleksi lama | Kelemahan |
|---|---|---|
| Feed `parseItem` (`YoutubeMusicApi.kt:925`) | elemen TERAKHIR array | rapuh bila urutan berubah |
| Detail fetch (`:395`) | loop lebar-terbaik | sudah terbaik, tanpa upgrade ukuran |
| `GoEngine.parseImage` | loop lebar-terbaik | sama |
| `ArtistApi.largestThumb` | elemen terakhir | rapuh |
| `GenreApi.largestThumb` | elemen terakhir | rapuh |
| Mini player + detail + Jam orbit + widget + kartu Library | berbagi `Track.imageUrl` | ikut sumber di atas |
| Kartu share lirik | text-only (`LyricCard.renderBitmap`) | N/A — tanpa artwork |
| Player `VideoMeta.imageUrl` | DIBUANG (hanya durasi dipakai) | wiring player-art dilewati (lihat §5.4) |

Tidak ada bentuk thumbnail-renderer lain atau manipulasi URL di tree.

### 5.2 Bukti live (probe, bukan tebakan)

- Fixture `harness131/fixtures/search_raw.json`: 31 array; 16
  puncaknya 120×120.
- Probe segar `harness146/fixtures/probe_search_20261125.json`
  (736 KB, query "Teh Hijau Tulus", HTTP 200, 33 array): array
  menaik (terakhir == maks di semua array), tapi 11/33 puncaknya
  120×120; lebar maks 120/400/544/1200.
- URL lh3/yt3 berbagi SATU id dasar, hanya akhiran `=wXX-hXX`
  yang berbeda (terbukti: id sama di 60/120/226/544). yt3 melayani
  sampai 1200 (terbukti); lh3 teramati hanya 60/120 (layanan gambar
  yang sama). 544 = ukuran PERSEGI terbesar yang TERBUKTI dilayani
  untuk id yang sama.
- Probe player (visionOS + WEB, videoId 8NS-4m3wicw): HTTP 200 tapi
  `LOGIN_REQUIRED` "Sign in to confirm you're not a bot" (IP
  datacenter VM; app di HP memakai visitorData) — TIDAK ada bukti
  thumbnail videoDetails dari sini.

### 5.3 Aturan + implementasi

File murni baru `data/ArtworkUrls.kt`: `ThumbCandidate`,
`pickLargestThumbUrl` (lebar maks; seri → terakhir; lebar ≤ 0 kalah
dari yang diketahui; semua tak diketahui → terakhir non-blank;
kosong → null), `upgradeThumbUrl` (HANYA URL lh3/yt3
`googleusercontent.com` yang sudah membawa `=wW-hH`, HANYA persegi
w == h, HANYA w < 544 dan w > 0 → `=w544-h544` dengan akhiran
dipertahankan; selain itu tak disentuh — termasuk i.ytimg,
radioart, non-persegi, ≥ 544), `selectArtworkUrl` = pick + upgrade.
Dipasang di 5 situs (parseItem, detail fetch, kedua largestThumb,
parseImage); fallback hqdefault dipertahankan. Komentar basi di
parseItem ("thumbnail pertama") diperbaiki.

Per-surface caps: Coil tetap downsampling ke ukuran view (widget
`.size(256, 256)`); tidak ada dependensi/izin baru; tidak ada URL
karangan; tidak ada upscale. Sebelum/sesudah per sumber: array yang
puncaknya 120 → 544 (bila persegi & lh3/yt3); array 400 → 544;
array 544/1200 → tetap (1200 non-persegi/tidak memenuhi syarat
upgrade — tidak dipaksa).

### 5.4 Opsi lanjutan (tidak dibangun)

Wiring `VideoMeta.imageUrl` dari player response: `TrackDurations`
mengembalikan `Result<Long>` dan hanya durasi yang dipakai; wiring
butuh perubahan signature + persistensi — terlalu invasif untuk
build ini. Dicatat sebagai opsi follow-up, bukan janji.

## 6. Harness146 (reproduce-first)

- Pra-perbaikan: 1/12 PASS (S1, S3, S4a–f, S5 gagal; kompilasi
  Kotlin gagal — ArtworkUrls.kt belum ada). Reproduksi terkonfirmasi.
- Pasca-perbaikan: 15/15 PASS (Kotlin fixtures 19/19 incl. T15–T19
  pasangan cakram; S1–S5).
- Koreksi checker selama build (didokumentasi, bukan dibengkokkan):
  S1 semula menghitung kata "BouncyBox" termasuk komentar penjelas —
  diperbaiki menghitung pemanggilan `BouncyBox(` saja (0 di
  MiniPlayer). S2 semula memeriksa accentFill/onAccent mentah —
  diperbaiki memeriksa pasangan TERPILIH `playDiscPair` + menegaskan
  hanya 4 varian berubah (S2b) + wiring token (S2c/S2d). S2 sempat
  menuntut 14 varian ter-dump (vacuous-pass diperbaiki).

## 7. Regresi spot-check

- h127: 1 FAIL — SAMA dengan log baseline tersimpan (RASI/LIGHT
  outline/bg 1.23 < 1.3, pre-existing, token outline di luar scope).
- h128: GUARDS ALL PASS.
- h130: 79 checks, 0 failures.
- h132: nav ratio checks PASS; 1 FAILURE = md5 `AppTheme.kt` beda
  dari baseline harness — wajar (file mendapat `playDiscPair` +
  helper kontras); baseline belum di-rebase di build ini.
- h137: ALL PASS. h139: CHECK139 ALL PASS (measure_text.py tidak
  teramati di run ini — fixture-blocked yang sudah dikenal).
- h140: ALL CHECKS PASS.
- h118: ALL PASS pada single-loop run ini. Investigasi watchdog h118
  (F2) TETAP TERBUKA — tidak dinyatakan fixed; riwayat suite tidak
  stabil (lihat BUILD sebelumnya). Jalur watchdog TIDAK disentuh.

## 8. Verifikasi build

- Backup: `sonara-debug-v1.0.apk` → `backups/sonara-debug-v1.0-vc1.apk`,
  hash backup terverifikasi MD5 `0934b8fbc38d63204017a3c3ef9229bf`
  (sama dengan APK sebelum overwrite).
- Clean rebuild `build-manual/build.sh` (membersihkan dulu), exit 0.
- aapt2: `package: name='com.sonara.app' versionCode='2'
  versionName='1.0.1'`, minSdk 26, targetSdk 34.
- apksigner: v2 + v3 true; sertifikat debug SAMA dengan v1.0
  (SHA-256 `E83B1D93...C5DA06`, cocok dengan fingerprint Firebase
  terdaftar).
- Warnings Kotlin: jenis sama dengan v1.0, jumlah 39 → 36 (3 situs
  mismatch hilang karena `ThumbCandidate` bertipe) — NOL warning baru.
- Artefak: `sonara-debug-v1.0.1.apk`, 64.733.149 byte,
  MD5 `c83a9d17ec9a1d037dce812f65d03da8`,
  SHA-256 `a5ed9a495756285175cb24b5956d63ab5766e4c3c1f6ab8126efddf0642d22a8`.

## 9. Langkah cek HP

0. About menampilkan **SONARA v1.0.1 (2)**.
1. Mini player di semua 7 tema × light/dark: lingkaran play + ikon
   terlihat jelas di samping prev/next, tap mengubah play/pause,
   ikon berganti Play↔Pause.
2. Play/pause morph di detail screen tidak berubah.
3. About: tidak ada paragraf/kredit MixMusic; perilaku mesin Go
   tidak berubah.
4. Artwork tampak lebih tajam, terutama cover detail + baris yang
   sebelumnya 120px.
5. Jam/lirik/komentar/feed/sinkronisasi tidak berubah.

## 10. File yang diubah

- `ui/SonaraApp.kt` — MiniPlayer: Box ganti BouncyBox, warna cakram
  dari `CurrentTokens.playDiscFill/playDiscTint`.
- `ui/theme/Theme.kt` — field `playDiscFill/playDiscTint` di
  `SonaraTokens` + pemetaan via `playDiscPair`.
- `data/AppTheme.kt` — `relativeLuminance`, `contrastRatio`,
  `playDiscPair`.
- `data/ArtworkUrls.kt` (baru) — seleksi + upgrade artwork.
- `data/YoutubeMusicApi.kt`, `data/ArtistApi.kt`,
  `data/GenreApi.kt`, `data/GoEngine.kt` — 5 situs memakai
  `selectArtworkUrl`.
- `ui/screens/SettingsAbout.kt` — hapus bullet + kredit MixMusic.
- `THIRD-PARTY-NOTICES.md` (baru) — atribusi proyek saja.
- `DESIGN-SPEC.md` §5.60. `harness146/` — dump pasangan cakram +
  fixture T15–T19 + checker S1/S2/S2b–d.
