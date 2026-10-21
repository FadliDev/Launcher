# Ember Launcher

Launcher Minecraft Java untuk Android (Kotlin + Jetpack Compose), dibuat dari nol dengan arsitektur ala ZalithLauncher.

## Fitur
- UI Compose: Home / Versi / Akun / Setelan
- Akun offline (UUID `OfflinePlayer:<nama>`)
- Download versi Mojang (client, library, asset; paralel, cek SHA-1)
- **Java ikut di dalam APK** (assets/components, sama seperti Zalith): JRE 8, 17, 21, 25 (arm64). Otomatis dipilih dari `javaVersion` di JSON versi, diekstrak sekali saat pertama main.
- **Renderer default: Krypton Wrapper** (NG-GL4ES, `libng_gl4es.so`). Kalau lib-nya tidak ada di APK, otomatis fallback ke GL4ES (`libgl4es_114.so`).
- Jembatan native Zalith (`src/main/jni`, ndk-build): JLI launch, EGL/GLES bridge, GLFW input
- Kontrol sentuh dasar: geser = kamera/kursor, tap = klik kiri, tahan = klik kiri ditahan (mining), 2 jari = klik kanan, Back = Esc

## Build di GitHub Actions
1. Push project ini ke repo GitHub.
2. Tab **Actions** → workflow **Build Ember** (jalan otomatis tiap push, atau *Run workflow*).
3. APK ada di **Artifacts → Ember-debug-apk**.

Workflow meng-clone `BZLZHH/NG-GL4ES`, mem-build jadi `libng_gl4es.so`, lalu memasukkannya ke APK. Opsi:
- Punya binary sendiri? Taruh di `third_party/libng_gl4es.so` (build dilewati).
- Pakai fork lain: set repo variable `KRYPTON_REPO` (dan `KRYPTON_REF` untuk branch/tag).
- Kalau build Krypton gagal, workflow tetap lanjut dengan peringatan dan APK jatuh ke GL4ES.

## Catatan
- Belum pernah dikompilasi/dites di perangkat; build pertama kemungkinan butuh beberapa perbaikan kecil.
- Belum ada: login Microsoft, loader (Fabric/Forge), Minecraft <= 1.12 (LWJGL2), MioLibPatcher, kontrol layar kustom, keyboard fisik.
- Lisensi: berisi kode dari ZalithLauncher/PojavLauncher (GPLv3/LGPLv3) → project ini ikut GPLv3 (lihat `LICENSE-ZalithLauncher-GPLv3`).
