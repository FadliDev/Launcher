package com.sally.ember.game

import android.app.Activity
import android.content.Context
import android.os.Process

/** Called from native code (stdio_is.c exit trap) when the JVM exits. Static method on this class. */
object ExitReporter {
    @JvmStatic
    fun showExitMessage(ctx: Context, code: Int, isSignal: Boolean) {
        runCatching { com.sally.ember.data.Paths.logFile.appendText("\n[JVM exit code=$code signal=$isSignal]\n") }
        (ctx as? Activity)?.runOnUiThread { ctx.finishAndRemoveTask() }
        Thread { Thread.sleep(400); Process.killProcess(Process.myPid()) }.start()
    }
}
