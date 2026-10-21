package com.sally.ember.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import android.widget.LinearLayout
import kotlin.math.hypot
import net.kdt.pojavlaunch.LwjglGlfwKeycode as K
import org.lwjgl.glfw.CallbackBridge as CB

private typealias LP = FrameLayout.LayoutParams
private const val MATCH = -1
private const val WRAP = -2

private val MOUSE_LEFT = K.GLFW_MOUSE_BUTTON_LEFT.toInt()
private val MOUSE_RIGHT = K.GLFW_MOUSE_BUTTON_RIGHT.toInt()

/** Tracks what is currently held so every press gets a matching release (menu opens, view hidden, etc.). */
internal object Held {
    private val keys = HashSet<Int>()
    private val buttons = HashSet<Int>()

    @Synchronized fun key(code: Int, down: Boolean) {
        if (down) { if (!keys.add(code)) return } else { if (!keys.remove(code)) return }
        CB.setModifiers(code, down)
        CB.sendKeyPress(code, CB.getCurrentMods(), down)
    }

    private val ui = Handler(Looper.getMainLooper())
    /** Press now, release a few ms later: a press+release in the same frame can be dropped by the game's input polling. */
    const val TAP_MS = 45L

    fun tap(code: Int) { key(code, true); ui.postDelayed({ key(code, false) }, TAP_MS) }

    /** Click at the pointer's current position. The position is re-sent first so the game can't be looking elsewhere. */
    fun click(btn: Int) {
        if (!CB.isGrabbing()) CB.sendCursorPos(CB.mouseX, CB.mouseY)
        mouse(btn, true)
        ui.postDelayed({ mouse(btn, false) }, TAP_MS)
    }

    @Synchronized fun mouse(btn: Int, down: Boolean) {
        if (down) { if (!buttons.add(btn)) return } else { if (!buttons.remove(btn)) return }
        CB.sendMouseButton(btn, down)
    }

    @Synchronized fun releaseAll() {
        keys.toList().forEach { key(it, false) }
        buttons.toList().forEach { mouse(it, false) }
    }
}

/** Round (or rounded-square) on-screen button. [toggle] buttons latch; others are held while pressed. */
internal class PadButton(
    ctx: Context, private val label: String, private val toggle: Boolean = false, private val round: Boolean = true,
    private val onChange: (Boolean) -> Unit
) : View(ctx) {
    private var active = false
    private val d = resources.displayMetrics.density
    private val bg = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = Color.argb(170, 255, 255, 255); strokeWidth = 2f * d
    }
    private val txt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
    }

    fun setActive(a: Boolean) { active = a; invalidate() }

    override fun onDraw(c: Canvas) {
        val r = minOf(width, height) / 2f
        val cx = width / 2f; val cy = height / 2f
        bg.color = if (active) Color.argb(150, 255, 255, 255) else Color.argb(80, 0, 0, 0)
        if (round) {
            c.drawCircle(cx, cy, r - stroke.strokeWidth, bg); c.drawCircle(cx, cy, r - stroke.strokeWidth, stroke)
        } else {
            val rect = RectF(stroke.strokeWidth, stroke.strokeWidth, width - stroke.strokeWidth, height - stroke.strokeWidth)
            c.drawRoundRect(rect, r * 0.25f, r * 0.25f, bg); c.drawRoundRect(rect, r * 0.25f, r * 0.25f, stroke)
        }
        txt.textSize = r * (if (label.length > 2) 0.42f else 0.62f)
        c.drawText(label, cx, cy - (txt.descent() + txt.ascent()) / 2f, txt)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (toggle) { active = !active; onChange(active) } else { active = true; onChange(true) }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!toggle) { active = false; onChange(false) }
            }
        }
        invalidate()
        return true
    }
}

