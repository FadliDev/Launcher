# DESIGN — ModForge Studio (UI/UX)

## 1. Prinsip Desain
- **Mobile-first:** semua alur bisa dilakukan dengan satu tangan, target sentuh ≥ 48dp.
- **Material 3 (Material You):** dynamic color, tema terang/gelap/AMOLED.
- **Progressive disclosure:** pemula lihat wizard dan visual, ahli bisa buka kode dan setting lanjutan.
- **Feedback jelas:** build, download, dan error selalu terlihat statusnya.

## 2. Navigasi
Bottom navigation (4 tab) + layar detail project:
1. **Projects** — daftar project
2. **GitHub & Build** — akun GitHub, daftar run, artifact
3. **Templates** — template per versi/loader
4. **Settings**

Layar **Project Workspace** (setelah buka project) memakai tab atas / bottom bar kontekstual:
`Overview · Code · Visual · Models · Build`

## 3. Layar Utama

### 3.1 Projects
- Kartu project: ikon, nama, badge loader (Fabric/Forge/NeoForge), versi MC, status build terakhir.
- FAB **+ Project Baru**.
- Aksi swipe/long-press: duplikat, export, hapus.
- Filter dan search berdasarkan loader/versi.

### 3.2 Create Project (wizard 4 langkah)
1. **Loader & Versi** — pilih loader (segmented button), versi MC (dropdown dengan search; versi diberi label Java yang dibutuhkan).
2. **Info Mod** — nama, mod ID (auto dari nama), package (auto `com.<author>.<modid>`), versi mod, author, deskripsi, lisensi, ikon.
3. **Build & GitHub** — Java dan Gradle (otomatis direkomendasikan, bisa diubah; indikator ✅ cocok / ⚠️ tidak cocok), akun GitHub terhubung, nama repo (default = nama project, bisa diedit), private/public.
4. **Template** — Empty, Item, Block, Command, dst. + ringkasan → **Buat** (repo otomatis dibuat dan commit pertama dikirim).

### 3.3 Project Settings (per instance)
Seperti "instance settings" launcher:
- General (nama, mod ID, package, versi, author, lisensi)
- Build Config (Java, Gradle, loader plugin, mappings)
- GitHub (repo, branch, visibilitas)
- Custom Build (mode Managed/Custom, build command, argumen Gradle, env, edit workflow YAML)
- Dependencies (tambah library/mod dependency)
- Danger zone (putuskan repo, hapus artifact lokal, hapus project)

### 3.4 Code Editor
- File tree (drawer kiri), tab file di atas, editor di tengah.
- Toolbar simbol di atas keyboard: `{ } ( ) ; < > " Tab`.
- Panel bawah geser-naik: **Problems** dan **Build Log**.
- Aksi cepat: Build ▶, Undo/Redo, Find, Format.

### 3.5 Visual Scripting
- Canvas node tak terbatas (pinch zoom, drag pan).
- Palet node di bottom sheet dengan kategori berwarna:
  - 🟥 Event · 🟦 Kondisi · 🟩 Aksi · 🟨 Variabel · 🟪 Math/Logic
- Port berbentuk titik: alur eksekusi (putih) dan data (berwarna sesuai tipe).
- Long-press node: duplikat, hapus, komentar.
- Tombol **Lihat Kode** → panel hasil Java (read-only) dan tombol Build.
- Mini-map dan tombol "Rapikan" (auto layout).

### 3.6 3D Item/Block Editor
```
┌───────────────────────────────┐
│ Toolbar: Move Rot Scale  ↶ ↷  │
├───────────────────────────────┤
│                               │
│        Viewport 3D            │
│     (orbit, pinch zoom)       │
│                               │
├───────────────────────────────┤
│ Tab: Elements | UV | Texture  │
│        | Display | Preview    │
│ [panel properti bawah]        │
└───────────────────────────────┘
```
- Tap untuk pilih cube/face; gizmo untuk geser/rotasi/scale.
- Panel properti: posisi from/to, rotasi, origin, face texture, tint.
- **UV tab:** kanvas UV per face + auto UV.
- **Texture tab:** kuas, penghapus, fill, picker warna, mirror, grid pixel.
- **Display tab:** preview gui/tangan/ground dengan slider transform.
- **Preview:** inventory slot, di tangan, di tanah.
- Landscape: panel properti pindah ke sisi kanan.

