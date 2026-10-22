# PRD — ModForge (working name)

> Aplikasi Android untuk membuat mod **Fabric Minecraft Java Edition** (1.16.5 → 26.3), lengkap dengan manajemen Gradle/Java per project, visual scripting, dan 3D item/block editor.

Versi dokumen: 0.3 · Tanggal: 2026-10-23 (build via GitHub Actions + code intelligence)

---

## 1. Ringkasan

ModForge adalah IDE mod Fabric yang berjalan di Android. **Build tidak dijalankan di HP, tapi di GitHub Actions.** User login GitHub sekali; setiap project baru otomatis dibuatkan repo; setiap kali tekan **Build**, app menyimpan (commit + push) project ke repo, GitHub Actions meng-compile mod dengan Java & Gradle sesuai setting project, lalu `.jar` hasilnya diunduh balik ke HP.

User tetap bisa memilih versi MC, Gradle, dan Java per project (konsep "instance"), bikin logic lewat visual scripting, dan bikin model lewat editor 3D. Karena build di cloud, HP nggak perlu install JDK/Gradle dan tetap ringan.

## 2. Masalah yang diselesaikan

| Masalah | Dampak |
|---|---|
| Setup modding (JDK, Gradle, Loom, IDE) ribet di PC, apalagi di HP | Pemula menyerah sebelum mulai |
| Tiap versi MC butuh Java, Gradle, Loom, dan API yang beda | Sering error versi mismatch |
| Bikin model item/block harus pakai tool terpisah (Blockbench) | Alur kerja terpecah |
| Nulis Java untuk logic dasar (item, block, event) repetitif | Lambat dan rawan salah |

## 3. Tujuan (Goals)

1. Dari install app → login GitHub → mod pertama ke-build dalam **≤ 10 menit** (build pertama lebih lama karena cache dingin).
2. Satu project = satu repo GitHub + satu konfigurasi terisolasi (versi MC, Gradle, Java, loader, dependency).
3. Build **tidak membebani HP** (tanpa JDK/Gradle on-device).
4. Mod sederhana (item, block, recipe, event dasar) bisa dibuat **tanpa menulis kode**.
5. Model item/block dibuat visual dan langsung ter-export ke format yang benar untuk versi targetnya.
6. UI minimalis, sedikit pilihan di layar, tiap layar punya satu aksi utama.

## 4. Non-Goals (v1)

- Tidak ada build lokal di HP (opsional di backlog; v1 hanya GitHub Actions).
- Tidak ada build tanpa internet (editing tetap bisa offline).
- Bukan pengganti Blockbench penuh (tidak ada animasi, rigging, entity model kompleks di v1).
- Tidak mendukung Forge/NeoForge/Quilt di v1 (arsitektur disiapkan, implementasi menyusul).
- Tidak ada marketplace di v1.
- Tidak ada run Minecraft di dalam app (testing lewat launcher eksternal atau export jar).
- Tidak mendukung Bedrock.

## 5. Target User

| Persona | Kebutuhan |
|---|---|
| **Pemula mobile-only** | Ga punya PC, mau bikin mod sederhana tanpa coding |
| **Modder hobi** | Mau prototyping cepat di HP, sesekali edit kode |
| **Dev Android/Termux** | Butuh kontrol Gradle/Java per project, akses ke repo sendiri, edit kode manual |

## 6. Version Support Matrix

Matriks ini **bukan hardcode**; diambil dari data remote (Fabric Meta API) + file fallback bawaan app. Nilai di bawah indikatif dan harus diverifikasi saat M1.

| Rentang MC | Java (minimum) | Gradle (indikatif) | Catatan |
|---|---|---|---|
| 1.16.5 | 8 (s/d 16) | 6.x–7.x | Loom lama, mappings Yarn/Mojang |
| 1.17.x | 16 | 7.x | |
| 1.18.x – 1.20.4 | 17 | 7.x–8.x | |
| 1.20.5 – 1.21.x | 21 | 8.x | Data components, item model definition baru (1.21.4+) |
| 26.x | 25 (verifikasi) | 8.x/9.x (verifikasi) | Versioning baru, tanpa obfuscation (verifikasi dampak ke mappings/Loom) |

Aturan: app **menolak kombinasi invalid** (mis. MC 1.20.6 + Java 17) dan menyarankan kombinasi yang benar secara otomatis.

