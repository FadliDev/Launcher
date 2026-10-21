package com.sally.ember.data

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

object Mojang {
    private const val MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
    private const val RESOURCES = "https://resources.download.minecraft.net/"
    // Default OkHttp read timeout is 10s and there is no resume, which is what made big installs fail on mobile data.
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(0, TimeUnit.MILLISECONDS) // no overall cap: large jars on slow links must not be cut off
        .retryOnConnectionFailure(true)
        .connectionPool(ConnectionPool(16, 5, TimeUnit.MINUTES))
        .build()
    private const val PARALLEL = 8
    private const val ATTEMPTS = 5

    suspend fun manifest(): List<McVersion> = withContext(Dispatchers.IO) {
        val arr = JSONObject(getText(MANIFEST)).getJSONArray("versions")
        (0 until arr.length()).map {
            val o = arr.getJSONObject(it); McVersion(o.getString("id"), o.getString("type"), o.getString("url"))
        }
    }

    fun isInstalled(id: String) = File(Paths.versions, "$id/$id.jar").exists() && File(Paths.versions, "$id/$id.json").exists()

    private data class Item(val url: String, val file: File, val sha1: String)

    /** Downloads version json, client jar, libraries and assets. [progress] gets (label, done, total). */
    suspend fun install(v: McVersion, progress: (String, Int, Int) -> Unit) = withContext(Dispatchers.IO) {
        val dir = File(Paths.versions, v.id).apply { mkdirs() }
        progress("Version JSON", 0, 1)
        val jsonFile = File(dir, "${v.id}.json")
        if (!jsonFile.exists()) jsonFile.writeText(getText(v.url))
        val json = JSONObject(jsonFile.readText())

        val items = mutableListOf<Item>()
        val client = json.getJSONObject("downloads").getJSONObject("client")
        items += Item(client.getString("url"), File(dir, "${v.id}.jar"), client.optString("sha1"))
        libraries(json).forEach { items += Item(it.first, File(Paths.libraries, it.second), it.third) }

        val ai = json.getJSONObject("assetIndex")
        val indexFile = File(Paths.assets, "indexes/${ai.getString("id")}.json")
        if (!indexFile.exists()) { indexFile.parentFile!!.mkdirs(); indexFile.writeText(getText(ai.getString("url"))) }
        val objects = JSONObject(indexFile.readText()).getJSONObject("objects")
        objects.keys().forEach { k ->
            val hash = objects.getJSONObject(k).getString("hash")
            items += Item(RESOURCES + hash.take(2) + "/" + hash, File(Paths.assets, "objects/${hash.take(2)}/$hash"), hash)
        }

        val todo = items.filterNot { isValid(it) }
        val total = items.size
        var done = total - todo.size
        progress(v.id, done, total)

        // Round 1 runs in parallel; anything that still fails is retried slowly in round 2 instead of aborting the whole install.
        var failed = download(todo, PARALLEL) { synchronized(this@Mojang) { done++; if (done % 20 == 0 || done == total) progress(v.id, done, total) } }
        if (failed.isNotEmpty()) {
            delay(3000)
            failed = download(failed.map { it.first }, 3) { synchronized(this@Mojang) { done++; if (done % 20 == 0 || done == total) progress(v.id, done, total) } }
        }
        if (failed.isNotEmpty()) {
            val (item, e) = failed.first()
            throw IOException("${failed.size} file gagal diunduh (koneksi lambat/timeout). Coba lagi, file yang sudah selesai tidak diunduh ulang. ${item.url}: ${e.message}", e)
        }
    }

    private suspend fun download(items: List<Item>, parallel: Int, onDone: () -> Unit): List<Pair<Item, Throwable>> = coroutineScope {
        val gate = Semaphore(parallel)
        items.map { j ->
            async {
                gate.withPermit {
                    runCatching { fetch(j) }
                        .onSuccess { onDone() }
                        .exceptionOrNull()?.let { if (it is CancellationException) throw it; j to it }
                }
            }
        }.awaitAll().filterNotNull()
    }

