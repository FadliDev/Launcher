# PLAN — ModForge Studio (Rencana Teknis)

## 1. Tech Stack
| Layer | Pilihan |
|---|---|
| Bahasa app | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Arsitektur | MVVM + Clean Architecture, modular Gradle |
| DI | Hilt |
| Database | Room (metadata project, cache versi) |
| Settings | DataStore |
| Async | Coroutines + Flow |
| Network | Ktor/OkHttp + kotlinx.serialization |
| Editor teks | Sora Editor (atau CodeEditor Compose) + Tree-sitter |
| 3D viewport | OpenGL ES 3 / Filament (render cube model) |
| Visual scripting | Canvas Compose custom (node graph) |
| Build | GitHub Actions (REST API + OAuth Device Flow), WorkManager untuk push/poll/unduh |
| Git | GitHub Git Data API (blob/tree/commit) atau JGit |
| Secret storage | Android Keystore + EncryptedSharedPreferences |

## 2. Struktur Modul
```
app/                    -> entry, navigation, DI
core/common, core/ui    -> util, komponen UI, tema
core/storage            -> file system, cache, workspace
feature/projects        -> list, create, settings project
feature/buildconfig     -> pilih Java/Gradle, custom Gradle & workflow
feature/editor          -> code editor, file tree
feature/github          -> OAuth, repo, commit/push, sinkronisasi
feature/build           -> trigger run, status, log, unduh artifact
feature/visualscript    -> node graph + codegen
feature/modeleditor     -> 3D block/item editor
feature/templates       -> template pack manager
domain/                 -> use case, model
data/                   -> repository, remote (Mojang, loader meta)
codegen/                -> generator Java per target (loader × versi)
```

## 3. Data Model Utama
```
Project {
  id, name, modId, package, version, author, description, license, iconPath
  loader: FABRIC | FORGE | NEOFORGE
  mcVersion
  toolchain: ToolchainProfile
  createdAt, updatedAt
}
ToolchainProfile {
  javaVersion, gradleVersion, loaderPluginVersion, mappings, jvmArgs, gradleProps,
  buildCommand, workflowMode(MANAGED|CUSTOM), branch, lastPushedCommit, repoFullName
}
VersionProfile (data-driven, dari JSON) {
  mcVersion, loader, javaRange, gradleRange, pluginVersion, mappingsOptions,
  itemModelFormat, packFormat, templateId
}
```
Semua aturan per versi disimpan di **`version-profiles.json`** yang bisa di-update dari server, jadi tidak perlu rilis app tiap ada versi baru.

## 4. Build Config Manager (pengganti Toolchain lokal)
Tidak ada JDK/Gradle di device. Pilihan per project hanya menghasilkan file konfigurasi:
1. `java-version` di step `actions/setup-java` pada workflow.
2. `distributionUrl` di `gradle/wrapper/gradle-wrapper.properties` (Gradle per project).
3. Plugin loader (Fabric Loom / ForgeGradle / NeoGradle) di `build.gradle`.
4. Matriks kompatibilitas (acuan awal, **verifikasi saat implementasi**):
   - MC 1.16.5 → Java 8 (Forge) / Java 8–16 (Fabric tergantung Loom)
   - MC 1.17–1.20.4 → Java 16/17
   - MC 1.20.5–1.21.x → Java 21
   - MC 26.x → Java sesuai `javaVersion` di Mojang version manifest (kemungkinan 25)
5. Mode **Managed**: app regenerasi file bertanda `# managed by ModForge`. Mode **Custom**: file yang diedit user tidak pernah ditimpa; app hanya memberi peringatan jika setting project bentrok dengan isi file.

## 5. GitHub Integration & Build Pipeline

### 5.1 Connect
- OAuth **Device Flow** (user buka github.com/login/device, masukkan kode); tidak perlu client secret di app.
- Scope minimum: `repo` dan `workflow`. Token disimpan di Keystore.

### 5.2 Repo
- Saat project dibuat: `POST /user/repos` dengan nama = slug nama project (huruf kecil, spasi jadi `-`, karakter ilegal dibuang). Jika sudah ada, tanya pakai repo itu atau tambah suffix.
- Commit awal berisi template, `.gitignore`, `.github/workflows/build.yml`.

