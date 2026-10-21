package com.sally.ember.game

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import net.kdt.pojavlaunch.LwjglGlfwKeycode as K
import org.lwjgl.glfw.CallbackBridge as CB
import kotlin.math.hypot
import kotlin.math.roundToInt

private typealias FrameLP = FrameLayout.LayoutParams
private const val FILL = -1
private const val CONTENT = -2

/** Ref-counts pressed keys/buttons by input source so two controls can't release each other's held key. */
internal object Held {
    private val keyOwners = HashMap<Int, MutableSet<String>>()
    private val mouseOwners = HashMap<Int, MutableSet<String>>()

    @Synchronized fun key(code: Int, down: Boolean, owner: String = "default") {
        val owners = keyOwners[code]
        if (down) {
            val set = keyOwners.getOrPut(code) { mutableSetOf() }
            if (set.add(owner) && set.size == 1) {
                CB.setModifiers(code, true)
                CB.sendKeyPress(code, CB.getCurrentMods(), true)
            }
        } else if (owners != null && owners.remove(owner) && owners.isEmpty()) {
            keyOwners.remove(code)
            CB.setModifiers(code, false)
            CB.sendKeyPress(code, CB.getCurrentMods(), false)
        }
    }

    @Synchronized fun tap(code: Int, owner: String = "tap-${System.nanoTime()}") {
        key(code, true, owner); key(code, false, owner)
    }

    @Synchronized fun mouse(button: Int, down: Boolean, owner: String = "default") {
        val owners = mouseOwners[button]
        if (down) {
            val set = mouseOwners.getOrPut(button) { mutableSetOf() }
            if (set.add(owner) && set.size == 1) CB.sendMouseButton(button, true)
        } else if (owners != null && owners.remove(owner) && owners.isEmpty()) {
            mouseOwners.remove(button)
            CB.sendMouseButton(button, false)
        }
    }

    @Synchronized fun releaseOwner(owner: String) {
        keyOwners.keys.toList().forEach { key(it, false, owner) }
        mouseOwners.keys.toList().forEach { mouse(it, false, owner) }
    }

    @Synchronized fun releaseAll() {
        keyOwners.keys.toList().forEach { code ->
            CB.setModifiers(code, false)
            CB.sendKeyPress(code, CB.getCurrentMods(), false)
        }
        mouseOwners.keys.toList().forEach { CB.sendMouseButton(it, false) }
        keyOwners.clear(); mouseOwners.clear()
    }
}

/** Full touch-control layer: editable button layouts, multiple visibility contexts, mouse gestures and hardware input. */
internal class TouchControls(ctx: Context, private val onKeyboard: () -> Unit) : FrameLayout(ctx), View.OnCapturedPointerListener {
    private val density = resources.displayMetrics.density
    private val slop = ViewConfiguration.get(ctx).scaledTouchSlop.toFloat()
    private val handler = Handler(Looper.getMainLooper())
    private var profile = ControlProfile.load(ctx)
    private val mouseGestures = MouseGestureView(ctx) { profile }
    private val gyro = GyroAimController(ctx) { dx, dy ->
        if (CB.isGrabbing()) {
            CB.sendCursorPos(CB.mouseX + dx, CB.mouseY + dy)
            mouseGestures.invalidatePointer()
        }
    }
    private val widgets = LinkedHashMap<String, ControlWidget>()
    private val activeGroups = mutableSetOf<String>()
    private val swipeTargets = mutableMapOf<String, ControlSpec?>()
    private var capturedMouseX = Float.NaN
    private var capturedMouseY = Float.NaN
    private var physicalMouseX = Float.NaN
    private var physicalMouseY = Float.NaN
    private val externalKeyOwners = mutableMapOf<String, Pair<Boolean, Int>>()
    private val externalButtonOwners = mutableMapOf<String, Pair<Boolean, Int>>()
    private val externalAxes = mutableMapOf<String, Int>()
    private var grabbing = CB.isGrabbing()
    private var editing = false
    private var resumed = false
    private var lastMouseButtons = 0
    private var lastHotbarTapIndex = -1
    private var lastHotbarTapAt = 0L
    private var gamepadRightX = 0f
    private var gamepadRightY = 0f
    private val gamepadMouseTick = object : Runnable {
        override fun run() {
            if (CB.isGrabbing() && (kotlin.math.abs(gamepadRightX) > profile.joystickDeadZone || kotlin.math.abs(gamepadRightY) > profile.joystickDeadZone)) {
                CB.sendCursorPos(CB.mouseX + gamepadRightX * profile.cameraSensitivity * 3.2f,
                    CB.mouseY + gamepadRightY * profile.cameraSensitivity * 3.2f)
                handler.postDelayed(this, 16L)
            }
        }
    }

    init {
        clipChildren = false
        clipToPadding = false
        isFocusable = true
        isFocusableInTouchMode = true
        setOnCapturedPointerListener(this)
        addView(mouseGestures, FrameLP(FILL, FILL))
        profile.controls.filter { it.group.isNotBlank() }.forEach { activeGroups.remove(it.group) }
        profile.save(context)
        applyProfileToGyro()
        rebuildWidgets()
        setGrabbing(grabbing)
    }

    private fun dp(value: Int) = (value * density).roundToInt()

    private fun rebuildWidgets() {
        widgets.values.forEach { removeView(it) }
        widgets.clear()
        profile.controls.forEach { spec ->
            val widget = ControlWidget(context, spec, this)
            widgets[spec.id] = widget
            addView(widget, FrameLP(dp(spec.widthDp), dp(spec.heightDp), Gravity.TOP or Gravity.START))
        }
        refreshPositions()
        refreshVisibility()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        refreshPositions()
    }

    private fun refreshPositions() {
        if (width <= 0 || height <= 0) return
        widgets.forEach { (id, widget) ->
            val spec = profile.controls.firstOrNull { it.id == id } ?: return@forEach
            applyPosition(widget, spec)
        }
        refreshDrawerPositions()
    }

    /** Align grouped drawer buttons from their launcher button, preserving Zalith's orientation behavior. */
    private fun refreshDrawerPositions() {
        if (width <= 0 || height <= 0) return
        val margin = dp(2)
        profile.controls.filter { it.action.startsWith("drawer:") }.forEach { drawer ->
            if (drawer.drawerOrientation == "FREE") return@forEach
            val group = drawer.action.substringAfter(':')
            val drawerLeft = (drawer.x * (width - dp(drawer.widthDp)).coerceAtLeast(0)).roundToInt()
            val drawerTop = (drawer.y * (height - dp(drawer.heightDp)).coerceAtLeast(0)).roundToInt()
            val drawerWidth = dp(drawer.widthDp); val drawerHeight = dp(drawer.heightDp)
            profile.controls.filter { it.group == group }.forEachIndexed { index, child ->
                val widget = widgets[child.id] ?: return@forEachIndexed
                val childWidth = dp(child.widthDp); val childHeight = dp(child.heightDp)
                var left = drawerLeft
                var top = drawerTop
                when (drawer.drawerOrientation) {
                    "RIGHT" -> { left = drawerLeft + drawerWidth + margin + (childWidth + margin) * index; top = drawerTop }
                    "LEFT" -> { left = drawerLeft - margin - childWidth * (index + 1) - margin * index; top = drawerTop }
                    "UP" -> { left = drawerLeft; top = drawerTop - margin - childHeight * (index + 1) - margin * index }
                    "DOWN" -> { left = drawerLeft; top = drawerTop + drawerHeight + margin + (childHeight + margin) * index }
                }
                left = left.coerceIn(0, (width - childWidth).coerceAtLeast(0))
                top = top.coerceIn(0, (height - childHeight).coerceAtLeast(0))
                child.x = if (width <= childWidth) 0f else left.toFloat() / (width - childWidth)
                child.y = if (height <= childHeight) 0f else top.toFloat() / (height - childHeight)
                applyPosition(widget, child)
            }
        }
    }

