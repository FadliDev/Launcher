package com.sally.editor

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.util.Locale
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/** Loads VS Code-format snippet packs bundled from rafamadriz/friendly-snippets. */
class FriendlySnippetRepository(context: Context) {
    private val appContext = context.applicationContext
    private val cache = mutableMapOf<String, List<CodeSnippet>>()
    private val executor = Executors.newSingleThreadExecutor()
    private val pendingCallbacks = mutableMapOf<String, MutableList<(List<CodeSnippet>) -> Unit>>()

    /**
     * Ensures snippet packs for this file are available. Bundled/cache files are used first;
     * missing packs are fetched from Friendly Snippets in the background and cached locally.
     * Callback is invoked on the worker thread; callers should switch to the UI thread.
     */
    fun ensureForFile(fileName: String, callback: (List<CodeSnippet>) -> Unit) {
        val existing = forFile(fileName)
        if (existing.isNotEmpty()) {
            callback(existing)
            return
        }
        val ext = extensionFor(fileName)
        val packs = packNamesFor(ext)
        if (packs.isEmpty()) {
            callback(emptyList())
            return
        }
        val key = packs.joinToString("|")
        val shouldStart = synchronized(pendingCallbacks) {
            val callbacks = pendingCallbacks.getOrPut(key) { mutableListOf() }
            callbacks.add(callback)
            callbacks.size == 1
        }
        if (!shouldStart) return
        executor.execute {
            val result = try {
                packs.forEach { pack -> downloadPackIfMissing(pack) }
                synchronized(cache) { cache.remove(ext) }
                forFile(fileName)
            } catch (error: Exception) {
                Log.w("SallySnippets", "Could not prepare snippet pack for $fileName", error)
                emptyList()
            }
            val callbacks = synchronized(pendingCallbacks) { pendingCallbacks.remove(key).orEmpty().toList() }
            callbacks.forEach { listener ->
                try { listener(result) } catch (error: Exception) {
                    Log.w("SallySnippets", "Snippet callback failed", error)
                }
            }
        }
    }

    private fun extensionFor(fileName: String): String {
        val lowerName = fileName.lowercase(Locale.ROOT)
        return when (lowerName) {
            "dockerfile" -> "dockerfile"
            "makefile", "gnumakefile" -> "makefile"
            else -> fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        }
    }

