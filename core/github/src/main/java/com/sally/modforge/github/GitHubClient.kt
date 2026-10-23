package com.sally.modforge.github

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

const val GITHUB_API = "https://api.github.com"
const val GITHUB_WEB = "https://github.com"
val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

private const val MAX_ATTEMPTS = 3

/** Error HTTP dari GitHub. Token tidak pernah ikut dimasukkan ke pesan. */
class GitHubException(val status: Int, message: String) : IOException(message)

class GitHubClient(
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build(),
    val json: Json = Json { ignoreUnknownKeys = true; explicitNulls = false },
) {
    /**
     * Mengirim request dengan retry + backoff untuk error jaringan, 5xx,
     * dan rate limit (429 / 403 dengan sisa kuota 0).
     */
    suspend fun call(
        method: String,
        url: String,
        token: String? = null,
        body: RequestBody? = null,
        headers: Map<String, String> = emptyMap(),
    ): String = withContext(Dispatchers.IO) {
        callWithRetry(method, url, token, body, headers)
    }

    private suspend fun callWithRetry(
        method: String,
        url: String,
        token: String?,
        body: RequestBody?,
        headers: Map<String, String>,
    ): String {
        var attempt = 1
        while (true) {
            val outcome = runCatching { send(method, url, token, body, headers) }
            val failure = outcome.exceptionOrNull()
            if (failure == null) {
                val res = outcome.getOrThrow()
                if (res.code in 200..299) return res.body
                if (!res.retryable() || attempt >= MAX_ATTEMPTS) {
                    throw GitHubException(res.code, res.errorMessage())
                }
                delay(res.waitMillis(attempt))
            } else {
                if (failure !is IOException || attempt >= MAX_ATTEMPTS) throw failure
                delay(backoff(attempt))
            }
            attempt++
        }
    }

    private fun send(
        method: String,
        url: String,
        token: String?,
        body: RequestBody?,
        extra: Map<String, String>,
    ): RawResponse {
        val builder = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "ModForge-Android")
        if (token != null) builder.header("Authorization", "Bearer $token")
        extra.forEach { (k, v) -> builder.header(k, v) }
        val payload = body ?: if (method == "GET") null else ByteArray(0).toRequestBody()
        builder.method(method, payload)

        http.newCall(builder.build()).execute().use { r ->
            return RawResponse(r.code, r.body?.string().orEmpty(), r.headers)
        }
    }
}

private class RawResponse(val code: Int, val body: String, val headers: Headers)

private fun RawResponse.retryable(): Boolean =
    code >= 500 ||
        code == 429 ||
        (code == 403 && (headers["x-ratelimit-remaining"] == "0" || headers["retry-after"] != null))

private fun RawResponse.waitMillis(attempt: Int): Long {
    headers["retry-after"]?.toLongOrNull()?.let { return (it * 1000L).coerceAtMost(60_000L) }
    headers["x-ratelimit-reset"]?.toLongOrNull()?.let { reset ->
        return (reset * 1000L - System.currentTimeMillis()).coerceIn(1_000L, 60_000L)
    }
    return backoff(attempt)
}

private fun RawResponse.errorMessage(): String =
    Regex("\"message\"\\s*:\\s*\"([^\"]*)\"").find(body)?.groupValues?.get(1)
        ?: "Permintaan ke GitHub gagal (HTTP $code)"

private fun backoff(attempt: Int): Long = (1_000L shl attempt).coerceAtMost(30_000L)
