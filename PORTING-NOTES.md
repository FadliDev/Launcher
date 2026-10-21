# Sally Launcher — Control & Mouse Port Notes

This ZIP is a **changed-files-only patch** for the Sally Launcher repository. Extract it into the repository root and allow the files to merge at their original relative paths.

## Adapted from ZalithLauncher

The port adapts the control/mouse behavior to Sally's existing `GameActivity`, Android view overlay, Pojav/LWJGL `CallbackBridge`, and dependency set. It intentionally does not copy Zalith classes that depend on Zalith-specific event buses, resources, preferences, or framework internals.

Included behavior:

- Editable, persistent touch layouts: drag and resize controls, change labels/actions/key combos, colors/opacity/outline/corners, duplicate/delete controls, toggle controls, per-menu/per-game visibility, swipe-through, and touch pass-through.
- Joystick options, configurable dead zone, sprint behavior, hotbar selector, double-tap hotbar shortcut, and quick-drop behavior.
- Expandable drawers with `DOWN`, `LEFT`, `UP`, `RIGHT`, and `FREE` direction handling; old Zalith drawer groups are imported from its JSON layout format.
- Touchpad/virtual mouse mode, pointer visibility/scale, pointer speed, relative in-game mouse-look, direct menu touch, long-press left-click, two-finger right-click, two-finger GUI scrolling, mouse sensitivity, and scroll sensitivity.
- Optional gyro aim, physical keyboard mapping, mouse buttons/wheel, Android pointer capture for physical mice, and basic gamepad buttons/triggers/axes/right-stick aim.
- Presets, JSON export, import through Android's document picker, and import support for legacy Zalith layout roots (`mControlDataList`, `mJoystickDataList`, `mDrawerDataList`).

## Important limits

- This is an adapted integration, not a literal 1:1 source copy. Some Zalith-only behavior cannot work without Zalith's own runtime APIs.
- Gamepad input has baseline mappings and axis support, but **the full Zalith gamepad remapping screen / all per-button multi-action and toggle configuration is not included in this patch**.
- An Android Gradle build and in-game runtime test could not be run in the current editing environment because it does not contain a Gradle wrapper/Gradle installation or Android SDK platform. Run the repository's GitHub Actions workflow to validate compilation and then test the controls in a real device/emulator.

## Build validation

The follow-up patch fixes the compile errors reported by the first GitHub Actions run: use numeric Android key codes for SYSRQ/BREAK instead of nonexistent `KEYCODE_PRINT_SCREEN` / `KEYCODE_PAUSE`, and call `EditText.setSingleLine(true)` rather than an invalid Kotlin property. This container still cannot run the Android Gradle build because it has no Gradle installation, wrapper, or Android SDK platform. Run the existing workflow `.github/workflows/android.yml` (or push the patch and inspect the Actions run). Do not treat this port as runtime-verified until that build succeeds and touch/mouse behavior is tested in Minecraft.

## License attribution

The adapted behavior is based on the GPL-3.0-licensed ZalithLauncher control system. The license text is included at `LICENSES/ZalithLauncher-GPL-3.0.txt`; keep the license and source-availability obligations in mind when distributing a combined/derived build. See `THIRD_PARTY_NOTICES.md`.
