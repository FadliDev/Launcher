# Implementation status

## Implemented in this starter
- Compose/Material 3 shell, dynamic palette, dark mode.
- Project create/list/delete, local project files, metadata, and ZIP export.
- Code editor as a plain multiline text editor.
- Basic starter code and project-level GitHub Actions workflow generation.
- CI workflow to create a debug APK artifact.

## Deliberately not marked as implemented
- GitHub OAuth/Device Flow and encrypted token store.
- Remote repository creation or push from the app.
- GitHub Actions run monitoring, cancellation, logs, or `.jar` download.
- Full version matrix / live loader metadata.
- Syntax highlighting, Tree-sitter autocomplete, visual scripting, model editing, and registry wizard.

## Validation
Source was reviewed and file structure is prepared for Android Studio / GitHub Actions. A local Android build could not be executed in this environment because Gradle and the Android SDK are not installed; CI build remains the actual build verification step.
