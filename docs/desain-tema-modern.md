# Tema Jade Ink: arah desain untuk Oppo Pad Mini

Status: putaran 1, belum pernah dirender. Semua yang tertulis di sini diperiksa dari kode dan dihitung dari nilai warna. Belum ada satu pun screenshot. Putaran 2 menunggu foto dari tablet.

## Arah

Saat membaca manhwa longstrip, yang harus menyala hanya panel komiknya. Di layar AMOLED, piksel hitam benar-benar mati, jadi latar `#000000` membuat panel terlihat seperti mengambang dan baterai lebih awet. Semua "perabot" aplikasi (bar navigasi, kartu, sheet) naik satu-dua anak tangga dari hitam dengan nada abu-abu yang sedikit dingin kehijauan, cukup untuk memisahkan lapisan tanpa terlihat seperti kotak abu-abu.

Aksennya dua. Jade `#5FE3B8` untuk aksi, pilihan, dan lencana "belum dibaca". Lilac `#C8B6FF` untuk lencana "sudah diunduh". Dua status yang paling sering dicek di perpustakaan sekarang beda warna dan beda rona, jadi bisa dibedakan sekilas.

Saya sempat memilih biru langit, lalu membatalkannya karena terlalu mirip Tidal Wave (`#5ED4FC`) dan Tokyo Night. Kuning keemasan juga saya buang karena aplikasi sudah memakai kuning (`colorScheme.active`) untuk menandai "filter aktif"; aksen kuning akan mengaburkan arti itu. Jade masih berada di antara Green Apple (hijau) dan Teal & Turquoise, tetapi belum dipakai tema lain.

Hal khas yang dipegang: sudut membulat yang konsentris. Kotak sentuh item grid perpustakaan dan sampul di dalamnya dibuat dengan selisih radius sama dengan padding-nya (12 dp dikurangi 4 dp = 8 dp), jadi garis seleksi dan sudut sampul sejajar. Ini detail kecil, tapi justru yang membuat aplikasi modern terasa rapi.

## Token warna

Mode gelap (yang akan dipakai Peter):

| Peran | Hex | Dipakai untuk |
| --- | --- | --- |
| background, surface, surfaceContainerLowest, surfaceDim | `#000000` | Latar semua layar |
| surfaceContainerLow | `#0B0E0D` | Lapisan paling tipis |
| surfaceContainer, surfaceVariant | `#121614` | Bar navigasi bawah, rail |
| surfaceContainerHigh | `#1A1F1D` | Kartu, menu |
| surfaceContainerHighest | `#232927` | Kolom isian, chip terpilih |
| surfaceBright | `#2E3532` | Permukaan paling terang |
| onSurface | `#E8ECEA` | Teks utama |
| onSurfaceVariant | `#A8B2AD` | Teks sekunder, ikon |
| outline / outlineVariant | `#7A8580` / `#2A302D` | Garis batas, pemisah |
| primary / onPrimary | `#5FE3B8` / `#00382A` | Tombol, toggle, FAB |
| primaryContainer / onPrimaryContainer | `#00513D` / `#7FFBD2` | Wadah aksi utama |
| secondary / onSecondary | `#5FE3B8` / `#00382A` | Lencana belum dibaca |
| secondaryContainer / onSecondaryContainer | `#13362C` / `#A6F5DA` | Pil penanda tab aktif di bar navigasi |
| tertiary / onTertiary | `#C8B6FF` / `#2F1D6B` | Lencana sudah diunduh |
| tertiaryContainer / onTertiaryContainer | `#463A82` / `#E6DEFF` | Wadah sekunder |
| error / onError | `#FFB4AB` / `#690005` | Error |

Mode terang (bila sistem tablet di mode terang):

| Peran | Hex |
| --- | --- |
| background, surface | `#F6F8F7` |
| surfaceContainer Lowest / Low / biasa / High / Highest | `#FFFFFF` / `#F0F4F2` / `#E7ECE9` / `#E1E7E4` / `#DAE1DD` |
| onSurface / onSurfaceVariant | `#141917` / `#404945` |
| primary / onPrimary | `#006C52` / `#FFFFFF` |
| secondaryContainer / onSecondaryContainer | `#CDEFE2` / `#062B20` |
| tertiary / onTertiary | `#5E4FA2` / `#FFFFFF` |
| outline / outlineVariant | `#707975` / `#C0C9C4` |

Kontras dihitung dengan rumus WCAG 2 (skrip Python di sesi ini) [Pasti]:

| Pasangan | Rasio |
| --- | --- |
| Teks utama di hitam | 17,6 : 1 |
| Teks sekunder di hitam / di bar navigasi / di `#232927` | 9,6 / 8,4 / 6,8 : 1 |
| Jade di hitam | 13,2 : 1 |
| Teks lencana belum dibaca | 8,2 : 1 |
| Ikon tab aktif di pilnya | 10,5 : 1 |
| Teks lencana sudah diunduh | 7,7 : 1 |
| Outline di hitam | 5,5 : 1 |
| Terang: primary di latar / outline di latar | 6,0 / 4,2 : 1 |

Semua teks lolos 4,5 : 1. Outline terang 4,2 : 1 lolos untuk elemen non-teks (batas 3 : 1).

Satu angka yang perlu dilihat di foto: bar navigasi `#121614` terhadap hitam hanya 1,15 : 1. Itu sengaja tipis, tetapi bisa jadi terlalu tipis di AMOLED dengan kecerahan rendah. Kalau di foto bar bawah tidak terbaca sebagai lapisan, naikkan ke `#171C1A`.

## Bentuk

Berlaku untuk semua tema, bukan hanya Jade Ink. Default Material ada di kurung.

