# Third-party notices

- **Sora Editor** (`io.github.rosemoe:editor`, `io.github.rosemoe:language-textmate`): upstream project at https://github.com/Rosemoe/sora-editor. Consult the upstream repository for the current license and notices.
- **AndroidX, Jetpack Compose, Material 3, Kotlin, and coroutines**: project dependencies resolved from their official Maven repositories; their license and notice terms remain applicable.
- **Optional remote snippets**: when no built-in snippet pack or cached pack exists for an extension, Sally IDE attempts to retrieve a VS Code-style JSON snippet file from https://github.com/rafamadriz/friendly-snippets. The file is saved only after the HTTP response succeeds and the JSON contains usable snippets. Review that repository's current license and the downloaded content before redistributing any fetched pack.

The TextMate grammars and sample snippet packs included in this starter project are minimal hand-authored starter resources; they are not intended to replace full upstream language grammars.
