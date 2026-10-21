package com.sally.ember.game

import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import net.kdt.pojavlaunch.LwjglGlfwKeycode as K
import org.lwjgl.glfw.CallbackBridge as CB

/**
 * Physical mouse / trackpad / stylus support (ported from Zalith's MinecraftGLSurface.dispatchGenericMotionEvent
 * and AndroidPointerCapture).
 *
 * Without this, a hovering mouse only produces ACTION_HOVER_MOVE generic events, which nothing listened to, so the game never
 * saw the cursor move and buttons never highlighted.
 *
 *  - Menus (cursor free): hover = absolute cursor position, buttons and wheel are forwarded.
 *  - In game (cursor grabbed): Android pointer capture gives relative deltas that steer the camera.
 *  - Pointer capture also feeds the launcher-drawn arrow in menus (the system cursor is hidden while captured).
 */
internal class MouseInput(private val host: View, private val enabled: () -> Boolean) : View.OnCapturedPointerListener {
    private val d = host.resources.displayMetrics.density
    private var wantCapture = false
    private var lastDevice = -1
    private var relativeAxes = false
    private var padX = 0f; private var padY = 0f; private var padTracking = false
    private var scrollAcc = 0f

    init { host.setOnCapturedPointerListener(this) }

    private fun pointerIndex(e: MotionEvent): Int {
        for (i in 0 until e.pointerCount) {
            val t = e.getToolType(i)
            if (t == MotionEvent.TOOL_TYPE_MOUSE || t == MotionEvent.TOOL_TYPE_STYLUS) return i
        }
        return -1
    }

    fun isMouseEvent(e: MotionEvent) = pointerIndex(e) >= 0

    /** Asks Android to capture the pointer (needs window focus; retried from [onWindowFocus]). */
    fun ensureCapture() {
        wantCapture = true
        if (!host.hasWindowFocus()) host.requestFocus() else if (!host.hasPointerCapture()) host.requestPointerCapture()
    }

    fun onWindowFocus(hasFocus: Boolean) { if (hasFocus && wantCapture) host.requestPointerCapture() }

    fun release() { wantCapture = false; if (host.hasPointerCapture()) host.releasePointerCapture() }

    private fun placeAbsolute(x: Float, y: Float) {
        // Android MotionEvent coordinates are in host-view space; GLFW expects native surface pixels.
        val sx = if (host.width > 0 && CB.windowWidth > 0) CB.windowWidth.toFloat() / host.width else 1f
        val sy = if (host.height > 0 && CB.windowHeight > 0) CB.windowHeight.toFloat() / host.height else 1f
        val w = (CB.windowWidth - 1).coerceAtLeast(0).toFloat()
        val h = (CB.windowHeight - 1).coerceAtLeast(0).toFloat()
        CB.sendCursorPos((x * sx).coerceIn(0f, w), (y * sy).coerceIn(0f, h))
    }

    private fun glfwButton(androidButton: Int): Int = when (androidButton) {
        MotionEvent.BUTTON_PRIMARY -> K.GLFW_MOUSE_BUTTON_LEFT.toInt()
        MotionEvent.BUTTON_SECONDARY -> K.GLFW_MOUSE_BUTTON_RIGHT.toInt()
        MotionEvent.BUTTON_TERTIARY -> K.GLFW_MOUSE_BUTTON_MIDDLE.toInt()
        else -> -1
    }

    private fun button(e: MotionEvent, down: Boolean): Boolean {
        val b = glfwButton(e.actionButton)
        if (b < 0) return false
        Held.mouse(b, down)
        return true
    }

