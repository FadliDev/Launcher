# Sally IDE — MVP Android

Sally IDE adalah editor kode Android berorientasi layar HP, menggunakan Kotlin, Jetpack Compose Material 3, dan Sora Editor. Rancangan proyek lengkap yang menjadi acuan ada di direktori [`docs/`](docs/).

## Fitur yang ada di MVP

- Editor kode dengan nomor baris, tema TextMate, bracket highlight, auto-indent, undo/redo.
- Grammar syntax highlighting dasar untuk Kotlin, Java, JavaScript, TypeScript, HTML, CSS, JSON, Python, C/C++, XML, Gradle/Groovy, Markdown, TOML, INI/properties, YAML, shell, SQL, Rust, Go, PHP, Ruby, Dart, Swift, dan Lua.
- Tab multi-file, indikator perubahan belum disimpan, file baru, buka/simpan lewat Android Storage Access Framework, dan workspace internal.
- Pencarian dan penggantian teks dalam file aktif.
- Bar tombol ekstra untuk TAB, kurung, tanda baca, dan navigasi kursor.
- Snippet bawaan untuk banyak bahasa, pemilih snippet, dan percobaan pengunduhan otomatis saat belum ada pack lokal. Pack yang berhasil diunduh disimpan di penyimpanan aplikasi. Unduhan memerlukan internet; jika offline editor masih bisa dipakai dengan snippet bawaan.
- Tema Light/Dark, Dynamic Color Android 12+, dan mode hitam AMOLED.
- GitHub Actions untuk membuat APK debug dan mengunggahnya sebagai artifact.

## Batasan tahap ini

MVP ini **belum** menjalankan Gradle untuk membangun aplikasi Android dari dalam ponsel. Project explorer berbentuk tree, template proyek lengkap, SDK/NDK manager, terminal, LSP penuh, dan build APK di perangkat direncanakan pada fase berikutnya sesuai `docs/ROADMAP.md`. Grammar yang dibundel merupakan grammar TextMate dasar, belum grammar vendor penuh yang sekomplet VS Code.

## Build lewat GitHub Actions

1. Upload/push isi direktori ini ke repository GitHub.
2. Buka tab **Actions**, jalankan workflow **Sally IDE Android CI** (atau push ke `main`/`master`).
3. Setelah job berhasil, buka run itu dan unduh artifact **SallyIDE-debug-apk**.

Workflow menggunakan JDK 17, Gradle 8.11.1, dan Android SDK platform 35.

## Build lokal

Persyaratan: JDK 17, koneksi internet pertama kali, dan Android SDK platform 35/build-tools 35.0.0.

Linux/macOS/Termux yang mendukung `curl`, `unzip`, dan `sha256sum`:

```sh
./gradlew :app:assembleDebug
```

Di Windows gunakan:

```bat
gradlew.bat :app:assembleDebug
```

Skrip Gradle di root adalah bootstrap sederhana yang mengunduh Gradle 8.11.1 dari server resmi dan memverifikasi SHA-256 sebelum dijalankan. Alternatifnya, buka folder di Android Studio dan sinkronkan proyek dengan Gradle 8.11.1.

APK debug jika build lokal berhasil ada di `app/build/outputs/apk/debug/app-debug.apk`.

## Identitas aplikasi

- Nama: Sally IDE
- Package: `com.sally.ide`
- Minimum Android: 8.0 (API 26)
- Versi awal: `0.1.0`
- Kredit: sally

## Validasi resource

```sh
python3 scripts/validate_assets.py
```

Script ini memvalidasi JSON grammar/theme/snippet dan memastikan tiap grammar yang terdaftar tersedia di assets.
