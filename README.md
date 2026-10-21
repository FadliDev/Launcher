# Sally Text Editor

Tahap 1 — fondasi Android native untuk text editor dan code editor.

## Fitur saat ini
- Kotlin + AndroidX
- Material Design 3 DayNight theme
- Editor teks native berbasis `EditText` monospace
- Buat dokumen baru, buka file melalui Android Storage Access Framework, simpan dan Save As
- Status bar editor: nomor baris, kolom, jumlah karakter, dan status perubahan
- GitHub Actions workflow untuk build APK debug dan mengunggah artifact

## Build
Project ini menggunakan Android Gradle Plugin 8.7.3 dan Kotlin 2.0.21, dengan JDK 17 dan Android SDK 35.

Workflow GitHub Actions mengunduh Gradle 8.9 secara langsung, jadi tidak memerlukan Gradle wrapper pada tahap awal. Untuk build lokal, gunakan Gradle 8.9:
```bash
gradle :app:assembleDebug
```

Artifact lokal:
`app/build/outputs/apk/debug/app-debug.apk`

## Tahap berikutnya
1. Engine dokumen sendiri dengan undo/redo dan model tab.
2. Nomor baris dan syntax highlighting terintegrasi.
3. Autocomplete, bracket matching, pencarian dan replace.
4. File explorer untuk direktori proyek.
