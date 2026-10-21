# Catatan perubahan

## 0.3.0

**Fase 3 — Toolchain & SDK manager**
- Layar Toolchain: unduh/hapus `android.jar` per API (Sable/android-platforms), distribusi Gradle (services.gradle.org + SHA-256), paket kustom `.zip` (JDK/build-tools/NDK).
- Downloader dengan resume, progress, batal, cek ruang disk, checksum.
- `.sallyide/project.json` per proyek + dialog Pengaturan proyek.
- Info ukuran penyimpanan dan pembersih cache.

**Perbaikan**
- Crash saat menekan snippet atau chip autocomplete di toolbar (lihat README, bagian Perbaikan di v0.3).
- Snippet/chip sekarang mengganti kata yang sedang diketik; tabstop dan lompat dengan TAB; indentasi mengikuti baris.
- Panah ↑ ↓ di extra key row bekerja sebagai navigasi kursor.
- Event kursor tidak lagi menyalin seluruh isi file.
- Laporan crash terakhir tampil saat aplikasi dibuka lagi.

## 0.2.0
- Proyek internal, file explorer tree, template proyek, cari di proyek, import/export zip, workspace restore.

## 0.1.0
- Editor dasar, highlight TextMate, tab, find/replace, extra key row, snippet.
