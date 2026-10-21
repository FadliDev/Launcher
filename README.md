# Sally Text Editor — Friendly Snippets + LSP groundwork patch

This is a changed-files-only patch for the existing Sally Text Editor repository. Merge the ZIP into the repository root and preserve relative paths.

## Snippets

- Uses the upstream Friendly Snippets VS Code-compatible JSON collection instead of maintaining a hardcoded per-language snippet list in `MainActivity.kt`.
- Gradle fetches Friendly Snippets during `preBuild` and bundles the JSON packs into APK assets. After installation, snippets are available offline.
- Snippet packs are selected by file extension, with mappings for many common languages and framework file types. Unknown extensions return no language-specific snippets rather than showing irrelevant snippets for another language.
- VS Code placeholders are normalized to plain text when inserted; tab-stop navigation and choice placeholders are not fully supported by Sora's current `SimpleCompletionItem` insertion path.

## LSP

- Adds Sora Editor's `editor-lsp` dependency.
- Raises `minSdk` to 26, matching Sora's documented LSP requirement.
- See `LSP_SETUP.md`: **the LSP client module is present, but no language-server process or transport is connected yet**. This patch does not claim semantic LSP completion/diagnostics are active. A language server and lifecycle/transport integration are required for real LSP behavior.

## Theme and UI

The existing Sally toolbar, Material components, colors, file picker, open/save behavior, and Sora editor theme setup are retained. This patch mainly changes snippet sourcing and Gradle dependencies.

## Build notes

The Friendly Snippets archive is downloaded at build time, so GitHub Actions needs network access to GitHub. Build was not run in this environment; verify the workflow before relying on the APK. Third-party licenses are listed in `THIRD_PARTY_NOTICES.md`.