## 7. Functional Requirements

Prioritas: **P0** wajib MVP · **P1** v1.0 · **P2** setelahnya.

### 7.1 Project & Instance Management
| ID | Requirement | Prio |
|---|---|---|
| FR-1.1 | Buat project baru lewat wizard (nama, package, versi MC, loader, template) | P0 |
| FR-1.2 | Tiap project punya konfigurasi sendiri: versi MC, Gradle, Java, Loom, Fabric Loader, Fabric API | P0 |
| FR-1.3 | Ganti versi Gradle per project (dropdown + custom) | P0 |
| FR-1.4 | Ganti versi Java per project (8/16/17/21/25) | P0 |
| FR-1.5 | Edit metadata: nama mod, mod id, package, versi mod, author, deskripsi, lisensi, icon, environment (client/server/both) | P0 |
| FR-1.6 | Validasi otomatis (mod id lowercase, package valid, kompatibilitas versi) | P0 |
| FR-1.7 | Duplikat, rename, hapus, export/import project (`.zip`) | P1 |
| FR-1.8 | Migrasi versi MC (mis. 1.20.1 → 1.21.1) dengan laporan perubahan | P2 |

### 7.2 GitHub Integration
| ID | Requirement | Prio |
|---|---|---|
| FR-2.1 | Login GitHub (OAuth Device Flow), token disimpan aman di Android Keystore | P0 |
| FR-2.2 | Setiap project baru **otomatis membuat repo** di akun user (nama default = mod id; pilihan private/public) | P0 |
| FR-2.3 | Generate `.github/workflows/build.yml` otomatis sesuai config project (Java, Gradle, branch); regenerate saat versi diganti | P0 |
| FR-2.4 | Gradle wrapper per project (`gradle-wrapper.properties` + jar) sesuai versi Gradle pilihan | P0 |
| FR-2.5 | Setiap Build, semua perubahan lokal di-commit & push ke repo (satu commit, pesan otomatis) | P0 |
| FR-2.6 | Hanya file yang berubah yang diunggah (diff berbasis hash) | P0 |
| FR-2.7 | Penanganan rate limit & jaringan putus (retry, resume) | P0 |
| FR-2.8 | Deteksi perubahan di repo dari luar app → dialog: Pull / Timpa / Buat branch | P1 |
| FR-2.9 | Hubungkan project ke repo yang sudah ada / import project dari repo | P1 |
| FR-2.10 | Kelola akun: logout, ganti akun, cabut akses, daftar repo yang dibuat ModForge | P1 |
| FR-2.11 | Tampilkan peringatan kuota GitHub Actions (terutama repo private) | P1 |
| FR-2.12 | Pilih branch, buat release GitHub otomatis dari build sukses | P2 |

### 7.3 Template System
| ID | Requirement | Prio |
|---|---|---|
| FR-3.1 | Template dasar per versi MC (Empty, Item, Block, Item+Block, Event) | P0 |
| FR-3.2 | Template terstruktur per **loader × versi** (Fabric dulu; Forge/NeoForge/Quilt slot disiapkan) | P0 |
| FR-3.3 | Template memakai placeholder (`{{mod_id}}`, `{{package}}`, dst.) + file kondisional per versi | P0 |
| FR-3.4 | Template bisa di-update tanpa update app (remote template repo) | P1 |
| FR-3.5 | User bisa bikin/import template sendiri | P2 |

