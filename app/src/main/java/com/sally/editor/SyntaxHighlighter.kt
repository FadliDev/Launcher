package com.sally.editor

import android.graphics.Color
import android.text.Editable
import android.text.Spanned
import android.text.style.ForegroundColorSpan

/** Lightweight syntax highlighter for common source files, including HTML/XML and CSS. */
object SyntaxHighlighter {
    private const val BLUE = 0xFF3973C6.toInt()
    private const val GREEN = 0xFF56845D.toInt()
    private const val ORANGE = 0xFFC07842.toInt()
    private const val PURPLE = 0xFF9565B5.toInt()
    private const val TEAL = 0xFF238A91.toInt()
    private const val RED = 0xFFB85450.toInt()
    private const val GRAY = 0xFF7D858D.toInt()

    private val htmlTag = Regex("</?\\s*[A-Za-z][A-Za-z0-9:-]*|/?>")
    private val htmlAttribute = Regex("\\b[A-Za-z_:][A-Za-z0-9:._-]*(?=\\s*=)")
    private val htmlAttributeValue = Regex("=\\s*(\"(?:\\\\.|[^\"])*\"|'(?:\\\\.|[^'])*')")
    private val htmlComment = Regex("<!--[\\s\\S]*?-->")
    private val htmlDoctype = Regex("(?i)<!doctype\\s+html[^>]*>")
    private val htmlEntity = Regex("&(?:#[0-9]+|#x[0-9a-fA-F]+|[A-Za-z][A-Za-z0-9]+);")
    private val cssSelector = Regex("(?m)(?:^|})(\\s*)([.#]?[A-Za-z_*][A-Za-z0-9_.*#:[\\]()>+~ -]*)(\\s*)(?=\\{)")
    private val cssProperty = Regex("\\b[a-zA-Z-]+(?=\\s*:)")
    private val keywordPattern = Regex("\\b(class|object|fun|val|var|if|else|when|for|while|return|public|private|protected|static|final|void|int|long|float|double|boolean|new|this|null|true|false|import|package|from|const|let|function|async|await|export|default|extends|implements|interface|def|print|try|catch|throw|switch|case|break|continue|typeof|instanceof|throw|yield)\\b")
    private val stringPattern = Regex("\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'")
    private val numberPattern = Regex("\\b\\d+(?:\\.\\d+)?\\b")
    private val commentPattern = Regex("//[^\\n]*|/\\*[\\s\\S]*?\\*/|#[^\\n]*")

    fun apply(editable: Editable, fileName: String = "") {
        val source = editable.toString()
        val length = editable.length
        editable.getSpans(0, length, ForegroundColorSpan::class.java).forEach(editable::removeSpan)
        if (length == 0) return

        val extension = fileName.substringAfterLast('.', "").lowercase()
        when (extension) {
            "html", "htm", "xhtml", "xml", "svg" -> applyMarkup(editable, source, extension == "html" || extension == "htm" || extension == "xhtml")
            "css", "scss", "less" -> applyCss(editable, source)
            else -> applyGeneric(editable, source)
        }
    }

    private fun applyMarkup(editable: Editable, source: String, isHtml: Boolean) {
        // Apply broad text tokens first; specific markup tokens are applied afterwards.
        applyMatches(editable, source, htmlComment, GREEN)
        applyMatches(editable, source, stringPattern, ORANGE)
        applyMatches(editable, source, numberPattern, PURPLE)
        applyMatches(editable, source, htmlEntity, TEAL)
        applyMatches(editable, source, htmlAttribute, RED)
        applyMatches(editable, source, htmlAttributeValue, ORANGE, valueGroup = 1)
        applyMatches(editable, source, htmlTag, BLUE)
        applyMatches(editable, source, htmlComment, GREEN)
        applyMatches(editable, source, htmlDoctype, PURPLE)
        if (isHtml) {
            // Highlight common embedded JavaScript/CSS keywords without replacing the markup colors.
            applyMatches(editable, source, Regex("(?i)\\b(script|style)\\b"), TEAL)
        }
    }

    private fun applyCss(editable: Editable, source: String) {
        applyMatches(editable, source, Regex("/\\*[\\s\\S]*?\\*/|//[^\\n]*"), GREEN)
        applyMatches(editable, source, stringPattern, ORANGE)
        applyMatches(editable, source, numberPattern, PURPLE)
        applyMatches(editable, source, cssSelector, BLUE, valueGroup = 2)
        applyMatches(editable, source, cssProperty, RED)
        applyMatches(editable, source, Regex("#[0-9a-fA-F]{3,8}\\b"), TEAL)
        applyMatches(editable, source, Regex("!important\\b"), PURPLE)
    }

    private fun applyGeneric(editable: Editable, source: String) {
        applyMatches(editable, source, commentPattern, GREEN)
        applyMatches(editable, source, stringPattern, ORANGE)
        applyMatches(editable, source, numberPattern, PURPLE)
        applyMatches(editable, source, keywordPattern, BLUE)
    }

    private fun applyMatches(
        editable: Editable,
        source: String,
        regex: Regex,
        color: Int,
        valueGroup: Int = 0
    ) {
        regex.findAll(source).forEach { match ->
            val range = match.groups[valueGroup]?.range ?: match.range
            if (range.first >= 0 && range.last < editable.length && !range.isEmpty()) {
                editable.setSpan(
                    ForegroundColorSpan(color),
                    range.first,
                    range.last + 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
    }
}
