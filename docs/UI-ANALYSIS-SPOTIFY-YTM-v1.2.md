# Analisis Layout / UI / UX / Assets — Spotify + YouTube Music → MIXTUNE v1.2

Dibuat: Sabtu 21 Nov 2026, 13:15 WIB — atas permintaan tuan di chat MIXTUNE
("coba lu analysis layout, ui, ux, assets, sprite, spotify dan yt music lalu terapkan semuanya ke mixtune",
13:08 WIB, setelah MIXTUNE v1.1 redesign selesai 12:32 WIB).

## Sumber yang dipakai

- **APK asli dari HP tuan, dianalisis REA 20 Nov 2026:**
  - Spotify **9.2.0.2053** — Kotlin, 108.190 kelas/15 dex. UI: **Encore design system +
    Compose hybrid**, tema `Theme.Glue`, **1.897 layout XML**, 3.952 resource XML,
    Compose (355 refs), Coil Compose, Lottie Compose, Rive runtime, Accompanist,
    Flexbox, Molecule runtime. Laporan: `~/workspace/tools/spotify/rea-analysis.md`.
  - YouTube Music **9.46.55** — native, 66.455 kelas/6 dex. UI **server-driven
    (Elements / EML)**: 215 file `assets/music_filegroup/_srs_resources_eml_bundle/`
    (komponen `animated_like_icon`, `bottom_sheet`, `chip_bar`, `avatar_stack`,
    `animated_subscribe_button`, …) — sebagian besar tampilan dikirim dari server,
    bukan dari kode aplikasi. Laporan: `~/workspace/tools/youtube-music/rea-analysis.md`.
- **Liputan UI terkini (web, dicek 21 Nov 2026):** redesign tablet Spotify 2026
  (sidebar + bottom nav Home/Search/Library/Create), YTM split-view Now Playing
  v9.14 Android / v9.15 iOS (2026), tweak Home feed YTM 2024 (chevron "More",
  judul carousel lebih kecil, deskripsi baris kedua dihapus), pola Home Spotify
  (sapaan per waktu + 6 shortcut + rak "Made for you").
- **Kode MIXTUNE v1.1 saat ini** (`~/workspace/mixtune-app/`): `ui/MixtuneApp.kt`
  (nav Beranda/Cari/Pustaka + PlayerBar), `ui/theme/Theme.kt` (satu aksen hijau
  #1DB954, bg #0A0A0F), `ui/UiCommon.kt` (CoverArt, TrackRow, Shelf, ShelfCard,
  SectionTitle, EmptyState), `ui/screens/HomeSearch.kt` (Home + Search),
  `ui/screens/QueueLibrary.kt` (Queue + Library),
  `ui/screens/SettingsAbout.kt` (Settings + NowPlaying).

## Batasan yang mengikat (jangan dilanggar)

1. **JANGAN menyalin aset proprietary.** Laporan REA Spotify tegas: no code, assets
   (font CircularSp, spoticon, Rive/Lottie art), endpoint, API key, atau brand
   material dari APK. Yang ditiru adalah **pola layout & interaksi**, dengan aset
   sendiri (ikon Material sudah dipakai v1.1, cover art asli dari API, gradasi
   dibuat dari cover via Palette).
2. **JANGAN menambah fitur palsu.** Tidak ada tab Podcast (tidak ada sumber podcast),
   tidak ada tombol Create/Blend (belum ada pembuatan playlist kolaboratif),
   tidak ada konten demo — aturan v1.1 tetap berlaku: kalau belum ada data nyata,
   tampilkan empty state yang jujur.
3. **Satu aksen hijau tetap.** v1.1 sudah diputuskan: hijau Spotify #1DB954 satu-satunya
   aksen; merah YTM hanya titik 6dp di label. Analisis ini TIDAK membatalkan itu —
   yang diambil dari YTM adalah struktur (rak, chip, split-view), bukan warnanya.
4. **UI YTM sebagian besar server-driven (EML).** Tidak bisa — dan tidak perlu —
   direplikasi 1:1 dari APK. Yang diterapkan adalah pola yang terlihat di layar,
   bukan komponen EML-nya.

## 1. Spotify — pola yang diambil

