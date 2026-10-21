# ROADMAP — Sally IDE

Dibuat bertahap. Estimasi waktu kasar untuk 1 developer; sesuaikan dengan kecepatan kerja.

```
Fase 0  Spike & validasi     ██
Fase 1  Editor dasar (MVP)   ████
Fase 2  Proyek & file        ███
Fase 3  SDK/Toolchain mgr    ███
Fase 4  Build Gradle         █████
Fase 5  Intelligence & LSP   ████
Fase 6  NDK / C++            ███
Fase 7  Polish & rilis       ███
Fase 8  Lanjutan             ░░░░
```

---

## Fase 0 — Spike & validasi (1–2 minggu)
Tujuan: pastikan ide ini feasible sebelum bangun UI besar.

- [ ] Jalankan JDK arm64 dari dalam app (coba opsi A/B/C di `PLAN.md` §4.5)
- [ ] Gradle "hello" jalan, bikin APK dari proyek Android minimal
- [ ] aapt2 arm64 override berhasil dipakai AGP
- [ ] Build mod Fabric kecil jadi jar
- [ ] Compile `.cpp` sederhana pakai clang NDK
- [ ] Cek struktur repo `sable/android-platforms` (apakah cukup untuk AGP)
- [ ] **Keputusan:** strategi eksekusi biner + distribusi

**Exit criteria:** minimal 1 build APK sukses di HP nyata.

## Fase 1 — Editor dasar / MVP (3–4 minggu)
- [ ] Setup proyek: Compose, MD3, modul, CI GitHub Actions
- [ ] Integrasi Sora-editor
- [ ] Highlighting Java, Kotlin, C++, XML, Gradle, JSON
- [ ] Tab multi-file, undo/redo, find & replace
- [ ] Auto indent, auto close bracket, bracket match, folding
- [ ] Extra key row
- [x] Snippet engine + snippet bawaan (tabstop, placeholder, TAB untuk lompat)
- [x] Auto complete dasar (keyword + snippet + kata di file) — chip di atas extra key row; popup bawaan Sora dimatikan
- [ ] Tema editor: light/dark + dynamic color MD3

**Rilis:** `v0.1` — editor teks yang enak dipakai.

## Fase 2 — Proyek & file explorer (2–3 minggu)
- [x] Buka folder proyek (storage internal app: `files/projects/<nama>`)
- [ ] Buka folder proyek dari penyimpanan luar lewat SAF
- [x] Sidebar tree file explorer + icon/warna per tipe file
- [x] Buat/rename/hapus/duplikat (long-press menu)
- [ ] Pindah file, drag & drop, multi-select
- [x] Pencarian file (fuzzy) + cari teks seluruh proyek (klik hasil lompat ke baris)
- [x] Import/export zip (proteksi zip-slip, batas ukuran)
- [x] Proyek terakhir, workspace restore (tab terbuka + proyek aktif)
- [x] Layout adaptif: drawer di HP, sidebar tetap di layar ≥ 720dp
- [ ] Divider sidebar/panel bawah yang bisa digeser
- [x] Template proyek (Android Kotlin, Android Java, Mod Fabric, kosong)

**Rilis:** `v0.2` (versi 0.2.0 di `app/build.gradle.kts`).

## Fase 3 — SDK & toolchain manager (2–3 minggu)
- [x] Layar "Toolchain": Gradle, SDK platform; JDK/build-tools/NDK lewat paket kustom `.zip` (URL sendiri)
- [x] Downloader (resume, checksum, progress, hapus)
- [x] Unduh `android.jar` per API dari `sable/android-platforms`
- [ ] Generate struktur SDK agar dikenali AGP (baru `platforms/android-N/android.jar` + `source.properties`; `package.xml`, `build.prop`, `data/` menyusul)
- [x] Setting per proyek `.sallyide/project.json`
- [x] Info ukuran storage + bersihkan cache

