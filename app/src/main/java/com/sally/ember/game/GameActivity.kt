package com.sally.ember.game

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.TextView
import com.sally.ember.data.Paths
import com.sally.ember.data.Prefs
import net.kdt.pojavlaunch.GrabListener
import net.kdt.pojavlaunch.LwjglGlfwKeycode
import net.kdt.pojavlaunch.utils.JREUtils
import org.lwjgl.glfw.CallbackBridge
import kotlin.concurrent.thread

/**
 * Runs in the ":game" process. Hosts the Surface that libpojavexec renders into, starts the JVM once
 * the Surface exists, and hosts the touch controls (joystick, buttons, mouse pointer) on top of it.
 */
class GameActivity : Activity(), SurfaceHolder.Callback {
    private lateinit var logView: TextView
    private lateinit var logScroll: ScrollView
    private lateinit var closeBtn: TextView
    private lateinit var controls: TouchControls
    private lateinit var kbInput: EditText
    private val ui = Handler(Looper.getMainLooper())
    private var started = false
    private var logVisible = true
    private var exited = false
    private var autoHidden = false
    private var kbOn = false
    private var kbResetting = false

    private val grabListener = GrabListener { grabbing -> runOnUiThread { if (::controls.isInitialized) controls.setGrabbing(grabbing) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Paths.init(this)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // The soft keyboard must never resize the window: that would resize the GL surface mid-game.
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        val root = FrameLayout(this)
        val sv = SurfaceView(this).also { it.holder.addCallback(this) }
        root.addView(sv)

        logView = TextView(this).apply {
            setTextColor(Color.rgb(184, 247, 200)); textSize = 9f; typeface = android.graphics.Typeface.MONOSPACE
            setBackgroundColor(Color.argb(150, 0, 0, 0)); setPadding(12, 12, 12, 12)
        }
        logScroll = ScrollView(this).apply { addView(logView) }
        root.addView(logScroll, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, (resources.displayMetrics.heightPixels * 0.75).toInt(), Gravity.TOP))

        controls = TouchControls(this) { toggleKeyboard() }
        root.addView(controls, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        // Hidden field that receives soft-keyboard input and forwards it to the game as GLFW char/key events.
        kbInput = buildKeyboardInput()
        root.addView(kbInput, FrameLayout.LayoutParams(2, 2, Gravity.BOTTOM or Gravity.START))

        closeBtn = TextView(this).apply {
            text = "TUTUP"; setTextColor(Color.WHITE); setPadding(24, 12, 24, 12); setBackgroundColor(Color.argb(200, 160, 30, 30)); visibility = View.GONE
            setOnClickListener { closeGame() }
        }
        root.addView(closeBtn, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.START))
        root.addView(TextView(this).apply {
            text = "LOG"; setTextColor(Color.WHITE); setPadding(24, 12, 24, 12); setBackgroundColor(Color.argb(120, 0, 0, 0))
            setOnClickListener { logVisible = !logVisible; applyLogVisibility() }
        }, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.END))

