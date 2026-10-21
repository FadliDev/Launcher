package com.sally.editor

import android.graphics.Color
import android.text.Editable
import android.text.Spanned
import android.text.style.ForegroundColorSpan

/** Lightweight first-pass syntax coloring; deliberately keeps editing in Android's native Editable. */
object SyntaxHighlighter {
    private val keywordPattern = Regex("\\b(class|object|fun|val|var|if|else|when|for|while|return|public|private|protected|static|final|void|int|long|float|double|boolean|new|this|null|true|false|import|package|from|const|let|function|async|await|export|default|extends|implements|interface|def|print|try|catch|throw)\\b")
    private val stringPattern = Regex("\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'")
    private val numberPattern = Regex("\\b\\d+(?:\\.\\d+)?\\b")
    private val commentPattern = Regex("//[^\\n]*|/\\*[\\s\\S]*?\\*/|#[^\\n]*")

    fun apply(editable: Editable) {
        val length = editable.length
        editable.getSpans(0, length, ForegroundColorSpan::class.java).forEach(editable::removeSpan)
        val source = editable.toString()
        applyMatches(editable, source, commentPattern, Color.rgb(110, 140, 115))
        applyMatches(editable, source, stringPattern, Color.rgb(190, 120, 75))
        applyMatches(editable, source, numberPattern, Color.rgb(160, 115, 205))
        applyMatches(editable, source, keywordPattern, Color.rgb(80, 120, 210))
    }

    private fun applyMatches(editable: Editable, source: String, regex: Regex, color: Int) {
        regex.findAll(source).forEach { match ->
            editable.setSpan(ForegroundColorSpan(color), match.range.first, match.range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
}
