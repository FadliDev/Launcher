# PLAN — Arsitektur & Rencana Teknis ModForge

Versi dokumen: 0.3 · Tanggal: 2026-10-23 (build via GitHub Actions + code intelligence)

---

## 1. Prinsip Teknis

1. **Iteratif**: rilis kecil yang bisa dites, tiap milestone punya hasil yang bisa dicoba di HP.
2. **Performance-first**: semua proses berat (Gradle, kompilasi, render) di luar main thread; hemat RAM.
3. **Data-driven**: matriks versi, template, dan katalog node dari file data (JSON/YAML), bukan hardcode.
4. **Isolasi per project**: satu project = satu `ProjectConfig` + satu working dir.
5. **Loader-agnostic core**: Fabric dulu, tapi model data & template punya dimensi `loader`.

## 2. Tech Stack

| Layer | Pilihan |
|---|---|
| Bahasa | Kotlin (UI/logic), sedikit C/C++ bila perlu (proses/pty) |
| UI | Jetpack Compose + Material 3 (tema custom minimalis) |
| DI | Hilt atau Koin |
| Async | Coroutines + Flow |
| Storage | Room (metadata), file system (project), DataStore (settings) |
| Code editor | Sora Editor (TextMate/tree-sitter, completion API, modul LSP; verifikasi di M0) + tree-sitter-java/json |
| 3D | OpenGL ES 3.0 (renderer cuboid kustom) di `GLSurfaceView`/`AndroidView` |
| Visual scripting | Canvas Compose kustom (node graph) + codegen engine |
| Build | GitHub Actions (remote); app hanya memanggil GitHub REST API |
| Network / GitHub | OkHttp + kotlinx.serialization (GitHub REST API), WorkManager untuk polling & notifikasi |
| Secure storage | Android Keystore + EncryptedSharedPreferences (token GitHub) |
| Template | Mustache-style engine ringan + manifest JSON |

> Catatan: modul editor kode bisa dipakai ulang dari proyek Sally IDE (Sora Editor + Compose) untuk menghemat waktu.

## 3. Struktur Modul

```
:app                    // entry, navigasi, DI
:core:model             // ProjectConfig, VersionMatrix, dll
:core:data              // Room, repository, file ops
:core:github            // auth (device flow), repo, Git Data API sync, Actions API
:core:build             // BuildBackend (GitHubActionsBackend), orchestrator, log parser, error translator
:core:template          // template engine + manifest + workflow generator
:core:language          // language services: highlight, indent, completion, diagnostics, symbol index, LSP client
:feature:account        // login GitHub, kelola akun
:feature:projects       // list, wizard, settings
:feature:editor         // file tree + code editor
:feature:buildlog       // build UI + histori
:feature:visualscript   // node editor + codegen
:feature:modeleditor    // 3D editor + exporter
:feature:assets         // texture/sound/lang manager
:feature:settings       // tema, bahasa, storage
:codegen:adapters       // adapter API per versi MC (registry, event)
```

## 4. Build via GitHub Actions

Semua kompilasi terjadi di runner GitHub (`ubuntu-latest`). HP hanya mengedit file, sync ke repo, memantau, dan mengunduh jar. Tidak ada JDK/Gradle di device.

### 4.1 Alur end-to-end

```
Tekan Build
  → validasi config (VersionMatrix)
  → regenerate file turunan (visual script → Java, model → JSON, workflow, wrapper)
  → sync ke repo (1 commit via Git Data API)
  → push memicu workflow  (atau workflow_dispatch)
  → cari run berdasarkan head_sha commit
  → polling status job/step
  → selesai: ambil log + annotations
  → sukses: unduh artifact (jar) ke HP
```

Abstraksi: `interface BuildBackend { prepare(), submit(), observe(): Flow<BuildState>, fetchArtifacts(), cancel() }`. Implementasi v1: `GitHubActionsBackend`. Backend lain (lokal/Termux) bisa ditambah tanpa mengubah UI.

### 4.2 Autentikasi GitHub
- **OAuth Device Flow**: app minta kode → user buka `github.com/login/device` dan memasukkan kode → app polling token. Tidak perlu server sendiri atau client secret.
- Scope minimal yang dibutuhkan: pembuatan repo dan push + workflow (umumnya `repo` + `workflow`; verifikasi di M0 apakah GitHub App/fine-grained token lebih sempit).
- Token disimpan di Keystore; tombol Logout/Cabut akses menghapus token lokal dan mengarahkan user ke halaman revoke.