### 7.4 Code Editor & Code Intelligence
| ID | Requirement | Prio |
|---|---|---|
| FR-4.1 | File tree + editor untuk Java, JSON, Gradle (Groovy/Kotlin DSL), YAML, `.properties` | P0 |
| FR-4.2 | **Syntax highlighting** untuk semua tipe file di atas (tema light/dark) | P0 |
| FR-4.3 | **Auto-indent**: indent otomatis setelah Enter dan `{`, dedent di `}`, reindent saat paste, Shift-Tab dedent | P0 |
| FR-4.4 | Auto-close bracket/kutip, smart backspace, bracket matching, indent guides, code folding | P0 |
| FR-4.5 | Extra key row, undo/redo, find/replace (regex), go to line | P0 |
| FR-4.6 | **Autocomplete dasar**: keyword, snippet (mis. registrasi item/block, mixin, event), simbol lokal (class/method/field di project) | P0 |
| FR-4.7 | **Autocomplete API Minecraft/Fabric** sesuai versi project (class, method, field, signature) lewat symbol index | P1 |
| FR-4.8 | Auto-import saat memilih class dari autocomplete; rapikan/hapus import tak terpakai | P1 |
| FR-4.9 | **LSP**: diagnostics, hover, go-to-definition, signature help, completion, rename, format, quick fix. Dilayani oleh language service bawaan (ringan, di dalam app) dan/atau LSP server eksternal opsional | P1 (diagnostics, hover, completion, definition) / P2 (sisanya) |
| FR-4.10 | Diagnostics dari build (GitHub Actions) tampil inline di editor dan gutter | P0 |
| FR-4.11 | Format kode (indent normalize / formatter Java ringan) | P1 |
| FR-4.12 | Autocomplete & validasi untuk JSON model, blockstate, recipe, loot table, `lang`, `fabric.mod.json` (schema bawaan per versi) | P1 |
| FR-4.13 | File hasil generate (visual script) read-only; menu "Eject" untuk jadi editable | P1 |
| FR-4.14 | Mode ringan: fitur berat (index, diagnostics semantik) dimatikan otomatis di RAM rendah | P1 |
| FR-4.15 | Pengaturan editor: ukuran font, tab size, auto-complete on/off, format on save | P1 |

### 7.5 Build System (Remote)
| ID | Requirement | Prio |
|---|---|---|
| FR-5.1 | Tombol Build: validasi → sync ke repo → jalankan workflow → pantau status | P0 |
| FR-5.2 | Status real-time per langkah (queued, setup Java, compile, upload) | P0 |
| FR-5.3 | Log build dan error dari Actions ditampilkan; error compile klik-ke-file/baris | P0 |
| FR-5.4 | `.jar` hasil build diunduh otomatis ke app; bisa Bagikan / Salin ke folder mods | P0 |
| FR-5.5 | Cancel build (cancel workflow run) | P0 |
| FR-5.6 | Notifikasi saat build selesai walau app di background | P0 |
| FR-5.7 | Histori build per project (nomor run, commit, status, jar) | P1 |
| FR-5.8 | Penerjemah error umum ("Java version mismatch" → saran perbaikan sekali tekan) | P1 |
| FR-5.9 | Tombol "Buka di GitHub" untuk run/repo | P1 |

### 7.6 Visual Scripting
| ID | Requirement | Prio |
|---|---|---|
| FR-6.1 | Editor node/blok dengan event, kondisi, aksi, variabel | P1 |
| FR-6.2 | Generate kode Java yang valid untuk versi target (adapter per versi API) | P1 |
| FR-6.3 | Katalog node: event (use item, break block, right-click, tick, entity hit, join world), aksi (give item, spawn, damage, effect, message, play sound, set block), logika (if/else, loop, compare, random) | P1 |
| FR-6.4 | Validasi graph real-time (port tipe, node tak terhubung) | P1 |
| FR-6.5 | Preview kode hasil generate | P1 |
| FR-6.6 | Custom node dari snippet Java | P2 |

### 7.7 3D Item/Block Editor
| ID | Requirement | Prio |
|---|---|---|
| FR-7.1 | Viewport 3D (orbit, pan, zoom) dengan grid 16×16×16 | P1 |
| FR-7.2 | Tambah/edit cuboid (posisi, ukuran, rotasi, origin) | P1 |
| FR-7.3 | Texture: import PNG, painter pixel sederhana, UV per face | P1 |
| FR-7.4 | Display transforms (gui, ground, hand, fixed, head) dengan preview | P1 |
| FR-7.5 | Export ke format sesuai versi: model JSON, blockstate, item model definition (1.21.4+), `lang`, loot table dasar | P1 |
| FR-7.6 | Template preset (block biasa, slab, stairs-like, item handheld, item generated) | P1 |
| FR-7.7 | Import model JSON yang sudah ada | P2 |
| FR-7.8 | Hitbox/voxel shape generator untuk block | P2 |

