package com.sally.ide.editor

import java.util.Calendar

/** One tabstop of an expanded snippet. Offsets are character indexes inside [ExpandedSnippet.text]. */
data class SnippetStop(val index: Int, val start: Int, val end: Int)

/** Snippet body after expanding placeholders; [stops] are in visiting order and always end with `$0`. */
data class ExpandedSnippet(val text: String, val stops: List<SnippetStop>) {
    companion object {
        /** Plain text (keywords, symbols): no tabstops except the end position. */
        fun plain(text: String) = ExpandedSnippet(text, listOf(SnippetStop(0, text.length, text.length)))
    }
}

/**
 * Small VS Code snippet parser: `$1`, `${1}`, `${1:default}` (nested too), `${1|a,b|}` (first choice),
 * `$0`, `\$` escapes and the common `TM_FILENAME`-style variables. Pure Kotlin so it can be unit tested.
 */
object SnippetParser {
    fun expand(body: String, fileName: String = ""): ExpandedSnippet {
        val parser = Parser(body, fileName)
        parser.parse(nested = false)
        val text = parser.out.toString()
        val ordered = parser.stops.filter { it.index > 0 }.sortedBy { it.index }.toMutableList()
        ordered += parser.stops.firstOrNull { it.index == 0 } ?: SnippetStop(0, text.length, text.length)
        return ExpandedSnippet(text, ordered)
    }

    /** Prefixes every line after the first with [indent], remapping the tabstop offsets. */
    fun applyIndent(snippet: ExpandedSnippet, indent: String): ExpandedSnippet {
        if (indent.isEmpty() || !snippet.text.contains('\n')) return snippet
        val text = snippet.text
        val map = IntArray(text.length + 1)
        val sb = StringBuilder(text.length + indent.length * 4)
        for (i in text.indices) {
            map[i] = sb.length
            sb.append(text[i])
            if (text[i] == '\n') sb.append(indent)
        }
        map[text.length] = sb.length
        return ExpandedSnippet(sb.toString(), snippet.stops.map { SnippetStop(it.index, map[it.start], map[it.end]) })
    }

    private class Parser(private val src: String, private val fileName: String) {
        var pos = 0
        val out = StringBuilder()
        val stops = mutableListOf<SnippetStop>()
        private val seen = HashMap<Int, String>()

        fun parse(nested: Boolean) {
            while (pos < src.length) {
                val c = src[pos]
                if (c == '\\' && pos + 1 < src.length && "\$}\\".indexOf(src[pos + 1]) >= 0) {
                    out.append(src[pos + 1])
                    pos += 2
                } else if (c == '}' && nested) {
                    return
                } else if (c == '$') {
                    if (!parseDollar()) {
                        out.append('$')
                        pos++
                    }
                } else {
                    out.append(c)
                    pos++
                }
            }
        }

        private fun parseDollar(): Boolean {
            val next = src.getOrNull(pos + 1) ?: return false
            if (next.isDigit()) {
                var p = pos + 1
                while (p < src.length && src[p].isDigit()) p++
                val index = src.substring(pos + 1, p).toInt()
                pos = p
                emitSimple(index)
                return true
            }
            if (next == '{') return parseBraced()
            if (next.isLetter() || next == '_') {
                var p = pos + 1
                while (p < src.length && (src[p].isLetterOrDigit() || src[p] == '_')) p++
                val name = src.substring(pos + 1, p)
                pos = p
                out.append(variable(name) ?: name)
                return true
            }
            return false
        }

        private fun parseBraced(): Boolean {
            val p = pos + 2
            if (p >= src.length) return false
            if (src[p].isDigit()) {
                var q = p
                while (q < src.length && src[q].isDigit()) q++
                val index = src.substring(p, q).toInt()
                when (src.getOrNull(q)) {
                    '}' -> {
                        pos = q + 1
                        emitSimple(index)
                        return true
                    }
                    ':' -> {
                        pos = q + 1
                        val start = out.length
                        parse(nested = true)
                        if (pos < src.length && src[pos] == '}') pos++
                        record(index, start)
                        return true
                    }
                    '|' -> {
                        val endChoice = src.indexOf("|}", q + 1)
                        if (endChoice < 0) return false
                        val first = src.substring(q + 1, endChoice).substringBefore(',')
                        pos = endChoice + 2
                        val start = out.length
                        out.append(first)
                        record(index, start)
                        return true
                    }
                    else -> return false
                }
            }
            if (src[p].isLetter() || src[p] == '_') {
                var q = p
                while (q < src.length && (src[q].isLetterOrDigit() || src[q] == '_')) q++
                val name = src.substring(p, q)
                when (src.getOrNull(q)) {
                    '}' -> {
                        pos = q + 1
                        out.append(variable(name) ?: name)
                        return true
                    }
                    ':' -> {
                        pos = q + 1
                        val start = out.length
                        parse(nested = true)
                        if (pos < src.length && src[pos] == '}') pos++
                        val value = variable(name)
                        if (value != null) {
                            out.setLength(start)
                            out.append(value)
                        }
                        return true
                    }
                    else -> return false
                }
            }
            return false
        }

        /** `$N` / `${N}`: first use creates a stop, later uses mirror the first one's text. */
        private fun emitSimple(index: Int) {
            val mirror = seen[index]
            if (mirror != null) {
                out.append(mirror)
            } else {
                stops += SnippetStop(index, out.length, out.length)
                seen[index] = ""
            }
        }

        private fun record(index: Int, start: Int) {
            val end = out.length
            if (!seen.containsKey(index)) {
                stops += SnippetStop(index, start, end)
                seen[index] = out.substring(start, end)
            }
        }

        private fun variable(name: String): String? = when (name) {
            "TM_FILENAME" -> fileName
            "TM_FILENAME_BASE" -> fileName.substringBeforeLast('.')
            "CURRENT_YEAR" -> Calendar.getInstance().get(Calendar.YEAR).toString()
            "TM_SELECTED_TEXT", "TM_CURRENT_LINE", "TM_CURRENT_WORD", "TM_LINE_INDEX", "TM_DIRECTORY",
            "TM_FILEPATH", "CLIPBOARD", "WORKSPACE_NAME", "LINE_COMMENT", "BLOCK_COMMENT_START",
            "BLOCK_COMMENT_END" -> ""
            else -> null
        }
    }
}
