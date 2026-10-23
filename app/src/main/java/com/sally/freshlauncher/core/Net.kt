package com.sally.freshlauncher.core

import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

object Net {
    fun get(url: String): ByteArray {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 60_000
        conn.instanceFollowRedirects = true
        try {
            if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode} untuk $url")
            return conn.inputStream.use { it.readBytes() }
        } finally {
            conn.disconnect()
        }
    }

    fun sha1(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-1").digest(bytes).joinToString("") { "%02x".format(it) }

    /** Unduh ke [target] hanya jika belum ada atau SHA-1-nya tidak cocok. */
    fun downloadVerified(url: String, target: File, sha1: String?) {
        if (target.exists() && (sha1 == null || sha1(target.readBytes()) == sha1)) return
        val bytes = get(url)
        if (sha1 != null) check(sha1(bytes) == sha1) { "SHA-1 tidak cocok: $url" }
        target.parentFile?.mkdirs()
        target.writeBytes(bytes)
    }
}

internal fun JSONObject.optStringOrNull(key: String): String? = optString(key).ifEmpty { null }
