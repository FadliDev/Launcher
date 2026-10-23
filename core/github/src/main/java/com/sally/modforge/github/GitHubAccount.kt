package com.sally.modforge.github

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromString
import kotlinx.serialization.json.put
import okhttp3.FormBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.Base64

/**
 * Pintu masuk ke akun GitHub: login (Device Flow atau PAT), penyimpanan token,
 * dan operasi repo dasar. Tidak ada build di HP; semua build berjalan di Actions.
 */
class GitHubAccount(
    context: Context,
    private val client: GitHubClient = GitHubClient(),
) {
    private val vault = TokenVault(context.applicationContext)
    private val prefs = context.getSharedPreferences("github_settings", Context.MODE_PRIVATE)

    private val _session = MutableStateFlow<GitHubSession?>(null)
    val session: StateFlow<GitHubSession?> = _session.asStateFlow()

    /** OAuth Client ID publik dari OAuth App penerbit. Bukan client secret. */
    var clientId: String
        get() = prefs.getString(KEY_CLIENT_ID, "").orEmpty()
        set(value) {
            prefs.edit().putString(KEY_CLIENT_ID, value.trim()).apply()
        }

    /** Memulihkan sesi dari token tersimpan. Token yang ditolak GitHub (401) dihapus. */
    suspend fun restore() {
        if (_session.value != null) return
        val token = withContext(Dispatchers.IO) { vault.load() } ?: return
        try {
            _session.value = GitHubSession(fetchUser(token).login, token)
        } catch (e: GitHubException) {
            if (e.status == 401) logout()
        }
    }

    /** Login dengan Personal Access Token. */
    suspend fun loginWithToken(token: String): GitHubSession {
        require(token.isNotBlank()) { "Token kosong" }
        val user = fetchUser(token)
        withContext(Dispatchers.IO) { vault.save(token) }
        return GitHubSession(user.login, token).also { _session.value = it }
    }

    /** Langkah 1 Device Flow: minta kode yang harus dimasukkan user di github.com/login/device. */
    suspend fun startDeviceFlow(): DeviceCodeResponse {
        val id = clientId.ifBlank { error("OAuth Client ID belum diisi") }
        val form = FormBody.Builder()
            .add("client_id", id)
            .add("scope", SCOPES)
            .build()
        val raw = client.call("POST", "$GITHUB_WEB/login/device/code", body = form, headers = ACCEPT_JSON)
        return client.json.decodeFromString(raw)
    }

    /** Langkah 2 Device Flow: polling sampai user menyetujui, ditolak, atau kedaluwarsa. */
    suspend fun pollDeviceFlow(device: DeviceCodeResponse): GitHubSession {
        val id = clientId
        var intervalMs = device.interval.coerceAtLeast(5) * 1000L
        val deadline = System.currentTimeMillis() + device.expiresIn * 1000L

        while (System.currentTimeMillis() < deadline) {
            delay(intervalMs)
            val form = FormBody.Builder()
                .add("client_id", id)
                .add("device_code", device.deviceCode)
                .add("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
                .build()
            val res: TokenResponse = client.json.decodeFromString(
                client.call("POST", "$GITHUB_WEB/login/oauth/access_token", body = form, headers = ACCEPT_JSON)
            )
            val token = res.accessToken
            if (token != null) return loginWithToken(token)

            when (res.error) {
                "authorization_pending" -> Unit
                "slow_down" -> intervalMs += 5_000L
                "expired_token" -> error("Kode kedaluwarsa, coba lagi")
                "access_denied" -> error("Akses ditolak di GitHub")
                else -> error(res.errorDescription ?: "Login gagal (${res.error})")
            }
        }
        error("Waktu habis, coba lagi")
    }

    /** Membuat repo privat baru dan satu commit awal berisi README. */
    suspend fun createRepoWithReadme(name: String, isPrivate: Boolean = true): GhRepo {
        require(REPO_NAME.matches(name)) { "Nama repo hanya boleh huruf, angka, titik, strip, dan underscore" }
        val s = requireSession()
        val body = buildJsonObject {
            put("name", name)
            put("private", isPrivate)
            put("auto_init", false)
        }.toString().toRequestBody(JSON_MEDIA)

        val repo: GhRepo = client.json.decodeFromString(
            client.call("POST", "$GITHUB_API/user/repos", s.token, body)
        )
        putFile(
            owner = s.login,
            repo = repo.name,
            path = "README.md",
            text = "# ${repo.name}\n\nDibuat dari ModForge.\n",
            message = "Initial commit dari ModForge",
        )
        return repo
    }

    /** Membuat atau memperbarui satu file lewat Contents API. */
    suspend fun putFile(
        owner: String,
        repo: String,
        path: String,
        text: String,
        message: String,
        branch: String? = null,
    ) {
        val s = requireSession()
        val encodedPath = path.split('/').joinToString("/") {
            URLEncoder.encode(it, "UTF-8").replace("+", "%20")
        }
        val body = buildJsonObject {
            put("message", message)
            put("content", Base64.getEncoder().encodeToString(text.toByteArray(Charsets.UTF_8)))
            if (branch != null) put("branch", branch)
        }.toString().toRequestBody(JSON_MEDIA)

        client.call("PUT", "$GITHUB_API/repos/$owner/$repo/contents/$encodedPath", s.token, body)
    }

    /** Menghapus token lokal. Izin di GitHub tetap ada sampai dicabut di halaman REVOKE_URL. */
    fun logout() {
        _session.value = null
        vault.clear()
    }

    private suspend fun fetchUser(token: String): GhUser =
        client.json.decodeFromString(client.call("GET", "$GITHUB_API/user", token))

    private fun requireSession(): GitHubSession =
        _session.value ?: error("Belum login ke GitHub")

    companion object {
        const val SCOPES = "repo workflow"
        const val REVOKE_URL = "https://github.com/settings/applications"
        private const val KEY_CLIENT_ID = "client_id"
        private val ACCEPT_JSON = mapOf("Accept" to "application/json")
        private val REPO_NAME = Regex("^[A-Za-z0-9._-]{1,100}$")
    }
}