### 4.3 Repo per project
- Saat wizard selesai: `POST /user/repos` (nama dari mod id, visibilitas pilihan user, default private), lalu push commit awal (template + workflow + wrapper).
- Nama bentrok → tambah suffix atau minta user mengganti.
- `project.json` menyimpan: `owner`, `repo`, `branch`, `lastSyncedSha`, `lastRunId`.

### 4.4 Sync engine (Git Data API, tanpa binary git)
1. Hitung hash blob git lokal untuk tiap file (`sha1("blob <len>\0" + bytes)`) dan bandingkan dengan tree remote → **hanya file berubah yang diunggah**.
2. File teks: masukkan konten langsung ke entry tree; file biner (png, jar wrapper): buat blob (base64).
3. `POST /git/trees` (dengan `base_tree`), `POST /git/commits`, `PATCH /git/refs/heads/<branch>`.
4. Jika `ref` remote ≠ `lastSyncedSha` → konflik: tawarkan Pull / Timpa / Branch baru (FR-2.8).
5. File yang dihapus lokal dihapus di tree (entry `sha: null`).
6. `.gitignore` standar (`build/`, `.gradle/`, `.modforge/`, `run/`) agar tidak ikut ter-upload.

### 4.5 Workflow yang digenerate

```yaml
name: Build
on:
  push:
    branches: [main]
  workflow_dispatch:
concurrency:
  group: build-${{ github.ref }}
  cancel-in-progress: true
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "{{java}}"
      - uses: gradle/actions/setup-gradle@v4
      - run: chmod +x gradlew && ./gradlew build --stacktrace
      - uses: actions/upload-artifact@v4
        with:
          name: "{{mod_id}}-jar"
          path: build/libs/*.jar
          retention-days: 7
```

- `{{java}}` diisi dari config project (8/16/17/21/25 sesuai matrix).
- Versi Gradle ditentukan `gradle/wrapper/gradle-wrapper.properties` (`distributionUrl`); `gradle-wrapper.jar` dan `gradlew` dibundel di template.
- Versi action (`@v4`, dst.) harus dicek/di-update lewat remote template repo.
- `concurrency` membatalkan run lama saat user build berulang (hemat kuota).
- Jar sumber (`*-sources.jar`, `*-dev.jar`) difilter agar hanya jar rilis yang ter-upload.

### 4.6 Per-project instance (Java / Gradle / Loom)
| Setting | Disimpan di | Efek |
|---|---|---|
| Minecraft | `gradle.properties` | Dependensi & mappings |
| Java | `build.yml` (`java-version`) + `sourceCompatibility` di `build.gradle` | JDK di runner |
| Gradle | `gradle-wrapper.properties` | Versi Gradle di runner |
| Loom / Fabric Loader / Fabric API | `build.gradle`, `gradle.properties` | Plugin & API |

Mengganti setting → app menampilkan diff file yang akan berubah → konfirmasi → file ditulis ulang dan ikut ter-sync saat build berikutnya.

### 4.7 Pemantauan & hasil
- Cari run: `GET /repos/{o}/{r}/actions/runs?head_sha=<sha>`.
- Status step: `GET /actions/runs/{id}/jobs` (polling adaptif: 3 dtk awal → 10 dtk).
- Log: `GET /actions/jobs/{id}/logs`; parser regex untuk error `javac`/Gradle → daftar diagnostic (file, baris, pesan).
- Annotations: `GET /check-runs/{id}/annotations` sebagai sumber tambahan.
- Artifact: `GET /actions/runs/{id}/artifacts` → unduh zip → ekstrak jar ke `outputs/` project.
- Notifikasi selesai: WorkManager (expedited) memantau run saat app di background.

### 4.8 Kuota & batas
- Repo public: menit Actions gratis; repo private: ada kuota bulanan sesuai paket akun (verifikasi angka terbaru di M0).
- Mitigasi: cache Gradle/Loom, `concurrency` cancel-in-progress, retention artifact singkat, peringatan di UI sebelum build jika kuota hampir habis (jika bisa dibaca).
- Rate limit REST API: baca header `x-ratelimit-*`, backoff otomatis, ETag untuk polling.

