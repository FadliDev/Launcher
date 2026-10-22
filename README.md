# Klip — Video Editor Android (tanpa langganan, tanpa watermark)

Kotlin · Jetpack Compose (MD3, tema gelap) · Media3 ExoPlayer + Transformer · minSdk 26

## Build
- Lokal: buka di Android Studio, atau `gradle assembleDebug` (Gradle 8.10.2, JDK 17)
- GitHub Actions: push ke repo → workflow `Build APK` → unduh artifact `klip-debug-apk`

## Sudah ada (M0 + sebagian M1)
Pilih video (Photo Picker), preview, timeline clip, trim, split, hapus, duplikat, geser urutan,
speed 0.25x–4x, volume 0–200%, rasio 9:16/16:9/1:1/4:5/4:3, undo/redo (50 langkah),
auto-save, export 720p/1080p/4K H.264 ke Movies/Klip tanpa watermark.

## Belum (lihat 03-ROADMAP.md)
Teks, filter, transisi, multi-track/overlay, audio terpisah, fade, reverse, proxy media,
Foreground Service export, keyframe, caption otomatis, dst.
