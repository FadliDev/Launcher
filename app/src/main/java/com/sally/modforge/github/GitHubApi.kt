package com.sally.modforge.github

import android.content.Context
import com.sally.modforge.data.LocalProjectStore
import com.sally.modforge.data.ModProject
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.zip.ZipInputStream

class DeviceLogin(
    val deviceCode: String,
    val userCode: String,
    val verificationUri: String,
    val intervalSeconds: Int,
    val expiresInSeconds: Int
)

data class GitHubUser(val login: String, val name: String = "")
data class RemoteRepository(val name: String, val fullName: String, val isPrivate: Boolean, val htmlUrl: String)
data class BuildResult(val project: ModProject, val runUrl: String, val artifact: File?, val summary: String)

/** GitHub REST + OAuth Device Flow implementation. All callers must execute network calls off-main. */
class GitHubApi(private val context: Context, private val store: LocalProjectStore) {
    companion object {
        private const val API = "https://api.github.com"
        private const val DEVICE_CODE_URL = "https://github.com/login/device/code"
        private const val ACCESS_TOKEN_URL = "https://github.com/login/oauth/access_token"
    }

    fun beginDeviceLogin(clientId: String): DeviceLogin {
        require(clientId.isNotBlank()) { "Isi OAuth Client ID di Pengaturan dulu." }
        val form = "client_id=${enc(clientId)}&scope=${enc("repo workflow")}".toByteArray()
        val json = requestRaw(DEVICE_CODE_URL, "POST", form, null, "application/x-www-form-urlencoded")
        return DeviceLogin(
            deviceCode = json.getString("device_code"),
            userCode = json.getString("user_code"),
            verificationUri = json.optString("verification_uri", "https://github.com/login/device"),
            intervalSeconds = json.optInt("interval", 5).coerceAtLeast(1),
            expiresInSeconds = json.optInt("expires_in", 900).coerceAtLeast(1)
        )
    }

    suspend fun pollDeviceLogin(clientId: String, login: DeviceLogin, onStatus: (String) -> Unit): String {
        val deadline = System.currentTimeMillis() + login.expiresInSeconds * 1000L
        var interval = login.intervalSeconds
        while (System.currentTimeMillis() < deadline) {
            delay(interval * 1000L)
            val form = "client_id=${enc(clientId)}&device_code=${enc(login.deviceCode)}" +
                "&grant_type=${enc("urn:ietf:params:oauth:grant-type:device_code")}"
            val response = requestRaw(
                ACCESS_TOKEN_URL,
                "POST",
                form.toByteArray(),
                null,
                "application/x-www-form-urlencoded",
                allowHttpError = true
            )
            val token = response.optString("access_token")
            if (token.isNotBlank()) return token
            when (response.optString("error")) {
                "authorization_pending" -> onStatus("Menunggu konfirmasi di GitHub…")
                "slow_down" -> { interval += 5; onStatus("GitHub meminta jeda lebih lama…") }
                "access_denied" -> error("Login ditolak di GitHub.")
                "expired_token" -> error("Kode login kedaluwarsa. Mulai lagi.")
                "incorrect_client_credentials" -> error("OAuth Client ID tidak valid.")
                else -> {
                    val description = response.optString("error_description")
                    if (description.isNotBlank()) error("Login GitHub: $description")
                    onStatus("Menghubungi GitHub…")
                }
            }
        }
        error("Kode login kedaluwarsa. Mulai lagi.")
    }

    fun getUser(token: String): GitHubUser {
        val json = apiJson(token, "GET", "/user")
        return GitHubUser(json.optString("login"), json.optString("name", ""))
    }

    fun listRepositories(token: String): List<RemoteRepository> {
        val json = JSONArray(apiText(token, "GET", "/user/repos?per_page=100&sort=updated"))
        return (0 until json.length()).mapNotNull { i ->
            val repo = json.optJSONObject(i) ?: return@mapNotNull null
            RemoteRepository(
                name = repo.optString("name"),
                fullName = repo.optString("full_name"),
                isPrivate = repo.optBoolean("private"),
                htmlUrl = repo.optString("html_url")
            )
        }
    }

