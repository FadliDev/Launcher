# Third-party notices

## ZalithLauncher control-system compatibility work

The Sally touch-control and mouse-input layer in this patch adapts the publicly available control-system concepts and interaction behavior from ZalithLauncher, including editable control layouts, context-aware controls, touchpad and mouse gestures, hotbar input, gamepad mapping, and gyro aim.

- Upstream project: ZalithLauncher
- Upstream license: GNU General Public License v3.0 (GPL-3.0)
- License text: `LICENSES/ZalithLauncher-GPL-3.0.txt`
- Sally-side implementation: newly integrated and adapted for Sally's `GameActivity`, `CallbackBridge`, and existing Android view hierarchy; it is not a drop-in copy of Zalith's classes, many of which depend on Zalith-only settings, events, and resources.

This notice does not replace the license terms that apply to any combined or distributed derivative work. Preserve the license and source availability requirements that apply to the repository when publishing builds containing this adapted implementation.
