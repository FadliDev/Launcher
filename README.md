# Sally Launcher

A new Android launcher project foundation for Minecraft: Java Edition. The UI and launcher core are original project code; the future game-engine integration is designed as a separate adapter boundary.

## Current milestone (0.1.0)

Implemented:
- Native Android app with Kotlin, Jetpack Compose, and Material 3-inspired dark portrait UI.
- Loads Mojang's official version manifest over HTTPS.
- Filters release, snapshot, and old-beta versions.
- Downloads a selected version's version JSON and client JAR into app-private storage.
- Checks the downloaded JAR size against metadata when provided.
- Defines a `MinecraftEngineAdapter` boundary so engine integration stays separate from UI.

Not implemented yet:
- Full version installer: asset index/assets, libraries, rules, natives, logging configuration, and checksum verification.
- Java runtime provisioning and architecture selection.
- Renderer/input integration and actual Minecraft process launch.
- Microsoft account authentication.
- Mod loader installation.

The **Play** button intentionally explains that the engine adapter is not available yet. Downloading a client JAR alone does not make a Minecraft installation runnable.

## Build

- Android Studio with JDK 17
- Android SDK Platform 35
- Gradle 8.9-compatible environment (AGP 8.7.3)

Open this directory in Android Studio and sync Gradle, then run the `app` configuration. This archive does not include the Gradle wrapper JAR; Android Studio can use its configured Gradle installation, or generate a wrapper locally with Gradle 8.9 (`gradle wrapper --gradle-version 8.9`).

## Package details

- Application ID: `com.sally.launcher`
- Version: `0.1.0` (`versionCode` 1)
- Minimum Android: API 26
- Target SDK: 35

## Engine integration strategy

Keep the launcher core independent from any engine implementation. The intended next step is to evaluate and integrate compatible PojavLauncher components behind `MinecraftEngineAdapter`, preserving their applicable license notices and source distribution obligations. Do not copy code or native binaries from third-party projects without reviewing their licenses and required notices.

## Legal / distribution note

Minecraft is a trademark of Mojang Studios/Microsoft. This project is unofficial and is not affiliated with or endorsed by Mojang or Microsoft. Use official Minecraft metadata and assets in accordance with applicable terms. Any future Pojav-derived components must comply with their actual upstream license and attribution requirements.
