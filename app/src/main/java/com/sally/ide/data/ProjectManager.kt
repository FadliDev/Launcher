package com.sally.ide.data

import android.content.Context
import android.content.SharedPreferences
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/** Satu hasil pencarian teks di dalam proyek. [line] berbasis 0. */
data class TextMatch(val file: File, val line: Int, val preview: String)

/**
 * Semua operasi file proyek (buat, ubah nama, hapus, duplikat, zip, pencarian).
 * Semua fungsi di sini blocking, panggil dari Dispatchers.IO bila datanya besar.
 */
object ProjectManager {
    const val MAX_OPEN_BYTES = 2L * 1024 * 1024
    private const val MAX_ZIP_ENTRIES = 20_000
    private const val MAX_ZIP_BYTES = 512L * 1024 * 1024
    private const val RECENT_KEY = "recent_projects"
    private val searchSkipDirs = setOf(".git", ".gradle", ".idea", ".kotlin", "build", "node_modules")
    private val exportSkipDirs = setOf(".gradle", ".kotlin", "build")

    fun projectsRoot(context: Context): File = File(context.filesDir, "projects").apply { mkdirs() }

    // ---------- daftar & proyek terakhir ----------

    fun listProjects(context: Context, prefs: SharedPreferences): List<File> {
        val recent = loadRecent(prefs)
        val dirs = projectsRoot(context).listFiles()?.filter { it.isDirectory }.orEmpty()
        return dirs.sortedWith(
            compareBy<File>(
                { val i = recent.indexOf(it.name); if (i < 0) Int.MAX_VALUE else i },
                { it.name.lowercase() }
            )
        )
    }

    fun loadRecent(prefs: SharedPreferences): List<String> =
        prefs.getString(RECENT_KEY, "").orEmpty().split('\n').filter { it.isNotBlank() }

    fun touchRecent(prefs: SharedPreferences, name: String) {
        val updated = (listOf(name) + loadRecent(prefs).filter { it != name }).take(20)
        prefs.edit().putString(RECENT_KEY, updated.joinToString("\n")).apply()
    }

    // ---------- nama ----------

    fun sanitizeName(raw: String, fallback: String = "Proyek"): String {
        val cleaned = raw.trim().replace(Regex("[\\\\/:*?\"<>|]"), "_").trim('.', ' ')
        return cleaned.ifBlank { fallback }.take(80)
    }

    fun isValidEntryName(name: String): Boolean =
        name.isNotBlank() && name != "." && name != ".." && name.length <= 255 &&
            !name.contains('/') && !name.contains('\\')

    fun normalizePackage(raw: String, projectName: String): String {
        val valid = Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$")
        val candidate = raw.trim().lowercase()
        if (valid.matches(candidate)) return candidate
        val tail = projectName.lowercase().filter { it.isLetterOrDigit() && it.code < 128 }.ifBlank { "app" }
        return "com.example." + if (tail.first().isDigit()) "app$tail" else tail
    }

    fun uniqueChild(parent: File, name: String): File {
        var target = File(parent, name)
        var n = 1
        val base = name.substringBeforeLast('.', name)
        val ext = name.substringAfterLast('.', "")
        while (target.exists()) {
            target = File(parent, if (ext.isBlank() || base == name) "$name-$n" else "$base-$n.$ext")
            n++
        }
        return target
    }

    fun isInside(root: File, file: File): Boolean {
        val r = root.canonicalPath
        val f = file.canonicalPath
        return f == r || f.startsWith(r + File.separator)
    }

    // ---------- proyek ----------

    fun createProject(context: Context, template: ProjectTemplate, rawName: String, rawPackage: String): File {
        val name = sanitizeName(rawName)
        val dir = uniqueChild(projectsRoot(context), name)
        val pkg = normalizePackage(rawPackage, name)
        val files = ProjectTemplates.render(template, dir.name, pkg)
        dir.mkdirs()
        files.forEach { (path, content) ->
            val out = File(dir, path)
            out.parentFile?.mkdirs()
            out.writeText(content)
        }
        return dir
    }

    fun deleteProject(dir: File): Boolean = dir.deleteRecursively()

    // ---------- operasi node ----------

    fun children(dir: File): List<File> =
        dir.listFiles()?.sortedWith(compareBy<File>({ !it.isDirectory }, { it.name.lowercase() })).orEmpty()

    fun createFile(parent: File, name: String): Result<File> = runCatching {
        require(isValidEntryName(name)) { "Nama tidak valid" }
        val target = File(parent, name)
        require(!target.exists()) { "$name sudah ada" }
        target.parentFile?.mkdirs()
        check(target.createNewFile()) { "Gagal membuat $name" }
        target
    }

    fun createFolder(parent: File, name: String): Result<File> = runCatching {
        require(isValidEntryName(name)) { "Nama tidak valid" }
        val target = File(parent, name)
        require(!target.exists()) { "$name sudah ada" }
        check(target.mkdirs()) { "Gagal membuat folder $name" }
        target
    }

    fun rename(file: File, newName: String): Result<File> = runCatching {
        require(isValidEntryName(newName)) { "Nama tidak valid" }
        val target = File(file.parentFile, newName)
        require(!target.exists()) { "$newName sudah ada" }
        check(file.renameTo(target)) { "Gagal mengganti nama" }
        target
    }

    fun delete(file: File): Result<Unit> = runCatching {
        check(file.deleteRecursively()) { "Gagal menghapus ${file.name}" }
    }

    fun duplicate(file: File): Result<File> = runCatching {
        val parent = file.parentFile ?: error("Tidak ada folder induk")
        val copyName = if (file.isDirectory || !file.name.contains('.')) {
            file.name + "-copy"
        } else {
            file.nameWithoutExtension + "-copy." + file.extension
        }
        val target = uniqueChild(parent, copyName)
        if (file.isDirectory) {
            check(file.copyRecursively(target, overwrite = false)) { "Gagal menduplikasi folder" }
        } else {
            file.copyTo(target, overwrite = false)
        }
        target
    }

