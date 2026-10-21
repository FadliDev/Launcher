# DESIGN — Sally IDE

Panduan desain UI/UX: Material Design 3, layout, komponen, icon, dan alur utama.

---

## 1. Prinsip desain

1. **Editor dulu.** Area kode selalu paling luas; panel lain bisa disembunyikan.
2. **Satu tangan.** Aksi penting di bawah (mudah dijangkau jempol).
3. **Ringkas tapi lengkap.** Fitur ala VSCode, dikemas sesuai layar kecil.
4. **MD3 konsisten.** Dynamic color, shape, typography, motion bawaan MD3.
5. **Feedback jelas.** Build, download, dan error selalu kelihatan statusnya.

## 2. Tema MD3

### Warna
- Pakai **dynamic color** (Material You) sebagai default; fallback skema statis.
- Mode: **Light, Dark, AMOLED (hitam murni)**.
- Seed color default: biru/ungu (bisa diganti user).
- Role warna dipakai konsisten:

| Role | Dipakai untuk |
|---|---|
| `primary` | tombol Run/Build, item aktif |
| `secondaryContainer` | item terpilih di tree, chip |
| `surface` / `surfaceContainer*` | editor, sidebar, panel (beda tingkat) |
| `error` / `errorContainer` | diagnostics error, build gagal |
| `tertiary` | warning, indikator LSP |

### Tema editor (terpisah dari tema app)
Dark+ , Light+, Monokai, GitHub, Dracula, plus "ikut tema app" (warna sintaks diturunkan dari palet MD3).

### Typography
- UI: Roboto / Google Sans Flex (bawaan MD3 type scale).
- Editor: monospace — JetBrains Mono (default), Fira Code, Cascadia; **ligature opsional**, ukuran 10–24sp, pinch-zoom.

### Shape & elevation
- Komponen kecil radius 8–12dp, kartu/sheet 16–28dp.
- Tonal elevation (bukan bayangan keras) sesuai MD3.

## 3. Layout

### 3.1 HP (portrait)
```
┌────────────────────────────┐
│ ☰  Proyek ▾    ▶ Build  ⋮ │  Top app bar
├────────────────────────────┤
│ MainActivity.kt ✕ │ x.xml  │  Tab bar (scroll)
├────────────────────────────┤
│ 1  package com.sally...    │
│ 2  class MainActivity {    │  Editor
│ 3    ...                   │
│                            │
├────────────────────────────┤
│ Tab ( ) { } ; < > ← → Ctrl │  Extra key row
├────────────────────────────┤
│ Kotlin  Ln 3, Col 5  LSP ● │  Status bar
├────────────────────────────┤
│ 📁  🔍  🔨  🧩  ⚙         │  Navigation bar
└────────────────────────────┘
```
- **Sidebar explorer** = modal navigation drawer (geser dari kiri / tombol ☰).
- **Panel bawah** (Output, Problems, Terminal) = bottom sheet yang bisa ditarik, 3 ukuran: tertutup / setengah / penuh.

### 3.2 Tablet / foldable / landscape
```
┌─────┬───────────────────────┬──────┐
│Rail │ Explorer │  Editor     │ Outline│
│     │ (tree)   │  tabs       │ (opsi) │
│     │          ├─────────────┤        │
│     │          │ Panel bawah │        │
└─────┴──────────┴─────────────┴────────┘
```
- Pakai **NavigationRail** + pane sidebar tetap (adaptive layout / window size class).
- Divider bisa digeser (resize sidebar dan panel bawah).

## 4. Komponen utama

### 4.1 Sidebar tree file explorer
- Tree dengan indentasi, panah expand/collapse, **icon per tipe file dan folder**.
- Aksi: tap = buka, long-press = menu (New, Rename, Delete, Duplicate, Copy path, Reveal), drag & drop pindah.
- Header: nama proyek, tombol New File, New Folder, Refresh, Collapse all, Filter.
- Indikator: file belum disimpan (titik), error (merah), perubahan git (warna/huruf M/A/U).
- Folder khusus (`src`, `res`, `jni`, `build`, `.gradle`) punya icon berbeda; `build/` redup.

### 4.2 Tab bar
- Tab scrollable, tombol tutup, titik "modified", long-press menu (Close others, Close all, Pin).
- Pratinjau tab (italic) untuk buka sekilas.

### 4.3 Editor
- Gutter: nomor baris, fold arrow, ikon diagnostics.
- Selection handle + magnifier, floating toolbar (Cut/Copy/Paste/Select all/Comment).
- Completion popup di atas keyboard, item: icon tipe (method, class, field, snippet), nama, detail tipe; panel dokumentasi bisa di-expand.
- Sticky scroll (opsional), indent guides, bracket pair color.
- Quick actions floating (lampu 💡) untuk quick fix.

### 4.4 Extra key row
Tab, `( ) { } [ ] < > ; " ' = / \ _`, panah, Home/End, Ctrl, Alt, Undo/Redo. Bisa disusun ulang di setting.

