# Design — Klip

Panduan UX/UI. Terinspirasi pola umum editor mobile (timeline di bawah, preview di atas), tetapi **identitas visual dan aset dibuat sendiri**.

## 1. Prinsip Desain

1. **Satu tangan**: kontrol utama di zona jempol (bawah layar).
2. **Preview selalu terlihat**: jangan pernah menutupi preview penuh saat mengedit.
3. **Progressive disclosure**: toolbar sederhana, opsi lanjutan muncul kontekstual.
4. **Gelap by default**: editor video lebih nyaman dan warna lebih akurat di tema gelap.
5. **Tanpa paywall visual**: tidak ada ikon mahkota, tidak ada upsell di alur edit.

## 2. Design System (Material 3)

### Warna (tema gelap)
| Token | Nilai | Pemakaian |
|---|---|---|
| `background` | `#0F0F12` | Latar utama |
| `surface` | `#1A1A20` | Panel, timeline |
| `surfaceVariant` | `#26262E` | Kartu, toolbar |
| `primary` | `#7C9CFF` | Aksi utama, playhead |
| `onPrimary` | `#0B1230` | Teks di atas primary |
| `secondary` | `#5EE0C0` | Aksen, trim handle aktif |
| `tertiary` | `#FFB86B` | Marker, peringatan lembut |
| `error` | `#FF6B6B` | Hapus, gagal export |
| `outline` | `#3A3A45` | Garis pemisah |

Tema terang tersedia, dan **Dynamic Color (Material You)** opsional di Android 12+.

### Tipografi
- Font UI: **Inter** atau **Roboto Flex** (bebas lisensi)
- Skala: Display 32 · Title 20 · Body 14 · Label 12 · Timecode **monospace** 12

### Bentuk & Elevasi
- Radius: kartu 16dp, tombol 12dp, chip 8dp, bottom sheet 28dp
- Elevasi tonal (MD3), bukan bayangan keras

### Ikon
- Material Symbols Rounded (open source), ukuran 24dp, touch target ≥ 48dp

### Motion
- Durasi 150–250 ms, easing `emphasized`
- Gesture timeline: tanpa animasi tambahan agar terasa langsung (responsif)

## 3. Peta Navigasi

```
Splash
 └─ Home
     ├─ Proyek Baru → Pemilih Media → Editor
     ├─ Daftar Proyek (grid) → Editor
     ├─ Settings
     └─ (v2) Template
Editor
 ├─ Export Sheet → Progress → Hasil (bagikan / buka)
 └─ Panel Alat (bottom sheet): Teks, Audio, Stiker, Efek, Filter, ...
```

## 4. Layar Utama

### 4.1 Home
```
┌──────────────────────────┐
│  Klip              ⚙     │
│                          │
│  ┌────────────────────┐  │
│  │  ＋  Proyek Baru    │  │  ← tombol besar
│  └────────────────────┘  │
│                          │
│  Proyek Terakhir         │
│  ┌─────┐ ┌─────┐ ┌─────┐ │
│  │thumb│ │thumb│ │thumb│ │
│  │ 0:32│ │ 1:05│ │ 0:15│ │
│  └─────┘ └─────┘ └─────┘ │
│  (tekan lama: ganti nama,│
│   duplikat, hapus)       │
└──────────────────────────┘
```

### 4.2 Pemilih Media
- Tab: Video · Foto · Semua · Album
- Multi-select dengan nomor urutan, pratinjau cepat (tap lama)
- Tombol **Tambah (n)** di bawah

### 4.3 Editor (layar inti)
```
┌──────────────────────────┐
│ ✕   Judul proyek   Export│  ← top bar
├──────────────────────────┤
│                          │
│        PREVIEW           │  ← 45–50% tinggi layar
│        (rasio proyek)    │
│                          │
├──────────────────────────┤
│ ▶  00:12 / 01:05  ↶ ↷ ⛶ │  ← kontrol putar & undo/redo
├──────────────────────────┤
│  ruler waktu             │
│ ─────────┼───────────────│  ← playhead di tengah
│ [video ][video ][video ] │  ← track utama
│ [text    ][sticker]      │  ← overlay
│ [♪ audio ▁▂▅▂▁▂▃▅▂▁]     │  ← audio
├──────────────────────────┤
│ Edit  Audio  Teks  Stiker│
│ Efek  Overlay Filter Rasio│  ← toolbar bawah (scroll horizontal)
└──────────────────────────┘
```

