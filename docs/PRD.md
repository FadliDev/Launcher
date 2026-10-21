# PRD — Sally IDE (nama sementara)

IDE / text editor ala VSCode untuk Android. Dipakai buat bikin aplikasi Android dan mod Minecraft langsung dari HP, tinggal edit lalu build.

| | |
|---|---|
| Versi dokumen | 0.1 |
| Platform | Android 8.0+ (minSdk 26), target arm64-v8a |
| UI | Material Design 3 (Material You) |
| Bahasa app | Kotlin + Jetpack Compose (editor pakai View/Sora-editor) |

---

## 1. Latar belakang & masalah

- Bikin app Android dan mod Minecraft biasanya butuh PC. Banyak yang cuma punya HP.
- Editor Android yang ada biasanya kurang lengkap (tanpa LSP, tanpa Gradle fleksibel, UI kuno).
- Setiap proyek (terutama mod Minecraft) butuh versi Gradle/JDK yang beda-beda.

## 2. Tujuan

1. Editor kode yang nyaman di layar kecil, rasa VSCode.
2. Support Java, Kotlin, C++ (NDK), dan XML Android.
3. Build Gradle langsung di HP, versi Gradle bisa diganti per proyek.
4. SDK (`android.jar`) dan NDK bisa diunduh & dikelola dari dalam app.
5. UI MD3 lengkap dengan icon.

### Non-goals (v1)
- Emulator Android di dalam app.
- Debugger penuh (breakpoint, step). Dipertimbangkan v2.
- Sinkronisasi cloud / kolaborasi realtime.
- Support iOS / desktop.

## 3. Target pengguna

| Persona | Kebutuhan |
|---|---|
| Modder Minecraft | Edit mod Fabric/Forge/NeoForge, pilih JDK & Gradle sesuai versi MC, build jar |
| Developer app Android | Edit Kotlin/Java/XML, build APK, pakai NDK kalau perlu |
| Pelajar / pemula | Template proyek siap pakai, error jelas |

## 4. Fitur & prioritas

Legenda: **P0** wajib v1, **P1** v1.x, **P2** nanti.

### 4.1 Editor
| ID | Fitur | Prio |
|---|---|---|
| ED-1 | Syntax highlighting (TextMate/tree-sitter) Java, Kotlin, C/C++, XML, Gradle (Groovy/KTS), JSON, TOML, Markdown | P0 |
| ED-2 | Tab banyak file, split view | P0 / P1 |
| ED-3 | Undo/redo, find & replace (regex), go to line | P0 |
| ED-4 | Snippet (bawaan + custom user) | P0 |
| ED-5 | Auto complete (keyword, snippet, simbol file, lalu LSP) | P0 |
| ED-6 | Auto indent, auto close bracket/quote, bracket matching, code folding | P0 |
| ED-7 | Extra key row (Tab, `{ } ( ) ; < >`, panah, Ctrl) untuk keyboard HP | P0 |
| ED-8 | Format code, rename symbol, quick fix | P1 |
| ED-9 | Minimap, sticky scroll, multi-cursor | P2 |
| ED-10 | Dukungan keyboard fisik + shortcut ala VSCode | P1 |

### 4.2 Language intelligence (LSP)
| ID | Fitur | Prio |
|---|---|---|
| LS-1 | Klien LSP generik (lsp4j): diagnostics, completion, hover, definition | P0 |
| LS-2 | Server Java (jdtls atau alternatif ringan) | P1 |
| LS-3 | Server Kotlin (kotlin-language-server) | P1 |
| LS-4 | Server C/C++ (clangd dari NDK) | P1 |
| LS-5 | Server XML / JSON / TOML | P2 |
| LS-6 | Fallback ringan: indexer sendiri (simbol + import) bila server berat | P0 |

### 4.3 Proyek & file
| ID | Fitur | Prio |
|---|---|---|
| FM-1 | Sidebar tree file explorer (expand/collapse, icon per tipe file) | P0 |
| FM-2 | Buat/rename/hapus/duplikat/pindah file & folder, drag & drop | P0 |
| FM-3 | Buka folder proyek dari penyimpanan internal; import/export zip | P0 |
| FM-4 | Pencarian file (fuzzy) dan pencarian teks di seluruh proyek | P0 |
| FM-5 | Template proyek: Android App (Kotlin/Compose), Android NDK, Mod Fabric, Mod Forge/NeoForge, Library | P0 |
| FM-6 | Git dasar (init, status, commit, diff, clone) | P1 |
| FM-7 | Riwayat file terakhir / proyek terakhir | P0 |