| Token | Nilai | Yang terkena |
| --- | --- | --- |
| extraSmall | 8 dp (4) | Sampul manga di mana-mana, lencana, snackbar, menu, kolom isian |
| small | 12 dp (8) | Kotak sentuh item grid, chip, baris pilihan, kartu di pengaturan |
| medium | 16 dp (12) | Kartu, logo tracker, dialog duplikat |
| large | 20 dp (16) | Menu aksi bawah di detail manga, FAB |
| extraLarge | 28 dp (28) | Sheet, SectionCard, Pill (tidak berubah) |

## Tipografi

Tetap memakai huruf sistem (Roboto, atau OPPO Sans bila ColorOS menggantinya). Tidak ada berkas font baru dan tidak ada dependensi baru. Yang berubah hanya bobot dan jarak huruf, juga untuk semua tema:

| Gaya | Perubahan | Alasan |
| --- | --- | --- |
| headlineLarge / Medium / Small | SemiBold, tracking -0,25 / -0,2 / -0,1 sp | Judul lebih padat dan tegas |
| titleLarge | SemiBold | Judul bar atas tidak lagi terlihat tipis |
| titleMedium | tracking 0,15 ke 0,1 sp | |
| bodyLarge / Medium / Small | tracking 0,5 / 0,25 / 0,4 ke 0,2 / 0,15 / 0,2 sp | Tracking lebar bawaan M3 adalah sumber utama kesan "lama" pada teks kecil |
| labelMedium / labelSmall | tracking 0,5 ke 0,3 sp | Label lencana dan keterangan lebih rapat |

Ukuran huruf tidak diubah, jadi tata letak tidak bergeser.

## Perubahan kode

1. `app/src/main/java/eu/kanade/presentation/theme/colorscheme/JadeInkColorScheme.kt` (baru): skema terang dan gelap, polanya sama dengan 15 skema lain.
2. `app/src/main/java/eu/kanade/presentation/theme/TachiyomiShapesAndType.kt` (baru): `TachiyomiShapes` dan `TachiyomiTypography`.
3. `app/src/main/java/eu/kanade/presentation/theme/TachiyomiTheme.kt`: mendaftarkan Jade Ink, mengirim `shapes` dan `typography` ke `MaterialExpressiveTheme`, dan mengecualikan Jade Ink dari penimpaan surface container saat mode AMOLED (skemanya sudah hitam dengan tangga container sendiri; tanpa pengecualian ini tangganya diganti `#0C0C0C`/`#131313`/`#1B1B1B` abu-abu netral).
4. `app/src/main/java/eu/kanade/domain/ui/model/AppTheme.kt`: entri `JADE_INK`, diletakkan paling depan supaya muncul pertama di pemilih tema.
5. `app/src/main/java/eu/kanade/domain/ui/UiPreferences.kt`: default `pref_app_theme` menjadi `JADE_INK` (sebelumnya Monet bila tersedia, kalau tidak Default). Dua import yang jadi tidak terpakai dihapus.
6. `app/src/main/java/eu/kanade/tachiyomi/ui/base/delegate/ThemingDelegate.kt` dan `app/src/main/res/values/themes.xml`: gaya XML `Theme.Tachiyomi.JadeInk` untuk bagian berbasis View dan jendela, disalin dari pola Tokyo Night.
7. `presentation-core/src/main/res/values/colors_jadeink.xml` dan `values-night/colors_jadeink.xml` (baru): warna yang sama dalam bentuk resource. Saya membangkitkannya dengan skrip dari berkas Kotlin, jadi nilainya identik.
8. `i18n/src/commonMain/moko-resources/base/strings.xml`: `theme_jadeink` = "Jade Ink".

Tidak ada yang menyentuh reader, upscale, loader, `util/waifu2x`, atau `ui/reader/loader`.

## Yang yakin dan yang masih tebakan

Yang saya yakini sesuai pola kode:

1. Cara mendaftarkan tema (enum, peta skema Compose, peta gaya XML, berkas warna di presentation-core, string) mengikuti persis jalur tema yang sudah ada [Pasti, dibaca dari berkas di atas]. Nama resource di `themes.xml` saya cocokkan otomatis dengan yang didefinisikan; tidak ada yang hilang, dan semua XML lolos `xmllint` [Pasti].
2. Preferensi enum disimpan berdasarkan nama, bukan urutan (`getEnum` di `core/common/.../PreferenceStore.kt` memakai `it.name`), jadi menaruh `JADE_INK` paling depan tidak mengacaukan pilihan yang sudah tersimpan [Pasti].
3. `MaterialExpressiveTheme` menerima `shapes` dan `typography`, `Shapes` punya konstruktor publik lima parameter, dan `darkColorScheme` menerima `surfaceBright`/`surfaceDim`/`scrim` [Pasti untuk sumber androidx-main di GitHub yang saya buka; Kemungkinan Besar untuk versi persis di BOM `2026.10.00`, karena maven Google diblokir proxy sesi ini sehingga artefak versinya tidak bisa saya buka]. Modul app sudah memakai `-opt-in` untuk API Expressive, jadi tidak ada error opt-in.

Yang masih tebakan atau belum terverifikasi:

1. Koreksi dari putaran 2: kompilasi sudah dijalankan dan lolos (`:app:compileDebugKotlin`, BUILD SUCCESSFUL), termasuk konstruktor `Typography(...)` 15 argumen yang tadinya saya anggap risiko terbesar [Pasti].
2. Apakah tampilannya terasa modern: [Tidak Diketahui] sampai ada foto.
3. Tracking negatif di headline bisa terlihat terlalu rapat kalau ColorOS mengganti font sistem ke OPPO Sans [Menebak, belum pernah dilihat].
4. Bar navigasi 1,15 : 1 terhadap hitam mungkin terlalu samar (lihat bagian kontras).
5. Sudut 8 dp pada lencana kecil bisa membuatnya hampir bulat penuh; itu disengaja, tetapi perlu dilihat.

