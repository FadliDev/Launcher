package com.sally.freshlauncher.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

object Installer {
    private const val ASSET_BASE = "https://resources.download.minecraft.net"

    suspend fun install(entry: VersionEntry, log: (String) -> Unit) = withContext(Dispatchers.IO) {
        val id = entry.id
        val verDir = File(GamePaths.versions, id).apply { mkdirs() }

        log("[$id] Mengunduh metadata")
        val jsonBytes = Net.get(entry.url)
        entry.sha1?.let { check(Net.sha1(jsonBytes) == it) { "SHA-1 metadata tidak cocok" } }
        File(verDir, "$id.json").writeBytes(jsonBytes)
        val json = JSONObject(String(jsonBytes))

        log("[$id] Mengunduh client.jar")
        val client = json.getJSONObject("downloads").getJSONObject("client")
        val jar = File(verDir, "$id.jar")
        Net.downloadVerified(client.getString("url"), jar, client.optStringOrNull("sha1"))
        val classpath = mutableListOf(jar.absolutePath)

        log("[$id] Mengunduh library")
        val libs = json.getJSONArray("libraries")
        for (i in 0 until libs.length()) {
            val lib = libs.getJSONObject(i)
            if (lib.has("natives")) continue // native belum didukung di skeleton ini
            if (!Rules.allowed(lib.optJSONArray("rules"))) continue
            val artifact = lib.optJSONObject("downloads")?.optJSONObject("artifact") ?: continue
            val file = File(GamePaths.libraries, artifact.getString("path"))
            Net.downloadVerified(artifact.getString("url"), file, artifact.optStringOrNull("sha1"))
            classpath += file.absolutePath
        }
        File(verDir, "classpath.txt").writeText(classpath.joinToString(":"))

        log("[$id] Mengunduh asset")
        val idx = json.getJSONObject("assetIndex")
        val indexFile = File(GamePaths.assets, "indexes/${idx.getString("id")}.json")
        Net.downloadVerified(idx.getString("url"), indexFile, idx.optStringOrNull("sha1"))
        val objects = JSONObject(indexFile.readText()).getJSONObject("objects")
        val hashes = objects.keys().asSequence()
            .map { objects.getJSONObject(it).getString("hash") }
            .distinct()
            .toList()

        val sem = Semaphore(8)
        coroutineScope {
            hashes.map { h ->
                async {
                    sem.withPermit {
                        val prefix = h.substring(0, 2)
                        Net.downloadVerified(
                            "$ASSET_BASE/$prefix/$h",
                            File(GamePaths.assets, "objects/$prefix/$h"),
                            h
                        )
                    }
                }
            }.awaitAll()
        }
        log("[$id] Asset: ${hashes.size} file")
        log("[$id] Selesai")
    }
}
