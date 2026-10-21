# PLAN — Sally IDE

Rencana teknis: arsitektur, teknologi, dan cara ngatasin bagian yang paling susah (build Gradle + toolchain di HP).

---

## 1. Gambaran arsitektur

```
┌──────────────────────────────────────────────┐
│ UI (Compose MD3 + Sora Editor)               │
│  Explorer · Tabs · Editor · Panel · Palette  │
├──────────────────────────────────────────────┤
│ Core (Kotlin)                                │
│  Project · Workspace · Settings · Snippets   │
│  Indexer · Search · Git                      │
├───────────────┬──────────────┬───────────────┤
│ LSP Client    │ Build Engine │ SDK Manager   │
│ (lsp4j)       │ (Gradle)     │ (download)    │
├───────────────┴──────┬───────┴───────────────┤
│ Process Runner (foreground service)          │
│  JDK · Gradle · aapt2 · d8 · clang · clangd  │
└──────────────────────────────────────────────┘
```

Prinsip: berat-berat (Gradle, LSP, clang) **selalu di proses terpisah** dan dikontrol lewat service, supaya UI tetap ringan.

## 2. Tech stack

| Area | Pilihan | Catatan |
|---|---|---|
| Bahasa | Kotlin | |
| UI | Jetpack Compose + Material 3 | dynamic color |
| Editor | **Sora-editor** (Rosemoon) | sudah ada TextMate, tree-sitter, LSP module, auto-complete |
| Highlighting | TextMate grammar (awal) → tree-sitter (lanjut) | grammar dari VSCode bisa dipakai |
| LSP client | Eclipse **lsp4j** (via sora-editor-lsp) | JSON-RPC lewat stdin/stdout |
| DB lokal | Room | proyek terakhir, snippet, index |
| Preferensi | DataStore | |
| Download | OkHttp + resume + checksum | |
| Arsip | commons-compress / xz | tar.xz, zip |
| Git | JGit | |
| Build app | Gradle KTS + version catalog, GitHub Actions | |
| Icon | Material Symbols + set icon file type (mis. vscode-icons / Material Icon Theme, cek lisensi) | |

## 3. Modul proyek

```
app/                 → entry, navigasi, DI
core-model/          → Project, FileNode, Settings
core-ui/             → tema MD3, komponen umum
feature-explorer/    → sidebar tree file
feature-editor/      → tab, editor, snippet, completion
feature-search/      → cari file & teks
feature-build/       → UI build, output, task list
feature-sdk/         → manager JDK/Gradle/SDK/NDK
feature-lsp/         → klien & manajer server LSP
feature-git/         → git dasar
feature-terminal/    → terminal
runtime/             → process runner, env, path toolchain
```

## 4. Strategi build Gradle (bagian tersulit)

### 4.1 Komponen yang dibutuhkan di HP
- **JDK 17 (dan 21 untuk MC 1.20.5+/1.21, 8 untuk legacy)** build aarch64 Linux/Android.
- **Gradle** distribusi (zip) — app yang unduh dan kelola, atau ikut `gradle-wrapper.properties`.
- **aapt2** versi arm64 Android, **d8/r8**, **apksigner**, **zipalign** (build-tools).
- **android.jar** untuk compileSdk.
- **NDK** host aarch64 (clang, lld, sysroot) bila pakai C++.

### 4.2 Fitur "ubah versi Gradle per proyek"
1. Baca `gradle/wrapper/gradle-wrapper.properties` proyek.
2. UI Settings Proyek → dropdown versi Gradle (daftar dari services.gradle.org atau yang sudah terunduh).
3. Opsi A: tulis ulang `distributionUrl` di wrapper properties.
4. Opsi B: pakai distribusi Gradle terkelola app (`GRADLE_HOME` per proyek), abaikan wrapper.
5. Simpan konfigurasi di `.sallyide/project.json` (gradleVersion, jdkVersion, compileSdk, ndkVersion, jvmArgs).
6. Cek kompatibilitas Gradle ↔ AGP ↔ JDK dan tampilkan peringatan (tabel kompatibilitas bawaan).

### 4.3 Tooling API
Jalankan Gradle lewat **Gradle Tooling API** (proses JVM helper kecil) supaya dapat: daftar task, progress, error terstruktur, cancel build. Fallback: spawn `gradle` CLI.

### 4.4 Override untuk lingkungan HP
Inject lewat init script (`~/.gradle/init.d/sallyide.gradle`) dan `gradle.properties`:
- `android.aapt2FromMavenOverride=<path aapt2 arm64>` (AGP bawaan Maven itu binary x86).
- `org.gradle.jvmargs=-Xmx...` sesuai RAM.
- Path `android.jar`/SDK diarahkan ke folder SDK terkelola app.
- `ANDROID_HOME` / `sdk.dir` di `local.properties` otomatis ditulis.

