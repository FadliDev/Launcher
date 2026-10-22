# ROADMAP — Sally IDE

> Legenda progres: `[ ]` belum · `[~]` kode sudah ditulis, **belum dikompilasi/diuji di perangkat** · `[x]` terverifikasi di perangkat. Detail di `/PROGRESS.md`.

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
- [~] Setup proyek: Compose, MD3, modul, CI GitHub Actions
- [~] Integrasi Sora-editor
- [~] Highlighting Java, Kotlin, C++, XML, Gradle, JSON
- [~] Tab multi-file, undo/redo, find & replace
- [ ] Auto indent, auto close bracket, bracket match, folding
- [~] Extra key row
- [~] Snippet engine + snippet bawaan
- [~] Auto complete dasar (keyword + snippet + kata di file)
- [~] Tema editor: light/dark + dynamic color MD3

**Rilis:** `v0.1` — editor teks yang enak dipakai.

## Fase 2 — Proyek & file explorer (2–3 minggu)
- [~] Buka folder proyek — **storage app saja** (proyek = subfolder `workspace/`); buka folder bebas via SAF belum
- [~] Sidebar tree file explorer + icon per tipe file
- [~] Buat/rename/hapus/duplikat (pindah & multi-select belum)
- [~] Pencarian file (fuzzy) + cari teks seluruh proyek
- [~] Import/export zip (SAF)
- [~] Daftar proyek terakhir, workspace restore (tab terbuka)
- [~] Layout adaptif: HP (drawer) / tablet & landscape lebar (pane tetap)
- [~] Template proyek (Android Kotlin, Android Java, Fabric, Kosong)

**Rilis:** `v0.2`.

## Fase 3 — SDK & toolchain manager (2–3 minggu)
- [ ] Layar "Toolchain": JDK, Gradle, SDK, build-tools, NDK
- [ ] Downloader (resume, checksum, progress, hapus)
- [ ] Unduh `android.jar` per API dari `sable/android-platforms`
- [ ] Generate struktur SDK agar dikenali AGP
- [ ] Setting per proyek `.sallyide/project.json`
- [ ] Info ukuran storage + bersihkan cache

**Rilis:** `v0.3`.

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
