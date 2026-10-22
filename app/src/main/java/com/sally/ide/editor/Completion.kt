package com.sally.ide.editor

enum class Kind(val tag: String) { SNIPPET("{ }"), KEYWORD("kw"), WORD("abc") }

data class Suggestion(val label: String, val kind: Kind, val snippet: Snippet? = null)

/** Completion ringan (keyword + snippet + kata di file). Berjalan di luar UI thread. */
object Completion {
    private val word = Regex("[A-Za-z_][A-Za-z0-9_]{2,}")
    const val MAX_INDEXED_CHARS = 300_000

    fun suggest(prefix: String, lang: Lang, snippets: List<Snippet>, text: String?, limit: Int = 14): List<Suggestion> {
        if (prefix.isEmpty()) return emptyList()
        val out = ArrayList<Suggestion>()
        val seen = HashSet<String>()
        fun ok(s: String) = s.length >= prefix.length && s.startsWith(prefix, ignoreCase = true) && !s.equals(prefix, false)

        snippets.filter { ok(it.prefix) || it.prefix == prefix }.sortedBy { it.prefix.length }.forEach {
            if (seen.add("s:" + it.prefix)) out += Suggestion(it.prefix, Kind.SNIPPET, it)
        }
        lang.keywords.filter { ok(it) }.sorted().forEach { if (seen.add(it)) out += Suggestion(it, Kind.KEYWORD) }
        if (text != null && out.size < limit) {
            word.findAll(text).map { it.value }.filter { ok(it) }.distinct().sorted().forEach {
                if (seen.add(it)) out += Suggestion(it, Kind.WORD)
            }
        }
        return out.take(limit)
    }
}