### 4.4 Spike wajib sebelum lanjut (Fase 0)
Buktikan dulu di perangkat nyata:
1. Biner JDK bisa dieksekusi dari app (lihat 4.5).
2. Gradle build "Hello World" Android sukses menghasilkan APK.
3. Gradle build mod Fabric sukses menghasilkan jar.
4. NDK compile `.cpp` sederhana jadi `.so`.

### 4.5 Cara eksekusi biner (batasan Android 10+)
| Opsi | Kelebihan | Kekurangan |
|---|---|---|
| A. Kemas biner sebagai `lib*.so` di `jniLibs` lalu exec dari `nativeLibraryDir` | Jalan di targetSdk modern | Ukuran APK besar, update toolchain harus lewat APK |
| B. `targetSdk` ≤ 28 + exec dari data dir | Simpel, fleksibel | Tidak bisa Play Store, makin dibatasi OS |
| C. proot/rootfs (seperti Termux proot-distro) | Lingkungan Linux utuh | Lebih lambat, lebih kompleks |

Rekomendasi: **mulai dengan A untuk biner inti kecil (loader, aapt2) + B/C untuk toolchain besar**, putuskan setelah spike. Distribusi lewat GitHub/F-Droid dulu.

## 5. SDK Manager (android.jar dari sable/android-platforms)

Repo: `https://github.com/sable/android-platforms` — berisi `android.jar` per API level.

Alur:
1. Ambil daftar API level (GitHub API `contents` atau index JSON yang di-cache).
2. User pilih level (mis. 34, 35, 36) → unduh `android.jar`.
3. Simpan: `<sdk>/platforms/android-<N>/android.jar`.
4. Tulis `source.properties` minimal + `package.xml` supaya AGP mengenali platform.
5. Tambahan yang AGP butuh dari platform: `build.prop`, `data/`, `core-for-system-modules.jar`, `framework.aidl`. **Cek apakah repo itu menyertakannya**; kalau tidak, ambil dari sumber lain atau generate stub.
6. Checksum (SHA-256) disimpan lokal setelah unduh pertama; resume download aktif.

> Catatan: periksa struktur repo dan lisensi saat implementasi. Repo pihak ketiga bisa berubah/hilang, jadi sediakan mirror & opsi URL custom.

## 6. LSP

- **Fase awal:** indexer ringan sendiri (parse import/class/function via tree-sitter) → completion simbol & go-to-definition dasar tanpa server berat.
- **Lanjut:** server sebagai proses terpisah, komunikasi stdio.
  - Java: jdtls (butuh JDK 17+, berat) — opsional, aktif manual.
  - Kotlin: kotlin-language-server.
  - C/C++: clangd dari NDK + `compile_commands.json`.
- LSP Manager: install/update server, start/stop per workspace, status di status bar, batas memori, auto-kill saat idle.
- Android SDK jar dimasukkan ke classpath server agar completion `android.*` jalan.

## 7. Editor

- Sora-editor: line number, gutter, fold, current-line, selection handle, magnifier.
- Snippet engine: format ala VSCode (`$1`, `${1:default}`, `$0`), file JSON per bahasa, snippet user.
- Completion: sumber gabungan (snippet + keyword + simbol file + LSP), dengan ranking.
- Extra key row, gesture (pinch zoom font), mode baca.
- File besar: baca streaming/chunk, batasi highlighting.

## 8. Template proyek

| Template | Isi |
|---|---|
| Android Kotlin (Compose MD3) | AGP, Kotlin, Compose, version catalog |
| Android Java (View) | |
| Android NDK (C++) | CMake + `native-lib.cpp` |
| Mod Fabric | fabric-loom, `fabric.mod.json`, versi MC dipilih |
| Mod Forge / NeoForge | sesuai versi MC |
| Library / Kotlin JVM | |

Template disimpan sebagai aset zip dan di-render dengan placeholder (nama, package, versi).

## 9. Testing & CI

- Unit test core (indexer, snippet, parser gradle properties).
- Instrumented test di perangkat arm64 nyata untuk runtime/build.
- GitHub Actions: build debug/release, lint, upload APK artifact.
- Matriks uji: Android 10, 12, 14+, RAM 4/6/8 GB.

## 10. Risiko utama (ringkas)

1. Eksekusi toolchain di Android modern → ditangani lewat Fase 0 spike.
2. Memori Gradle → atur heap, no-daemon default, peringatan RAM.
3. Ketersediaan biner aarch64 (JDK, aapt2, NDK) → siapkan skrip build/mirror sendiri.
4. LSP Java berat → opsional + indexer ringan dulu.
