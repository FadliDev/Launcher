package com.sally.launcher.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

private const val VERSION_MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"

data class MinecraftVersion(
    val id: String,
    val type: String,
    val url: String,
    val releaseTime: String
)

data class InstallResult(val versionId: String, val directory: File, val clientJar: File)

class MinecraftVersionRepository(context: Context) {
    private val root = File(context.filesDir, "sally/minecraft").apply { mkdirs() }

    suspend fun fetchVersions(): List<MinecraftVersion> = withContext(Dispatchers.IO) {
        val json = JSONObject(downloadText(VERSION_MANIFEST_URL))
        val versions = json.getJSONArray("versions")
        buildList {
            for (i in 0 until versions.length()) {
                val item = versions.getJSONObject(i)
                add(MinecraftVersion(
                    id = item.getString("id"),
                    type = item.getString("type"),
                    url = item.getString("url"),
                    releaseTime = item.optString("releaseTime", "")
                ))
            }
        }
    }

    /** Downloads version metadata and the client JAR. Full libraries/assets resolution is a later milestone. */
    suspend fun installClient(version: MinecraftVersion, onProgress: (String) -> Unit): InstallResult = withContext(Dispatchers.IO) {
        val versionDir = File(root, "versions/${version.id}").apply { mkdirs() }
        val metadataFile = File(versionDir, "${version.id}.json")
        onProgress("Mengunduh metadata ${version.id}…")
        downloadTo(version.url, metadataFile)

        val metadata = JSONObject(metadataFile.readText())
        val downloads = metadata.getJSONObject("downloads")
        if (!downloads.has("client")) throw IllegalStateException("Versi ${version.id} tidak menyediakan client JAR.")
        val client = downloads.getJSONObject("client")
        val jarFile = File(versionDir, "${version.id}.jar")
        onProgress("Mengunduh client JAR ${version.id}…")
        downloadTo(client.getString("url"), jarFile)

        val expectedSize = client.optLong("size", -1L)
        if (expectedSize > 0 && jarFile.length() != expectedSize) {
            jarFile.delete()
            throw IllegalStateException("Ukuran client JAR tidak cocok; unduhan dihapus dan bisa dicoba ulang.")
        }
        InstallResult(version.id, versionDir, jarFile)
    }

    private fun downloadText(url: String): String {
        val connection = open(url)
        return try { connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() } }
        finally { connection.disconnect() }
    }

    private fun downloadTo(url: String, destination: File) {
        val temp = File(destination.parentFile, destination.name + ".part")
        val connection = open(url)
        try {
            connection.inputStream.use { input -> temp.outputStream().buffered().use { output -> input.copyTo(output) } }
            if (destination.exists()) destination.delete()
            if (!temp.renameTo(destination)) throw IllegalStateException("Gagal menyimpan ${destination.name}")
        } catch (t: Throwable) {
            temp.delete()
            throw t
        } finally { connection.disconnect() }
    }

    private fun open(address: String): HttpURLConnection = (URL(address).openConnection() as HttpURLConnection).apply {
        connectTimeout = 15_000
        readTimeout = 45_000
        instanceFollowRedirects = true
        setRequestProperty("User-Agent", "SallyLauncher/0.1.0 (Android)")
        connect()
        if (responseCode !in 200..299) {
            disconnect()
            throw IllegalStateException("Server mengembalikan HTTP $responseCode")
        }
    }
}
