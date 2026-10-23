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

    /** Folder JRE. Diisi dari arsip .tar.xz lewat menu Pengaturan. */
    val jreHome: File get() = File(root, "jre-17")

    /** Versi dianggap terpasang jika sudah punya classpath.txt (ditulis di akhir instalasi). */
    fun installedVersions(): Set<String> =
        versions.listFiles()
            ?.filter { File(it, "classpath.txt").exists() }
            ?.map { it.name }
            ?.toSet()
            ?: emptySet()
}
