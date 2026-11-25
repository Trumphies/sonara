# DESIGN-SPEC — Analisis Spotify & YouTube Music → MIXTUNE v1.2

Dokumen ini hasil analisis **resource APK asli** (bukan tebakan):
- Spotify 9.2.0.2053 — `~/workspace/tools/spotify/apktool-out-9.2.0.2053/` (apktool decode) + `rea-analysis.md`
- YouTube Music 9.46.55 — `~/workspace/tools/youtube-music/work/apktool_out/` (apktool decode) + `rea-analysis.md`

Semua nilai hex/dp/sp di bawah dikutip dari `colors.xml`, `dimens.xml`,
`styles.xml`, dan file `layout/*.xml` hasil decode. Font proprietary
(Spotify Mix, Google Sans, YouTube Sans) **tidak** bisa dibundel — MIXTUNE
tetap memakai font sistem (Roboto) dengan ukuran/weight yang sama.

---

## 1. Spotify 9.2.0.2053 — temuan

### 1.1 Arsitektur visual
- Platform **Cosmos + Esperanto** (protobuf RPC), native core **Orbit**
  (`liborbit-jni-spotify.so`). Design system: **Encore** (`TextAppearance.Encore.*`,
  `Encore.Button.*`, `Encore.Elements.*`) di atas tema **Glue**
  (`Theme.Glue` ← `Theme.Paste`). Home & sebagian besar layar adalah
  Compose/Encore (layout XML hanya untuk Now Playing, player bar, track row).
- Font: **Spotify Mix** (`spotify_mix_ui_regular/bold`, `spotify_mix_ui_title_bold/extrabold`,
  `spotify_mix_mono_regular`) + icon font **Spoticon** + vektor `encore_icon_*_24`
  (filled, viewport 24dp, pathData sederhana — gaya ikon solid, bukan outline).

### 1.2 Warna (terverifikasi dari `colors.xml`)
| Peran | Token | Hex |
|---|---|---|
| Background utama | `dark_base_background_base` / `gray_7` / `gray_background` | **#121212** |
| Lapisan kartu / surface | `gray_15` / `gray_layer` | **#282828** |
| Teks primer | `cat_text_primary_default` | **#FFFFFF** |
| Teks sekunder / muted | `cat_text_secondary_default` / `gray_70` | **#B3B3B3** |
| Abu inaktif / border | `gray_inactive` / `gray_30` | **#585858 / #535353** |
| Aksen (9.x) | `spotify_green_157` / `accent` | **#1ED760** |
| Aksen (klasik) | `green` | #1DB954 |
| Tombol primer | `cat_button_primary_background_default` | #1ED760, teks **#000000** |
| Merah error | `red` | #CD1A2B |
| Bottom sheet | `bottom_sheet_background_color` | #B3000000 (hitam 70%) |

### 1.3 Tipografi — Encore (`styles.xml` + `dimens.xml`)
Skala Encore: base **16sp**, smaller **13sp**, smaller_2 **11sp**, smaller_3 **9sp**,
larger **20sp**, larger_2 **24sp**, larger_3 **32sp**, larger_4 **40sp**, larger_5 **48sp**.
| Peran | Style | Ukuran / Font |
|---|---|---|
| Judul layar besar | `HeadlineLarge` | 48sp, title extrabold |
| Judul seksi / layar | `TitleLarge` / `TitleMedium` / `TitleSmall` | 32 / 24 / 20sp, title bold |
| Judul lagu (row) | `BodyMedium` | 16sp, regular (bold untuk varian) |
| Artis / subtitle | `BodySmall` | 13sp, regular |
| Label kecil / badge | `Marginal` / `MarginalBold` / `Badge` | 11sp / 9sp, bold |
| Corner radius Encore | `encore_corner_radius_*` | 2 / **4** / 6 / 8 / **16** dp |

### 1.4 Spacing & ukuran (`dimens.xml`)
- Spacer Encore: 2 / 4 / 6 / 8 / **12** / **16** / 20 / 24 / 32 / 40 / 48 / 64 dp.
- Kontrol: `encore_control_size_smaller` 32dp, `base` **48dp**, `larger` **56dp**
  (tombol primer large = 56dp, penuh membulat/pill).
- **Track row** (`layout_default_track_row.xml`): min height **64dp**,
  cover `track_row_cover_art_size` **48dp**, margin akhir cover **12dp**,
  gap judul→subtitle **2dp**, padding horizontal **16dp**,
  artwork placeholder gelap (`queue_artwork_placeholder_background`).
- **Home**: `home_carousel_spacing` **16dp**; header seksi top **8dp**,
  bottom **10dp**, gambar header 48dp, gap teks 2dp.
- **Player bar mengambang** (`now_playing_bar.xml`): tinggi **56dp**,
  margin horizontal **8dp**, margin bawah **8dp**, radius **8dp**,
  cover 1:1 radius **4dp**, content padding **8dp**, content **40dp**,
  tombol aksi **40dp**, progress bar **2dp** di tepi bawah.
  Isi bar: cover + carousel judul/artis + (opsional add/connect) + **tombol
  play/pause saja** (tidak ada tombol next). Tap bar → Now Playing
  (transisi expand via MotionLayout `now_playing_bar_scene`).
- **Bottom nav**: tinggi **56dp** (`design_bottom_navigation_height`),
  ikon **24dp**, label padding 10dp, margin 8dp. Ikon filled saat aktif,
  outline saat tidak aktif; label aktif putih, non-aktif abu.
- Tombol play Now Playing: lingkaran hijau besar (kontrol large 56–72dp),
  ikon 32–36dp; kontrol prev/next ikon 32dp dalam target 56dp;
  shuffle/repeat ikon 24dp dalam target 48dp, aktif = hijau.

### 1.5 Pola UX & motion
- Home: sapaan besar di atas, quick-pick grid, lalu rak horizontal;
  **tidak ada TopAppBar persisten** — judul layar adalah teks sapaan.
- Tap baris → putar; **long-press baris → menu konteks**; drag handle di
  antrean untuk reorder; swipe bar ke kiri/kanan = prev/next.
- Lagu yang sedang diputar: judul **hijau** + indikator equalizer;
  shuffle/repeat aktif = hijau; liked = hati terisi.
- Now Playing: cover besar radius kecil (4–8dp), judul 24sp bold,
  artis 16sp muted, slider tipis (progress hijau, thumb putih),
  waktu 11sp; lirik layar penuh, baris aktif putih bold, sisanya abu.
- Empty state: ilustrasi garis sederhana + teks, bukan konten palsu.

---

## 2. YouTube Music 9.46.55 — temuan

### 2.1 Arsitektur visual
- Material 3 (GM3) + token YouTube (`yt_sys_*`): `gm3_sys_color_*` → palet
  `gm3_ref_palette_neutral*`; tipografi `yt_sys_typography_typestack_*`;
  pengukuran `yt_sys_measurement_*`. Font: **Google Sans**
  (UI: `google_sans_text_*`) + **YouTube Sans** (display: `youtube_sans_*`).
  Ikon: vektor `quantum_ic_*_vd_theme_24` (Material outline 24dp).

### 2.2 Warna (terverifikasi)
| Peran | Token | Hex |
|---|---|---|
| Background | `gm3_sys_color_dark_background` → `neutral6` | **#131314** |
| Surface / lapis kedua | `gm3_ref_palette_neutral10` | #1F1F1F |
| Pivot bar (bottom nav) | `ytm_color_grey_12` | **#1D1D1D** |
| Abu lapis | `ytm_color_grey_10` | #282828 |
| Teks primer | `ytm_text_color_primary` / `ytm_color_text_01` | **#FFFFFE** |
| Teks sekunder | `ytm_text_color_secondary` / `ytm_color_text_02` | **#AAAAAA** |
| Teks sekunder translusen | `ytm_text_color_secondary_translucent` | #AAFFFFFF |
| Aksen merah | `yt_red40` / `yt_red60` | #FF4E45 / **#CC0000** (merah merek #FF0000) |
| Aksen primer (GM3) | `gm3_sys_color_dark_primary` → `primary80` | #A8C7FA |

### 2.3 Tipografi — Typestack (`styles.xml`)
Body: Xs 10 / S 12 / **M 14** / L 16 / Xl 18sp (regular/medium/bold).
Action (chip/tombol): Xs 10 / S 12 / **M 14** / L 16 / Xl 18sp.
Display (YouTube Sans): Xs 24 / S 32 / M 40 / L 56sp.
Judul list: `YtmTitle02` 14sp medium, `YtmSubtitle01` 14sp regular,
`Title2` 14sp medium, `Title3` 18sp.

### 2.4 Spacing & ukuran (`dimens.xml`)
- Spacing item: 2 / 4 / 8 / 10 / 12 / **16** / 20 / 24 / 32 dp
  (`item_*_spacing`).
- Radius (`yt_sys_measurement_radius_*`): condensed 4 / compact 8 /
  cozy 12 / comfortable 16 / prominent 20 / hero 24 / huge 32 dp.
- Ukuran (`yt_sys_measurement_size_*`): mini 8 / tiny 6 / condensed 12 /
  compact 16 / cozy 18 / comfortable 24 / prominent 36 / hero 40 /
  legend 48 / huge 56 dp.
- **Mini player** (`swipeable_mini_player_item_container.xml`):
  thumbnail **48dp** (`mini_player_watch_fragment_length` → legend),
  margin 16dp, judul `YtmTitle02` 14sp medium putih, subtitle 14sp
  sekunder translusen, judul bisa marquee.
- **Carousel/rak**: header padding horizontal 16dp, top 12dp bottom 12dp;
  item margin 8dp; peek 24dp (`carousel_peek_width` → comfortable);
  strapline margin-top 4dp; bottom padding 8dp.
- **Browse** (`browse_fragment.xml`): CoordinatorLayout + AppBarLayout
  (toolbar collapse saat scroll), tab tanpa indikator
  (`tabIndicatorHeight` 0), FAB putih.
- **Pivot bar** (`pivot_bar.xml`): TabLayout di bawah, background
  `#1D1D1D`, tanpa indikator, ripple tak terbatas.
- Chip: border tipis, tinggi ~32dp, teks 14sp medium; chip aktif =
  background putih + teks hitam.
- List item standar: tinggi 64dp (Material), thumb 48dp.
- Bottom sheet grid: kolom 80dp, padding 24dp horizontal / 16dp vertikal.

### 2.5 Pola UX & motion
- Rak horizontal dengan **peek** (kartu berikut mengintip 24dp) +
  snap; header rak dengan gambar 40dp + judul.
- Mini player: swipe ke atas → Now Playing penuh (transisi expand),
  swipe kiri/kanan → prev/next; thumbnail 48dp persegi (radius 0).
- Lirik sinkron: baris aktif putih bold, sisanya abu; auto-scroll.
- Search: field gelap membulat + chip filter horizontal yang bisa di-scroll.
- Library: pill filter di atas + daftar; empty state teks polos.
- Download: ikon download 24dp → check saat selesai.

---

## 3. Perbandingan singkat

| Aspek | Spotify | YouTube Music |
|---|---|---|
| Background | #121212 | #131314 |
| Surface kartu | #282828 | #1F1F1F / #1D1D1D |
| Teks sekunder | #B3B3B3 | #AAAAAA |
| Aksen utama | hijau #1ED760 | merah #FF0000 |
| Font | Spotify Mix (proprietary) | Google Sans + YouTube Sans |
| Ikon | filled 24dp (Spoticon/Encore) | outline 24dp (Material/quantum) |
| Track row | 64dp / cover 48dp / radius 4dp | 64dp / thumb 48dp / radius 0–4dp |
| Player bar | mengambang 56dp, margin 8dp, radius 8dp, play saja | menempel, thumb 48dp, judul marquee |
| Rak | gap 16dp, header 8/10dp | gap 8dp, peek 24dp, header 12/12dp |
| Bottom nav | 56dp, filled aktif | pivot bar #1D1D1D, tanpa indikator |
| Tombol primer | pill 56dp, hijau, teks hitam | pill, putih saat aktif |
| Cover Now Playing | besar, radius 4–8dp | besar, radius 8–12dp |

---

## 4. Keputusan untuk MIXTUNE v1.2 (fusi)

Prinsip: **kerangka & aksen dari Spotify, pola rak & list dari YTM** —
karena MIXTUNE adalah satu antrean gabungan: identitas hijau Spotify
sebagai aksen tunggal, struktur rak/list mengikuti YTM yang datanya
memang berasal dari YTM.