        ExitReporter.onExit = { code, sig -> runOnUiThread { onJvmExit("JVM exit code=$code${if (sig) " (signal)" else ""}") } }
        setContentView(root)
        window.insetsController?.let { it.hide(WindowInsets.Type.systemBars()); it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE }
        applyLogVisibility()
        CallbackBridge.addGrabListener(grabListener)
        tailLog()
    }

    // Never leave a key or mouse button stuck down when the app loses focus (notification shade, app switch, screen off).
    override fun onResume() {
        super.onResume()
        if (::controls.isInitialized) controls.onHostResume()
    }

    override fun onPause() {
        if (::controls.isInitialized) controls.onHostPause()
        super.onPause()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (::controls.isInitialized) {
            val fromPhysicalKeyboard = (event.source and InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD
            // Leave typed text to the focused IME field while the software keyboard is open.
            if (!(kbOn && fromPhysicalKeyboard) && controls.onHardwareKey(event)) return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: android.view.MotionEvent): Boolean {
        if (::controls.isInitialized && controls.onHardwareMotion(event)) return true
        return super.dispatchGenericMotionEvent(event)
    }

    @Deprecated("Deprecated in Android")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == TouchControls.REQUEST_IMPORT && resultCode == RESULT_OK) {
            data?.data?.let { uri -> if (::controls.isInitialized) controls.importProfile(uri) }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (::controls.isInitialized) {
            controls.onHostWindowFocus(hasFocus)
            if (!hasFocus) controls.releaseAll()
        }
    }

    override fun onDestroy() {
        CallbackBridge.removeGrabListener(grabListener)
        super.onDestroy()
    }

    /** Log and controls are mutually exclusive: while the log is up (startup / crash) it must be scrollable. */
    private fun applyLogVisibility() {
        logScroll.visibility = if (logVisible) View.VISIBLE else View.GONE
        val showControls = !logVisible && !exited
        controls.visibility = if (showControls) View.VISIBLE else View.GONE
        if (!showControls) controls.releaseAll()
    }

    private fun tailLog() {
        val t = runCatching { Paths.logFile.readText().takeLast(12000) }.getOrDefault("")
        if (t != logView.text.toString()) {
            logView.text = t
            if (!exited) logScroll.post { logScroll.fullScroll(View.FOCUS_DOWN) }
        }
        // Once Minecraft has opened its window, get the log out of the way so the controls are usable.
        if (!autoHidden && !exited && (t.contains("Backend library") || t.contains("OpenAL initialized"))) {
            autoHidden = true; logVisible = false; applyLogVisibility()
        }
        ui.postDelayed(::tailLog, 700)
    }

    /** Keeps the log on screen after the JVM ends so the error can be read; the user closes manually. */
    private fun onJvmExit(@Suppress("UNUSED_PARAMETER") msg: String) {
        exited = true
        logVisible = true
        applyLogVisibility()
        closeBtn.visibility = View.VISIBLE
        logScroll.post { logScroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun closeGame() { finishAndRemoveTask(); android.os.Process.killProcess(android.os.Process.myPid()) }

    // ---- soft keyboard -> GLFW ----

    private fun buildKeyboardInput() = EditText(this).apply {
        alpha = 0.01f
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_ACTION_SEND
        setText(SENTINEL); setSelection(text.length)
        setOnEditorActionListener { _, _, _ -> CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_ENTER.toInt()); true }
        setOnKeyListener { _, code, ev ->
            if (ev.action == KeyEvent.ACTION_DOWN && code == KeyEvent.KEYCODE_ENTER) {
                CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_ENTER.toInt()); true
            } else false
        }
        // A zero-width sentinel char is always present so a Backspace is visible as "the sentinel disappeared".
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable) {
                if (kbResetting) return
                val t = s.toString()
                if (t.isEmpty()) {
                    CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_BACKSPACE.toInt())
                } else {
                    t.replace(SENTINEL, "").forEach { ch -> CallbackBridge.sendChar(ch, 0) }
                }
                kbResetting = true
                s.replace(0, s.length, SENTINEL)
                kbResetting = false
            }
        })
    }

    private fun toggleKeyboard() {
        val imm = getSystemService(InputMethodManager::class.java)
        kbOn = !kbOn
        if (kbOn) {
            kbInput.requestFocus()
            imm.showSoftInput(kbInput, InputMethodManager.SHOW_FORCED)
        } else {
            imm.hideSoftInputFromWindow(kbInput.windowToken, 0)
        }
    }

    // ---- surface / JVM ----

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
                runOnUiThread { onJvmExit("JVM exited with code $code") }
            }
        } else {
            runCatching { CallbackBridge.sendUpdateWindowSize(w, h) }
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) { runCatching { JREUtils.releaseBridgeWindow() } }

    @Deprecated("Back = Escape in game")
    override fun onBackPressed() {
        if (exited) { closeGame(); return }
        CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_ESCAPE.toInt(), 0, true)
        CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_ESCAPE.toInt(), 0, false)
    }

    private companion object { const val SENTINEL = "​" }
}
