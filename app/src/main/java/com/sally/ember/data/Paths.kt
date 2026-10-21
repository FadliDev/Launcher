package com.sally.ember.data

import android.content.Context
import java.io.File

object Paths {
    lateinit var root: File; private set
    val versions get() = File(root, "versions")
    val libraries get() = File(root, "libraries")
    val assets get() = File(root, "assets")
    /** JRE must live in INTERNAL storage: Android/data/... on shared storage is mounted noexec, so dlopen(libjli.so) fails there. */
    lateinit var runtime: File; private set
    val lwjgl get() = File(root, "lwjgl3")
    val gameDir get() = File(root, "game")
    val logFile get() = File(root, "latest.log")

    fun init(ctx: Context) {
        root = File(ctx.getExternalFilesDir(null) ?: ctx.filesDir, "ember")
        runtime = File(ctx.filesDir, "runtime")
        listOf(versions, libraries, assets, runtime, lwjgl, gameDir).forEach { it.mkdirs() }
        // Drop the old (unusable) copy on external storage left by earlier builds.
        File(root, "runtime").takeIf { it.exists() }?.let { old -> Thread { old.deleteRecursively() }.start() }
    }
}
