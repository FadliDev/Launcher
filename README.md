# Sally Text Editor — crash fix + code-editor engine patch

Patch ini mengganti editor `EditText` dan highlighter regex buatan sendiri dengan **Sora Editor** (`io.github.Rosemoe.sora-editor`) dan dukungan grammar **TextMate**. Perubahan disimpan sebagai file patch saja; salin/merge isi ZIP ke root repo, lalu push agar GitHub Actions membangun APK.

## Yang berubah

- Menghapus jalur highlighting penuh yang berjalan setiap perubahan teks dan menggantinya dengan syntax analysis incremental Sora Editor. Ini ditujukan untuk mengatasi crash saat mengetik pada implementasi sebelumnya.
- Menambahkan popup autocomplete berbasis identifier dari TextMate serta snippet yang disesuaikan ekstensi file.
- Tombol **Snippets** untuk memilih template HTML, CSS, JavaScript/TypeScript, Python, Java/Kotlin, C/C++, shell, JSON, SQL, dan XML.
- Memakai fitur editor Sora untuk auto-indent, pasangan bracket, undo/redo, selection, dan editor code view.
- Tetap memakai file picker Android untuk Open/Save.

## Bahasa

Grammar universal yang disertakan memberikan highlighting dasar lintas banyak bahasa dan format source, bukan grammar khusus atau semantic analysis untuk benar-benar setiap bahasa. Dukungan lebih presisi untuk bahasa tertentu dapat ditambahkan berikutnya dengan grammar TextMate khusus atau Language Server Protocol (LSP). Autocomplete saat ini merupakan identifier completion dari TextMate ditambah snippet; bukan AI completion atau LSP.

## Build

Jalankan workflow GitHub Actions yang sudah ada. Versi dependency dipatok ke Sora Editor 0.23.6 dan Kotlin 2.1.21; dependency diambil Gradle dari Maven Central. **Build tidak berhasil diverifikasi di lingkungan pembuatan patch ini**, jadi cek hasil workflow setelah push.

Referensi library: https://github.com/Rosemoe/sora-editor