Catatan untuk instalasi yang sudah ada: default hanya berlaku bila `pref_app_theme` belum pernah disimpan. Bila Peter pernah memilih tema, aplikasinya tetap di tema itu sampai dia memilih Jade Ink di Pengaturan > Tampilan. Mode tema (`pref_theme_mode_key`) tetap mengikuti sistem; Jade Ink baru tampil hitam bila tablet di mode gelap.

## Yang perlu difoto untuk putaran 2

Mode gelap, tablet posisi tegak dan mendatar bila sempat:

1. Perpustakaan, tampilan grid, dengan beberapa judul yang punya lencana belum dibaca dan sudah diunduh, plus satu item yang sedang dipilih (tekan lama). Ini layar yang paling berubah: latar, sudut sampul, lencana, bar navigasi bawah.
2. Detail manga, digulir sampai daftar bab, lalu satu foto dengan beberapa bab dipilih supaya menu aksi bawah muncul.
3. Pengaturan > Tampilan, bagian pemilih tema (memperlihatkan semua tema berdampingan dan teks judul baru).
4. Satu sheet atau dialog, misalnya filter/urutkan di perpustakaan.
5. Reader dengan menu terbuka (bar atas dan bawah). Reader sendiri tidak diubah, tetapi bar menunya ikut warna tema.
6. Satu foto di mode terang, layar perpustakaan saja.

## Di luar lingkup (dicatat, tidak diubah)

1. Pratinjau di `AppThemePreferenceWidget.kt` memakai `RoundedCornerShape(17.dp)` dan `13.dp` yang ditulis langsung, jadi tidak ikut skala bentuk baru. Bukan rusak, hanya tidak konsisten.
2. Masih ada sekitar 11 `RoundedCornerShape(...)` dengan angka langsung di `app/src/main/java/eu/kanade/presentation`; bagian itu tidak ikut skala bentuk baru. Layak dirapikan setelah putaran 2 bila fotonya menunjukkan sudut yang tidak seragam.

## Putaran 2: struktur tampilan

Peter merasa vibe-nya masih beda dari aplikasi modern, jadi putaran ini mengubah bentuk komponen, bukan hanya warna. Semua masih tanpa render; yang saya periksa adalah kode, kompilasi, dan kontras.

### Sumber kesan "old school" yang saya temukan di kode

1. Lencana di sampul (belum dibaca, diunduh, bahasa) digambar sebagai potongan persegi yang dilekatkan jadi satu strip (`Badge` default `RectangleShape`, padding 3 dp, `BadgeGroup` memotong satu bentuk untuk semuanya). Ini ciri visual Tachiyomi lama yang paling kentara.
2. Item grid yang dipilih diisi blok warna aksen penuh (`drawRect(secondary)`), dan teksnya dibalik warnanya.
3. Scrim judul pada grid ringkas tingginya hanya 33%, lalu dipotong dengan sudut bawah 4 dp yang ditulis langsung. Sejak putaran 1 sudut sampul jadi 8 dp, jadi sudut scrim itu tidak lagi pas dengan sampulnya.
4. Tombol aksi di detail manga (Pustaka, interval, tracking, WebView) yang tidak aktif digambar dengan `onSurface` alfa 38%, sehingga tampak seperti tombol yang dimatikan.
5. Rail navigasi, top app bar mode seleksi, dan bar reader memakai `surfaceColorAtElevation(3.dp)`, yaitu cara M3 versi 2021 yang mencampur warna aksen ke permukaan. Versi M3 sekarang memakai peran `surfaceContainer*`.
6. Pengaturan berupa daftar rata dari tepi ke tepi dengan judul grup kecil (`bodyMedium`). Pengaturan Android sekarang mengelompokkan baris dalam kartu bersudut.

### Yang saya ubah

| Area | Berkas | Perubahan |
| --- | --- | --- |
| Lencana | `presentation-core/.../components/Badges.kt` | Pil terpisah (bentuk `CircleShape`, padding 6 dp, jarak 4 dp), huruf `labelMedium` SemiBold. Berlaku di perpustakaan, browse, migrasi, dialog duplikat. |
| Grid perpustakaan | `app/.../library/components/CommonMangaItem.kt` | Seleksi jadi `secondaryContainer` dengan cincin `primary` 2 dp mengikuti sudut item. Scrim judul 45% tinggi, gradasi tiga titik sampai 85% hitam, sudut bawahnya ikut `shapes.extraSmall`. Judul grid 12 ke 13 sp. Jarak grid 4 ke 6 dp (horizontal) dan 8 dp (vertikal). Berlaku juga untuk grid browse karena konstanta jaraknya sama. |
| Navigasi | `app/.../tachiyomi/ui/home/HomeScreen.kt` | Rail tablet memakai `surfaceContainer`. Label tab `labelLarge` ke `labelMedium` supaya tidak terpotong di rail. |
| Top app bar | `app/.../presentation/components/AppBar.kt` | Mode seleksi memakai `secondaryContainer` dengan ikon dan judul `onSecondaryContainer`, jadi jelas sedang memilih. Mode biasa tetap `surface` (sama dengan `surfaceColorAtElevation(0.dp)` sebelumnya). |
| Detail manga | `app/.../manga/components/MangaInfoHeader.kt` | Empat tombol aksi jadi ubin tonal (`shapes.medium`, jarak 8 dp). Tidak aktif: `surfaceContainerHigh` dengan teks `onSurfaceVariant` (7,7 : 1). Aktif: `secondaryContainer` dengan ikon jade (8,3 : 1). Label `labelMedium`, maksimal 2 baris. |
| Daftar bab | `app/.../manga/components/MangaChapterListItem.kt` | Judul bab yang belum dibaca memakai `FontWeight.Medium`; yang sudah dibaca tetap redup seperti sebelumnya. |
| Pengaturan | `app/.../more/settings/PreferenceScreen.kt`, `PreferenceItem.kt`, `widget/BasePreferenceWidget.kt`, `widget/PreferenceGroupHeader.kt` | Item dalam satu grup jadi satu kartu `surfaceContainer`, inset 16 dp, sudut luar 20 dp, sudut dalam 4 dp, celah 2 dp. Item yang tersembunyi tidak ikut dihitung, jadi sudut atas dan bawah tetap pas. Judul grup `titleSmall` warna `primary`, sejajar dengan teks di dalam kartu. |
| Reader | `app/.../reader/appbars/AutoScrollPanel.kt` | Panel auto scroll jadi kartu melayang dengan inset dan sudut 24 dp yang sama dengan pil navigator bab di atasnya. Warna dan logikanya tidak berubah. |