### 4.4 Build system
| ID | Fitur | Prio |
|---|---|---|
| BD-1 | Jalankan Gradle di HP (tombol Build / Run task) | P0 |
| BD-2 | **Pilih versi Gradle per proyek** (ubah `gradle-wrapper.properties` atau pakai distribusi terkelola app) | P0 |
| BD-3 | Pilih JDK per proyek (8 / 17 / 21) | P0 |
| BD-4 | Output build live + parsing error jadi klik-ke-baris | P0 |
| BD-5 | Daftar task Gradle (assembleDebug, build, clean, dll) | P0 |
| BD-6 | Cache Gradle & offline mode | P1 |
| BD-7 | Install APK hasil build langsung / share jar mod | P0 |
| BD-8 | Signing keystore manager | P1 |
| BD-9 | Gradle daemon on/off, atur heap (memory) | P1 |

### 4.5 SDK & NDK manager
| ID | Fitur | Prio |
|---|---|---|
| SD-1 | Unduh `android.jar` per API level dari repo `https://github.com/sable/android-platforms` | P0 |
| SD-2 | Pilih compileSdk per proyek; app otomatis arahkan path android.jar | P0 |
| SD-3 | Unduh build-tools (aapt2, d8, apksigner versi Android/arm64) | P0 |
| SD-4 | Unduh & pilih NDK (versi bisa lebih dari satu) | P0 |
| SD-5 | Verifikasi checksum, resume download, hapus versi lama | P0 |
| SD-6 | Unduh JDK dan Gradle distribusi | P0 |

### 4.6 UI / tema
| ID | Fitur | Prio |
|---|---|---|
| UI-1 | MD3 penuh: dynamic color, light/dark/AMOLED, adaptive layout | P0 |
| UI-2 | Icon set lengkap (UI + file type + folder) | P0 |
| UI-3 | Tema editor (Dark+, Light+, Monokai, dll) + custom | P1 |
| UI-4 | Tablet / foldable / landscape: sidebar + editor + panel bawah | P0 |
| UI-5 | Command palette (ala Ctrl+Shift+P) | P1 |
| UI-6 | Terminal bawaan (shell + akses toolchain) | P1 |

## 5. Persyaratan non-fungsional

- **Performa:** buka file 1 MB < 1 detik; scroll 60 fps; tidak ANR di UI thread.
- **Memori:** editor idle < 300 MB. Gradle/LSP jalan di proses terpisah supaya bisa di-kill tanpa menutup app.
- **Baterai/panas:** build berjalan sebagai foreground service dengan notifikasi + opsi hentikan.
- **Penyimpanan:** toolchain besar (JDK, NDK, Gradle cache) → tampilkan ukuran dan opsi bersihkan.
- **Keamanan:** unduhan wajib HTTPS + checksum; tidak menjalankan kode dari sumber tak dikenal tanpa konfirmasi.
- **Offline:** editor dan build (setelah cache terisi) bisa dipakai tanpa internet.
- **Lokalisasi:** Indonesia + Inggris.

## 6. Constraint teknis penting

- Android 10+ melarang eksekusi biner dari direktori data yang writable (W^X). Toolchain harus dijalankan lewat salah satu cara: biner dikemas sebagai native lib di `nativeLibraryDir`, atau pakai `targetSdk` rendah, atau loader/proot. Keputusan arsitektur ini harus divalidasi lewat spike (lihat `PLAN.md`).
- JDK, aapt2, clang/NDK harus versi **aarch64 Linux/Android** (bukan binary PC). Perlu build sendiri atau pakai build komunitas.
- Gradle butuh RAM besar (>= 1–2 GB). Perangkat RAM < 4 GB mungkin hanya bisa build proyek kecil.
- Proyek mod Minecraft (Loom/ForgeGradle) mengunduh dependency besar; butuh internet saat pertama.

## 7. Metrik sukses

- Dari install sampai build APK pertama dari template < 10 menit (di luar waktu unduh).
- Template Android Kotlin build sukses di >= 90% perangkat arm64 RAM >= 6 GB.
- Template mod Fabric build sukses dan menghasilkan jar.
- Crash-free session >= 99%.

## 8. Risiko

| Risiko | Dampak | Mitigasi |
|---|---|---|
| Eksekusi biner dibatasi Android | Build tidak jalan | Spike awal, pilih strategi sebelum bikin fitur lain |
| Gradle terlalu berat di HP | Build lambat/OOM | Atur heap, mode no-daemon, peringatan RAM, proyek kecil dulu |
| LSP Java/Kotlin berat | Lag | Mulai dari indexer ringan, LSP opsional, jalan di proses terpisah |
| Toolchain arm64 sulit didapat | Tertunda | Pakai build komunitas, siapkan skrip build sendiri |
| Ukuran toolchain besar | User bingung storage | Install on-demand, tampilkan ukuran |
| Kebijakan Play Store | Ditolak | Distribusi awal lewat APK/GitHub/F-Droid |

## 9. Pertanyaan terbuka

1. Nama final app dan package (mis. `com.sally.ide`)?
2. Distribusi: APK langsung, GitHub Releases, atau Play Store?
3. LSP Java: jdtls penuh atau indexer sendiri dulu?
4. Mod Minecraft: prioritas Fabric dulu atau sekalian Forge/NeoForge?
