# ModForge Studio (skeleton v0.1)

App Android (Kotlin + Compose M3) untuk bikin mod Minecraft Java. Build mod = GitHub Actions.
Dokumen acuan: `docs/` (PRD, PLAN, ROADMAP, DESIGN).

## Build app ini
Push folder ini ke repo GitHub -> workflow `Build APK` -> unduh artifact `ModForgeStudio-debug`.
(Atau di Termux: `gradle :app:assembleDebug`, Gradle 8.10+ dan JDK 17.)

## Setup GitHub login (sekali)
1. GitHub > Settings > Developer settings > OAuth Apps > New. Callback URL bebas (mis. http://localhost).
2. Centang **Enable Device Flow**. Salin **Client ID** (tanpa secret).
3. Di app: GitHub -> tempel Client ID -> Connect GitHub.

## Alur
Buat project -> edit/Save (lokal) -> tekan Build -> repo dibuat otomatis (nama = nama project)
-> commit+push via Git Data API -> workflow jalan -> jar diunduh ke app -> "Simpan ke Download".

## Belum ada (lihat ROADMAP)
NeoForge & versi lain (tambah di `assets/version-profiles.json`), pull/merge, WorkManager,
editor kanvas visual scripting + codegen, 3D editor. Katalog node ada di `script/NodeCatalog.kt`.