### 3.7 Build (GitHub Actions)
- Status bar di atas editor: `Tersimpan (lokal) ✓` saat Save; saat Build ditekan: `Mengirim ke GitHub… → Building… → Selesai ✓`.
- Tombol **Build** manual, **Cancel**, dan **Buka di GitHub**.
- Progress run dengan job/step; log dengan warna (error merah, warning kuning), bisa disalin.
- Setelah sukses: jar diunduh otomatis, kartu hasil menampilkan nama jar, ukuran, commit, tombol **Share / Buka Folder**.
- Jika gagal: ringkasan error + tombol **Lihat baris error** dan **Edit Gradle**.
- Banner kecil saat offline: "Menunggu internet, perubahan akan dikirim otomatis".

### 3.8 GitHub & Build (tab 2)
- **Akun:** tombol **Connect GitHub** (tampil kode perangkat), status terhubung, tombol Disconnect.
- **Daftar run** semua project: status, commit, durasi, aksi unduh ulang artifact.
- **Custom Gradle:** pintasan ke `build.gradle`, `gradle.properties`, dan workflow YAML dengan penanda Managed/Custom per file.
- **Storage:** pemakaian artifact lokal dan tombol bersihkan.

## 4. Design System
| Elemen | Spesifikasi |
|---|---|
| Warna | Dynamic color M3; aksen default hijau "creeper" `#4CAF50`, sekunder oranye `#FF9800` |
| Tipografi | Roboto/Google Sans untuk UI, JetBrains Mono untuk kode |
| Shape | Rounded 12–28dp |
| Ikon | Material Symbols Rounded |
| Motion | Transisi shared-axis, animasi singkat (≤300ms) |
| Badge loader | Fabric (krem), Forge (abu-biru), NeoForge (oranye) |

## 5. Komponen Reusable
- `ProjectCard`, `LoaderBadge`, `VersionPicker`, `ToolchainStatusChip`
- `BuildProgressCard`, `LogView`, `SymbolToolbar`, `GitHubConnectCard`, `SyncStatusBar`
- `NodeView`, `PortView`, `NodePaletteSheet`
- `Viewport3D`, `GizmoOverlay`, `PropertyPanel`, `UVCanvas`, `PixelCanvas`

## 6. Aksesibilitas
- Kontras WCAG AA, dukungan ukuran font besar.
- Deskripsi konten untuk semua ikon.
- Mode high-contrast untuk node graph.
- Opsi haptic feedback untuk gizmo/snap.

## 7. Layout Responsif
- **Phone portrait:** satu panel, bottom sheet untuk properti.
- **Phone landscape / tablet:** dua panel (tree + editor, viewport + properti).
- **Foldable/tablet besar:** tiga panel (tree, editor, log).

## 8. Empty State & Error State
- Belum ada project: ilustrasi + "Buat mod pertamamu" + tombol cepat template.
- GitHub belum terhubung: ajakan **Connect GitHub** dengan penjelasan singkat (repo dibuat otomatis, build di cloud).
- Build gagal: ringkasan penyebab + tombol "Lihat baris error" dan "Saran perbaikan".
- Rate limit/kuota Actions habis: tampilkan pesan dan saran (repo public, tunda build).

## 9. Alur Pengguna Utama
1. Buka app → **+ Project** → pilih Fabric 1.21.x → isi info → template Item.
2. Connect GitHub (sekali) → repo otomatis dibuat sesuai nama project.
3. Buka **Models** → buat model pedang 3D → simpan.
4. Buka **Visual** → "saat klik kanan item → spawn petir".
5. **Save** (lokal) → tekan **Build** → otomatis push → build di GitHub → jar otomatis masuk ke HP → salin ke folder mods.