| Pola | Detail | Status di MIXTUNE v1.1 |
|---|---|---|
| Sapaan per waktu | "Selamat pagi/siang/sore/malam" di atas Home, konten berubah ikut jam | Ada `Type.greeting` 26sp tapi belum dipakai sebagai sapaan dinamis — **terapkan** |
| 6 shortcut di puncak Home | Grid 2 kolom × 3 baris: cover kecil + label, dari yang paling sering diputar / terakhir diputar | Belum ada — **terapkan** (data nyata: recent + liked) |
| Filter chips | Chip "Musik / Podcast" di bawah sapaan | Hanya "Musik" yang relevan (tanpa podcast) — **terapkan sebagai chip filter sumber: Semua / Spotify / YTM / Lokal**, gantikan tab teks di Cari |
| Rak "Made for you" + "Jump back in" | Rak horizontal cover besar + "Dengarkan lagi" (recent) | Rak ada (3 rak), tapi belum ada rak "Dengarkan lagi" dari riwayat nyata — **tambahkan** |
| Bottom nav | Home / Search / Library (+ Create di 2026) | Sudah Beranda/Cari/Pustaka — **pertahankan**, jangan tambah Create palsu |
| Mini player | Di atas nav: cover 48–56dp, judul + artis 1 baris, tombol play bulat, progress tipis | Sudah mirip (PlayerBar) — **rapikan**: tombol play jadi lingkaran putih isi hitam ala Spotify, atau tetap hijau penuh; progress 3dp tetap |
| Now Playing | Cover besar, judul/artis, slider progress, shuffle/repeat, tombol antrean, lirik, pilih perangkat | Ada cover 280dp + lirik — **tambahkan**: background gradasi dari warna cover (Palette, seperti Spotify yang mewarnai latar dari artwork), tombol antrean yang jelas, lirik sebagai sheet |
| Library | Baris pin + filter chip (Playlist/Artis/Album), sort | Pustaka masih daftar seksi — **terapkan chip filter + sort** |
| Motion & aset | Rive/Lottie untuk animasi mikro, Coil untuk cover + crossfade, Shimmer saat loading, mosaic cover untuk playlist campuran | Belum ada shimmer/animasi mikro — **terapkan**: skeleton shimmer saat rak loading, crossfade cover, hati like beranimasi (ala `animated_like_icon` YTM) |
| Tipografi | CircularSp Bold untuk judul, hierarki ketat | Pakai sistem v1.1 (judul 24sp, seksi 20sp bold, lagu 15sp, artis 13sp muted) — **jangan ganti font**, cukup terapkan hierarki secara konsisten |

## 2. YouTube Music — pola yang diambil

| Pola | Detail | Status di MIXTUNE v1.1 |
|---|---|---|
| Mood chips | Chip horizontal di bawah header: Energize, Feel good, Workout, Party, Podcasts | **Terapkan sebagai chip suasana di Beranda** — tapi isinya query nyata ke YTM (mis. "Workout", "Chill", "Fokus"), bukan label kosong |
| Speed dial 3×3 | Grid 3×3 tile (playlist/album/lagu yang disukai) di puncak Home | Belum ada — **gabungkan dengan shortcut Spotify**: satu grid 2×3 (6 tile) sudah cukup di layar HP, jangan 3×3 penuh |
| Quick picks | List vertikal: cover + judul + artis + menu ⋮, tombol "Play all" di rak | Sudah ada sebagai list vertikal — **tambahkan tombol "Putar semua" + menu ⋮ per lagu** (tambah ke antrean, simpan, bagikan) |
| Rak + chevron | Judul rak lebih kecil, tombol "More" berbentuk pil diganti chevron › | SectionTitle 20sp sudah — **ganti tombol "Lihat semua" (kalau ada) jadi chevron ›** |
| Bottom nav 2026 | Search pindah ke bottom nav | Sudah ada Cari di nav — **sesuai, pertahankan** |
| Mini player | Bar bawah: cover, judul, kontrol play | Sudah ada PlayerBar — **samakan**: judul 1 baris + artis muted, progress tipis |
| Now Playing split-view (v9.14, 2026) | **Perubahan terbesar**: bawah layar jadi antrean "Up Next"; swipe-up di mana saja → split-view setengah layar, swipe-up lagi → antrean full-screen; cover sedikit lebih besar; progress bar lebih tebal dan mengembang saat di-scrub; toggle Lagu/Video jadi ikon; tombol lirik pindah ke sebelah jempol naik/turun; Related lewat tap judul + chevron; sheet Lirik/Related tidak lagi berwarna | **Ini inti v1.2**: terapkan split-view antrean (swipe-up setengah → penuh), progress bar tebal yang mengembang saat disentuh, aksi lirik di samping like/dislike, Related = hasil Match (tap judul) |
| Aksi di Now Playing | Carousel aksi: Like/Dislike, Lirik, Simpan, Download, Bagikan, Radio | Sebagian sudah ada (download, lirik) — **rapikan jadi satu baris aksi horizontal** dengan ikon, bukan tombol teks |
| Aset | `animated_like_icon` (hati beranimasi), `chip_bar`, `avatar_stack`, font YTSans | **Tiru polanya**: hati like beranimasi saat ditekan, chip bar konsisten |

