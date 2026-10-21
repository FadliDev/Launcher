package com.sally.ember.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import kotlin.math.abs
import kotlin.math.hypot
import net.kdt.pojavlaunch.LwjglGlfwKeycode as K
import org.lwjgl.glfw.CallbackBridge as CB

/** Zalith-inspired split input processor: relative mouse-look in-game, pointer/touchpad gestures in GUIs. */
internal class MouseGestureView(
    context: Context,
    private val profile: () -> ControlProfile
) : View(context) {
    private val density = resources.displayMetrics.density
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private val handler = Handler(Looper.getMainLooper())
    private var activePointer = -1
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var downTime = 0L
    private var moved = false
    private var multiTouch = false
    private var scrollMoved = false
    private var leftHeld = false
    private var secondDownAt = 0L
    private var secondDownX = 0f
    private var secondDownY = 0f
    private var scrollAccumulator = 0f
    private var lastAverageY = 0f
    private var longPressTriggered = false
    private var pointerVisible = true
    private var suppressClickForGesture = false
    private val pointer = Path().apply {
        moveTo(0f, 0f); lineTo(0f, 24f * density); lineTo(6f * density, 18f * density)
        lineTo(11f * density, 28f * density); lineTo(15f * density, 26f * density)
        lineTo(10f * density, 16f * density); lineTo(19f * density, 16f * density); close()
    }
    private val pointerFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val pointerStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 1.5f * density
    }

    private val longPress = Runnable {
        val p = profile()
        if (activePointer != -1 && !moved && !multiTouch && p.gesturesEnabled) {
            longPressTriggered = true
            leftHeld = true
            Held.mouse(K.GLFW_MOUSE_BUTTON_LEFT.toInt(), true, "gesture-left")
        }
    }

    fun invalidatePointer() = invalidate()

    /** Mirrors a control's touch stream into the mouse gesture processor, like Zalith pass-through. */
    fun dispatchPassThroughTouchEvent(event: MotionEvent) {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) suppressClickForGesture = true
        onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        val p = profile()
        if (CB.isGrabbing() || !pointerVisible || !p.pointerVisible || !p.touchpadEnabled) return
        val maxX = (CB.windowWidth - 1).coerceAtLeast(0).toFloat()
        val maxY = (CB.windowHeight - 1).coerceAtLeast(0).toFloat()
        val px = CB.mouseX.coerceIn(0f, maxX)
        val py = CB.mouseY.coerceIn(0f, maxY)
        canvas.save(); canvas.translate(px, py); canvas.scale(p.pointerScale, p.pointerScale)
        canvas.drawPath(pointer, pointerFill); canvas.drawPath(pointer, pointerStroke)
        canvas.restore()
    }

    fun setPointerVisible(visible: Boolean) {
        pointerVisible = visible
        invalidate()
    }

    fun syncCursor() {
        if (CB.isGrabbing()) return
        CB.sendCursorPos(CB.mouseX.coerceIn(0f, (CB.windowWidth - 1).coerceAtLeast(0).toFloat()),
            CB.mouseY.coerceIn(0f, (CB.windowHeight - 1).coerceAtLeast(0).toFloat()))
    }

    private fun avgY(e: MotionEvent): Float {
        if (e.pointerCount == 0) return 0f
        var total = 0f
        for (i in 0 until e.pointerCount) total += e.getY(i)
        return total / e.pointerCount
    }

    private fun clampCursor(x: Float, y: Float) {
        CB.sendCursorPos(x.coerceIn(0f, (CB.windowWidth - 1).coerceAtLeast(0).toFloat()),
            y.coerceIn(0f, (CB.windowHeight - 1).coerceAtLeast(0).toFloat()))
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val p = profile()
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activePointer = event.getPointerId(0)
                downX = event.x; downY = event.y; lastX = downX; lastY = downY
                downTime = event.eventTime; moved = false; multiTouch = false; scrollMoved = false
                leftHeld = false; longPressTriggered = false; scrollAccumulator = 0f
                if (!CB.isGrabbing() && (p.directTouch || !p.touchpadEnabled)) clampCursor(event.x, event.y) else syncCursor()
                if (p.gesturesEnabled) handler.postDelayed(longPress, p.longPressMs.toLong())
                invalidate()
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (!multiTouch) {
                    multiTouch = true; secondDownAt = event.eventTime; scrollMoved = false; scrollAccumulator = 0f
                    secondDownX = event.getX(event.actionIndex); secondDownY = event.getY(event.actionIndex)
                    lastAverageY = avgY(event); handler.removeCallbacks(longPress)
                    if (leftHeld) { Held.mouse(K.GLFW_MOUSE_BUTTON_LEFT.toInt(), false, "gesture-left"); leftHeld = false }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (multiTouch) {
                    if (!CB.isGrabbing() && event.pointerCount >= 2 && p.gesturesEnabled) {
                        val nowY = avgY(event)
                        // Match Zalith's Scroller: downward finger movement produces positive wheel Y.
                        scrollAccumulator += (nowY - lastAverageY) * p.scrollSensitivity
                        lastAverageY = nowY
                        val threshold = 6f * density
                        while (scrollAccumulator >= threshold) { CB.sendScroll(0.0, 1.0); scrollAccumulator -= threshold; scrollMoved = true }
                        while (scrollAccumulator <= -threshold) { CB.sendScroll(0.0, -1.0); scrollAccumulator += threshold; scrollMoved = true }
                    }
                    return true
                }
                val index = event.findPointerIndex(activePointer)
                if (index < 0) return true
                val x = event.getX(index); val y = event.getY(index)
                val dx = x - lastX; val dy = y - lastY
                if (!moved && hypot(x - downX, y - downY) > touchSlop) {
                    moved = true
                    if (!longPressTriggered) handler.removeCallbacks(longPress)
                }
                if (CB.isGrabbing()) {
                    // In-game mouse-look is relative. Do not clamp at window edges or 360-degree turning stops.
                    CB.sendCursorPos(CB.mouseX + dx * p.cameraSensitivity, CB.mouseY + dy * p.cameraSensitivity)
                } else if (p.directTouch || !p.touchpadEnabled) {
                    clampCursor(x, y)
                } else {
                    clampCursor(CB.mouseX + dx * p.mouseSpeed, CB.mouseY + dy * p.mouseSpeed)
                }
                lastX = x; lastY = y
                invalidate()
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (multiTouch && event.pointerCount == 2) {
                    val liftedX = event.getX(event.actionIndex)
                    val liftedY = event.getY(event.actionIndex)
                    val quickSecondTap = !scrollMoved && event.eventTime - secondDownAt < 150L &&
                        hypot(liftedX - secondDownX, liftedY - secondDownY) < touchSlop * 2f
                    if (quickSecondTap) {
                        // Two-finger tap is a right click; hotbar double-tap has its own F shortcut.
                        Held.mouse(K.GLFW_MOUSE_BUTTON_RIGHT.toInt(), true, "gesture-right")
                        Held.mouse(K.GLFW_MOUSE_BUTTON_RIGHT.toInt(), false, "gesture-right")
                    }
                    val remaining = if (event.actionIndex == 0) 1 else 0
                    activePointer = event.getPointerId(remaining)
                    lastX = event.getX(remaining); lastY = event.getY(remaining)
                    moved = true; multiTouch = false
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(longPress)
                if (leftHeld) { Held.mouse(K.GLFW_MOUSE_BUTTON_LEFT.toInt(), false, "gesture-left"); leftHeld = false }
                else if (event.actionMasked == MotionEvent.ACTION_UP && !CB.isGrabbing() && !suppressClickForGesture && !moved && !multiTouch && event.eventTime - downTime < 800L) {
                    // A quick tap in gameplay is not a click in Zalith: in-game click is long-press-hold.
                    if (p.directTouch || !p.touchpadEnabled) clampCursor(event.x, event.y)
                    Held.mouse(K.GLFW_MOUSE_BUTTON_LEFT.toInt(), true, "gesture-left")
                    Held.mouse(K.GLFW_MOUSE_BUTTON_LEFT.toInt(), false, "gesture-left")
                }
                activePointer = -1; multiTouch = false; moved = false; suppressClickForGesture = false; invalidate()
            }
        }
        return true
    }

    fun cancelInput() {
        handler.removeCallbacks(longPress)
        if (leftHeld) Held.mouse(K.GLFW_MOUSE_BUTTON_LEFT.toInt(), false, "gesture-left")
        leftHeld = false; activePointer = -1; multiTouch = false; suppressClickForGesture = false
        invalidate()
    }
}