Total 11 berkas, +145/−50 baris. Tidak ada perubahan pada data, preferensi, navigasi Voyager, reader viewer, upscale, atau loader.

### Kontras pasangan baru (mode gelap Jade Ink, rumus WCAG) [Pasti]

| Pasangan | Rasio |
| --- | --- |
| Teks ubin aksi tidak aktif | 7,7 : 1 |
| Ikon / teks ubin aksi aktif | 8,3 / 10,5 : 1 |
| Teks / subjudul di kartu pengaturan | 15,3 / 8,4 : 1 |
| Judul top app bar saat mode seleksi | 10,5 : 1 |
| Cincin seleksi terhadap hitam | 13,2 : 1 |
| Terang: ubin tidak aktif / ikon ubin aktif | 7,4 / 5,2 : 1 |

### Yang yakin

1. Semua perubahan lolos `:app:compileDebugKotlin` (BUILD SUCCESSFUL) [Pasti]. Satu-satunya peringatan di berkas yang saya sentuh adalah `Slider` lama di `AutoScrollPanel.kt` baris 67, yang memang sudah ada sebelumnya.
2. Semua API yang saya pakai sudah dipakai di kode ini: `ButtonDefaults.buttonColors` dan parameter `shape`/`contentPadding` pada `TextButton` milik presentation-core, `topAppBarColors`, `Modifier.border`, `CornerBasedShape.copy`, `isTabletUi()` [Pasti].
3. Tidak ada pemanggil `Badge` kustom yang mengirim `shape` sendiri, jadi default baru berlaku seragam [Pasti, dicek dengan grep].

### Yang masih tebakan

1. Lencana pil di sampul bisa terasa terlalu ramai bila satu judul punya tiga lencana sekaligus (diunduh, belum dibaca, bahasa) di kolom sempit [Menebak].
2. Kartu pengaturan memakai `surfaceContainer`. Di Jade Ink itu `#121614` di atas hitam (1,15 : 1); kartu bisa nyaris tidak terlihat di kecerahan rendah. Bila begitu, ganti ke `surfaceContainerHigh` di `PreferenceScreen.kt` [Menebak].
3. Layar pengaturan yang itemnya tidak berada dalam grup tetap rata tanpa kartu, jadi bisa ada campuran gaya di satu layar [Kemungkinan Besar, dari struktur `PreferenceScreen`].
4. Jarak grid yang lebih lega mengurangi jumlah sampul per layar bila kolom diatur "otomatis" (`GridCells.Adaptive(128.dp)`) [Kemungkinan Besar]. Jumlah kolom tetap mengikuti setelan Peter bila dia mengatur angkanya sendiri.

### Ditunda karena berisiko

1. Warna bar atas dan bawah reader serta `ChapterNavigator`. Keduanya ada di bawah komentar "Match with toolbar background color set in ReaderActivity", jadi warnanya harus sama dengan yang diatur `ReaderActivity` di `ui/reader`. Mengubah satu sisi saja akan membuat bar sistem dan bar menu beda warna.
2. Header detail manga (ukuran sampul, latar blur). Tata letaknya berbeda antara ponsel dan tablet dan bergantung pada `appBarPadding`; tanpa foto saya tidak mau menebak.
3. Menu aksi bawah saat bab dipilih (`MangaBottomActionMenu.kt`). Sudah memakai `surfaceContainerHigh` dan sudut `shapes.large`, jadi sudah cukup modern.
4. Merapikan sekitar 11 `RoundedCornerShape(...)` berangka tetap di `presentation/`. Lebih aman dikerjakan setelah ada foto.

### Yang perlu difoto untuk putaran 3

Mode gelap, tablet mendatar, kecerahan yang biasa Peter pakai saat membaca:

1. Perpustakaan, grid ringkas: beberapa judul dengan lencana diunduh dan belum dibaca, plus satu atau dua item yang dipilih (tekan lama) supaya top app bar mode seleksi ikut terlihat.
2. Perpustakaan, grid nyaman (judul di bawah sampul), satu layar penuh.
3. Detail manga bagian atas: ubin aksi, satu judul yang sudah di pustaka dan satu yang belum.
4. Daftar bab yang berisi campuran bab sudah dan belum dibaca.
5. Pengaturan > Tampilan dan Pengaturan > Pembaca (kartu grup dan judul grup).
6. Reader dengan menu terbuka dan panel auto scroll terlihat.
7. Satu foto rail navigasi kiri dengan tab Pembaruan yang punya lencana angka.

## Putaran 3: palet gelap yang tenang untuk ruang remang

