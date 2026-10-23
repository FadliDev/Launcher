# ModForge Android — M1

Status: **M1 (Fondasi & Akun GitHub)**. Sudah ada:

- Proyek multi-modul: `:app`, `:core:model`, `:core:github`
- `VersionMatrix` (data dari `core/model/src/main/assets/version-matrix.json`) + validator, dengan unit test
- Login GitHub: OAuth Device Flow atau Personal Access Token
- Token dienkripsi AES-GCM dengan kunci Android Keystore
- Klien GitHub REST dengan retry, backoff, dan penanganan rate limit
- Layar Akun: login, tes buat repo + satu commit awal, logout, cabut akses
- Tema minimalis sesuai `docs/specs/DESIGN.md`

Belum ada (lihat `docs/specs/ROADMAP.md`): wizard project, template engine, sync engine, build orchestrator, editor.

## Build

Build dilakukan di GitHub Actions (`.github/workflows/android.yml`). Untuk lokal, butuh JDK 17 dan Android SDK platform 35:

```bash
gradle wrapper --gradle-version 8.10.2   # sekali saja, membuat ./gradlew
./gradlew test
./gradlew :app:assembleDebug
```

APK debug ada di `app/build/outputs/apk/debug/app-debug.apk`.

## Mengaktifkan Device Flow

Buat OAuth App di GitHub, aktifkan opsi Device Flow, lalu isi **Client ID** di layar GitHub. Client ID bersifat publik; jangan pernah memasukkan client secret.

## Catatan

- Nilai di `version-matrix.json` ditandai `"verified": false`. Verifikasi lewat CI di M0 sebelum dipakai untuk build.
- String UI masih hardcoded dalam Bahasa Indonesia. Pemindahan ke `strings.xml` (ID/EN) dijadwalkan di M6.
- Hapus token dari perangkat tidak mencabut izin di GitHub. Gunakan tombol "Cabut izin di GitHub".
