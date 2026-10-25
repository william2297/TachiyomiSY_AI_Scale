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

1. Kompilasi belum dijalankan. Tidak ada Android SDK di sesi ini. Risiko kompilasi terbesar yang saya lihat adalah pemanggilan konstruktor `Typography(...)` dengan 15 argumen, karena kelas itu juga punya konstruktor 30 argumen. Menurut aturan resolusi overload Kotlin, kandidat yang tidak memakai nilai default menang, dan pustaka itu sendiri memanggil `Typography()` tanpa masalah [Kemungkinan Besar]. Kalau build gagal di baris itu, ganti dengan `Typography().copy(...)`.
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
