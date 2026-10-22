package com.sally.modforge.github

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.delay
import kotlinx.serialization.json.*
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.security.MessageDigest

class GitHubException(val code: Int, msg: String) : IOException(msg)

data class DeviceCode(val deviceCode: String, val userCode: String, val verificationUri: String, val interval: Int)

fun JsonObject.str(k: String): String? = (this[k] as? JsonPrimitive)?.contentOrNull
fun JsonObject.long(k: String): Long = this[k]!!.jsonPrimitive.long

fun gitBlobSha(b: ByteArray): String {
    val md = MessageDigest.getInstance("SHA-1")
    md.update("blob ${b.size}\u0000".toByteArray())
    md.update(b)
    return md.digest().joinToString("") { "%02x".format(it) }
}

/** Token + client id disimpan terenkripsi (Android Keystore). */
class Secrets(ctx: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        ctx, "secrets",
        MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
    var token: String?
        get() = prefs.getString("token", null)
        set(v) { prefs.edit().putString("token", v).apply() }
    var login: String?
        get() = prefs.getString("login", null)
        set(v) { prefs.edit().putString("login", v).apply() }
    var clientId: String
        get() = prefs.getString("client_id", "") ?: ""
        set(v) { prefs.edit().putString("client_id", v).apply() }
}

class GitHubClient(private val token: () -> String?) {
    private val api = "https://api.github.com"
    private val http = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    private val jsonType = "application/json".toMediaType()

    private fun req(method: String, url: String, body: JsonElement? = null): JsonElement {
        val b = Request.Builder().url(url)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
        token()?.let { b.header("Authorization", "Bearer $it") }
        val rb = when {
            body != null -> json.encodeToString(JsonElement.serializer(), body).toRequestBody(jsonType)
            method == "GET" -> null
            else -> "".toRequestBody(jsonType)
        }
        b.method(method, rb)
        http.newCall(b.build()).execute().use { r ->
            val txt = r.body?.string().orEmpty()
            if (!r.isSuccessful) throw GitHubException(r.code, "GitHub ${r.code}: ${txt.take(300)}")
            return if (txt.isBlank()) JsonNull else json.parseToJsonElement(txt)
        }
    }

    // ---- OAuth Device Flow (tanpa client secret) ----
    private fun form(url: String, vararg kv: Pair<String, String>): JsonObject {
        val fb = FormBody.Builder().apply { kv.forEach { add(it.first, it.second) } }.build()
        val rq = Request.Builder().url(url).header("Accept", "application/json").post(fb).build()
        http.newCall(rq).execute().use { r -> return json.parseToJsonElement(r.body!!.string()).jsonObject }
    }

    fun startDeviceFlow(clientId: String): DeviceCode {
        val o = form("https://github.com/login/device/code", "client_id" to clientId, "scope" to "repo workflow")
        val code = o.str("device_code") ?: throw IOException(o.str("error_description") ?: "Client ID salah / Device Flow belum diaktifkan")
        return DeviceCode(code, o.str("user_code")!!, o.str("verification_uri")!!, o["interval"]?.jsonPrimitive?.int ?: 5)
    }

    suspend fun pollToken(clientId: String, dc: DeviceCode): String {
        var wait = dc.interval
        while (true) {
            delay(wait * 1000L)
            val o = form(
                "https://github.com/login/oauth/access_token",
                "client_id" to clientId, "device_code" to dc.deviceCode,
                "grant_type" to "urn:ietf:params:oauth:grant-type:device_code",
            )
            o.str("access_token")?.let { return it }
            when (o.str("error")) {
                "authorization_pending" -> {}
                "slow_down" -> wait += 5
                else -> throw IOException(o.str("error_description") ?: "Login GitHub gagal")
            }
        }
    }

    fun user(): String = req("GET", "$api/user").jsonObject.str("login")!!

    // ---- Repo & commit (Git Data API: atomik, tanpa git lokal) ----
    /** auto_init=true supaya Git Data API bisa dipakai (repo kosong menolak blob/tree). */
    fun createRepo(name: String, private: Boolean): Pair<String, String> {
        val r = req("POST", "$api/user/repos", buildJsonObject {
            put("name", name); put("private", private); put("auto_init", true)
            put("description", "Created by ModForge Studio")
        }).jsonObject
        return r.str("full_name")!! to (r.str("default_branch") ?: "main")
    }