### 5.3 Push saat tombol Build ditekan
0. **Save** hanya menulis ke penyimpanan lokal project (tidak ada akses jaringan).
1. Build ditekan → hitung diff file (hash) terhadap `lastPushedCommit`.
2. Jika ada perubahan, bikin satu commit lewat Git Data API (blobs → tree → commit → update ref) supaya atomik dan tanpa git lokal.
3. Pesan commit otomatis (mis. `Update from ModForge: 3 files`).
4. Offline → Build menampilkan "butuh internet"; perubahan tetap aman di lokal sampai Build berikutnya.
5. Jika tidak ada perubahan: tawarkan re-run build terakhir (`workflow_dispatch`).
6. Jika remote berubah (HEAD berbeda): tampilkan dialog pull/merge sebelum push.
7. Setelah push sukses, lanjut ke pemantauan run (5.5).

### 5.4 Workflow
Contoh `build.yml` yang dihasilkan (Java dan Gradle dari profil project):
```yaml
name: Build
on:
  push:
    branches: [main]
  workflow_dispatch:
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21        # dari profil project
      - uses: gradle/actions/setup-gradle@v4
      - run: ./gradlew build      # buildCommand bisa dicustom
      - uses: actions/upload-artifact@v4
        with:
          name: mod-jar
          path: build/libs/*.jar
```
(Versi action dicek saat implementasi.)

### 5.5 Pantau & Terima Artifact
1. Setelah push, cari run terbaru untuk commit SHA (`GET /repos/{o}/{r}/actions/runs?head_sha=`).
2. Poll status (backoff) di WorkManager; notifikasi saat selesai.
3. Sukses → `GET /actions/runs/{id}/artifacts` → unduh zip → ekstrak `.jar` ke `projects/<id>/artifacts/<run>/`.
4. Gagal → ambil log job, parse error Gradle/Java → tampil di panel Problems dengan klik-ke-baris.
5. Retensi artifact GitHub terbatas, jadi unduh otomatis segera setelah sukses.

### 5.6 Custom Gradle
- Editor bebas untuk `build.gradle`, `settings.gradle`, `gradle.properties`, `gradle-wrapper.properties`, dan workflow YAML.
- Setting tambahan: build command, argumen Gradle, env variable, matrix versi Java, secrets repo (tulis-saja).
- Validasi ringan (YAML lint, cek field wajib) sebelum push.

## 6. Visual Scripting
- Format graph disimpan JSON (`scripts/*.vs.json`).
- Pipeline: Graph → IR (intermediate representation) → Java generator per target (`FabricGen_1_20`, `ForgeGen_1_16`, ...).
- Generator memakai **adapter API per versi** supaya node yang sama (mis. "OnBlockBreak") menghasilkan kode event yang benar di tiap loader.
- Validasi graph (tipe port, siklus, node tidak terhubung) sebelum generate.

## 7. 3D Model Editor
- Parser/serializer JSON Java Block/Item model (elements, from/to, rotation, faces, uv, display, textures, parent).
- Render: kubus textured, grid, gizmo move/rotate/scale, picking face.
- Undo/redo berbasis command pattern.
- Adapter output per versi (format item model & blockstate berubah di versi baru).
- Texture editor: pensil, eraser, fill, color picker, mirror, layer sederhana.

## 8. Template System
- Template pack: `template.zip` + `manifest.json` (loader, mcVersion range, variabel, file yang di-render).
- Engine templating: placeholder `{{modId}}`, `{{package}}`, dll.
- Template minimum per kombinasi: Empty, Item, Block, Command.

## 9. Testing
- Unit test: codegen, parser model, resolver toolchain.
- Integration test (CI): build template untuk tiap kombinasi versi × loader (matrix GitHub Actions) supaya template tidak rusak.
- Test GitHub client dengan mock server (commit, run polling, unduh artifact, rate limit, konflik).
- UI test: Compose test untuk alur buat project.
- Beta tertutup di beberapa device (RAM 4/6/8/12 GB).

## 10. Tim & Peran (indikatif)
- 1–2 Android dev, 1 dev tooling/Gradle, 1 dev 3D/graphics, 1 desainer UI, 1 tester modding.