    /** (url, relativePath, sha1) for every library the JVM needs. LWJGL natives jars are skipped (Android natives come from the APK). */
    fun libraries(json: JSONObject): List<Triple<String, String, String>> {
        val out = mutableListOf<Triple<String, String, String>>()
        val libs = json.getJSONArray("libraries")
        for (i in 0 until libs.length()) {
            val l = libs.getJSONObject(i)
            if (!allowed(l.optJSONArray("rules"))) continue
            val art = l.optJSONObject("downloads")?.optJSONObject("artifact") ?: continue
            // Desktop native jars are useless here: the launcher ships Android natives (liblwjgl.so etc.) in the APK.
            if (art.getString("path").contains("-natives")) continue
            out += Triple(art.getString("url"), art.getString("path"), art.optString("sha1"))
        }
        return out
    }

    /** Android counts as "linux" for rules; feature-based rules (demo, custom resolution) are ignored. */
    private fun allowed(rules: JSONArray?): Boolean {
        if (rules == null) return true
        var ok = false
        for (i in 0 until rules.length()) {
            val r = rules.getJSONObject(i)
            val os = r.optJSONObject("os")
            val matches = (os == null || os.optString("name") == "linux") && !r.has("features")
            if (matches) ok = r.getString("action") == "allow"
        }
        return ok
    }

    private fun isValid(j: Item) = j.file.exists() && (j.sha1.isEmpty() || sha1(j.file) == j.sha1)

    /** Downloads to <file>.part, resuming with a Range request after a timeout/drop, then verifies SHA-1 and renames. */
    private fun fetch(j: Item) {
        if (isValid(j)) return
        j.file.parentFile!!.mkdirs()
        val part = File(j.file.path + ".part")
        var last: Throwable? = null
        for (attempt in 0 until ATTEMPTS) {
            try {
                val have = if (part.exists()) part.length() else 0L
                val req = Request.Builder().url(j.url).apply { if (have > 0) header("Range", "bytes=$have-") }.build()
                http.newCall(req).execute().use { r ->
                    if (r.code == 416) { part.delete(); throw IOException("HTTP 416, restart") }
                    check(r.isSuccessful) { "HTTP ${r.code} ${j.url}" }
                    val append = r.code == 206 && have > 0
                    r.body!!.byteStream().use { i -> java.io.FileOutputStream(part, append).use { o -> i.copyTo(o, 32 * 1024) } }
                }
                if (j.sha1.isNotEmpty() && sha1(part) != j.sha1) { part.delete(); throw IOException("SHA-1 mismatch ${j.url}") }
                j.file.delete()
                check(part.renameTo(j.file)) { "rename failed ${j.file}" }
                return
            } catch (e: IOException) {          // timeouts, resets, bad hash: retry (partial data is kept for resume)
                last = e
            } catch (e: IllegalStateException) { // non-2xx
                last = e
                if (e.message?.startsWith("HTTP 4") == true && !e.message!!.startsWith("HTTP 408") && !e.message!!.startsWith("HTTP 429")) throw e
            }
            Thread.sleep(minOf(1000L shl attempt, 8000L))
        }
        throw last ?: IOException("download failed ${j.url}")
    }

    private fun getText(url: String): String {
        var last: Throwable? = null
        for (attempt in 0 until ATTEMPTS) {
            try {
                return http.newCall(Request.Builder().url(url).build()).execute().use { check(it.isSuccessful) { "HTTP ${it.code}" }; it.body!!.string() }
            } catch (e: IOException) { last = e } catch (e: IllegalStateException) { last = e }
            Thread.sleep(minOf(1000L shl attempt, 8000L))
        }
        throw last!!
    }

    private fun sha1(f: File): String {
        val md = MessageDigest.getInstance("SHA-1")
        f.inputStream().use { s -> val b = ByteArray(65536); while (true) { val n = s.read(b); if (n < 0) break; md.update(b, 0, n) } }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