    private fun applyPosition(widget: ControlWidget, spec: ControlSpec) {
        if (width <= 0 || height <= 0) return
        val lp = (widget.layoutParams as? FrameLP) ?: FrameLP(dp(spec.widthDp), dp(spec.heightDp), Gravity.TOP or Gravity.START)
        lp.width = dp(spec.widthDp); lp.height = dp(spec.heightDp)
        lp.leftMargin = (spec.x.coerceIn(0f, 1f) * (width - dp(spec.widthDp)).coerceAtLeast(0)).roundToInt()
        lp.topMargin = (spec.y.coerceIn(0f, 1f) * (height - dp(spec.heightDp)).coerceAtLeast(0)).roundToInt()
        if (widget.layoutParams !== lp) widget.layoutParams = lp else widget.requestLayout()
    }

    private fun refreshVisibility() {
        widgets.forEach { (id, view) ->
            val spec = profile.controls.firstOrNull { it.id == id } ?: return@forEach
            val isSettings = spec.action == "settings"
            val inContext = if (editing) true else if (grabbing) spec.displayInGame else spec.displayInMenu
            val groupOk = spec.group.isBlank() || spec.group in activeGroups || editing
            val hotbarOk = spec.action != "hotbar" || profile.hotbarEnabled
            val controlsOk = profile.controlsVisible || isSettings || editing
            view.visibility = if (inContext && groupOk && hotbarOk && controlsOk) View.VISIBLE else View.GONE
            view.alpha = if (editing) 1f else spec.opacity.coerceIn(0.15f, 1f)
            view.isEnabled = view.visibility == View.VISIBLE
            view.invalidate()
        }
        mouseGestures.visibility = View.VISIBLE
        mouseGestures.invalidatePointer()
    }

    fun setGrabbing(isGrabbing: Boolean) {
        // A grab transition switches from absolute UI pointer coordinates to relative camera deltas.
        capturedMouseX = Float.NaN; capturedMouseY = Float.NaN
        physicalMouseX = Float.NaN; physicalMouseY = Float.NaN
        grabbing = isGrabbing
        if (!isGrabbing) releaseAll() else onHostWindowFocus(hasWindowFocus())
        refreshVisibility()
        if (!isGrabbing) {
            mouseGestures.post { mouseGestures.syncCursor() }
            mouseGestures.postDelayed({ mouseGestures.syncCursor() }, 150)
        }
        applyProfileToGyro()
    }

    fun releaseAll() {
        widgets.values.forEach { it.cancelTouch() }
        swipeTargets.keys.toList().forEach { finishSwipe(it) }
        mouseGestures.cancelInput()
        Held.releaseAll()
        externalKeyOwners.clear(); externalButtonOwners.clear(); externalAxes.clear()
        lastMouseButtons = 0; gamepadRightX = 0f; gamepadRightY = 0f
        capturedMouseX = Float.NaN; capturedMouseY = Float.NaN
        physicalMouseX = Float.NaN; physicalMouseY = Float.NaN
        handler.removeCallbacks(gamepadMouseTick)
        if (hasPointerCapture()) releasePointerCapture()
    }

    fun onHostResume() { resumed = true; gyro.onResume(); applyProfileToGyro(); onHostWindowFocus(hasWindowFocus()) }
    fun onHostPause() { resumed = false; releaseAll(); gyro.onPause() }

    fun onHostWindowFocus(hasFocus: Boolean) {
        if (!hasFocus) {
            if (hasPointerCapture()) releasePointerCapture()
            capturedMouseX = Float.NaN; capturedMouseY = Float.NaN
            physicalMouseX = Float.NaN; physicalMouseY = Float.NaN
        } else if (CB.isGrabbing()) {
            post { if (isAttachedToWindow && hasWindowFocus() && CB.isGrabbing() && !hasPointerCapture()) requestPointerCapture() }
        }
    }

