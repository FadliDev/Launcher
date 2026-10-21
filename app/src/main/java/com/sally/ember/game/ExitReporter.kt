package com.sally.ember.game

import android.content.Context
import android.os.Process
import java.util.concurrent.CountDownLatch

/** Called from native code (stdio_is.c exit trap) when the JVM calls exit(code != 0) or aborts. */
object ExitReporter {
    /** Set by GameActivity so the crash log stays on screen instead of the process dying right away. */
    @Volatile var onExit: ((Int, Boolean) -> Unit)? = null

    @JvmStatic
    fun showExitMessage(ctx: Context, code: Int, isSignal: Boolean) {
        runCatching { com.sally.ember.data.Paths.logFile.appendText("\n[JVM exit code=$code signal=$isSignal]\n") }
        val cb = onExit
        if (cb == null) {
            Thread { Thread.sleep(400); Process.killProcess(Process.myPid()) }.start()
            return
        }
        cb(code, isSignal)
        // Block the JVM's exiting thread: native nominal_exit() calls System.exit(0) right after this returns,
        // which would kill the process (= back to home screen) before the log can be read. The user closes it.
        CountDownLatch(1).await()
    }
}