    suspend fun buildProject(
        token: String,
        source: ModProject,
        onStatus: (String) -> Unit
    ): BuildResult {
        onStatus("Memeriksa akun GitHub…")
        val user = getUser(token)
        var project = source
        if (project.repoOwner.isBlank() || project.repoName.isBlank()) {
            onStatus("Membuat repository ${project.modId}…")
            val repo = createRepository(token, project)
            project = project.copy(
                repoOwner = repo.optJSONObject("owner")?.optString("login").orEmpty().ifBlank { user.login },
                repoName = repo.optString("name", project.modId),
                branch = repo.optString("default_branch", "main")
            )
            if (project.repoOwner.isBlank()) project = project.copy(repoOwner = user.login)
            store.saveProject(project)
        }

        onStatus("Menyinkronkan file project ke GitHub…")
        val files = store.readSourceFiles(project)
        require(files.isNotEmpty()) { "Project belum memiliki file untuk disinkronkan." }
        val commitSha = pushTree(token, project, files)
        project = project.copy(lastSyncedSha = commitSha)
        store.saveProject(project)

        onStatus("Menunggu GitHub Actions…")
        var run: JSONObject? = null
        for (attempt in 0 until 120) {
            val runs = apiJson(token, "GET", "/repos/${project.repoOwner}/${project.repoName}/actions/runs?head_sha=$commitSha&per_page=10")
                .optJSONArray("workflow_runs") ?: JSONArray()
            run = (0 until runs.length()).mapNotNull { runs.optJSONObject(it) }
                .firstOrNull { it.optString("head_sha") == commitSha }
            if (run != null) break
            if (attempt < 119) delay(3_000)
        }
        val buildRun = run ?: error("Commit sudah dikirim, tetapi workflow tidak muncul. Pastikan token memiliki izin workflow dan GitHub Actions aktif di repo.")
        val runId = buildRun.getLong("id")
        var freshRun = buildRun
        for (attempt in 0 until 120) {
            freshRun = apiJson(token, "GET", "/repos/${project.repoOwner}/${project.repoName}/actions/runs/$runId")
            val status = freshRun.optString("status")
            val conclusion = freshRun.optString("conclusion")
            onStatus(when {
                status != "completed" -> "GitHub Actions: ${status.replaceFirstChar(Char::uppercase)}…"
                conclusion == "success" -> "Build sukses, mengunduh JAR…"
                else -> "Build gagal, mengambil log…"
            })
            if (status == "completed") break
            if (attempt < 119) delay(5_000)
        }
        val runUrl = freshRun.optString("html_url", "https://github.com/${project.repoOwner}/${project.repoName}/actions")
        if (freshRun.optString("status") != "completed") error("Build masih berjalan. Buka Actions untuk melihat progres: $runUrl")
        if (freshRun.optString("conclusion") != "success") {
            val log = getBuildFailureSummary(token, project, runId)
            error("Build gagal.\n$log\n\nLihat detail: $runUrl")
        }

        val artifactJson = apiJson(token, "GET", "/repos/${project.repoOwner}/${project.repoName}/actions/runs/$runId/artifacts")
            .optJSONArray("artifacts") ?: JSONArray()
        val artifact = (0 until artifactJson.length()).mapNotNull { artifactJson.optJSONObject(it) }
            .firstOrNull { it.optBoolean("expired") == false && it.optString("name").contains(project.modId, true) }
            ?: (0 until artifactJson.length()).mapNotNull { artifactJson.optJSONObject(it) }.firstOrNull { !it.optBoolean("expired") }
        val downloaded = artifact?.let { downloadArtifact(token, project, it.getLong("id")) }
        return BuildResult(project, runUrl, downloaded, if (downloaded != null) "Build sukses: ${downloaded.name}" else "Build sukses, tetapi artifact JAR tidak ditemukan.")
    }

    private fun createRepository(token: String, project: ModProject): JSONObject {
        val body = JSONObject()
            .put("name", project.modId)
            .put("description", project.description)
            .put("private", project.visibility != "public")
            .put("auto_init", true)
        // Do not silently attach to an unrelated repository when the requested name already exists.
        return apiJson(token, "POST", "/user/repos", body)
    }

