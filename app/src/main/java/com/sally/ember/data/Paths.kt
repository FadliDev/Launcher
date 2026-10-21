package com.sally.ember.data

import android.content.Context
import java.io.File

object Paths {
    lateinit var root: File; private set
    val versions get() = File(root, "versions")
    val libraries get() = File(root, "libraries")
    val assets get() = File(root, "assets")
    val runtime get() = File(root, "runtime")
    val lwjgl get() = File(root, "lwjgl3")
    val gameDir get() = File(root, "game")
    val logFile get() = File(root, "latest.log")

    fun init(ctx: Context) {
        root = File(ctx.getExternalFilesDir(null) ?: ctx.filesDir, "ember")
        listOf(versions, libraries, assets, runtime, lwjgl, gameDir).forEach { it.mkdirs() }
    }
}