    private fun downloadPackIfMissing(packName: String) {
        val target = cachedPackFile(packName)
        if (target.isFile && target.length() > 2) return
        target.parentFile?.mkdirs()
        val connection = (URL("https://raw.githubusercontent.com/rafamadriz/friendly-snippets/main/snippets/$packName.json").openConnection() as HttpURLConnection).apply {
            connectTimeout = 12000
            readTimeout = 20000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "SallyTextEditor/1.0")
        }
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return
            val text = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            // Validate before caching so a 404 page or truncated response is never treated as snippets.
            val parsed = JSONObject(text)
            if (parsed.length() == 0) return
            val temp = File(target.parentFile, target.name + ".tmp")
            temp.writeText(text, Charsets.UTF_8)
            if (!temp.renameTo(target)) {
                target.writeText(text, Charsets.UTF_8)
                temp.delete()
            }
        } catch (error: Exception) {
            Log.i("SallySnippets", "No downloadable Friendly Snippets pack for $packName", error)
        } finally {
            connection.disconnect()
        }
    }

    private fun cachedPackFile(packName: String): File =
        File(appContext.filesDir, "friendly-snippets/snippets/$packName.json")

    fun forFile(fileName: String): List<CodeSnippet> {
        val ext = extensionFor(fileName)
        val packNames = packNamesFor(ext)
        return synchronized(cache) {
            cache.getOrPut(ext) {
                packNames.flatMap { readPack(it) }
                    .distinctBy { it.prefix to it.body }
                    .take(500)
            }
        }
    }

    private fun readPack(packName: String): List<CodeSnippet> {
        return try {
            val cached = cachedPackFile(packName)
            val json = if (cached.isFile && cached.length() > 2) {
                cached.readText(Charsets.UTF_8)
            } else {
                val path = "friendly-snippets/snippets/$packName.json"
                appContext.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
            }
            val root = JSONObject(json)
            buildList {
                val keys = root.keys()
                while (keys.hasNext()) {
                    val name = keys.next()
                    val item = root.optJSONObject(name) ?: continue
                    val prefixValue = item.opt("prefix")
                    val prefixes = when (prefixValue) {
                        is String -> listOf(prefixValue)
                        is org.json.JSONArray -> (0 until prefixValue.length()).mapNotNull { i -> prefixValue.optString(i).takeIf(String::isNotBlank) }
                        else -> emptyList()
                    }
                    val bodyValue = item.opt("body")
                    val bodyLines = when (bodyValue) {
                        is String -> listOf(bodyValue)
                        is org.json.JSONArray -> (0 until bodyValue.length()).map { i -> bodyValue.optString(i) }
                        else -> emptyList()
                    }
                    if (prefixes.isEmpty() || bodyLines.isEmpty()) continue
                    val body = normalizeSnippetBody(bodyLines.joinToString("\n"))
                    val description = item.optString("description", name).takeIf { it.isNotBlank() } ?: name
                    prefixes.forEach { prefix -> add(CodeSnippet(prefix, description, body)) }
                }
            }
        } catch (_: java.io.FileNotFoundException) {
            emptyList()
        } catch (error: Exception) {
            Log.w("SallySnippets", "Couldn't load Friendly Snippets pack: $packName", error)
            emptyList()
        }
    }

    /** Convert VS Code snippet placeholders to useful plain text for Sora's completion insertion. */
    private fun normalizeSnippetBody(source: String): String {
        return source
            .replace(Regex("\\$\\{\\d+:([^}]+)}")) { it.groupValues[1] }
            .replace(Regex("\\$\\{\\d+\\|([^}]*)\\|}")) { it.groupValues[1].substringBefore(',') }
            .replace(Regex("\\$\\{\\d+}"), "")
            .replace(Regex("\\$\\d+"), "")
            .replace(Regex("\\$\\{[^}]+}"), "")
    }

    private fun packNamesFor(ext: String): List<String> = when (ext) {
        "html", "htm", "xhtml" -> listOf("html")
        "css" -> listOf("css")
        "scss" -> listOf("scss", "css")
        "sass" -> listOf("sass", "scss", "css")
        "less" -> listOf("less", "css")
        "js", "mjs", "cjs" -> listOf("javascript")
        "jsx" -> listOf("javascriptreact", "javascript")
        "ts" -> listOf("typescript", "javascript")
        "tsx" -> listOf("typescriptreact", "typescript", "javascript")
        "py", "pyw", "pyi" -> listOf("python")
        "java" -> listOf("java")
        "kt", "kts" -> listOf("kotlin")
        "c", "h" -> listOf("c")
        "cc", "cpp", "cxx", "hpp", "hh", "hxx" -> listOf("cpp", "c")
        "cs" -> listOf("csharp")
        "go" -> listOf("go")
        "rs" -> listOf("rust")
        "rb", "rake", "gemspec" -> listOf("ruby")
        "php" -> listOf("php")
        "swift" -> listOf("swift")
        "dart" -> listOf("dart")
        "lua" -> listOf("lua")
        "sh", "bash", "zsh", "fish" -> listOf("shellscript")
        "json", "jsonc" -> listOf("json")
        "xml", "svg", "xsd", "xsl" -> listOf("xml")
        "sql" -> listOf("sql")
        "md", "markdown", "mdx" -> listOf("markdown")
        "yml", "yaml" -> listOf("yaml")
        "toml" -> listOf("toml")
        "ini", "conf" -> listOf("ini")
        "dockerfile" -> listOf("docker")
        "vue" -> listOf("vue")
        "svelte" -> listOf("svelte")
        "ex", "exs" -> listOf("elixir")
        "erl", "hrl" -> listOf("erlang")
        "hs" -> listOf("haskell")
        "jl" -> listOf("julia")
        "r" -> listOf("r")
        "scala" -> listOf("scala")
        "sol" -> listOf("solidity")
        "tex", "latex" -> listOf("latex")
        "pl", "pm" -> listOf("perl")
        "clj", "cljs", "cljc" -> listOf("clojure")
        "fs", "fsi", "fsx" -> listOf("fsharp")
        "gradle" -> listOf("groovy")
        "groovy" -> listOf("groovy")
        "tf", "tfvars" -> listOf("terraform")
        "graphql", "gql" -> listOf("graphql")
        "prisma" -> listOf("prisma")
        "astro" -> listOf("astro")
        "makefile", "mk" -> listOf("make")
        // Friendly Snippets often names packs after the language id. This fallback allows
        // newly added upstream languages to be downloaded without shipping a new app build.
        else -> ext.takeIf { it.isNotBlank() && it.matches(Regex("[a-z0-9_-]{1,40}")) }
            ?.let { listOf(it) } ?: emptyList()
    }
}
