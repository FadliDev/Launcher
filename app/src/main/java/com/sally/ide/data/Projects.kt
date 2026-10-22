package com.sally.ide.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

enum class ProjectKind(val label: String) { ANDROID("Android"), MOD("Mod Minecraft"), PLAIN("Proyek") }

data class ProjectInfo(val dir: File, val kind: ProjectKind, val modified: Long)

/** Proyek = subfolder di <storage app>/workspace. Semua fungsi blocking: panggil dari IO dispatcher. */
class ProjectStore(val root: File) {
    init { root.mkdirs() }

    fun list(): List<ProjectInfo> =
        root.listFiles()
            ?.filter { it.isDirectory && !it.name.startsWith(".") }
            ?.map { ProjectInfo(it, kindOf(it), it.lastModified()) }
            ?.sortedByDescending { it.modified }
            ?: emptyList()

    private fun kindOf(d: File): ProjectKind = when {
        File(d, "app/build.gradle.kts").exists() || File(d, "app/build.gradle").exists() -> ProjectKind.ANDROID
        File(d, "src/main/resources/fabric.mod.json").exists() -> ProjectKind.MOD
        else -> ProjectKind.PLAIN
    }

    /** v0.1 menyimpan file lepas langsung di workspace/. Pindahkan ke satu proyek supaya tidak hilang. */
    fun migrateLegacy() {
        val loose = root.listFiles()?.filter { it.isFile } ?: return
        if (loose.isEmpty()) return
        val dest = uniqueDir("Workspace-lama")
        dest.mkdirs()
        loose.forEach { it.renameTo(File(dest, it.name)) }
    }

    fun validProjectName(n: String) = Fs.validName(n) && !n.startsWith(".") && Regex("[A-Za-z0-9 _.\\-]+").matches(n)

    fun create(name: String, t: Template): File? {
        if (!validProjectName(name)) return null
        val dir = File(root, name)
        if (dir.exists()) return null
        return try {
            t.files(name).forEach { (rel, text) ->
                val f = File(dir, rel)
                f.parentFile?.mkdirs()
                f.writeText(text)
            }
            dir.mkdirs()
            dir
        } catch (e: Exception) {
            dir.deleteRecursively()
            null
        }
    }

    fun delete(dir: File): Boolean = dir.parentFile == root && dir.deleteRecursively()

    private fun uniqueDir(base: String): File {
        var d = File(root, base)
        var i = 2
        while (d.exists()) { d = File(root, "$base-$i"); i++ }
        return d
    }

    // ---------- zip ----------
    private fun displayName(ctx: Context, uri: Uri): String? =
        ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }

    /** Kalau semua entri ada di satu folder induk (pola umum "Proyek/..."), folder itu dibuang. */
    private fun topPrefix(ctx: Context, uri: Uri): String? {
        val tops = HashSet<String>()
        var allNested = true
        ctx.contentResolver.openInputStream(uri)?.use { raw ->
            ZipInputStream(raw.buffered()).use { z ->
                while (true) {
                    val e = z.nextEntry ?: break
                    val n = e.name.replace('\\', '/')
                    if (n.startsWith("__MACOSX/")) continue
                    val i = n.indexOf('/')
                    if (i < 0) { allNested = false; break }
                    tops += n.substring(0, i)
                }
            }
        }
        return if (allNested && tops.size == 1) tops.first() + "/" else null
    }

    /** Ekstrak zip jadi proyek baru. Proteksi zip-slip + batas ukuran total. */
    fun importZip(ctx: Context, uri: Uri): File {
        val raw = (displayName(ctx, uri) ?: "Proyek").removeSuffix(".zip").removeSuffix(".ZIP")
        val safe = raw.replace(Regex("[^A-Za-z0-9 _.\\-]"), "_").trim('.', ' ').ifEmpty { "Proyek" }
        val dest = uniqueDir(safe)
        val prefix = topPrefix(ctx, uri)
        dest.mkdirs()
        try {
            val canon = dest.canonicalPath
            var total = 0L
            val input = ctx.contentResolver.openInputStream(uri) ?: throw IOException("Tidak bisa membaca file")
            input.use { rawIn ->
                ZipInputStream(rawIn.buffered()).use { z ->
                    while (true) {
                        val e = z.nextEntry ?: break
                        var rel = e.name.replace('\\', '/')
                        if (prefix != null) rel = rel.removePrefix(prefix)
                        if (rel.isEmpty() || rel.startsWith("__MACOSX/")) continue
                        val out = File(dest, rel)
                        if (!out.canonicalPath.startsWith(canon + File.separator)) throw IOException("Entri zip tidak aman: ${e.name}")
                        if (e.isDirectory) {
                            out.mkdirs()
                        } else {
                            out.parentFile?.mkdirs()
                            out.outputStream().use { o -> total += z.copyTo(o) }
                            if (total > MAX_IMPORT_BYTES) throw IOException("Zip terlalu besar")
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            dest.deleteRecursively()
            throw t
        }
        return dest
    }

    /** Tulis proyek ke [uri] sebagai zip (tanpa build/ dan .gradle/). */
    fun exportZip(ctx: Context, dir: File, uri: Uri) {
        val out = ctx.contentResolver.openOutputStream(uri) ?: throw IOException("Tidak bisa menulis file")
        out.use { rawOut ->
            ZipOutputStream(rawOut.buffered()).use { z ->
                dir.walkTopDown()
                    .onEnter { it == dir || (it.name != "build" && it.name != ".gradle") }
                    .filter { it.isFile }
                    .forEach { f ->
                        val rel = f.relativeTo(dir).path.replace(File.separatorChar, '/')
                        z.putNextEntry(ZipEntry(dir.name + "/" + rel))
                        f.inputStream().use { it.copyTo(z) }
                        z.closeEntry()
                    }
            }
        }
    }

    companion object { const val MAX_IMPORT_BYTES = 1024L * 1024 * 1024 }
}
