package com.sally.ember.game

import android.content.Context
import com.sally.ember.data.Paths
import org.tukaani.xz.XZInputStream
import java.io.File

/** Bundled JREs live in assets/components/jre-N/{universal,bin-arm64}.tar.xz, same layout as Zalith. */
enum class Jre(val dir: String, val major: Int) { J8("jre-8", 8), J17("jre-17", 17), J21("jre-21", 21), J25("jre-25", 25) }

object JreManager {
    fun forMajor(major: Int): Jre = when {
        major <= 8 -> Jre.J8
        major <= 17 -> Jre.J17
        major <= 21 -> Jre.J21
        else -> Jre.J25
    }

    fun home(j: Jre) = File(Paths.runtime, j.dir)

    /** "lib/aarch64" on JREs that use an arch dir, otherwise "lib". */
    fun libDir(home: File) = if (File(home, "lib/aarch64").isDirectory) "lib/aarch64" else "lib"

    /** Unpacks (or re-unpacks after an app update) the JRE; [onStatus] gets human-readable progress. */
    fun ensure(ctx: Context, j: Jre, onStatus: (String) -> Unit = {}) {
        val home = home(j)
        val bundled = ctx.assets.open("components/${j.dir}/version").use { it.readBytes().toString(Charsets.UTF_8).trim() }
        val marker = File(home, ".ember_version")
        if (marker.exists() && marker.readText() == bundled && File(home, "bin/java").exists()) return

        onStatus("Mengekstrak ${j.dir} (sekali saja)...")
        home.deleteRecursively(); home.mkdirs()
        for (part in listOf("universal", "bin-arm64")) {
            ctx.assets.open("components/${j.dir}/$part.tar.xz").buffered().use { Untar.extract(XZInputStream(it), home) }
        }
        marker.writeText(bundled)
    }

    /** LWJGL's GLFW stub classes (override the stock GLFW so input/windowing goes through the bridge). */
    fun ensureGlfwJar(ctx: Context): File {
        val out = File(Paths.lwjgl, "lwjgl-glfw-classes.jar")
        val len = ctx.assets.openFd("components/lwjgl3/lwjgl-glfw-classes.jar").use { it.length }
        if (!out.exists() || out.length() != len) {
            out.parentFile!!.mkdirs()
            ctx.assets.open("components/lwjgl3/lwjgl-glfw-classes.jar").use { i -> out.outputStream().use { i.copyTo(it) } }
        }
        return out
    }
}
