# FreshLauncher

Launcher Minecraft Java untuk Android — skeleton awal, ditulis dari nol dengan Jetpack Compose.

## Cakupan versi ini
- UI Compose dengan 3 tab: Beranda, Versi, Akun
- Akun offline dengan UUID standar Minecraft
- Ambil daftar versi dari Mojang (`version_manifest_v2.json`)
- Unduh metadata, client.jar, library, dan asset (terverifikasi SHA-1)
- Susun argumen JVM dan game dari metadata versi (`LaunchBuilder`)

## Yang belum ada
- **Runtime JVM**: `GameLauncher` masih berupa stub (`PendingRuntimeLauncher`). Perlu JRE terbundel dan pemanggilan `libjli.so` via JNI.
- **Native library** (LWJGL natives): unduhan native dilewati.
- **Bridge GL/GLFW dan input**: belum ada. Rujuk arsitektur ZalithLauncher sebagai referensi.
- **Renderer** (Krypton Wrapper, dll.): belum ada.
- **Java args dari pengguna, mod loader, modpack**: belum ada.

## Build
Butuh JDK 17 dan Android SDK (API 34). Dengan Gradle 8.9 terpasang:

    gradle :app:assembleDebug

Atau cukup push ke GitHub; workflow `.github/workflows/build.yml` menghasilkan APK debug sebagai artifact.

## Catatan
Kode ini tidak menyalin source ZalithLauncher. Jika nanti ada bagian yang diambil langsung dari sana, lisensi GPL-3.0 berlaku untuk bagian tersebut.
