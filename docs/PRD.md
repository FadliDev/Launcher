# PRD — ModForge Studio (nama sementara)

Aplikasi Android untuk membuat mod Minecraft Java (Forge, NeoForge, Fabric) dari versi 1.16.5 sampai 26.3, lengkap dengan manajemen project per-instance, visual scripting, dan 3D item/block editor.

## 1. Latar Belakang & Masalah
- Bikin mod biasanya butuh PC, IDE (IntelliJ/Eclipse), setup JDK + Gradle manual, dan paham Java.
- Setiap versi Minecraft punya Java, Gradle, mappings, dan API loader yang beda-beda, jadi sering error saat setup.
- Pemain Android yang mau bikin mod hampir tidak punya tools yang proper.

## 2. Tujuan
1. Bikin mod dari HP: dari nol sampai file `.jar` siap pakai, dengan **build di GitHub Actions** (HP tidak perlu menjalankan Gradle).
2. Setup otomatis dan terisolasi per project (Java + Gradle + loader + versi MC) yang ditulis ke konfigurasi build di repo project.
3. Bisa bikin mod tanpa coding lewat visual scripting.
4. Bisa bikin model item/block 3D secara visual.
5. Template siap pakai untuk setiap kombinasi versi × loader.

### Non-Goals (v1)
- Bukan IDE penuh (tidak ada debugger/refactor level IntelliJ).
- Tidak menjalankan Minecraft di dalam app (hanya build dan terima jar).
- Tidak ada build lokal di device (semua build lewat GitHub Actions).
- Tidak mendukung Bedrock.

## 3. Target Pengguna
| Persona | Kebutuhan |
|---|---|
| Pemula (pemain, tanpa coding) | Visual scripting, template, 3D editor |
| Modder menengah | Edit kode Java, ganti Java/Gradle, custom Gradle & workflow |
| Modder pro | Multi-version project, custom workflow, git |

## 4. Scope Platform & Versi
- **Loader:** Fabric, Forge, NeoForge (NeoForge mulai ±1.20.1+).
- **Versi MC:** 1.16.5 → 26.3 (daftar versi diambil dinamis dari Mojang version manifest + metadata loader, bukan hardcode).
- Catatan: mulai seri 26.x Minecraft pakai penamaan tahun dan tanpa obfuscation; tooling dan Java requirement-nya harus diverifikasi saat implementasi.

## 5. Fitur Inti

### F1. Project Manager (per-instance)
- Buat, duplikat, import, export, hapus project.
- Per project bisa set: nama mod, mod ID, package, versi mod, author, deskripsi, lisensi, ikon, loader, versi MC.
- Tiap project punya **profil build sendiri**: versi Java, versi Gradle, versi loader/Loom/ForgeGradle, mappings (Mojang/Yarn/Parchment), JVM args, Gradle properties.
- Tiap project terhubung ke **satu repo GitHub** (nama repo = nama project).

### F2. Build Config Manager (Java & Gradle per project)
- Tidak ada JDK/Gradle yang di-install di HP. Pilihan Java dan Gradle per project dituliskan ke workflow GitHub Actions dan `gradle-wrapper.properties` di repo.
- Pilih Java (8, 17, 21, 25, dst) lewat dropdown; dipakai oleh `actions/setup-java`.
- Pilih Gradle (6.x–9.x) lewat dropdown; ditulis ke Gradle wrapper project.
- Auto-rekomendasi pasangan Java + Gradle + loader sesuai versi MC (tabel kompatibilitas dari `version-profiles.json`, bisa di-update dari server).
- Validasi kompatibilitas sebelum push (peringatan jika Java/Gradle tidak cocok).

### F3. Code Editor
- Syntax highlight Java/JSON/Gradle/TOML, autocomplete dasar, file tree, search/replace, undo/redo.
- Error dari build ditampilkan klik-ke-baris.

### F4. Build via GitHub Actions
- **Connect GitHub** sekali (OAuth), token disimpan aman di device.
- Saat project dibuat, app otomatis membuat repo GitHub dengan **nama sesuai nama project** (private/public bisa dipilih).
- **Save hanya menyimpan lokal.** Repo baru di-update (commit + push) **saat tombol Build ditekan**: app mengumpulkan semua perubahan sejak build terakhir, commit, push, lalu memicu workflow.
- Push memicu workflow build; app memantau status run (queued, running, success, failed) dan menampilkan log.
- **Artifact otomatis dikirim ke app:** setelah build sukses, `.jar` dari artifact diunduh otomatis ke folder project di HP.
- Tombol Build manual (`workflow_dispatch`) dan Cancel run.
- **Custom Gradle tetap bebas:** `build.gradle`, `gradle.properties`, `settings.gradle`, task/argumen Gradle, dan file workflow `.github/workflows/*.yml` bisa diedit manual. Lihat F9.