/** Left thumb stick -> W/A/S/D; pushing fully forward holds Ctrl (sprint). */
internal class Joystick(ctx: Context) : View(ctx) {
    private var id = -1
    private var kx = 0f; private var ky = 0f
    private val d = resources.displayMetrics.density
    private val base = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(60, 0, 0, 0) }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = Color.argb(150, 255, 255, 255); strokeWidth = 2f * d
    }
    private val knob = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(130, 255, 255, 255) }

    override fun onDraw(c: Canvas) {
        val r = width / 2f
        c.drawCircle(r, r, r - ring.strokeWidth, base); c.drawCircle(r, r, r - ring.strokeWidth, ring)
        c.drawCircle(r + kx, r + ky, r * 0.32f, knob)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> { id = e.getPointerId(0); update(e.x, e.y) }
            MotionEvent.ACTION_MOVE -> { val i = e.findPointerIndex(id); if (i >= 0) update(e.getX(i), e.getY(i)) }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> release()
        }
        return true
    }

    private fun update(x: Float, y: Float) {
        val r = width / 2f
        val maxR = r * 0.6f
        var dx = x - r; var dy = y - r
        val len = hypot(dx, dy)
        if (len > maxR) { dx = dx / len * maxR; dy = dy / len * maxR }
        kx = dx; ky = dy
        val nx = dx / maxR; val ny = dy / maxR
        Held.key(K.GLFW_KEY_W.toInt(), ny < -0.35f)
        Held.key(K.GLFW_KEY_S.toInt(), ny > 0.35f)
        Held.key(K.GLFW_KEY_A.toInt(), nx < -0.35f)
        Held.key(K.GLFW_KEY_D.toInt(), nx > 0.35f)
        val ctrl = K.GLFW_KEY_LEFT_CONTROL.toInt()
        if (ny < -0.95f) Held.key(ctrl, true) else if (ny > -0.75f) Held.key(ctrl, false)
        invalidate()
    }

    fun release() {
        id = -1; kx = 0f; ky = 0f
        listOf(K.GLFW_KEY_W, K.GLFW_KEY_A, K.GLFW_KEY_S, K.GLFW_KEY_D, K.GLFW_KEY_LEFT_CONTROL).forEach { Held.key(it.toInt(), false) }
        invalidate()
    }
}

/**
 * Full-screen touch area behind the buttons.
 * In game (cursor grabbed): drag = look, tap = attack/click, long-press = hold attack, 2nd finger tap = use (right click).
 * In menus: draws a mouse pointer. Touchpad mode (default) moves it relatively; [directTouch] puts it under the finger.
 * Tap = left click, long-press = drag, two-finger tap = right click, two-finger swipe = scroll.
 */
internal class LookView(ctx: Context) : View(ctx) {
    var directTouch = false
    private val d = resources.displayMetrics.density
    private val slop = ViewConfiguration.get(ctx).scaledTouchSlop.toFloat()
    private val camSens = 1.1f
    private val padSens = 1.5f
    private val LONG_PRESS_MS = 350L

    private var id = -1
    private var lastX = 0f; private var lastY = 0f; private var downX = 0f; private var downY = 0f
    private var downAt = 0L
    private var moved = false; private var holding = false; private var multi = false
    private var scrolled = false; private var noTap = false
    private var scrollAcc = 0f; private var lastAvgY = 0f

    private val longPress = Runnable { if (!moved && !multi && id != -1) { holding = true; Held.mouse(MOUSE_LEFT, true) } }

    private val arrow = Path().apply {
        moveTo(0f, 0f); lineTo(0f, 22f * d); lineTo(5f * d, 17.5f * d); lineTo(9f * d, 26f * d)
        lineTo(12.5f * d, 24.5f * d); lineTo(8.5f * d, 16f * d); lineTo(15f * d, 16f * d); close()
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = Color.BLACK; strokeWidth = 1.5f * d }

    override fun onDraw(c: Canvas) {
        if (CB.isGrabbing()) return
        if (id == -1) CB.syncCursorFromGame() // game may have recentred it (menu opened); not while a finger is steering it
        c.save(); c.translate(CB.mouseX, CB.mouseY)
        c.drawPath(arrow, fill); c.drawPath(arrow, edge)
        c.restore()
        postInvalidateOnAnimation() // the game can move the cursor itself (e.g. recentre), so keep redrawing in menus
    }

