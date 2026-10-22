package com.sally.ide.ui

import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.sally.ide.EditorTab
import com.sally.ide.data.Prefs
import com.sally.ide.editor.EditorRuntime
import com.sally.ide.editor.Lang
import com.sally.ide.editor.Snippet
import com.sally.ide.editor.SnippetEngine
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import io.github.rosemoe.sora.widget.EditorSearcher

/** Pegangan ke CodeEditor untuk aksi dari toolbar / key row / find bar. Semua dipanggil di main thread. */
class EditorHandle {
    var editor: CodeEditor? = null

    fun insert(text: String) { editor?.insertText(text, text.length) }
    fun undo() { editor?.takeIf { it.canUndo() }?.undo() }
    fun redo() { editor?.takeIf { it.canRedo() }?.redo() }
    fun left() { editor?.moveSelectionLeft() }
    fun right() { editor?.moveSelectionRight() }
    fun up() { editor?.moveSelectionUp() }
    fun down() { editor?.moveSelectionDown() }
    fun home() { editor?.moveSelectionHome() }
    fun end() { editor?.moveSelectionEnd() }

    fun cursor(): Pair<Int, Int> = editor?.cursor?.let { it.leftLine + 1 to it.leftColumn + 1 } ?: (1 to 1)

    fun gotoLine(line1: Int) {
        val ed = editor ?: return
        val ln = (line1 - 1).coerceIn(0, ed.text.lineCount - 1)
        ed.setSelection(ln, 0)
    }

    /** Awalan identifier tepat sebelum kursor (kosong jika ada seleksi). */
    fun prefix(): String {
        val ed = editor ?: return ""
        val c = ed.cursor
        if (c.isSelected) return ""
        val line = ed.text.getLineString(c.leftLine)
        val col = c.leftColumn.coerceAtMost(line.length)
        return Regex("[A-Za-z_][A-Za-z0-9_]*$").find(line.substring(0, col))?.value ?: ""
    }

    fun textForIndex(max: Int): String? {
        val ed = editor ?: return null
        return if (ed.text.length > max) null else ed.text.toString()
    }

    /** Ganti [prefixLen] karakter sebelum kursor dengan teks biasa. */
    fun replacePrefix(prefixLen: Int, replacement: String) {
        val ed = editor ?: return
        val c = ed.cursor
        val ln = c.leftLine
        ed.text.replace(ln, c.leftColumn - prefixLen, ln, c.leftColumn, replacement)
    }

    /** Sisipkan snippet; ganti [prefixLen] karakter sebelum kursor (atau seleksi bila ada). */
    fun insertSnippet(snippet: Snippet, prefixLen: Int, tabSize: Int) {
        val ed = editor ?: return
        val c = ed.cursor
        val startLine = c.leftLine
        val startCol = if (c.isSelected) c.leftColumn else c.leftColumn - prefixLen
        val endLine = c.rightLine
        val endCol = c.rightColumn
        val lineText = ed.text.getLineString(startLine)
        val indent = lineText.takeWhile { it == ' ' || it == '\t' }
        val exp = SnippetEngine.expand(snippet.body, indent, " ".repeat(tabSize))
        ed.text.replace(startLine, startCol, endLine, endCol, exp.text)
        val (l1, c1) = offsetToPos(startLine, startCol, exp.text, exp.selStart)
        val (l2, c2) = offsetToPos(startLine, startCol, exp.text, exp.selEnd)
        runCatching { ed.setSelectionRegion(l1, c1, l2, c2) }
    }

    private fun offsetToPos(line: Int, col: Int, text: String, offset: Int): Pair<Int, Int> {
        val before = text.substring(0, offset)
        val nl = before.count { it == '\n' }
        return if (nl == 0) line to col + before.length else line + nl to before.length - (before.lastIndexOf('\n') + 1)
    }

    // ---- find & replace ----
    fun search(query: String, ignoreCase: Boolean, regex: Boolean): Boolean {
        val s = editor?.searcher ?: return true
        if (query.isEmpty()) { s.stopSearch(); return true }
        return runCatching { s.search(query, EditorSearcher.SearchOptions(ignoreCase, regex)) }.isSuccess
    }
    fun next() { editor?.searcher?.takeIf { it.hasQuery() }?.gotoNext() }
    fun prev() { editor?.searcher?.takeIf { it.hasQuery() }?.gotoPrevious() }
    fun replaceCurrent(r: String) { editor?.searcher?.takeIf { it.hasQuery() }?.replaceCurrentMatch(r) }
    fun replaceAll(r: String) { editor?.searcher?.takeIf { it.hasQuery() }?.replaceAll(r) }
    fun stopSearch() { editor?.searcher?.stopSearch() }
}

private class Host {
    var tab: EditorTab? = null
    var theme: String? = null
    var lang: Lang? = null
    var font = -1f
    var wrap: Boolean? = null
    var lines: Boolean? = null
    var tabSize = -1
}

@Composable
fun CodeEditorView(
    tab: EditorTab,
    prefs: Prefs,
    editorTheme: String,
    handle: EditorHandle,
    onChanged: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val changed by rememberUpdatedState(onChanged)
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            EditorRuntime.init(ctx)
            CodeEditor(ctx).also { ed ->
                val host = Host()
                ed.tag = host
                ed.typefaceText = Typeface.MONOSPACE
                // Completion popup bawaan dimatikan; saran muncul di SuggestionBar (lebih aman di layar kecil).
                ed.getComponent(EditorAutoCompletion::class.java).isEnabled = false
                ed.subscribeEvent(ContentChangeEvent::class.java) { ev, _ ->
                    if (ev.action != ContentChangeEvent.ACTION_SET_NEW_TEXT) host.tab?.dirty = true
                    changed()
                }
                ed.subscribeEvent(SelectionChangeEvent::class.java) { _, _ -> changed() }
                handle.editor = ed
            }
        },
        update = { ed ->
            val host = ed.tag as Host
            if (host.theme != editorTheme) {
                host.theme = editorTheme
                ed.colorScheme = EditorRuntime.colorScheme(editorTheme)
                host.lang = null // paksa set ulang bahasa supaya warna token ikut tema
            }
            if (host.tab !== tab) {
                host.tab?.let { old ->
                    old.cursorLine = ed.cursor.leftLine
                    old.cursorCol = ed.cursor.leftColumn
                }
                host.tab = null
                ed.setText(tab.content)
                host.tab = tab
                host.lang = null
                runCatching {
                    val ln = tab.cursorLine.coerceIn(0, ed.text.lineCount - 1)
                    ed.setSelection(ln, tab.cursorCol.coerceIn(0, ed.text.getColumnCount(ln)))
                }
            }
            if (host.lang != tab.lang) {
                host.lang = tab.lang
                ed.setEditorLanguage(EditorRuntime.language(tab.lang))
            }
            if (host.font != prefs.fontSize) { host.font = prefs.fontSize; ed.setTextSize(prefs.fontSize) }
            if (host.wrap != prefs.wordWrap) { host.wrap = prefs.wordWrap; ed.isWordwrap = prefs.wordWrap }
            if (host.lines != prefs.lineNumbers) { host.lines = prefs.lineNumbers; ed.isLineNumberEnabled = prefs.lineNumbers }
            if (host.tabSize != prefs.tabSize) { host.tabSize = prefs.tabSize; ed.tabWidth = prefs.tabSize }
        },
        onRelease = { ed ->
            handle.editor = null
            ed.release()
        },
    )
}