Peter mengeluh Jade Ink bikin sakit mata saat membaca malam di ruang remang. Diagnosis Master Hei sebagian besar benar. Saya terlalu bangga pada angka kontras dan tidak bertanya kapan dan di mana aplikasi ini dipakai. Ada satu tambahan dari saya: masalah terbesar bukan teks 17,6 : 1. Masalahnya ada pada area isi yang terang dan jenuh, yaitu jade `#5FE3B8` (kroma OKLCH 0,132) pada lencana, sakelar, slider, dan pil navigasi. Area seperti itu menyala jauh lebih besar daripada garis huruf.

### Cara menghitung

Warna dibangun di OKLCH. Rona dan kroma dipatok, lalu terang dicari dengan pencarian biner sampai rasio WCAG 2 tercapai: `(L1 + 0,05) / (L2 + 0,05)`, dengan L = 0,2126 R + 0,7152 G + 0,0722 B pada kanal sRGB yang sudah dilinearkan. Skripnya ada di luar repo (scratchpad sesi). Hasilnya ada di `app/src/uiPreview/java/eu/kanade/presentation/preview/CalmPalettes.kt`.

Sasaran: teks utama sekitar 10 : 1, teks sekunder minimal 6,3 : 1 terhadap latar (4,8 : 1 di atas kartu), aksen 6,5 sampai 7 : 1, dan lencana sekitar 5,6 : 1 terhadap latar dengan teks gelap di dalamnya. Tidak ada putih murni.

### Tiga kandidat

| Token | A. Jade Redup | B. Arang Hangat | C. Grafit |
| --- | --- | --- | --- |
| background | `#000000` | `#100E0B` | `#08090B` |
| surfaceContainerLow / biasa / High / Highest | `#0E100F` / `#161918` / `#202221` / `#292C2B` | `#191611` / `#221F1A` / `#2A2722` / `#33302A` | `#111213` / `#1A1B1C` / `#222325` / `#2A2B2D` |
| onSurface | `#AEB5B1` | `#C6BFB3` | `#BBBDC0` |
| onSurfaceVariant | `#898E8B` | `#999387` | `#8F9194` |
| primary / onPrimary | `#729F8D` / `#091F18` | `#899C80` / `#151D11` | `#8994A7` / `#161B24` |
| secondary (belum dibaca, label tab aktif) / onSecondary | `#698C7E` / `#03130D` | `#808F79` / `#0B1208` | `#808998` / `#0C0F16` |
| secondaryContainer (pil navigasi) / on | `#1C3028` / `#ADC3B9` | `#262E22` / `#B7C1B2` | `#272C34` / `#B8BEC8` |
| tertiary (diunduh) / onTertiary | `#84819C` / `#0F0E18` | `#9D8677` / `#150D08` | `#7C8087` / `#0E0F12` |
| Kroma aksen (OKLCH) | 0,056 | 0,046 | 0,031 |

Nilai lengkap 36 peran ada di `CalmPalettes.kt`.

| Rasio WCAG | A | B | C | Jade Ink sekarang |
| --- | --- | --- | --- | --- |
| Teks utama / latar | 10,0 | 10,6 | 10,6 | 17,6 |
| Teks sekunder / latar | 6,3 | 6,3 | 6,3 | 9,6 |
| Teks sekunder / surfaceContainerHigh | 4,8 | 4,9 | 5,0 | 6,8 |
| Aksen / latar | 7,1 | 6,5 | 6,5 | 13,2 |
| Isi lencana belum dibaca / latar | 5,7 | 5,6 | 5,6 | 13,2 |
| Teks lencana / isi lencana | 5,1 | 5,5 | 5,4 | 8,2 |
| Label tab aktif / bar navigasi | 4,8 | 4,8 | 4,9 | sekitar 11 |
| Ikon pil navigasi / pil | 7,5 | 7,5 | 7,5 | 10,5 |

### Yang lemah dari tiap kandidat

1. A, Jade Redup: latar hitam pekat tetap memberi lompatan terang paling besar antara UI dan halaman putih, jadi efek halasi di tepi huruf berkurang tapi tidak hilang. Lencana ungu-abu dan hijau-abu di atas sampul terasa pudar.
2. B, Arang Hangat: kehilangan hitam pekat AMOLED (piksel latar menyala sedikit). Kalau kecerahan tinggi, nuansa coklatnya bisa terbaca "kusam". Lencana diunduh berwarna coklat-oranye `#9D8677` bisa tenggelam di sampul yang bernada kulit.
3. C, Grafit: identitasnya paling lemah. Lencana belum dibaca (abu kebiruan) dan diunduh (abu netral) hanya beda sedikit, jadi status di perpustakaan paling susah dibedakan sekilas. Di antara ketiganya, ini yang paling mendekati tampilan generik.

### Rekomendasi

Saya merekomendasikan B, Arang Hangat [Kemungkinan Besar, berdasarkan alasan berikut; belum diuji dengan mata Peter]. Alasannya:

1. Latar yang sedikit terangkat mengurangi lompatan terang antara UI dan halaman komik, dan menghindari smear hitam AMOLED saat menggulir.
2. Teks hangat mengandung sedikit biru, yang lebih nyaman di malam hari.
3. Aksen sage paling tidak mencolok tapi masih punya rona yang jelas.

Kalau Peter tetap ingin hitam pekat, A adalah pilihan kedua.

### Temuan lain dari render (berlaku untuk semua palet, termasuk Jade Ink sekarang)