    override fun onCapturedPointer(view: View, event: MotionEvent): Boolean {
        if (!event.isFromSource(InputDevice.SOURCE_MOUSE) && !event.isFromSource(InputDevice.SOURCE_TRACKBALL)) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                val device = event.device
                val hasRelX = device?.getMotionRange(MotionEvent.AXIS_RELATIVE_X, event.source) != null
                val hasRelY = device?.getMotionRange(MotionEvent.AXIS_RELATIVE_Y, event.source) != null
                val dx: Float
                val dy: Float
                if (hasRelX && hasRelY) {
                    dx = event.getAxisValue(MotionEvent.AXIS_RELATIVE_X)
                    dy = event.getAxisValue(MotionEvent.AXIS_RELATIVE_Y)
                } else if (event.isFromSource(InputDevice.SOURCE_TRACKBALL)) {
                    dx = event.x; dy = event.y
                } else if (capturedMouseX.isFinite() && capturedMouseY.isFinite()) {
                    dx = event.x - capturedMouseX; dy = event.y - capturedMouseY
                } else {
                    capturedMouseX = event.x; capturedMouseY = event.y
                    return true
                }
                capturedMouseX = event.x; capturedMouseY = event.y
                if (CB.isGrabbing()) {
                    CB.sendCursorPos(CB.mouseX + dx * profile.cameraSensitivity, CB.mouseY + dy * profile.cameraSensitivity)
                } else {
                    CB.sendCursorPos((CB.mouseX + dx * profile.mouseSpeed).coerceIn(0f, (CB.windowWidth - 1).coerceAtLeast(0).toFloat()),
                        (CB.mouseY + dy * profile.mouseSpeed).coerceIn(0f, (CB.windowHeight - 1).coerceAtLeast(0).toFloat()))
                }
                mouseGestures.invalidatePointer()
                return true
            }
            MotionEvent.ACTION_BUTTON_PRESS, MotionEvent.ACTION_BUTTON_RELEASE -> {
                val button = when (event.actionButton) {
                    MotionEvent.BUTTON_PRIMARY -> K.GLFW_MOUSE_BUTTON_LEFT.toInt()
                    MotionEvent.BUTTON_SECONDARY -> K.GLFW_MOUSE_BUTTON_RIGHT.toInt()
                    MotionEvent.BUTTON_TERTIARY -> K.GLFW_MOUSE_BUTTON_MIDDLE.toInt()
                    else -> return false
                }
                Held.mouse(button, event.actionMasked == MotionEvent.ACTION_BUTTON_PRESS, "captured-mouse-$button")
                return true
            }
            MotionEvent.ACTION_SCROLL -> {
                CB.sendScroll(event.getAxisValue(MotionEvent.AXIS_HSCROLL).toDouble(), event.getAxisValue(MotionEvent.AXIS_VSCROLL).toDouble())
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                capturedMouseX = Float.NaN; capturedMouseY = Float.NaN
                return true
            }
        }
        return false
    }

    private fun applyProfileToGyro() {
        gyro.sensitivity = profile.gyroSensitivity
        gyro.invertX = profile.gyroInvertX
        gyro.invertY = profile.gyroInvertY
        gyro.setEnabled(profile.gyroEnabled)
    }

    fun onHardwareKey(event: KeyEvent): Boolean {
        if (GameInputMapper.isGamepad(event)) {
            val action = GameInputMapper.gamepadKey(event.keyCode) ?: return false
            val owner = "gamepad-${event.deviceId}-${event.keyCode}"
            when (action) {
                is GameInputMapper.HardwareAction.Key -> Held.key(action.code, event.action != KeyEvent.ACTION_UP, owner)
                is GameInputMapper.HardwareAction.Mouse -> Held.mouse(action.code, event.action != KeyEvent.ACTION_UP, owner)
            }
            return true
        }
        // Real keyboards go through GLFW so keyboard/mouse users are not limited to the touch overlay.
        if (event.isSystem || event.action == KeyEvent.ACTION_MULTIPLE) return false
        val glfw = GameInputMapper.keyboardKey(event.keyCode) ?: return false
        val owner = "keyboard-${event.deviceId}-${event.keyCode}"
        Held.key(glfw, event.action != KeyEvent.ACTION_UP, owner)
        if (event.action == KeyEvent.ACTION_UP) externalKeyOwners.remove(owner) else externalKeyOwners[owner] = true to glfw
        // Forward printable characters separately: GLFW key callbacks do not synthesize text in this bridge.
        if (event.action == KeyEvent.ACTION_DOWN && event.isPrintingKey &&
            !event.isCtrlPressed && !event.isAltPressed && !event.isMetaPressed) {
            val unicode = event.unicodeChar
            if (unicode != 0) CB.sendChar(unicode.toChar(), CB.getCurrentMods())
        }
        return true
    }

    fun onHardwareMotion(event: MotionEvent): Boolean {
        if (hasPointerCapture() && event.isFromSource(InputDevice.SOURCE_MOUSE)) return false
        if (event.isFromSource(InputDevice.SOURCE_MOUSE)) return processPhysicalMouse(event)
        if (event.isFromSource(InputDevice.SOURCE_JOYSTICK) || event.isFromSource(InputDevice.SOURCE_GAMEPAD)) {
            return processGamepadAxes(event)
        }
        return false
    }

    private fun processPhysicalMouse(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_HOVER_MOVE, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_HOVER_ENTER -> {
                if (CB.isGrabbing()) {
                    if (event.actionMasked == MotionEvent.ACTION_HOVER_ENTER || !physicalMouseX.isFinite() || !physicalMouseY.isFinite()) {
                        physicalMouseX = event.x; physicalMouseY = event.y
                    } else {
                        val dx = event.x - physicalMouseX; val dy = event.y - physicalMouseY
                        CB.sendCursorPos(CB.mouseX + dx * profile.cameraSensitivity, CB.mouseY + dy * profile.cameraSensitivity)
                        physicalMouseX = event.x; physicalMouseY = event.y
                    }
                } else {
                    CB.sendCursorPos(event.x.coerceIn(0f, (CB.windowWidth - 1).coerceAtLeast(0).toFloat()),
                        event.y.coerceIn(0f, (CB.windowHeight - 1).coerceAtLeast(0).toFloat()))
                    physicalMouseX = event.x; physicalMouseY = event.y
                }
                mouseGestures.invalidatePointer()
                val state = event.buttonState
                val buttonMap = listOf(MotionEvent.BUTTON_PRIMARY to K.GLFW_MOUSE_BUTTON_LEFT.toInt(),
                    MotionEvent.BUTTON_SECONDARY to K.GLFW_MOUSE_BUTTON_RIGHT.toInt(), MotionEvent.BUTTON_TERTIARY to K.GLFW_MOUSE_BUTTON_MIDDLE.toInt())
                buttonMap.forEach { (androidButton, glfwButton) ->
                    val was = (lastMouseButtons and androidButton) != 0
                    val now = (state and androidButton) != 0
                    val owner = "physical-mouse-$glfwButton"
                    if (was != now) Held.mouse(glfwButton, now, owner)
                }
                lastMouseButtons = state
                if (event.actionMasked == MotionEvent.ACTION_MOVE || event.actionMasked == MotionEvent.ACTION_HOVER_MOVE) {
                    val sy = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
                    val sx = event.getAxisValue(MotionEvent.AXIS_HSCROLL)
                    if (sy != 0f || sx != 0f) CB.sendScroll(sx.toDouble(), sy.toDouble())
                }
                return true
            }
            MotionEvent.ACTION_BUTTON_PRESS, MotionEvent.ACTION_BUTTON_RELEASE -> {
                val down = event.actionMasked == MotionEvent.ACTION_BUTTON_PRESS
                if (event.isFromSource(InputDevice.SOURCE_MOUSE) && event.x.isFinite() && event.y.isFinite() && !CB.isGrabbing()) {
                    CB.sendCursorPos(event.x.coerceIn(0f, (CB.windowWidth - 1).coerceAtLeast(0).toFloat()),
                        event.y.coerceIn(0f, (CB.windowHeight - 1).coerceAtLeast(0).toFloat()))
                }
                val btn = when (event.actionButton) {
                    MotionEvent.BUTTON_PRIMARY -> K.GLFW_MOUSE_BUTTON_LEFT.toInt()
                    MotionEvent.BUTTON_SECONDARY -> K.GLFW_MOUSE_BUTTON_RIGHT.toInt()
                    MotionEvent.BUTTON_TERTIARY -> K.GLFW_MOUSE_BUTTON_MIDDLE.toInt()
                    else -> return false
                }
                Held.mouse(btn, down, "physical-mouse-$btn")
                lastMouseButtons = event.buttonState
                return true
            }
            MotionEvent.ACTION_SCROLL -> {
                if (!CB.isGrabbing()) CB.sendCursorPos(event.x.coerceIn(0f, (CB.windowWidth - 1).coerceAtLeast(0).toFloat()),
                    event.y.coerceIn(0f, (CB.windowHeight - 1).coerceAtLeast(0).toFloat()))
                CB.sendScroll(event.getAxisValue(MotionEvent.AXIS_HSCROLL).toDouble(), event.getAxisValue(MotionEvent.AXIS_VSCROLL).toDouble())
                return true
            }
        }
        return false
    }

    private fun processGamepadAxes(event: MotionEvent): Boolean {
        val id = event.deviceId
        val dead = profile.joystickDeadZone
        val x = event.getAxisValue(MotionEvent.AXIS_X).takeIf { it.isFinite() } ?: 0f
        val y = event.getAxisValue(MotionEvent.AXIS_Y).takeIf { it.isFinite() } ?: 0f
        val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
        val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)
        setAxisKey("$id-left", K.GLFW_KEY_A.toInt(), x < -dead || hatX < -0.5f)
        setAxisKey("$id-right", K.GLFW_KEY_D.toInt(), x > dead || hatX > 0.5f)
        setAxisKey("$id-forward", K.GLFW_KEY_W.toInt(), y < -dead || hatY < -0.5f)
        setAxisKey("$id-back", K.GLFW_KEY_S.toInt(), y > dead || hatY > 0.5f)

        val device = event.device
        val rx = if (device?.getMotionRange(MotionEvent.AXIS_RX, event.source) != null)
            event.getAxisValue(MotionEvent.AXIS_RX) else event.getAxisValue(MotionEvent.AXIS_Z)
        val ry = if (device?.getMotionRange(MotionEvent.AXIS_RY, event.source) != null)
            event.getAxisValue(MotionEvent.AXIS_RY) else event.getAxisValue(MotionEvent.AXIS_RZ)
        gamepadRightX = rx.takeIf { it.isFinite() } ?: 0f
        gamepadRightY = ry.takeIf { it.isFinite() } ?: 0f
        handler.removeCallbacks(gamepadMouseTick)
        if (CB.isGrabbing() && (kotlin.math.abs(gamepadRightX) > dead || kotlin.math.abs(gamepadRightY) > dead)) handler.post(gamepadMouseTick)
        val l2 = event.getAxisValue(MotionEvent.AXIS_LTRIGGER).coerceIn(0f, 1f) > 0.5f
        val r2 = event.getAxisValue(MotionEvent.AXIS_RTRIGGER).coerceIn(0f, 1f) > 0.5f
        Held.mouse(K.GLFW_MOUSE_BUTTON_RIGHT.toInt(), l2, "gamepad-axis-$id-l2")
        Held.mouse(K.GLFW_MOUSE_BUTTON_LEFT.toInt(), r2, "gamepad-axis-$id-r2")
        return true
    }

    private fun setAxisKey(owner: String, code: Int, down: Boolean) {
        Held.key(code, down, "gamepad-axis-$owner")
        if (down) externalAxes[owner] = code else externalAxes.remove(owner)
    }

    fun handleAction(spec: ControlSpec, pressed: Boolean, toggleState: Boolean = false) {
        val owner = "touch-${spec.id}"
        when (spec.action) {
            "key" -> spec.keyCodes.forEach { Held.key(it, if (spec.toggle) toggleState else pressed, owner) }
            "mouse-left" -> Held.mouse(K.GLFW_MOUSE_BUTTON_LEFT.toInt(), if (spec.toggle) toggleState else pressed, owner)
            "mouse-right" -> Held.mouse(K.GLFW_MOUSE_BUTTON_RIGHT.toInt(), if (spec.toggle) toggleState else pressed, owner)
            "mouse-middle" -> Held.mouse(K.GLFW_MOUSE_BUTTON_MIDDLE.toInt(), if (spec.toggle) toggleState else pressed, owner)
            "sprint" -> Held.key(K.GLFW_KEY_LEFT_CONTROL.toInt(), if (spec.toggle) toggleState else pressed, owner)
            "keyboard" -> if (pressed) onKeyboard()
            "settings" -> if (pressed) showControlPanel()
            "toggle-mouse" -> if (pressed) {
                // Zalith's SPECIALBTN_VIRTUALMOUSE toggles the touchpad/cursor mode, not merely the arrow.
                profile.touchpadEnabled = !profile.touchpadEnabled
                mouseGestures.setPointerVisible(profile.pointerVisible)
                persist(); refreshVisibility()
            }
            "toggle-controls" -> if (pressed) {
                profile.controlsVisible = !profile.controlsVisible
                persist(); refreshVisibility()
            }
            // Preserve Zalith's special-button wheel sign conventions.
            "scroll-up" -> if (pressed) CB.sendScroll(0.0, -1.0)
            "scroll-down" -> if (pressed) CB.sendScroll(0.0, 1.0)
            "menu" -> if (pressed) Held.tap(K.GLFW_KEY_ESCAPE.toInt())
            else -> if (spec.action.startsWith("drawer:") && pressed) {
                val group = spec.action.substringAfter(':')
                if (!activeGroups.add(group)) activeGroups.remove(group)
                persist(); refreshVisibility()
            }
        }
    }

    fun dispatchPassThrough(widget: View, event: MotionEvent) {
        val forwarded = MotionEvent.obtain(event)
        try {
            forwarded.offsetLocation(widget.left.toFloat(), widget.top.toFloat())
            mouseGestures.dispatchPassThroughTouchEvent(forwarded)
        } finally {
            forwarded.recycle()
        }
    }

    fun beginSwipe(spec: ControlSpec) {
        if (spec.swipeable && !spec.toggle) swipeTargets[spec.id] = spec
    }

    fun updateSwipe(spec: ControlSpec, widget: View, localX: Float, localY: Float) {
        if (!spec.swipeable || spec.toggle || editing) return
        val px = widget.left + localX
        val py = widget.top + localY
        val target = profile.controls.firstOrNull { candidate ->
            if (!candidate.swipeable || candidate.toggle || candidate.action == "joystick" || candidate.action == "hotbar") return@firstOrNull false
            val view = widgets[candidate.id] ?: return@firstOrNull false
            view.visibility == View.VISIBLE && px >= view.left && px <= view.right && py >= view.top && py <= view.bottom
        }
        val current = swipeTargets[spec.id]
        if (current?.id == target?.id) return
        if (current != null) handleAction(current, false)
        if (target != null) handleAction(target, true)
        swipeTargets[spec.id] = target
    }

    fun finishSwipe(sourceId: String) {
        if (!swipeTargets.containsKey(sourceId)) return
        val current = swipeTargets.remove(sourceId)
        if (current != null) handleAction(current, false)
    }

    fun updateJoystick(spec: ControlSpec, dx: Float, dy: Float, forwardLocked: Boolean = false) {
        val owner = "touch-${spec.id}-stick"
        val dead = profile.joystickDeadZone
        Held.key(K.GLFW_KEY_W.toInt(), dy < -dead || (spec.joystickForwardLock && forwardLocked), owner)
        Held.key(K.GLFW_KEY_S.toInt(), dy > dead, owner)
        Held.key(K.GLFW_KEY_A.toInt(), dx < -dead, owner)
        Held.key(K.GLFW_KEY_D.toInt(), dx > dead, owner)
        val sprint = profile.joystickSprint && (dy < -0.82f || (spec.joystickForwardLock && forwardLocked)) && hypot(dx, dy) > 0.92f
        Held.key(K.GLFW_KEY_LEFT_CONTROL.toInt(), sprint, owner)
    }

    fun tapHotbar(index: Int) {
        if (index !in 0..8) return
        val now = android.os.SystemClock.uptimeMillis()
        if (profile.doubleTapEnabled && lastHotbarTapIndex == index && now - lastHotbarTapAt in 1L..280L) {
            Held.tap(K.GLFW_KEY_F.toInt(), "hotbar-double-${System.nanoTime()}")
            lastHotbarTapIndex = -1; lastHotbarTapAt = 0L
        } else {
            Held.tap(K.GLFW_KEY_1.toInt() + index, "hotbar-${System.nanoTime()}")
            lastHotbarTapIndex = index; lastHotbarTapAt = now
        }
    }

    fun moveInEditor(spec: ControlSpec, widget: View, left: Float, top: Float) {
        if (!editing || width <= 0 || height <= 0) return
        val maxLeft = (width - widget.width).coerceAtLeast(0).toFloat()
        val maxTop = (height - widget.height).coerceAtLeast(0).toFloat()
        val targetLeft = left.coerceIn(0f, maxLeft)
        val targetTop = top.coerceIn(0f, maxTop)
        spec.x = if (maxLeft == 0f) 0f else targetLeft / maxLeft
        spec.y = if (maxTop == 0f) 0f else targetTop / maxTop
        val lp = widget.layoutParams as FrameLP
        lp.leftMargin = targetLeft.roundToInt(); lp.topMargin = targetTop.roundToInt()
        widget.layoutParams = lp
        if (spec.action.startsWith("drawer:")) refreshDrawerPositions()
        persist()
    }

    fun editControl(spec: ControlSpec) {
        val label = EditText(context).apply { setText(spec.label); hint = "Button label"; singleLine = true }
        val width = SeekBar(context).apply { max = 232; progress = (spec.widthDp - 28).coerceIn(0, max) }
        val height = SeekBar(context).apply { max = 172; progress = (spec.heightDp - 28).coerceIn(0, max) }
        val opacity = SeekBar(context).apply { max = 85; progress = ((spec.opacity - 0.15f) * 100).roundToInt().coerceIn(0, max) }
        val radius = SeekBar(context).apply { max = 100; progress = spec.cornerRadius.roundToInt().coerceIn(0, max) }
        val borderWidth = SeekBar(context).apply { max = 80; progress = (spec.strokeWidthDp * 10f).roundToInt().coerceIn(0, max) }
        val backgroundColor = EditText(context).apply { setText(String.format("#%08X", spec.background)); hint = "Background ARGB"; singleLine = true }
        val outlineColor = EditText(context).apply { setText(String.format("#%08X", spec.outline)); hint = "Outline ARGB"; singleLine = true }
        val combo = TextView(context).apply {
            text = "Edit key combination (up to 4 keys)"
            setTextColor(Color.rgb(85, 115, 205)); setPadding(dp(8), dp(8), dp(8), dp(8))
            setOnClickListener { showKeyComboEditor(spec) }
        }
        val duplicate = TextView(context).apply {
            text = "Duplicate this control"; setTextColor(Color.rgb(85, 115, 205)); setPadding(dp(8), dp(8), dp(8), dp(8))
            setOnClickListener { duplicateControl(spec) }
        }
        val toggle = CheckBox(context).apply { text = "Toggle / latch mode"; isChecked = spec.toggle }
        val game = CheckBox(context).apply { text = "Show during gameplay"; isChecked = spec.displayInGame }
        val menu = CheckBox(context).apply { text = "Show in menus"; isChecked = spec.displayInMenu }
        val swipe = CheckBox(context).apply { text = "Allow swipe-through"; isChecked = spec.swipeable }
        val passthrough = CheckBox(context).apply { text = "Touch pass-through while aiming"; isChecked = spec.passThrough }
        val absoluteJoystick = CheckBox(context).apply { text = "Absolute joystick (follows finger)"; isChecked = spec.joystickAbsolute }
        val forwardLock = CheckBox(context).apply { text = "Joystick forward lock"; isChecked = spec.joystickForwardLock }
        val drawerOrientation = TextView(context).apply {
            text = "Drawer direction: ${spec.drawerOrientation} · Tap to change"
            setTextColor(Color.rgb(85, 115, 205)); setPadding(dp(8), dp(8), dp(8), dp(8))
            visibility = if (spec.action.startsWith("drawer:")) View.VISIBLE else View.GONE
            setOnClickListener {
                val directions = arrayOf("Down", "Left", "Up", "Right", "Free")
                val values = arrayOf("DOWN", "LEFT", "UP", "RIGHT", "FREE")
                AlertDialog.Builder(context).setTitle("Drawer direction")
                    .setSingleChoiceItems(directions, values.indexOf(spec.drawerOrientation).coerceAtLeast(0)) { dialog, which ->
                        spec.drawerOrientation = values[which]
                        text = "Drawer direction: ${spec.drawerOrientation} · Tap to change"
                        dialog.dismiss(); persist(); rebuildWidgets()
                    }.setNegativeButton("Cancel", null).show()
            }
        }
        val action = TextView(context).apply {
            text = "Action: ${actionLabel(spec)}  ·  Tap to change"; setTextColor(Color.rgb(85, 115, 205)); setPadding(dp(8), dp(8), dp(8), dp(8))
            setOnClickListener {
                chooseAction { updated ->
                    spec.action = updated.first; spec.keyCodes = updated.second.toMutableList()
                    text = "Action: ${actionLabel(spec)}  ·  Tap to change"
                    drawerOrientation.visibility = if (spec.action.startsWith("drawer:")) View.VISIBLE else View.GONE
                }
            }
        }
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), dp(4))
            addView(label); addView(action); addView(drawerOrientation); addView(combo); addView(duplicate)
            addView(TextView(context).apply { text = "Width" }); addView(width)
            addView(TextView(context).apply { text = "Height" }); addView(height)
            addView(TextView(context).apply { text = "Opacity" }); addView(opacity)
            addView(TextView(context).apply { text = "Corner radius" }); addView(radius)
            addView(TextView(context).apply { text = "Outline width" }); addView(borderWidth)
            addView(backgroundColor); addView(outlineColor)
            addView(toggle); addView(game); addView(menu); addView(swipe); addView(passthrough)
            if (spec.action == "joystick") { addView(absoluteJoystick); addView(forwardLock) }
        }
        AlertDialog.Builder(context).setTitle("Edit ${spec.id}").setView(content)
            .setNeutralButton("Delete") { _, _ ->
                Held.releaseOwner("touch-${spec.id}")
                profile.controls.removeAll { it.id == spec.id }; widgets.remove(spec.id)?.let { removeView(it) }
                persist(); refreshVisibility()
            }
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                spec.label = label.text.toString().trim().ifBlank { "BTN" }.take(12)
                spec.widthDp = width.progress + 28; spec.heightDp = height.progress + 28
                spec.opacity = (opacity.progress / 100f + 0.15f).coerceIn(0.15f, 1f)
                spec.cornerRadius = radius.progress.toFloat()
                spec.strokeWidthDp = borderWidth.progress / 10f
                parseColorField(backgroundColor.text.toString())?.let { spec.background = it }
                parseColorField(outlineColor.text.toString())?.let { spec.outline = it }
                spec.toggle = toggle.isChecked; spec.displayInGame = game.isChecked; spec.displayInMenu = menu.isChecked
                spec.swipeable = swipe.isChecked; spec.passThrough = passthrough.isChecked
                spec.joystickAbsolute = absoluteJoystick.isChecked; spec.joystickForwardLock = forwardLock.isChecked
                persist(); rebuildWidgets()
            }.show()
    }

    private fun parseColorField(raw: String): Int? = runCatching {
        val value = raw.trim().removePrefix("#")
        when (value.length) {
            6 -> (0xFF000000L or value.toLong(16)).toInt()
            8 -> value.toLong(16).toInt()
            else -> null
        }
    }.getOrNull()

    private fun duplicateControl(spec: ControlSpec) {
        val clone = spec.copy(
            id = "${spec.id}-copy-${System.currentTimeMillis()}",
            x = (spec.x + 0.04f).coerceAtMost(1f),
            y = (spec.y + 0.04f).coerceAtMost(1f),
            keyCodes = spec.keyCodes.toMutableList()
        )
        profile.controls.add(clone)
        persist(); rebuildWidgets(); editing = true; refreshVisibility()
        Toast.makeText(context, "Control duplicated", Toast.LENGTH_SHORT).show()
    }

    private fun showKeyComboEditor(spec: ControlSpec) {
        val options = mutableListOf<Pair<String, Int>>()
        options += "Space" to K.GLFW_KEY_SPACE.toInt()
        options += "Enter" to K.GLFW_KEY_ENTER.toInt()
        options += "Escape" to K.GLFW_KEY_ESCAPE.toInt()
        options += "Tab" to K.GLFW_KEY_TAB.toInt()
        options += "Shift" to K.GLFW_KEY_LEFT_SHIFT.toInt()
        options += "Ctrl" to K.GLFW_KEY_LEFT_CONTROL.toInt()
        options += "Alt" to K.GLFW_KEY_LEFT_ALT.toInt()
        options += "Backspace" to K.GLFW_KEY_BACKSPACE.toInt()
        options += "Up" to K.GLFW_KEY_UP.toInt(); options += "Down" to K.GLFW_KEY_DOWN.toInt()
        options += "Left" to K.GLFW_KEY_LEFT.toInt(); options += "Right" to K.GLFW_KEY_RIGHT.toInt()
        for (code in K.GLFW_KEY_0.toInt()..K.GLFW_KEY_9.toInt()) options += code.toChar().toString() to code
        for (code in K.GLFW_KEY_A.toInt()..K.GLFW_KEY_Z.toInt()) options += code.toChar().toString() to code
        for (code in K.GLFW_KEY_F1.toInt()..K.GLFW_KEY_F12.toInt()) options += "F${code - K.GLFW_KEY_F1.toInt() + 1}" to code
        val checked = BooleanArray(options.size) { i -> options[i].second in spec.keyCodes }
        AlertDialog.Builder(context).setTitle("Key combination (max 4)")
            .setMultiChoiceItems(options.map { it.first }.toTypedArray(), checked) { dialog, which, isChecked ->
                if (isChecked && checked.count { it } > 4) {
                    checked[which] = false
                    (dialog as? AlertDialog)?.listView?.setItemChecked(which, false)
                    Toast.makeText(context, "Maksimal 4 tombol per kontrol", Toast.LENGTH_SHORT).show()
                } else checked[which] = isChecked
            }
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Use combo") { _, _ ->
                spec.keyCodes = options.indices.filter { checked[it] }.map { options[it].second }.toMutableList()
                if (spec.keyCodes.isNotEmpty()) spec.action = "key"
                persist(); rebuildWidgets()
            }.show()
    }

    private fun actionLabel(spec: ControlSpec): String = when (spec.action) {
        "key" -> spec.keyCodes.joinToString(" + ") { keyName(it) }.ifBlank { "No key" }
        "mouse-left" -> "Mouse left click"
        "mouse-right" -> "Mouse right click / use"
        "mouse-middle" -> "Mouse middle click"
        "joystick" -> "Movement joystick"
        "hotbar" -> "Hotbar selector"
        "keyboard" -> "Open touch keyboard"
        "settings" -> "Control settings"
        "toggle-mouse" -> "Toggle virtual mouse"
        "toggle-controls" -> "Show / hide controls"
        "scroll-up" -> "Mouse wheel up"
        "scroll-down" -> "Mouse wheel down"
        "sprint" -> "Sprint / Ctrl"
        "menu" -> "Game menu / Escape"
        else -> if (spec.action.startsWith("drawer:")) "Toggle drawer ${spec.action.substringAfter(':')}" else spec.action
    }

    private fun keyName(code: Int): String = when (code) {
        K.GLFW_KEY_SPACE.toInt() -> "SPACE"; K.GLFW_KEY_LEFT_SHIFT.toInt() -> "LSHIFT"
        K.GLFW_KEY_LEFT_CONTROL.toInt() -> "LCTRL"; K.GLFW_KEY_LEFT_ALT.toInt() -> "LALT"
        K.GLFW_KEY_ESCAPE.toInt() -> "ESC"; K.GLFW_KEY_ENTER.toInt() -> "ENTER"
        K.GLFW_KEY_TAB.toInt() -> "TAB"; K.GLFW_KEY_BACKSPACE.toInt() -> "BACKSPACE"
        in 48..57, in 65..90 -> code.toChar().toString()
        in 290..301 -> "F${code - 289}"
        else -> "KEY $code"
    }

    private fun chooseAction(done: (Pair<String, List<Int>>) -> Unit) {
        val keys = listOf(
            "Space / Jump" to ("key" to listOf(K.GLFW_KEY_SPACE.toInt())),
            "Shift / Sneak" to ("key" to listOf(K.GLFW_KEY_LEFT_SHIFT.toInt())),
            "Ctrl / Sprint" to ("key" to listOf(K.GLFW_KEY_LEFT_CONTROL.toInt())),
            "W" to ("key" to listOf(K.GLFW_KEY_W.toInt())), "A" to ("key" to listOf(K.GLFW_KEY_A.toInt())),
            "S" to ("key" to listOf(K.GLFW_KEY_S.toInt())), "D" to ("key" to listOf(K.GLFW_KEY_D.toInt())),
            "E / Inventory" to ("key" to listOf(K.GLFW_KEY_E.toInt())), "Q / Drop" to ("key" to listOf(K.GLFW_KEY_Q.toInt())),
            "F / Swap hands" to ("key" to listOf(K.GLFW_KEY_F.toInt())), "Escape" to ("key" to listOf(K.GLFW_KEY_ESCAPE.toInt())),
            "F1" to ("key" to listOf(K.GLFW_KEY_F1.toInt())), "F3" to ("key" to listOf(K.GLFW_KEY_F3.toInt())),
            "F5" to ("key" to listOf(K.GLFW_KEY_F5.toInt())), "F11" to ("key" to listOf(K.GLFW_KEY_F11.toInt())),
            "Mouse left" to ("mouse-left" to emptyList()), "Mouse right / use" to ("mouse-right" to emptyList()),
            "Mouse middle" to ("mouse-middle" to emptyList()), "Keyboard" to ("keyboard" to emptyList()),
            "Virtual mouse" to ("toggle-mouse" to emptyList()), "Show/hide controls" to ("toggle-controls" to emptyList()),
            "Wheel up" to ("scroll-up" to emptyList()), "Wheel down" to ("scroll-down" to emptyList()),
            "Movement joystick" to ("joystick" to emptyList()), "Hotbar 1–9" to ("hotbar" to emptyList()),
            "Game menu" to ("menu" to emptyList()), "Extras drawer" to ("drawer:extras" to emptyList())
        )
        AlertDialog.Builder(context).setTitle("Choose control action").setItems(keys.map { it.first }.toTypedArray()) { _, index -> done(keys[index].second) }.show()
    }

    private fun showControlPanel() {
        val items = arrayOf(
            if (editing) "Finish layout editing" else "Edit layout (drag controls; tap to edit)",
            "Mouse, gestures and gyro settings",
            "Change layout preset",
            "Add a control button",
            "Export layout JSON",
            "Import layout JSON",
            "Reset layout to default",
            if (profile.controlsVisible) "Hide on-screen controls" else "Show on-screen controls",
            if (profile.pointerVisible) "Hide virtual mouse pointer" else "Show virtual mouse pointer"
        )
        AlertDialog.Builder(context).setTitle("Sally control system")
            .setItems(items) { _, which -> when (which) {
                0 -> {
                    editing = !editing
                    refreshVisibility()
                    Toast.makeText(context, if (editing) "Layout editor aktif: geser tombol, tap tombol untuk mengedit." else "Layout tersimpan.", Toast.LENGTH_SHORT).show()
                }
                1 -> showMouseSettings()
                2 -> choosePreset()
                3 -> addControl()
                4 -> exportProfile()
                5 -> importProfilePicker()
                6 -> AlertDialog.Builder(context).setTitle("Reset layout?").setMessage("Layout saat ini akan diganti dengan layout default.")
                    .setNegativeButton("Batal", null).setPositiveButton("Reset") { _, _ -> profile = ControlProfile.default(); activeGroups.clear(); persist(); rebuildWidgets(); applyProfileToGyro() }.show()
                7 -> { profile.controlsVisible = !profile.controlsVisible; persist(); refreshVisibility() }
                8 -> { profile.pointerVisible = !profile.pointerVisible; mouseGestures.setPointerVisible(profile.pointerVisible); persist(); refreshVisibility() }
            } }.show()
    }

    private fun choosePreset() {
        val names = arrayOf("Minecraft Touch", "Minimal", "Mouse & Keyboard", "Compact Touch")
        AlertDialog.Builder(context).setTitle("Layout preset").setItems(names) { _, i ->
            profile = when (names[i]) {
                "Minimal" -> ControlProfile.preset("Minimal")
                "Mouse & Keyboard" -> ControlProfile.preset("Mouse & Keyboard")
                "Compact Touch" -> ControlProfile.preset("Compact Touch")
                else -> ControlProfile.default()
            }
            activeGroups.clear(); editing = false; persist(); rebuildWidgets(); applyProfileToGyro()
        }.show()
    }

    private fun addControl() {
        chooseAction { chosen ->
            val id = "custom-${System.currentTimeMillis()}"
            val spec = ControlSpec(id, when (chosen.first) {
                "key" -> chosen.second.firstOrNull()?.let { keyName(it) } ?: "KEY"
                "mouse-left" -> "LMB"; "mouse-right" -> "RMB"; "mouse-middle" -> "MMB"
                "keyboard" -> "KB"; "toggle-mouse" -> "MOUSE"; "joystick" -> "MOVE"; "hotbar" -> "HOTBAR"
                else -> "BTN"
            }, chosen.first, 0.47f, 0.4f, if (chosen.first == "hotbar") 320 else 54,
                if (chosen.first == "hotbar") 38 else 54, keyCodes = chosen.second.toMutableList(),
                displayInGame = true, displayInMenu = false)
            profile.controls.add(spec); editing = true; persist(); rebuildWidgets()
        }
    }

    private fun showMouseSettings() {
        val speed = SeekBar(context).apply { max = 480; progress = ((profile.mouseSpeed - 0.2f) * 100).roundToInt().coerceIn(0, max) }
        val camera = SeekBar(context).apply { max = 490; progress = ((profile.cameraSensitivity - 0.1f) * 100).roundToInt().coerceIn(0, max) }
        val scroll = SeekBar(context).apply { max = 380; progress = ((profile.scrollSensitivity - 0.2f) * 100).roundToInt().coerceIn(0, max) }
        val long = SeekBar(context).apply { max = 820; progress = profile.longPressMs - 180 }
        val pointerScale = SeekBar(context).apply { max = 250; progress = ((profile.pointerScale - 0.5f) * 100f).roundToInt().coerceIn(0, max) }
        val gyroSens = SeekBar(context).apply { max = 390; progress = ((profile.gyroSensitivity - 0.1f) * 100).roundToInt().coerceIn(0, max) }
        val direct = CheckBox(context).apply { text = "Direct touch in game menus"; isChecked = profile.directTouch }
        val touchpad = CheckBox(context).apply { text = "Virtual mouse / touchpad enabled"; isChecked = profile.touchpadEnabled }
        val gestures = CheckBox(context).apply { text = "Enable long-press / two-finger gestures"; isChecked = profile.gesturesEnabled }
        val double = CheckBox(context).apply { text = "Double-tap shortcut (F)"; isChecked = profile.doubleTapEnabled }
        val hotbar = CheckBox(context).apply { text = "Show touch hotbar"; isChecked = profile.hotbarEnabled }
        val gyroOn = CheckBox(context).apply { text = "Enable gyro aim while playing"; isChecked = profile.gyroEnabled }
        val invertX = CheckBox(context).apply { text = "Invert gyro X"; isChecked = profile.gyroInvertX }
        val invertY = CheckBox(context).apply { text = "Invert gyro Y"; isChecked = profile.gyroInvertY }
        val dead = SeekBar(context).apply { max = 70; progress = ((profile.joystickDeadZone - 0.1f) * 100).roundToInt().coerceIn(0, max) }
        val sprint = CheckBox(context).apply { text = "Joystick forward edge = sprint"; isChecked = profile.joystickSprint }
        fun row(title: String, bar: SeekBar) = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL; addView(TextView(context).apply { text = title }); addView(bar)
        }
        val body = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), dp(4))
            addView(row("Mouse pointer speed: ${(profile.mouseSpeed * 100).roundToInt()}%", speed))
            addView(row("Camera sensitivity: ${(profile.cameraSensitivity * 100).roundToInt()}%", camera))
            addView(row("Scroll sensitivity: ${(profile.scrollSensitivity * 100).roundToInt()}%", scroll))
            addView(row("Long press: ${profile.longPressMs} ms", long))
            addView(row("Pointer size: ${(profile.pointerScale * 100).roundToInt()}%", pointerScale))
            addView(row("Gyro sensitivity", gyroSens)); addView(row("Joystick dead zone", dead))
            addView(direct); addView(touchpad); addView(gestures); addView(double); addView(hotbar); addView(gyroOn); addView(invertX); addView(invertY); addView(sprint)
        }
        AlertDialog.Builder(context).setTitle("Mouse & control settings").setView(android.widget.ScrollView(context).apply { addView(body) })
            .setNegativeButton("Cancel", null).setPositiveButton("Save") { _, _ ->
                profile.mouseSpeed = speed.progress / 100f + 0.2f
                profile.cameraSensitivity = camera.progress / 100f + 0.1f
                profile.scrollSensitivity = scroll.progress / 100f + 0.2f
                profile.longPressMs = long.progress + 180
                profile.pointerScale = pointerScale.progress / 100f + 0.5f
                profile.gyroSensitivity = gyroSens.progress / 100f + 0.1f
                profile.joystickDeadZone = dead.progress / 100f + 0.1f
                profile.directTouch = direct.isChecked; profile.touchpadEnabled = touchpad.isChecked
                profile.gesturesEnabled = gestures.isChecked
                profile.doubleTapEnabled = double.isChecked; profile.hotbarEnabled = hotbar.isChecked
                profile.gyroEnabled = gyroOn.isChecked; profile.gyroInvertX = invertX.isChecked; profile.gyroInvertY = invertY.isChecked
                profile.joystickSprint = sprint.isChecked
                persist(); mouseGestures.setPointerVisible(profile.pointerVisible); applyProfileToGyro(); refreshVisibility()
            }.show()
    }

    private fun exportProfile() {
        runCatching {
            val dir = context.getExternalFilesDir("controls") ?: context.filesDir
            if (!dir.exists()) dir.mkdirs()
            val out = java.io.File(dir, "sally-controls.json")
            out.writeText(profile.toJson().toString(2))
            Toast.makeText(context, "Layout diekspor: ${out.absolutePath}", Toast.LENGTH_LONG).show()
        }.onFailure { Toast.makeText(context, "Gagal ekspor: ${it.message}", Toast.LENGTH_LONG).show() }
    }

    private fun importProfilePicker() {
        val activity = context as? Activity ?: return
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE); type = "application/json"
        }
        runCatching { activity.startActivityForResult(intent, REQUEST_IMPORT) }
            .onFailure { Toast.makeText(context, "Pemilih file tidak tersedia: ${it.message}", Toast.LENGTH_LONG).show() }
    }

    fun importProfile(uri: Uri) {
        runCatching {
            val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: error("File tidak bisa dibaca")
            val imported = ControlProfile.fromJson(JSONObject(raw), width.coerceAtLeast(1), height.coerceAtLeast(1), density)
            profile = imported; activeGroups.clear(); editing = false
            persist(); rebuildWidgets(); applyProfileToGyro()
            Toast.makeText(context, "Layout Sally berhasil diimpor", Toast.LENGTH_SHORT).show()
        }.onFailure { Toast.makeText(context, "Impor layout gagal: ${it.message}", Toast.LENGTH_LONG).show() }
    }

    private fun persist() { profile.save(context) }

    private inner class ControlWidget(context: Context, val spec: ControlSpec, private val host: TouchControls) : View(context) {
        private val bg = Paint(Paint.ANTI_ALIAS_FLAG)
        private val border = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = dp(1).toFloat() }
        private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textAlign = Paint.Align.CENTER; typeface = android.graphics.Typeface.DEFAULT_BOLD }
        private var active = false
        private var toggleState = false
        private var pointerId = -1
        private var downX = 0f; private var downY = 0f; private var downRawX = 0f; private var downRawY = 0f
        private var originLeft = 0f; private var originTop = 0f
        private var moved = false; private var editingOnDown = false
        private var settingsActivatedInEditor = false
        private var passThroughActive = false
        private var pointerOutOfBounds = false
        private var stickX = 0f; private var stickY = 0f
        private var stickBaseX = 0f; private var stickBaseY = 0f
        private var forwardLockLatched = false
        private var hotbarIndex = 0
        private val hotbarDropRunnable = object : Runnable {
            override fun run() {
                if (pointerId != -1 && spec.action == "hotbar" && !host.editing && CB.isGrabbing()) {
                    Held.tap(K.GLFW_KEY_Q.toInt(), "hotbar-drop-${System.nanoTime()}")
                    handler.postDelayed(this, 250L)
                }
            }
        }
        private val editRunnable = Runnable {
            if (editingOnDown && !moved && pointerId != -1) {
                moved = true
                host.editControl(spec)
            }
        }
        private val owner get() = "touch-${spec.id}"

        init { isClickable = true; importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES; contentDescription = spec.label.ifBlank { spec.id } }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val a = if (host.editing) 1f else spec.opacity
            bg.color = spec.background
            bg.alpha = (Color.alpha(spec.background) * a).roundToInt().coerceIn(0, 255)
            border.color = spec.outline; border.alpha = (Color.alpha(spec.outline) * a).roundToInt().coerceIn(0, 255)
            val w = width.toFloat(); val h = height.toFloat()
            border.strokeWidth = spec.strokeWidthDp * density
            if (spec.action == "joystick") {
                val r = minOf(w, h) / 2f - dp(2)
                val baseX = if (spec.joystickAbsolute && active) stickBaseX else w / 2f
                val baseY = if (spec.joystickAbsolute && active) stickBaseY else h / 2f
                canvas.drawCircle(w / 2, h / 2, r, bg); canvas.drawCircle(w / 2, h / 2, r, border)
                val knobR = r * 0.31f
                canvas.drawCircle(baseX + stickX * r * 0.62f, baseY + stickY * r * 0.62f, knobR, bg.apply { color = if (active) 0x99FFFFFF.toInt() else 0x66FFFFFF })
                canvas.drawCircle(baseX + stickX * r * 0.62f, baseY + stickY * r * 0.62f, knobR, border.apply { color = 0xBBFFFFFF.toInt() })
                bg.color = spec.background; border.color = spec.outline
            } else if (spec.action == "hotbar") {
                val gap = dp(2).toFloat(); val slotW = (w - gap * 8) / 9f
                for (i in 0..8) {
                    val left = i * (slotW + gap)
                    val rect = RectF(left, dp(1).toFloat(), left + slotW, h - dp(1))
                    bg.color = if (i == hotbarIndex) 0xAAFFFFFF.toInt() else spec.background
                    canvas.drawRoundRect(rect, dp(4).toFloat(), dp(4).toFloat(), bg)
                    canvas.drawRoundRect(rect, dp(4).toFloat(), dp(4).toFloat(), border)
                    text.textSize = minOf(h * 0.54f, dp(17).toFloat()); text.color = if (i == hotbarIndex) Color.BLACK else Color.WHITE
                    canvas.drawText((i + 1).toString(), rect.centerX(), rect.centerY() - (text.ascent() + text.descent()) / 2, text)
                }
                bg.color = spec.background; text.color = Color.WHITE
            } else {
                val roundRect = RectF(dp(1).toFloat(), dp(1).toFloat(), w - dp(1), h - dp(1))
                val corner = (minOf(w, h) * (spec.cornerRadius / 100f) * 0.5f).coerceAtLeast(0f)
                canvas.drawRoundRect(roundRect, corner, corner, bg)
                canvas.drawRoundRect(roundRect, corner, corner, border)
                if (host.editing) {
                    bg.color = 0x553F83F8; canvas.drawRoundRect(roundRect, dp(9).toFloat(), dp(9).toFloat(), bg)
                    bg.color = spec.background
                }
                text.color = Color.WHITE
                text.textSize = minOf(h * if (spec.label.length > 4) 0.23f else 0.33f, dp(16).toFloat()).coerceAtLeast(dp(9).toFloat())
                canvas.drawText(spec.label, w / 2, h / 2 - (text.ascent() + text.descent()) / 2, text)
                if (host.editing) {
                    text.textSize = dp(9).toFloat(); text.color = 0xFFFFFFFF.toInt()
                    canvas.drawText("↔", w - dp(8).toFloat(), dp(10).toFloat(), text)
                }
            }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    pointerId = event.getPointerId(0); downX = event.x; downY = event.y
                    downRawX = event.rawX; downRawY = event.rawY; moved = false; editingOnDown = host.editing
                    settingsActivatedInEditor = host.editing && spec.action == "settings"
                    originLeft = (layoutParams as? FrameLP)?.leftMargin?.toFloat() ?: left.toFloat()
                    originTop = (layoutParams as? FrameLP)?.topMargin?.toFloat() ?: top.toFloat()
                    passThroughActive = !host.editing && spec.passThrough && CB.isGrabbing()
                    pointerOutOfBounds = false
                    if (settingsActivatedInEditor) {
                        host.handleAction(spec, true)
                    } else if (!host.editing) {
                        active = true
                        if (spec.toggle) { toggleState = !toggleState; host.handleAction(spec, true, toggleState) }
                        else if (spec.action != "joystick" && spec.action != "hotbar") host.handleAction(spec, true)
                        if (spec.action == "joystick") {
                            if (spec.joystickAbsolute) { stickBaseX = event.x; stickBaseY = event.y }
                            else { stickBaseX = width / 2f; stickBaseY = height / 2f }
                            updateStick(event.x, event.y)
                        }
                        if (spec.action == "hotbar") {
                            hotbarIndex = ((event.x / width.coerceAtLeast(1)) * 9).toInt().coerceIn(0, 8)
                            host.tapHotbar(hotbarIndex)
                            handler.removeCallbacks(hotbarDropRunnable)
                            handler.postDelayed(hotbarDropRunnable, profile.longPressMs.toLong())
                            invalidate()
                        }
                        if (spec.swipeable && !spec.toggle) host.beginSwipe(spec)
                        if (passThroughActive) host.dispatchPassThrough(this, event)
                    } else {
                        handler.postDelayed(editRunnable, 600L)
                    }
                    invalidate(); return true
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    if (passThroughActive) host.dispatchPassThrough(this, event)
                    return true
                }
                MotionEvent.ACTION_POINTER_UP -> {
                    if (passThroughActive) host.dispatchPassThrough(this, event)
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val idx = event.findPointerIndex(pointerId)
                    if (idx < 0) return true
                    if (host.editing) {
                        val dx = event.rawX - downRawX; val dy = event.rawY - downRawY
                        if (!moved && hypot(dx, dy) > slop) { moved = true; handler.removeCallbacks(editRunnable) }
                        if (moved) host.moveInEditor(spec, this, originLeft + dx, originTop + dy)
                    } else if (spec.action == "joystick") updateStick(event.getX(idx), event.getY(idx))
                    else if (spec.action == "hotbar") {
                        val nextIndex = ((event.getX(idx) / width.coerceAtLeast(1)) * 9).toInt().coerceIn(0, 8)
                        if (nextIndex != hotbarIndex) {
                            hotbarIndex = nextIndex
                            host.tapHotbar(hotbarIndex)
                            handler.removeCallbacks(hotbarDropRunnable)
                            handler.postDelayed(hotbarDropRunnable, profile.longPressMs.toLong())
                        }
                        invalidate()
                    }
                    if (!host.editing && spec.swipeable) host.updateSwipe(spec, this, event.getX(idx), event.getY(idx))
                    if (passThroughActive) host.dispatchPassThrough(this, event)
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(editRunnable)
                    handler.removeCallbacks(hotbarDropRunnable)
                    if (passThroughActive) { host.dispatchPassThrough(this, event); passThroughActive = false }
                    if (host.editing) {
                        if (!settingsActivatedInEditor && !moved && event.actionMasked == MotionEvent.ACTION_UP && !editRunnableTriggered()) host.editControl(spec)
                    } else {
                        host.finishSwipe(spec.id)
                        if (spec.action == "joystick") releaseStick()
                        else if (spec.action == "hotbar") {
                            // The active slot is selected on ACTION_DOWN and as the finger crosses slots.
                        } else if (!spec.toggle) host.handleAction(spec, false)
                        active = spec.toggle && toggleState
                    }
                    pointerId = -1; pointerOutOfBounds = false; invalidate(); return true
                }
            }
            return true
        }

        private var editorRunnableDidRun = false
        private fun editRunnableTriggered(): Boolean = editorRunnableDidRun.also { editorRunnableDidRun = false }

        private fun updateStick(x: Float, y: Float) {
            val r = minOf(width, height) / 2f
            val maxR = (r * 0.62f).coerceAtLeast(1f)
            val baseX = if (spec.joystickAbsolute) stickBaseX else width / 2f
            val baseY = if (spec.joystickAbsolute) stickBaseY else height / 2f
            var dx = x - baseX; var dy = y - baseY
            val length = hypot(dx, dy)
            if (length > maxR) { dx = dx / length * maxR; dy = dy / length * maxR }
            stickX = (dx / maxR).coerceIn(-1f, 1f); stickY = (dy / maxR).coerceIn(-1f, 1f); active = true
            if (spec.joystickForwardLock) {
                if (stickY < -0.82f) forwardLockLatched = true
                else if (stickY > 0.25f) forwardLockLatched = false
            }
            host.updateJoystick(spec, stickX, stickY, forwardLockLatched); invalidate()
        }

        private fun releaseStick() {
            Held.releaseOwner("touch-${spec.id}-stick")
            stickX = 0f; stickY = 0f; active = false; forwardLockLatched = false
            stickBaseX = width / 2f; stickBaseY = height / 2f; invalidate()
        }

        fun cancelTouch() {
            handler.removeCallbacks(editRunnable); handler.removeCallbacks(hotbarDropRunnable)
            Held.releaseOwner(owner); Held.releaseOwner("touch-${spec.id}-stick")
            if (passThroughActive) host.finishSwipe(spec.id)
            passThroughActive = false; pointerOutOfBounds = false
            pointerId = -1; active = false; toggleState = false; stickX = 0f; stickY = 0f; forwardLockLatched = false
            stickBaseX = width / 2f; stickBaseY = height / 2f
            invalidate()
        }
    }

    companion object { const val REQUEST_IMPORT = 4782 }
}
