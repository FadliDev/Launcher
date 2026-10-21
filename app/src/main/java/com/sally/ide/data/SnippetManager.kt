package com.sally.ide.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Reads VS Code-style snippet JSON, then downloads a pack only when no bundled pack exists. */
object SnippetManager {
    data class Snippet(
        val name: String,
        val prefix: String,
        val body: String,
        val description: String = ""
    )

    private val aliases = mapOf(
        "kt" to "kotlin", "kts" to "kotlin", "js" to "javascript", "jsx" to "javascript",
        "mjs" to "javascript", "cjs" to "javascript", "ts" to "typescript", "tsx" to "typescript",
        "htm" to "html", "scss" to "css", "less" to "css", "h" to "c", "cc" to "cpp",
        "cxx" to "cpp", "hpp" to "cpp", "hh" to "cpp", "hxx" to "cpp", "mdx" to "markdown",
        "yml" to "yaml", "bash" to "shell", "zsh" to "shell", "rs" to "rust", "rb" to "ruby",
        "kotlin-dsl" to "kotlin", "gradle" to "groovy", "properties" to "ini", "jsonc" to "json"
    )

    suspend fun load(context: Context, fileName: String): Pair<List<Snippet>, String> = withContext(Dispatchers.IO) {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        val language = aliases[extension] ?: extension
        if (language.isBlank()) return@withContext emptyList<Snippet>() to "File ini belum punya bahasa yang dikenali."

        val cached = File(context.filesDir, "snippet-packs/$language.json")
        try {
            val bundled = runCatching {
                context.assets.open("snippets/$language.json").bufferedReader().use { it.readText() }
            }.getOrNull()
            if (!bundled.isNullOrBlank()) return@withContext parse(bundled) to "Snippet bawaan · $language"

            if (cached.exists() && cached.length() > 2L) {
                val items = parse(cached.readText())
                if (items.isNotEmpty()) return@withContext items to "Snippet tersimpan · $language"
            }

            val url = "https://raw.githubusercontent.com/rafamadriz/friendly-snippets/main/snippets/$language.json"
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 7000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "SallyIDE-Android")
                setRequestProperty("Accept", "application/json")
            }
            try {
                if (connection.responseCode !in 200..299) {
                    return@withContext emptyList<Snippet>() to "Snippet pack $language belum tersedia; editor tetap bisa dipakai."
                }
                val json = connection.inputStream.bufferedReader().use { it.readText() }
                val parsed = parse(json)
                if (parsed.isNotEmpty()) {
                    cached.parentFile?.mkdirs()
                    cached.writeText(json)
                }
                parsed to if (parsed.isNotEmpty()) "Snippet diunduh · $language" else "Pack $language kosong."
            } finally {
                connection.disconnect()
            }
        } catch (_: Exception) {
            emptyList<Snippet>() to "Offline: snippet untuk $language belum tersedia di lokal."
        }
    }

    private fun parse(jsonText: String): List<Snippet> {
        val root = JSONObject(jsonText)
        val result = mutableListOf<Snippet>()
        val keys = root.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val raw = root.optJSONObject(key) ?: continue
            val prefixValue = raw.opt("prefix")
            val prefix = when (prefixValue) {
                is String -> prefixValue
                is JSONArray -> prefixValue.optString(0, "")
                else -> ""
            }.trim()
            if (prefix.isBlank()) continue
            val bodyValue = raw.opt("body")
            val body = when (bodyValue) {
                is String -> bodyValue
                is JSONArray -> (0 until bodyValue.length()).joinToString("\n") { bodyValue.optString(it) }
                else -> ""
            }
            if (body.isBlank()) continue
            val descriptionValue = raw.opt("description")
            val description = when (descriptionValue) {
                is String -> descriptionValue
                is JSONArray -> descriptionValue.optString(0, "")
                else -> ""
            }
            result += Snippet(key, prefix, body, description)
        }
        return result.sortedBy { it.prefix.lowercase() }
    }
}
