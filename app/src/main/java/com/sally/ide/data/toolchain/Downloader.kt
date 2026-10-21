package com.sally.ide.data.toolchain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * HTTPS-only downloader with resume (Range), progress, SHA-256 verification and cancellation.
 * The `.part` file stays on disk when cancelled or interrupted, so the next attempt continues from there.
 */
object Downloader {
    class DownloadResult(val file: File, val sha256: String)

    suspend fun download(
        url: String,
        target: File,
        partial: File,
        expectedSha256: String? = null,
        onProgress: (done: Long, total: Long) -> Unit
    ): DownloadResult = withContext(Dispatchers.IO) {
        if (!url.startsWith("https://")) throw IOException("Hanya HTTPS yang diizinkan: $url")
        target.parentFile?.mkdirs()
        partial.parentFile?.mkdirs()

        var attempt = 0
        while (true) {
            attempt++
            val existing = if (partial.exists()) partial.length() else 0L
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "SallyIDE-Android")
                if (existing > 0) setRequestProperty("Range", "bytes=$existing-")
            }
            try {
                val code = connection.responseCode
                if (code == 416 && attempt == 1) {
                    partial.delete() // stale partial, start over once
                    continue
                }
                if (code != 200 && code != 206) throw IOException("Server membalas HTTP $code")

                val resumed = code == 206 && existing > 0
                val offset = if (resumed) existing else 0L
                val total = totalLength(connection, code, offset)
                if (total > 0) {
                    val free = (target.parentFile ?: partial.parentFile)?.usableSpace ?: Long.MAX_VALUE
                    if (free < total - offset + 64L * 1024 * 1024) throw IOException("Ruang penyimpanan tidak cukup")
                }

                RandomAccessFile(partial, "rw").use { raf ->
                    if (!resumed) raf.setLength(0)
                    raf.seek(offset)
                    var done = offset
                    var lastReport = 0L
                    val buffer = ByteArray(64 * 1024)
                    connection.inputStream.use { input ->
                        while (true) {
                            ensureActive()
                            val read = input.read(buffer)
                            if (read < 0) break
                            raf.write(buffer, 0, read)
                            done += read
                            val now = System.nanoTime()
                            if (now - lastReport > 150_000_000L) {
                                lastReport = now
                                onProgress(done, total)
                            }
                        }
                    }
                    onProgress(done, if (total > 0) total else done)
                }

                val digest = sha256(partial)
                if (expectedSha256 != null && !digest.equals(expectedSha256.trim(), ignoreCase = true)) {
                    partial.delete()
                    throw IOException("Checksum tidak cocok (diharapkan ${expectedSha256.trim().take(12)}…, dapat ${digest.take(12)}…)")
                }
                if (target.exists()) target.delete()
                if (!partial.renameTo(target)) {
                    partial.copyTo(target, overwrite = true)
                    partial.delete()
                }
                return@withContext DownloadResult(target, digest)
            } finally {
                connection.disconnect()
            }
        }
        throw IOException("Unduhan gagal")
    }

    /** Small text resource (checksum files, JSON indexes). */
    suspend fun fetchText(url: String, accept: String = "*/*"): String = withContext(Dispatchers.IO) {
        if (!url.startsWith("https://")) throw IOException("Hanya HTTPS yang diizinkan: $url")
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 20_000
            setRequestProperty("User-Agent", "SallyIDE-Android")
            setRequestProperty("Accept", accept)
        }
        try {
            if (connection.responseCode !in 200..299) throw IOException("HTTP ${connection.responseCode}")
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                md.update(buffer, 0, read)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun totalLength(connection: HttpURLConnection, code: Int, offset: Long): Long {
        if (code == 206) {
            val range = connection.getHeaderField("Content-Range") // bytes 100-999/1000
            val total = range?.substringAfter('/', "")?.toLongOrNull()
            if (total != null) return total
            val length = connection.contentLengthLong
            return if (length > 0) offset + length else -1L
        }
        return connection.contentLengthLong
    }
}
