package com.sally.ember.game

import java.io.File
import java.io.InputStream

/** Minimal ustar reader: files, directories, symlinks, GNU long names (enough for the bundled JREs). */
object Untar {
    fun extract(i: InputStream, dest: File) {
        val hdr = ByteArray(512)
        var longName: String? = null
        val destPath = dest.canonicalPath + File.separator
        while (true) {
            if (!readFully(i, hdr) || hdr.all { it == 0.toByte() }) return
            var name = str(hdr, 0, 100)
            val prefix = str(hdr, 345, 155)
            if (prefix.isNotEmpty()) name = "$prefix/$name"
            val size = str(hdr, 124, 12).trim().ifEmpty { "0" }.toLong(8)
            val type = hdr[156].toInt().toChar()
            val link = str(hdr, 157, 100)
            if (type == 'L') {
                val b = ByteArray(size.toInt()); readFully(i, b)
                longName = String(b).trimEnd('\u0000'); skip(i, pad(size)); continue
            }
            if (longName != null) { name = longName; longName = null }
            val out = File(dest, name.removePrefix("./"))
            val safe = (out.canonicalPath + File.separator).startsWith(destPath)
            when {
                !safe -> skip(i, size + pad(size))
                type == '5' -> out.mkdirs()
                type == '2' -> { out.parentFile?.mkdirs(); runCatching { android.system.Os.symlink(link, out.path) }; skip(i, size + pad(size)) }
                type == '0' || type == '\u0000' -> {
                    out.parentFile?.mkdirs()
                    out.outputStream().use { o ->
                        var left = size; val buf = ByteArray(65536)
                        while (left > 0) {
                            val n = i.read(buf, 0, minOf(left, buf.size.toLong()).toInt())
                            if (n < 0) break
                            o.write(buf, 0, n); left -= n
                        }
                    }
                    val mode = str(hdr, 100, 8).trim().ifEmpty { "644" }.toInt(8)
                    out.setReadable(true, false)
                    out.setExecutable(mode and 0b001_001_001 != 0, false)
                    skip(i, pad(size))
                }
                else -> skip(i, size + pad(size))
            }
        }
    }

    private fun pad(size: Long) = (512 - size % 512) % 512
    private fun skip(i: InputStream, n: Long) { var l = n; while (l > 0) { val s = i.skip(l); if (s <= 0) { if (i.read() < 0) return; l-- } else l -= s } }
    private fun str(b: ByteArray, off: Int, len: Int) = String(b, off, len).substringBefore('\u0000')
    private fun readFully(i: InputStream, b: ByteArray): Boolean {
        var r = 0
        while (r < b.size) { val n = i.read(b, r, b.size - r); if (n < 0) return false; r += n }
        return true
    }
}