### 4.9 Layout data lokal

```
<app files>/
  projects/<id>/
    project.json              // ProjectConfig + info repo
    src/ ...                  // source mod (sumber kerja)
    .github/workflows/build.yml
    gradle/wrapper/...
    .modforge/                // state sync, histori build, log, outputs/
```

### 4.10 ProjectConfig (contoh)

```json
{
  "id": "a1b2c3",
  "name": "My Cool Mod",
  "modId": "mycoolmod",
  "package": "com.sally.mycoolmod",
  "loader": "fabric",
  "minecraft": "1.21.1",
  "java": "21",
  "gradle": "8.10.2",
  "loom": "1.7-SNAPSHOT",
  "fabricLoader": "0.16.x",
  "fabricApi": "0.xx.x+1.21.1",
  "mappings": "mojang",
  "environment": "*",
  "template": "item-block",
  "github": {
    "owner": "sally",
    "repo": "mycoolmod",
    "branch": "main",
    "visibility": "private",
    "lastSyncedSha": "abc123",
    "lastRunId": 0
  },
  "visualScripts": ["scripts/main.graph.json"],
  "models": ["models/ruby_block.mfmodel.json"]
}
```

Nilai versi hanya contoh; diisi dari VersionMatrix.

## 5. Version Matrix Service

- Sumber: Fabric Meta API (game, loader, installer) + file `version-matrix.json` bawaan sebagai fallback offline.
- Tiap entri: `minecraft`, `javaMin`, `javaMax`, `gradleRecommended`, `gradleRange`, `loomVersion`, `mappingsOptions`, `fabricApi`, `packFormat`, `features` (flag seperti `itemModelDefinitions`, `dataComponents`).
- `features` dipakai oleh template engine, codegen, dan exporter model untuk memilih output yang benar.
- Validator: `validate(config) → List<Issue>` dengan saran perbaikan otomatis.

## 6. Template System

```
templates/
  fabric/
    manifest.json               // daftar template + rentang versi
    common/                     // file bersama (gitignore, license, dsb.)
    1.16.5/ ... 1.21.x/ 26.x/   // overlay per grup versi
    items/ blocks/ events/      // fragmen fitur
```

- Render: `base` → overlay grup versi → fragmen fitur → substitusi placeholder.
- Aturan kondisional di manifest (`"when": {"mc": ">=1.20.5"}`).
- Slot loader: `templates/<loader>/` — menambah Forge/NeoForge = menambah folder + adapter, bukan ubah core.
- Test otomatis (CI): generate tiap template × versi, build di server, laporkan matrix hijau/merah.

## 7. Build Orchestration

1. Validasi config + cek login GitHub dan koneksi.
2. Regenerate file turunan (visual script → Java, model → JSON, lang, workflow, wrapper).
3. Sync ke repo (diff hash → satu commit).
4. Tunggu run muncul (`head_sha`), lalu pantau job/step.
5. Parse log/annotations → diagnostic.
6. Sukses: unduh artifact → `outputs/` → tawarkan Share / Copy to mods. Gagal: tampilkan error + saran perbaikan.

Error translator: tabel regex → pesan manusiawi + aksi (mis. "Unsupported class file major version" → "Ganti Java ke 21", tombol sekali tekan yang mengubah config + workflow).

Status machine: `Idle → Validating → Syncing → Queued → Running(step) → Succeeded | Failed | Cancelled`, disimpan agar bisa dilanjutkan jika app ditutup.

## 8. Visual Scripting

### 8.1 Model data
```
Graph { nodes[], edges[], variables[] }
Node  { id, type, props, inputs[], outputs[] }   // port bertipe: Exec, Bool, Number, String, Item, Block, Entity, Player, World, Pos
```

### 8.2 Pipeline codegen
`Graph → validasi tipe → IR (intermediate representation) → Adapter versi → Java source (JavaPoet-style builder)`.

