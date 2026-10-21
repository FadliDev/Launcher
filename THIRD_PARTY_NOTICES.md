# Third-party notices

- **Sora Editor** (`io.github.rosemoe:editor`, `io.github.rosemoe:language-textmate`): upstream project at https://github.com/Rosemoe/sora-editor. Consult the upstream repository for the current license and notices.
- **AndroidX, Jetpack Compose, Material 3, Kotlin, and coroutines**: project dependencies resolved from their official Maven repositories; their license and notice terms remain applicable.
- **Optional remote snippets**: when no built-in snippet pack or cached pack exists for an extension, Sally IDE attempts to retrieve a VS Code-style JSON snippet file from https://github.com/rafamadriz/friendly-snippets. The file is saved only after the HTTP response succeeds and the JSON contains usable snippets. Review that repository's current license and the downloaded content before redistributing any fetched pack.

The TextMate grammars and sample snippet packs included in this starter project are minimal hand-authored starter resources; they are not intended to replace full upstream language grammars.
- **Android platform jars (Toolchain Manager)**: when the user taps download, Sally IDE fetches `android-N/android.jar` from https://github.com/Sable/android-platforms. Those jars contain full framework implementations rather than Google's stubs; check the upstream repository for licensing before redistributing them.
- **Gradle distributions**: the version list and `-bin.zip` files come from https://services.gradle.org (Gradle is Apache-2.0 licensed); each download is verified against the published SHA-256.

