# PROGRESS — Sally IDE

File ini dicatat ulang **setiap ada update**. Entri terbaru di atas (bagian *Log perubahan*).

Legenda: ✅ terverifikasi di perangkat · 🟡 kode ditulis, belum dikompilasi/diuji · ⬜ belum dikerjakan

## Status saat ini
**Versi:** v0.1.0 (Fase 1 — Editor dasar) · **Status build:** 🟡 v0.1.1 lolos kompilasi, tapi v0.1.3 memperbaiki 2 crash startup dari stack trace; menunggu uji ulang di perangkat

| Fase | Isi | Status |
|---|---|---|
| 0 | Spike & validasi (JDK/Gradle/aapt2 di HP) | ⬜ perlu perangkat nyata |
| 1 | Editor dasar (MVP) | 🟡 ~85% kode ditulis, menunggu build pertama |
| 2 | Proyek & file explorer | ⬜ (explorer minimal sudah ada di Fase 1) |
| 3 | SDK & toolchain manager | ⬜ |
| 4 | Build Gradle | ⬜ |
| 5 | Intelligence & LSP | ⬜ |
| 6 | NDK / C++ | ⬜ |
| 7 | Polish & rilis | ⬜ |

## Fase 1 — rincian
| Item | Status | Catatan |
|---|---|---|
| Setup proyek Compose + MD3 + CI | 🟡 | Single-module `app` dulu (lebih aman dibuild di Termux); dipecah modul saat Fase 2–3. Workflow: `.github/workflows/build.yml` |
| Integrasi Sora-editor 0.23.4 | 🟡 | `CodeEditorView.kt`, satu `Content` per tab → undo/redo tidak hilang saat pindah tab |
| Highlighting Java/Kotlin/C++/XML/Gradle/JSON (+TOML, Properties, Markdown) | 🟡 | Grammar TextMate **buatan sendiri yang sederhana** (`tools/gen_assets.py`). Bisa diganti grammar VSCode penuh |
| Tab multi-file | 🟡 | Titik "modified", dialog simpan saat tutup tab, auto-save saat app ke background |
| Undo/redo | 🟡 | Tombol di top bar + key row |
| Find & replace (regex, case) | 🟡 | `FindBar` memakai `editor.searcher` |
| Go to line | 🟡 | Menu ⋮ |
| Auto indent / auto close bracket / bracket match | 🟡? | Mengandalkan default Sora + `language-configuration.json`; **perlu dicek di perangkat** |
| Code folding | ⬜ | TextMate di Sora tidak otomatis memberi blok fold; ditunda |
| Extra key row | 🟡 | Tab, panah, `{ } ( ) [ ] < > ; " ' = / \ _ +`, Home/End. **Ctrl belum** |
| Snippet engine + snippet bawaan | 🟡 | Format VSCode (`$1`, `${1:def}`, `$0`); placeholder pertama otomatis terpilih. Navigasi tabstop berikutnya belum. Bawaan: Kotlin, Java, C++, XML, Gradle, Markdown |
| Auto complete dasar | 🟡 | Keyword + snippet + kata di file lewat **SuggestionBar** (bukan popup Sora, popup bawaan dimatikan). File >300K karakter: hanya keyword/snippet |
| Tema: light/dark/AMOLED + dynamic color | 🟡 | Tema editor Dark+/AMOLED/Light+ ikut mode app |
| Explorer minimal | 🟡 | Tree workspace + file baru. Rename/hapus/pindah/icon per tipe = Fase 2 |

## Keputusan & penyimpangan dari dokumen
- Single-module dulu (PLAN §3 menyarankan multi-module).
- Completion lewat SuggestionBar, bukan popup Sora (lebih sedikit API Sora yang dipakai dan tidak menutupi editor di layar kecil).
- Workspace: `Android/data/com.sally.ide/files/workspace` (storage app, tanpa izin storage). SAF/buka folder bebas = Fase 2.
- Font editor: monospace sistem dulu (JetBrains Mono perlu aset font; belum ditambahkan).
- Teks UI sementara hard-coded Bahasa Indonesia; ekstraksi ke strings.xml ID/EN di Fase 7.
- Gradle wrapper **jar tidak disertakan** (tidak bisa diunduh di sandbox). Jalankan `gradle wrapper` sekali, atau pakai `gradle assembleDebug` langsung.