- **IR** netral versi ("on item use → if sneaking → give item").
- **Adapter** menerjemahkan IR ke API MC/Fabric yang benar untuk versi target (nama kelas, signature, perubahan seperti ItemStack/Components, `Identifier`/`ResourceLocation`, dll).
- Node punya `since`/`until` versi; node yang tak didukung ditandai di UI.
- Output di `src/main/java/<pkg>/generated/` dengan header "GENERATED, jangan edit"; opsi eject.

### 8.3 Katalog node awal (MVP skrip)
Event: OnItemUse, OnBlockBreak, OnBlockUse, OnPlayerJoin, OnServerTick.
Aksi: GiveItem, SendMessage, ApplyEffect, SpawnEntity, SetBlock, PlaySound, DamageEntity.
Logika: If, Compare, And/Or/Not, Random, Delay.
Data: Number, String, Item/Block picker (dari registry project), Variable get/set.

## 9. 3D Item/Block Editor

### 9.1 Data model (`.mfmodel.json`, internal)
```
Model { kind: item|block, textures{}, elements[ {from,to,rotation{origin,axis,angle},faces{north,south,...{uv,texture,rotation}}} ], display{gui,ground,fixed,thirdperson_*,firstperson_*,head}, parent? }
```
Format internal ≈ format model JSON vanilla, sehingga export hampir 1:1.

### 9.2 Rendering
- OpenGL ES 3.0, shader sederhana (unlit + shading arah), texture atlas kecil dengan nearest filtering.
- Gizmo move/scale/rotate, grid 16³, snapping 1/16 (dan 0.5/0.25 opsional).
- Batas elemen per model (mis. 64) di mode hemat untuk device low-end.
- Picking: raycast ke cuboid.

### 9.3 Exporter per versi
| Fitur versi | Output |
|---|---|
| Semua | `assets/<ns>/models/{item,block}/*.json`, `textures/`, `lang/en_us.json` |
| Block | `blockstates/*.json`, loot table, tag mining (sesuai versi) |
| ≥ 1.21.4 | `assets/<ns>/items/*.json` (item model definition) |
| Perubahan pack format | Diatur via `packFormat` dari matrix |

Kode registrasi item/block otomatis ditambahkan lewat codegen (fitur "Add to registry").

### 9.4 Texture painter (minimal)
Pensil, penghapus, fill, color picker, mirror; kanvas 16×16/32×32; undo/redo.

## 10. Code Intelligence (Editor, Autocomplete, LSP)

Tujuan: user bisa ngoding Java/JSON/Gradle di HP dengan nyaman, **tanpa JDK di device**. Semua fitur dilayani lewat satu interface, sehingga implementasinya bisa bawaan atau LSP eksternal.

### 10.1 Arsitektur `LanguageService`

```
interface LanguageService {
  highlight(doc): Spans            // via tree-sitter / TextMate
  indentFor(doc, caret): Indent    // auto-indent
  complete(doc, caret): List<Item> // autocomplete
  diagnostics(doc): List<Diag>
  hover(doc, pos): Hover?
  definition(doc, pos): Location?
  signatureHelp(doc, caret): Sig?
  format(doc, range?): List<Edit>
}
```

| Implementasi | Dipakai untuk | Catatan |
|---|---|---|
| `BuiltinJavaService` | Java (default) | tree-sitter + symbol index, jalan di dalam app, ringan |
| `BuiltinJsonService` | JSON model/blockstate/recipe/lang/mod.json | JSON schema bawaan per versi MC |
| `BuiltinGradleService` | `build.gradle`, `gradle.properties` | highlight + snippet + validasi dasar |
| `LspService(transport)` | Opsional (Java/lainnya) | klien LSP generik (JSON-RPC via stdio atau WebSocket) |

### 10.2 Tier fitur

| Tier | Isi | Berjalan di | Target |
|---|---|---|---|
| **0 (dasar)** | Highlight, auto-indent, auto-close, bracket match, snippet, completion keyword + simbol lokal, diagnostics sintaks | HP | MVP editor (M3) |
| **1 (API-aware)** | Completion class/method/field Minecraft & Fabric sesuai versi, auto-import, hover signature, signature help, go-to-definition (project + API stub) | HP + symbol index | M3 |
| **2 (LSP)** | Klien LSP generik; backend eksternal opsional (jdtls via Termux/remote) untuk rename, quick fix, diagnostics semantik penuh | Eksternal | M6 / pasca-v1.0 |

