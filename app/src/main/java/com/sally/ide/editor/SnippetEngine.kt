package com.sally.ide.editor

import android.content.Context
import org.json.JSONObject

data class Snippet(val name: String, val prefix: String, val body: String, val description: String)

/** Hasil ekspansi: teks final + rentang seleksi (placeholder pertama / $0) relatif terhadap awal teks. */
data class Expansion(val text: String, val selStart: Int, val selEnd: Int)

/**
 * Engine snippet format VSCode: $1, ${1:default}, $0, escape \$.
 * Fase 1: placeholder pertama dipilih otomatis, mirror ikut default. Navigasi tabstop berikutnya = backlog.
 */
object SnippetEngine {
    private val stop = Regex("""\$\{(\d+)(?::([^}]*))?\}|\$(\d+)""")

    fun expand(raw: String, indent: String, tabUnit: String): Expansion {
        val body = raw.replace("\\\$", "\u0000").replace("\t", tabUnit).replace("\n", "\n" + indent)
        val sb = StringBuilder()
        val stops = sortedMapOf<Int, IntArray>()
        var last = 0
        for (m in stop.findAll(body)) {
            sb.append(body, last, m.range.first)
            val idx = (m.groups[1]?.value ?: m.groups[3]!!.value).toInt()
            val def = m.groups[2]?.value ?: ""
            val start = sb.length
            sb.append(def)
            if (idx !in stops) stops[idx] = intArrayOf(start, sb.length)
            last = m.range.last + 1
        }
        sb.append(body, last, body.length)
        val text = sb.toString().replace('\u0000', '$')
        val target = stops.entries.firstOrNull { it.key > 0 }?.value ?: stops[0]
        return if (target != null) Expansion(text, target[0], target[1]) else Expansion(text, text.length, text.length)
    }

    private val cache = HashMap<String, List<Snippet>>()

    @Synchronized
    fun load(ctx: Context, lang: Lang): List<Snippet> {
        val file = lang.snippetFile ?: return emptyList()
        return cache.getOrPut(file) {
            runCatching {
                val json = JSONObject(ctx.assets.open("snippets/$file.json").bufferedReader().use { it.readText() })
                json.keys().asSequence().map { name ->
                    val o = json.getJSONObject(name)
                    val b = o.opt("body")
                    val body = if (b is org.json.JSONArray) (0 until b.length()).joinToString("\n") { b.getString(it) } else b.toString()
                    Snippet(name, o.optString("prefix", name), body, o.optString("description", ""))
                }.toList()
            }.getOrDefault(emptyList())
        }
    }
}
