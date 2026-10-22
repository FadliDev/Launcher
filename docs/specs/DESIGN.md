# DESIGN — UI/UX ModForge

Versi dokumen: 0.3 · Tanggal: 2026-10-23 (login GitHub + remote build + code intelligence)

Tema: **simple, minimalis, user friendly**.

---

## 1. Prinsip Desain

1. **Satu layar, satu aksi utama.** Tombol utama selalu jelas (FAB / tombol bawah).
2. **Sembunyikan kompleksitas.** Opsi lanjutan (Gradle, Loom, mappings) ada di "Advanced", default sudah benar.
3. **Default pintar.** Pilih versi MC → Java & Gradle otomatis terpilih yang kompatibel.
4. **Feedback jelas.** Progres, error, dan sukses selalu terlihat dan bisa ditindaklanjuti (tombol "Perbaiki").
5. **Thumb-friendly.** Aksi penting di bagian bawah layar, target sentuh ≥ 48dp.
6. **Konsisten.** Satu pola list, satu pola dialog, satu pola form.

## 2. Visual Identity

### 2.1 Warna (token)

| Token | Light | Dark | Dipakai untuk |
|---|---|---|---|
| `bg` | `#FAFAFA` | `#0F1115` | Latar utama |
| `surface` | `#FFFFFF` | `#171A21` | Kartu, sheet |
| `surface-2` | `#F1F2F4` | `#1F232C` | Input, chip |
| `outline` | `#E2E4E8` | `#2B303B` | Garis tipis |
| `text` | `#16181D` | `#ECEEF2` | Teks utama |
| `text-dim` | `#6B7280` | `#8B93A1` | Teks sekunder |
| `primary` | `#3DDC84` | `#3DDC84` | Aksi utama (hijau lembut) |
| `on-primary` | `#06220F` | `#06220F` | Teks di atas primary |
| `danger` | `#E5484D` | `#FF6369` | Error |
| `warning` | `#F5A524` | `#F5A524` | Peringatan |
| `info` | `#3B82F6` | `#60A5FA` | Info |

Aturan: **satu warna aksen saja** (primary). Sisanya netral. Warna status hanya untuk status.

### 2.2 Tipografi
- Font: Inter (UI), JetBrains Mono (kode/log).
- Skala: Title 22 · Heading 18 · Body 15 · Caption 12 · Code 13.
- Maksimal 2 bobot per layar (Regular & Semibold).

### 2.3 Bentuk & Jarak
- Radius: 12dp (kartu), 10dp (input), 999dp (chip).
- Grid spasi 4dp: 4/8/12/16/24/32.
- Elevasi minim; pakai garis `outline` tipis, bukan bayangan besar.

### 2.4 Ikon
Outline 1.5–2px, satu set konsisten (mis. Lucide/Material Symbols Outlined). Tidak lebih dari satu ikon per tombol.

### 2.5 Motion
Transisi 150–200ms, ease-out. Tanpa animasi dekoratif. Hormati pengaturan "reduce motion".

## 3. Navigasi

Bottom navigation (3 tab) + layar project sebagai konteks terpisah.

```
┌─────────────────────────────┐
│  Projects │  GitHub   │ ⚙  │   ← bottom nav (home level)
└─────────────────────────────┘

Project (masuk ke satu project)
 ├─ Overview
 ├─ Code
 ├─ Visual
 ├─ Models (3D)
 ├─ Assets
 └─ Build
```

Di dalam project: tab bawah scrollable atau segmented bar atas dengan 5–6 bagian. Tombol **Build ▶** selalu ada (top bar kanan).

## 4. Layar Utama

### 4.1 Projects (Home)

```
┌─────────────────────────────┐
│ ModForge              🔍    │
├─────────────────────────────┤
│ ┌─────────────────────────┐ │
│ │ ▣ My Cool Mod           │ │
│ │ MC 1.21.1 · Java 21     │ │
│ │ Gradle 8.10 · Fabric    │ │
│ │              [Build ▶]  │ │
│ └─────────────────────────┘ │
│ ┌─────────────────────────┐ │
│ │ ▣ Ruby Tools            │ │
│ │ MC 1.20.1 · Java 17     │ │
│ └─────────────────────────┘ │
│                             │
│                      (＋)   │  ← FAB: New project
├─────────────────────────────┤
│  Projects │  GitHub   │ ⚙  │
└─────────────────────────────┘
```