### 10.3 Syntax Highlighting
- tree-sitter (Java, JSON, YAML, Groovy/Kotlin) untuk parse inkremental, TextMate grammar sebagai fallback (Gradle, `.properties`).
- Dua lapis: lexical (instan) lalu semantic ringan (membedakan class/method/field dari index), di-update setelah debounce.
- Warna dari token tema (light/dark) agar konsisten dengan DESIGN.

### 10.4 Auto-Indent
- Berbasis tree-sitter (node kontainer) plus aturan bahasa: setelah `{` → +1 level; `}` → dedent otomatis; Enter di antara `{|}` → buat baris kosong ber-indent dan `}` di baris bawah; lanjutan baris setelah operator/koma di argumen.
- Reindent saat paste (menyesuaikan level indent konteks); Shift-Tab dedent; tab size dan spasi/tab dari pengaturan project (`.editorconfig` dihormati).
- JSON: indent per level, auto-close `{ } [ ] " "`; YAML: pertahankan indent induk.

### 10.5 Autocomplete
- Pipeline `CompletionProvider` berantai: **Keyword → Snippet → Simbol lokal → API index → Schema JSON**, digabung dan di-rank (prefix match > fuzzy, konteks tipe, frekuensi pakai, kedekatan di file).
- Pemicu: mengetik identifier, `.`, `::`, `@`, dan `Ctrl+Space` setara (tombol di key row).
- Konteks: setelah `.` hanya member dari tipe yang dihitung secara heuristik (variabel lokal, `this`, static class); posisi tipe → class; posisi `import` → package/class.
- Snippet khusus modding: registrasi item/block, `ModInitializer`, `ClientModInitializer`, mixin, event callback, recipe/loot JSON; disesuaikan versi MC.
- Auto-import: memilih class yang belum di-import → tambahkan baris `import` di posisi benar.
- Performa: jalan di background, debounce 100–150 ms, hasil dibatasi (mis. 50 item), batalkan permintaan lama saat ketikan baru.

### 10.6 Symbol Index (API Minecraft/Fabric per versi)
Masalah: autocomplete API butuh daftar class/method Minecraft + Fabric API sesuai versi dan mappings project, sedangkan HP tidak punya JDK/dependensi.

Solusi: **index dibuat di CI, diunduh app.**
1. Job CI (repo ModForge sendiri) per kombinasi `(minecraft, mappings, fabricApi)`: memuat jar Minecraft yang sudah di-map + Fabric API, membaca class (ASM/classfile reader), lalu menulis index ringkas (class, tipe, extends/implements, method + signature, field, anotasi penting, deprecated). Hanya nama dan signature, bukan kode sumber.
2. Hasil dikompresi (zstd) dan dipublikasikan sebagai asset rilis; app mengunduh sesuai `ProjectConfig` dan menyimpannya di cache bersama antar project dengan versi sama.
3. Di app: dimuat ke SQLite (FTS) atau struktur trie/memory-mapped agar query < 50 ms dan RAM kecil; dimuat lazy.
4. Dependensi tambahan project (library lain): job opsional di workflow project menghasilkan `index-deps.json` sebagai artifact yang ikut diunduh bersama jar.
5. Fallback jika lisensi/mappings membatasi distribusi index: index dibangun di Actions milik user (job terpisah, sekali per versi) lalu diunduh.

### 10.7 Diagnostics
Tiga sumber digabung ke satu daftar:
1. **Sintaks** (instan, tree-sitter): tanda kurung/titik koma/kutip hilang.
2. **Semantik ringan** (index): import tidak ditemukan, method/class tidak dikenal, deprecated.
3. **Build** (Actions): error javac dari log/annotations; tersimpan di gutter sampai file diedit.

Quick fix awal: tambah import, hapus import tak terpakai, buat variabel/method sederhana, perbaiki typo nama class (saran dari index).