**Perilaku timeline**
- Playhead tetap di tengah, timeline yang bergeser (gaya mobile)
- Pinch untuk zoom, drag dua jari untuk scroll vertikal antar track
- Tap clip → muncul toolbar kontekstual (Split, Speed, Volume, Hapus, Duplikat, Animasi, Efek, Transisi)
- Handle trim kiri/kanan dengan haptic saat snap ke playhead/clip lain
- Tekan lama & seret untuk reorder
- Tombol **+** di ujung track utama untuk tambah clip

### 4.4 Panel Alat (Bottom Sheet)
- Tinggi 35–40%, preview tetap terlihat
- Drag handle untuk memperbesar/menutup
- Contoh **Teks**: tab *Ketik · Font · Gaya · Animasi*; slider langsung memperbarui preview

### 4.5 Export
```
┌──────────────────────────┐
│  Export                  │
│  Resolusi   [720][1080][4K]│
│  Frame rate [24][30][60] │
│  Kualitas   ───●─── Tinggi│
│  Perkiraan ukuran: 48 MB │
│  ☐ Simpan juga audio saja│
│                          │
│  [      Export      ]    │
└──────────────────────────┘
```
- Progress: persen, estimasi waktu, tombol Batal; layar boleh dikunci/di-background
- Hasil: preview, **Bagikan** (WhatsApp, IG, TikTok, YouTube), **Buka di Galeri**
- Pesan jelas: "Tanpa watermark"

### 4.6 Settings
Bahasa · Tema · Lokasi penyimpanan · Hapus cache/proxy · Kualitas preview · Analytics (opt-in) · Tentang & lisensi open source · Donasi (opsional)

## 5. Komponen Kunci

| Komponen | Catatan |
|---|---|
| TimelineView | Canvas Compose kustom, virtualisasi clip, cache thumbnail |
| ClipItem | Thumbnail strip, label durasi, handle trim, highlight terpilih |
| PlayheadView | Garis `primary` + timecode melayang |
| ToolbarContextual | Muncul dengan animasi slide-up saat clip dipilih |
| ValueSlider | Slider dengan angka, reset double-tap |
| ColorPicker | Preset + hex + eyedropper dari preview |
| ExportProgressCard | Foreground notification sinkron |

## 6. Interaksi & Gesture

| Gesture | Aksi |
|---|---|
| Tap clip | Pilih |
| Tap dua kali preview | Putar/jeda |
| Drag di preview | Pindah elemen terpilih |
| Pinch/rotate di preview | Skala/rotasi elemen |
| Geser timeline | Scrub |
| Pinch timeline | Zoom |
| Tekan lama clip | Mode reorder |
| Swipe turun bottom sheet | Tutup panel |

Haptic: snap, split, hapus. Konfirmasi hapus lewat Snackbar **Undo** (bukan dialog).

## 7. Konten Teks (Microcopy)

- Nada: ramah, ringkas, tanpa jargon.
- Empty state: "Belum ada proyek. Yuk bikin video pertamamu."
- Error export: "Export gagal: ruang penyimpanan kurang (butuh ±120 MB). Kosongkan dulu lalu coba lagi."

## 8. Aksesibilitas

- Kontras minimal WCAG AA
- Label `contentDescription` untuk semua ikon
- Dukungan font scale hingga 200% di luar area timeline
- Mode tangan kiri (opsi: pindah tombol aksi utama)
- Hindari informasi hanya lewat warna

## 9. Responsif

| Layar | Layout |
|---|---|
| Phone portrait | Layout di atas |
| Phone landscape | Preview kiri, panel alat kanan, timeline bawah |
| Tablet/foldable | Panel samping tetap + timeline lebar |

## 10. Aset & Lisensi

- Ikon: Material Symbols (Apache 2.0)
- Font: Inter / Roboto (OFL / Apache)
- Stiker, transisi, LUT filter: buat sendiri atau pakai CC0
- Musik: tidak dibundel; user pakai file sendiri (atau pustaka CC0 opsional)
- **Jangan** memakai nama, logo, ikon, atau template CapCut.

## 11. Deliverable Desain Berikutnya

1. Wireframe low-fi seluruh layar (Figma)
2. Prototipe interaktif editor (uji dengan 5–8 user)
3. Komponen Compose di `:core:designsystem`
4. Uji usability: tugas "buat video 15 detik + teks + export" < 5 menit
