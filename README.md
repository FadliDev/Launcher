# FreshLauncher

Launcher Minecraft Java untuk Android. Gratis dan open source di bawah **GNU GPL v3**.

## Status
Tahap awal pengembangan. Fitur yang sudah ada:
- UI Jetpack Compose dengan tab Beranda, Versi, Akun, dan Pengaturan
- Akun offline dengan UUID standar Minecraft
- Unduhan versi dari Mojang, dengan verifikasi SHA-1
- Pengaturan RAM untuk game

Sudah diintegrasikan (belum diuji di perangkat):
- Engine native ZalithLauncher di `app/src/main/jni` dan `jniLibs/arm64-v8a` (dibangun dengan ndk-build)
- Pemanggilan `VMLauncher.launchJVM` lewat `NativeJvm`

Belum ada:
- Setup environment JRE (LD_LIBRARY_PATH, renderer, linker namespace) sebelum JVM dipanggil
- JRE itu sendiri (tidak di-commit ke git, lihat rencana di bawah)
- Renderer grafis, bridge input, dan dukungan mod loader

## Lisensi
FreshLauncher berlisensi **GPL-3.0**, lihat `LICENSE`. Atribusi lengkap untuk pustaka dan proyek yang dipakai ada di `NOTICE.md`.

FreshLauncher adalah proyek komunitas dan tidak berafiliasi dengan Mojang atau Microsoft.

## Build
Butuh JDK 17 dan Android SDK (API 34). Dengan Gradle 8.9 terpasang:

    gradle :app:assembleDebug

Atau push ke GitHub. Workflow `.github/workflows/build.yml` menghasilkan APK debug sebagai artifact.

## Rencana berikutnya
1. Setup environment JRE dan pengujian launch di perangkat.
2. Penyediaan JRE terpisah dari repo (unduh saat build atau pasang manual), supaya riwayat git tidak membengkak.
3. Menjalankan Minecraft end-to-end di perangkat.