### 7.8 Asset & Resource
| ID | Requirement | Prio |
|---|---|---|
| FR-8.1 | Asset manager (texture, sound, lang) per project | P1 |
| FR-8.2 | Resource pack format otomatis sesuai versi MC | P1 |
| FR-8.3 | Registry helper: daftar item/block/recipe dalam project dengan form sederhana (nama, stack size, hardness, tool requirement, dsb.) | P1 |

## 8. Non-Functional Requirements

| Area | Target |
|---|---|
| Performa | UI 60 fps; sync & polling di background; editor 3D ≥ 30 fps pada device mid-range; popup autocomplete muncul ≤ 100 ms pada file ≤ 2.000 baris |
| Build | Waktu build target ditetapkan di M0 (cold vs warm cache); cache Gradle/Loom diaktifkan |
| Jaringan | Hemat data: upload hanya file berubah; polling adaptif |
| Android | minSdk 26 (Android 8), target SDK terbaru, arm64-v8a (x86_64 opsional) |
| Offline | Editing, visual script, dan editor 3D jalan offline; build butuh internet |
| Keamanan | Token di Keystore, scope minimal, tidak ada secret di-commit, repo default private |
| Privasi | Tidak ada telemetri tanpa opt-in; user diberi tahu kode project disimpan di repo GitHub miliknya |
| Reliabilitas | Auto-save, crash recovery, retry sync, log build tersimpan |
| Aksesibilitas | Target sentuh ≥ 48dp, kontras WCAG AA, dark/light |
| i18n | Indonesia + Inggris sejak awal |

## 9. Success Metrics

- ≥ 80% project template "Empty" sukses build pertama kali tanpa edit manual.
- Waktu dari tekan Build sampai jar terunduh ≤ target (didefinisikan di M0; cold vs warm cache).
- ≥ 70% user baru berhasil export satu item dengan model 3D pada sesi pertama.
- Crash-free session ≥ 99%.

## 10. Risiko & Mitigasi

| Risiko | Mitigasi |
|---|---|
| Kuota GitHub Actions (repo private punya batas menit bulanan; repo public gratis) | Cache Gradle, cancel run lama otomatis, peringatan kuota, opsi repo public |
| Bergantung pada GitHub (outage, perubahan API, rate limit) | Retry + backoff, pesan error jelas, abstraksi `BuildBackend` agar backend lain bisa ditambah |
| Build lebih lambat dari lokal (antre + cold cache) | Cache `setup-gradle`, status progres jelas, notifikasi saat selesai |
| Kode user tersimpan di GitHub | Default private, penjelasan di onboarding, tidak commit data sensitif |
| Token GitHub bocor | Keystore, scope minimal, tombol cabut akses |
| API Minecraft/Fabric berubah antar versi | Adapter layer + template per versi + CI matrix |
| Versioning baru Minecraft (26.x) mengubah mappings/Loom | Verifikasi dini di M0, template 26.x dipisah |
| Editor 3D berat di device low-end | Renderer ringan (cuboid only), batas elemen |
| LSP penuh (mis. jdtls) berat dan butuh JDK + dependensi, bertabrakan dengan keputusan "tanpa JDK di HP" | Language service bawaan ringan (tree-sitter + symbol index) sebagai default; LSP eksternal hanya opsional |
| Symbol index API Minecraft: ukuran data & lisensi mappings | Index hanya berisi nama/signature, dibangun per versi di CI, diverifikasi lisensi; fallback dibangun di Actions milik user |
| Scope terlalu besar | Roadmap bertahap, MVP hanya akun + project + build + editor |

## 11. Open Questions

1. Visibilitas repo default: private (aman, tapi memakai kuota menit) atau public (gratis, tapi kode terbuka)?
2. OAuth App (device flow) atau GitHub App untuk akses lebih granular?
3. Apakah Java 25 (untuk MC 26.x) sudah tersedia di `actions/setup-java`? (verifikasi di M0)
4. Apakah Forge/NeoForge masuk v1.0 atau v2?
5. Mappings default: Mojang official vs Yarn untuk versi lama?
6. Perlu opsi build lokal (Termux) sebagai fitur opsional pasca-v1.0?
7. Symbol index API dihosting di repo ModForge (release assets) atau dibangun di Actions milik tiap user? (lisensi mappings, kuota)
8. Perlu dukungan LSP eksternal (jdtls via Termux/remote) di v1.x atau cukup language service bawaan?

