# BUILD v1.0.6 — SONARA 1.0.6 (versionCode 7)

Batch 4 celah MixMusic: LIBRARY ORGANIZATION + STATS + DOWNLOAD
MANAGEMENT. Dibangun hanya dari data lokal / signed-out yang sudah
terbukti ada: `play_counts_v1` (v1.10.16, dari nol, tanpa timestamp
per-putaran), riwayat + liked lokal, unduhan `Downloads.kt` yang
sudah ada. Semua string UI Bahasa Inggris. Tidak ada izin baru.
Tidak ada perubahan: tema/token (14 varian), Jam sync, lirik
(v1.0.4 + rumus pemusatan + LyricsVersionPolicy), komentar, feed,
mesin Go, sinkron, notifikasi/widget, kode background-playback
v1.0.2, fitur v1.0.3 + v1.0.5.

## Inventaris sebelum membangun (jujur)

- Unduhan SUDAH ada: `data/Downloads.kt` — rekaman YTM ke
  `filesDir/downloads/<videoId>.webm` + metadata prefs `downloads`,
  bisa diputar offline, tampilan Offline di Library. Yang belum
  ada: progres, jeda/lanjut, coba lagi, auto-download.
- `play_counts_v1` TIDAK punya timestamp per-putaran — "Your Year"
  TIDAK bisa dihitung darinya secara jujur. Solusi: ledger kejadian
  baru `play_events_v1` (mulai dicatat di v1.0.6, cap 2.000, yang
  terlama dibuang); tahun-tahun sebelum v1.0.6 menampilkan empty
  state jujur, bukan angka karangan.
- Model `Playlist` (id/nama/trackIds/createdAt, tersinkron cloud)
  TIDAK diubah: state pin/folder/tag/auto-download disimpan
  TERPISAH di prefs lokal `playlist_org_v1` (pola yang sama dengan
  cover lokal v1.10.41) — tidak pernah naik ke cloud.
- Grid root Library hanya menampilkan 2 slot playlist; playlist
  ke-3 dst. tidak punya jalan masuk dari root. Seksi "Your
  playlists" baru menutup celah ini.
- Detail playlist SEBELUMNYA tanpa cari/urut; Disliked sudah punya
  (v1.0.3) — pola yang sama dipakai ulang.

## Yang dibangun

1. Organisasi Koleksi (`data/LibraryOrgPolicy.kt`, murni + fixture):
   pin (pin dulu, persisten lokal), folder (buat/ganti nama/hapus;
   playlist pindah masuk/keluar; hapus folder TIDAK menghapus
   playlist di dalamnya; hitungan per folder), tag per playlist
   (normalisasi: huruf kecil, maks 24 karakter, maks 8 per
   playlist; tag kosong/duplikat ditolak) + filter multi-tag (AND)
   di seksi root. Urutan tampil: pin → folder (urutan folder) →
   tanpa folder; dalam grup, urutan simpan dipertahankan.
   Serialisasi org.json dengan fallback default bila JSON rusak.
   `LocalStore`: `playlistOrg()` / `savePlaylistOrg()` /
   `prunePlaylistOrg()` (kunci `playlist_org_v1`).
2. Cari + urut DI DALAM playlist (`QueueLibrary.kt` tampilan
   detail): kolom cari menyaring judul/artis lagu playlist itu
   sendiri; chip urut Date added / Title / Artist / Most played
   (hitungan dari `play_counts_v1`, tiebreak stabil = indeks
   simpan). Lagu yang sama boleh muncul dua kali — penyambungan
   kembali memakai antrean kemunculan per id supaya kunci + indeks
   aslinya tepat. Drag-reorder DINONAKTIFKAN saat daftar sedang
   dicari/diurut (indeks tampil tidak memetakan ke indeks simpan);
   Play all memakai entri yang sedang tampil. Dialog Organize dari
   header playlist: sakelar Pin, sakelar Auto-download, pilih
   folder, editor tag (tambah/hapus + tambah-cepat dari tag yang
   sudah dipakai). Hapus playlist ikut membersihkan state org-nya.
   Seksi root "Your playlists": chip filter tag, label grup
   (Pinned / nama folder + hitungan / Playlists — hanya bila ada
   pin/folder), baris playlist (thumb cover 48dp, nama, jumlah lagu
   + tag, tombol pin), tombol "New folder", dialog buat/ganti
   nama/hapus folder. Baris "Your stats" di bawah baris Disliked.