1. Bar menu reader memakai alfa 0,9 (`ReaderAppBars.kt`, `ChapterNavigator.kt`). Di atas halaman putih, 10% putih tembus dan bar jadi pita abu-abu terang. Koreksi atas catatan Putaran 2: `ReaderActivity` ternyata tidak lagi mengatur warna toolbar; komentar "Match with toolbar background color set in ReaderActivity" sudah usang. Kedua sisi yang harus sama ada di dua berkas Compose itu.
2. Judul di atas sampul memakai `Color.White` tetap (`CommonMangaItem.kt`). Di pratinjau saya pakai `onSurface` dengan scrim lebih pekat (0,35 → 60%, ujung 94%). Hasilnya tetap terbaca dan tidak memakai putih murni.
3. Di M3, `secondary` juga menjadi warna label tab aktif di bar navigasi. Itu sebabnya lencana belum dibaca tidak bisa dibuat isi gelap tanpa merusak label tab. Percobaan pertama saya gagal karena hal ini dan baru ketahuan di render.
4. Sumber cahaya terbesar saat membaca tetaplah halaman komik putih. Palet UI tidak bisa memperbaikinya. Kecerahan layar dan filter warna di pengaturan reader akan berdampak lebih besar daripada palet [Kemungkinan Besar].

### Pratinjau

Dirender dengan Robolectric dan Roborazzi di ukuran mendekati tablet (1172x781 dp mendatar, 781x1172 dp potret; kelas `DesignPreviewTest` lama memakai `w1680dp`, angka piksel, bukan dp). Path di `/home/user/preview-out5/`:

`calm-library-landscape.png`, `calm-library-portrait.png`, `calm-settings-landscape.png`, `calm-settings-portrait.png`, `calm-reader-landscape.png`, `calm-reader-portrait.png`, dan pembanding `calm-baseline-jade-ink-landscape.png`, `calm-baseline-jade-ink-portrait.png`.

Sampul berupa gradien contoh dan halaman komik berupa blok putih. PNG sRGB di monitor tidak bisa meniru AMOLED di ruang gelap: halasi, black smear, dan adaptasi pupil hanya bisa dinilai di tablet [Pasti, keterbatasan alat].

### Status: terpasang di aplikasi

Peter memilih B. Ketiga palet sekarang jadi tema nyata dengan nama bahasa Inggris, karena aplikasinya akan berbahasa Inggris:

| Kandidat | Nama di aplikasi | Enum | Berkas skema |
| --- | --- | --- | --- |
| B | Warm Charcoal (bawaan instalasi baru) | `WARM_CHARCOAL` | `WarmCharcoalColorScheme.kt` |
| A | Muted Jade | `MUTED_JADE` | `MutedJadeColorScheme.kt` |
| C | Graphite | `GRAPHITE` | `GraphiteColorScheme.kt` |

Token gelap persis sama dengan tabel di atas. Skrip mengecek bahwa ke-36 peran di Kotlin, XML `values-night`, dan hasil hitungan identik untuk ketiga tema [Pasti]. Pratinjau sekarang membaca skema asli dari aplikasi, bukan salinan.

Jade Ink tetap ada di enum supaya pilihan yang sudah tersimpan tidak rusak. Entri itu disembunyikan dari pemilih (`titleRes` null, seperti entri deprecated lain) dan diarahkan ke Warm Charcoal, baik untuk skema Compose maupun gaya XML. `colors_jadeink.xml` dan gaya `Theme.Tachiyomi.JadeInk` dihapus karena tidak dirujuk lagi. `JadeInkColorScheme.kt` dibiarkan karena pratinjau "Before" masih memakainya.

Mode terang saya turunkan dari rona dan kroma yang sama: latar kertas lembut (OKLCH L 0,965, bukan putih murni), teks utama 12 : 1, teks sekunder 5,5 : 1 di atas container tertinggi, aksen dan lencana 5,5 : 1 dengan teks putih kebiruan di atasnya, dan label tab aktif sekitar 4,9 : 1.

| Token terang | A. Muted Jade | B. Warm Charcoal | C. Graphite |
| --- | --- | --- | --- |
| background | `#F0F5F2` | `#F9F3E9` | `#F2F3F7` |
| surfaceContainer | `#E2E8E5` | `#EBE5DC` | `#E5E6E9` |
| surfaceContainerHighest | `#D5DAD8` | `#DED8CF` | `#D7D9DC` |
| onSurface | `#2A312E` | `#372E20` | `#2D2F34` |
| onSurfaceVariant | `#4C5451` | `#5A5142` | `#505258` |
| primary | `#1D6F56` | `#4E6A3F` | `#526280` |
| secondary | `#2B6D57` | `#516945` | `#54627C` |
| secondaryContainer | `#BADACC` | `#C8D6C1` | `#C9D1E0` |
| tertiary | `#635D86` | `#7D5B45` | `#5F626A` |

Perubahan lain yang ikut terpasang:

1. Bar menu reader dan navigator bab memakai satu konstanta `READER_BARS_ALPHA = 0.97f` di `ReaderAppBars.kt`. Sebelumnya 0,9 di mode gelap dan 0,95 di mode terang; sekarang 0,97 untuk keduanya.
2. Judul di atas sampul memakai `onSurface` tema di mode gelap (`inverseOnSurface` di mode terang, karena scrim selalu hampir hitam). Scrim-nya jadi transparan → 60% hitam di 35% → 94% hitam di dasar.

Yang belum terverifikasi: tampilan di tablet AMOLED sungguhan di ruang remang, dan mode terang (belum dirender) [Tidak Diketahui].

## Putaran 4: rail, tab kategori, dan daftar perpustakaan

Bahan: screenshot nyata dari tablet Peter (build 1.3.16.8, perpustakaan tampilan daftar, potret). Warnanya masih tema lama karena pilihan tema tersimpan; itu sudah ditangani dengan migrasi di commit `00796ee`. Putaran ini mengurus struktur dan ikon.

### Yang membuat layar itu terlihat lama

