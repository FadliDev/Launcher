# LSP support in Sally Text Editor

Sally now includes Sora Editor's `editor-lsp` client module and uses Android API 26+ as required by the Sora LSP guide. This is the editor-side protocol integration dependency, not a claim that every language server is already installed or running.

## Why servers are separate

LSP is a protocol. Each language needs a compatible server executable/runtime and project environment. A client library cannot provide language intelligence by itself. Examples include `pyright-langserver` for TypeScript/Python projects (requires Node.js), `typescript-language-server` (Node.js), `clangd` (native executable), `rust-analyzer` (native executable), and `gopls` (Go executable). Those server runtimes are not bundled in this patch.

## Next implementation boundary

To make LSP active end-to-end, Sally needs a server transport/lifecycle layer with:

1. a supported server source (bundled Android executable, user-provided runtime, or explicitly configured remote LSP gateway);
2. workspace/root URI and document URI management;
3. `initialize` / `initialized`, `textDocument/didOpen`, `didChange`, `didSave`, and `shutdown` lifecycle;
4. completion, hover, definition, references, and diagnostics mapped into Sora's `editor-lsp` API;
5. process/socket cancellation, timeouts, and cleanup on document change/activity destruction.

This patch deliberately does not fake semantic completion by returning hardcoded results when no server is running. The current offline autocomplete is provided by the language-specific Friendly Snippets packs. LSP is not yet connected to a running server in this patch.

## References

- Sora Editor LSP guide: https://project-sora.github.io/sora-editor-docs/guide/using-lsp
- Sora Editor getting started and module list: https://project-sora.github.io/sora-editor-docs/guide/getting-started
