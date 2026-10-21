package com.sally.ember.game

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.FrameLayout
import android.widget.TextView
import com.sally.ember.data.Paths
import com.sally.ember.data.Prefs
import net.kdt.pojavlaunch.LwjglGlfwKeycode
import net.kdt.pojavlaunch.utils.JREUtils
import org.lwjgl.glfw.CallbackBridge
import kotlin.concurrent.thread

/**
 * Runs in the ":game" process. Hosts the Surface that libpojavexec renders into, starts the JVM once
 * the Surface exists, and turns touches into GLFW mouse events.
 */
class GameActivity : Activity(), SurfaceHolder.Callback {
    private lateinit var logView: TextView
    private val ui = Handler(Looper.getMainLooper())
    private var started = false
    private var logVisible = true

    // touch state
    private var lastX = 0f; private var lastY = 0f; private var downAt = 0L; private var moved = false
    private var holding = false
    private val longPress = Runnable { if (!moved) { holding = true; CallbackBridge.sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT.toInt(), true) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Paths.init(this)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val root = FrameLayout(this)
        val sv = SurfaceView(this).also { it.holder.addCallback(this) }
        root.addView(sv)
        logView = TextView(this).apply {
            setTextColor(Color.rgb(184, 247, 200)); textSize = 9f; typeface = android.graphics.Typeface.MONOSPACE
            setBackgroundColor(Color.argb(150, 0, 0, 0)); setPadding(12, 12, 12, 12)
        }
        root.addView(logView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP))
        root.addView(TextView(this).apply {
            text = "LOG"; setTextColor(Color.WHITE); setPadding(24, 12, 24, 12); setBackgroundColor(Color.argb(120, 0, 0, 0))
            setOnClickListener { logVisible = !logVisible; logView.visibility = if (logVisible) View.VISIBLE else View.GONE }
        }, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.END))
        setContentView(root)
        window.insetsController?.let { it.hide(WindowInsets.Type.systemBars()); it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE }
        tailLog()
    }

    private fun tailLog() {
        logView.text = runCatching { Paths.logFile.readText().takeLast(1500) }.getOrDefault("")
        ui.postDelayed(::tailLog, 700)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {}

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
        CallbackBridge.physicalWidth = w; CallbackBridge.physicalHeight = h
        CallbackBridge.windowWidth = w; CallbackBridge.windowHeight = h
        runCatching { JREUtils.setupBridgeWindow(holder.surface) }
        if (!started) {
            started = true
            val prefs = Prefs(this)
            val acc = prefs.accounts().first { it.uuid == intent.getStringExtra("account") }
            val version = intent.getStringExtra("version")!!
            thread(name = "jvm") {
                val code = try { GameLauncher.launch(this, version, acc, prefs, w, h) }
                catch (t: Throwable) { Paths.logFile.appendText("\nLauncher error: ${android.util.Log.getStackTraceString(t)}\n"); -99 }
                Paths.logFile.appendText("\n[JVM exited with code $code]\n")
            }
        } else {
            runCatching { CallbackBridge.sendUpdateWindowSize(w, h) }
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) { runCatching { JREUtils.releaseBridgeWindow() } }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val left = LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT.toInt()
        val right = LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT.toInt()
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = e.x; lastY = e.y; downAt = e.eventTime; moved = false
                if (!CallbackBridge.isGrabbing()) CallbackBridge.sendCursorPos(e.x, e.y)
                ui.postDelayed(longPress, 400)
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = e.x - lastX; val dy = e.y - lastY
                if (kotlin.math.abs(dx) + kotlin.math.abs(dy) > 6f) { moved = true; ui.removeCallbacks(longPress) }
                if (CallbackBridge.isGrabbing()) CallbackBridge.sendCursorPos(CallbackBridge.mouseX + dx, CallbackBridge.mouseY + dy)
                else CallbackBridge.sendCursorPos(e.x, e.y)
                lastX = e.x; lastY = e.y
            }
            MotionEvent.ACTION_UP -> {
                ui.removeCallbacks(longPress)
                if (holding) { holding = false; CallbackBridge.sendMouseButton(left, false) }
                else if (!moved && e.eventTime - downAt < 300) {
                    CallbackBridge.sendMouseButton(left, true); CallbackBridge.sendMouseButton(left, false)
                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> { // second finger tap = right click
                CallbackBridge.sendMouseButton(right, true); CallbackBridge.sendMouseButton(right, false)
            }
        }
        return true
    }

    @Deprecated("Back = Escape in game")
    override fun onBackPressed() {
        CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_ESCAPE.toInt(), 0, true)
        CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_ESCAPE.toInt(), 0, false)
    }
}