3. Statistik + Your Year + kartu tahun (`data/PlayEvents.kt`,
   `data/StatsCard.kt`, `ui/screens/StatsScreen.kt`,
   `Screen.STATS`): halaman "Your stats" — seksi Your Year (tahun
   kalender berjalan, UTC): total putaran, menit mendengarkan
   (HANYA dari durasi yang diketahui; putaran tanpa durasi
   dihitung di total tapi tidak di menit — footnote jujur),
   top 5 artis + top 5 lagu tahun ini, tombol Share year card
   (aktif hanya bila ada data); seksi All time: total putaran
   (`play_counts_v1`), jumlah liked, Most played + Top artists
   (fungsi yang sudah ada). Empty state jujur di semua seksi
   ("No plays recorded yet", "Your year starts with your first
   play") — TIDAK ada angka contoh/demo. Kartu tahun 1080×1350
   (pola kartu lirik yang sudah ada, warna literal ARUS, font Plus
   Jakarta Sans bila termuat): tahun, total putaran, menit,
   top artis + top lagu, footnote "Minutes count only plays with
   a known length · Recorded on this device since v1.0.6".
   `PlayerManager.startTrack` mencatat `PlayEvent` di samping
   `recordPlay` yang sudah ada (dibungkus `runCatching`).
4. Manajemen unduhan (`data/DownloadTasks.kt` murni,
   `data/DownloadManager.kt` singleton): tugas per lagu
   (QUEUED/DOWNLOADING/PAUSED/FAILED/COMPLETED) + progres 0..1
   (total tak diketahui = 0, bukan tebakan) + persistensi
   `download_tasks_v1` (tugas DOWNLOADING/QUEUED yang tersimpan
   kembali sebagai PAUSED saat muat). Eksekusi: OkHttp + HTTP
   Range dari ukuran berkas `.part` (restart dari 0 bila server
   menjawab 200/416; 206 = tempel); progres StateFlow (throttle
   250 ms, persist throttle 2 s); jeda via pembatalan Call
   (cancel job saja tidak cukup karena `execute()` memblokir);
   selesai → ganti nama `.part` → `.webm` + daftarkan metadata
   lewat `Downloads.addEntry` (format penyimpanan TIDAK berubah;
   `Downloads.fileFor`/`partFileFor` baru dipakai manager).
   Tampilan Offline: seksi "Active downloads" di atas daftar
   (progres, teks status jujur "Downloading X of Y" / "Paused at
   X" / pesan gagal, tombol Pause/Resume/Retry/Delete).
   `TrackSheet`: aksi Download dialihkan lewat manager (label
   mengikuti status tugas; unduhan yang sedang berjalan tidak
   dimulai dua kali; toast mengarah ke Offline downloads).
   Auto-download: sakelar per playlist di dialog Organize (default
   MATI, lokal saja); `maybeAutoDownload` dipanggil dari keempat
   jalur simpan-ke-playlist (satu lagu + bulk, tambah + buat
   baru) — hanya lagu yang benar-benar baru ditambahkan, hanya
   YTM, yang sudah terunduh/aktif dilewati; toast gabungan jujur
   ("Saved to X · Downloading 1 song"). "Delete all downloads" di
   Settings kini juga memanggil `DownloadManager.resetAll`
   (hentikan semua + hapus catatan tugas + berkas `.part`;
   `.webm` dihapus `deleteAll` seperti sebelumnya).
   Baris Delete di tampilan Offline juga memanggil
   `DownloadManager.forget` (lupakan catatan tugas, tanpa operasi
   file) supaya tidak tampil sebagai unduhan aktif.

## Yang TIDAK dibangun (jujur)

- "Your Year" dari `play_counts_v1` (tidak ada timestamp — angka
  tahunan dari sana berarti mengarang; ledger baru mulai v1.0.6).
- Tag/folder/pin yang tersinkron (sengaja lokal saja, mengikuti
  pola cover v1.10.41).
- Folder bertingkat / folder di dalam folder (satu tingkat saja).
- Statistik berbasis akun YTM / riwayat server (signed-out tidak
  tersedia; semua angka dari perangkat ini).
- Unduhan kualitas-pilih-per-lagu / antrean prioritas (satu antrean
  sederhana; kualitas mengikuti pengaturan audio yang sudah ada).
- Kartu tahun untuk tahun lampau (hanya tahun berjalan; data
  tahun lain tidak ada sebelum v1.0.6).

## Verifikasi

- harness151 (reproduce-first): pada tree v1.0.5 yang belum
  disentuh = 15 FAIL + compile fail (reproduksi terkonfirmasi).
  Setelah build: 22/22 source checks PASS (R1–R15 + guard G1–G4)
  + 47/47 fixture Kotlin PASS (F1 pin/folder/tag/sort/search, F6
  play events + year stats termasuk fixture epoch 2026, F7
  progres/resume, F8 round-trip JSON org). Dua koreksi fixture
  yang jujur: `folderOf` dipanggil lewat objek policy (bukan
  method instance); `LyricsApi.kt` + `HttpClient.kt` ditambahkan
  ke daftar kompilasi fixture (PlayStats bergantung pada
  LyricsApi → SonaraHttp). Tidak ada ekspektasi fixture yang
  diubah.
- Guard md5: AppTheme.kt `2480bca8703269751d32bfa3688cc488`
  (byte-identik), LyricsFollow.kt
  `cccee070d2178bde1b69c09e61f63607`, LyricsApi.kt
  `3f82d64bdb38ea09d483a76a406747d9` — ketiganya tidak berubah.
  (Pemeriksaan awal sempat membaca `ui/theme/Theme.kt` yang
  salah — guard yang benar adalah `data/AppTheme.kt`.)
- Clean rebuild: 0 error. Peringatan: hanya set yang sudah ada
  sebelumnya (kotlin-home stdlib/reflect, Java-type-mismatch di
  file API yang tidak disentuh, ikon deprecated); 0 peringatan
  BARU dari kode v1.0.6 — dua peringatan di QueueLibrary
  (baris 1324 `when renameProblem`, 2540 `CollectionCard`) dan
  peringatan di SettingsAbout/TrackSheet semuanya baris yang
  sudah ada sebelum build ini. Satu putaran build pertama gagal
  kompilasi (8 error: impor `Label` tak terpakai, param
  `ArusSearchField`, `Set` vs `List` di LazyRow, `createFolder`/
  `renameFolder` nullable, `folderCounts` 2-argumen, urutan
  deklarasi `canDownload` di TrackSheet) — semuanya diperbaiki
  sebelum build sukses; tidak ada yang disembunyikan.
- aapt2: package `com.sonara.app`, versionCode 7, versionName
  1.0.6, label SONARA.
- apksigner: v2+v3 true, v1/v3.1 false, 1 signer, sertifikat
  SHA-256 `e83b1d93926c1ebbfebd8d0f95391ff5b4fc83b978ebbef1fd88f451c2c5da06`
  — SAMA dengan registrasi Firebase / backup v1.0.5.
- Backup v1.0.5 terverifikasi hash (MD5
  `feb084db6a9ff42da28f3a248e14440e`) di
  `backups/sonara-debug-v1.0.5-vc6.apk` SEBELUM overwrite.

## Regresi (spot-check)

- h150: ALL PASS (39/39 source + fixture Kotlin F1–F9).
- h149: ALL PASS (Kotlin F1–F12, termasuk guard rumus pemusatan
  lirik).
- h148: 45 PASS, 0 FAIL — dijalankan dengan path jar org.json
  yang diperbaiki (salinan run.sh di /tmp menunjuk jar modules-2
  78 KB; harness148 TIDAK dimodifikasi; run.sh apa adanya gagal
  karena jar transforms cache hanya stub 2,7 KB — kerusakan env
  yang sudah ada sebelumnya).
- h130: 79 checks, 0 failures + GUARDS OK.
- h118: SATU putaran pada tree v1.0.6 — ALL PASS (56 PASS,
  F1–F6 lolos termasuk F2). F2 TIDAK dinyatakan fixed — suite
  tidak stabil antar putaran (riwayat: gagal deterministik 3× di
  10.41, lolos di 10.42/10.44/v1.0/v1.0.1, F4 gagal di
  v1.0.2/v1.0.3/v1.0.5) dan investigasi watchdog khusus masih
  terbuka; tidak ada jalur watchdog yang disentuh build ini.
- h118b fixture-blocked + h139 `measure_text` fixture-blocked
  (font /tmp terhapus) + h127 1 kegagalan lama + h128 11
  kegagalan lama: tidak dijalankan ulang (blok env yang sudah
  ada sebelumnya, bukan regresi build ini).

## Artefak

- APK: `sonara-debug-v1.0.6.apk` — ukuran 65.372.125 byte,
  MD5 `2dd00c6ed2b569209da992c8ae62c82b`,
  SHA-256 `14edae45eee47fce0905ab39a9266fe1189e218a98ef5e7340d3d9e16572e907`.
  Link: https://muse.ai/files/1439248322600143/1021546556925170/v9ib7jw5awt5xil6md4ll3hz/sonara-debug-v1.0.6.apk
  (kedaluwarsa 27 Nov 2026 12:13 WIB / 05:13 UTC).
- File baru: `data/LibraryOrgPolicy.kt`, `data/PlayEvents.kt`,
  `data/DownloadTasks.kt`, `data/DownloadManager.kt`,
  `data/StatsCard.kt`, `ui/screens/StatsScreen.kt`,
  `harness151/check_151.py`, `harness151/Harness151.kt`,
  `harness151/run.sh`.
- File diubah: `data/LocalStore.kt` (org + play events + tugas
  unduhan), `data/Downloads.kt` (`fileFor`/`partFileFor`/
  `addEntry`), `player/PlayerManager.kt` (catat play event),
  `ui/SonaraApp.kt` (`Screen.STATS` + cabang + `onOpenStats`),
  `ui/screens/QueueLibrary.kt` (org root + detail cari/urut +
  dialog organize + seksi unduhan aktif + folder dialog),
  `ui/screens/TrackSheet.kt` (Download via manager +
  auto-download di 4 jalur simpan), `ui/screens/SettingsAbout.kt`
  (`resetAll` di hapus-semua-unduhan),
  `build-manual/build.sh` (vc7/1.0.6).

## Langkah cek di HP

About: SONARA v1.0.6 (7). (1) Library → "Your playlists" tampil
semua playlist; pin satu → naik ke atas + label Pinned; New
folder → pindahkan playlist ke folder → hitungan folder benar;
hapus folder → playlist tetap ada tanpa folder. (2) Tag di dialog
Organize (ikon folder di header detail) → chip tag di root
menyaring (multi-tag = AND). (3) Di dalam playlist: cari lagu,
urut Title/Artist/Most played; Play all memakai daftar tampil;
drag-reorder hanya saat tanpa cari + urut Date added. (4) "Your
stats": angka = data nyata perangkat ini (tahun ini mulai dari
0 bila belum ada putaran sejak v1.0.6 — itu jujur, bukan bug);
Share year card hanya aktif bila ada data. (5) Unduh lagu dari
TrackSheet → tampil di Offline "Active downloads" dengan
progres; Pause → Resume melanjutkan (bukan dari 0); matikan
paksa saat mengunduh → kembali sebagai Paused; Retry saat gagal.
(6) Nyalakan Auto-download di satu playlist → tambah lagu baru
→ terunduh otomatis + toast menyebutkannya; playlist lain
tidak ikut. (7) Delete all downloads di Settings membersihkan
juga tugas aktif + .part. (8) Jam/lyrics/podcast/tema/sync/
background tidak berubah.