**Rilis:** `v0.3` (versi 0.3.0). Sisa: dukungan `.tar.gz/.tar.xz`, sumber JDK/NDK arm64 yang terverifikasi, struktur SDK penuh untuk AGP.

## Fase 4 — Build Gradle (4–5 minggu)
- [ ] Foreground service + process runner
- [ ] Jalankan task Gradle (assembleDebug, build, clean)
- [ ] **Pilih versi Gradle per proyek** (wrapper rewrite / distribusi terkelola)
- [ ] Pilih JDK per proyek
- [ ] Init script otomatis (aapt2 override, SDK path, heap)
- [ ] Panel output build + parsing error → klik lompat ke baris
- [ ] Gradle Tooling API: daftar task, cancel, progress
- [ ] Install APK hasil build / share jar mod
- [ ] Signing debug otomatis; keystore manager sederhana
- [ ] Peringatan kompatibilitas Gradle ↔ AGP ↔ JDK

**Rilis:** `v0.4` — **bisa build app Android & mod Minecraft dari HP.** (Milestone utama)

## Fase 5 — Intelligence & LSP (4 minggu)
- [ ] Indexer ringan (tree-sitter) → completion simbol, go-to-definition
- [ ] Klien LSP generik: diagnostics, completion, hover, signature, definition
- [ ] LSP Manager (install, start/stop, status, batas memori)
- [ ] kotlin-language-server
- [ ] jdtls (opsional, manual aktif)
- [ ] Classpath otomatis dari Gradle + `android.jar`
- [ ] Format code, rename symbol, quick fix, organize imports
- [ ] Command palette

**Rilis:** `v0.5`.

## Fase 6 — NDK / C++ (3 minggu)
- [ ] Unduh & pilih versi NDK per proyek
- [ ] Template NDK (CMake)
- [ ] Build `.so` via Gradle externalNativeBuild
- [ ] clangd + `compile_commands.json`
- [ ] Highlight/completion C++ lebih baik

**Rilis:** `v0.6`.

## Fase 7 — Polish & rilis publik (3 minggu)
- [ ] Terminal bawaan
- [ ] Git dasar (JGit): clone, status, commit, diff
- [ ] Tema editor tambahan + custom
- [ ] Shortcut keyboard fisik ala VSCode
- [ ] Template mod Forge/NeoForge
- [ ] Performa: file besar, memori, baterai
- [ ] Crash reporting opsional, onboarding, dokumentasi
- [ ] Lokalisasi ID/EN
- [ ] Beta tertutup → perbaikan → **v1.0**

## Fase 8 — Lanjutan (backlog)
- Debugger (JDWP), logcat viewer, preview layout XML / Compose
- Ekstensi/plugin (snippet pack, tema, grammar)
- Split editor, minimap, multi-cursor
- Remote build (opsional, ke PC/server) untuk HP lemah
- Sinkron settings, backup proyek
- Emulator Minecraft mod test (run client dari app, bila memungkinkan)

---

## Milestone ringkas

| Versi | Isi utama | Target |
|---|---|---|
| v0.1 | Editor + highlight + snippet | Fase 1 |
| v0.2 | File explorer + proyek + template | Fase 2 |
| v0.3 | Toolchain & SDK manager | Fase 3 |
| **v0.4** | **Build Gradle, ganti versi Gradle** | Fase 4 |
| v0.5 | LSP + intelligence | Fase 5 |
| v0.6 | NDK/C++ | Fase 6 |
| v1.0 | Polish, Git, terminal, rilis | Fase 7 |

## Urutan prioritas kalau mau dipercepat
Fase 0 → Fase 1 → Fase 3 → Fase 4 (build dulu), baru Fase 2 yang lengkap dan Fase 5. Dengan begitu fitur paling berharga (build di HP) bisa dicoba lebih awal.

## Definisi "selesai" per fase
Fitur jalan di perangkat nyata, tidak crash, ada catatan perubahan, dan APK debug dari CI bisa diunduh.