    private fun avgY(e: MotionEvent): Float { var s = 0f; for (i in 0 until e.pointerCount) s += e.getY(i); return s / e.pointerCount }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                id = e.getPointerId(0); lastX = e.x; lastY = e.y; downX = e.x; downY = e.y; downAt = e.eventTime
                moved = false; holding = false; multi = false; scrolled = false; noTap = false; scrollAcc = 0f
                if (!CB.isGrabbing()) { if (directTouch) CB.sendCursorPos(e.x, e.y) else CB.syncCursorFromGame() }
                postDelayed(longPress, LONG_PRESS_MS)
            }
            MotionEvent.ACTION_POINTER_DOWN -> if (!multi) {
                multi = true; scrolled = false; noTap = true
                removeCallbacks(longPress)
                if (holding) { Held.mouse(MOUSE_LEFT, false); holding = false }
                lastAvgY = avgY(e); scrollAcc = 0f
            }
            MotionEvent.ACTION_MOVE -> {
                if (multi) {
                    if (!CB.isGrabbing() && e.pointerCount >= 2) {
                        val a = avgY(e); scrollAcc += a - lastAvgY; lastAvgY = a
                        val step = 36f * d
                        while (scrollAcc > step) { CB.sendScroll(0.0, 1.0); scrollAcc -= step; scrolled = true }
                        while (scrollAcc < -step) { CB.sendScroll(0.0, -1.0); scrollAcc += step; scrolled = true }
                    }
                    return true
                }
                val i = e.findPointerIndex(id)
                if (i < 0) return true
                val x = e.getX(i); val y = e.getY(i)
                val dx = x - lastX; val dy = y - lastY
                if (!moved && hypot(x - downX, y - downY) > slop) { moved = true; if (!holding) removeCallbacks(longPress) }
                lastX = x; lastY = y
                if (CB.isGrabbing()) {
                    if (moved || holding) CB.sendCursorPos(CB.mouseX + dx * camSens, CB.mouseY + dy * camSens)
                } else if (directTouch) {
                    CB.sendCursorPos(x, y)
                } else {
                    CB.sendCursorPos(
                        (CB.mouseX + dx * padSens).coerceIn(0f, (CB.windowWidth - 1).coerceAtLeast(0).toFloat()),
                        (CB.mouseY + dy * padSens).coerceIn(0f, (CB.windowHeight - 1).coerceAtLeast(0).toFloat())
                    )
                }
            }
            MotionEvent.ACTION_POINTER_UP -> if (multi && e.pointerCount == 2) {
                if (!scrolled) Held.click(MOUSE_RIGHT) // two-finger tap = right click / use
                val rem = if (e.actionIndex == 0) 1 else 0
                id = e.getPointerId(rem); lastX = e.getX(rem); lastY = e.getY(rem)
                moved = true; multi = false
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                removeCallbacks(longPress)
                if (holding) { Held.mouse(MOUSE_LEFT, false); holding = false }
                else if (e.actionMasked == MotionEvent.ACTION_UP && !moved && !noTap && e.eventTime - downAt < LONG_PRESS_MS) {
                    Held.click(MOUSE_LEFT)
                }
                id = -1; multi = false; noTap = false
            }
        }
        return true
    }
}

/** All on-screen controls. Gameplay buttons only show while the cursor is grabbed; the pointer toggle only in menus. */
internal class TouchControls(ctx: Context, onKeyboard: () -> Unit) : FrameLayout(ctx) {
    private val d = resources.displayMetrics.density
    private fun dp(v: Int) = (v * d).toInt()

    private val look = LookView(ctx)
    private val joystick = Joystick(ctx)
    private val sneak: PadButton
    private val gameViews = ArrayList<View>()
    private val menuViews = ArrayList<View>()