1. Ikon rail berasal dari animated vector drawable lama Mihon: garis tipis, semua outline, tanpa beda terisi saat terpilih.
2. Item rail dirapatkan di tengah layar (`Arrangement.Center`), jadi rail terlihat melayang. Aplikasi Android sekarang menaruhnya di atas.
3. Tab kategori memakai `PrimaryScrollableTabRow` dengan garis bawah dan garis pemisah, gaya tab era M2.
4. Baris daftar 56 dp memotong sampul tegak menjadi persegi 40 dp. Sampul manhwa jadi tidak bisa dikenali.
5. Lencana jumlah ada di tepi kanan. Di tablet, jaraknya ratusan dp dari judul, jadi mata harus menyeberangi baris kosong.

### Yang diubah

| Area | Berkas | Perubahan |
| --- | --- | --- |
| Ikon tab | `ui/library/LibraryTab.kt`, `ui/updates/UpdatesTab.kt`, `ui/history/HistoryTab.kt`, `ui/browse/BrowseTab.kt`, `ui/more/MoreTab.kt` | Material Symbols Rounded: terisi saat terpilih, outline saat tidak (Library = collections_bookmark, Updates = new_releases, Browse = explore). History dan More tidak punya versi terisi yang berbeda di Material Symbols, jadi keduanya hanya outline. |
| Ikon baru | `icons/material-symbols/src/main/valkyrieResources/rounded/{history,more_horiz,new_releases}.svg`, `roundedFilled/{collections_bookmark,explore,new_releases}.svg` | SVG resmi dari google/material-design-icons (Apache-2.0), satu path, format sama dengan yang ada. ImageVector dibangkitkan Valkyrie saat build. |
| Rail | `ui/home/HomeScreen.kt` | Item di atas (`Arrangement.Top`). Latar rail sama dengan latar halaman, bukan lempeng terpisah, supaya pil terpilih kontras. Label terpilih SemiBold. |
| Tab kategori | `presentation/library/components/LibraryTabs.kt` | Baris pil yang bisa digulir: terpilih = pil `secondaryContainer`, lainnya = pil outline. Jumlah item di dalam pil. Pil terpilih ikut tergulir ke layar saat pager digeser. Semantik `Role.Tab` dan `selected` tetap ada. |
| Baris daftar | `presentation/library/components/CommonMangaItem.kt` (`MangaListItem`) | Tinggi 84 dp, sampul 2:3 (sekitar 45x68 dp), judul `titleMedium`, lencana di bawah judul sebagai baris metadata. Juga berlaku di daftar Browse. |
| Kolom daftar | `presentation/library/components/LibraryList.kt` | `GridCells.Adaptive(480.dp)`: satu kolom di potret dan ponsel, dua kolom di tablet mendatar. |

### Pratinjau dan penilaian

Path di `/home/user/preview-out8/`: `library-list-before-portrait.png`, `library-list-after-portrait.png`, `library-list-before-landscape.png`, `library-list-after-landscape.png`. Keduanya memakai Warm Charcoal supaya yang berbeda hanya struktur. "Sesudah" memakai komponen asli (`LibraryTabs`, `MangaListItem`, lencana). Rail dibangun ulang dengan ikon dan gaya label yang sama karena `HomeScreen` butuh Voyager. Sampul diisi gradien lewat `LocalAsyncImagePreviewHandler` milik Coil.

Ada tiga putaran render. Putaran pertama memperlihatkan pil rail yang hampir tak terlihat dan lencana yang terpisah jauh dari judul di mode mendatar. Putaran kedua memindahkan lencana ke bawah judul dan menaruh rail di latar halaman. Putaran ketiga menambah dua kolom untuk mode mendatar.

Penilaian jujur: daftar dan tab jelas berubah ke arah aplikasi sekarang (sampul terbaca, judul lebih tegas, kategori sebagai pil, tidak ada ruang kosong di mode mendatar). Perubahan rail lebih halus. Ikon baru masih satu keluarga dengan ikon lama (Material, garis 400), jadi bedanya terutama di posisi atas, ikon terisi, dan pil.

### Yang masih lemah

1. Pil indikator rail dan pil kategori terpilih memakai `secondaryContainer` palet tenang yang sengaja redup. Terlihat, tapi tidak tegas. Menaikkan terang token itu adalah keputusan warna yang belum saya ambil.
2. Baris lebih tinggi berarti lebih sedikit judul per layar di potret (sekitar 12–13, sebelumnya sekitar 19).
3. Di dua kolom, urutan baca berjalan kiri-kanan lalu turun, bukan satu kolom lurus. Untuk daftar yang diurutkan abjad ini perlu dinilai Peter.
4. Lencana masih dibedakan hanya lewat warna (belum dibaca vs diunduh), tanpa ikon atau label.
5. Pratinjau "sebelum" sudah memakai lencana pil dari putaran 2, jadi tidak 100% sama dengan build lama.
6. Drawable `animated_*` dan `presentation/util/AnimatedVectorPainter.kt` kini tidak dipakai tab. Saya biarkan; bisa dihapus terpisah.

## Putaran 5: tiga arah ikon dan rail (belum diterapkan ke aplikasi)

Peter benar bahwa ikon di putaran 4 tidak terasa berganti. Ikon barunya masih Material Symbols Rounded dengan bobot garis 400, keluarga yang sama dengan ikon lama. Putaran ini menyajikan tiga arah yang berbeda jelas, masing-masing dengan keluarga ikon dan bentuk rail sendiri. Belum ada yang dipasang di `app/src/main`.

| Gaya | Keluarga ikon (lisensi, sumber) | Keadaan ikon | Bentuk rail |
| --- | --- | --- | --- |
| A | Phosphor, MIT, phosphor-icons/core (`assets/bold`, `assets/fill`) | Bold saat tidak terpilih, fill saat terpilih ("More" tetap bold) | Rail tipis 68 dp, ikon 28 dp, label hanya di bawah item terpilih, pil `secondaryContainer` |
| B | Fluent UI System Icons, MIT, microsoft/fluentui-system-icons (`_24_regular`, `_24_filled`) | Regular / filled | Rail melayang: kolom bersudut 36 dp di `surfaceContainerHigh` dengan jarak 12 dp, item terpilih = lingkaran `primary` penuh |
| C | Tabler Icons, MIT, tabler/tabler-icons (`icons/outline`) | Garis 1,75 saat tidak terpilih, 2,6 saat terpilih | Rail klasik 88 dp, label selalu tampil, pil `primary` penuh yang tegas |

