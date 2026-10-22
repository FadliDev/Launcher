# Plan Teknis & Eksekusi — Klip

## 1. Tech Stack

| Layer | Pilihan | Alasan |
|---|---|---|
| Bahasa | Kotlin (+ sedikit C++/NDK bila perlu) | Standar Android |
| UI | Jetpack Compose + Material 3 | Cepat dibangun, mudah tema dinamis |
| Arsitektur | MVVM + Clean (UI → Domain → Data), Unidirectional Data Flow | Mudah dites, cocok untuk state editor kompleks |
| DI | Hilt | Standar |
| Async | Kotlin Coroutines + Flow | |
| Preview/Playback | Media3 ExoPlayer + custom compositor | Hardware decoding, stabil |
| Efek & render | Media3 `Effect` / OpenGL ES 3.0 (GLSL shader) | Efek real-time di GPU |
| Export | Media3 Transformer (MediaCodec hardware encoder) | Cepat, hemat baterai |
| Audio | Media3 audio processors + waveform extractor | |
| Penyimpanan proyek | Room (SQLite) + JSON timeline | Undo/redo & auto-save mudah |
| Caption otomatis | whisper.cpp (NDK) model tiny/base | Offline |
| Hapus background | MediaPipe Selfie Segmentation / ML Kit | On-device |
| Gambar | Coil | |
| Build | Gradle (Kotlin DSL), version catalog, CI GitHub Actions | |
| Testing | JUnit, Turbine, Compose UI test, Macrobenchmark | |

> Catatan: ffmpeg-kit sudah di-retire oleh maintainernya. Hindari sebagai dependensi inti. Jika perlu FFmpeg, pakai fork komunitas yang aktif atau build sendiri (LGPL).

## 2. Arsitektur Modul

```
:app                  → entry point, navigasi, DI
:core:model           → data class Project, Track, Clip, Effect, Keyframe
:core:timeline        → engine timeline, operasi edit, undo/redo (command pattern)
:core:render          → compositor GL, shader, efek, transisi
:core:media           → decoder, proxy generator, thumbnail, waveform
:core:export          → pipeline export (Transformer)
:core:ai              → whisper, segmentasi
:core:data            → Room, repository, file project
:core:designsystem    → tema MD3, komponen UI
:feature:home         → daftar proyek
:feature:editor       → layar editor (timeline, preview, toolbar)
:feature:text         → editor teks & caption
:feature:audio        → musik, voice over
:feature:export       → pengaturan & progress export
:feature:settings
```

## 3. Model Data Inti (sederhana)

```kotlin
data class Project(val id: String, val name: String, val aspect: Aspect,
    val tracks: List<Track>, val durationUs: Long)

data class Track(val id: String, val type: TrackType, val clips: List<Clip>)

data class Clip(
    val id: String, val source: Uri,
    val startUs: Long,        // posisi di timeline
    val inUs: Long, val outUs: Long,   // trim dari sumber
    val speed: Float = 1f, val volume: Float = 1f,
    val transform: Transform = Transform(),
    val effects: List<Effect> = emptyList(),
    val keyframes: List<Keyframe> = emptyList(),
    val transitionIn: Transition? = null
)
```

Semua perubahan lewat **Command** (`SplitClip`, `TrimClip`, `MoveClip`, `AddEffect`, …) → mudah undo/redo dan serialisasi.

## 4. Strategi Kunci

**Preview mulus**
- Proxy media otomatis (540p/720p, GOP pendek) untuk clip 1080p+/4K; export tetap pakai file asli.
- Cache thumbnail & waveform di disk.
- Batasi decoder aktif (maks 2–3 simultan), reuse texture.
- Render preview di resolusi layar, bukan resolusi project.

**Export andal**
- Pilih encoder hardware (H.264/HEVC); fallback ke software.
- Jalankan di Foreground Service + notifikasi progress; bisa resume/cancel.
- Validasi ruang penyimpanan sebelum mulai.

**Penyimpanan**
- Auto-save tiap perubahan (debounce 2 detik).
- Referensi media via URI persisten; deteksi file hilang & tawarkan relink.

## 5. Struktur Tim & Peran

| Peran | Fokus |
|---|---|
| Android engineer (media/GL) | Timeline engine, render, export |
| Android engineer (UI) | Compose UI, gesture timeline |
| UI/UX designer | Design system, prototipe |
| QA | Matriks perangkat, regresi export |

Solo dev: urutan kerja di bawah sudah disusun agar bisa dikerjakan bertahap.

## 6. Fase Pengerjaan

### Fase 0 — Fondasi (1–2 minggu)
- Setup project, modul, CI, tema MD3
- Prototype: ExoPlayer memutar 1 clip + trim → export 1 clip via Transformer
- Keputusan teknis: compositor (Media3 vs custom GL)

### Fase 1 — Core Editor (4–6 minggu)
- Model timeline + command + undo/redo
- UI timeline (scroll, zoom, drag handle trim, playhead, snap)
- Import media, thumbnail, waveform
- Split/hapus/duplikat/reorder, speed, volume

### Fase 2 — Visual (3–4 minggu)
- Teks (render ke bitmap/GL), stiker/overlay
- Filter & adjustment, transisi
- Multi-track overlay, transform gesture (pinch/rotate/drag)

### Fase 3 — Export & Stabilitas (2–3 minggu)
- Pipeline export penuh, Foreground Service
- Proxy media, optimasi memori
- Beta tertutup (20–50 user)

### Fase 4 — Rilis MVP (1–2 minggu)
- Polish UI, onboarding, bahasa ID/EN
- Play Store listing, kebijakan privasi
- Rilis open beta → produksi bertahap (staged rollout 10% → 100%)

### Fase 5+ — Fitur lanjutan
Keyframe, caption otomatis, hapus background, chroma key, voice over, dst. (lihat Roadmap).

## 7. Strategi Testing

- **Unit**: operasi timeline, command, undo/redo, serialisasi.
- **Golden test**: render frame referensi untuk efek/transisi.
- **Instrumented**: alur import → edit → export.
- **Device lab**: minimal 8 perangkat (Android 8–15; Snapdragon, MediaTek, Exynos, Tensor).
- **Performa**: Macrobenchmark (startup, scroll timeline), profil memori GPU.
- **Fuzz media**: VFR, rotasi metadata, HDR, audio-only, file korup.

## 8. CI/CD

- GitHub Actions: lint (ktlint/detekt), unit test, build APK/AAB tiap PR.
- Artefak debug APK otomatis; release signed via secrets.
- Crash reporting: Firebase Crashlytics atau alternatif open source (Sentry/ACRA) — opt-in.

## 9. Daftar Risiko Teknis Awal

1. Sinkronisasi audio-video saat speed ramp & multi-track.
2. Perbedaan perilaku MediaCodec antar chipset.
3. Memori GPU saat banyak layer 4K.
4. Export HDR/10-bit (tunda ke v2).
5. Ukuran model Whisper vs ukuran APK (download on-demand lewat Play Feature Delivery / unduh dalam app).
