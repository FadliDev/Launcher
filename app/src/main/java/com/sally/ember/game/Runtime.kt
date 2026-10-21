package com.sally.ember.game

import android.content.Context
import com.sally.ember.data.Paths
import org.tukaani.xz.XZInputStream
import java.io.File
import java.io.InputStream

/** Unpacks a JRE (assets/runtime/jre.tar.xz) and, optionally, LWJGL (assets/lwjgl3.tar.xz) once. */
object Runtime {
    fun jliPath(): File? = Paths.runtime.walkTopDown().firstOrNull { it.name == "libjli.so" }
    fun isReady() = jliPath() != null

    fun ensureInstalled(ctx: Context) {
        if (!isReady()) unpack(ctx, "runtime/jre.tar.xz", Paths.runtime)
        if (Paths.lwjgl.listFiles().isNullOrEmpty()) runCatching { unpack(ctx, "runtime/lwjgl3.tar.xz", Paths.lwjgl) }
    }

    fun javaHome(): File = jliPath()!!.parentFile!!.let { if (it.name == "jli") it.parentFile!!.parentFile!! else it.parentFile!! }

    fun ldPath(): String {
        val jli = jliPath()!!.parentFile!!
        val lib = if (jli.name == "jli") jli.parentFile!! else jli
        return listOf(jli, lib, File(lib, "server"), File(lib, "jli"), Paths.lwjgl).joinToString(":") { it.path } +
            ":/system/lib64:/vendor/lib64"
    }

    private fun unpack(ctx: Context, asset: String, dest: File) {
        dest.mkdirs()
        ctx.assets.open(asset).use { untar(XZInputStream(it.buffered()), dest) }
    }

    /** Minimal ustar reader: regular files, directories and symlinks (enough for a JRE). */
    private fun untar(i: InputStream, dest: File) {
        val hdr = ByteArray(512)
        var longName: String? = null
        while (true) {
            if (!readFully(i, hdr)) return
            if (hdr.all { it == 0.toByte() }) return
            var name = str(hdr, 0, 100)
            val prefix = str(hdr, 345, 155)
            if (prefix.isNotEmpty()) name = "$prefix/$name"
            val size = str(hdr, 124, 12).trim().ifEmpty { "0" }.toLong(8)
            val type = hdr[156].toInt().toChar()
            val link = str(hdr, 157, 100)
            if (type == 'L') { // GNU long name
                val b = ByteArray(size.toInt()); readFully(i, b); longName = String(b).trimEnd('\u0000'); skipPad(i, size); continue
            }
            if (longName != null) { name = longName; longName = null }
            val out = File(dest, name.removePrefix("./"))
            if (!out.canonicalPath.startsWith(dest.canonicalPath)) { i.skip(size + pad(size)); continue }
            when (type) {
                '5' -> out.mkdirs()
                '2' -> { out.parentFile?.mkdirs(); runCatching { android.system.Os.symlink(link, out.path) } }
                '0', '\u0000' -> {
                    out.parentFile?.mkdirs()
                    out.outputStream().use { o ->
                        var left = size; val buf = ByteArray(65536)
                        while (left > 0) { val n = i.read(buf, 0, minOf(left, buf.size.toLong()).toInt()); if (n < 0) break; o.write(buf, 0, n); left -= n }
                    }
                    val mode = str(hdr, 100, 8).trim().ifEmpty { "644" }.toInt(8)
                    out.setExecutable(mode and 0b001_001_001 != 0, false)
                    skipPad(i, size); continue
                }
                else -> {}
            }
            if (type != '0' && type != '\u0000') i.skip(size + pad(size))
        }
    }

    private fun pad(size: Long) = (512 - size % 512) % 512
    private fun skipPad(i: InputStream, size: Long) { var l = pad(size); while (l > 0) { val s = i.skip(l); if (s <= 0) break; l -= s } }
    private fun str(b: ByteArray, off: Int, len: Int): String = String(b, off, len).substringBefore('\u0000')
    private fun readFully(i: InputStream, b: ByteArray): Boolean {
        var r = 0; while (r < b.size) { val n = i.read(b, r, b.size - r); if (n < 0) return false; r += n }; return true
    }
}