## Cara build
```
cd SallyIDE
gradle assembleDebug          # atau ./gradlew setelah `gradle wrapper --gradle-version 8.9`
# APK: app/build/outputs/apk/debug/
```
Butuh JDK 17 + Android SDK (compileSdk 35). CI GitHub Actions membangun otomatis.

## ⚠️ Yang paling mungkin error saat build pertama
Kode ditulis dari pengetahuan API Sora-editor 0.23.x tanpa bisa dikompilasi. Titik rawan (kirim log error-nya, biar langsung diperbaiki):
1. `EditorRuntime.kt` — nama paket/kelas TextMate (`ThemeModel`, `AssetsFileResolver`, `TextMateColorScheme.create`)
2. `CodeEditorView.kt` — `EditorSearcher.SearchOptions(...)`, `replaceCurrentMatch`, `subscribeEvent`, `getComponent(EditorAutoCompletion)`, `moveSelectionHome/End`
3. Versi library (AGP 8.7.3 / Kotlin 2.0.21 / Compose BOM 2024.10.01) bila Termux Gradle-nya beda
4. Nama ikon `material-icons-extended`

## Berikutnya
1. Build pertama + perbaiki error kompilasi
2. Uji di perangkat: highlight, tab, snippet, find/replace, tema
3. Kalau stabil → rilis `v0.1` lalu lanjut sesuai urutan cepat ROADMAP: **Fase 0 spike → Fase 3 → Fase 4** (build di HP)

## Log perubahan
### 2026-10-22 — v0.1.3 (2 crash dari stack trace)
- **Crash 1 (thread AsyncAnalyzer):** `NoSuchMethodError ArrayList.toArray(IntFunction)` di tm4e → API Java 11 tidak ada di Android 10. Desugar library dinaikkan 2.0.4 → 2.1.5. *Belum terbukti*; kalau masih crash, fallback: highlight tanpa tm4e / mode aman
- **Crash 2 (main):** `IndexOutOfBounds` di `ScrollableTabRow` M3 saat tab ke-2 dibuka → diganti tab strip buatan sendiri (`TabBar`)
### 2026-10-22 — v0.1.2 (diagnosis crash startup)
- Laporan: app crash saat masuk sampai beranda. Stack trace belum ada
- Ditambah `CrashLog` (SallyApp.kt): crash ditulis ke file, peluncuran berikutnya menampilkan `CrashScreen` (salin teks)
- Ditambah **mode aman** (tanpa TextMate/highlight) dari layar crash atau Setelan, untuk mengisolasi apakah highlight penyebabnya
- Dugaan awal (belum terbukti): TextMate/tm4e di thread background (grammar/tema/language-configuration)

### 2026-10-22 — v0.1.1 (perbaikan build)
- Log CI: hanya 7 error, semuanya di `CodeEditorView.kt`; file lain lolos kompilasi
- `moveSelectionLeft/Right/Up/Down/Home/End` tidak ada di Sora 0.23.4 → diganti navigasi kursor manual via `setSelection`
- `replaceCurrentMatch` tidak ada → diganti `replaceThis`
- Titik rawan nomor 2 di atas sekarang tinggal `EditorSearcher.SearchOptions`, `subscribeEvent`, `getComponent`

### 2026-10-22 — v0.1.0 (scaffold + Fase 1)
- Proyek dibuat dari PRD/PLAN/ROADMAP/DESIGN (disalin ke `docs/`)
- Ditambahkan: Gradle KTS + version catalog, CI, Manifest, ikon adaptif
- Ditambahkan: Compose MD3 UI (top bar, tab bar, editor, suggestion bar, extra key row, status bar, bottom nav, drawer explorer, sheet snippet & setelan)
- Ditambahkan: `tools/gen_assets.py` → grammar/tema/snippet di `assets/`
- `docs/ROADMAP.md`: item Fase 1 yang sudah ditulis ditandai `[~]`
