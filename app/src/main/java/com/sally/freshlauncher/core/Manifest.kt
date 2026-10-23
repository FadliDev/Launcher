package com.sally.freshlauncher.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class VersionEntry(
    val id: String,
    val type: String,
    val url: String,
    val sha1: String?
)

object VersionManifest {
    private const val URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"

    suspend fun fetch(): List<VersionEntry> = withContext(Dispatchers.IO) {
        val arr = JSONObject(String(Net.get(URL))).getJSONArray("versions")
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            VersionEntry(
                id = o.getString("id"),
                type = o.getString("type"),
                url = o.getString("url"),
                sha1 = o.optStringOrNull("sha1")
            )
        }
    }
}
