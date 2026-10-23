# FreshLauncher

Launcher Minecraft Java untuk Android. Gratis dan open source di bawah **GNU GPL v3**.

## Status
Tahap awal pengembangan. Fitur yang sudah ada:
- UI Jetpack Compose dengan tab Beranda, Versi, Akun, dan Pengaturan
- Akun offline dengan UUID standar Minecraft
- Unduhan versi dari Mojang, dengan verifikasi SHA-1
- Pengaturan RAM untuk game

Belum ada:
- Runtime JVM yang bisa dipakai (JRE dan engine native belum terintegrasi)
- Renderer grafis, bridge input, dan dukungan mod loader

## Lisensi
FreshLauncher berlisensi **GPL-3.0**, lihat `LICENSE`. Atribusi lengkap untuk pustaka dan proyek yang dipakai ada di `NOTICE.md`.

FreshLauncher adalah proyek komunitas dan tidak berafiliasi dengan Mojang atau Microsoft.

## Build
Butuh JDK 17 dan Android SDK (API 34). Dengan Gradle 8.9 terpasang:

    gradle :app:assembleDebug

Atau push ke GitHub. Workflow `.github/workflows/build.yml` menghasilkan APK debug sebagai artifact.

## Rencana berikutnya
1. Integrasi engine native dari ZalithLauncher (sumber dan `jniLibs`).
2. Penyediaan JRE terpisah dari repo, supaya riwayat git tidak membengkak.
3. Menjalankan Minecraft end-to-end di perangkat.