### F5. Visual Scripting
- Editor berbasis node/blok (event → kondisi → aksi).
- Contoh event: block dipecah, item di-klik kanan, entity mati, player join, tick.
- Contoh aksi: spawn entity, beri efek, drop item, kirim pesan, teleport, ubah block.
- Di-generate ke kode Java sesuai versi + loader (lewat code generator per target).
- Bisa lihat dan edit hasil kode (one-way generate untuk v1, round-trip di versi lanjut).

### F6. 3D Item/Block Editor (Java Block/Item Model)
- Edit model format Java Block/Item (JSON elements, cube-based) dengan viewport 3D.
- Tambah/geser/rotasi/scale cube, face texture, UV editor, display transforms (gui, hand, ground, dll).
- Texture painter sederhana + import PNG.
- Preview di berbagai konteks (inventory, tangan, ground).
- Export ke `assets/<modid>/models/...` + blockstate/item definition sesuai format versi (format item model berubah di versi baru).

### F7. Template System
- Template per versi × loader: Empty Mod, Item, Block, Entity, Command, Creative Tab, Config.
- Template dibuat dari "template pack" (zip + manifest) sehingga bisa di-update tanpa update app.

### F8. Registry/Content Wizard
- Wizard bikin item, block, entity, recipe, loot table, tag, lang file dengan form UI.

### F9. GitHub Integration & Custom Build
- **Mode Managed:** app membuat dan memperbarui file build otomatis dari setting project (file diberi penanda `# managed by ModForge`).
- **Mode Custom:** file yang diedit manual ditandai custom dan tidak akan ditimpa app; user bebas ubah Gradle script, task, dependency, plugin, matrix build, dan workflow.
- Custom build command (mis. `./gradlew build -x test --no-daemon`), env variable, dan secrets repo bisa diatur dari app.
- Branch default `main`, opsi branch lain; deteksi konflik jika repo diubah dari luar (pull/merge dengan konfirmasi).
- `.gitignore` otomatis; app memperingatkan sebelum push file sensitif.

## 6. Kebutuhan Non-Fungsional
- **Performa:** UI 60 fps; push dan unduh artifact berjalan di background (WorkManager).
- **Konektivitas:** build butuh internet; edit dan save tetap offline; Build butuh internet.
- **Storage:** artifact dan cache project punya indikator ukuran dan tombol bersihkan.
- **Keamanan:** token GitHub di Android Keystore, scope minimum, tidak pernah ditulis ke repo/log; verifikasi workflow template sebelum push.
- **Minimum device:** Android 8+ (karena tidak ada build lokal, RAM 3 GB cukup).
- **Lokalisasi:** Indonesia + English.

## 7. Risiko & Mitigasi
| Risiko | Mitigasi |
|---|---|
| Kuota GitHub Actions (menit gratis terbatas untuk repo private; cek kebijakan terbaru) | Tampilkan estimasi pemakaian, opsi repo public, cache Gradle di workflow |
| Rate limit GitHub API | Batch commit lewat Git Data API, push hanya saat Build, backoff |
| Build ditekan padahal tidak ada perubahan | Deteksi diff; jika kosong, tawarkan Re-run build terakhir |
| Token GitHub bocor | Android Keystore, scope minimum, tombol disconnect/revoke |
| Konflik jika repo diedit dari PC | Deteksi perubahan remote, dialog pull/merge |
| Perubahan API loader tiap versi | Template pack + code generator ter-versi, CI matrix test |
| Penamaan/format 26.x berbeda | Abstraksi "version profile" di data, bukan di kode |
| Ukuran artifact dan retensi artifact terbatas | Unduh otomatis segera setelah sukses, simpan lokal |

## 8. Metrik Sukses
- ≥ 80% build template "Hello Item" sukses di semua kombinasi versi × loader yang didukung.
- Waktu dari buat project → jar pertama diterima di HP < 10 menit (tergantung antrean GitHub Actions).
- Crash-free sessions ≥ 99%.

## 9. Pertanyaan Terbuka
1. Auth GitHub: OAuth Device Flow atau GitHub App (disarankan Device Flow supaya tidak perlu server sendiri)?
2. Default repo private atau public?
3. Apakah NeoForge termasuk di v1 (disarankan ya)?
4. Model monetisasi (gratis/open source/premium)?
5. Dukungan GitLab/Gitea atau remote lain di masa depan?
