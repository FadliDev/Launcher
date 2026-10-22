package com.sally.ide

import android.app.Application
import android.content.Context
import android.util.Log
import java.io.File

const val APP_VERSION = "0.3.0"

class SallyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLog.install(this)
    }
}

/** Catat crash ke file supaya bisa dibaca & disalin di peluncuran berikutnya (tanpa adb). */
object CrashLog {
    private fun file(ctx: Context) = File(ctx.filesDir, "last_crash.txt")
    private fun sp(ctx: Context) = ctx.getSharedPreferences("sally_safe", Context.MODE_PRIVATE)

    fun install(app: Application) {
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            runCatching { file(app).writeText("Thread: ${t.name}\n" + Log.getStackTraceString(e)) }
            prev?.uncaughtException(t, e)
        }
    }

    fun read(ctx: Context): String? = file(ctx).takeIf { it.exists() }?.readText()
    fun clear(ctx: Context) { file(ctx).delete() }
    fun isSafe(ctx: Context) = sp(ctx).getBoolean("safe", false)
    fun setSafe(ctx: Context, v: Boolean) { sp(ctx).edit().putBoolean("safe", v).apply() }
}