| Aspek | Keputusan | Sumber |
|---|---|---|
| Background | **#121212** | Spotify |
| Surface (nav, player bar) | **#1D1D1D** | YTM (pivot bar) |
| SurfaceVariant (kartu, chip, field) | **#282828** | Spotify (gray_15) |
| Teks primer | **#FFFFFF** | Spotify |
| Teks sekunder | **#B3B3B3** | Spotify |
| Border / outline | **#535353** | Spotify (gray_30) |
| Aksen tunggal | **#1ED760** (Spotify 9.x); varian gelap #1DB954 | Spotify |
| Merah YTM | **#FF0000**, hanya titik 6dp di label sumber | YTM |
| Font | sistem (Roboto) — Spotify Mix & Google Sans proprietary, tidak dibundel | — |
| Greeting | 32sp Black (Encore TitleLarge) | Spotify |
| Judul layar | 24sp Black (TitleMedium) | Spotify |
| Judul seksi | 20sp Bold (TitleSmall) | Spotify |
| Judul lagu (row) | 16sp SemiBold (BodyMedium) | Spotify |
| Artis | 13sp (BodySmall) | Spotify |
| Label sumber | 11sp Medium (Marginal) | Spotify |
| Judul Now Playing | 24sp Bold | Spotify |
| Artis Now Playing | 16sp (BodyMedium) | Spotify |
| Lirik aktif / non-aktif | 18sp Bold putih / 16sp #B3B3B3 | Spotify lirik + YTM sinkron |
| Label bottom nav | 11sp Medium | Spotify |
| Padding horizontal | 16dp | keduanya |
| Gap rak | 16dp (Spotify) + peek 24dp (YTM) | fusi |
| Header seksi | top 24dp (jarak antar seksi) / bottom 10dp (Spotify) | Spotify |
| Radius cover row | 4dp | Spotify |
| Radius cover rak / Now Playing | 8dp | fusi (4–8 Spotify, 8–12 YTM) |
| Radius tombol primer | pill 28dp, tinggi 56dp | Spotify Encore large |
| Track row | min 64dp, cover 48dp, gap cover 12dp, gap judul→artis 2dp | Spotify |
| Player bar | mengambang 56dp, margin 8dp, radius 8dp, cover 40dp radius 4dp, tombol play lingkaran putih 40dp ikon hitam, progress 2dp hijau di bawah, swipe kiri/kanan = prev/next | Spotify |
| Bottom nav | tinggi 56dp, bg #1D1D1D, ikon 24dp, label 11sp; aktif = ikon filled + hijau, non-aktif = outline + #B3B3B3 | fusi |
| TopAppBar | **dihapus** — sapaan + tombol avatar/pengaturan di baris header Beranda (Spotify tidak punya TopAppBar persisten) | Spotify |
| Search field | pill putih (bg #FFFFFF, teks hitam, hint #535353), tinggi 48dp, radius 24dp | Spotify |
| Chip filter | pill outline (border #535353, 32dp, teks 14sp Medium); aktif = bg putih + teks hitam | YTM |
| Cover placeholder | solid #282828 + huruf putih 70% (tanpa gradasi) | Spotify |
| Ikon | Material Filled saat aktif / Outlined saat non-aktif (ikon Material adalah set berbeda dari Spoticon & quantum — bukan jiplakan aset) | fusi |
| Launcher | latar #121212 + lingkaran hijau #1ED760 + nota hitam — komposisi original, tanpa logo trademark | original |
| Lirik | tanpa kartu background; baris aktif 18sp bold putih, sisanya 16sp #B3B3B3 | Spotify + YTM |
| Antrean | judul lagu yang sedang diputar berwarna hijau + label "SEDANG DIPUTAR" 11sp bold hijau | Spotify |
| Transisi | tap player bar → Now Playing slide-up; kembali = slide-down (ala expand MotionLayout Spotify / swipe YTM) | fusi |
| Interaksi | long-press baris lagu → menu konteks (Spotify); swipe player bar = prev/next (keduanya) | fusi |

## 5. Identitas ARUS — MIXTUNE v1.3 (menggantikan fusi v1.2)

Verdict user atas v1.2 (21 Nov 2026, 15:03 WIB): *"gak ada perubahan kek
tetep keliatan jelek dan terlalu ai"*. Keputusan: desain diprototype dulu
di web artifact `mixtune-design-prototype`, user menyetujui arah **ARUS**
(16:02 WIB: *"udahh bagus cuman coba lebih di poles"*), lalu dibangun ke
APK v1.3 setelah GAS final (17:31 WIB). Tema gelap Encore §4 DIGANTI
TOTAL — tidak ada lagi mode gelap; satu-satunya tema adalah ARUS terang.

### 5.1 Prinsip

Nyaman dipakai (scroll bebas, daftar rapat, kontrol di zona jempol) +
identitas sendiri yang eye-catching (Garis Arus + poster editorial +
tema kertas hangat terang — kebalikan dari aplikasi musik yang serba
gelap). Struktur Spotify (rak, bottom nav ikon, mini player, layar Now
Playing terpisah) DIBONGKAR — bukan tema baru di atas struktur lama.

### 5.2 Palet ARUS

| Token | Nilai | Pakai |
|---|---|---|
| PaperBg | **#FFF8EC** | background utama |
| PaperSurface | #FFFFFDF7 | permukaan kartu/field |
| PaperSurfaceVariant | #F6E9D2 | permukaan sekunder |
| Ink | #2A1C10 | teks primer |
| InkMuted | #8A7260 | teks sekunder |
| PaperOutline | #E7D3B3 | outline field/chip |
| BurntOrange | **#C14A12** | aksen utama (tombol, judul aktif, segmen aktif) |
| BurntOrangeDark | #9C3A0D | aksen pressed |
| BurntOrangeSoft | #FBE7D8 | latar lembut aksen |
| WaveGreen | #1ED760 | Garis Arus (Spotify) |
| WaveRed | #FF0000 | Garis Arus (YTM) |
| DetailBgFallback | #241407 | background detail bila ekstraksi warna gagal |
| DetailTextPrimary | #FFFFFF | teks di atas background detail gelap |
| DetailTextSecondary | #D8C9B8 | teks sekunder di detail |
| DetailPanelBg | #33200F | panel lembar LYRICS/RELATED |
| SheetBg / SheetSurface | #201409 / #33200F | sheet menu titik-tiga |
| SheetText / SheetMuted | #FFFFF8EC / #D8C9B8 | teks sheet |

### 5.3 Garis Arus (tanda khas)

Gelombang anyaman DUA sinus: hijau Spotify (fase 0) + merah YTM (fase π),
amplitudo 5dp, stroke 3dp. Dipakai sebagai: (1) progress bar detail lagu
+ strip mini player — MENGALIR (fase bergeser, 2500ms/siklus) saat lagu
jalan, diam saat pause; (2) hiasan Kartu Poster; (3) penanda lagu yang
sedang diputar (di samping equalizer mini). Progress BISA DIKLIK untuk
seek (tap = posisi, knob putih 12dp + halo). Warna gelombang diambil
dari identitas kedua sumber — bukan dekorasi acak.

### 5.4 Ukuran & tipografi ARUS

| Aspek | Nilai |
|---|---|
| Baris lagu | 72dp, cover 52dp, radius 8dp |
| Segmen filter sumber | 46dp, tap-only (tanpa drag), aktif = oranye bakar |
| Kartu Poster editorial | 220dp, full-bleed cover + scrim + Garis Arus + tombol play |
| Mini player | kapsul radius 28dp, cover BULAT 48dp berputar (8000ms/putaran) |
| Tombol play detail | lingkaran PUTIH 80dp, ikon gelap 40dp |
| Cover detail | 320dp, radius 8dp; mode Video = 16:9 |
| Baris pill aksi | 44dp, radius 22dp |
| Tab detail | teks 13sp, aktif = bold + garis bawah putih 28×2dp |
| Lirik panel | aktif 20sp bold putih, non-aktif 18sp redup |
| Sapaan Beranda | 32sp Black (dipertahankan dari Encore) |
| Judul lagu detail | 26sp Bold; artis 16sp |

### 5.5 Layar

- **Beranda:** scroll bebas (tanpa snap), segmen Campur/Spotify/YouTube
  Music yang BENAR-BENAR menyaring daftar, SATU Kartu Poster (lagu
  pertama daftar), lalu daftar rapat 72dp (6–7 lagu/layar). Data =
  pencarian YTM nyata + lagu disukai Spotify bila login. Tanpa rak.
- **Cari:** kolom cari kertas + outline hangat, segmen sumber yang sama,
  hasil paralel + banner fallback 403 (tidak berubah dari v1.2).
- **Koleksi** (dulu "Pustaka"): daftar bersih + empty state jujur.
- **Detail lagu:** overlay full-screen slide-up. Bar atas: chevron +
  toggle Song/Video + titik tiga. Artwork besar + halo "bernapas"
  (alpha 0,18↔0,42, 2400ms) + geser masuk tiap ganti lagu. Baris pill:
  Like / Dislike / Komentar / Save — TANPA angka (tidak ada API
  like/komentar; angka karangan DILARANG). Toggle Video = artwork 16:9
  + catatan jujur (video penuh di luar scope v1.3). Background gelap
  dari warna dominan artwork (rata-rata piksel bitmap 64×64 via Coil,
  digelapkan 62%; fallback #241407). Tab UP NEXT (tampilan penuh +
  pratinjau 5 lagu berikutnya) / LYRICS (lirik sinkron LRCLIB, baris
  aktif menyala, auto-scroll halus, pill Share putih melayang) /
  RELATED ("You might also like" dari pencarian artis yang sama +
  "Recommended playlists" dari playlist NYATA user — kolase 2×2 dari
  cover terkait, nama + jumlah lagu asli). Saat tab LYRICS/RELATED
  aktif: header mini (thumbnail + judul/artis + tombol play, panel
  lembar radius 24dp; ketuk header kembali ke tampilan penuh).
- **Sheet menu titik-tiga:** sheet gelap — header + like + X, 3 tile
  (Play next / Save to playlist / Share), 8 aksi NYATA: Start mix
  (cari artis → antrean), Add to queue, Save to library, Download
  (rekaman YTM beneran), Go to album/artist (buka tab Cari),
  View song credits (dialog metadata asli), Pin to Speed dial
  (toast jujur: belum tersedia di v1.3). Panel komentar = empty state
  jujur (tidak ada endpoint komentar).

### 5.6 Token GERAKAN + RESPONS SENTUH

| Token | Nilai | Pakai |
|---|---|---|
| fast / medium / slow | 150ms / 300ms / 450ms | transisi umum |
| stagger | 45ms per item | masuk berjenjang (ganti tab/filter, buka detail/sheet) |
| spin | 8000ms/putaran | cover mini player |
| waveFlow | 2500ms/siklus | Garis Arus mengalir |
| breathe | 2400ms bolak-balik | halo artwork |
| Masuk berjenjang | fade + slide 24dp ke atas, FastOutSlowIn | daftar + artwork |
| Ganti lagu | artwork geser masuk + cover mini player flip 360° (600ms) | detail + mini player |
| Equalizer mini | 4 batang menari (320–620ms) | baris lagu aktif |
| Knob | berdenyut (skala 1↔1,15, 900ms) | progress detail |
| Like pop | snap 0,6 → spring kembali | pill Like |
| Tombol play | membal (spring MediumBouncy) | BouncyBox |
| Ikon nav | loncat 1,28× saat aktif | JumpIcon |
| Segmen | mantul (spring saat pilih) | SourceSegment |
| Reduced-motion | `Settings.Global.ANIMATOR_DURATION_SCALE == 0` → semua animasi aliran/putar/denyut dimatikan | `rememberReducedMotion()` |

### 5.7 Yang dibuang dari v1.2

Rak horizontal, bottom nav ikon+label, mini player bar mengambang
Spotify, layar Now Playing terpisah + slider hijau, chip filter YTM,
kolom cari pill putih, badge sumber, tema gelap + toggle mode gelap,
label "Pustaka" (jadi "Koleksi").

### 5.8 Launcher v1.3

Latar kertas #FFF8EC + lingkaran oranye bakar #C14A12 + nota kertas +
dua gelombang Garis Arus (hijau/merah) di bawah nota. Komposisi
original, tanpa logo trademark.

### 5.9 Fidelitas v1.4 — penyamaan ke prototype ARUS

v1.3 dinilai user "belum sama persis" dengan prototype. Audit
elemen-per-elemen (§5.1–§5.8) menemukan 11 selisih; semuanya diperbaiki
di v1.4, tanpa mengubah mesin/auth/antrean:

| # | Elemen | v1.3 | v1.4 |
|---|--------|------|------|
| 1 | Pencarian di Cari | Hanya saat tombol "Cari" ditekan | **Pencarian hidup**: debounce 500ms tiap ketik/ganti segmen, job lama dibatalkan, query kosong mengosongkan hasil; tombol "Cari" dihapus |
| 2 | Koreografi masuk detail | Hanya artwork yang berjenjang | **Semua blok berjenjang** (StaggerIn 0–6): artwork → judul → pill → progress → kontrol → Berikutnya → tab; diulang tiap ganti lagu via `key(t.id)` |
| 3 | Radius cover baris lagu | 4dp | **8dp** (Corners.medium, §5.4) |
| 4 | Equalizer mini | 3 batang | **4 batang**, durasi 340–610ms (dalam rentang 320–620ms §5.6) |
| 5 | Knob Garis Arus | Lingkaran putih radius 8dp | **Diameter 12dp (radius 6dp) + halo** putih 35% radius 10dp + bayangan lembut |
| 6 | Label pill simpan | Dinamis "Simpan"/"Tersimpan" | **Statis "Save"** (copy prototype) |
| 7 | Pill Share (LYRICS) | Kanan bawah panel | **Tengah bawah** panel (sesuai referensi YTM) |
| 8 | Kolase RELATED | Tanpa avatar | **Avatar inisial** (lingkaran oranye bakar 32dp, huruf pertama nama playlist) di sudut kiri bawah kolase |
| 9 | Penanda baris diputar | Equalizer saja | **Equalizer + Garis Arus kecil** 30dp di sampingnya (§5.3) |
| 10 | Elemen ekstra | Slider volume di detail; chip "Coba cari" di Beranda | **Keduanya dihapus** — tidak ada di prototype |
| 11 | String versi | v1.3 / code 4 | **v1.4 / code 5**; lirik 20sp aktif / 18sp non-aktif (§5.4) |

**Keputusan yang diambil sendiri (tidak tertulis di spec, bisa dikoreksi
user):**

- Knob "12dp + halo" diartikan diameter 12dp (inti putih radius 6dp),
  halo putih transparan radius 10dp, bayangan hitam 22% radius 8dp.
- Label "LAGI MUTER" **TIDAK ditambahkan**: di prototype ARUS, penanda
  lagu aktif adalah Garis Arus + equalizer (§5.3); label itu milik
  identitas DECK yang sudah diganti ARUS. Kalau user mau labelnya juga,
  tinggal tambah.
- Durasi batang equalizer dipilih 340/420/530/610ms (semua dalam
  320–620ms); urutan tinggi batang bervariasi agar tariannya tidak
  monoton.
- Pencarian hidup memakai debounce 500ms (nilai umum; tidak tertulis
  di spec) dan membatalkan job pencarian sebelumnya agar hasil lama
  tidak menimpa hasil query terbaru.

Yang sudah sama sejak v1.3 dan tidak diubah: palet/tema, perilaku Garis
Arus, background warna dominan, segmen 46dp, poster 220dp, mini player
kapsul + putar/flip/swipe, nav kapsul, top bar detail, artwork 320dp +
halo, pill tanpa angka, seek gelombang, play 80dp, repeat 3-mode, gaya
tab, header mini + panel lembar, lirik + auto-scroll, baris/kolase
RELATED + geser, sheet menu + aksi nyata, empty state komentar jujur,
mode Video 16:9 + catatan jujur, reduced-motion, launcher, demo 0,
tanpa aset proprietary.

### 5.10 Polish v1.5 — "polish lagi abis abisan" (tanpa fitur baru, tanpa redesign)

User 21 Nov 2026 (21:09 WIB): *"polish lagi abis abisan"*. Semua layar
dipoles sampai detail eksekusi; identitas, struktur, copy, dan ukuran
§5.1–§5.9 TIDAK berubah. Elemen yang sengaja dihapus di v1.4 (slider
volume detail, chip "Coba cari") tetap dihapus.

| # | Dimensi | Sebelum (v1.4) | Sesudah (v1.5) |
|---|---|---|---|
| 1 | Loading Beranda/Cari/Koleksi | Spinner di tengah layar kosong | **Skeleton** berbentuk isi asli (poster + baris lagu berdenyut sinkron, satu transisi per daftar); di Cari, skeleton hanya saat belum ada hasil — hasil lama bertahan dan update in-place |
| 2 | Artwork | Gambar jebret masuk; huruf placeholder di belakang | **Crossfade 400ms** (Coil) di semua cover + poster; huruf tetap jadi placeholder saat loading; gambar gagal = huruf |
| 3 | Tint detail | Warna dominan jebret tiap ganti lagu | **Transisi warna 450ms** (animateColorAsState) |
| 4 | Seek Garis Arus | Tap saja | **Tap + seret** (satu gesture, event di-consume supaya klik induk tidak ikut); knob **berdenyut** skala 1↔1,15 (900ms) saat jalan |
| 5 | Animasi tak berujung | Transisi fase/napas selalu jalan → recomposition 60fps walau pause | Transisi fase (gelombang), denyut knob, dan napas halo **hanya dibuat saat lagu jalan**; saat pause tidak ada animasi yang berjalan |
| 6 | Data Beranda | Hidup di HomeScreen → ganti tab = muat ulang + spinner + kedip | **Dipegang MixtuneState** (root): dimuat sekali, bertahan antar tab; muat ulang hanya saat login berubah / "Coba Lagi"; permintaan saat muatan jalan ditandai stale, dimuat ulang sekali setelahnya |
| 7 | Daftar Cari | `key(searchTick)` membangun ulang LazyColumn tiap hasil → scroll melompat + animasi berulang | LazyColumn ber-key lagu, update in-place; animasi masuk hanya 8 baris pertama (baris saat scroll tidak pop-in) |
| 8 | Sheet | Handle pajangan; isi mentok navigation bar | **Handle bisa diseret** ke bawah (>90dp = tutup, kurang = membal kembali); isi terangkat di atas navigation bar (`navigationBarsPadding`) |
| 9 | Status/nav bar | `windowLightStatusBar=false` (ikon putih di atas kertas — tidak terbaca sebelum edge-to-edge); ikon gelap di layar detail gelap | Jendela alas: kertas + ikon gelap; **detail lagu mengubah ikon jadi terang** selama tampil dan mengembalikannya saat keluar (`SystemBarAppearance`) |
| 10 | Field Cari | Border statis; keyboard menutupi hasil | Border **oranye + 2dp saat fokus** (animasi 150ms); tombol Search menutup keyboard; hasil terangkat di atas keyboard (`imePadding`) |
| 11 | Ripple | Klik di permukaan gelap tanpa feedback terlihat (ripple gelap) | **Ripple terang** di pill/tab/baris detail/sheet (material3 `ripple`) + `onClickLabel` aksesibilitas di semua klik utama |
| 12 | Sentuhan | Tombol play mini player 44dp | Area sentuh **48dp** (visual tetap 44dp); chip Song/Video min 40dp |
| 13 | Error Koleksi | Error Spotify hanya teks | Error + tombol **"Coba lagi"** yang benar-benar memuat ulang |
| 14 | Posisi putar | Polling 500ms (knob melompat) | Polling **250ms** (knob + lirik lebih halus; recomposition tetap ringan) |
| 15 | Empty state | Teks rata kiri di kolom tengah | Teks **rata tengah**; baris lagu ber-key stabil di semua daftar Koleksi |

Sengaja TIDAK diubah: segmen 46dp dan pill 44dp (ukuran spec — di bawah
48dp tapi sesuai identitas yang disetujui); tidak ada divider baru
(desain ARUS memang tanpa divider); `SpinningCover`/`MiniEqualizer`
sudah tergating sejak v1.3/v1.4; polling tetap memakai Handler (bukan
coroutine) supaya perilaku crossfade/gapless tidak berubah.

### 5.11 Polish v1.6 — "Gas, poles semua jadi v1.6" (tanpa fitur baru, tanpa redesign)

User 21 Nov 2026 (22:05 WIB): *"bisa di polish lebih lagi ga?"* →
*"Gas, poles semua jadi v1.6"*. Delapan item polish ronde 2; identitas,
struktur, copy, dan ukuran §5.1–§5.10 TIDAK berubah. Elemen yang
sengaja dihapus di v1.4 (slider volume detail, chip "Coba cari") tetap
dihapus.

| # | Dimensi | Sebelum (v1.5) | Sesudah (v1.6) |
|---|---|---|---|
| 1 | Transisi artwork | Detail slide-up; artwork jebret masuk | **Hero morph**: cover mini player membesar jadi artwork detail (350ms buka / 300ms tutup, lerp antar-rect, overlay blok ketukan); fallback slide-up bila reduced-motion atau rect belum terukur |
| 2 | Haptics | Tanpa getar | **Getar halus** via `View.performHapticFeedback` (hormat setelan sistem): Like=Confirm, kontrol/tab=Tap/Selection, lepas seek=Confirm, drag antrean=LongPress + pindah=Tap, nav/segmen/play/swipe=Tap |
| 3 | Fade suara | Play/pause "jedug" | **Fade-in 300ms / fade-out 150ms** pada ExoPlayer, dikalikan dengan skala crossfade (satu penulis volume); dilewati saat gapless ON; embed YTM hanya volume dasar (IFrame mereset volume) |
| 4 | Lirik | Highlight per baris | **Highlight per kata** bila LRCLIB `hasWordSync` (parser `lyricsfile`); fallback per baris; auto-scroll lengket (skip bila baris aktif sudah nyaman terlihat, pusatkan offset `viewport/2 − 56dp`) |
| 5 | Notifikasi media | Tanpa artwork; ikon play/pause basi setelah toggle; tanpa MediaSession | **Artwork** large icon (cache Coil) + refresh saat ganti lagu DAN play/pause + **MediaSession framework** (metadata/state, callback transport, MediaStyle 3 aksi ringkas) |
| 6 | Antrean | Urutan tetap | **Drag-to-reorder** di layar Antrean penuh (auto-scroll tepi, `animateItem`) dan pratinjau UP NEXT detail (garis drop oranye); kunci `queueUid` stabil; urutan baru dipakai via `moveQueue` |
| 7 | Ukuran APK | 83.727.187 B | **61.035.347 B (−27,1%)**: filter `material-icons-extended` 11.105→118 kelas (37,4 MB→323 KB, jaring pengaman build + cek dex 32 ikon) + hapus 9 AAR daun tak terpakai; dex 50 MB→27 MB; R8 tidak tersedia (hanya d8) |
| 8 | Daya tahan | Cover detail tetap 320dp; cache per panggilan | Cover **240dp saat landscape**; baris mini player min 56dp (font besar); konten **max 720dp terpusat** (tablet); **cache Coil tunggal** + **prefetch 2 artwork berikutnya** |

Nilai yang dipilih sendiri (tidak tertulis di spec, dicatat di sini):
progres morph memakai `Animatable<Float>` + `lerp()` karena
`Rect.VectorConverter` tidak tersedia di animation-core 1.7.0 pada
build manual; durasi fade 300ms/150ms dan tick 25ms; threshold
auto-scroll tepi antrean 80px dengan langkah 18px per 16ms; ukuran
prefetch artwork notifikasi 512px. Target morph hero = posisi artwork
detail terakhir yang terukur (identik antar lagu karena layoutnya
sama); bila belum pernah terukur, fallback slide-up.

### 5.12 Rebuild TOTAL v1.7 — dari 7 screenshot ground truth prototype

User 21 Nov 2026 (23:10 WIB) mengirim 7 screenshot prototype
(Beranda, detail Song, detail LYRICS header-mini, detail RELATED
header-mini, detail Video 16:9, Cari, Koleksi) dan menilai v1.6
"masih gak mirip prototype". Akar masalah (diakui jujur): rebuild
v1.3–v1.6 bekerja dari ringkasan tertulis spec prototype, bukan
dari pixel prototype yang sebenarnya. v1.7 dibangun DARI screenshot
itu — satu-satunya kebenaran visual. Yang bertentangan dengan
§5.1–§5.11 dikalahkan screenshot. Chrome yang diabaikan: status
bar HP asli, header hitam halaman artifact (hamburger + avatar
chibi + titik-tiga), dan fake status bar "21:49 MIXTUNE 5G" di
screenshot 6–7 — TIDAK direplikasi, TIDAK ada header wordmark
MIXTUNE di app.

**Warna (sampling piksel, file 907×2048):** kertas #FFF9ED,
segmen #F2E7D3, hitam pekat #1B1813 (vinyl #14130F), gold poster
#E8BA0F, gold avatar #FFB727, oranye bakar #FF4E24 (varian
#FF4F28 di kartu Koleksi — disamakan), teal #0E7C7B/#147C7B,
ungu Galau #7A50D0, slate Fokus #445879, deco Liked #FF8C71,
panel header mini #473706, kartu lirik #3D3119, panel related
#403517, pill detail putih ~12%, wave detail abu #6F6862–
#8B8683 knob putih, video rect #7F6609, teks sekunder
coklat-abu #8A8378. Gradien detail: dari warna dominan artwork
(untuk gold: atas #483807) ke hampir hitam coklat.

**Nilai terukur (÷2,13 px/dp):** margin konten 19dp; poster
204dp / radius 28dp / play 64dp / judul 42sp Black; segmen 64dp
/ pill aktif 48dp; baris 72dp / cover 54dp / radius 14dp /
judul Bold 17sp; mini player 73dp / play 56dp; nav 68dp;
artwork detail 320dp / radius 28dp / judul Black 36sp / play
72dp / pill 48dp; tile genre ~169×104dp / radius 24dp / label
20sp; kartu Koleksi ~169×136dp / radius 24dp; lirik aktif 28sp
Black / nonaktif 22sp; headline Cari/Koleksi 38sp Black;
label mono 11sp Monospace letter-spacing ~0,12em.

**Nilai yang ditebak (tidak bisa diukur pasti dari JPEG,
dicatat jujur):** hijau gelombang poster #1FA34F (anyaman hijau
+ krem di kiri bawah poster), teks sekunder #8A8378, pill
poster #574A12, pill toggle aktif #6B5F43, tan strip #DCCFB2,
gelombang strip progress #8A8378. Font display = SansSerif
Black (900) — font proprietary tidak dibundel; label mono =
Monospace kapital.

**Struktur per layar (ditiru habis termasuk copy):**
- Beranda: avatar gold "T" + sapaan mengikuti jam + "tuan 👋" +
  tombol not bulat putih (→ Pengaturan); poster (pill "PILIHAN
  HARI INI", vinyl terpotong, judul putih, "Artis — Album",
  Garis Arus hijau+krem, play putih); segmen hitam + caption
  mono "SEMUA LAGU DARI DUA SUMBER"; "Lagi ramai" + "N LAGU";
  baris format screenshot + divider hairline; strip Garis Arus
  terpisah di ATAS mini player; mini player HITAM; nav kapsul
  PUTIH (aktif pill HITAM, ikon Koleksi = grid).
- Cari: headline 2 baris + subteks persis; kolom cari pill
  PUTIH 56dp border gelap 2dp; "Jelajahi" + tile genre 2×2
  (tap = cari genre, data real); "Semua lagu" + "TAP BUAT
  MUTER" + daftar browse nyata (saat mengetik: "Hasil
  pencarian" + "N HASIL"). Segmen dihapus dari Cari.
- Koleksi: headline + subteks persis; grid 2×2 (Disukai,
  Offline, 2 playlist ASLI atau kartu "Buat playlist" jujur);
  "Terakhir diputar" = riwayat putar NYATA perangkat
  (`LocalStore.recentTracks`, maks 20 — baru di v1.7).
- Detail: gradien dominan; toggle Song/Video; artwork 320dp
  (halo oranye DIHAPUS); pill gabungan Like|Dislike +
  Komentar + Save scroll-horizontal TANPA angka; progress
  gelombang ABU; tab di TENGAH di atas daftar UP NEXT
  (underline selebar tab); header mini + panel lembar untuk
  LYRICS/RELATED; mode Video (v1.8) = rect 16:9 berisi VIDEO
  BENARAN (stream video-only, TextureView, tanpa judul/label di
  dalam rect); sheet menu di-restyled ke palet detail gelap.

**Gabung dua sumber:** lagu yang sama (judul+artis
ternormalisasi) dari Spotify dan YTM digabung jadi SATU baris
dengan dua titik + label "SP + YT" (`mergeDualSource` di
UiCommon.kt) — dipakai Beranda, Cari, dan Koleksi (Disukai).

**Adaptasi jujur (penyimpangan sadar):** artwork asli
menggantikan placeholder gold (placeholder hanya bila artwork
kosong; vinyl tidak digambar di atas artwork); angka like/
komentar TIDAK ditampilkan (screenshot ilustratif, tidak ada
API asli); sapaan mengikuti jam; playlist/angka Koleksi = data
asli (slot kosong = "Buat playlist", bukan nama karangan);
"Terakhir diputar" kosong sebelum ada lagu yang diputar;
mode Video (v1.8) memutar video YT sungguhan (stream video-only
tersinkron ke audio, dibisukan); bila tidak ada stream video,
app kembali jujur ke mode Song + toast, bukan artwork pura-pura
video. Judul detail 2 baris memakai lineHeight eksplisit 42sp
(perbaikan tumpang-tindih v1.7 di HP user).

**Tetap (tidak regress):** mesin Go, PKCE, fallback 403, Match,
antrean, crossfade/gapless, fade, haptics, lirik per kata,
notifikasi media, drag-reorder, seek, swipe, skeleton/empty/
error, polling 250ms, crossfade artwork 400ms, hero morph,
stagger, equalizer, flip cover, reduced-motion. Tanpa demo
(grep demo|soundhelix = 0), tanpa aset proprietary.

### 5.13 YTM murni v1.9 — semua fungsi Spotify dicopot (tanpa redesign)

User 22 Nov 2026 (00:42 WIB): "murni ytm aja". Keputusan ini diambil
setelah jalur resmi Spotify dibahas (Client ID wajib untuk OAuth,
Redirect URI + owner Premium masih kurang) dan jalur tidak resmi
DITOLAK — tidak dikerjakan, tidak boleh dikerjakan. v1.9 mencopot
SEMUA fungsi Spotify dan TIDAK mengubah desain, ukuran, warna, copy
(v1.7/v1.8), atau perilaku YTM apa pun. Yang bertentangan dengan
§5.1–§5.12 (segmen, dua sumber, Match, PKCE, fallback 403) dikalahkan
bagian ini.

**Dicopot total:**
- File dihapus: `data/SpotifyAuth.kt` (OAuth PKCE + token), `data/SpotifyApi.kt`
  (Web API: me/search/liked/playlists/devices). Konstanta Client ID
  dihapus dari source (nilainya tidak ditulis di laporan/log/file mana pun);
  field Client ID di Pengaturan + `maskClientId` dihapus.
- `Models.kt`: enum `Source.SPOTIFY`, field `Track.spotifyId` /
  `Track.matchScore`, `SpotifyUser`, `SpotifyPlaybackMode` dihapus.
  `Source` kini hanya `YOUTUBE_MUSIC` dan `LOCAL`.
- Match dihapus: `YoutubeMusicApi.matchSpotifyTrack/matchScore/wordOverlap/
  artistMatches/cleanTitle`, `GoEngine.matchScore`,
  `PlayerManager.playSpotifyTrack/matchAndPlay` + cabang Connect.
  Mesin Go (.aar) TIDAK diubah/di-rebuild — hanya tidak lagi dipanggil
  untuk Match; fungsi Go `matchScore` di dalam .aar menganggur.
- Search: pencarian paralel Spotify + banner fallback 403 dihapus — Cari
  kini YTM saja (debounce 500ms dipertahankan).
- Segmen Beranda (Campur/Spotify/YT Music) dihapus total beserta composable
  `SourceSegment`; caption menjadi "SEMUA LAGU DARI YOUTUBE MUSIC".
  Daftar Beranda kini trending YTM + lagu yang disukai (lokal).
- Koleksi: seksi liked/playlist Spotify + tombol "Pengaturan & Login Spotify"
  dihapus; slot playlist kini hanya playlist lokal; meta kartu Disukai
  tanpa embel-embel sumber.
- Label sumber: "SPOTIFY" / "SP + YT" dihapus dari baris lagu, detail,
  dan RELATED; baris kini hanya "YT MUSIC" / "LOKAL" (titik warna
  dipertahankan sebagai bagian desain). `mergeDualSource` diganti
  `mergeTracks` (dedup judul+artis, sumber tunggal).
- Login/logout + deep link `mixtune://spotify-callback` (AndroidManifest +
  `MainActivity.handleIntent` + `MixtuneApplication.authEvents`) dihapus.
- Teks About/batasan ditulis ulang YTM saja; `SecurePrefs` dipertahankan
  (alias kunci diganti) walau saat ini tidak menyimpan apa pun.
- `SpotifyAuth.defaultHttp()` diganti `MixtuneHttp.defaultHttp()` (file baru
  `data/HttpClient.kt`) — dipakai YTM, lirik, download.

**Dipertahankan (tidak regress):** seluruh identitas visual ARUS v1.7/v1.8
(tema, Garis Arus, poster, nav kapsul, mini player hitam, ukuran/warna
terukur); search YTM + stream audio visionOS (Opus 251); mode Video v1.8
(stream video-only + ExoPlayer kedua dibisukan + sinkron 250ms); lirik
LRCLIB per kata; download YTM; scan MP3 lokal; antrean + drag-reorder;
crossfade/gapless/fade; haptics; notifikasi media; hero morph; skeleton/
empty state; "Terakhir diputar"; playlist lokal yang real. Tanpa demo
(grep demo|soundhelix = 0), tanpa aset proprietary, `grep -ri spotify
app/src` = 0. Data lama: track tersimpan bersumber Spotify dipetakan ke
YTM saat dibaca (bukan crash); antrean tersimpan berisi track Spotify
dibuang bersih saat restore.

**Verifikasi v1.9 (VM):** search YTM HTTP 200 (11 videoId), stream audio
Opus itag 251 Range 0-0 → HTTP 206, stream video itag 136 720p AVC →
HTTP 206, LRCLIB 200 (57 baris sinkron). Verifikasi visual + perilaku
di HP tetap dari user — VM tidak punya emulator.

### 5.14 Login Google + sinkron cloud v1.10 (tanpa redesign)

User 22 Nov 2026 (04:02 WIB): login pakai Google supaya playlist,
lagu disukai, dan riwayat tersimpan permanen dan lintas platform.
Setup console Firebase dilakukan (bukan oleh user) — project Spark
gratis tanpa billing, app Android terdaftar dengan SHA-1 keystore
debug, Google sign-in ENABLED, Firestore production + rules
owner-only `users/{userId}` published. Konfigurasi di-wire dari
file google-services.json asli (project_id disebut di BUILD-v1.10.md;
nilai kunci TIDAK ditulis di laporan mana pun). Tidak ada langkah
console untuk user.

**Arsitektur (mengikuti keputusan):** Firebase Auth (Google) +
Firestore lewat REST (Identity Toolkit + Firestore v1) dengan OkHttp
yang sudah ada — TANPA Firebase SDK (build manual tanpa Gradle).
Google Sign-In memakai Play Services Auth SDK 21.2.0 + closure
transitifnya (auth-api-phone, auth-base, base, basement, fido,
tasks) + fragment 1.5.7 / loader 1.1.0, diunduh dari maven.google.com
dan diintegrasikan seperti .aar lain; closure diverifikasi dengan
jdeps (tidak ada referensi di luar closure + libs yang ada; referensi
`libcore.io` di protobuf internal bersifat reflektif dengan fallback
aman). Komponen manifest AAR (SignInHubActivity, RevocationBoundService,
GoogleApiActivity, meta-data gms.version) ditulis manual ke manifest
app karena build manual tidak punya manifest merger. `SyncConfig.kt`
memegang apiKey/projectId/Web client ID; selama placeholder, app
jalan normal signed-out dengan status jujur (bukan error).

**Data yang disinkronkan (hanya ini):** playlist buatan user (nama +
daftar videoId + snapshot judul/artis/artwork + updatedAt), lagu
disukai (videoId + snapshot + likedAt), riwayat "Terakhir diputar"
(videoId + playedAt; maks 50 di cloud, 20 tampil). BUKAN: file
download (hanya metadata), pengaturan, antrean sesaat.

**Perilaku:** local-first. Push debounce 2 detik saat ada perubahan +
saat login; pull saat login + saat app dibuka dalam keadaan login.
Merge last-write-wins per dokumen berdasar `updatedAtMs`; hapus
eksplisit (unlike) dicatat tombstone supaya ikut terhapus di cloud
dan tidak bangkit saat pull. Login pertama: lokal di-push dulu, baru
pull. Offline/tanpa login: semua fitur jalan; status sinkron jujur
("Tersimpan di perangkat" / "Tersinkron • jam" / "Menunggu
sinkron"). Logout: data lokal tetap; ada opsi "Hapus data cloud saya"
di Pengaturan (hapus koleksi user + keluar, dengan konfirmasi).
Token (Google ID token, Firebase ID/refresh token) disimpan
terenkripsi (SecurePrefs, pola PKCE); tidak pernah di-log.

**Skema Firestore** (agar klien platform lain bisa membaca):
`users/{uid}/playlists/{id}` {name, updatedAtMs, updatedAt, tracks[]
{trackId, videoId, title, artist, imageUrl, source}},
`users/{uid}/liked/{videoId}` {trackId, videoId, title, artist,
imageUrl, source, likedAtMs, updatedAtMs, updatedAt},
`users/{uid}/history/{videoId}` {… playedAtMs, updatedAtMs, updatedAt},
`users/{uid}/meta/profile` {displayName, email, photoUrl, updatedAtMs,
updatedAt}. Nilai Firestore: stringValue/integerValue (string)/
timestampValue (RFC3339 UTC)/arrayValue/mapValue.

**UI (tanpa redesign):** seksi akun di Koleksi (paling atas) + di
Pengaturan — kartu krem ARUS, avatar foto/inisial, nama/email, status
sinkron, tombol "Masuk dengan Google" / "Sinkronkan sekarang" /
"Keluar" (+ "Hapus data cloud saya" di Pengaturan). State
belum-dikonfigurasi menampilkan status jujur, bukan error. Tidak ada
demo, tidak ada aset proprietary.

**Verifikasi v1.10 (VM):** build 0 error; mode signed-out berperilaku
persis v1.9 (smoke test: search 200/11 videoId, visitor OK, player
OK, audio 251 → 206, video 136 → 206, LRCLIB 57 baris); bentuk
request REST Auth + Firestore diuji melawan stub HTTP lokal yang
menegaskan payload + header Authorization (26/26 cek stub lolos;
harness JVM tidak bisa membaca respons localhost di sandbox ini —
terbukti quirk lingkungan lewat HttpURLConnection polos — jadi bentuk
yang sama di-replay via Python; kode Kotlin-nya sendiri terkompilasi
bersih dan bentuk request-nya identik). Verifikasi login Google +
sinkron beneran HANYA dari HP user setelah install — VM tidak bisa
login akun Google user.

### 5.15 Perbaikan dari laporan HP v1.10.1 (tanpa redesign)

Lima item dalam SATU build, dari laporan user 22 Nov 2026
(09:59–10:02 WIB, Infinix X6853, Android 16).

**A. Crash toast (09:59).** `Toast.makeText` dari callback login
Google di `Dispatchers.IO` → `NullPointerException: Can't toast on a
thread that has not called Looper.prepare()`. Perbaikan: SATU helper
`showToast()` (main-thread-safe dari thread apa pun) di
`ui/ToastHelper.kt`; `SyncManager` mengembalikan semua callback pesan
ke `Dispatchers.Main` (`emitMessage`); sapuan 20 situs `Toast`
(TrackSheet 14, QueueLibrary 1, SettingsAbout 4, MainActivity 1)
dikonversi ke helper; satu situs `showSnackbar` (LaunchedEffect) sudah
main-safe dan dibiarkan. Semua sentuhan View lain sudah lewat main
Handler (WebEmbedPlayer, PlayerManager, PlaybackService).

**B. Tombol Back di semua layar (10:00, "di semua nya").**
`ArusBackButton` (UiCommon): lingkaran 48dp, panah 24dp, label
"Kembali", varian terang (lingkaran putih) + gelap (detail/sheet).
Satu tumpukan navigasi di `MixtuneApp` (`navStack`, elemen terakhir =
layar aktif): pindah tab / buka detail / Pengaturan / laporan crash /
antrean mendorong tumpukan; Back = pop. Detail menutup ke layar
pembuka (animasi hero dipertahankan; pop terjadi di akhir morph).
`BackHandler` root memakai tumpukan yang sama; handler yang lebih
dalam (detail, sheet, tampilan detail Koleksi) diprioritaskan. Di
root (Beranda, tumpukan 1 elemen), Back = `moveTaskToBack` (ke latar
belakang, TIDAK force-close). Tombol Back: Beranda (baris header),
Cari (di atas headline), Koleksi (item pertama + tampilan detail),
Pengaturan (sebaris judul), detail (ganti chevron — mode penuh DAN
header mini, satu bar yang sama), Antrean, laporan crash, dan sheet
menu (Back = tutup sheet; `BackHandler` di `ArusBottomSheet`).

**C. Mode Video (10:01).** Rect 1:1 (dulu 16:9), video mengisi kotak
dengan crop tengah (`VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING`).
`VideoStatus.FAILED` baru; `READY` HANYA dari `onRenderedFirstFrame`
(dulu optimistis langsung setelah `prepare()`); artwork digambar DI
ATAS TextureView opaque sampai frame pertama (dulu artwork di bawah
TextureView transparan — rapuh); watchdog 15 detik untuk LOADING yang
menggantung (resolve/buffering tanpa frame → FAILED + alasan);
`UNAVAILABLE`/`FAILED` → kembali jujur ke Song + toast alasan.
Resolve stream video tidak berubah (rantai kandidat + gate sudah ada
sejak v1.8).

**D. "Source error" (10:01).** Akar teks: `PlaybackException.message`
mentah ("Source error") ditampilkan apa adanya. Perbaikan:
(1) `friendlyPlaybackError()` memetakan errorCode ke pesan Indonesia
spesifik (jaringan / stream ditolak-kedaluwarsa / format tak
didukung); (2) rantai format audio `audioCandidates()` — Opus
251/250/249 → AAC 140 → format lain, maks 6, gate Range 0-0 maks 3
per client (dulu SATU format); (3) fallback multi-client InnerTube
(VISIONOS → WEB → ANDROID → TVHTML5) + fallback manifest HLS
(non-live) + kandidat DASH terbaik tanpa gate sebagai lapis akhir;
(4) `playabilityStatus` dibedakan (usia/wilayah/dihapus/member/hak
cipta); (5) `restoreQueue` MEMBUANG `streamUrl` tersimpan (URL
kedaluwarsa ~6 jam = penyebab klasik error instan); (6) error putar
di awal lagu → re-resolve SEKALI, lalu pesan jujur; (7) snackbar
error + tombol "Coba lagi" (`retryCurrent()`, resolve baru, tanpa
auto-skip); (8) HLS dinyatakan mime-nya di `playExo`
(`APPLICATION_M3U8`) supaya tidak salah ditebak Progressive; (9) gate
Range-check timeout 10s/12s (gate 4s terbukti gagal total di jaringan
lambat — salinan debug terinstrumentasi). Mesin Go (.aar) TIDAK
disentuh. Fallback client ANDROID saat ini menjawab HTTP 400 dan WEB
menjawab UNPLAYABLE dari jaringan ini — keduanya dilewati dengan
anggun; VISIONOS + HLS yang bekerja.

**E. Lirik (10:02, item terakhir).** Audit: jalur lama = SATU panggilan
`/api/get` eksak (artis/judul/album/durasi), hanya `syncedLyrics`,
`plainLyrics` diabaikan. Perluasan (`LyricsApi.fetchLyrics`): varian
query (tanpa album, tanpa durasi, artis/judul tanpa "feat.", judul
dasar tanpa tag versi) + `/api/search` toleransi ±2 detik + fallback
lirik POLOS LRCLIB + lirik bawaan YTM via InnerTube browse (tab
Lyrics MPLY → `musicDescriptionShelfRenderer`, teks polos, sumber
Musixmatch; terverifikasi hidup dari VM 8/10) + guard durasi: lirik
sinkron dari entri yang durasinya meleset >10 detik diturunkan jadi
polos (tidak pura-pura sinkron). UI: `LyricsPanel` membedakan
SYNCED (baris aktif + auto-scroll + caption sinkron) vs PLAIN
(baris terang merata + caption "Lirik tanpa timing (dari …)"); state
kosong jujur dipertahankan; lirik TIDAK PERNAH dikarang.

**Verifikasi v1.10.1 (VM, harness JVM atas kode Kotlin asli):**
batch 10 lagu (probe3) — audio 10/10 resolve (setelah perbaikan gate:
DASH Opus; sebelum perbaikan gate: 10/10 lewat HLS), video 10/10
(itag 134–137 AVC), lirik 9/10 ter-cover (8 sinkron + 1 polos;
sebelum: 8 sinkron + 2 kosong), search tidak berubah, videoId palsu →
error jujur tanpa crash. Kejujuran: batch VM TIDAK mereproduksi
"error" HP (jaringan VM cepat, URL segar) — pengerasan menutup
penyebab di sisi playback-time/URL-basi yang tidak terjangkau batch
VM; render video, login Google, dan perilaku Back HANYA bisa
diverifikasi dari HP user.

### 5.16 Perbaikan sinkron dari laporan HP v1.10.2 (tanpa redesign)

Laporan user 22 Nov 2026 (11:01 WIB, Infinix X6853, Android 16):
login Google TIDAK crash (fix toast v1.10.1 bekerja, akun tampil),
tapi sinkron gagal dengan pesan mentah HTTP 400 `updateMask`.

**Akar masalah.** `FirebaseRest.upsertDocument` mengirim POST
(`postJson`) ke URL dokumen `.../documents/users/{uid}/{coll}/{doc}`
dengan query `updateMask.fieldPaths=...`. `updateMask` hanya dikenal
`UpdateDocumentRequest` (PATCH); POST ke URL dokumen diikat server
ke `CreateDocumentRequest` yang tidak punya field itu → HTTP 400
"Unknown name updateMask.fieldPaths". Semua upsert (profil, playlist,
liked, history) kena — sync pertama setelah login selalu gagal di
`pushProfile`/`pushAll`. Call lain sudah benar: list (GET + pageSize/
pageToken), delete (DELETE), batchWrite (POST `:batchWrite`).

**Perbaikan.** `patchJson()` baru (OkHttp `.patch()`); `upsertDocument`
kini PATCH dengan `updateMask.fieldPaths` di query string; nilai
query (`fieldPaths`, `pageToken`) di-URL-encode. Rules Firestore,
skema, dan console TIDAK diubah. Mesin Go (.aar) TIDAK disentuh.

**Pesan error ramah.** `SyncManager.friendlySyncError()` /
`friendlyLoginError()`: kegagalan sinkron/login/hapus-data-cloud
menampilkan pesan Indonesia singkat ("Sinkron gagal — coba lagi
nanti." / "Tidak ada internet — coba lagi nanti." / "Sesi berakhir,
masuk lagi."); JSON/body HTTP mentah TIDAK PERNAH sampai ke UI —
detail teknis hanya ke logcat (`Log.w`). `SyncSection` menampilkan
`syncState.error` apa adanya, jadi otomatis ramah.

**Layout tombol kartu akun.** `SecondaryButton` (UiCommon) memakai
`heightIn(min = 48.dp)` + teks rata tengah, bukan `height(48.dp)`
tetap: label dua baris ("Sinkronkan sekarang" di setengah lebar HP
kecil) tumbuh dan tampil utuh; pemakaian satu baris lain tetap 48dp,
desain tidak berubah.

**Verifikasi v1.10.2:** stub Firestore ketat (menolak query param
tak dikenal per endpoint, persis binding Google) — bentuk lama 9/9
sesuai harapan (4 upsert gagal 400 dengan pesan SAMA PERSIS seperti
di HP + list/delete/batchWrite/no-auth lolos); bentuk baru 9/9
lolos; harness JVM atas `FirebaseRest.kt` asli: 5/5 call OK dan log
stub membuktikan PATCH + updateMask. Cek endpoint asli dengan token
dummy → 401 untuk bentuk lama MAUPUN baru (Google memeriksa auth
sebelum binding) — jadi 401 bukan bukti bentuk diterima; bukti
bentuknya adalah stub + harness. Sinkron sungguhan dengan akun
Google + Firestore HANYA bisa diverifikasi dari HP user.

### 5.17 Perbaikan playback total dari laporan HP v1.10.3 (tanpa redesign)

Laporan 22 Nov 11:34 WIB: SEMUA lagu gagal, pesan kira-kira "tidak ada
stream untuk lagu ini" — padahal v1.10 memutar lagu di HP yang sama.
Diagnosis membedakan tiga hipotesis dengan bukti (detail penuh:
BUILD-v1.10.3.md): (a) REGRESI v1.10.1 — TERBUKTI; (b) perubahan sisi
YouTube — TIDAK terbukti (probe per-client: VISIONOS OK 10/10, 21–23
format, HLS ada, gate 206; WEB/ANDROID/TVHTML5 sama seperti v1.10.1);
(c) jaringan/timeout — TIDAK terbukti sebagai penyebab total (gate
gagal pada TTFB >12 dtk, terukur, tapi fallback HLS tanpa gate
menyelamatkan resolve).

Akar masalah: `restoreQueue` v1.10.1 membuang `streamUrl` tersimpan
(niatnya menghindari URL basi) tapi MEMPERTAHANKAN `engine` EXOPLAYER
dari JSON; `startTrack` mempercayai `engine` buta-buta dan memanggil
`playExo` langsung, yang gagal instan "Tidak ada stream untuk lagu
ini" TANPA satu pun percobaan resolve — untuk SETIAP lagu antrean
pulihan, mini player, `resume`, auto-advance, dan tombol "Coba lagi"
(`retryCurrent` membuang URL dengan engine tetap EXOPLAYER). Di v1.10
`restoreQueue` mempertahankan URL, jadi track pulihan masih mencoba
putar → error → `retryResolve` → resolve baru.

Perbaikan (`PlayerManager.kt` saja): guard resolve di `startTrack`
(track YTM tanpa `streamUrl` tapi punya `youtubeVideoId` SELALU lewat
`playYtmTrack` dulu; track lokal/unduhan dengan URL `content://` /
`file://` tetap langsung `playExo`); `restoreQueue` menormalisasi
engine ke YOUTUBE bila URL dibuang dan mempertahankan URI lokal/
unduhan; `retryCurrent` kini benar-benar memicu resolve baru. Rantai
format, multi-client, gate, HLS, pesan error, mesin Go, kode sinkron,
dan tema ARUS tidak diubah.

**Verifikasi v1.10.3:** harness JVM atas kode Kotlin asli, batch 10
lagu yang sama — SEBELUM (class v1.10.2) dan SESUDAH sama-sama audio
10/10, video 10/10, lirik 8 sinkron + 2 polos + 0 kosong, search 14
hasil (resolve memang tidak rusak; yang diperbaiki routingnya,
dibuktikan jejak kode + bytecode `startTrack`/`restoreQueue`).
Pemutaran sungguhan di HP (antrean pulihan, Koleksi/riwayat, "Coba
lagi", lokal/unduhan setelah restart) HANYA bisa diverifikasi dari HP
user.

### 5.18 Lima perbaikan v1.10.4 (tanpa redesign)

- **Header identitas Google:** bila login, avatar = foto profil akun
  Google (Coil, inisial sebagai fallback) dan nama = display name akun;
  belum login/logout = "T"/"tuan" seperti sebelumnya; profil tersimpan
  dibersihkan saat login baru supaya ganti akun tidak mewarisi
  identitas lama. Sapaan waktu + emoji 👋 tetap.
- **Sheet menu lagu:** kartu diangkat 216dp di atas chrome bawah
  (strip + mini player + nav) dan tinggi maks dihitung dari tinggi
  layar (lantai 240dp, tetap scroll); palet kertas hangat ARUS
  (`PaperBg`/`SegmentBg`/`InkBlack`/`InkMuted`, like aktif
  `BurntOrange`); label tetap bahasa Inggris; perilaku aksi tidak berubah.
- **Playlist persisten:** pull sinkron tidak lagi menghapus playlist/
  liked lokal yang belum pernah di-push (`shouldDropOnPull`: metaTs > 0
  disyaratkan); merge tetap local-first.
- **Feed global:** Beranda memakai query "viral songs global" +
  "top hits global" dengan konteks gl=US/hl=en; pencarian biasa user
  tetap gl=ID/hl=id. Terverifikasi 10/10 internasional dari VM.
- **Share berlink:** teks share = judul + artis + URL
  `music.youtube.com/watch?v={videoId}` + "(via MIXTUNE)"; tanpa
  videoId → teks saja. Kedua jalur share memakai `shareTextFor` yang sama.

### 5.19 Migrasi seluruh teks UI ke bahasa Inggris v1.10.5 (hanya string)

- Keputusan user 22 Nov 12:17 ("seluruh app inggris"). 307 literal unik /
  365 kemunculan diterjemahkan (tabel lengkap di BUILD-v1.10.5.md §5),
  mengikuti skill `app_string_migration`: inventarisasi tokenizer sadar-komentar,
  peta jalur render, pemeriksaan record tersimpan (tidak ada label Indonesia
  buatan app di storage — tidak ada migrasi data).
- Dipertahankan: nama proper (MIXTUNE, `Garis Arus`), data YTM (judul/artis/
  album), data user (nama playlist, nama akun), substring pencocokan reason
  YouTube + filter footer lirik `Sumber:`, query/parameter API, komentar/KDoc.
- Perubahan perilaku yang disengaja dan dicatat: tile genre `Galau` →
  `Heartbreak` (label sekaligus query); fallback header logout `tuan 👋` →
  `friend 👋`; referensi versi basi di teks About diperbarui ke v1.10.5 (16).
- Verifikasi: residual Indonesia di UI = 0 (sapuan literal + pemeriksaan dex);
  harness JVM batch 10 lagu: audio 10/10, video 10/10, lirik 8 synced + 2
  plain + 0 empty, search 20 hasil; bentuk PATCH sinkron dan merge playlist
  tidak berubah. Tampilan akhir hanya bisa diverifikasi dari HP user.

### 5.20 Jamming Together + perbaikan layout + Garis Arus dinamis + auto-sync + redesign Settings/Browse v1.10.6

- Jam (RTDB, project mixtune-9f676 yang sama; rules auth-only di /jam/$code):
  host buat sesi → kode 6 karakter (alfabet tanpa 0/O/1/I) → tamu join,
  posisi + play/pause sinkron via SSE (fallback polling 2,5 dtk/30 dtk),
  host menulis state tiap perubahan, drift correction > 2 dtk tiap ~5 dtk,
  antrean sesi via POST push key, presence 15 dtk/90 dtk (host 60 dtk),
  leave/end jujur. Offset jam server tidak tersedia via REST (400) → 0.
  UI: pill Jam di Home, bar sesi, LIVE di detail; tamu dikunci dari kontrol
  putar. Verifikasi: harness JVM 53/53, simulasi dua klien 28/28; dua HP
  sungguhan hanya dari HP user.
- Sheet menu: tanpa angka angkat tetap — tinggi maks fraksi layar terlihat,
  clearance = inset navigation bar + 12dp, clip/background sebelum scroll;
  baris terakhir utuh setelah scroll. Pill detail: tanpa scroll horizontal,
  bobot + padding diperketat, muat 360dp. Detail: top padding 0 + statusBarsPadding
  di dalam latar gelap (tanpa pita terang).
- Garis Arus: caption UI dihapus; warna dari palet artwork (kuantisasi manual,
  fallback merah + hijau muda); gelombang strip inset 10dp (lebih pendek,
  sejajar timestamp); gelombang progress fase-terkunci + polling posisi 100ms
  sehingga batas terisi cocok dengan angka timestamp. Poster wave tidak berubah.
- Sinkron: kartu AccountSyncSection dihapus dari semua page; Sign out/Delete
  cloud data jadi baris Settings; auto-sync 60 dtk (foreground, ON_START/ON_STOP)
  + setiap perubahan lokal (debounce 2 dtk, semua jalur mutasi diaudit);
  batas platform: tidak ada background work 1 menit di Android.
- Ikon Settings header: gir kustom gambar tangan (ArusGearIcon), bukan ikon
  template. Settings: seksi Account/Playback/Downloads & Storage/About & Limits;
  kontrol baru yang benar-benar bekerja: sleep timer, kualitas audio (playback +
  download), hitung + hapus unduhan, clear cache Coil. Theme di-SKIP (tema tidak
  terpusat — ArusColors tetap, preferensi tema tidak dibaca).
- Tab detail instan (Box, tanpa AnimatedContent); scroll tiap tab di-hoist;
  intro StaggerIn sekali saja. Lirik: satu ukuran tetap 22sp/28sp, penekanan
  hanya warna; footer Share + caption di dalam alur (bukan overlay).
- Browse: tile ber-artwork YTM (cache di root, fallback warna), seksi Popular now
  + Trending dari feed global Beranda, kategori Podcasts (pencarian podcast —
  YtmResult tanpa penanda tipe), "See all" → halaman semua kategori.
- Verifikasi HP yang masuk: playback jalan (13:08), "Synced • 13:09" membuktikan
  fix PATCH v1.10.2 di HP (13:12), lirik synced jalan (13:17). Tampilan akhir
  hanya bisa diverifikasi dari HP user.

### 5.21 Login persisten + refresh token + strip satu baris + Settings dua tingkat v1.10.7

- Auth: akar masalah terbukti dari kode — (1) `restoreSession()` catch-all
  menghapus sesi pada kegagalan transien; (2) tidak ada retry-on-401 dan
  semua gagal refresh menjadi session-expired + hapus sesi, diperparah
  race field `session` null; (3) bukan wipe versi (tidak ada kode
  penghapus prefs saat upgrade). Perbaikan: klasifikasi
  `RefreshTokenInvalid` (HTTP 400/401 Secure Token) vs `RefreshTransient`
  (IOException/5xx) di `FirebaseRest`; `SessionInvalid` vs
  `SessionTransient` di `SyncManager`; margin refresh 5 menit; refresh
  single-flight dari SecurePrefs (tanpa field memori); retry-on-401
  sekali + ulangi putaran; 403 tidak pernah menghapus sesi; Jam
  `jamCredentials(forceRefresh=true)` sebelum tulis RTDB; hack
  `KEY_ID_EXP="0"` dihapus. Token tidak pernah di-log; hanya SecurePrefs.
- Strip: satu Row (jam–gelombang–jam, gelombang `weight(1f)` 24dp);
  animasi fase mengalir 5000ms/siklus saat bermain, beku saat pause,
  hormat reduced-motion; batas terisi tetap `width*frac` via clipRect.
  Padding bawah konten +8dp (Home/Search/Library).
- Header Home: avatar 48dp, gir 44dp, sapaan 13sp/nama 22sp,
  `maxLines=1` + ellipsis; pill Jam padding 12dp.
- Detail: `nowPlayingCover` 320→256dp + trim spacer (−16dp) → tab +
  1–2 baris isi muat satu layar (anggaran §BUILD-v1.10.7.md); Video 1:1
  utuh. Mini-header seragam: `headerCompact` + `selectTab`; tampilan
  awal tetap penuh; ketuk kartu mini-header kembali ke tampilan penuh;
  `UpNextCompactPanel` untuk tab 0.
- Browse: 13 kategori (Pop, Indie, Heartbreak, Rock, Dangdut, K-Pop,
  Hip-Hop, R&B, Jazz, Electronic, Acoustic, Metal, Podcasts); label =
  query; artwork per kategori di `loadGenreArtworks`; semua di "See all".
- Settings: navigasi dua tingkat — daftar 4 kategori (ikon lingkaran
  `SegmentBg` + ikon `BurntOrange` + chevron) → halaman per kategori;
  kontrol yang sama dipindah verbatim; tanpa kategori/kontrol pajangan;
  Back header + `BackHandler` kembali ke daftar. Versi About v1.10.7 (18).
- Verifikasi: harness auth JVM (fake transport interceptor — sandbox
  memblokir TCP JVM→localhost, dibuktikan Probe.kt) 21/21; audit statis
  9 situs `clearSessionOnly()`; simulasi Jam 28/28. Endpoint sungguhan +
  visual akhir hanya dari HP user.

### 5.22 Hapus 3 blok teks header + perbaikan akar masalah session-expired Jam v1.10.8

- Teks dihapus (hanya itu, tanpa redesign): headline + subjudul Search
  ("Search once, and find it right away." / "Type a title or artist —
  results from YouTube Music appear automatically."); headline + subjudul
  Library ("Your library, all mixed together." / "One collection for
  every song — YouTube Music, offline downloads, and local music.");
  caption Home "ALL SONGS FROM YOUTUBE MUSIC". Spacer sisa dirapikan;
  tombol back tetap; placeholder kolom cari bukan bagian yang dihapus.
  Kelima literal 0 di source UI maupun di semua `.dex` APK kiriman.
- Auth: akar masalah TERBUKTI — `refreshIdToken` membaca `id_token`
  padahal kontrak Secure Token mengembalikan ID token baru di
  `access_token` (setiap refresh → token kosong → start Jam yang selalu
  force-refresh gagal setiap kali); refresh tanpa rotasi `refresh_token`
  dibuang; `withToken` meruntuhkan semua gagal kredensial menjadi
  `SessionGone`. Perbaikan: parse `access_token` (fallback `id_token`);
  pertahankan refresh token lama bila respons tak merotasi;
  `jamCredentialsOrThrow` membedakan `JamAuthInvalid` vs
  `JamAuthTransient`; `withToken` hanya `SessionGone` untuk Invalid,
  Transient → `JamTransient` (backoff tanpa toast/teardown); semua situs
  "Session expired" tersisa hanya jalur `SessionInvalid`. Versi About
  v1.10.8 (19).
- Dialog Jam: `publishUi()` sinkron setelah `sessionScope` diisi di
  `startSession`; dialog memakai `sessionAlive = active && code
  non-kosong` — `LaunchedEffect(sessionAlive)` + pengaman render +
  guard tap "Open session" — panel sesi/kode tidak pernah tampil
  kosong; sesi berakhir → dialog kembali ke menu.
- Verifikasi: harness manager Jam 36/36 (kode asli melawan RTDB
  palsu), simulasi dua-klien 28/28, audit transisi dialog dari
  kode. Endpoint sungguhan + SecurePrefs + visual akhir hanya dari
  HP user.

### 5.23 Perbaikan cacat Jam v1.10.8: `currentCreds` hilang + `ended` tak tertulis v1.10.9

- Akar masalah TERBUKTI (harness mereproduksi fakta HP 16 gagal
  sebelum perbaikan): `startSession` memanggil `teardown()` yang
  mengosongkan `currentCreds` dan tidak pernah mengisinya kembali —
  `withToken` selalu `SessionGone` → toast expired palsu + teardown
  milidetik setelah "Jam started", tambah lagu "You're not in a Jam",
  dialog kembali ke menu; join/presence rusak sama. Perbaikan:
  `currentCreds = creds` segera setelah `teardown()` di `startSession`.
- `endSession`: coroutine PATCH `ended` jalan setelah teardown
  mengosongkan kredensial → flag tidak pernah tertulis; kini kredensial
  ditangkap sebelum teardown (pola `leaveSession`).
- Catch `SessionGone` di `streamLoop` (SSE + polling): lewati toast
  bila sesi sudah tidak aktif (teardown disengaja). Dialog dan
  SyncManager dibebaskan audit — penyebabnya murni di manajer sesi.
- Verifikasi: harness109 36/36 (create/join/401-retry/invalid/
  transient/auth-revoked), simulasi dua-klien 28/28. Versi About
  v1.10.9 (20).

### 5.24 Dua perbaikan Jam v1.10.10: flag hold sticky + session queue = Up Next

- Akar masalah gejala 1 TERBUKTI (audit + reproduksi harness 2 gagal
  sebelum perbaikan): `holdPausedVideoId` di-set saat host pause dan
  tidak pernah dihapus saat host kembali memutar → watch + ticker
  mem-pause tamu lagi dalam hitungan ms, resume diblokir flag basi.
  Perbaikan: flag dihapus total; hold diturunkan dari snapshot
  terbaru di `guestHoldPauseWatch`; ticker disederhanakan ke patokan
  snapshot. Kunci kontrol tamu di UI tidak berubah.
- Gejala 2: antrean RTDB append-only + tamu tanpa queue watch →
  dialog dan Up Next tidak mungkin sama. Perbaikan: fungsi murni
  `upcomingJamQueue` (buang prefix terputar) sebagai satu sumber
  kebenaran; `JamUiState.upcomingQueue` dipakai dialog dan Up Next
  (tampilan penuh + mini-header) selama sesi aktif; tamu read-only,
  host ketuk → `playTrack`; `PlayerManager` tidak diubah; antrean
  lokal utuh di luar Jam.
- Verifikasi: harness110 66/66 (regresi T1–T8 + Q1–Q5 + U1–U9;
  factory `Dispatchers.Main` Java + player palsu autoStart agar
  jalur tamu teruji di JVM), simulasi dua-klien 28/28 (protokol
  tidak berubah). Versi About v1.10.10 (21).

### 5.25 Sheet menu menempel gelap + Add to Jam queue + deep link Jam (v1.10.11)

- Sheet menu lagu ("jelekk banget", "melayang") — akar struktural:
  sheet dirender di dalam padding konten Scaffold sehingga tidak
  pernah mencapai tepi bawah. Perbaikan: `DockedDarkSheet` baru
  berbasis Compose `Dialog` window-level (tanpa lebar default
  platform, tanpa fitsSystemWindows): lebar penuh edge-to-edge,
  bawah menyentuh tepi layar dan menutupi bottom chrome selama
  terbuka, radius sudut atas 16dp, gesture bar jadi padding dalam
  sheet. Gelap pola referensi saja (permukaan `#211D19`, tile
  `#37322C`, sekunder `#D8C9B8`, liked = burnt orange; tanpa aset
  atau branding YTM yang disalin). Header tetap (artwork + judul +
  artis + like + X). Semua aksi lama dipertahankan dan ter-wire
  sama (tidak ada aksi referensi yang ditambahkan); scroll internal
  di layar pendek; Back menutup sheet. Scope hanya TrackSheet.
- "Add to Jam queue": baris aksi setelah "Add to queue", ikon
  `GroupAdd`, hanya saat sesi Jam hidup (gate `sessionAlive` yang
  sama), memakai jalur `addTrackToQueue` yang sama dengan tombol +
  dialog Jam; toast jujur; tanpa entri mati.
- Deep link `mixtune://jam/<KODE>` (custom scheme, tanpa hosting):
  teks share memuat link + kode polos cadangan; intent-filter
  VIEW+DEFAULT+BROWSABLE scheme `mixtune` host `jam` di
  MainActivity; cold start + `onNewIntent`; kode valid →
  `pendingJamCode` dikonsumsi sekali → dialog Join terisi;
  sesi lain → konfirmasi pindah (tidak menimpa diam-diam); belum
  login → login dulu lalu auto-join; tidak valid/tidak ada/berakhir
  → pesan jujur jalur manual. Parse fungsi murni di `JamModel.kt`;
  tanpa perubahan RTDB/backend.
- Verifikasi: harness111 33/33 (parse D1–D24, shareText, join via
  link J1–J9 melawan RTDB palsu), intent-filter terbaca di
  manifest APK, audit layout dari kode. Visual akhir + join 2 HP
  sungguhan hanya dari HP user; link hanya bekerja bila MIXTUNE
  terinstall. Versi About v1.10.11 (22).

### 5.26 Perbaikan delay/jitter sinkron Jam (v1.10.12)

- Gejala HP: delay dua arah host↔tamu (kadang host dulu, kadang
  tamu dulu). Akar (audit kode): (1) publish ganti-lagu terjadi
  saat host masih buffering — tamu mengekstrapolasi dari stempel
  T0 dan bisa mendahului host sebesar waktu muat; (2) publish
  flip fire-and-forget tanpa retry; (3) SSE tanpa watchdog
  (readTimeout 0, soket half-open menggantung, status tetap
  LIVE); (4) posisi dari `updatedAtMs` server dengan
  `clockOffsetMs = 0` permanen → skew jam 1–3 dtk; (5) koreksi
  longgar (ambang 2000ms, ticker 5 dtk, tanpa cap durasi).
- Perbaikan: hook `onAudibleStart` di PlayerManager (READY
  ExoPlayer / state embed) → host publish ulang force saat lagu
  benar-benar berbunyi, dedup per `trackId@generation`;
  publish flip di-retry 2× (backoff 300ms), gagal total →
  flag `publishDegraded` → status RECONNECTING jujur, pulih
  saat flip berikutnya berhasil; SSE readTimeout 45 dtk +
  `SseStalledException` → langsung polling 2,5 dtk tanpa hitung
  failure; `expectedPositionMs` dari `receivedAtMs` lokal
  (baseline hanya maju bila `versionKey` berubah) + cap di
  `durationMs`; ambang `needsSeek` 750ms; `guestDriftTicker`
  2 dtk. Prefetch + publish posisi berkala ditahan.
- Verifikasi: harness112 71/71 (regresi deep link D/J, audible
  A, retry B, stall C, matematika P, drift E); simulasi
  dua-klien 30/30 dengan port Python diperbarui ke aturan baru.
  Batasan: sinkron 2 HP sungguhan + jaringan nyata hanya dari
  HP user; target <1 dtk, <300ms tidak dijanjikan; resolve tamu
  dari nol tiap ganti lagu masih menjadi sisa jitter terbesar.
  Versi About v1.10.12 (23).

### 5.27 Logo "Dua riak, satu sumber" (v1.10.13)

- Pilihan user FINAL (22 Nov 16:51, setelah 7 batch konsep A–U,
  makna "Dengar bareng orang tersayang"): titik pusat + busur oranye
  bercelah di kiri-bawah + cincin charcoal. Redraw vector
  (`ic_logo_ripples.xml`, viewport 108): cincin r=29 stroke 3.2
  `#1B1813` (InkBlack), busur r=15.5 stroke 2.8 `#C14A12` sapuan
  ~300° celah ~60° kiri-bawah (dipertahankan dari konsep), titik
  r=2.2; diskalakan ke zona aman adaptive icon (~64dp).
- Launcher: legacy `ic_launcher.xml` (latar `#FFF8EC` + mark sama) +
  adaptive `drawable-anydpi-v26/ic_launcher.xml` (background
  `@color/paper_bg`, foreground mark). Lockup in-app: mark 56dp di
  halaman About Pengaturan, tanpa teks baru; header Beranda tidak
  ditambah logo. Tanpa perubahan perilaku/fitur.
  Versi About v1.10.13 (24).

### 5.28 Redesign UI Jamming Together, arah C: kartu nempel, tanpa dialog (v1.10.14)
  Dialog Jam DIHAPUS (MENU/JOIN/CODE/SESSION + header/kontennya, tanpa
  dead code tersisa). Pengganti: `JamPinnedCard` di slot bottomBar yang
  sama dengan bar sesi lama (di atas ArusStrip + mini player + nav),
  hanya tampil saat sesi hidup — CIUT satu baris kapsul hitam 56dp
  (dot LIVE + kode + N listening + judul lagu ellipsis + chevron,
  tanpa End/Leave); MEKAR (default sesi baru, state di-hoist ke
  MixtuneApp, reset tiap kode baru): kode 30sp + status koneksi +
  warning hostAway + Share link primer + Now playing (CoverArt +
  judul/artis, state waiting/local-only sama) + Listening + nama
  anggota + Up next (`upcomingQueue`, sama dengan Up Next) + Add
  current song + cari lagu (debounce 600ms, max 6, tombol +) +
  footer Share link / End session / Leave; scroll internal cap 600dp.
  `JamStartSheet`: sheet kecil terpusat — Start a Jam + 6 kotak kode
  inline (satu BasicTextField, dekorasi kotak, IME Done = Join) +
  Join; abis berhasil sheet menutup, kartu muncul MEKAR (tidak ada
  layar kode perantara). Deep link, pendingAction sign-in, gate
  `sessionAlive`, share text, lockout tamu: tidak berubah.
  `JamLivePill` di detail lagu: tap kembali ke layar utama (via
  `onBack`). `JamSessionManager`/`JamRest`/`JamModel` tidak disentuh.
  Harness112 71/71 + sim dua-klien 30/30. Visual kartu hanya dari
  screenshot HP user.
  Versi About v1.10.14 (25).

### 5.29 Batch 1 Jam-sync: prefetch + drift halus + publish berkala (v1.10.15)
  Tanpa perubahan UI/skema RTDB. (1) PREFETCH TAMU: saat sesi hidup,
  tamu me-resolve stream (URL + engine) lagu berikutnya di
  `upcomingQueue` di background ke `StreamPrefetchCache` in-memory
  satu slot, TTL 5 menit; `playYtmTrack` memakai cache bila fresh,
  bila tidak resolve ulang normal (tidak pernah memutar URL basi);
  guard: tidak prefetch bila videoId snapshot berbeda dari lagu
  tamu saat ini (melindungi entri yang ditunggu perpindahan lagu);
  cooldown 30 dtk; host tidak prefetch; di luar Jam tidak ada
  prefetch. (2) DRIFT RATE: `driftCorrection` murni di JamModel —
  selisih 750–2000ms → rate 0.98× (di depan) / 1.02× (tertinggal)
  sampai <250ms lalu 1.0×; >2000ms → seek (ambang 750ms + cap
  durasi v1.10.12 tetap); `setPlaybackRate` satu titik kontrol di
  PlayerManager; reset 1.0f di startTrack/stop/pause/teardown;
  buffering/error → diam; video tamu mengikuti audio seperti
  sekarang. (3) PUBLISH BERKALA: host publish ulang posisi tiap
  10 dtk selama memutar (via `publishFromPlayer(force=true)` di
  `hostSeekWatch`, tanpa retry; berhenti saat pause/lagu
  lokal/leave/end). Harness115 129/129 + sim dua-klien 30/30.
  Batasan: prefetch tidak menghilangkan latensi kirim 0,1–0,5 dtk;
  rate konvergen ~20ms/dtk; bukti 2 HP hanya dari HP user.
  Versi About v1.10.15 (26).

### 5.30 Batch 2 fitur: "Most played" + riwayat pencarian + kartu lirik + swipe-hapus (v1.10.16)
  Temuan inventaris: `recent_tracks` dedup tanpa timestamp/count —
  frekuensi tidak bisa diturunkan darinya. (1) MOST PLAYED: buku
  baru `play_counts_v1` lokal (tidak disinkron) dicatat di
  `startTrack` bersama `addRecentTrack`; `PlayStats.kt` murni
  (urutan hitungan lalu recency, grup artis case-insensitive);
  seksi di atas "Recently played": 5 lagu + 3 artis ("N play(s)"
  riil), tap artis → Cari; kosong → empty state jujur; label
  "Most played" tanpa klaim rentang waktu. (2) RIWAYAT CARI: key
  `recent_searches_v1` lokal; hanya pencarian dieksekusi (tombol
  Search / ketuk hasil), ≥2 karakter, maks 8, dedup
  case-insensitive; seksi di bawah search box, di atas Browse,
  hanya saat query kosong; chip + × + Clear all. (3) KARTU LIRIK:
  `LyricCard.kt` render 1080×1350 (kertas #FFF9ED, oranye
  #FF4E24, teks asli verbatim; synced → baris aktif, plain → 4
  baris awal, kosong → tombol tidak tampil); PNG di cache +
  `FileProvider` (`com.mixtune.app.fileprovider`, `cache-path`
  `lyric-cards/`), ACTION_SEND + `shareTextFor`; tombol di header
  `LyricsPanel`, pill Share bawah tidak diubah. (4) SWIPE-HAPUS:
  `QueueMath.kt` (`indexAfterInsert`/`indexAfterRemove`) dipakai
  `PlayerManager.insertIntoQueue` (Undo); `SwipeToDismissDelete`
  end-to-start, hapus setelah animasi settle; lagu berjalan tidak
  bisa digeser; drag-reorder tidak diubah; swipe nonaktif selama
  sesi Jam (QueueScreen) dan UP NEXT sesi memakai
  `SessionUpNextList` tanpa swipe; Undo via snackbar di
  `MixtuneApp` ("Removed from queue" + Undo, 4 dtk).
  Harness116 33/33 (kode asli). Visual hanya dari screenshot HP
  user. Versi About v1.10.16 (27).

### 5.31 Batch 3 fitur: widget home screen + reaksi di Jam (v1.10.17)
  (1) WIDGET HOME SCREEN: `AppWidgetProvider` satu class, dua
  receiver di manifest (2×1: 110×40dp, sel 2×1; 4×1: 250×40dp,
  sel 4×1; `updatePeriodMillis=0` — di-push sendiri, bukan
  polling); layout `LinearLayout` gelap `#211D19` (pola sheet
  gelap), artwork + judul/artis ellipsis + Play/Pause + Next
  (4×1 tambah Previous); state dari StateFlow PlayerManager yang
  sama dengan notifikasi media (`currentTrack`/`isPlaying`),
  di-push PlaybackService tiap ganti lagu/toggle + onUpdate;
  artwork via Coil (cache yang sama, `allowHardware=false`);
  tombol = PendingIntent ke `PlaybackService` ACTION_* yang sama
  dengan notifikasi (bekerja tanpa buka app); tap lain → buka
  MainActivity; tanpa lagu → empty state jujur ("Nothing playing
  — open MIXTUNE to play something."), bukan lagu contoh; tanpa
  permission baru. (2) REAKSI DI JAM: 4 jenis (heart/fire/clap/
  wow, emoji sebagai konten pesan); node baru `reactions` di
  bawah sesi yang sama (rules auth-only yang sama, tidak diubah;
  POST push key + sentinel `.sv`); dibaca lewat SSE/polling
  yang sama (tidak ada koneksi kedua; `publishUi()` kini dipanggil
  tiap snapshot baru supaya reaksi tampil cepat untuk host
  maupun tamu); tampil sekilas — mengambang ~3 detik di kartu
  mekar + badge ~5 detik di baris ciut, tanpa daftar permanen;
  hanya < 60 detik yang dibaca (`pruneReactions`); rate limit
  lokal 1 per 2 detik (manager + cooldown visual tombol);
  mengirim reaksi bukan kontrol (kunci tamu tetap, playback/
  antrean tidak berubah). Harness117: logika murni + manager asli
  vs RTDB palsu (semua lulus; fake diperluas: POST koleksi
  generik + broadcast SSE). Widget di launcher XOS + reaksi 2 HP
  hanya dari HP user. Versi About v1.10.17 (28).

### 5.32 Redesign Home + Browse + perbaikan Jam + History + App Link Jam (v1.10.18)
  HOME: chip mood (All = 2 query global US/en; mood lain = 1 query
  bertema) memuat ulang feed lewat jalur search yang sama; seksi
  Global hits today (poster rail), Community Picks (kartu tint oranye,
  3 lagu, label jujur — bukan playlist sungguhan), Archive Mix
  (riwayat + play_counts) + Replay Mix (play_counts saja, kosong →
  seksi hilang), Workout, New releases, Your recap (2 kartu warna
  digambar sendiri; tanpa klaim rentang waktu), kartu stasiun gelap
  "Listen together, in sync" → JamStartSheet; poster emas dipensiunkan.
  BROWSE: Explore (3 tile → Charts/Moods & genres/New releases,
  BackHandler + subPage state); grid 12 kategori; Popular/Trending/
  Podcasts/See all tetap. CHARTS: region ID/US via jalur Kotlin paksa
  (forceKotlin — Go mengabaikan gl/hl); Trending 20 + Daily Top (20,
  bukan 100) + Top artists; provenance jujur "Built from trending
  searches on YouTube Music" (tidak ada endpoint chart resmi).
  BOTTOM CHROME: strip dihapus → wave 8dp di bibir mini player
  (matematika + warna + sinkron sama); chip Jam 30dp + sheet overlay
  ~75% (isi kartu mekar verbatim); auto-hide scroll (ambang 36dp,
  ChromeVisibilityState + ChromeScrollEffect, guard sheet/dialog);
  floating pill 32dp hanya saat Jam aktif + chrome hilang; mini player
  TETAP LENGKAP (varian slim ditolak — keputusan desain tercatat).
  JAM: watchdog follow-health tamu (ended/stall/buffering-stall,
  retry maks 3 + status jujur di sheet); hapus Up Next host-only
  (DELETE queue/$key + prune lokal, tanpa Undo); konsumsi antrean
  per push key (queueKey + consumedThroughKey di state; lagu terputar
  tidak pernah muncul lagi); panah chevron disubsumsi chip+sheet;
  header Home tanpa Back + pill padat; mark broadcast (3 drawable,
  geometri §BUILD doc); profil artis (ArtistApi: browse + fallback
  pencarian jujur; kindLabel hanya dari kata subtitle; tanpa angka);
  perbaikan largestThumb (rantai musicThumbnailRenderer langsung).
  HISTORY: pill "History" di slot toggle (sheet pola CommentsPanel);
  isi = deskripsi video + about artis, verbatim + berlabel sumber,
  kosong → empty state jujur; TIDAK ADA sumber sejarah/karangan.
  VIDEO DIHAPUS TOTAL (second ExoPlayer + resolve + UI). LIRIK:
  follow-scroll (effect berkunci tab + hold manual 3 detik + clear
  saat seek/ganti lagu); font Plus Jakarta Sans Regular OFL
  (22sp/28sp seragam, letterSpacing 0.3sp); crossfade 350ms +
  accent bar 4×20dp dalam kolom tetap 14dp. APP LINK: share
  https://moloqo.github.io/mixtune/jam/<KODE>; manifest autoVerify +
  host + pathPrefix; parser menerima https + custom scheme; SHA256
  keystore terverifikasi cocok (E8:3B:1D:93:…:DA:06). Harness118b
  96/96 (fixture JSON nyata); regresi 115/116/118/119 + sim 30/30.
  Visual + perilaku 2 HP + klik WA hanya dari HP user.
  Versi About v1.10.18 (29).

### 5.33 Komentar YTM + label artis + Charts Indonesia + anti-crash key + lirik tengah (v1.10.19)
  KOMENTAR (read-only): endpoint `next` klien WEB (WEB_REMIX mengembalikan
  NOL engagementPanels); panel = engagementPanels[] panelIdentifier
  "engagement-panel-comments-section" → continuation halaman 1;
  reload/append continuation, 20 thread/halaman; parse entity-payload
  (commentKey/surfaceKey/toolbarStateKey → mutations: teks verbatim,
  @handle, waktu verbatim, like-count singkatan verbatim, hearted,
  pinnedText bila ada, avatar terbesar, reply token); balasan jalur
  parse sama (viewModel level-ganda + level-tunggal); TIDAK ADA total
  count; komentar-nonaktif = tidak ada panel komentar; tanpa composer,
  tanpa tombol like/balas, tanpa angka karangan; parser defensif
  (field opsional, thread malformed di-skip); cache memori selama panel
  terbuka, refetch saat dibuka ulang; Kotlin-only (Go engine tidak
  punya komentar). LABEL ARTIS: subtitle kolom kedua di-parse lewati
  label tipe/durasi/tanggal/Explicit ("Lagu • X" → X; "Video" saja →
  ""); berlaku di parseItem Kotlin + ArtistApi (fallback dipertahankan);
  Go parseArtists sudah benar (array artists); render: header detail
  "Unknown artist" tanpa underline/ketuk/navigasi bila kosong; baris +
  mini player + antrean + share dikosongkan/disesuaikan; data lama
  label tipe diperlakukan kosong saat render (tanpa migrasi); guard
  logika (RELATED/mix/Go-to-artist) tidak memakai label. CHARTS:
  gl/hl saja tidak mengubah campuran (query Inggris "trending songs"
  gl=ID → 2/18 Indonesia); chip Indonesia = query Indonesia ("lagu
  trending Indonesia" 14/15 ID, 0 India; "top lagu Indonesia" 11/15);
  Global tetap Inggris; New releases = "lagu baru Indonesia" + ID/id +
  forceKotlin (12/15 ID, 0 India; sebelumnya 0/16); provenance jujur
  dipertahankan. ANTI-CRASH KEY: dedupe id di searchRaw (kedua jalur),
  mergeTracks (id dulu, lalu judul+artis), buildChartData antar-section
  (Daily Top = sisa unik + rank ulang), mostPlayedTracks, recentTracks,
  album/similar ArtistApi; key Charts section-scoped ("trending#…",
  "daily#…", "artist#…"); antrean tetap queueUid (UUID); playlist
  detail memakai id+indeks (pengulangan disengaja). SWIPE: latar merah
  + ikon hanya saat dismissDirection != Settled (progress == 1f saat
  diam — BUKAN gate; offset == 0f ⟺ Settled, terverifikasi bytecode
  Material3); mekanik swipe/Undo/Jam/× host-only tidak diubah. LIRIK
  TENGAH: baris aktif SELALU di tengah viewport (skip hanya bila sudah
  dalam toleransi 16dp dari tengah); offset = viewportH/2 − tinggiItem/2
  dari tinggi item AKTUAL terukur (fallback rata-rata terlihat, lalu
  estimasi 28sp+20dp); contentPadding vertikal setengah viewport agar
  baris tepi bisa memusat; hold dari event sentuh langsung (Press/
  Release + tanpa-pointer-tertekan; tanpa consume; cap 30 detik selama
  menempel, 3 detik setelah lepas) menggantikan watcher
  isScrollInProgress + flag 10.18; font/lineHeight/crossfade 10.18
  tidak berubah; lirik polos tanpa auto-scroll. CAPTION "YT MUSIC"
  dihapus dari TrackRow (satu-satunya renderer caption; 21 call site)
  — URL share, label sumber History, teks About/kredit, dan label
  kualitas audio dipertahankan dengan alasan tercatat. Harness120
  64/64 (subtitle probe nyata + dedupe + offset tengah); harness119c
  36/36; regresi 115/116/118b (97/97)/119 + sim 30/30. Visual +
  perilaku HP hanya dari HP user.
  Versi About v1.10.19 (30).

### 5.34 Anti-crash key tuntas + ghost bar swipe-hapus (v1.10.20)

  Crash `Key "ytm:…" was already used` TERULANG di 10.19 (vc30,
  23:07 WIB, id `ytm:xTvyyoF_LZY`, signature sama dengan crash 10.18):
  perbaikan 10.19 hanya menutup jalur Charts — 15 situs lain masih
  memakai key id-mentah. Defense-in-depth: semua key daftar tampilan
  = `id + "#" + index` (unik secara konstruksi), dedupe perakitan
  dipertahankan sebagai perbaikan data. Audit grep penuh semua
  `items()`/`itemsIndexed()` (LazyRow/Grid termasuk); yang tidak
  diubah: key section-scoped Charts, `queueUid` antrean, situs yang
  sudah ber-suffix, key enum/count/index. Ghost bar merah + ikon
  setelah swipe-hapus di Up Next detail: `ReorderableUpcoming`
  (Column manual, kedua panel detail) tanpa `key()` memakai ulang
  slot posisional — dismissState baris terhapus diwarisi baris
  berikutnya; diperbaiki `key(next.queueUid)`; indeks hapus + Undo
  dieksonerasi (konfirmasi HP "beneran hilang" + QueueMath). Gate
  background: `getDismissDirection()` (javap Material3 1.3.0) =
  Settled ⟺ offset 0f/NaN; gate `!= Settled` → tidak dilukis saat
  diam; saat swipe konten bergerak bersama offset, ikon diam di
  belakang. Harness121 59/59 (dedupe perakitan + simulasi key +
  model slot + gate); regresi 115/116/118b (97/97)/119/119c/120 +
  sim 30/30. Visual + perilaku HP hanya dari HP user.
  Versi About v1.10.20 (31).

### 5.35 Lima fitur satu batch: kelola playlist + multi-select + ikuti artis + lanjutkan posisi + link YTM (v1.10.21)

  Pilihan user "4,6,7,9,10" + "langsung aja jadiin 1 batch" —
  Batch B dibatalkan, satu APK. #4 kelola playlist: rename dialog
  (blank/duplikat ditolak jujur, validasi murni), hapus dialog
  konfirmasi (playlist saja), drag-to-reorder di detail playlist
  (pola antrean, urutan persisten); sinkron TIDAK di-refactor —
  tombstone + LWW sudah benar (savePlaylists diff → push hapus
  dokumen tombstone → pull lewati tombstone; shouldDropOnPull
  murni), batas LWW dokumen utuh didokumentasikan. #9 multi-select:
  long-press di Home/Cari/Library/profil artis/Up Next/Queue,
  kunci `scope#id#kemunculan` (tidak bocor antar-daftar),
  SelectionActionBar gelap (counter + Add to queue/Save to
  playlist, hitung jujur), bulk-queue disembunyikan saat Jam,
  drag/swipe mati saat mode; long-press dimiliki drag di antrean
  detail + detail playlist → masuk mode lewat tombol Select.
  #6 ikuti artis: Follow/Unfollow di header profil (LOKAL saja);
  rak Home "New from artists you follow" dari top songs profil
  (3/artis, round-robin + dedupe, maks 10, cache/artis TTL 1 jam,
  maks 5 artis/refresh, tanpa background work). #7 lanjutkan
  posisi: track ≥10 menit, simpan tiap 5 dtk + pause/stop, pakai
  bila >10 dtk dan < durasi−15 dtk, hapus bila hampir habis/seek
  <10 dtk, nonaktif saat Jam (gate PlayerManager), hanya jalur
  ExoPlayer. #10 link YTM: intent filter chooser TANPA autoVerify
  (filter App Link Jam tidak disentuh), parser murni (v= pertama,
  youtu.be, playlist-only → toast jujur), resolve `/player` +
  buka detail; malformed → toast tanpa crash. Harness122 78/78
  (dua ekspektasi awal salah hitung, diperbaiki dari trace
  algoritma); regresi 115/116/118b/119/119c/120/121 + sim 30/30.
  Visual + perilaku HP hanya dari HP user.
  Versi About v1.10.21 (32).

### 5.36 Lirik: easing gulir + crossfade ala GETAR, port jujur (v1.10.22)
  Pemicu: user menyukai animasi + stay-tengah + scroll/easing lirik di
  prototype GETAR. Diff dari source prototype (inspeksi 3a51e00c):
  prototype TIDAK punya mekanisme lirik apa pun — daftar statis 6 baris
  tanpa timestamp, baris indeks 2 hardcode aktif, tanpa scroll/easing/
  transisi; kesan tengah berasal dari layout statis + field mengalir.
  Tidak ada kurva prototype untuk disalin. Yang di-port (dalam aturan
  lirik: font/lineHeight tetap, penekanan warna/opacity saja): gulir
  follow `animateScrollBy` jarak terukur + tween `FastOutSlowInEasing`
  (CubicBezier 0.4, 0.0, 0.2, 1.0 — padanan standar Material, BUKAN
  salinan nilai prototype) + durasi proporsional jarak 300–600ms
  (`centerDistancePx`, `followScrollDurationMs`, murni di
  `LyricsFollowState`); fallback item tak terlihat tetap
  `animateScrollToItem` + offset tengah; crossfade warna baris + bar
  aksen 350ms → `Motion.slowMs` 450ms. Penekanan ukuran/bobot ala
  prototype (19px/800) DITOLAK (aturan 10.6 + keluhan 13:19); rata
  tengah horizontal prototype TIDAK diadopsi (desain mapan); hold
  sentuh, seek-recenter, lirik polos, per-kata: tidak berubah.
  Harness123 55/55 (jarak, durasi, easing, simulasi playback/hold/
  seek/cepat/polos/ganti lagu/anti-jitter/fallback); regresi
  115/116/118b-sebagian/119/119c/120/121/122 (118b terhenti di fixture
  /tmp yang hilang — blokir lingkungan, bukan regresi). Visual +
  perilaku HP hanya dari HP user.
  Versi About v1.10.22 (33).

### 5.37 Follow lirik closed-loop + anti-crash key seksi (v1.10.23)
  Dua perbaikan. (1) Lirik: video 10.22 membuktikan baris aktif
  beku di tepi bawah selama satu baris panjang lalu lompat
  multi-baris. Audit: hold HANYA dari sentuh (bukan status
  scroll), jam posisi 100ms segar, satu efek, matematika tengah
  benar, jeda antarbaris Espresso (LRCLIB) 2,1–4,7 dtk — cacatnya
  adalah efek per-baris yang restart tiap pindah baris dan
  menggulir satu tembakan tanpa verifikasi, sehingga satu
  shortfall transien meninggalkan baris di luar tengah sampai
  baris berikutnya. Perbaikan: worker persisten (restart hanya
  tab/lagu) + target lewat Channel CONFLATED + gulir lalu
  verifikasi dari layout segar dan koreksi (maks 2, jeda settle
  50ms) sampai di tengah; hold digabung (gulir sekali ke target
  terbaru), seek-jump tetap membatalkan hold; easing/durasi
  10.22, crossfade 450ms, font tetap, penekanan warna/opacity
  saja. (2) Crash Library: satu LazyColumn menampung Most played
  + Top artists + Recently played + Local dengan key `id#index`
  tanpa prefix — indeks restart per blok, lagu yang sama di
  indeks lokal yang sama pada dua seksi bertabrakan persis
  (`ytm:xTvyyoF_LZY#1`, id yang sama dengan crash 10.19).
  Perbaikan: key ber-prefix seksi (`mostplayed:`/`topartist:`/
  `recent:`/`local:`, plus pengerasan `jamsearch:` di Jam dan
  `similar:` di profil artis); isi tidak di-dedupe. Harness124
  34/34 (repro crash + simulasi lama-vs-baru di timing Espresso
  asli + hold/seek/cepat/polos); regresi 115/116/119/119c/120/
  121/122/123 lulus (118b terhenti di fixture /tmp yang hilang —
  blokir lingkungan, bukan regresi). Visual + perilaku HP hanya
  dari HP user.
  Versi About v1.10.23 (34).

### 5.38 Polish layar detail + animasi (v1.10.24)
  Layar detail saja, tanpa perubahan perilaku, identitas ARUS
  tetap. Pemicu: screenshot HP user 23 Nov 04:25 WIB (Espresso,
  tab UP NEXT, jeda di 0:00) + permintaan "polished design
  disini dan perbanyak animasi". Polish: (1) pill "Comments"
  terpotong jadi "Commen" di lebar ~336dp — padding 10→8dp,
  spacer 4→3dp, label 13→12sp + ellipsis (model aritmatika
  harness125: lama 98dp > 93,3dp tersedia, baru 89dp muat;
  ≤320dp terdegradasi anggun via ellipsis); (2) Garis Arus lebih
  hadir (tinggi 28→32dp, amplitudo 5→6dp); (3) aksi "View full
  queue" menempel ke empty state (padding vertikal 12→8dp);
  (4) latar panel mini-header menganimasikan antar tab (300ms);
  (5) baris tab ikut intro berjenjang (indeks 5, total tetap
  525ms ≤ anggaran 600ms). Animasi — semua terikat state nyata,
  spec di data/DetailMotionSpec.kt (diuji harness125 50/50):
  morph ikon play/pause 150ms FastOutSlowIn (tombol besar +
  mini-header; pembungkus key dihapus supaya morph beranimasi;
  instan saat reduced-motion); skala tekan spring (transport
  0,88 + tint 200ms + titik aktif 4dp shuffle/repeat; pill 0,94;
  tab 0,97 + warna label 200ms); SATU indikator tab yang
  bergeser 300ms (garis dasar 1dp tetap); isi panel tab
  crossfade 180ms + geser 8dp (keluar 120ms; state di-hoist,
  tanpa reload); napas artwork 1,0↔1,035 selama 5,2 dtk hanya
  saat lagu jalan (jeda kembali halus 450ms; diam saat
  reduced-motion); amplitudo gelombang responsif main/jeda
  (6dp↔3dp, 450ms, opt-in hanya call site detail — strip mini
  player tidak berubah). Loop ambient hanya hidup saat lagu
  jalan + gerak diizinkan (tanpa recomposition 60fps saat jeda).
  BATAS KERAS: perilaku lirik 10.23 (worker + verifikasi,
  font 22sp/28sp, warna/opacity saja) TIDAK disentuh; Jam,
  sheet, menu, komentar, History, antrean tidak disentuh.
  Verifikasi visual sandbox tidak tersedia untuk Compose —
  aritmatika layout + spec diuji harness, keindahan akhir
  dinilai screenshot HP user.
  Versi About v1.10.24 (35).

### 5.39 Feed rekomendasi Indonesia (v1.10.25)
  Kode feed/query SAJA, tanpa redesign UI, tanpa perubahan perilaku
  di luar komposisi feed, identitas ARUS tetap. Pemicu: laporan user
  23 Nov 04:26 WIB — terlalu banyak lagu India di rekomendasi,
  Browse, dll. Root cause: feed Beranda/Browse memakai feed GLOBAL
  gl=US/hl=en (query Inggris generik) sejak v1.10.4; Charts
  Indonesia sudah dilokalisasi di 10.19 tapi feed utama belum.
  Perbaikan mengikuti pola 10.19 yang terbukti (query berbahasa
  Indonesia + gl=ID/hl=id + jalur Kotlin dipaksa, karena mesin Go
  mengabaikan gl/hl): feed Beranda ("lagu viral Indonesia" /
  "lagu terpopuler Indonesia"), semua chip mood ("lagu senang /
  santai / perjalanan / semangat Indonesia"), Community Picks
  ("lagu favorit Indonesia"), Workout ("lagu workout Indonesia").
  Semua call site memakai forceKotlin=true supaya jalur yang
  diprobe-lah yang jalan. Probe live jalur Kotlin 23 Nov 2026
  (harness125/feeds_probe*.json, heuristik klasifikasi TERBUKA yang
  sama dengan 10.19): SEBELUM 4 IN / 1 ID / 109 other (n=116);
  SESUDAH 0 IN / 84 ID / 31 other (n=115). BATAS KEJUJURAN: jalur
  Go (yang dipakai HP sebelum 10.25) tidak bisa diprobe dari VM
  (libgojni.so hanya arm64-v8a) — angka probe mengukur jalur
  Kotlin, dan forceKotlin membuat app memakai persis jalur itu;
  campuran di HP adalah arbiter akhir. TIDAK berubah: pencarian
  yang diketik user (default ID/id, Go-first), chip Global Charts
  (query Inggris + gl=US/hl=en, tetap terjangkau dan berlabel),
  artwork tile genre (US/en, hanya gambar), endpoint komentar,
  Related (berbasis artis), data lokal (riwayat/play_counts).
  TIDAK ADA heuristik artis/bahasa/ethnis atau blocklist — hanya
  lokalisasi region + query. Label jujur: poster Beranda "Global
  hits today" + kicker "The best global tunes right now" menjadi
  "Hits today" / "The best tunes right now" (hanya kata, tanpa
  perubahan layout). Query/param diuji harness126 41/41
  (reproduksi hitungan probe dari fixture + tabel query).
  Versi About v1.10.25 (36).

### 5.40 Theme switcher: 4 tema x 2 mode (v1.10.26)
  Permintaan user 23 Nov 04:42–04:43 WIB: theme switcher di
  Settings, 3 tema baru yang bener-bener berbeda, tiap tema punya
  dark + light mode. ARUS (default) + Senja Gelap + Blok + Getar;
  PITA dicoret (user: "kurang"), Galeri dicoret (terlalu mirip
  ARUS). SATU tabel token di data/AppTheme.kt (murni Kotlin,
  diperiksa harness127 kontras WCAG 153/153): Senja Dark +
  Blok Light dikutip persis dari CSS prototype (lihat BUILD
  doc); ARUS Light = token persis v1.7–v1.10.25; GETAR =
  tinta #0E0E14 + field seeded per lagu + Archivo/Mono.
  Nama lama dipertahankan sebagai getter @Composable; peran
  ganda dipisah (teks vs StageBg/ChromeBg, aksen vs AccentFill,
  aksen panggung gelap DetailAccent). Field GETAR mengalir
  hanya saat jalan (Animatable, settle saat jeda, statis saat
  reduced-motion) di poster tanpa artwork + latar detail.
  Font OFL dibundel: Bricolage Grotesque ExtraBold (Blok),
  Archivo Bold (Getar), Plus Jakarta Sans Bold (Senja).
  Prefs theme_id/theme_mode (fallback ARUS/Light); state di
  root, terap instan tanpa restart. TIDAK bertema: widget,
  notifikasi, kartu lirik, panggung detail (tetap gelap),
  warna dekorasi tetap. Versi About v1.10.26 (37).

### 5.41 Structural themes: whole visual system per theme (v1.10.27)

  Koreksi atas verdict HP user 23 Nov 11:22 WIB ("cuman ganti warna
  doang"): v1.10.26 menokenisasi WARNA saja, satu sistem bentuk/
  tipografi/spacing dipakai 8 varian. v1.10.27 mem-port perbedaan
  STRUKTURAL prototype ke app: tiap tema punya identitas bentuk,
  tipografi, densitas, treatment komponen, dan karakter gerak
  sendiri; mode Light/Dark hanya membalik luminansi — identitas
  bertahan di kedua mode (harness128: vektor struktural identik
  antar-mode per tema, background berbeda).
  Bentuk (dp, per tema — card/tile/chip/sheet/button/miniPlayer):
  ARUS 16/16/999/28/999/999 (lunak, poster 28, pill 28); Senja
  10/10/999/20/999/20 (lebih tajam, poster 16, rowCover 8); Blok
  6/6/8/6/8/8 (hampir kotak, sesuai spesifikasi koreksi — radius
  22px prototype TIDAK dipakai); GETAR 12/12/10/12/10/12.
  Tipografi: ARUS Plus Jakarta Sans display seperti sekarang;
  Senja display + body Plus Jakarta Sans; Blok Bricolage
  ExtraBold + judul display UPPERCASE (hanya judul display/
  section — judul lagu & nama orang tidak pernah di-caps) ukuran
  32/40/38 + letterspacing display -0.5sp; GETAR Archivo display
  + JetBrains Mono (baru dibundel, Regular+Bold, OFL) untuk label/
  metadata dengan letterspacing 1.5sp, ukuran 22/34/32.
  Densitas: Senja lebih rapat (row 6/64dp, grid 8, section 6/16);
  Blok lebih chunky (row 12/76dp, grid 14, section 14/28, cardPad
  16); GETAR whitespace lega (section 14/26, cardPad 18, row
  11/72dp, grid 12); ARUS baseline (8/72, grid 10, 10/24).
  Treatment: Blok border 2dp + hard shadow offset (poster 5,
  tile 4, chip 3dp) + divider 2dp solid; Senja glow luar 12dp/
  35% pada elemen aktif/berjalan + divider 1dp solid; GETAR
  hairline 1dp + GetarField di latar detail DAN header Home
  (seeded per track, mengalir hanya saat jalan, statis saat
  reduced-motion); ARUS treatment hari ini (wave Garis Arus,
  nav kapsul, poster editorial). Gerak per tema (morph/tab/
  panel/exit/tint/waveAmp ms + easing): ARUS 150/300/180/120/
  200/450 FastOutSlowIn (= konstanta legacy 10.24); Senja
  195/390/235/155/260/580 FastOutSlowIn; Blok 110/225/135/90/
  150/340 LinearOutSlowIn; GETAR 160/200/140/100/160/400 Linear.
  Urutan section & hierarki info SAMA di semua tema; yang beda
  styling/densitas/treatment. Perilaku identik (navigasi, feed,
  lirik follow, Jam, sync, komentar). TIDAK bertema (tetap):
  widget, notifikasi, kartu lirik, panggung detail. Harness128:
  200 checks + 13 guard sumber (tidak ada literal bentuk/gerak
  hardcoded di luar tabel token); regresi 115–127 hijau.
  Versi About v1.10.27 (38).

### 5.42 Follow lirik: basis koordinat konten (v1.10.28)
  Perbaikan cacat basis yang lolos 10.19–10.27, dibuktikan video HP
  (baris aktif menempel di tepi bawah daftar, osilasi ~3 dtk) + source
  Compose 1.7.0: `LazyListItemInfo.offset` diukur dari AWAL KONTEN
  (setelah beforeContentPadding) — `visualOffset` baru ditambahkan
  saat `place()`; `viewportStartOffset = −beforeContentPadding`.
  Rumus lama mengasumsikan offset dari tepi viewport sehingga tiap
  gulir undershoot tepat sebesar padding dan loop verifikasi (basis
  yang sama salahnya) melaporkan "tengah" di titik tetap yang salah
  (pusat layar = H). Sim harness123/124 lama buta karena dua galatnya
  (model offset + rumus) saling meniadakan. Ketiga fungsi pemusatan
  kini menerima `beforeContentPaddingPx` eksplisit dari `layoutInfo`;
  pusat viewport dalam koordinat konten = H/2 − pad (= 0 untuk
  halfViewport); `centerScrollOffset` = pad + size/2 − H/2 (bertanda,
  tanpa clamp). Desain halfViewport dipertahankan; hold/seek/easing/
  toleransi/tampilan lirik tidak berubah. Harness129: 41 checks +
  guard (repro rumus lama = tepi bawah + verifikasi buta; rumus baru
  memusat di timing Espresso penuh, densitas tema, teks duplikat,
  seek, masuk tab, baris pertama/terakhir, hold+resume, perubahan
  cepat, shortfall). Harness120/123/124 ditulis ulang ke model benar
  (OldController 124 memakai replika verbatim rumus lama); harness125
  dipulihkan (AppTheme.kt masuk daftar kompilasi — kerusakan pra-ada
  10.27). Regresi 115–129 hijau (118b fixture-blocked pra-ada).
  Versi About v1.10.28 (39).

### 5.43 Klaster bawah SLIM (v1.10.29)
  Pilihan user dari POC (varian Slim, 12:18 WIB): total tumpukan
  122dp (wave 6 + mini player 54 + gap 6 + nav 48 + inset 8; dengan
  chip Jam 156dp), hemat 47dp vs kode lama (169dp). Semua kontrol
  dipertahankan: cover 38dp, play 44dp, sentuh like/prev/next 40dp,
  judul 14sp/artis 12sp, ikon nav 22dp/label 11sp, pill nav 40dp,
  chip Jam 28dp. Ukuran hidup sebagai 22 field `cluster*` di
  `AppThemePalette`, identik di ke-8 varian (identitas tema dibawa
  radius/border/glow/font — Slim keputusan global, bukan per tema);
  `Sizes` klaster lama + `Type.navLabel` dihapus, satu sumber lewat
  `BottomCluster`. Perilaku tidak berubah: seek wave, putar/flip/
  hero cover, swipe prev/next, like, auto-hide, treatment per tema,
  reduced-motion. Harness130: 61 checks + guard (tumpukan/invariance/
  sentuh/geometri 360/412dp). Regresi 115–129 hijau.
  Versi About v1.10.29 (40).

### 5.44 Kapsul navigasi sedikit lebih besar (v1.10.30)
  Permintaan user 13:26 WIB (lingkaran merah hanya pada kapsul
  navigasi, mini player tidak dilingkari): nav 48 → 56dp, pill
  item 40 → 48dp, ikon 22 → 24dp, label 11 → 12sp — token tabel
  saja, nilai identik di ke-8 varian, padding vertikal kapsul
  tetap 4dp (2×4 + 48 = 56). Total tumpukan 130dp (wave 6 +
  mini player 54 + gap 6 + nav 56 + inset 8; dengan chip Jam
  164dp), hemat 39dp vs kode lama. Mini player, wave, gap, inset,
  chip Jam, follow lirik 10.28, Jam/sync, komentar, feed, dan
  identitas tema TIDAK berubah. Reproduce-first: ekspektasi
  harness130 diperbarui dulu dan gagal 22 checks pada kode lama,
  lalu lulus 61/61 + guard pada kode baru (diferensial yang
  sama, input yang sama). Regresi 115–130 hijau.
  Versi About v1.10.30 (41).

### 5.45 Durasi lagu dari lookupVideo + background detail per lagu (v1.10.31)
  Dua laporan user 13:27 WIB: (a) durasi "--:--" di list; (b) minta
  background detail berbeda tiap lagu. Akar (a), dibuktikan probe
  respons mentah WEB_REMIX (fixture `harness131/fixtures/
  search_raw.json`, 30 renderer): TIDAK ADA field durasi di respons
  (0 string mirip-durasi, 0 `lengthText`, `fixedColumns` absen) —
  subtitle lagu seragam `"Lagu • <Artist>"`; `Track` lahir dengan
  `durationMs = 0` dan salinan list tak pernah di-backfill. Perbaikan:
  `TrackDurations` (cache per videoId, 1 worker, jeda 120ms,
  dedupe + cooldown gagal 60 dtk / nol 600 dtk, cap 500, persist
  prefs `duration_cache_v1`) yang lazy-resolve baris terlihat via
  `lookupVideo`/`lengthSeconds` (probe: 6/6 OK, fixture
  `player_lengths.json`) — TIDAK memakai ulang `resolveStream`;
  `TrackRow`, `DetailTrackRow` (gate hide Jam dipertahankan),
  `TrackActionSheet` mengonsultasi cache; `RankedTrackRow` dan
  Jam tak berubah; durasi hanya tampil bila nyata, sisanya jujur.
  (b): panel + mini-header + gradien stage kini SATU keluarga
  turunan artwork — `ArtworkColors` (murni): rata-rata cover
  (64×64, cache Coil bersama, tahan-warna-sebelumnya) diskalakan
  ke luminans ≤ 0.055 (panel) / mini-header ≤ 0.09, hue
  dipertahankan; fallback warna tetap bila tanpa artwork/gagal;
  animasi 300–450ms; foreground tetap; flip mode tidak mengubah
  warna turunan; GETAR/`StageBg`/klaster bawah tak berubah.
  Harness131: 103 checks (kontras WCAG ketiga foreground tetap
  atas 6 rata-rata nyata + 7 tepi, termasuk putih murni; plafon
  0.085 pertama GAGAL aksen lalu diperketat) + diferensial
  parser 0/30 lama vs 6/6 baru + guard. Regresi 115–130 hijau.
  Versi About v1.10.31 (42).

### 5.46 Label nav terpilih di mode gelap (v1.10.32)
  Laporan HP 15:09 WIB: label nav terpilih tak terlihat di mode gelap.
  Akar: `CapsuleNav` meng-hardcode `Color.White` untuk label terpilih
  di atas pill `ChromeBg` yang membalik antar mode — di ARUS Dark
  (1.05:1) dan BLOK Dark (1.12:1) putih-di-atas-kertas praktis
  invisibel. Perbaikan: label terpilih memakai `OnChrome` (token yang
  sama dengan ikon terpilih + chip mood/region), 13.02–17.63 di 8/8
  varian. Sibling `FilterChipYtm` (kode mati) yang meng-hardcode
  putih di atas `accentFill` ikut memakai `OnAccent` (cacat kelas
  sama; pasangan aksen ARUS/BLOK 3.30/3.43 adalah identitas brand
  bersama tombol play — di luar cakupan). Tabel token byte-identik;
  ukuran/treatment/identitas tema tak berubah. Harness132 diferensial
  (10 failures kode lama → 0 kode baru) + regresi 115–131 hijau.
  Versi About v1.10.32 (43).

### 5.47 Halaman genre khusus (v1.10.33)
  Permintaan user 15:09–15:12 WIB (video referensi aplikasi YouTube
  Music + 3 klarifikasi): ketuk genre/mood membuka HALAMAN GENRE
  khusus (header + seksi Songs, Featured playlists, Community
  playlists, Music videos, Albums dalam satu guliran), bukan daftar
  hasil search; layout tile genre DIBIARKAN; di tab Search hanya 4
  genre yang tampil + View all/Show less; tiap artwork tile 1:1.
  Data: jalur utama browse kategori YTM (FEmusic_moods_and_genres_
  category + params opak yang ditemukan saat runtime dari respons
  Explore FEmusic_explore dan di-cache per sesi — params tidak bisa
  dibangun dari label; alias label Inggris→tombol Indonesia
  terdokumentasi di GenreApi, label tanpa tombol jatuh ke fallback).
  Jalur fallback: pencarian biasa, hanya seksi Songs (seksi
  playlist/album tidak dibuat-buat). Parsing MURNI di `GenreApi`
  (rak dicocokkan dari judul yang diketahui ID+Inggris; rak tak
  dikenal dilewati). Tile: `aspectRatio(1f)` ganti height 104.dp
  (lebar dari weight, layout 2 kolom tidak berubah). Grid Browse:
  4 pertama saat ciut, semua saat View all; halaman "All categories"
  terpisah dihapus (diganti ekspansi inline + Show less). Ketuk
  playlist/album = putar isi koleksinya (browse VL…/MPRE… + parse
  track, toast jujur bila gagal); ketuk video = putar audionya;
  baris lagu memakai TrackRow + cache durasi 10.31. Chip mood
  Beranda TIDAK disentuh; nav/lirik/Jam/komentar/feed/durasi/
  background detail/identitas tema tidak disentuh. Header memakai
  scrim 0.55 yang sama dengan tile supaya judul putih terbaca.
  Harness133 diferensial (parser REAL atas fixture REAL: 28/28 +
  source checks 12 failures kode lama → 0 kode baru) + regresi
  115–132 hijau. Versi About v1.10.33 (44).

### 5.48 Tombol Back terlihat + main dari Up Next Jam + autoplay Jam (v1.10.34)

  Laporan HP 20:58 WIB (Settings, ARUS Dark, build 10.26): tombol
  Back lingkaran terang dengan panah nyaris tak kelihatan. Akar:
  `ArusBackButton` memakai `InkBlack` (token teks yang membalik
  antar mode) di atas lingkaran `Color.White` paten — 4 varian gagal
  (ARUS Dark 1.05:1, SENJA Light 1.14:1, BLOK Dark 1.12:1, GETAR
  Light 1.15:1). Perbaikan token-level: panah memakai `InkFixed`
  (17.70:1 di semua varian); varian gelap tidak berubah (putih di
  atas putih-transparan: 9.60–13.32:1). Cacat kelas yang sama
  diperbaiki di lingkaran play putih kartu koleksi (HomeSearch).
  Audit penuh 8 varian (harness134, token REAL): teks primer
  >= 4.5, muted >= 3.0, teks aksen >= 3.0 (bar kiriman 10.26),
  onAccent >= 3.0 (identitas brand, tidak diubah), permukaan gelap
  tetap >= 4.5. Scrim bawah 0.55 ditambahkan di tile genre TANPA
  artwork (label putih di atas warna fallback terang: Hip-Hop
  1.79:1 → 7.34:1) dan di CollectionCard (emas 1.83:1 → 7.42:1;
  kartu "Create playlist" bertitel token dikecualikan). Pengecualian
  terdokumentasi yang DIBIARKAN: judul putih display di atas emas
  poster ARUS 1.83 (treatment sejak v1.7), placeholder CoverArt
  emas, error BLOK Light 4.30 (marjinal, hue di luar scope).
  Ukuran/bentuk/hue/radius/tipografi/densitas/treatment tidak
  berubah; tabel token byte-identik.

  Main dari Up Next Jam (permintaan 20:59 WIB): baris Up Next di
  kartu Jam (sebelumnya TANPA handler ketuk) dan di tab detail kini
  bisa diketuk. Host melompat langsung lewat
  `JamSessionManager.jumpToQueueItem` — memutar SALINAN antrean
  lokal yang dipetakan ke push key item (bukan playTrack per id,
  supaya salinan duplikat tidak tertukar), lalu publish paksa lewat
  jalur yang sama (tamu mengikuti, tanpa langkah tunggu baru).
  Tamu mengirim permintaan lompat ke node baru `jumpRequests`
  (pola reaksi, rules auth-only yang sama, TTL 30 detik); host
  menerapkannya lewat `hostJumpRequestWatch` lalu menghapus
  child-nya (sekali pakai, tidak pernah desync — tamu TIDAK PERNAH
  memutar lokal). Ketuk lagu berjalan = no-op jujur. Hapus
  host-only + tanpa Undo saat Jam tidak berubah. Baris memakai
  `clickable` biasa saja — tanpa long-press/drag — supaya handle
  reorder v1.10.36 bisa hidup berdampingan; identitas baris tetap
  push key (stabil melewati lompat/autoplay).

  Autoplay fallback Jam (tambahan 21:00 WIB): lagu berakhir natural
  saat sesi aktif dan Up Next KOSONG → host memilih satu lagu nyata
  dari feed Beranda (query + jalur search yang sama dengan Home
  10.25, forceKotlin, gl=ID/hl=id), melewati yang sudah dimainkan
  sesi ini / masih di antrean / baru berakhir, resolve-nya harus
  berhasil (maks 5 kandidat) sebelum di-commit ke antrean sesi +
  diputar. Antrean user selalu menang (tidak kosong = diam);
  pause manual tidak memicu (hook hanya di `onTrackEnded`,
  bukan repeat-one); hanya host (tamu tidak pernah autoplay
  lokal); gagal total = toast jujur sekali. Pemicu = hook baru
  `PlayerManager.onTrackEndedNatural` (ExoPlayer + embed). Tidak
  ada perubahan timing sinkron/polling/prefetch/resolve di build
  ini — laporan delay pergantian lagu ~10 detik (21:00 WIB) adalah
  milik v1.10.35; model latensi di harness134 (SSE+prefetch 450ms,
  polling+cold-resolve 4.4 dtk, terburuk 7 dtk) HANYA menjelaskan,
  bukan mengukur HP. Harness134 diferensial (source checks 15
  failures kode lama → 0 kode baru; rasio + protokol + pick +
  model latensi ALL PASS) + regresi 115–133 hijau. Versi About
  v1.10.34 (45).

### 5.49 Delay pergantian lagu Jam diperbaiki + versi About runtime (v1.10.35)

  Laporan HP 21:00 WIB (2 HP): host sudah ~10 detik di lagu baru,
  tamu masih di lagu sebelumnya. Rantai diukur dari kode nyata +
  probe (harness135/probe_resolve.py): publish host BUKAN lambat
  (ratusan ms); akar DOMINAN = prefetch 1 slot (10.15) yang hanya
  mencakup lagu pertama Up Next — lompat ke lagu ke-2/ke-3,
  autoplay, atau lagu luar antrean = cold resolve sekuensial
  (Go + sampai 2 ronde Kotlin × 4 klien, gate Range 10/12 detik;
  probe VM: visionOS hangat ~240ms / dingin 1.000ms, WEB ~255ms,
  RTT RTDB dingin 1.571ms; gate Range tak terukur dari VM,
  bukti 10.1 berlaku: first byte ~6 detik di jaringan lambat).
  Akar kedua = celah delivery: deteksi stall SSE 45 detik, backoff
  reconnect, polling 2,5 detik. Simulasi kebijakan LAMA mereproduksi
  laporan: SSE drop + lagu ke-3 + jaringan sedang = 10.900ms.
  Perbaikan: cache prefetch multi-slot (4 slot, TTL 5 menit,
  synchronized, take per entri); `PREFETCH_TARGET_COUNT = 3` +
  cooldown 10 detik (dulu 30) + fungsi murni
  `prefetchTargetVideoIds`; `guestApplyLoop` menendang prefetch lagu
  baru SEBELUM apply Main-thread (guard state-belum-diterapkan
  dipertahankan); backoff SSE 500ms, polling fallback 1.500ms,
  safety poll tamu tiap 3 detik (GET hanya bila tak ada snapshot
  3 detik, via `lastSnapshotAtMs`); single-flight resolve di
  PlayerManager (`sharedResolve`, prefetch + putar berbagi satu
  resolve; `retryResolve` tetap segar langsung). Hasil model:
  kasus normal (SSE hidup + prefetch hit) 750ms (<1 detik);
  SSE drop/stall dibatasi 3.750ms (dari 10.900ms); lagu luar
  antrean tetap cold resolve 5.100ms (batas jujur). Drift,
  ticker 2 detik, timeout SSE 45 detik, publish host, queueUid,
  kunci tamu, tap-to-play/autoplay 10.34, tema, lirik, durasi,
  genre TIDAK berubah; tanpa langkah tunggu baru. Drive-by:
  About memakai label versi RUNTIME dari PackageManager
  (`appVersionLabel`) — string hardcode "v1.10.26 (37)" tampil
  sejak 10.26 sampai 10.34 karena build.sh tak pernah menulis
  ulangnya, sehingga screenshot 20:58 WIB tak membuktikan versi
  terpasang. Harness135 diferensial (baseline 21 failures K1–K6+K8
  → 0; simulasi 39/39 + 21/21) + regresi 115–134 hijau (115/119
  dipulihkan dari stub drift yang sudah ada sebelumnya; klaim
  "115 hijau" di BUILD 10.34 tidak bisa diandalkan). Versi About
  v1.10.35 (46).

### 5.50 Susun ulang Up Next tanpa hapus, normal + Jam (v1.10.36)

  Permintaan user 21:02 WIB: atur urutan lagu Up Next tanpa menghapus
  lagunya, termasuk di Jam Together. Inventaris membuktikan celahnya:
  (1) Jam — TIDAK ADA reorder sama sekali (kartu Jam hanya clickable,
  SessionUpNextList berkomentar "tanpa drag-to-reorder"; antrean RTDB
  terurut push key sehingga urutan tidak bisa diubah tanpa mengganti
  kunci, dan mengganti kunci merusak pelacakan konsumsi per kunci
  10.18); (2) normal — drag long-press ADA (ReorderableUpcoming +
  QueueList) tapi TANPA affordance terlihat (tanpa gagang/petunjuk),
  sehingga tidak bisa ditemukan; (3) layar Queue penuh membolehkan
  lagu berjalan digeser (tidak konsisten dengan blokir swipe-hapus).
  Perbaikan: node baru `queueOrder` (daftar push key urutan tampil;
  parse: order dulu, sisa kunci di belakang urutan push key; sesi
  lama tanpa node = perilaku lama persis) + node `reorderRequests`
  (tamu minta, host validasi permutasi + terapkan + hapus child; TTL
  30 detik, pola jumpRequests 10.34). Host menulis queueOrder +
  menyelaraskan antrean lokalnya (salinan terpetakan sesudah lagu
  berjalan dipindah via rencana moveQueue murni; tanpa langkah tunggu
  baru; tamu mengikuti lewat snapshot yang sama). Lagu berjalan
  DIPIN: tidak ada di upcoming (Jam + detail), dan drag-nya diblokir
  di layar Queue penuh. Gagang seret terlihat (`DragHandleDots`, 2×3
  titik, tanpa ikon extended) di Up Next detail normal (long-press
  baris dipertahankan sebagai pemicu kedua), tab Up Next Jam, dan
  kartu Jam — satu komponen `ReorderableColumn` (key per item,
  indikator drop, tinggi baris seragam) untuk kedua permukaan Jam;
  ketuk baris (main 10.34), hapus host-only (10.18), tanpa Undo
  (10.16), kunci tamu, prefetch/delay 10.35, dan ke-8 varian tema
  TIDAK berubah; tanpa perubahan hue/radius/tipografi/densitas/
  treatment. Batas jujur: drag tidak auto-scroll di tepi viewport
  (daftar Column; layar Queue LazyColumn tidak disentuh kecuali
  pin); urutan baru tamu tampil setelah host menerapkan + snapshot
  berikutnya (bukan optimistis lokal — tanpa desync). Harness136
  diferensial (baseline 7 failures source check + 3/3 reproduksi
  celah → 0 failures; invarian 20/20; API baru 37/37) + regresi
  115–135 hijau (118b fixture-blocked pra-ada; koreksi checker B2
  harness134 yang kedaluwarsa bentuk — diungkap di BUILD doc).
  Versi About v1.10.36 (47), label runtime.

### 5.51 Tiga tema struktural baru: RASI, PASAR, SENYAP (v1.10.37)

  Permintaan user 21:03 WIB: tema baru yang "bener bener baru dan
  freshh". Syarat dari verdict 10.26 ("cuman ganti warna doang"):
  identitas harus STRUKTURAL — bentuk, tipografi, densitas,
  treatment, gerak — bukan palet. Prototipe klik
  `mixtune-new-theme-directions` (RASI/PASAR/SENYAP, tiap arah Light
  + Dark, mock Home/Detail/Search) disajikan dulu; user 21:11 WIB
  menjawab "gas semuanya" — ketiga arah masuk app. PITA tetap
  ditolak, Galeri tetap tidak dipakai (keputusan 10.26/10.27).
  Satu tabel token (AppTheme.kt) diperluas: `AppThemeId` +=
  RASI/PASAR/SENYAP, storage `rasi`/`pasar`/`senyap`, 10 field baru
  di AKHIR AppThemePalette dengan default (literal lama tetap
  mengompilasi): `cornerDetailCover` (sentinel −1 = ikut
  cornerPoster) + 9 flag treatment (`orbitRing`,
  `dotGridBackground`, `constellationLines`, `dashedBorders`,
  `ticketNotches`, `stampBadge`, `flatNoCards`, `textTabNav`,
  `chartNumerals`). Enam literal palet baru (97 field, named args)
  + enum font baru (`AppBodyFont` SPACE_GROTESK/INTER,
  `AppLabelFont` PLEX_MONO/INTER, `AppDisplayFont` SPACE_GROTESK/
  ANTON/FRAUNCES) + easing OVERSHOOT
  (`CubicBezierEasing(0.34, 1.8, 0.64, 1.0)`). Struktur
  mode-invariant: light == dark per tema (hanya warna berubah).
  Klaster bawah (122/130dp, 10.29/10.30) IDENTIK di ke-14 varian.
  Empat tema lama byte-identik dengan snapshot pra-perubahan
  (harness137 §D).

  RASI "Peta bintang": radii serba bundar (card 28, tile 22, chip/
  button/miniPlayer/rowCover/detailCover 999, sheet 28), artwork
  bundar + cincin orbit (OrbitRingOverlay: ring accent + titik
  mengorbit 9 detik linear, statis saat reduced-motion, glow ganda
  di mode gelap) + dot-grid latar (grid 22dp, 10%) + garis
  konstelasi antar baris; display Space Grotesk, label Plex Mono
  hanya untuk label kecil; densitas lapang (rowPaddingV 12,
  rowMinHeight 74, judul seksi/poster/detail 26/38/34); gerak
  lambat ease-in-out (420/650/420/300/520/900); Light kertas
  langit #EEF4FB + tinta #101A2E + aksen #3157D5, Dark langit
  dalam #070D24 + starlight #E8EFFF + aksen #8AB4FF (onChrome/
  onAccent gelap #070D24 — putih di aksen hanya 2,2:1, sengaja
  dibalik demi kontras 9,21:1).

  PASAR "Pasar malam": tepi karcis perforasi (border putus-putus
  8dp/6dp + notch 18dp) + divider putus-putus (2dp, alpha 35%) +
  stiker miring + stempel bundar "PICK" (hanya dekoratif, info
  duplikat di stiker); display Anton condensed UPPERCASE (judul
  tampilan saja — judul lagu/nama TIDAK pernah di-caps), body
  Inter; densitas sangat rapat (rowPaddingV 6, rowMinHeight 62,
  judul 30/40/36, border kartu 2dp); gerak pop pegas cepat
  OVERSHOOT (90/200/120/80/120/280); Light kertas krem #FFF6DC +
  tinta #1C1305 + merah tomat #E63329 (accentFill digelapkan ke
  #C81E15 demi putih 5,75:1) + kuning kunyit, Dark arang #1A1816
  + aksen neon #FF3B30 (onAccent #1A1816, 4,99:1); TANPA hard
  shadow (itu identitas Blok).

  SENYAP "Hening": TANPA kartu/border/shadow — surface == background,
  baris dipisah whitespace + satu hairline (1dp, alpha 18%), nav
  berupa TEXT TAB bergaris bawah (tanpa kapsul), numeral chart
  besar Fraunces Light 30sp ("01", "02", …) di samping baris;
  display Fraunces serif raksasa (judul poster 44, detail 40),
  body Inter Light; densitas paling lapang (rowPaddingV 14,
  rowMinHeight 78); gerak fade lambat LINEAR saja (500/500/500/
  400/500/800); Light putih tulang #FAF8F3 + tinta #111110, Dark
  kebalikannya; mode gelap posterFallback #2A2926.

  Font dibundel sebagai TTF statis OFL di res/font (12 file:
  Space Grotesk 3, Plex Mono 2, Anton 1, Inter 4, Fraunces 2 —
  dari subset latin woff2 Google Fonts → fontTools TTF +
  instancer pin → rewrite name-table; tanpa tabel fvar; string OFL
  di name-table; OFL.txt terverifikasi HTTP 200 per famili; tanpa
  unduhan runtime). Treatment diterapkan di semua permukaan
  (Home, Search + tile genre 1:1, Library, detail, sheet/dialog,
  Jam, antrean): dot-grid di Box konten root Scaffold, garis
  konstelasi + divider putus-putus + numeral di TrackRow (param
  `chartNumber` di 13 situs), PosterCard PASAR (notch + stiker +
  stempel) / SENYAP (flat, tanpa vinyl), CollectionCard SENYAP
  (transparan + outline 1dp), CapsuleNav cabang TextTabNav untuk
  SENYAP, MiniPlayer SENYAP flat (bar tinta), cover detail
  `Corners.detailCover` + OrbitRingOverlay. Permukaan yang sengaja
  TIDAK bertema (precedent 10.26): widget, notifikasi media, kartu
  berbagi lirik, stage detail, windowBackground. Field generatif
  GETAR, lirik follow, sinkron Jam, durasi, halaman genre, reorder
  10.36, tap-to-play/autoplay: tidak disentuh. Kontras: 14 varian
  lulus semua 13 pasangan (bar 10.32; pengecualian brand ARUS/BLOK
  yang terdokumentasi tidak diperluas). Harness137 reproduce-first
  (gagal kompilasi pra-perubahan → 271/271 PASS pasca) + check_137
  guards + regresi 115–137 hijau (perawatan stub harness117/118
  dkk. diungkap di BUILD-v1.10.37.md). Versi About v1.10.37 (48),
  label runtime.

### 5.52 Browse disembunyikan saat hasil pencarian tampil (v1.10.38)

Tab Search: seksi Browse (header "Browse" + tombol "View all"/"Show
less" + grid tile genre 1:1) kini dibungkus `if (!searching)` di
`SearchScreen` (`HomeSearch.kt`) — header dan grid berada dalam satu
blok bergate yang sama, termasuk padding atas header, sehingga saat
query aktif tidak ada celah tersisa: hasil ("Search results" + jumlah)
tampil langsung di bawah kolom pencarian. Saat query dikosongkan,
Browse kembali persis seperti 10.33 (4 tile + View all / Show less,
tile 1:1, sub-halaman Moods & genres tak berubah). Satu-satunya
instance seksi Browse (audit grep: `BROWSE_CATEGORIES` hanya di file
ini). Tidak ada perubahan token/tema (14 varian utuh), perilaku
pencarian, halaman genre, Jam, lirik, durasi, atau reorder. Harness138
reproduce-first (baseline K1/K2/K7 FAIL → kode baru 8/8 PASS) +
regresi 115–137 hijau (118: satu flake waktu F2 pada loop penuh,
lolos 2 re-run terisolasi — diungkap di BUILD-v1.10.38.md; 118b
fixture-blocked pra-ada). Versi About v1.10.38 (49), label runtime.


### 5.53 Audit tema menyeluruh + sinkron lirik terverifikasi durasi (v1.10.39)
Audit 14 varian satu per satu dari laporan HP user (screenshot RASI
Dark): (1) pill "Comments" terpotong di SEMUA tema pada layar
320–360dp — dibuktikan metrik font asli (19 kegagalan); diperbaiki
dengan bobot 0.9/1.2/0.9 + padding 6dp + ikon 16dp + letterSpacing 0
(ALL FIT 7 tema × 5 lebar). (2) Titik orbit RASI menjorok 3dp keluar
cover — digeser ke dalam sebesar jari-jarinya (tepi luar = ring),
glow digeser 2dp. (3) Judul lagu di-caps di BLOK/PASAR (detail +
poster) — casing dihapus, hanya judul display/seksi yang boleh
di-caps. (4) Tile mood tanpa scrim (Jazz 2,94 / Electronic 2,74) —
scrim 0,55 seperti tile artwork (→ 9,86 / 9,55). (5) Numeral chart
SENYAP `widthIn(min = 44.dp)` (3 digit tidak terpotong). Kontras
semua pasangan di 14 varian lolos bar 10.32 (harness139, 221 cek);
identitas struktural 7 tema tetap berbeda, klaster 130dp identik.
Lirik: akar desinkron adalah fetch memakai `durationMs` 0 (salinan
feed) tanpa pernah diulang — guard versi ±10 dtk dilewati dan entri
versi-salah diterima; dibuktikan probe LRCLIB (Blinding Lights 200 vs
263 dtk, baris pertama selisih 22,86 dtk). Perbaikan:
`LyricsVersionPolicy` + efek lirik berkunci durasi terverifikasi
(fetch provisional lalu ambil-ulang saat durasi diketahui; entri
versi-salah turun jadi lirik polos, tanpa timestamp karangan) +
polling embed 250ms. Centering 10.28, tema, dan Jam tidak disentuh.
Harness139 reproduce-first (gagal kompilasi pra-perubahan → 221/221;
guard 16/16; metrik font 19 FAIL → ALL FIT) + regresi 115–138 hijau.
Versi About v1.10.39 (50), label runtime.

### 5.54 Sheet pekat, centering artwork, audit visibilitas menyeluruh, mark Jam logo-saja, kartu Library 1:1 (v1.10.40)

Scrim sheet (ArusBottomSheet/DockedDarkSheet/JamSheetOverlay) dari
hitam translusen menjadi hitam pekat — baris "Select" layar detail
tidak lagi tembus di celah sheet (kontras efektif lama 4.27–5.11:1
terbukti lewat komposisi alpha; kini 1.00:1). Investigasi artwork
tidak-tengah: video RASI terukur −98.5px, screenshot BLOK −0.5px
(lagu/build sama) — mekanisme tidak terbukti (stale build/state
atau penyebab belum ditemukan); satu cacat nyata diperbaiki:
koordinat hero dikonversi `toLocal()` dua kali, kini tepat satu
kali (no-op bila root origin (0,0)). Audit visibilitas: kartu
pemilih tema memakai fill Transparent + bayangan `tokens.text`
(1.00:1) — kini surface pekat + nama InkBlack + cek InkBlack;
token `onAccent` ARUS/BLOK putih→tinta (3.30/3.43→5.37/5.41,
companion aksen kini gelap); akar kelas: `primary`=accentFill
dipakai default M3 sebagai foreground (1.41:1 di SENJA/GETAR
Light) — skema tidak di-remap, 16 TextButton + 5 field + kontrol
form memakai warna aksen eksplisit; pengecualian terkunci: aksen
permukaan dialog ARUS/BLOK 2.69:1. Header: pill "Jam" → mark
broadcast logo-saja 48dp (varian menurut luminansi bg/cakram,
ambang 0.35f; cacat laten pill lama 1.00–1.01:1 diperbaiki).
Kartu koleksi Library: `aspectRatio(1f)` (dulu tinggi fixed
136dp), padding pindah ke Column teks (scrim kini full-bleed),
lingkaran SunDecoration ke TopEnd. Harness140/140b/140c/140d/140e/
140f diferensial hijau + regresi 115–139 hijau. Versi About
v1.10.40 (51), label runtime.

### 5.55 Cover playlist dari galeri, lokal saja (v1.10.41)

Playlist buatan user bisa punya cover foto: dialog "New playlist"
dan dialog rename/edit di Library kini punya baris cover — pilih
gambar dari galeri lewat system photo picker (ImageOnly, tanpa
permission penyimpanan baru), preview 56dp, ganti, dan hapus.
Gambar yang dipilih DISALIN ke penyimpanan privat aplikasi
(`filesDir/playlist_covers/<id>.jpg`, kunci = id playlist) —
bukan referensi Uri picker, supaya cover tetap ada habis restart
dan tetap offline. Foto diperkecil (sisi terpanjang ≤1024px),
orientasi EXIF dibakar ke dalam gambar, ditulis JPEG kualitas 85
secara atomik (file temp + rename). Cover tampil di kartu koleksi
1:1 (center-crop full-bleed, scrim 0.55 + judul/meta putih di
atasnya, dekorasi matahari/garis dilewati) dan di header detail
playlist (thumb 56dp di samping judul). Di SENYAP, kartu ber-cover
tetap tampil sebagai kartu foto — konten user menang atas
treatment datar. Rename/reorder mempertahankan cover (id tidak
berubah); hapus playlist menghapus file cover-nya; `refresh()`
membuang file cover yang playlist-nya sudah tidak ada (mis.
terhapus lewat sinkron). Kunci cache Coil berversi (path +
lastModified) supaya cover yang diganti tidak menampilkan gambar
basi. Model `Playlist` dan sinkron Firestore TIDAK disentuh
(id/nama/trackIds/createdAt saja): cover lokal saja, TIDAK
tersinkron ke perangkat lain — tanpa backend baru, tanpa layanan
berkartu. Jalur quick-create di sheet "Save to playlist" tetap
tanpa picker (cover bisa ditambahkan belakangan lewat
rename/edit); kartu Liked/Offline tidak berubah. Harness141
reproduce-first (baseline 28 FAIL → tree baru 33/33 + policy
Kotlin standalone hijau) + regresi 115–140 hijau KECUALI h118 F2
(gagal deterministik 3× dengan signature identik pada input yang
byte-identik dengan tree 10.40 — bukan regresi 10.41, lihat
BUILD-v1.10.41.md §6). Versi About
v1.10.41 (52), label runtime.


### 5.56 Jam Together gaya Pulse + tab Jam keempat (v1.10.42)

Jam Together dibangun ulang mengikuti layout Pulse: permukaan
layar-penuh yang sama dibuka dari mark Jam di header maupun dari
tab Jam keempat di navigasi bawah. Urutannya dari atas: judul
sesi + badge "N listening · host <nama>", orbit (artwork ASLI
lagu yang sedang jalan di tengah 104dp melingkar, cincin
berdenyut warna aksen tema, avatar anggota di sekeliling lingkaran
dengan label nama, statis saat reduced-motion), kotak kode undangan
(kode besar ber-spasi + tombol Copy yang menyalin link bridge
https yang sudah ada), toggle shared-control ("Everyone can
control playback", DEFAULT OFF = perilaku lama persis: tamu
terkunci, host mengontrol; saat ON, permintaan kontrol tamu
diterapkan host lewat pola yang sama dengan jumpRequests),
baris reaksi (sistem 10.17 yang sama: 4 jenis, emoji melayang,
rate limit sama), baris Now playing, antrean Jam dengan tombol
vote per baris (chevron + angka; satu vote per anggota per lagu,
ketuk untuk membatalkan; vote hanya mengurutkan lagu yang belum
jalan — lagu yang sedang jalan tetap menempel; urutan tampil =
vote terbanyak lalu urutan antrean; host memainkan lagu
vote-teratas saat lagu habis secara alami, antrean buatan user
tetap menang atas autoplay, tanpa pengulangan langsung),
tombol "Add songs" (alur tambah-ke-Jam yang sudah ada), chat
(daftar pesan + kolom input + kirim; nama pengirim berwarna,
pesan sendiri rata kanan; teks verbatim tanpa moderasi — sesi
berkode privat; 50 pesan terakhir dipertahankan), dan tombol
End/Leave (host: "End Jam for everyone", tamu: "Leave Jam").
Node RTDB baru di bawah `/jam/$code`: `votes/` (satu kunci per
item antrean → uid anggota), `messages/` (teks + pengirim +
waktu), `sharedControl` (bool milik host), `controlRequests`
(pola jumpRequests, TTL 30 detik); rules TIDAK berubah
(auth-only). Kehadiran memakai node `members/` yang sudah ada +
heartbeat REST tiap 10 detik; anggota yang basi >30 detik hilang
dari orbit (hitungan "listening" tetap memakai ambang 90 detik,
host-away 60 detik — tidak diubah). Navigasi bawah kini EMPAT
tab (Home, Search, Library, Jam) — tiga tab lama dan layarnya
persis seperti sebelumnya; tab Jam memakai mark broadcast
Canvas (warna token tema) + dot LIVE aksen saat sesi aktif;
SENYAP tetap tab teks bergaris bawah, kini 4 tab. Tinggi nav
tetap 56dp, mini player/wave/gap/inset tidak berubah; pil item
menyusut agar muat; label tetap terbaca di ke-14 varian. Kartu
Jam yang menempel di atas mini player TETAP sebagai indikator
live ringkas dan tetap membuka sheet overlay lama (perilaku
overlay tidak diubah); tab Jam membuka permukaan layar-penuh.
Sheet join 6-kotak dipensiunkan — kolom kode inline di empty
state layar Jam, deep link `mixtune://jam/<KODE>` tetap mendarat
terisi + auto-join. Harness142 reproduce-first (baseline 22
FAIL/11 GUARD PASS → tree baru 33/33 cek sumber + 37/37 Kotlin
standalone) + regresi 115–141 hijau KECUALI: h118 F2 TIDAK
mereproduksi pada tree ini (lolos 3× — 1 loop + 2 ulang
terisolasi — namun F4 gagal 1× pada ulang terisolasi pertama
dengan tamu yang tak pernah mencapai status playing; area
watchdog tetap sensitif-waktu, investigasi khusus tetap
terbuka, lihat BUILD-v1.10.42.md §6), h118b tetap fixture-blocked,
h139 measure_text fixture-blocked (font /tmp hilang; bagian
Kotlin + cek-nya hijau), h128 menyisakan 11 kegagalan identitas
tema yang sudah ada sebelumnya (tabel token byte-identik dengan
10.41). Versi About v1.10.42 (53), label runtime.

### 5.57 Reaksi Jam terbang melintasi layar (v1.10.43)

Emoji reaksi Jam terbang melintasi layar gaya Pulse, di layar Jam
layar-penuh saja (sheet overlay tidak berubah): saat anggota mana
pun mengirim reaksi (4 jenis 10.17 yang sama), emoji muncul di
sepertiga bawah layar, naik ~46% tinggi layar sambil bergeser
horizontal ±48dp + berotasi ±15 derajat (arah dari seed kunci
reaksi), lalu memudar selama 3 detik; maks 8 floaters, tertua
dibuang dulu. Satu floater per event — dedupe berdasarkan kunci
node reaksi (pengiriman ulang snapshot tidak memicu animasi
kedua), jalur yang sama untuk reaksi lokal dan remote. Overlay
tanpa modifier sentuhan (pass-through, tidak menggeser layout);
satu Animatable per floater. Reduced-motion: tidak ada yang
terbang — badge statis di atas tombol (10.17) menjadi fallback.
Kebijakan murni di `data/FloatingReactions.kt`; payload/rate
limit/prune reaksi, chat, vote, antrean, tema, dan sinkron TIDAK
berubah; tidak ada node RTDB baru. Harness143 reproduce-first
(baseline 13 FAIL/7 GUARD PASS → 20/20 cek sumber + 19/19 Kotlin)
+ regresi 115–142 hijau kecuali h118 (tally 6/5/5 dengan tanda
`calls=1 base=1` yang sama — suite watchdog tidak stabil,
investigasi khusus tetap terbuka, lihat BUILD-v1.10.43.md §3),
h118b fixture-blocked, h139 measure_text fixture-blocked,
h127 1 + h128 11 kegagalan pre-existing. Versi About v1.10.43
(54), label runtime.

### 5.58 Pill sleep timer, tumpukan avatar Jam, pill ikon-saja (v1.10.44)

Tiga scope satu build. (A) Pill sleep timer di baris pill layar
detail, memakai mesin `SleepTimer`/`setSleepTimer` yang sama
dengan Settings → Playback (v1.10.6, tanpa mesin kedua):
nonaktif = ikon bulan saja, aktif = ikon + sisa waktu ("24m",
pembulatan ke atas, refresh 30 detik); ketuk membuka dialog
pilihan Off/15/30/60 yang sama dengan Settings + baris status
"stops at HH:mm" yang sama; Settings tidak diubah (chip-nya
pilihan terakhir di Settings, baris statusnya kebenaran mesin).
(B) Tumpukan avatar anggota sesi Jam (data `members` yang sudah
ada, tanpa node/fetch baru): maks 3 di tab Jam nav kapsul (sudut
kanan atas item, menggantikan dot LIVE) dan mark header (sudut
kanan bawah cakram), maks 2 di nav teks SENYAP; anggota lebih
jadi chip "+N"; avatar = `JamMemberAvatar` orbit yang sama
(foto/inisial) + cincin pemisah; hanya saat sesi hidup dan ada
anggota segar, idle tetap logo-saja, dot LIVE tetap bila anggota
(masih) kosong. (C) Pill Comments + Save di detail menjadi
ikon-saja (`iconOnly` di `DetailPill`, label tetap
contentDescription + onClickLabel); Like/Dislike tidak berubah;
baris 4 pill berbobot 1.0/0.6/0.6/0.6 (tersempit 55dp di 320dp).
Tidak ada token tema yang berubah. Harness144 reproduce-first
(baseline 20 REPRO FAIL → 26/26 cek sumber + 17/17 Kotlin) +
regresi 115–143 hijau kecuali h127 1 + h128 11 pre-existing,
h118b fixture-blocked, h139 measure_text fixture-blocked;
koreksi checker h132 (jendela 80→130 baris) dan h139 G1
(invaran baru pill ikon-saja) terdokumentasi di
BUILD-v1.10.44.md §2; h118 ALL PASS pada satu lari loop ini
(suite tidak stabil antar-lari — investigasi khusus tetap
terbuka, lihat BUILD-v1.10.44.md §3). Versi About v1.10.44
(55), label runtime.

### 5.59 Rename total MIXTUNE → SONARA + reset versi (v1.0)

Identitas app diganti total atas perintah user ("ganti identitas
app gua jadi SONARA"), scope display-only awal diganti redirect
menjadi rename total + reset versi. Package
`com.mixtune.app` → `com.sonara.app` (direktori, package,
import, manifest, R, build script); seluruh nama kelas/fungsi/
identifier (`SonaraApplication`, `SonaraApp`, `SonaraState`,
`SonaraTheme`, `SonaraWidget`, `SonaraHttp`,
`sonaraTextButtonColors/FieldColors`); seluruh string
user-visible → SONARA (label launcher/widget, About, kartu
lirik, notifikasi, share, crash); otoritas FileProvider,
channel ID, nama prefs, bridge JS, User-Agent ikut rename.
Deep link primer `sonara://jam/<KODE>`; `mixtune://` tetap
diterima sebagai fallback (intent-filter tambahan +
`JAM_DEEP_LINK_SCHEME_LEGACY`); URL bridge https
`moloqo.github.io/mixtune/jam/<KODE>` TIDAK berubah.
Firebase project tetap `mixtune-9f676`, keystore debug sama
(sidik jari identik); `google-services.json` hanya 2
`package_name` diubah, tanpa OAuth client id karangan —
registrasi app Android baru di console masih menjadi langkah
parent. Logo ripples, Garis Arus, 7 nama tema, UI Inggris,
tabel token tidak berubah. `versionName` 1.0, `versionCode` 1
(reset); install segar = app terpisah, data lokal tidak
pindah, data cloud sinkron kembali setelah login. Harness145
reproduce-first (baseline 27 REPRO FAIL → 38/38) + bukti
level artefak (aapt2/dex: nol byte `MIXTUNE`, `mixtune`
hanya di allowlist bridge/project-id/skema-legacy) +
regresi 115–144 hijau kecuali h118b/h139 fixture-blocked,
h127 1 + h128 11 pre-existing (koreksi checker h118/h119
stub, h140f, h141 G4, h142 G1 terdokumentasi di
BUILD-v1.0.md §2; h118 ALL PASS pada re-run terisolasi —
investigasi khusus tetap terbuka). Versi About SONARA v1.0
(1), label runtime. Dokumen build: BUILD-v1.0.md (dokumen
v1.0 lama dipindah ke BUILD-v1.0-legacy.md, utuh).

### 5.60 Mini-player play disc fix + HD artwork + About tanpa MixMusic (v1.0.1)

Tiga scope dalam satu batch (keputusan user: tidak ada v1.0.2).

1. Tombol play mini player hilang di HP (screenshot ARUS Light 25 Nov
02:28: ruang 44dp ada, lingkaran + ikon tidak tergambar sama sekali).
Penyebab TIDAK terbukti dari nilai token (dump tabel 14 varian:
ARUS Light kontras 5.37/5.37 — lolos) dan TIDAK terbukti dari
struktur (kode benar). Eksperimen kontrol di screenshot yang sama:
cakram play Community Picks (pola Box + clip + background
AccentFill + ikon OnAccent yang sama) tampil sempurna #FF4E24.
Satu-satunya pembeda: BouncyBox (graphicsLayer scale) membungkus
clip+background di MiniPlayer. Perbaikan: BouncyBox dilepas, diganti
Box biasa + clickable (ripple + getar dipertahankan); ukuran,
posisi, urutan, warna, dan perilaku lain tidak berubah. Detail
screen MorphPlayPauseIcon/BigPlayButton TIDAK disentuh.
2. Pasangan warna cakram dari token murni [playDiscPair] di
AppTheme.kt, disimpan sebagai playDiscFill/playDiscTint di
SonaraTokens: cakram harus kontras terhadap kapsul GELAP, bukan
terhadap background tema. Audit 14 varian menemukan 2 cacat kelas
yang sama: SENYAP Light/Dark accentFill == warna kapsul (kontras
1.00 — cakram tak terlihat) dan RASI Light accentFill 2.86
terhadap stage. Aturan: flatNoCards → (background, text);
accentOnDark != accentFill → cakram accentOnDark, ikon onAccent
kecuali kontrasnya < 3.0 → text; sisanya (accentFill, onAccent).
Hanya 4 varian berubah (RASI Light, PASAR Light isi saja, SENYAP
Light/Dark); 10 varian lain identik. Harness146 memeriksa ke-14
varian (kontras >= 3.0 + ukuran 44/24/54).
3. Artwork HD: seleksi satu pintu [selectArtworkUrl] di
ArtworkUrls.kt (murni, teruji harness): lebar-terbesar eksplisit
(dulu elemen terakhir di parseItem/largestThumb — rapuh bila urutan
array berubah), lalu naikkan parameter ukuran `=w..-h..` URL
lh3/yt3 googleusercontent ke 544 (terbukti dilayani untuk id gambar
yang sama di respons live; hanya bila persegi dan < 544; URL lain
tak disentuh). Dipakai di parseItem, detail fetch, largestThumb
ArtistApi/GenreApi, dan GoEngine.parseImage. Probe live 25 Nov:
array feed menaik, puncak 120 (11/33), 400, 544, 1200; URL
lh3/yt3 satu id + akhiran ukuran saja. Fallback hqdefault tidak
berubah; tidak ada URL karangan; Coil tetap downsampling ke ukuran
view (widget 256px). Kartu share lirik text-only (tanpa artwork).
VideoMeta.imageUrl (player) masih tidak dipakai — opsi lanjutan.
4. About: paragraf MixMusic + baris kredit dihapus SELURUHNYA dari
UI atas perintah user ("hapus paragraph mixmusic di about
sepenuhnya biarin mesin Go-nya"). Mesin Go (libgojni.so dari
mixmusic.aar) TIDAK disentuh; komentar kode + THIRD-PARTY-NOTICES.md
baru di root proyek mencatat atribusi MIT. Harness146 menegaskan
nol "mixmusic" di string user-visible/About dan di string pool APK.

### 5.61 Background playback yang sungguhan (v1.0.2)

Foreground service + MediaSession + notifikasi sudah ada sejak lama;
yang diperbaiki adalah empat celah yang membuat pemutaran tetap mati
di background (layar mati / swipe dari recents), semuanya terbukti
dari kode sebelum disentuh (harness147: 14/14 gagal di tree v1.0.1).

1. Wake mode + atribut audio: ExoPlayer dibangun tanpa setWakeMode
dan tanpa AudioAttributes — izin WAKE_LOCK dideklarasi tapi tidak
ada yang memegang wake lock, jadi CPU boleh tidur saat layar mati
(terutama di OEM agresif seperti Infinix/XOS). Kini builder memakai
setWakeMode(C.WAKE_MODE_LOCAL) + AudioAttributes USAGE_MEDIA /
CONTENT_TYPE_MUSIC (handleAudioFocus=false — fokus dimiliki
PlayerManager, lihat butir 3).
2. Silent swallow: startForegroundService() di PlayerManager dan
startForeground() di PlaybackService menelan SEMUA exception, dan
flag `started` disetel true bahkan bila startForeground gagal —
service bisa jalan TANPA status foreground dan tidak pernah mencoba
lagi. Kini: `started` hanya setelah startForeground sukses, gagal =
tetap false + Log.w ke logcat, dan start berikutnya (tiap ganti
lagu/resume memanggil startService) mencoba lagi. Notifikasi tetap
diposting lewat nm.notify supaya kontrol terlihat walau foreground
belum berhasil.
3. Fokus audio: tidak ada AudioAttributes, audio-focus listener, atau
becoming-noisy handling sama sekali. Kini satu pemilik di
PlayerManager untuk KEDUA engine (fokus bawaan ExoPlayer hanya
menutup jalur ExoPlayer dan ducking internalnya dilawan applyVolume
yang menulis volume tiap 100ms): request/abandon AudioFocusRequest,
keputusan murni di AudioFocusPolicy (LOSS permanen = jeda + lepas
fokus tanpa resume otomatis; TRANSIENT = jeda, gain boleh resume;
CAN_DUCK = volume ×0.2 lewat applyVolume; GAIN tanpa kehilangan =
abaikan, tidak pernah meng-override jeda user), receiver
ACTION_AUDIO_BECOMING_NOISY (headphone dicabut = jeda). Fokus
diminta di playExo/playEmbed/resume, dilepas di pause/stop.
setHandleAudioBecomingNoisy(false) di builder supaya tidak ada dua
pemilik perilaku noisy.
4. WebView embed milik activity: WebView dibuat composition
(activity context) dan dipertahankan attach() selamanya — activity
dihancurkan (swipe dari recents) = WebView mati = lagu jalur embed
berhenti; activity dibuat ulang = WebView basi dipakai terus.
Kini WebEmbedPlayer memiliki SATU WebView dari application context
(tema application context sudah AppTheme; WebView ini audio-only,
tanpa dialog/input, jadi alasan lama tidak berlaku), dibuat malas
di thread Main, dilepas dari parent lama supaya bisa di-host ulang;
composition hanya me-host view yang sama lewat AndroidView.
attach() dipertahankan sebagai kompatibilitas dan mengabaikan view
asing.

Tidak berubah: perilaku notifikasi/MediaSession/widget, sinkron Jam,
sleep timer, lirik, komentar, feed, tema, mesin Go, string UI.
Tidak ada izin baru. Batas jujur: pembuktian mati-hidup di background
hanya bisa dari HP user (termasuk setelan baterai/autostart Infinix
yang berada di luar kode); harness membuktikan celah kode tertutup,
bukan kelangsungan hidup proses di perangkat.

### 5.62 Pencarian, radio, tidak suka, info lagu (v1.0.3)

Batch 1 celah MixMusic, semuanya dibangun hanya dari jalur data yang
terbukti live/signed-out dari VM (fixture respons di harness148).
Saran ketikan dari endpoint get_search_suggestions (signed-out
hanya mengembalikan saran query; saran entitas didukung parser
secara defensif). Urutan hasil: Best match = urutan server; Most
played/Newest = urutan sisi klien dari hitungan/tahun yang
benar-benar dibawa subtitle respons (yang tanpa data tetap di
urutan server, di belakang) — tidak ada kontrol sort server.
Hitungan tampil di hasil hanya bila nyata ("audiens bulanan" artis
bukan hitungan). Tempel link YTM + Enter membuka/mutar lewat pola
pending-link yang sama; link lain = toast jujur; teks link tidak
dicari dan tidak auto-play. Radio: daftar TERPISAH dari antrean
user (seksi Radio di UP NEXT, tanpa reorder/swipe), lagu user
selalu menang, isi ulang dari lagu radio terakhir, NON-Jam saja
(clearRadio di satu-satunya titik aktivasi Jam + aksi
disembunyikan selama sesi + next() bercabang). Dislike: set
persisten lokal (track lengkap, tidak disinkron), advance melewati
yang tidak disukai (ketuk eksplisit tetap menang), Library
mendapat daftar Disliked (terbaru dulu, cari + urut, Restore).
Track info: sumber + format/bitrate stream nyata + jejak langkah
resolve apa adanya; lagu gagal menjelaskan percobaannya lewat
tombol Details di kotak error. SponsorBlock: lompatan segmen +
penanda wave + chip Undo 6 detik + toggle Playback (API terjangkau,
tetapi belum ada payload segmen sungguhan dari VM — perilaku
dibuktikan dari bentuk respons yang didokumentasikan). Live:
label LIVE ganti durasi + seek mati (HLS lewat jalur resolve yang
ada). Label "made with AI" TIDAK dibangun — tidak tersedia
signed-out (nol badge/token di respons search maupun player);
mengarang label dilarang. Tidak berubah: tema/token, Jam, sleep
timer, lirik, komentar, feed, mesin Go, sinkron, notifikasi/widget,
background-playback v1.0.2. Tidak ada izin baru.

### 5.63 Lirik batch 2: cari, edit, offset, sumber, fullscreen (v1.0.4)

Batch 2 celah MixMusic, dibangun hanya dari jalur data yang terbukti
live/signed-out dari VM (fixture respons di harness149). Terjemahan
per-baris, romaji, dan lirik-dari-caption TIDAK dibangun: LRCLIB
tidak mengembalikan field terjemahan/romaji apa pun (80 entri, 4
lagu EN/JP/KR/ID — union key identik), endpoint timedtext YouTube
mengembalikan body kosong untuk semua bahasa, dan respons player
tidak punya captionTracks; membangun ketiganya berarti mengarang
data. Highlight per-kata SUDAH ada sejak v1.6 (render baris aktif
dari timing kata ASLI lyricsfile LRCLIB — hasWordSync terbukti
untuk banyak lagu, fixture Blinding Lights: 37 baris, timing kata
naik) dan tidak diubah. Yang baru: (1) Cari di lirik — baris hasil
disorot, naik/turun melingkar, lompat memakai rumus pemusatan yang
sama dengan follow. (2) Edit lirik — satu field per baris,
timestamp dipertahankan, baris kosong dibuang, timing kata dibuang
hanya pada baris yang teksnya berubah; tersimpan LOKAL per lagu
(maks 100 lagu), tidak disinkron, "Reset to fetched" mengembalikan
hasil fetch; penanda "Edited" di panel + caption. (3) Offset timing
— per-lagu ±10 detik (langkah 0,5 detik, dialog di panel) +
global ±10 detik (slider Settings → Playback); posisi efektif =
posisi putar + global + per-lagu, hanya menggeser baris aktif +
highlight kata, tidak mengubah pemutar. (4) Pemilih sumber —
Auto (LRCLIB sinkron → YTM → LRCLIB polos, persis urutan lama),
LRCLIB saja, atau YouTube Music saja (polos); pilihan eksplisit
tidak jatuh ke sumber lain. (5) Lirik fullscreen — daftar sendiri
dengan follow memakai rumus pemusatan yang sama (centerScrollOffset
+ isNearCenter, satu koreksi), gaya Standard (22/28sp) / Large
(30/38sp), warna token stage detail yang sama di semua 14 varian.
Rumus pemusatan (LyricsFollow.kt) dan kebijakan versi
(LyricsVersionPolicy) TIDAK disentuh. Tidak berubah: tema/token,
Jam, sleep timer, komentar, feed, mesin Go, sinkron,
notifikasi/widget, background-playback v1.0.2, fitur v1.0.3. Tidak
ada izin baru.

### 5.64 Podcast, kecepatan putar & ekstra sleep timer (v1.0.5)

Batch 3 celah MixMusic, dibangun hanya dari jalur data yang terbukti
live/signed-out dari VM (fixture respons di harness150). Halaman
acara podcast dari endpoint /browse WEB_REMIX: header (judul, host,
deskripsi, artwork) + daftar episode nyata + paginasi continuation +
Follow lokal; baris acara muncul di hasil Cari (maks 3, jalur Kotlin
terpisah karena Go engine tidak membawa baris acara) dan di Library
("Podcasts you follow"). Episode ditandai dari subtitle
"Episode • <tanggal> • <Acara>" (artis = nama acara); ketuk nama
artis di detail saat episode membuka halaman acara, bukan profil
artis. Rak "Keep listening" di Beranda: snapshot posisi-lanjut
(terbaru dulu, played dikeluarkan, maks 10), ketuk = lanjut dari
posisi tersimpan; mark played lokal per episode (mark menghapus
posisi, un-mark tidak menghidupkannya). Chapters dari panel
macro-markers /next (23 marker pada episode probe): tab CHAPTERS
ke-4 di detail hanya bila chapters termuat, ketuk chapter = seek
lewat guard Jam; transcript TIDAK dibangun (continuation
get_transcript mengembalikan HTTP 400 signed-out — mengarangnya
berarti mengarang data). Kecepatan putar 0.5–2.0× (7 langkah) lewat
dialog bersama di detail ("Speed: 1.0×" di bawah kontrol, sembunyi
saat Jam), TrackSheet, dan Settings (default); pilihan per-episode
diingat untuk konten ≥ 10 menit dan diterapkan saat putar; selama
Jam, drift Jam yang memiliki rate (1.0/0.98/1.02). Sleep timer
memakai mesin yang sama: mode baru "End of track" (berhenti di akhir
lagu/episode, tanpa advance/repeat/autoplay) di dialog detail +
Settings; daftar menit TIDAK diubah (terkunci harness144) dan
fade-out saat timer berbunyi sudah ada sebelumnya. Rumus pemusatan
lirik dan LyricsVersionPolicy TIDAK disentuh. Tidak berubah: tema/
token, Jam, komentar, feed, mesin Go, sinkron, notifikasi/widget,
background-playback v1.0.2, fitur v1.0.3. Tidak ada izin baru.

### 5.66 File lokal ekstra, output audio, layout & tema kustom (v1.0.7)

Batch 5 (terakhir) celah MixMusic. File lokal: folder SAF
pilihan user (izin persistable, maks 8) digabung dengan scan
MediaStore; edit tag tampil per file (lokal saja, file tidak
pernah ditulis); duplikat (judul+artis+durasi ternormalisasi)
digabung dengan pemilih salinan; berkas hilang ditandai meredup
+ dialog + toggle Hide missing; scan ulang saat Library dibuka
+ tombol Rescan, tanpa background watcher. Output audio:
dialog perangkat nyata AudioManager + rute MediaRouter (tap =
minta sistem berpindah); Chromecast TIDAK dibangun (Cast SDK
404 di Maven Central + butuh jalur remote-playback paralel) dan
LAN remote TIDAK dibangun (butuh protokol pairing/trust; soket
tanpa autentikasi = lubang) — keduanya dicatat jujur, tanpa
tombol palsu. Layout Beranda/Koleksi: Editorial/Compact/Grid
(pilihan Settings, rak + "Your playlists" saja yang berubah).
Tema kustom melapis DI ATAS palet dasar (tabel token tidak
diubah): 8 aksen + radius, resolve menegakkan WCAG 4.5:1
dengan fallback ke pasangan dasar. Command palette di Search:
12 perintah (navigasi + playback lewat gerbang Jam + jalan
pintas Settings Playback); daftar lengkap saat query kosong,
chip cocok saat mengetik. Tidak berubah: token tema, Jam,
lirik + rumus pemusatan, komentar, feed, mesin Go, sinkron,
notifikasi/widget, background-playback v1.0.2, fitur v1.0.3 +
v1.0.5 + v1.0.6. Tidak ada izin baru.

### 5.65 Organisasi Koleksi, statistik & manajemen unduhan (v1.0.6)

Batch 4 celah MixMusic, dibangun hanya dari data lokal yang sudah
terbukti ada. Pin/folder/tag/auto-download playlist disimpan
TERPISAH di prefs lokal (`playlist_org_v1`) — model Playlist tidak
diubah dan state ini tidak pernah disinkron (pola cover lokal
v1.10.41). Seksi "Your playlists" di root Library menampilkan semua
playlist (grid hanya 2 slot): pin dulu, lalu folder, lalu tanpa
folder; filter tag multi-pilih AND; hapus folder tidak menghapus
playlist. Di dalam playlist: cari judul/artis + urut Date added /
Title / Artist / Most played (hitungan nyata, tiebreak stabil);
drag-reorder dinonaktifkan saat dicari/diurut; Play all memakai
daftar tampil. Statistik ("Your stats", dari baris di Library):
Your Year dari ledger kejadian baru `play_events_v1` — TIDAK bisa
dihitung dari `play_counts_v1` karena tanpa timestamp, jadi tahun
ini mulai dari nol di v1.0.6 (empty state jujur, bukan angka
karangan); menit hanya dari durasi yang diketahui; kartu tahun
1080×1350 mengikuti pola kartu lirik, dibagikan hanya bila ada
data. Unduhan: DownloadManager (HTTP Range dari berkas .part,
restart bila server menjawab 200/416, 206 = tempel) + seksi
"Active downloads" di tampilan Offline (progres, Pause/Resume/
Retry/Delete) + auto-download per playlist (default mati, hanya
lagu yang baru ditambahkan); format penyimpanan `.webm` + metadata
tidak berubah; aksi Download di TrackSheet dialihkan lewat manager.
Rumus pemusatan lirik dan LyricsVersionPolicy TIDAK disentuh.
Tidak berubah: tema/token, Jam, komentar, feed, mesin Go, sinkron,
notifikasi/widget, background-playback v1.0.2, fitur v1.0.3 +
v1.0.5. Tidak ada izin baru.

### 5.67 Penghapusan command palette (v1.0.8)

Atas permintaan user ("hilangkan aja", 25 Nov 13:06 WIB): command
palette v1.0.7 dihapus TOTAL. File `data/CommandPalette.kt`
dihapus; `CommandsSection`, blok chip perintah saat mengetik, tiga
parameter `SearchScreen` (`onNavigate`, `onToggleThemeMode`,
`onOpenPlaybackSettings`), helper `runCommand` /
`guardedCommandPlayback`, dan import yang menganggur dihapus dari
`HomeSearch.kt`; tiga argumen palette dihapus dari call site di
`SonaraApp.kt`. Query kosong kembali menampilkan Browse persis
seperti sebelum v1.0.7 (Recent searches → tile Explore → header
Browse + grid genre, aturan sembunyi-Browse v1.10.38 utuh, tanpa
celah kosong); mengetik menampilkan hasil lagu + saran tanpa chip
perintah. Speed/sleep/output tetap dijangkau dari layar detail,
TrackSheet, dan Settings → Playback. Tidak berubah: file lokal,
layout, tema kustom, audio output, token tema, Jam, lirik, komentar,
feed, mesin Go, sinkron, notifikasi/widget, background-playback
v1.0.2, fitur v1.0.3 + v1.0.5 + v1.0.6. Tidak ada izin baru.
