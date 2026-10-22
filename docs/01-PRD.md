# PRD — Klip (nama sementara): Video Editor Android Tanpa Langganan

> Versi 1.0 · Status: Draft · Platform: Android 8.0+ (API 26+)

## 1. Ringkasan

Klip adalah aplikasi video editor Android untuk konten pendek (TikTok, Reels, Shorts) dan video biasa. Fokus: **semua fitur inti gratis, tanpa watermark, tanpa langganan, tanpa paywall**, dan **pemrosesan offline di perangkat**.

## 2. Masalah

- Editor populer menaruh fitur penting (export 4K, efek, auto caption, hapus background) di balik langganan.
- Watermark dan iklan mengganggu.
- Banyak fitur AI butuh internet dan upload video ke server (privasi).
- Editor ringan gratis biasanya terlalu basic atau penuh iklan.

## 3. Tujuan & Non-Tujuan

**Tujuan**
1. Editor timeline multi-track yang mulus di HP kelas menengah.
2. Fitur ala CapCut: trim, split, speed, transisi, teks, stiker, filter, keyframe, caption otomatis, hapus background.
3. Export sampai 4K 60fps tanpa watermark.
4. Semua fitur inti berjalan offline.

**Non-Tujuan (v1)**
- Editor desktop / web.
- Template marketplace berbasis cloud.
- Kolaborasi real-time.
- Platform iOS.

## 4. Target Pengguna

| Persona | Kebutuhan |
|---|---|
| Kreator pemula (pelajar, 15–25 th) | Edit cepat, template, caption otomatis, gratis |
| Kreator menengah | Multi-track, keyframe, kontrol audio, export kualitas tinggi |
| UMKM / jualan online | Video produk cepat, teks & stiker, rasio 9:16 |

## 5. Fitur

### 5.1 MVP (Must Have)
- Import video/foto/audio dari galeri (Photo Picker + SAF)
- Project: buat, simpan otomatis, duplikat, hapus
- Timeline: trim, split, hapus, duplikat, reorder, zoom, snap
- Multi-track video/overlay + audio (minimal 3 layer video, 3 audio)
- Preview real-time + play/pause/seek + frame step
- Speed (0.1x–10x), reverse, volume, fade in/out
- Teks: font, warna, stroke, shadow, background, posisi, animasi masuk/keluar
- Transisi dasar (fade, slide, zoom, wipe)
- Filter & adjustment (brightness, contrast, saturation, warmth, vignette)
- Rasio: 9:16, 16:9, 1:1, 4:5, 4:3
- Undo/redo (minimal 50 langkah)
- Export: 720p/1080p/4K, 24/30/60 fps, bitrate pilihan, **tanpa watermark**
- Background musik dari file lokal + ekstrak audio dari video

### 5.2 v1.x (Should Have)
- Keyframe (posisi, skala, rotasi, opacity)
- Stiker, emoji, overlay PNG/GIF
- Caption otomatis offline (Whisper on-device) + styling
- Hapus background (segmentasi orang, on-device)
- Chroma key (green screen)
- Efek video (glitch, blur, RGB split, shake)
- Voice over (rekam langsung)
- Freeze frame, picture-in-picture, mask
- Stabilizer dasar

### 5.3 v2 (Nice to Have)
- Template/proyek siap pakai (lokal, bisa diimpor/ekspor file template)
- Beat sync otomatis
- Noise reduction audio
- Auto reframe (ubah rasio dengan subjek tetap di tengah)
- Plugin efek komunitas (shader GLSL)
- Backup proyek ke Drive (opsional, tanpa akun wajib)

## 6. Kebutuhan Non-Fungsional

| Area | Target |
|---|---|
| Performa preview | ≥ 30 fps untuk 1080p di Snapdragon 6-series / Helio G90 |
| Cold start | < 2 detik |
| Export 1 menit 1080p30 | < 90 detik di HP mid-range |
| Ukuran APK | < 60 MB (split per ABI) |
| Stabilitas | Crash-free session ≥ 99.5% |
| Privasi | Tidak ada upload video; analytics opt-in |
| Aksesibilitas | Touch target ≥ 48dp, kontras AA, TalkBack di UI utama |
| Bahasa | Indonesia, Inggris (v1) |

## 7. Model Bisnis (tanpa langganan)

- **Gratis penuh, tanpa watermark.**
- Opsi pendanaan: donasi (Saweria/Trakteer/GitHub Sponsors), open source, atau iklan **non-intrusif hanya di layar beranda** (opsional, tidak pernah di editor/export).
- Tidak ada fitur dikunci.

## 8. Risiko & Mitigasi

| Risiko | Mitigasi |
|---|---|
| Fragmentasi codec/hardware Android | Gunakan Media3 (Transformer + ExoPlayer), fallback software encoder, test matrix perangkat |
| Preview patah di HP low-end | Proxy media (transcode resolusi rendah), preview 540p adaptif |
| Lisensi musik/aset | Hanya bundel aset berlisensi bebas (CC0/OFL); user bawa musik sendiri |
| Lisensi library (FFmpeg LGPL/GPL) | Pakai Media3 sebagai inti; jika FFmpeg dipakai, build LGPL dan link dinamis |
| Meniru merek/aset CapCut | Jangan pakai nama, logo, aset, atau template CapCut; UI terinspirasi, bukan disalin |
| Memory leak / OOM | Texture pool, batasi decoder paralel, profiling rutin |

## 9. Metrik Sukses

- Retensi D7 ≥ 25%
- Rata-rata export sukses per user aktif ≥ 2/minggu
- Export failure rate < 2%
- Rating Play Store ≥ 4.4
- Waktu dari buka app → export pertama < 5 menit

## 10. Kriteria Rilis MVP

- Semua fitur 5.1 lolos QA pada minimal 8 perangkat uji (low/mid/high, Android 8–15)
- Tidak ada crash blocker; crash-free ≥ 99%
- Export tanpa watermark terverifikasi
- Kebijakan privasi & lisensi open source tersedia