- Kartu menampilkan "instance chip": versi MC, Java, Gradle.
- Long-press: duplikat, rename, export, hapus.
- Empty state: ilustrasi sederhana + tombol "Buat mod pertama".

### 4.2 New Project Wizard (3 langkah)

**Langkah 1 — Dasar**
```
Nama mod        [ My Cool Mod        ]
Mod ID          [ mycoolmod          ] (otomatis, bisa diedit)
Package         [ com.sally.mycoolmod]
Repo GitHub     [ mycoolmod          ] (otomatis)
Visibilitas     (●) Private   ( ) Public
```
Jika belum login GitHub, wizard meminta login dulu (satu layar, satu tombol).
**Langkah 2 — Versi**
```
Minecraft       [ 1.21.1        ▾ ]
Loader          [ Fabric        ▾ ]   (lain: segera hadir)
Java            [ 21 (disarankan) ▾ ]
Gradle          [ 8.10.2 (disarankan) ▾ ]
▸ Advanced (Loom, Fabric Loader, Fabric API, mappings)
```
Jika kombinasi invalid → banner kuning + tombol "Perbaiki otomatis".

**Langkah 3 — Template**
Kartu horizontal: `Empty` · `Item` · `Block` · `Item + Block` · `Event`.
Tombol bawah: **Buat Project & Repo**.

Indikator langkah: titik 1-2-3 di atas, tombol Next di bawah (thumb zone).

### 4.3 GitHub (Akun)

```
┌─────────────────────────────┐
│ GitHub                      │
├─────────────────────────────┤
│ ● @username          [Keluar]│
│                             │
│ Repo dari ModForge          │
│  mycoolmod      private  ›  │
│  ruby-tools     public   ›  │
│                             │
│ Izin akses       [Cabut]    │
│ Peringatan kuota Actions    │
└─────────────────────────────┘
```

**Login (Device Flow), satu layar:**
```
Masuk dengan GitHub
Buka github.com/login/device
dan masukkan kode:

      ABCD-1234        [Salin]

[ Buka GitHub ]
Menunggu konfirmasi...
```
Setelah berhasil: otomatis kembali ke layar sebelumnya. Teks kecil menjelaskan: "Repo dibuat otomatis per project, build berjalan di GitHub Actions, token disimpan aman di HP."

### 4.4 Project Overview & Settings

- Header: nama, ikon, chip versi.
- Bagian **Instance**: Minecraft, Java, Gradle (tiap baris tap → bottom sheet pilih versi).
- Bagian **Metadata**: nama, mod id, package, versi mod, author, deskripsi, lisensi, environment.
- Bagian **Advanced**: Loom, Fabric Loader, Fabric API, mappings, branch, retensi artifact, visibilitas repo.
- Bagian **Repo**: link ke GitHub, status sinkron (✔ tersinkron / ● ada perubahan lokal).
- Perubahan versi menampilkan dialog ringkas: "Yang akan berubah" + Konfirmasi.

### 4.5 Code Editor

```
┌─────────────────────────────┐
│ ☰  MyItem.java      ▶ ⋮     │
├─────────────────────────────┤
│  1 package com.sally.myitem;│
│  2                          │
│  3 public class MyItem {    │
│  4   Item.Sett|             │
│     ┌───────────────────────┐
│     │ ⓜ Settings            │
│     │    net.minecraft.item │
│     │ ⓜ Settings()          │
│     └───────────────────────┘
├─────────────────────────────┤
│ Tab ⇤ { } ( ) ; . < > ← →   │  ← key row
└─────────────────────────────┘
 MC 1.21.1 · Index ✔ · 2 ⚠
```

