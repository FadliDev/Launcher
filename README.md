# ModForge Studio

Android-first starter app for creating Minecraft Java mod projects. UI and initial feature scope follow the provided `PRD.md`, `PLAN.md`, `DESIGN.md`, and `ROADMAP.md`.

## Current starter features

- Material 3, portrait-first UI with dynamic color and a dark-theme toggle.
- Four main destinations: Projects, GitHub & Build, Templates, Settings.
- Four-step project creation wizard for loader, Minecraft version, mod metadata, and starter template.
- Local project storage isolated in the app's private storage.
- Fabric starter templates for Empty, Item, Block and Command, plus starter structures for Forge and NeoForge.
- Basic file explorer/editor with manual local save.
- Export a project as ZIP using Android's share sheet.
- Per-mod GitHub Actions workflow template which builds and uploads a `.jar` artifact.
- GitHub Actions workflow in this repository to build the Android debug APK.

## Important limitations of this first implementation

This is a buildable starter, not the complete application described in the PRD. GitHub OAuth/Device Flow, repository creation, push-on-build, run polling, remote logs, and automatic artifact download are not implemented in the Android UI yet. Visual scripting and the 3D model editor are roadmap screens. No fake success state is shown for these features.

The project generator pins initial example profiles for Fabric 1.21.1, Forge 1.20.1, and NeoForge 1.21.1. Other versions produce a warning because loader/plugin/mappings/API coordinates must be verified before a reliable build. For the most dependable first test, choose Fabric 1.21.1 and the Item template. Check current official loader docs before publishing a mod.

## Build APK in GitHub Actions

1. Push this folder to a GitHub repository.
2. Open the **Actions** tab and run **Build ModForge Studio APK** (or push a commit).
3. Download `ModForgeStudio-debug-apk` from the workflow run artifacts.

The workflow installs Gradle 8.13 directly, so the repository does not need a checked-in Gradle wrapper JAR for CI.

## Open in Android Studio

Use a recent Android Studio with Android SDK Platform 36 and JDK 17. This starter intentionally does not check in a Gradle wrapper JAR; CI installs Gradle 8.13 directly. In a local terminal with Gradle 8.13 available, run `gradle :app:assembleDebug`. The first build needs network access to download dependencies.

## Project persistence and export

Project source files are stored in app-private storage under `files/modforge/projects/`. Export ZIP excludes internal `project.json` metadata. Exported projects include `.github/workflows/build.yml` and can be uploaded to a repository manually.

## Next implementation milestone

Implement GitHub Device Flow with a developer-configured OAuth client ID, encrypted token storage, create-repository + atomic Git Data API commits, build status polling, job log parsing, and artifact `.jar` download. Then add Room/DataStore, a proper code editor with syntax highlighting, version-profile packs, and tests for each supported loader/version pair.