    private fun key(code: Short) = code.toInt()

    private fun br(v: View, size: Int, end: Int, bottom: Int) =
        addView(v, LP(dp(size), dp(size), Gravity.BOTTOM or Gravity.END).apply { marginEnd = dp(end); bottomMargin = dp(bottom) })

    private fun tr(v: View, size: Int, end: Int, top: Int) =
        addView(v, LP(dp(size), dp(size), Gravity.TOP or Gravity.END).apply { marginEnd = dp(end); topMargin = dp(top) })

    init {
        addView(look, LP(MATCH, MATCH))

        // movement
        addView(joystick, LP(dp(150), dp(150), Gravity.BOTTOM or Gravity.START).apply { marginStart = dp(28); bottomMargin = dp(24) })
        gameViews += joystick

        // right-hand actions
        val jump = PadButton(ctx, "JUMP") { Held.key(key(K.GLFW_KEY_SPACE), it) }
        sneak = PadButton(ctx, "SNK", toggle = true) { Held.key(key(K.GLFW_KEY_LEFT_SHIFT), it) }
        val use = PadButton(ctx, "USE") { Held.mouse(MOUSE_RIGHT, it) }
        val hit = PadButton(ctx, "HIT") { Held.mouse(MOUSE_LEFT, it) }
        br(jump, 64, 28, 24); br(sneak, 52, 104, 24); br(use, 56, 100, 92); br(hit, 64, 28, 104)
        gameViews += listOf(jump, sneak, use, hit)

        // top row: always available
        tr(PadButton(ctx, "ESC", round = false) { if (it) Held.tap(key(K.GLFW_KEY_ESCAPE)) }, 40, 8, 8)
        tr(PadButton(ctx, "E", round = false) { if (it) Held.tap(key(K.GLFW_KEY_E)) }, 40, 54, 8)
        tr(PadButton(ctx, "T", round = false) { if (it) Held.tap(key(K.GLFW_KEY_T)) }, 40, 100, 8)
        tr(PadButton(ctx, "KB", round = false) { if (it) onKeyboard() }, 40, 146, 8)

        // top row: in game only
        val drop = PadButton(ctx, "Q", round = false) { if (it) Held.tap(key(K.GLFW_KEY_Q)) }
        val f5 = PadButton(ctx, "F5", round = false) { if (it) Held.tap(key(K.GLFW_KEY_F5)) }
        tr(drop, 40, 192, 8); tr(f5, 40, 238, 8)
        gameViews += listOf(drop, f5)

        // top row: in menus only. Highlighted = pointer jumps to the finger, otherwise touchpad-style.
        val direct = PadButton(ctx, "DIR", toggle = true, round = false) { look.directTouch = it }
        tr(direct, 40, 192, 8)
        menuViews += direct

        // hotbar, laid over the game's own hotbar
        val bar = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        for (i in 0 until 9) {
            val slot = PadButton(ctx, "${i + 1}", round = false) { if (it) Held.tap(key(K.GLFW_KEY_1) + i) }
            slot.alpha = 0.55f
            bar.addView(slot, LinearLayout.LayoutParams(dp(34), dp(34)).apply { marginStart = dp(2); marginEnd = dp(2) })
        }
        addView(bar, LP(WRAP, WRAP, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = dp(2) })
        gameViews += bar

        setGrabbing(CB.isGrabbing())
    }

    /** Switches between the in-game layout and the menu (pointer) layout. */
    fun setGrabbing(grabbing: Boolean) {
        if (!grabbing) releaseAll()
        gameViews.forEach { it.visibility = if (grabbing) View.VISIBLE else View.GONE }
        menuViews.forEach { it.visibility = if (grabbing) View.GONE else View.VISIBLE }
        look.invalidate()
    }

    /** Lifts every held key/button (menu opened, controls hidden, JVM gone). */
    fun releaseAll() {
        joystick.release()
        Held.releaseAll()
        sneak.setActive(false)
    }
}
