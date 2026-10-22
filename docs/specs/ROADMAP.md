# ROADMAP — ModForge

Versi dokumen: 0.3 · Tanggal: 2026-10-23 (build via GitHub Actions + code intelligence)

Estimasi durasi dihitung untuk 1 developer (part-time/hobi bisa dikali 1.5–2). Tiap milestone harus menghasilkan sesuatu yang **bisa dicoba di HP**.

```
M0 Spike ─► M1 Akun GitHub ─► M2 Project+Remote Build (MVP) ─► M3 Editor+Template ─► M4 3D Editor ─► M5 Visual Script ─► M6 Polish/Beta ─► v1.0 ─► v2.x
```

---

## M0 — Spike & Validasi (1 minggu)

Tujuan: buktikan alur "HP → GitHub → Actions → jar" jalan.

- [ ] Buat template Fabric + workflow manual, jalankan di Actions untuk 1.16.5, 1.18.2, 1.20.1, 1.21.x, 26.x
- [ ] Cek Java 8/16/17/21/25 di `setup-java` + Gradle wrapper per versi
- [ ] Verifikasi Loom/mappings untuk MC 26.x
- [ ] Prototipe OAuth Device Flow di Android
- [ ] Prototipe sync 100+ file lewat Git Data API (waktu, rate limit)
- [ ] Prototipe unduh artifact + parse log error
- [ ] Ukur waktu build & menit Actions (cold vs warm cache)
- [ ] Putuskan: visibilitas repo default, nama final app, package id

**Exit criteria:** laporan hasil spike + keputusan scope OAuth dan visibilitas repo.

## M1 — Fondasi & Akun GitHub (2–3 minggu)

- [ ] Setup proyek multi-modul, tema minimalis, navigasi dasar
- [ ] `VersionMatrix` (remote + fallback) + validator
- [ ] Login GitHub (Device Flow) + penyimpanan token di Keystore
- [ ] GitHub API client (retry, backoff, rate limit, ETag)
- [ ] Layar Akun (status login, logout, cabut akses)
- [ ] Fungsi buat repo + push commit awal

**Exit criteria:** login GitHub dari app, buat repo, dan push satu file.

## M2 — Project Manager + Remote Build (MVP, 3–4 minggu)

- [ ] Wizard project baru (nama, mod id, package, MC, Java, Gradle, repo) yang otomatis membuat repo
- [ ] Template engine + template "Empty" + workflow generator + Gradle wrapper
- [ ] Project settings (ganti Gradle/Java/MC per project → regenerate workflow/wrapper dengan diff)
- [ ] Sync engine (diff hash, satu commit per build, deteksi konflik dasar)
- [ ] Build orchestrator: trigger, pantau run/step, cancel
- [ ] Log + annotations, error klik-ke-baris
- [ ] Unduh jar + Share; notifikasi selesai (WorkManager)
- [ ] Auto-save, crash recovery, daftar project, histori build dasar

**Exit criteria (MVP):** buat project → pilih versi → Build → repo ter-update → Actions jalan → jar terunduh dan jalan di Minecraft, untuk minimal 1.18.2, 1.20.1, 1.21.x.

## M3 — Editor Cerdas & Template Lengkap (4–6 minggu)

- [ ] File tree + code editor (Java/JSON/Gradle/YAML), key row, find/replace
- [ ] **Syntax highlighting** (tree-sitter + TextMate fallback), tema light/dark
- [ ] **Auto-indent** (Enter, `{`/`}`, paste reindent, Shift-Tab), auto-close bracket/kutip, bracket matching, folding
- [ ] **Autocomplete Tier 0**: keyword, snippet modding, simbol lokal; popup di atas keyboard
- [ ] **Symbol index** Minecraft/Fabric per versi (job CI + unduh di app) + **autocomplete API** + auto-import
- [ ] Language service bawaan: diagnostics sintaks/semantik ringan, hover, signature help, go-to-definition
- [ ] Autocomplete & validasi JSON (model, blockstate, recipe, lang, `fabric.mod.json`)
- [ ] Mode ringan editor + pengaturan editor
- [ ] Template: Item, Block, Item+Block, Event untuk semua versi
- [ ] Registry helper (form untuk bikin item/block tanpa kode)
- [ ] Asset manager dasar (texture, lang)
- [ ] Error translator + diagnostik klik-ke-baris
- [ ] Export/import project (`.zip`), duplikat project
- [ ] CI matrix template × versi

