package com.sally.freshlauncher.core

import android.content.Context
import java.io.File

object GamePaths {
    private lateinit var root: File

    fun init(context: Context) {
        root = File(context.filesDir, "fresh")
    }

    val versions: File get() = File(root, "versions")
    val libraries: File get() = File(root, "libraries")
    val assets: File get() = File(root, "assets")
    val gameDir: File get() = File(root, "game").apply { mkdirs() }

    /** Lokasi JRE yang diharapkan. Untuk tes, salin JRE ke folder ini secara manual. */
    val jreHome: File get() = File(root, "jre-17")
    val jliLib: File get() = File(jreHome, "lib/libjli.so")

    /** Versi dianggap terpasang jika sudah punya classpath.txt (ditulis di akhir instalasi). */
    fun installedVersions(): Set<String> =
        versions.listFiles()
            ?.filter { File(it, "classpath.txt").exists() }
            ?.map { it.name }
            ?.toSet()
            ?: emptySet()
}