### 10.8 LSP (klien generik)
- Klien JSON-RPC sesuai spesifikasi LSP (initialize, didOpen/didChange, completion, hover, definition, signatureHelp, publishDiagnostics, formatting, rename, codeAction); Sora Editor menyediakan modul LSP yang bisa dipakai ulang (verifikasi di M0).
- Transport: stdio (proses lokal) atau WebSocket (remote).
- **Kenapa bukan jdtls di HP sebagai default:** jdtls butuh runtime Java baru, memori besar, serta import project Gradle (mengunduh dependensi dan men-remap Minecraft), yang bertabrakan dengan keputusan build hanya di GitHub dan membebani device. Karena itu v1 memakai `BuiltinJavaService`; LSP eksternal adalah opsi lanjutan:
  - **Lokal (opsional):** jdtls di Termux/JDK on-device untuk power user.
  - **Remote (riset):** language server di Codespaces/server milik user via WebSocket.
- Capability negotiation: fitur yang tidak didukung server dimatikan otomatis di UI; jika server mati/lambat, fallback ke `BuiltinJavaService`.

### 10.9 Performa & Mode Ringan
- Parse inkremental (hanya bagian berubah), highlight di thread terpisah, render hanya viewport.
- Batas ukuran file untuk fitur semantik (mis. > 5.000 baris → highlight sederhana saja).
- Mode ringan otomatis (RAM rendah): index API tidak dimuat penuh, diagnostics semantik dimatikan, completion hanya Tier 0.
- Ukuran unduhan index ditampilkan sebelum mengunduh; bisa dihapus dari Settings.

## 11. UX Teknis

- Semua operasi file/jaringan di `Dispatchers.IO`; sync dan pemantauan build lewat WorkManager agar aman saat app ke background.
- Auto-save debounce 1–2 detik; snapshot lokal ringan sebelum sync/migrasi versi.
- Mode offline: semua editor tetap jalan; tombol Build menampilkan "Butuh internet".
- Notifikasi progres dan hasil build; tombol "Buka di GitHub".

## 12. Strategi Testing

| Level | Isi |
|---|---|
| Unit | VersionMatrix validator, template renderer, workflow generator, codegen IR, exporter model, hash blob git, aturan auto-indent, ranking completion, query symbol index |
| Mock API | `MockWebServer` untuk auth, sync, polling, error 403/404/422, rate limit |
| Golden-file | Output template/codegen/exporter/workflow dibandingkan snapshot |
| CI matrix (repo ModForge sendiri) | Generate tiap template × versi → build di Actions → matrix hijau/merah |
| Integrasi | Akun GitHub tes: buat repo → sync → build → unduh jar → hapus repo |
| Manual | Uji mod hasil di Minecraft: 1.16.5, 1.18.2, 1.20.1, 1.21.1, 1.21.x terbaru, 26.x |

## 13. Keamanan & Privasi

- Token GitHub di Keystore; tidak pernah ditulis ke log atau project.
- Scope seminimal mungkin; tombol Logout/Cabut akses jelas.
- Repo default private; user diberi tahu bahwa source project disimpan di repo miliknya.
- Jangan commit secret (`.gitignore`, pemindai sederhana untuk pola token/key sebelum push).
- Checksum/hash untuk template remote; tanpa telemetri default, crash report opt-in.

## 14. Hal yang Perlu Diverifikasi Lebih Dulu (Spike M0)

1. Template Fabric + workflow berhasil build di Actions untuk 1.16.5, 1.18.2, 1.20.1, 1.21.x, 26.x; catat waktu cold/warm cache.
2. Ketersediaan Java 8/16/17/21/25 di `actions/setup-java` dan kompatibilitas Gradle wrapper per versi.
3. Perilaku Loom di MC 26.x (unobfuscated) dan mappings yang dipakai.
4. OAuth Device Flow: setup OAuth App, scope minimum, perilaku token.
5. Performa sync Git Data API untuk ±100–300 file (waktu, rate limit).
6. Download artifact (redirect + auth) dan parsing log di Android.
7. Konsumsi menit Actions per build dan kuota repo private di paket Free.
8. Sora Editor: dukungan tree-sitter, auto-indent kustom, completion API, dan modul LSP di versi yang dipakai; ukuran APK.
9. Ukuran & waktu query symbol index (Minecraft + Fabric API) per versi; format penyimpanan terbaik (SQLite FTS vs trie).
10. Lisensi distribusi index (nama/signature dari mappings yang dipakai) dan kelayakan hosting di release repo.
11. Kelayakan LSP eksternal (jdtls via Termux, atau remote) sebagai opsi lanjutan.
