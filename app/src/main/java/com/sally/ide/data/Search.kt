package com.sally.ide.data

import java.io.File

data class Hit(val file: File, val line: Int, val text: String)

/** Pencarian proyek sederhana (nama file fuzzy + isi teks). Blocking; [tick] dipanggil per file untuk cek pembatalan. */
object ProjectSearch {
    private const val MAX_FILE_BYTES = 1_000_000L

    private fun walk(root: File) = root.walkTopDown().onEnter { it == root || it.name !in Fs.SKIP_DIRS }

    private fun subsequence(q: String, s: String): Boolean {
        var i = 0
        for (c in s) if (i < q.length && c == q[i]) i++
        return i == q.length
    }

    fun files(root: File, q: String, limit: Int = 60, tick: () -> Unit = {}): List<File> {
        val query = q.trim().lowercase()
        if (query.isEmpty()) return emptyList()
        val out = ArrayList<Pair<Int, File>>()
        for (f in walk(root)) {
            tick()
            if (!f.isFile) continue
            val n = f.name.lowercase()
            val score = when {
                n.startsWith(query) -> 0
                n.contains(query) -> 1
                subsequence(query, n) -> 2
                else -> continue
            }
            out += score to f
        }
        return out.sortedWith(compareBy({ it.first }, { it.second.name.length })).take(limit).map { it.second }
    }

    fun text(root: File, q: String, ignoreCase: Boolean, limit: Int = 200, tick: () -> Unit = {}): List<Hit> {
        if (q.isEmpty()) return emptyList()
        val out = ArrayList<Hit>()
        for (f in walk(root)) {
            if (out.size >= limit) break
            tick()
            if (!f.isFile || f.length() > MAX_FILE_BYTES || looksBinary(f)) continue
            f.bufferedReader().use { r ->
                var n = 0
                while (true) {
                    val line = r.readLine() ?: break
                    n++
                    if (line.contains(q, ignoreCase)) {
                        out += Hit(f, n, line.trim().take(200))
                        if (out.size >= limit) break
                    }
                }
            }
        }
        return out
    }

    private fun looksBinary(f: File): Boolean = runCatching {
        f.inputStream().use { s ->
            val buf = ByteArray(1024)
            val n = s.read(buf)
            (0 until maxOf(n, 0)).any { buf[it] == 0.toByte() }
        }
    }.getOrDefault(true)
}