Remix Icon tidak dipakai: sejak Januari 2026 lisensinya "Remix Icon License v1.0", bukan MIT atau Apache. Material Symbols dengan bobot lain tidak tersedia sebagai SVG di repo Google (hanya bobot 400).

SVG dan lisensinya ada di `tools/preview/icon-candidates/{phosphor,fluent,tabler}`. Pratinjau membaca path-nya lewat `app/src/uiPreview/.../IconCandidates.kt` (dibangkitkan skrip). Ikon bar atas (search, filter, more vertikal) ikut keluarga masing-masing.

PNG di `/home/user/preview-out9/`: `rail-styles-{portrait,landscape}.png` (berdampingan; di potret tiap sel hanya sekitar 390 dp tinggi, jadi rail B terpotong, lihat versi penuh), `rail-style-{a,b,c}-{portrait,landscape}.png` (ukuran penuh), `rail-icons-closeup-{portrait,landscape}.png` (kelima ikon, tidak terpilih dan terpilih, diperbesar 1,6x).

### Penilaian

1. A, Phosphor slim: paling jelas berbeda dari Material. Glyph bold berbentuk tebal dan geometris, versi fill terbaca sebagai "aktif". Rail paling sempit, jadi daftar dapat ruang paling banyak. Pil memakai `secondaryContainer` yang tenang, cocok dengan tujuan baca di ruang remang. Kelemahannya: label tujuan yang tidak terpilih tersembunyi, jadi orang baru harus menebak dari ikon. Ikon bar atas yang tebal juga lebih berat daripada teks di sekitarnya.
2. B, Fluent floating: bentuk rail paling khas dan lingkaran terpilih paling mencolok. Kelemahannya: ikon regular Fluent bergaris tipis, jadi dalam keadaan tidak terpilih masih mirip Material. Keluhan "ikon tidak berubah" bisa terulang. Lingkaran `primary` penuh juga bidang terang terbesar di layar.
3. C, Tabler bold classic: paling mudah dipakai karena label selalu tampil dan pil terpilih paling tegas. Kelemahannya: ikon garis Tabler masih keluarga outline, jadi bedanya dari Material lebih kecil daripada A. Pil `primary` penuh juga lebih terang daripada gaya tenang lainnya.

Rekomendasi: A [Kemungkinan Besar memenuhi keluhan Peter, karena perubahan bentuk ikonnya paling besar; belum dilihat di tablet]. Kalau Peter ingin label selalu terlihat, C adalah pilihan kedua.

### Putaran 5, lanjutan: gaya A terpasang

Peter memilih A. Ikon Phosphor sekarang ada di modul baru `icons/phosphor`. Polanya sama dengan `icons/simple-icons`: Valkyrie dengan paket `Phosphor.Bold` dan `Phosphor.Fill`, dan lisensi MIT di `icons/phosphor/LICENSE`. Modul itu didaftarkan di `settings.gradle.kts` dan `app/build.gradle.kts`.

Kelima tab memakai bold saat tidak terpilih dan fill saat terpilih ("More" bold di dua keadaan). Ikon ini juga muncul di bar bawah ponsel lewat `TabOptions`. Bar atas memakai `MagnifyingGlass` dan `DotsThreeVertical` di `AppBar.kt` (berlaku untuk semua bar atas), serta `FunnelSimple` di toolbar Library, Manga, Updates, Upcoming, dan Sources.

Di tablet, `HomeScreen` memakai rail tipis sendiri: 68 dp, ikon 28 dp, pil `secondaryContainer` 52x36 dp dengan transisi warna 200 ms dan ripple hanya di pil, label hanya di bawah item terpilih, semantik `Role.Tab`, dan lencana tetap di ikon. Rail ini dipasang lewat `NavigationSuiteScaffoldLayout`. Ponsel tetap memakai `NavigationSuiteScaffold` dengan bar bawah M3.

Kandidat Fluent dan Tabler, `tools/preview/icon-candidates`, `IconCandidates.kt`, dan `RailStylesPreviewTest.kt` sudah dihapus. Render akhir ada di `/home/user/preview-out10/library-list-after-{portrait,landscape,phone}.png`.

## Putaran 6: label rail dan ikon yang tersisa

Rail tablet sekarang menampilkan label kecil (`labelSmall`, 11 sp) di bawah setiap ikon, bukan hanya di item terpilih. Label yang tidak aktif memakai `onSurfaceVariant` dengan bobot Medium (6,3 : 1 di Warm Charcoal, cukup tenang tapi tetap di atas 4,5 : 1). Label yang aktif memakai `onSurface` SemiBold. Lebar rail tetap 68 dp, dan jarak antaritem diturunkan dari 6 ke 4 dp supaya label tidak membuat rail lebih tinggi dari yang perlu.

Ikon Material yang tersisa di layar utama diganti padanan Phosphor bold: kembali (`ArrowLeft`, paket baru `BoldAutoMirrored` agar tercermin di RTL), tutup, pilih semua dan balik pilihan, segarkan, jadwal, hapus riwayat, cari global, bantuan, daftar More, daftar Pengaturan utama, serta bar reader (penanda, peningkatan gambar, putar/jeda auto scroll, pengaturan, bab sebelumnya/berikutnya). Lembar semua ikon yang dipakai dirender di `phosphor-icons-*.png`.
