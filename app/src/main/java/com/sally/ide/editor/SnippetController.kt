package com.sally.ide.editor

import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion

/**
 * Every toolbar insertion (autocomplete chips, snippet dialog, extra keys) goes through here.
 *
 * Why not CodeEditor.insertText: it was called right after setSelection() with Sora's own completion
 * thread still alive, so the editor was mutated from two sides at once. Here we do ONE Content.replace()
 * on the main thread, place the cursor explicitly, and never throw to the caller (Result instead).
 */
class SnippetController {
    private var stops: List<IntArray> = emptyList()
    private var next = 0
    private var current: IntArray? = null
    private var lastLength = 0

    fun reset() {
        stops = emptyList()
        next = 0
        current = null
    }

    /** Replaces the selection, or the typed word before the cursor when it is a prefix of [matchPrefix]. */
    fun insert(editor: CodeEditor?, snippet: ExpandedSnippet, matchPrefix: String = "", fileName: String = ""): Result<Unit> =
        runCatching {
            reset()
            val ed = usable(editor)
            val content = ed.text
            val cursor = content.cursor
            val startLine = cursor.leftLine
            var startCol = cursor.leftColumn
            val endLine = cursor.rightLine
            val endCol = cursor.rightColumn
            val lineText = content.getLineString(startLine)
            if (!cursor.isSelected && matchPrefix.isNotEmpty()) {
                val typed = wordBefore(lineText, startCol)
                if (typed.isNotEmpty() && matchPrefix.startsWith(typed, ignoreCase = true)) startCol -= typed.length
            }
            val indent = lineText.takeWhile { it == ' ' || it == '\t' }
            val expanded = SnippetParser.applyIndent(snippet, indent)
            val startIndex = content.indexer.getCharIndex(startLine, startCol)

            content.replace(startLine, startCol, endLine, endCol, expanded.text)

            val absolute = expanded.stops.map { intArrayOf(startIndex + it.start, startIndex + it.end) }
            if (absolute.size > 1) {
                stops = absolute
                next = 1
                current = absolute[0]
                lastLength = content.length
            }
            select(ed, absolute.first()[0], absolute.first()[1])
            ed.requestFocus()
            Unit
        }

    fun insertText(editor: CodeEditor?, text: String, matchPrefix: String = ""): Result<Unit> =
        insert(editor, ExpandedSnippet.plain(text), matchPrefix)

    /** TAB while a snippet is active: jump to the next tabstop. False means "not handled, do a normal TAB". */
    fun jumpNext(editor: CodeEditor?): Boolean {
        val ed = editor ?: return false
        val cur = current
        if (cur == null || next >= stops.size) {
            reset()
            return false
        }
        return runCatching {
            usable(ed)
            val content = ed.text
            val delta = content.length - lastLength
            val caret = content.indexer.getCharIndex(content.cursor.leftLine, content.cursor.leftColumn)
            if (caret < cur[0] || caret > cur[1] + delta) {
                reset()
                return@runCatching false
            }
            val oldEnd = cur[1]
            for (i in next until stops.size) {
                if (stops[i][0] >= oldEnd) {
                    stops[i][0] += delta
                    stops[i][1] += delta
                }
            }
            val target = stops[next]
            next++
            current = target
            lastLength = content.length
            select(ed, target[0], target[1])
            if (next >= stops.size) reset()
            true
        }.getOrElse {
            reset()
            false
        }
    }

    private fun select(editor: CodeEditor, start: Int, end: Int) {
        val content = editor.text
        val max = content.length
        val s = content.indexer.getCharPosition(start.coerceIn(0, max))
        if (start >= end) {
            editor.setSelection(s.line, s.column, true)
        } else {
            val e = content.indexer.getCharPosition(end.coerceIn(0, max))
            editor.setSelectionRegion(s.line, s.column, e.line, e.column)
        }
    }

    companion object {
        /** Typed identifier right before [column]; used by chips and to know what a snippet should replace. */
        fun wordBefore(line: String, column: Int): String {
            val safe = column.coerceIn(0, line.length)
            var i = safe
            while (i > 0 && (line[i - 1].isLetterOrDigit() || line[i - 1] == '_' || line[i - 1] == '$')) i--
            return line.substring(i, safe)
        }

        fun usable(editor: CodeEditor?): CodeEditor {
            val ed = editor ?: error("Editor belum siap")
            check(ed.isAttachedToWindow) { "Editor sedang tidak aktif" }
            return ed
        }

        /** Our own chip bar replaces Sora's popup; its background completion thread raced with toolbar edits. */
        fun disableBuiltInCompletion(editor: CodeEditor) {
            runCatching { editor.getComponent(EditorAutoCompletion::class.java).isEnabled = false }
        }

        fun moveCursor(editor: CodeEditor?, dLine: Int, dColumn: Int) {
            runCatching {
                val ed = usable(editor)
                val content = ed.text
                var line = content.cursor.leftLine
                var col = content.cursor.leftColumn
                if (dLine != 0) {
                    line = (line + dLine).coerceIn(0, content.lineCount - 1)
                    col = col.coerceAtMost(content.getColumnCount(line))
                } else {
                    col += dColumn
                    if (col < 0 && line > 0) {
                        line--
                        col = content.getColumnCount(line)
                    } else if (col > content.getColumnCount(line) && line < content.lineCount - 1) {
                        line++
                        col = 0
                    }
                    col = col.coerceIn(0, content.getColumnCount(line))
                }
                ed.setSelection(line, col, true)
                ed.requestFocus()
            }
        }
    }
}