- ☰ membuka file tree (drawer dari kiri). File generated ditandai ikon gembok; ⋮ → "Eject jadi editable".
- **Syntax highlight:** warna dari token tema; kelas, method, string, komentar, anotasi terbedakan jelas, kontras AA di light/dark.
- **Auto-indent & auto-close:** Enter setelah `{` otomatis masuk satu level dan menutup `}`; kutip dan kurung menutup sendiri; paste menyesuaikan indent. Key row punya **Tab** dan **⇤ (dedent)**.
- **Autocomplete:** popup muncul di atas keyboard (maks 6 item, scrollable). Tiap item: ikon tipe (class, method, field, snippet, keyword) + nama + package/signature di baris kecil. Tap atau Tab untuk menerima; class yang belum di-import otomatis menambah `import`.
- **Snippet:** ketik `regitem`, `regblock`, `mixin`, dst. → template dengan titik isian yang bisa dipindah dengan Tab.
- **Signature help:** bar tipis di atas keyboard menampilkan parameter yang sedang diisi (parameter aktif tebal).
- **Hover / dokumentasi:** long-press simbol → bottom sheet berisi signature, package, tanda deprecated, dan "Buka definisi".
- **Diagnostics:** garis bergelombang merah (error) / kuning (peringatan), titik di gutter; tap → bottom sheet pesan + **Quick fix** (tambah import, hapus import tak terpakai, perbaiki typo nama). Error dari build ikut tampil sampai file diedit.
- **Status bar kecil** di bawah: versi MC, status index (✔ siap / ↓ belum diunduh), jumlah peringatan; tap untuk membuka daftar masalah.
- **Index belum ada:** banner "Unduh data autocomplete untuk MC 1.21.1" + tombol **Unduh** (ukuran ditampilkan sebelum mulai). Selama belum ada, autocomplete Tier 0 tetap jalan.
- **JSON:** autocomplete key/nilai untuk model, blockstate, recipe, lang; error skema ditandai inline.
- **Pengaturan editor** (⋮ → Pengaturan): ukuran font, tab size (2/4), autocomplete on/off, format on save, tampilkan indent guides, mode ringan.
- **Satu tangan:** aksi penting (Tab, dedent, undo/redo, kursor) ada di key row bawah; popup tidak menutupi baris yang sedang diketik.

### 4.6 Build (Remote)

```
┌─────────────────────────────┐
│ Build #12          [Cancel] │
│ main · a1b2c3d              │
├─────────────────────────────┤
│ ● Sinkron ke GitHub     ✔   │
│ ● Antre di Actions      ✔   │
│ ● Setup Java 21         ✔   │
│ ● Compile               ◔   │
│ ○ Unggah jar                │
├─────────────────────────────┤
│ > Task :compileJava         │
│ > ...                       │
├─────────────────────────────┤
│ ⚠ 2 error  [Lihat]          │
└─────────────────────────────┘
```
- Sukses: kartu hijau "Build selesai · mycoolmod-1.0.jar" + tombol **Bagikan** / **Salin ke mods** / **Buka di GitHub**.
- Gagal: daftar error klik-ke-baris + saran perbaikan ("Ganti Java ke 21" → tombol Terapkan).
- Offline: banner "Butuh internet untuk build" dan tombol Build nonaktif.
- Konflik repo: dialog "Repo berubah di GitHub: **Pull** / **Timpa** / **Buat branch**".
- User boleh menutup app; notifikasi muncul saat build selesai.
- Riwayat build (tab di layar Build): daftar run dengan status, commit, dan jar yang bisa diunduh ulang.

## 5. Visual Scripting UI

```
┌─────────────────────────────┐
│ ‹ main.graph        ▶ ⋮    │
├─────────────────────────────┤
│                             │
│  ┌────────────┐             │
│  │ On Item Use│──┐          │
│  └────────────┘  │  ┌─────┐ │
│                  └▶ │ If  │ │
│                     └─────┘ │
│            (canvas, pan/zoom)│
├─────────────────────────────┤
│ [＋ Node]  [Kode]  [Variabel]│
└─────────────────────────────┘
```

- Canvas: pinch zoom, drag pan, tap node untuk edit properti (bottom sheet).
- **＋ Node** membuka sheet berkategori dengan pencarian: Event / Aksi / Logika / Data.
- Warna port per tipe (Exec putih, Bool hijau, Number biru, String kuning, Item/Block ungu).
- Koneksi: tarik dari port; port yang tidak kompatibel redup.
- Node tak didukung versi target → border merah + tooltip "Tersedia mulai 1.20.5".
- Tombol **Kode** menampilkan Java hasil generate (read-only) di bottom sheet.
- Error validasi tampil sebagai badge di node, daftar lengkap di panel bawah.

## 6. 3D Item/Block Editor UI