    /** Hover / wheel / button events. Returns true when consumed. Call from Activity.dispatchGenericMotionEvent. */
    fun onGeneric(e: MotionEvent): Boolean {
        if (!enabled()) return false
        val i = pointerIndex(e)
        if (i < 0) return false
        if (CB.isGrabbing()) ensureCapture()
        return when (e.actionMasked) {
            MotionEvent.ACTION_HOVER_ENTER, MotionEvent.ACTION_HOVER_MOVE -> {
                if (!CB.isGrabbing()) placeAbsolute(e.getX(i), e.getY(i)) // grabbed: the camera moves via pointer capture instead
                true
            }
            MotionEvent.ACTION_HOVER_EXIT -> true
            MotionEvent.ACTION_SCROLL -> {
                CB.sendScroll(e.getAxisValue(MotionEvent.AXIS_HSCROLL).toDouble(), e.getAxisValue(MotionEvent.AXIS_VSCROLL).toDouble()); true
            }
            MotionEvent.ACTION_BUTTON_PRESS -> button(e, true)
            MotionEvent.ACTION_BUTTON_RELEASE -> button(e, false)
            else -> false
        }
    }

    /** Touch-class events from a mouse (press-and-drag). Position only: the button itself arrives as ACTION_BUTTON_PRESS. */
    fun onTouch(e: MotionEvent) {
        val i = pointerIndex(e)
        if (i < 0) return
        if (CB.isGrabbing()) { ensureCapture(); return }
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> placeAbsolute(e.getX(i), e.getY(i))
        }
    }

    /** Relative mouse deltas (or absolute trackpad data) while the pointer is captured. */
    override fun onCapturedPointer(view: View, e: MotionEvent): Boolean {
        if (!enabled()) return false
        e.device?.let { dev ->
            if (dev.id != lastDevice) {
                lastDevice = dev.id; padTracking = false
                relativeAxes = dev.getMotionRange(MotionEvent.AXIS_RELATIVE_X) != null && dev.getMotionRange(MotionEvent.AXIS_RELATIVE_Y) != null
            }
        }
        var dx = 0f; var dy = 0f
        if (e.source and InputDevice.SOURCE_CLASS_TRACKBALL != 0) {
            if (relativeAxes) { dx = e.getAxisValue(MotionEvent.AXIS_RELATIVE_X); dy = e.getAxisValue(MotionEvent.AXIS_RELATIVE_Y) }
            else { dx = e.x; dy = e.y }
        } else {
            // Absolute touchpad: track it like a touchscreen.
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { padX = e.x; padY = e.y; padTracking = true }
                MotionEvent.ACTION_MOVE -> if (padTracking) { dx = (e.x - padX) * 1.5f; dy = (e.y - padY) * 1.5f; padX = e.x; padY = e.y }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> padTracking = false
            }
        }

        if (CB.isGrabbing()) {
            val sx = if (host.width > 0 && CB.windowWidth > 0) CB.windowWidth.toFloat() / host.width else 1f
            val sy = if (host.height > 0 && CB.windowHeight > 0) CB.windowHeight.toFloat() / host.height else 1f
            if (dx != 0f || dy != 0f) CB.sendCursorPos(CB.mouseX + dx * sx, CB.mouseY + dy * sy)
        } else if (e.pointerCount >= 2) {
            // Two-finger touchpad swipe = scroll.
            scrollAcc += dy
            val step = 24f * d
            while (scrollAcc > step) { CB.sendScroll(0.0, 1.0); scrollAcc -= step }
            while (scrollAcc < -step) { CB.sendScroll(0.0, -1.0); scrollAcc += step }
        } else {
            scrollAcc = 0f
            CB.syncCursorFromGame() // the game may have recentred the cursor itself
            if (dx != 0f || dy != 0f) placeAbsolute(CB.mouseX + dx, CB.mouseY + dy)
        }

        return when (e.actionMasked) {
            MotionEvent.ACTION_BUTTON_PRESS -> button(e, true)
            MotionEvent.ACTION_BUTTON_RELEASE -> button(e, false)
            MotionEvent.ACTION_SCROLL -> {
                CB.sendScroll(e.getAxisValue(MotionEvent.AXIS_HSCROLL).toDouble(), e.getAxisValue(MotionEvent.AXIS_VSCROLL).toDouble()); true
            }
            else -> true
        }
    }
}