### 4.5 Status bar
Bahasa, Ln/Col, encoding, line ending, indent (spasi/tab), status LSP, cabang git, status build.

### 4.6 Panel bawah
Tab: **Output**, **Problems**, **Terminal**, **Logcat** (nanti).
- Output build: teks monospace, error/warning diwarnai, path:baris bisa di-klik.
- Problems: daftar diagnostics per file, filter error/warning.

### 4.7 Command palette
Tombol di top bar atau gesture; pencarian fuzzy untuk file, perintah, setting, simbol (`>`, `@`, `:`) mirip VSCode.

### 4.8 Pencarian
Layar Search: input + toggle (case, regex, whole word), include/exclude glob, hasil dikelompokkan per file, replace all.

## 5. Layar & alur

| Layar | Isi |
|---|---|
| **Home / Welcome** | Proyek terakhir, New Project, Open Folder, Clone, Import zip |
| **New Project** | Pilih template → nama, package, lokasi, minSdk/compileSdk, versi Gradle, JDK |
| **Editor (utama)** | Seperti §3 |
| **Project Settings** | Gradle, JDK, compileSdk, NDK, JVM args, ABI, signing |
| **Toolchain Manager** | JDK, Gradle, Android SDK (android.jar), build-tools, NDK; unduh/hapus/ukuran |
| **Settings** | Tema, editor, keyboard, snippet, LSP, build, tentang |
| **Snippet Manager** | Daftar per bahasa, tambah/edit |
| **Build Output** | Layar penuh dari panel bawah |

### Alur: build pertama
1. Home → **New Project** → pilih "Android Kotlin (Compose)".
2. App cek toolchain → bila belum ada, tampil **dialog "Perlu diunduh"** (JDK, Gradle, android.jar API N, build-tools) dengan total ukuran → Download.
3. Proyek terbuka di Editor.
4. Tap **▶ Build** → pilih task (assembleDebug) → panel Output naik otomatis.
5. Sukses → snackbar "APK siap" + tombol **Install** / **Share**.
6. Gagal → error dengan tombol "Lompat ke baris".

### Alur: ubah versi Gradle
Project Settings → Gradle → dropdown versi (terunduh ✓ / bisa diunduh ☁) → peringatan kompatibilitas (mis. "AGP 8.5 butuh Gradle ≥ 8.7") → Simpan → build berikutnya pakai versi itu.

### Alur: unduh android.jar
Toolchain Manager → Android SDK → daftar API level (dari `sable/android-platforms`) → tap unduh → progress + resume → ✓ terpasang, bisa dipilih sebagai compileSdk.

## 6. Icon

- **UI:** Material Symbols (Rounded), ukuran 24dp, weight konsisten.
- **File type:** set icon file (Kotlin, Java, C/C++, header, XML, Gradle, JSON, TOML, Markdown, properties, `.so`, `.apk`, `.jar`, gambar, zip, dll) + folder (terbuka/tertutup, khusus).
- **Folder khusus:** src, res, java, kotlin, cpp/jni, assets, libs, build, gradle, .git.
- **Icon app:** adaptive icon (foreground/background/monochrome untuk themed icon).
- Sumber icon: Material Symbols (Apache 2.0) dan set file icon open-source (cek lisensi, mis. Material Icon Theme / vscode-icons) atau gambar sendiri sebagai SVG → VectorDrawable.

## 7. Interaksi & motion

- Transisi MD3: shared axis untuk perpindahan layar, container transform untuk FAB → sheet.
- Gesture: swipe kanan = buka sidebar, swipe antar tab, pinch = zoom font, double-tap kata = pilih.
- Haptic ringan untuk aksi penting (build selesai/gagal).
- Predictive back MD3 didukung.

## 8. State penting

| State | Tampilan |
|---|---|
| Toolchain belum ada | Banner + tombol "Siapkan" |
| Indexing | Progress tipis di status bar |
| LSP loading/ready/error | Titik kuning/hijau/merah di status bar |
| Build berjalan | Notifikasi foreground + tombol Stop + indikator top bar |
| Download | Progress per item, bisa pause/resume |
| Storage hampir penuh | Peringatan + shortcut bersihkan cache |
| File berubah di luar app | Dialog "Reload / Keep" |

## 9. Aksesibilitas

- Kontras sesuai MD3, ukuran target sentuh ≥ 48dp.
- Skala font sistem didukung (editor punya ukuran sendiri).
- Content description untuk semua icon, dukung TalkBack untuk navigasi (tree dan tab).
- Mode warna buta warna untuk diagnostics (ikon + warna).

## 10. Struktur resource (acuan implementasi)

```
res/
  drawable/ic_*              → icon UI (vector)
  drawable/ic_file_*         → icon tipe file
  drawable/ic_folder_*       → icon folder
  values/themes.xml          → tema MD3
assets/
  textmate/                  → grammar + tema editor
  snippets/                  → snippet bawaan per bahasa
  templates/                 → zip template proyek
```
