# ROADMAP — ModForge Studio

Estimasi untuk tim kecil (2–4 orang). Durasi bisa dikurangi/ditambah sesuai kapasitas.

## Fase 0 — Riset & Spike (2–3 minggu)
- [ ] Spike: OAuth Device Flow GitHub + buat repo dari app
- [ ] Spike: commit/push via Git Data API (multi-file) + trigger workflow
- [ ] Spike: polling run + unduh dan ekstrak artifact `.jar`
- [ ] Workflow template yang build sukses untuk Fabric 1.21.x dan Forge 1.20.1
- [ ] Kumpulkan matriks Java/Gradle/loader untuk 1.16.5 → 26.3
- [ ] Prototipe render kubus 3D (OpenGL ES/Filament)

## Fase 1 — MVP: Project + GitHub Build (6–8 minggu)
- [ ] Setup proyek, modul, tema Material 3
- [ ] Project Manager (CRUD, setting nama/modId/package/dll)
- [ ] Connect GitHub (OAuth), buat repo otomatis sesuai nama project
- [ ] Push (commit via Git Data API) otomatis saat tombol Build ditekan; Save hanya lokal
- [ ] Build Config (Java & Gradle per project) → generate workflow + wrapper
- [ ] Trigger build, status run, log, unduh artifact otomatis
- [ ] Template: Empty Mod untuk Fabric & Forge (1.16.5, 1.18.2, 1.20.1, 1.21.x)
- [ ] Code editor dasar + file tree (termasuk edit Gradle/workflow manual)
- **Milestone M1:** buat project → tekan Build → otomatis push ke repo → build di GitHub → jar masuk ke HP.

## Fase 2 — Cakupan Versi & Loader (4–6 minggu)
- [ ] Tambah NeoForge
- [ ] Semua versi 1.16.5 → 26.3 lewat version profile
- [ ] Template Item, Block, Command, Creative Tab per versi
- [ ] CI matrix: auto-test build semua template
- [ ] Mode Managed vs Custom (proteksi file custom), editor workflow YAML
- [ ] Penanganan konflik (pull/merge), pilihan branch, opsi build hanya manual
- [ ] Pengelolaan artifact lokal + estimasi kuota Actions
- **Milestone M2:** semua kombinasi versi × loader yang didukung lolos build otomatis.

## Fase 3 — 3D Item/Block Editor (6–8 minggu)
- [ ] Viewport 3D + kamera orbit
- [ ] Edit cube (add/move/rotate/scale), face texture, UV
- [ ] Display transforms + preview konteks (gui/hand/ground)
- [ ] Texture painter sederhana + import PNG
- [ ] Export model + registrasi otomatis ke project
- [ ] Adapter format model per versi
- **Milestone M3:** bikin item custom lengkap dengan model 3D tanpa nulis JSON.

## Fase 4 — Visual Scripting (8–10 minggu)
- [ ] Node graph editor (pan/zoom, connect, group)
- [ ] Library node dasar: event, kondisi, aksi, variabel, math
- [ ] IR + code generator (Fabric & Forge/NeoForge)
- [ ] Validasi graph + preview kode hasil generate
- [ ] Debug helper: error di Java dipetakan balik ke node
- [ ] Contoh script siap pakai
- **Milestone M4:** mod sederhana (item yang spawn entity saat klik kanan) dibuat tanpa coding.

## Fase 5 — Polish & Beta (4–6 minggu)
- [ ] Wizard Content (item, block, entity, recipe, loot table, tag, lang)
- [ ] Onboarding + tutorial interaktif
- [ ] Optimasi performa & memori
- [ ] Lokalisasi ID/EN
- [ ] Crash reporting (opt-in), analitik anonim
- [ ] Beta tertutup → beta terbuka
- **Milestone M5:** rilis publik v1.0.

## Fase 6 — Pasca v1.0 (berkelanjutan)
- Round-trip visual script ↔ kode
- Entity/animation editor (model hewan/mob)
- Git integration (commit/push dari app)
- Marketplace template & node komunitas
- Dukungan Quilt / Architectury (multi-loader project)
- Dukungan GitLab/Gitea dan self-hosted runner
- Tablet/foldable layout, mode landscape penuh
- Plugin system untuk node/template pihak ketiga
- Update cepat untuk versi Minecraft baru lewat version profile + template pack

## Ringkasan Timeline
| Fase | Durasi | Kumulatif |
|---|---|---|
| 0 Riset | 2–3 mgg | ~0,7 bln |
| 1 MVP | 6–8 mgg | ~3 bln |
| 2 Versi & Loader | 4–6 mgg | ~4,5 bln |
| 3 3D Editor | 6–8 mgg | ~6,5 bln |
| 4 Visual Script | 8–10 mgg | ~9 bln |
| 5 Polish & Beta | 4–6 mgg | ~10,5 bln |

## Prioritas Jika Waktu Terbatas
1. Project + GitHub connect + push-saat-Build + build + artifact (inti)
2. Template berkualitas
3. 3D editor
4. Visual scripting (paling kompleks, bisa dirilis bertahap: node sedikit dulu)