    // ---------- baca file ----------

    fun looksBinary(file: File): Boolean = runCatching {
        file.inputStream().use { input ->
            val buffer = ByteArray(4096)
            val read = input.read(buffer)
            (0 until maxOf(read, 0)).any { buffer[it].toInt() == 0 }
        }
    }.getOrDefault(true)

    /** Null jika file terlalu besar atau berisi biner. */
    fun readTextSafe(file: File): String? {
        if (!file.isFile || file.length() > MAX_OPEN_BYTES || looksBinary(file)) return null
        return runCatching { file.readText() }.getOrNull()
    }

    // ---------- zip ----------

    fun exportZip(projectDir: File, out: OutputStream) {
        ZipOutputStream(out.buffered()).use { zip ->
            val rootName = projectDir.name
            projectDir.walkTopDown()
                .onEnter { it == projectDir || it.name !in exportSkipDirs }
                .forEach { file ->
                    if (file == projectDir) return@forEach
                    val rel = file.relativeTo(projectDir).invariantSeparatorsPath
                    if (file.isDirectory) {
                        zip.putNextEntry(ZipEntry("$rootName/$rel/"))
                        zip.closeEntry()
                    } else {
                        zip.putNextEntry(ZipEntry("$rootName/$rel"))
                        file.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
        }
    }

    /** Ekstrak zip ke folder proyek baru. Dilindungi dari zip-slip dan zip bomb sederhana. */
    fun importZip(context: Context, input: InputStream, nameHint: String): File {
        val temp = File.createTempFile("import", ".zip", context.cacheDir)
        try {
            temp.outputStream().use { input.copyTo(it) }
            ZipFile(temp).use { zip ->
                val entries = zip.entries().asSequence().toList()
                require(entries.size <= MAX_ZIP_ENTRIES) { "Zip terlalu banyak isi" }
                val names = entries.map { it.name.replace('\\', '/') }.filter { it.isNotBlank() }
                val firstSegments = names.filter { !it.endsWith("/") || it.count { c -> c == '/' } > 1 }
                    .map { it.substringBefore('/') }.toSet()
                val singleRoot = firstSegments.size == 1 &&
                    names.all { it.startsWith(firstSegments.first() + "/") }
                val strip = if (singleRoot) firstSegments.first() + "/" else ""
                val projectName = sanitizeName(
                    if (singleRoot) firstSegments.first() else nameHint.removeSuffix(".zip"),
                    "Proyek Import"
                )
                val dest = uniqueChild(projectsRoot(context), projectName)
                dest.mkdirs()
                val destCanonical = dest.canonicalPath
                var total = 0L
                try {
                    for (entry in entries) {
                        val rel = entry.name.replace('\\', '/').removePrefix(strip)
                        if (rel.isBlank()) continue
                        val target = File(dest, rel)
                        val canonical = target.canonicalPath
                        require(canonical == destCanonical || canonical.startsWith(destCanonical + File.separator)) {
                            "Zip berisi path berbahaya"
                        }
                        if (entry.isDirectory) {
                            target.mkdirs()
                        } else {
                            target.parentFile?.mkdirs()
                            zip.getInputStream(entry).use { src ->
                                target.outputStream().use { dst ->
                                    val buffer = ByteArray(16 * 1024)
                                    while (true) {
                                        val n = src.read(buffer)
                                        if (n < 0) break
                                        total += n
                                        require(total <= MAX_ZIP_BYTES) { "Isi zip terlalu besar" }
                                        dst.write(buffer, 0, n)
                                    }
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
        } finally {
            temp.delete()
        }
    }

    // ---------- pencarian ----------

    /** Skor makin kecil makin cocok; null jika tidak cocok. */
    private fun fuzzyScore(query: String, name: String, path: String): Int? {
        val q = query.lowercase()
        val n = name.lowercase()
        val idx = n.indexOf(q)
        if (idx >= 0) return idx
        val p = path.lowercase()
        var qi = 0
        var first = -1
        var last = -1
        for (i in p.indices) {
            if (qi < q.length && p[i] == q[qi]) {
                if (first < 0) first = i
                last = i
                qi++
            }
        }
        return if (qi == q.length) 100 + (last - first) else null
    }

    fun fuzzyFiles(root: File, query: String, limit: Int = 60): List<File> {
        if (query.isBlank()) return emptyList()
        return root.walkTopDown()
            .onEnter { it == root || it.name !in searchSkipDirs }
            .filter { it.isFile }
            .mapNotNull { f ->
                val rel = f.relativeTo(root).invariantSeparatorsPath
                fuzzyScore(query.trim(), f.name, rel)?.let { f to it }
            }
            .sortedBy { it.second }
            .take(limit)
            .map { it.first }
            .toList()
    }

    fun searchText(root: File, query: String, limit: Int = 200): List<TextMatch> {
        if (query.isBlank()) return emptyList()
        val results = ArrayList<TextMatch>()
        val files = root.walkTopDown()
            .onEnter { it == root || it.name !in searchSkipDirs }
            .filter { it.isFile && it.length() <= 1024 * 1024 }
        for (file in files) {
            if (results.size >= limit) break
            if (looksBinary(file)) continue
            runCatching {
                var lineNo = 0
                file.useLines { lines ->
                    for (line in lines) {
                        if (line.contains(query, ignoreCase = true)) {
                            results.add(TextMatch(file, lineNo, line.trim().take(160)))
                            if (results.size >= limit) break
                        }
                        lineNo++
                    }
                }
            }
        }
        return results
    }
}