**Exit criteria:** matrix CI hijau untuk semua template di versi 1.16.5 → 26.x yang didukung.

## M4 — 3D Item/Block Editor (4–6 minggu)

- [ ] Renderer OpenGL ES (grid, cuboid, orbit/pan/zoom)
- [ ] Edit elemen: tambah, move, resize, rotate, duplikat, hapus
- [ ] Texture import + painter 16×16 + UV per face
- [ ] Display transforms + preview (gui/hand/ground)
- [ ] Preset model (block, slab, item handheld/generated)
- [ ] Exporter per versi (termasuk item model definition 1.21.4+)
- [ ] "Add to registry" otomatis
- [ ] Mode hemat untuk device low-end

**Exit criteria:** user bikin item/block 3D, export, build, dan modelnya tampil benar di game pada ≥ 4 versi sampel.

## M5 — Visual Scripting (5–7 minggu)

- [ ] Canvas node graph (pan/zoom, drag, connect, snap)
- [ ] Sistem port bertipe + validasi real-time
- [ ] IR + codegen + adapter untuk grup versi utama (mulai 1.20.1 & 1.21.x, lalu mundur ke 1.16.5 dan maju ke 26.x)
- [ ] Katalog node MVP (5 event, 7 aksi, logika dasar, variabel)
- [ ] Preview kode, eject ke Java
- [ ] Template contoh (mis. "item yang memberi efek saat di-klik kanan")
- [ ] Golden test codegen per versi

**Exit criteria:** mod item+event dibuat penuh lewat visual script, build & jalan di ≥ 4 versi.

## M6 — Polish & Beta (3–4 minggu)

- [ ] Onboarding singkat (3 layar), template contoh
- [ ] Optimasi performa (startup, build, editor 3D)
- [ ] Remote template repo + update template tanpa update app
- [ ] Klien LSP generik (JSON-RPC stdio/WebSocket) + capability negotiation, fallback ke language service bawaan
- [ ] Quick fix, rename, format kode
- [ ] Lokalisasi ID/EN, aksesibilitas, dark/light
- [ ] Dokumentasi in-app + changelog
- [ ] Beta tertutup, kumpulkan bug & metrik

**Exit criteria:** crash-free ≥ 99%, 80% build template pertama sukses di device beta.

## v1.0 Release

- [ ] Rilis publik (jalur distribusi sesuai keputusan M0)
- [ ] Halaman proyek, dokumentasi, panduan memulai

---

## Pasca-v1.0 (Backlog)

| Versi | Fitur |
|---|---|
| v1.1 | Migrasi versi MC dengan laporan perubahan, custom node dari snippet |
| v1.2 | Import model JSON/Blockbench, hitbox generator |
| v1.3 | Opsional: build lokal (Termux/JDK on-device) untuk mode offline |
| v1.4 | LSP eksternal opsional: jdtls via Termux / server remote |
| v2.0 | Loader tambahan: NeoForge, Forge, Quilt (pakai slot template yang sudah disiapkan) |
| v2.1 | Entity model sederhana, animasi item dasar |
| v2.2 | Template/komunitas sharing, dependency manager (Modrinth) |
| v2.3 | Integrasi test-run via launcher (Pojav/Zalith) |

## Ringkasan Timeline (indikatif)

| Milestone | Durasi | Kumulatif (maks) |
|---|---|---|
| M0 | 1 mgg | 1 |
| M1 | 2–3 mgg | 4 |
| M2 (MVP) | 3–4 mgg | 8 |
| M3 | 4–6 mgg | 14 |
| M4 | 4–6 mgg | 20 |
| M5 | 5–7 mgg | 27 |
| M6 | 3–4 mgg | 31 |

Total ±6–8 bulan (1 dev). **MVP bisa dicoba di ±7–8 minggu**, lebih cepat dari rencana awal karena tidak perlu membangun toolchain JDK/Gradle on-device.

## Risiko yang Mempengaruhi Jadwal

1. Kuota GitHub Actions untuk repo private (bisa memaksa default repo public atau batasan jumlah build).
2. Perubahan API GitHub / scope OAuth yang lebih ketat dari perkiraan.
3. Perubahan API/versioning Minecraft 26.x yang belum stabil.
4. Kompleksitas adapter codegen untuk rentang versi yang lebar (1.16.5 → 26.x) → batasi dulu ke grup versi utama.
5. Ukuran/lisensi symbol index API dan kompleksitas language service bawaan (bisa digeser: fitur API-aware lebih dulu, quick fix/rename menyusul).