```
┌─────────────────────────────┐
│ ‹ ruby_block        ✔ ⋮    │
├─────────────────────────────┤
│                             │
│         [ viewport 3D ]     │
│          grid 16×16×16      │
│                             │
├─────────────────────────────┤
│ Elemen │ Texture │ Display  │  ← tab panel
├─────────────────────────────┤
│ Cube 1   Pos X[ ] Y[ ] Z[ ] │
│          Size ...  Rot ...  │
├─────────────────────────────┤
│ ↔ Move  ⤢ Scale  ⟳ Rotate   │  ← tool bar
└─────────────────────────────┘
```

- Viewport mengambil ≥ 55% tinggi layar; panel bawah bisa di-drag naik/turun.
- Gesture: 1 jari orbit, 2 jari pan + pinch zoom; tap pilih elemen; long-press menu (duplikat/hapus).
- Toolbar bawah: Move / Scale / Rotate + Undo/Redo.
- Tab **Elemen**: daftar cuboid (visibilitas, kunci, rename) + properti numerik.
- Tab **Texture**: daftar texture, import, painter 16×16 (palet, pensil, hapus, fill, mirror), UV per face dengan preview.
- Tab **Display**: pilih konteks (GUI, Ground, Hand, Fixed, Head) + preview kecil + slider transform.
- Menu ⋮: Preset, Import JSON, Export, "Tambahkan ke registry".
- Mode hemat: batas elemen dan efek dikurangi otomatis jika FPS turun.

## 7. Komponen UI

| Komponen | Pola |
|---|---|
| Version picker | Bottom sheet daftar + pencarian + label "disarankan" |
| Instance chip | Chip kecil `MC 1.21.1`, `Java 21`, `Gradle 8.10` |
| Banner validasi | Kuning (warning)/merah (error) + tombol aksi di kanan |
| Progress | Linear indicator + label langkah |
| Dialog | Judul singkat, 1 kalimat isi, maksimal 2 tombol |
| Empty state | Ikon + 1 kalimat + 1 tombol |
| Snackbar | Konfirmasi aksi ringan, dengan "Urungkan" bila relevan |
| Form | Label di atas, helper text di bawah, error inline |

## 8. Alur Utama

**Alur 1 — Mod pertama**
Buka app → (onboarding 3 layar) → Login GitHub → ＋ → Wizard → pilih template → Overview → **Build** → jar → Bagikan.

**Alur 2 — Ganti versi project**
Overview → tap "Java" → pilih → app cek kompatibilitas → dialog perubahan → konfirmasi → workflow & wrapper diperbarui otomatis di build berikutnya.

**Alur 3 — Bikin item 3D**
Models → ＋ Item → preset → edit cuboid → texture → Display → ✔ Export → "Tambahkan ke registry" → Build.

**Alur 4 — Logic tanpa kode**
Visual → ＋ Node (On Item Use) → ＋ Aksi (Apply Effect) → hubungkan → Kode (cek) → Build.

## 9. Onboarding

3 layar singkat, bisa dilewati:
1. "Bikin mod Fabric langsung dari HP."
2. "Pilih versi, Java, dan Gradle per project."
3. "Visual script & editor 3D tanpa ribet."
Lalu layar **Masuk dengan GitHub** dengan satu tombol, plus penjelasan singkat: repo dibuat otomatis per project dan build berjalan di GitHub Actions.

## 10. Copywriting

- Bahasa santai tapi jelas; kalimat pendek; hindari jargon (jelaskan "Loom" di tooltip).
- Error: apa yang salah + apa yang harus dilakukan. Contoh: "Java 17 tidak cocok dengan Minecraft 1.21. Pakai Java 21?" [Ganti]
- Dukungan ID & EN; semua string di resource.

## 11. Aksesibilitas & Responsif

- Kontras minimal WCAG AA, dark/light/ikut sistem.
- Skala font sistem didukung sampai 130%.
- Content description untuk semua ikon; urutan fokus logis.
- Tablet/landscape: layout dua panel (file tree + editor, viewport 3D + panel samping).
- Satu tangan: aksi utama di bawah, hindari target kecil di pojok atas.

## 12. Do & Don't

| Do | Don't |
|---|---|
| Satu warna aksen | Banyak warna dekoratif |
| Default yang kompatibel | Memaksa user memilih semua opsi teknis |
| Sembunyikan opsi lanjutan | Tampilkan Gradle/Loom di layar awal |
| Error dengan tombol perbaikan | Dump log mentah sebagai satu-satunya info |
| Whitespace lega | Panel padat dengan banyak tombol kecil |