    fun head(repo: String, branch: String): String? = try {
        req("GET", "$api/repos/$repo/git/ref/heads/$branch").jsonObject["object"]!!.jsonObject.str("sha")
    } catch (e: GitHubException) {
        if (e.code == 404 || e.code == 409) null else throw e
    }

    private fun blob(repo: String, bytes: ByteArray): String =
        req("POST", "$api/repos/$repo/git/blobs", buildJsonObject {
            put("content", Base64.encodeToString(bytes, Base64.NO_WRAP)); put("encoding", "base64")
        }).jsonObject.str("sha")!!

    /** Tree dibangun penuh dari file lokal (tanpa base_tree) -> repo = persis isi project. Blob lama dipakai ulang. */
    fun commit(
        repo: String, branch: String, files: Map<String, ByteArray>, shas: Map<String, String>,
        known: Map<String, String>, message: String, parent: String?,
    ): String {
        val entries = files.keys.map { path ->
            val sha = if (known[path] == shas[path]) shas[path]!! else blob(repo, files[path]!!)
            buildJsonObject { put("path", path); put("mode", "100644"); put("type", "blob"); put("sha", sha) }
        }
        val tree = req("POST", "$api/repos/$repo/git/trees", buildJsonObject {
            put("tree", JsonArray(entries))
        }).jsonObject.str("sha")!!
        val c = req("POST", "$api/repos/$repo/git/commits", buildJsonObject {
            put("message", message); put("tree", tree)
            put("parents", buildJsonArray { if (parent != null) add(parent) })
        }).jsonObject.str("sha")!!
        if (parent == null) {
            req("POST", "$api/repos/$repo/git/refs", buildJsonObject { put("ref", "refs/heads/$branch"); put("sha", c) })
        } else {
            req("PATCH", "$api/repos/$repo/git/refs/heads/$branch", buildJsonObject { put("sha", c); put("force", false) })
        }
        return c
    }

    // ---- Actions ----
    fun dispatch(repo: String, branch: String) {
        req("POST", "$api/repos/$repo/actions/workflows/build.yml/dispatches", buildJsonObject { put("ref", branch) })
    }

    fun runs(repo: String, branch: String): List<JsonObject> =
        req("GET", "$api/repos/$repo/actions/runs?branch=$branch&per_page=10")
            .jsonObject["workflow_runs"]!!.jsonArray.map { it.jsonObject }

    fun run(repo: String, id: Long): JsonObject = req("GET", "$api/repos/$repo/actions/runs/$id").jsonObject

    fun artifacts(repo: String, runId: Long): List<JsonObject> =
        req("GET", "$api/repos/$repo/actions/runs/$runId/artifacts")
            .jsonObject["artifacts"]!!.jsonArray.map { it.jsonObject }

    fun download(url: String, dest: File) {
        val rq = Request.Builder().url(url).header("Authorization", "Bearer ${token()}").build()
        http.newCall(rq).execute().use { r ->
            if (!r.isSuccessful) throw GitHubException(r.code, "Unduh artifact gagal (${r.code})")
            dest.outputStream().use { o -> r.body!!.byteStream().copyTo(o) }
        }
    }

    /** Log job yang gagal (ekor 6000 karakter) untuk panel error. */
    fun failedLog(repo: String, runId: Long): String = runCatching {
        val jobs = req("GET", "$api/repos/$repo/actions/runs/$runId/jobs").jsonObject["jobs"]!!.jsonArray
        val job = jobs.map { it.jsonObject }.firstOrNull { it.str("conclusion") == "failure" } ?: return@runCatching ""
        val rq = Request.Builder().url("$api/repos/$repo/actions/jobs/${job.long("id")}/logs")
            .header("Authorization", "Bearer ${token()}").build()
        http.newCall(rq).execute().use { it.body?.string().orEmpty().takeLast(6000) }
    }.getOrDefault("")
}
