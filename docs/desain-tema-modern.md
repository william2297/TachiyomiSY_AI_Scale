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
