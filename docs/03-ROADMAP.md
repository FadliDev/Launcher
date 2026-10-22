# Roadmap — Klip

Perkiraan untuk tim kecil (1–3 orang). Sesuaikan durasi dengan kapasitas.

## Ringkasan Timeline

| Milestone | Perkiraan | Hasil |
|---|---|---|
| M0 — Prototype | Minggu 1–2 | Trim 1 clip + export jalan |
| M1 — Alpha | Minggu 3–8 | Timeline lengkap, multi-track, speed, volume |
| M2 — Beta | Minggu 9–14 | Teks, filter, transisi, export penuh |
| M3 — MVP Launch | Minggu 15–18 | Rilis Play Store v1.0 |
| M4 — v1.1 | Bulan 5–6 | Keyframe, stiker, voice over, efek |
| M5 — v1.2 | Bulan 7–8 | Caption otomatis, hapus background |
| M6 — v2.0 | Bulan 9–12 | Template, beat sync, auto reframe, plugin efek |

## Detail per Milestone

### M0 — Prototype (Minggu 1–2)
- [ ] Setup repo, modul, CI, tema MD3
- [ ] Putar video dengan ExoPlayer
- [ ] Trim + export via Transformer
- [ ] Uji di 3 perangkat berbeda
- **Exit criteria:** video hasil export bisa diputar normal, audio sinkron.

### M1 — Alpha (Minggu 3–8)
- [ ] Model timeline, command pattern, undo/redo
- [ ] UI timeline: scroll, zoom, trim handle, playhead, snap
- [ ] Import galeri, thumbnail, waveform
- [ ] Split / hapus / duplikat / reorder
- [ ] Speed, reverse, volume, fade
- [ ] Multi-track video & audio
- [ ] Auto-save & daftar proyek
- **Exit criteria:** bisa mengedit video 3–5 menit dengan 10+ clip tanpa crash.

### M2 — Beta (Minggu 9–14)
- [ ] Teks + animasi dasar
- [ ] Filter & adjustment
- [ ] Transisi (fade, slide, zoom, wipe)
- [ ] Overlay & gesture transform
- [ ] Rasio proyek (9:16, 16:9, 1:1, 4:5)
- [ ] Export 720p–4K, fps & bitrate, Foreground Service
- [ ] Proxy media untuk file besar
- [ ] Beta tertutup 20–50 user, kumpulkan crash & feedback
- **Exit criteria:** export failure < 3%, preview ≥ 30 fps di perangkat mid-range.

### M3 — MVP Launch (Minggu 15–18)
- [ ] Bug bash & polish UI
- [ ] Onboarding singkat, bahasa ID/EN
- [ ] Aksesibilitas dasar
- [ ] Play Store listing, screenshot, kebijakan privasi
- [ ] Staged rollout 10% → 50% → 100%
- **Exit criteria:** crash-free ≥ 99%, rating beta ≥ 4.2.

### M4 — v1.1 (Bulan 5–6)
- [ ] Keyframe (posisi, skala, rotasi, opacity)
- [ ] Stiker, emoji, GIF
- [ ] Voice over
- [ ] Efek (glitch, blur, RGB split, shake)
- [ ] Freeze frame, PiP
- [ ] Library font & stiker bebas lisensi

### M5 — v1.2 (Bulan 7–8)
- [ ] Caption otomatis offline (Whisper tiny/base) + editor subtitle
- [ ] Hapus background (segmentasi)
- [ ] Chroma key
- [ ] Mask dasar, stabilizer
- [ ] Unduh model AI on-demand

### M6 — v2.0 (Bulan 9–12)
- [ ] Template proyek (impor/ekspor file)
- [ ] Beat sync otomatis
- [ ] Auto reframe
- [ ] Noise reduction audio
- [ ] Plugin efek komunitas (shader GLSL)
- [ ] Backup proyek ke Drive (opsional)
- [ ] Tablet & foldable layout

## Backlog Ide (belum dijadwalkan)
- Export HDR / 10-bit
- Kompres khusus platform (preset WhatsApp, IG, YouTube)
- Teks-ke-suara (TTS) offline
- Terjemahan caption offline
- Editor berbasis kurva (speed ramp)
- Versi iOS / desktop

## Prioritas (Now / Next / Later)

| Now | Next | Later |
|---|---|---|
| Timeline & export stabil | Keyframe, stiker | Template, plugin |
| Preview performa | Caption otomatis | Auto reframe |
| Teks, filter, transisi | Hapus background | HDR, desktop |

## Cara Mengukur Progress

- Setiap milestone punya **exit criteria** terukur (lihat di atas).
- Review tiap 2 minggu: fitur selesai, bug terbuka, metrik performa.
- Bila terlambat, **potong scope fitur**, jangan kompromikan stabilitas export.
