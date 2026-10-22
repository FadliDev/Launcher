package com.sally.ide.data

import java.io.File

/** Utilitas file kecil. Semua fungsi di sini blocking: panggil dari IO dispatcher. */
object Fs {
    val SKIP_DIRS = setOf("build", ".gradle", ".git", ".idea", "node_modules")

    fun validName(n: String): Boolean =
        n.isNotBlank() && n.length <= 100 && n != "." && n != ".." &&
            n.none { it == '/' || it == '\\' || it == '\u0000' }

    /** Nama salinan yang belum dipakai: "a copy.kt", "a copy 2.kt", ... */
    fun uniqueCopy(src: File): File {
        val parent = src.absoluteFile.parentFile ?: src.absoluteFile
        val dot = src.name.lastIndexOf('.')
        val hasExt = src.isFile && dot > 0
        val base = if (hasExt) src.name.substring(0, dot) else src.name
        val ext = if (hasExt) src.name.substring(dot) else ""
        for (i in 1..999) {
            val suffix = if (i == 1) " copy" else " copy $i"
            val c = File(parent, base + suffix + ext)
            if (!c.exists()) return c
        }
        return File(parent, base + " copy " + System.nanoTime() + ext)
    }
}
