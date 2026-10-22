# ModForge Android

ModForge adalah fondasi IDE Android untuk membuat mod Minecraft Java Edition berbasis Fabric. Fokus build awal: buat project, edit file teks dasar, sinkronkan ke GitHub, jalankan GitHub Actions, lalu unduh JAR kembali ke perangkat.

> **Status rilis:** fondasi MVP 0.1.0. Ini belum rilis penuh sesuai seluruh PRD. Editor syntax-aware/autocomplete, diagnostics klik-ke-baris, node scripting, editor 3D dan texture painter belum diimplementasikan.

## Yang sudah ada

- UI Jetpack Compose + Material 3, tema terang/gelap, aksen hijau `#3DDC84`.
- Daftar project lokal, wizard project, konfigurasi Minecraft/Java/Gradle per project.
- Generator project Fabric untuk template Empty, Item, Block, Item + Block, dan Event.
- Editor teks dasar untuk file Java, JSON, Gradle, YAML dan file project lain.
- GitHub OAuth Device Flow yang bisa dikonfigurasi menggunakan OAuth Client ID penerbit aplikasi; alternatif Personal Access Token.
- Token GitHub dienkripsi menggunakan AES-GCM dengan kunci Android Keystore.
- Pembuatan repository, pengiriman file melalui Git Data API, pemantauan Actions, pengunduhan artefak JAR, dan menu share.
- GitHub Actions workflow untuk membuild APK Android dan mengunggah artifact.

## Build APK di GitHub Actions

1. Buat repository GitHub baru lalu upload seluruh isi project ini ke branch `main`.
2. Buka tab **Actions** dan aktifkan workflow bila GitHub meminta persetujuan.
3. Jalankan workflow **Build ModForge APK** atau push commit ke `main`.
4. Setelah sukses, buka artifact `modforge-debug-apk`, unduh ZIP artifact lalu ambil `app-debug.apk`.

Workflow menggunakan Gradle yang dipasang oleh `gradle/actions/setup-gradle`; jadi tidak membutuhkan `gradle-wrapper.jar` untuk build di Actions. File `gradlew` disertakan sebagai launcher ringan untuk lingkungan lokal dan akan menggunakan Gradle dari PATH atau mengunduh distribusi yang ditentukan di `gradle/wrapper/gradle-wrapper.properties`.

## Build lokal

Dibutuhkan JDK 17 dan Android SDK platform 37. Jalankan:

```bash
./gradlew :app:assembleDebug
```

APK debug akan berada di `app/build/outputs/apk/debug/app-debug.apk`.

## Mengaktifkan GitHub OAuth Device Flow

Device Flow memerlukan OAuth App terdaftar dan opsi Device Flow aktif. Penerbit aplikasi perlu membuat OAuth App GitHub, kemudian memasukkan **Client ID** ke **Pengaturan → OAuth Device Flow** di aplikasi. Client ID adalah identifier publik, bukan client secret. Untuk aplikasi yang belum punya OAuth App sendiri, layar GitHub juga menyediakan jalur Personal Access Token.

Token klasik perlu scope `repo` dan `workflow` agar dapat membuat repository privat serta mengirim workflow ke repository. Untuk fine-grained token, pastikan izin repository/workflow yang diperlukan tersedia. Hindari menggunakan token dengan akses lebih luas dari yang diperlukan dan gunakan hanya pada perangkat pribadi.

## Catatan versi Minecraft

Matrix versi di UI mengikuti baseline PRD, tetapi rentang 1.16.5, 1.18.2 dan 26.3 ditandai eksperimental. Kombinasi Fabric Loom, mappings, Loader, Fabric API dan Java perlu diverifikasi lewat CI untuk masing-masing versi. Template paling cocok untuk pengujian awal adalah Minecraft 1.21.1 atau 1.20.1. Jangan menganggap semua kombinasi dalam matrix sudah lulus build hanya karena dapat dipilih dari dropdown.

## Struktur utama

```text
app/src/main/java/com/sally/modforge/
  data/       # model project, penyimpanan lokal, controller
  core/       # generator template Fabric
  github/     # OAuth, token vault, GitHub REST/Git Data/Actions
  ui/         # Compose UI, tema
.github/workflows/android.yml
```

## Keamanan & privasi

- Source project disimpan pada direktori privat aplikasi sampai user memilih untuk build/sync ke GitHub.
- Token tidak ditulis ke file project, log build atau repository.
- Repository baru default-nya private.
- Menghapus token dari perangkat tidak otomatis mencabut izin OAuth di GitHub; cabut izin juga di GitHub Settings jika diperlukan.

## Lisensi

Kode fondasi ini disediakan untuk pengembangan pribadi dan dapat diubah sesuai kebutuhan. Template mod menghasilkan proyek Fabric; tetap periksa lisensi dependency dan ketentuan layanan GitHub yang digunakan.

## Validasi source

Generator template sudah diuji untuk contoh 1.21.1 Item + Block, 1.20.1 Event, 1.18.2 Block, dan 26.3 Empty; JSON yang dihasilkan lolos parsing. APK Android penuh belum dikompilasi di lingkungan pembuat source ini, sehingga workflow **Build ModForge APK** harus menjadi verifikasi kompilasi berikutnya. Status build mod Fabric sendiri tetap bergantung pada kompatibilitas dependency tiap versi dan diverifikasi saat workflow project dijalankan.

Dokumen sumber produk ada di `docs/specs/` (`PRD.md`, `PLAN.md`, `DESIGN.md`, `ROADMAP.md`).
