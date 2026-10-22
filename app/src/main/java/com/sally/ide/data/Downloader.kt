package com.sally.ide.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

/** Unduh file dengan resume (file `.part` + header Range), progres, dan verifikasi SHA-256 opsional. */
object Downloader {
    class ChecksumException(msg: String) : Exception(msg)

    /**
     * Unduh [url] ke [dest]. Kalau [dest.part] sudah ada, lanjutkan dari situ.
     * @param sha256 hash yang diharapkan (hex) atau null untuk melewati verifikasi
     * @return hash SHA-256 (hex) file akhir
     */
    suspend fun download(url: String, dest: File, sha256: String?, onProgress: (done: Long, total: Long) -> Unit): String =
        withContext(Dispatchers.IO) {
            dest.parentFile?.mkdirs()
            val part = File(dest.path + ".part")
            var offset = if (part.exists()) part.length() else 0L

            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "SallyIDE")
                if (offset > 0) setRequestProperty("Range", "bytes=$offset-")
            }
            try {
                val code = conn.responseCode
                when {
                    code == 416 -> { /* part sudah lengkap */ }
                    code == 206 -> Unit
                    code == 200 -> if (offset > 0) { part.delete(); offset = 0 } // server mengabaikan Range
                    else -> throw java.io.IOException("HTTP $code untuk $url")
                }
                if (code != 416) {
                    val len = conn.contentLengthLong
                    val total = if (len > 0) len + offset else -1L
                    RandomAccessFile(part, "rw").use { raf ->
                        raf.seek(offset)
                        conn.inputStream.use { input ->
                            val buf = ByteArray(64 * 1024)
                            var done = offset
                            var lastReport = 0L
                            while (true) {
                                coroutineContext.ensureActive()
                                val n = input.read(buf)
                                if (n < 0) break
                                raf.write(buf, 0, n)
                                done += n
                                val now = System.nanoTime()
                                if (now - lastReport > 100_000_000L) { lastReport = now; onProgress(done, total) }
                            }
                            onProgress(done, if (total > 0) total else done)
                        }
                    }
                }
            } finally {
                conn.disconnect()
            }

            val actual = sha256Of(part)
            if (sha256 != null && !actual.equals(sha256.trim(), ignoreCase = true)) {
                part.delete()
                throw ChecksumException("Checksum tidak cocok (unduhan rusak, coba lagi)")
            }
            dest.delete()
            if (!part.renameTo(dest)) throw java.io.IOException("Gagal memindahkan ${part.name}")
            actual
        }

    /** Ambil teks kecil (mis. file `.sha256`). */
    suspend fun fetchText(url: String): String = withContext(Dispatchers.IO) {
        val c = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000; readTimeout = 30_000; instanceFollowRedirects = true
            setRequestProperty("User-Agent", "SallyIDE")
        }
        try {
            if (c.responseCode != 200) throw java.io.IOException("HTTP ${c.responseCode} untuk $url")
            c.inputStream.bufferedReader().use { it.readText() }
        } finally { c.disconnect() }
    }

    fun sha256Of(f: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        f.inputStream().use { i ->
            val buf = ByteArray(64 * 1024)
            while (true) { val n = i.read(buf); if (n < 0) break; md.update(buf, 0, n) }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
