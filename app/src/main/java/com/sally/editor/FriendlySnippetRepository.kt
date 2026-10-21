package com.sally.editor

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.util.Locale

/** Loads VS Code-format snippet packs bundled from rafamadriz/friendly-snippets. */
class FriendlySnippetRepository(context: Context) {
    private val appContext = context.applicationContext
    private val cache = mutableMapOf<String, List<CodeSnippet>>()

    fun forFile(fileName: String): List<CodeSnippet> {
        val lowerName = fileName.lowercase(Locale.ROOT)
        val ext = when (lowerName) {
            "dockerfile" -> "dockerfile"
            "makefile", "gnumakefile" -> "makefile"
            else -> fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        }
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
            val path = "friendly-snippets/snippets/$packName.json"
            val json = appContext.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
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
        else -> emptyList()
    }
}
