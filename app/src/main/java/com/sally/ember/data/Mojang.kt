package com.sally.ember.data

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

object Mojang {
    private const val MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
    private const val RESOURCES = "https://resources.download.minecraft.net/"
    private val http = OkHttpClient()

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

        val total = items.size
        var done = 0
        val gate = Semaphore(12)
        coroutineScope {
            items.map { j ->
                async {
                    gate.withPermit { fetch(j) }
                    synchronized(this@Mojang) { done++; if (done % 20 == 0 || done == total) progress(v.id, done, total) }
                }
            }.awaitAll()
        }
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

    private fun fetch(j: Item) {
        if (j.file.exists() && (j.sha1.isEmpty() || sha1(j.file) == j.sha1)) return
        j.file.parentFile!!.mkdirs()
        repeat(3) { attempt ->
            runCatching {
                http.newCall(Request.Builder().url(j.url).build()).execute().use { r ->
                    check(r.isSuccessful) { "HTTP ${r.code} ${j.url}" }
                    val tmp = File(j.file.path + ".part")
                    r.body!!.byteStream().use { i -> tmp.outputStream().use { o -> i.copyTo(o) } }
                    check(tmp.renameTo(j.file)) { "rename failed" }
                }
            }.onSuccess { return }.onFailure { if (attempt == 2) throw it }
        }
    }

    private fun getText(url: String): String =
        http.newCall(Request.Builder().url(url).build()).execute().use { check(it.isSuccessful) { "HTTP ${it.code}" }; it.body!!.string() }

    private fun sha1(f: File): String {
        val md = MessageDigest.getInstance("SHA-1")
        f.inputStream().use { s -> val b = ByteArray(65536); while (true) { val n = s.read(b); if (n < 0) break; md.update(b, 0, n) } }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