## 3. Yang TIDAK diambil (sengaja)

- Warna merah YTM sebagai aksen — tetap titik kecil di label (keputusan v1.1).
- Tab Podcast / Samples / Explore YTM — tidak ada sumber datanya.
- Tombol Create/Blend Spotify — belum ada fiturnya; menambahkannya = tombol palsu.
- Font CircularSp / YTSans — proprietary; tetap font sistem + hierarki v1.1.
- Rive/Lottie art milik Spotify — animasi dibuat sendiri (Compose animate* + ikon Material).
- Tablet sidebar Spotify 2026 — MIXTUNE target HP; jangan bangun layout tablet dulu.

## 4. Rencana penerapan ke MIXTUNE v1.2 (urut prioritas)

1. **Beranda (HomeSearch.kt):** sapaan dinamis per waktu + grid shortcut 2×3 dari
   recent/liked nyata + chip suasana (query YTM nyata) + rak "Dengarkan lagi" +
   Quick Picks dengan tombol "Putar semua" + menu ⋮ per lagu.
2. **Now Playing (SettingsAbout.kt `NowPlayingScreen`):** split-view antrean
   (swipe-up setengah → penuh), progress bar tebal yang mengembang saat scrub,
   background gradasi dari warna cover (Palette), baris aksi ikon (Like animasi,
   Lirik, Download, Bagikan, Radio/Match), Related lewat tap judul + chevron.
3. **Cari (HomeSearch.kt `SearchScreen`):** tab sumber jadi chip filter
   (Semua / Spotify / YTM / Lokal) ala Spotify; hasil tetap paralel.
4. **Pustaka (QueueLibrary.kt `LibraryScreen`):** chip filter
   (Semua / Playlist / Disukai / Download / Lokal) + sort (Terbaru / A–Z).
5. **Antrean (QueueLibrary.kt `QueueScreen`):** ganti tombol teks "Acak: ON/OFF"
   dan "Ulang: OFF" jadi ikon shuffle/repeat ala Spotify (ikon menyala hijau saat
   aktif) — ini yang paling kelihatan "jelek" sekarang karena masih tombol teks.
6. **Motion & loading (UiCommon.kt):** shimmer skeleton saat rak loading,
   crossfade cover (Coil), hati like beranimasi, press-state spring di kartu.
7. **Ukuran APK:** v1.1 membengkak 61 → 83 MB karena ikon Material extended tanpa
   minify — aktifkan minify/shrink di build v1.2 (catatan dari pengiriman v1.1).

## 5. Prasyarat yang masih menunggu tuan (di luar build)

- Tambah redirect URI **`mixtune://spotify-callback`** di dashboard Spotify
  (prasyarat login Spotify; sudah disampaikan saat v1.0, belum dikonfirmasi).
  Tanpa ini, rak/riwayat Spotify tetap kosong dan app hanya jalan di jalur YTM —
  fallback itu sudah built-in, tapi login Spotify tetap butuh langkah ini.
- Setelah langganan Premium owner app, search Spotify 403 hilang (aturan server
  Spotify, sudah diverifikasi ke docs MixMusic). Sampai saat itu, YTM tetap jalur
  utama — jangan janjikan search Spotify jalan penuh.

## 6. Verifikasi sebelum diserahkan

- Build manual + `aapt2` + `apksigner` OK, versionCode naik (v1.1 = 2 → v1.2 = 3),
  keystore SAMA dengan v1.0/v1.1 (install nimpa tanpa uninstall).
- Smoke test VM: search YTM 200, stream visionOS 206, LRCLIB lirik, Match jalan.
- Screenshot tiap layar yang berubah (Beranda, Now Playing split-view, Antrean)
  dicek visual dulu sebelum klaim selesai — pelajaran INVITO: cek capture dulu,
  baru bilang beres.
- Tuan cek dari HP: (1) sapaan + shortcut muncul dari lagu nyata, (2) swipe-up di
  Now Playing memunculkan antrean, (3) progress bar mengembang saat di-scrub.
