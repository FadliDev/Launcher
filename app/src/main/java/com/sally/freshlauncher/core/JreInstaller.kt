package com.sally.freshlauncher.core

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.tukaani.xz.XZInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths

/** Mengekstrak arsip JRE (.tar.xz) yang dipilih pengguna ke folder JRE. */
object JreInstaller {

    fun extract(input: InputStream, dest: File, log: (String) -> Unit) {
        dest.mkdirs()
        val destCanon = dest.canonicalPath
        TarArchiveInputStream(XZInputStream(input)).use { tar ->
            var entry: TarArchiveEntry? = tar.nextTarEntry
            var count = 0
            while (entry != null) {
                val out = File(dest, entry.name)
                // Cegah path traversal (zip-slip): hasil harus tetap di dalam folder tujuan.
                check(out.canonicalPath.startsWith(destCanon)) { "Entri tar tidak aman: ${entry.name}" }
                when {
                    entry.isDirectory -> out.mkdirs()
                    entry.isSymbolicLink -> {
                        out.parentFile?.mkdirs()
                        if (out.exists() || Files.isSymbolicLink(out.toPath())) out.delete()
                        Files.createSymbolicLink(out.toPath(), Paths.get(entry.linkName))
                    }
                    entry.isFile -> {
                        out.parentFile?.mkdirs()
                        FileOutputStream(out).use { tar.copyTo(it) }
                        if (entry.mode and 0x49 != 0) out.setExecutable(true, false)
                    }
                }
                count++
                if (count % 500 == 0) log("Mengekstrak JRE... ($count file)")
                entry = tar.nextTarEntry
            }
            log("Selesai mengekstrak $count entri")
        }
    }
}