    private fun pushTree(token: String, project: ModProject, files: Map<String, ByteArray>): String {
        val owner = project.repoOwner
        val repo = project.repoName
        val branch = project.branch.ifBlank { "main" }
        val ref = apiJson(token, "GET", "/repos/$owner/$repo/git/ref/heads/${encPath(branch)}")
        val parentSha = ref.getJSONObject("object").getString("sha")
        if (project.lastSyncedSha.isNotBlank() && parentSha != project.lastSyncedSha) {
            throw IOException("Repository berubah di GitHub sejak sync terakhir. Build dihentikan agar perubahan remote tidak tertimpa; pull/merge belum tersedia di versi MVP ini.")
        }
        val parentCommit = apiJson(token, "GET", "/repos/$owner/$repo/git/commits/$parentSha")
        val baseTree = parentCommit.getJSONObject("tree").getString("sha")
        val treeEntries = JSONArray()
        files.toSortedMap().forEach { (path, bytes) ->
            val content = String(bytes, Charsets.UTF_8)
            val blobBody = JSONObject().put("content", content).put("encoding", "utf-8")
            val blob = apiJson(token, "POST", "/repos/$owner/$repo/git/blobs", blobBody)
            treeEntries.put(JSONObject().put("path", path).put("mode", "100644").put("type", "blob").put("sha", blob.getString("sha")))
        }
        val tree = apiJson(token, "POST", "/repos/$owner/$repo/git/trees", JSONObject().put("base_tree", baseTree).put("tree", treeEntries))
        val commit = apiJson(token, "POST", "/repos/$owner/$repo/git/commits", JSONObject()
            .put("message", "Update ${project.modId} from ModForge")
            .put("tree", tree.getString("sha"))
            .put("parents", JSONArray().put(parentSha)))
        val sha = commit.getString("sha")
        apiJson(token, "PATCH", "/repos/$owner/$repo/git/refs/heads/${encPath(branch)}", JSONObject().put("sha", sha).put("force", false))
        return sha
    }

    private suspend fun downloadArtifact(token: String, project: ModProject, artifactId: Long): File? {
        val connection = URL("$API/repos/${project.repoOwner}/${project.repoName}/actions/artifacts/$artifactId/zip").openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = true
        connection.connectTimeout = 20_000
        connection.readTimeout = 60_000
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        connection.connect()
        if (connection.responseCode !in 200..299) {
            val code = connection.responseCode
            connection.disconnect()
            throw IOException("Gagal mengunduh artifact (HTTP $code)")
        }
        val output = File(store.projectDir(project.id), ".modforge/outputs").apply { mkdirs() }
        var result: File? = null
        ZipInputStream(connection.inputStream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.endsWith(".jar", true)) {
                    val target = File(output, File(entry.name).name)
                    target.outputStream().use { out -> zip.copyTo(out) }
                    result = target
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        connection.disconnect()
        return result
    }

    private fun getBuildFailureSummary(token: String, project: ModProject, runId: Long): String {
        return runCatching {
            val jobs = apiJson(token, "GET", "/repos/${project.repoOwner}/${project.repoName}/actions/runs/$runId/jobs")
                .optJSONArray("jobs") ?: JSONArray()
            val lines = mutableListOf<String>()
            for (i in 0 until jobs.length()) {
                val job = jobs.optJSONObject(i) ?: continue
                lines += "Job: ${job.optString("name")} — ${job.optString("conclusion", job.optString("status"))}"
                val steps = job.optJSONArray("steps") ?: JSONArray()
                for (j in 0 until steps.length()) {
                    val step = steps.optJSONObject(j) ?: continue
                    if (step.optString("conclusion") == "failure") lines += "• ${step.optString("name")}: gagal"
                }
            }
            lines.joinToString("\n").ifBlank { "GitHub Actions gagal. Buka log run untuk detail." }
        }.getOrDefault("GitHub Actions gagal. Buka log run untuk detail.")
    }

    private fun apiJson(token: String, method: String, path: String, body: JSONObject? = null): JSONObject =
        JSONObject(apiText(token, method, path, body))

    private fun apiText(token: String, method: String, path: String, body: JSONObject? = null): String =
        requestRaw("$API$path", method, body?.toString()?.toByteArray(Charsets.UTF_8), token, "application/json")
            .toString()

    private fun requestRaw(
        url: String,
        method: String,
        body: ByteArray? = null,
        token: String? = null,
        contentType: String = "application/json",
        allowHttpError: Boolean = false
    ): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = 20_000
        connection.readTimeout = 40_000
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "ModForge-Android")
        connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
        if (body != null) {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", contentType)
            connection.outputStream.use { it.write(body) }
        }
        return try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299 && !allowHttpError) {
                val message = runCatching { JSONObject(response).optString("message") }.getOrDefault(response.take(200))
                throw IOException("GitHub API HTTP $status: $message")
            }
            if (response.isBlank()) JSONObject() else JSONObject(response)
        } finally {
            connection.disconnect()
        }
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")
    private fun encPath(value: String): String = value.split('/').joinToString("/") { enc(it) }
}
