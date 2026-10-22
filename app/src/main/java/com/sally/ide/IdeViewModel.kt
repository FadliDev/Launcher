package com.sally.ide

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sally.ide.data.Keys
import com.sally.ide.data.Prefs
import com.sally.ide.data.SettingsRepo
import com.sally.ide.data.ThemeMode
import com.sally.ide.editor.Lang
import com.sally.ide.editor.Snippet
import com.sally.ide.editor.SnippetEngine
import io.github.rosemoe.sora.text.Content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Satu tab editor. [content] dipegang per tab supaya undo/redo tidak hilang saat pindah tab. */
class EditorTab(val file: File, val content: Content) {
    var dirty by mutableStateOf(false)
    var cursorLine = 0
    var cursorCol = 0
    val lang: Lang = Lang.forFile(file.name)
}

data class TreeRow(val file: File, val depth: Int, val expanded: Boolean)

class IdeViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = SettingsRepo(app)
    val prefs = repo.flow.stateIn(viewModelScope, SharingStarted.Eagerly, Prefs())

    val workspace: File = File(app.getExternalFilesDir(null) ?: app.filesDir, "workspace")

    val tabs = mutableStateListOf<EditorTab>()
    var activeIndex by mutableIntStateOf(-1)
        private set
    val active: EditorTab? get() = tabs.getOrNull(activeIndex)

    var rows by mutableStateOf<List<TreeRow>>(emptyList())
        private set
    var selectedDir by mutableStateOf(workspace)
        private set
    private val expanded = HashSet<String>()

    var message by mutableStateOf<String?>(null)
    var pendingClose by mutableIntStateOf(-1)

    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { Seed.ensure(workspace) }
            refreshTree()
            File(workspace, "Main.kt").takeIf { it.exists() }?.let { openFile(it) }
        }
    }

    // ---------- tree ----------
    fun refreshTree() {
        viewModelScope.launch {
            rows = withContext(Dispatchers.IO) { buildRows() }
        }
    }

    private fun buildRows(): List<TreeRow> {
        val out = ArrayList<TreeRow>()
        fun walk(dir: File, depth: Int) {
            val kids = dir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: return
            for (f in kids) {
                val open = f.isDirectory && f.path in expanded
                out += TreeRow(f, depth, open)
                if (open) walk(f, depth + 1)
            }
        }
        walk(workspace, 0)
        return out
    }

    fun onRowClick(row: TreeRow) {
        if (row.file.isDirectory) {
            selectedDir = row.file
            if (!expanded.add(row.file.path)) expanded.remove(row.file.path)
            refreshTree()
        } else openFile(row.file)
    }

    // ---------- tabs / files ----------
    fun openFile(file: File) {
        val existing = tabs.indexOfFirst { it.file == file }
        if (existing >= 0) { activeIndex = existing; return }
        viewModelScope.launch {
            val text = withContext(Dispatchers.IO) { readText(file) }
            if (text == null) { message = "Tidak bisa membuka ${file.name} (bukan teks / terlalu besar)"; return@launch }
            val tab = EditorTab(file, Content(text))
            tabs += tab
            activeIndex = tabs.lastIndex
        }
    }

    private fun readText(file: File): String? {
        if (!file.isFile || file.length() > MAX_BYTES) return null
        val bytes = file.readBytes()
        if (bytes.take(4096).any { it == 0.toByte() }) return null
        return String(bytes, Charsets.UTF_8).replace("\r\n", "\n")
    }

    fun select(i: Int) { if (i in tabs.indices) activeIndex = i }

    fun requestClose(i: Int) {
        val t = tabs.getOrNull(i) ?: return
        if (t.dirty) pendingClose = i else closeNow(i)
    }

    fun closeNow(i: Int) {
        if (i !in tabs.indices) return
        tabs.removeAt(i)
        activeIndex = when {
            tabs.isEmpty() -> -1
            activeIndex > i -> activeIndex - 1
            activeIndex >= tabs.size -> tabs.lastIndex
            else -> activeIndex
        }
        pendingClose = -1
    }

    fun saveAndClose(i: Int) {
        val t = tabs.getOrNull(i) ?: return
        save(t) { closeNow(i) }
    }

    fun save(tab: EditorTab? = active, after: () -> Unit = {}) {
        val t = tab ?: return
        val text = t.content.toString() // ambil di main thread, tulis di IO
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { runCatching { t.file.writeText(text) }.isSuccess }
            if (ok) { t.dirty = false; after() } else message = "Gagal menyimpan ${t.file.name}"
        }
    }

    fun saveAll() {
        tabs.filter { it.dirty }.forEach { t ->
            val text = t.content.toString()
            runCatching { t.file.writeText(text); t.dirty = false }
        }
    }

    fun createFile(name: String) {
        val n = name.trim()
        if (n.isEmpty() || n.contains('/') || n.contains('\\') || n.startsWith(".."))
            { message = "Nama file tidak valid"; return }
        val f = File(selectedDir, n)
        viewModelScope.launch {
            val created = withContext(Dispatchers.IO) { runCatching { !f.exists() && f.createNewFile() }.getOrDefault(false) }
            if (!created) { message = "File sudah ada / gagal dibuat"; return@launch }
            expanded += selectedDir.path
            refreshTree()
            openFile(f)
        }
    }

    // ---------- snippets ----------
    fun snippetsFor(lang: Lang): List<Snippet> = SnippetEngine.load(getApplication(), lang)

    // ---------- settings ----------
    private fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        viewModelScope.launch { repo.edit(block) }
    }
    fun setThemeMode(m: ThemeMode) = edit { it[Keys.THEME] = m.name }
    fun setDynamic(v: Boolean) = edit { it[Keys.DYNAMIC] = v }
    fun setFontSize(v: Float) = edit { it[Keys.FONT] = v }
    fun setWordWrap(v: Boolean) = edit { it[Keys.WRAP] = v }
    fun setLineNumbers(v: Boolean) = edit { it[Keys.LINES] = v }
    fun setTabSize(v: Int) = edit { it[Keys.TAB] = v }

    companion object { const val MAX_BYTES = 5L * 1024 * 1024 }
}

/** Isi awal workspace supaya app langsung bisa dicoba. */
object Seed {
    fun ensure(root: File) {
        root.mkdirs()
        if (root.list()?.isNotEmpty() == true) return
        File(root, "Main.kt").writeText(
            "package com.sally.demo\n\n/** Selamat datang di Sally IDE */\nfun main() {\n    val nama = \"Sally\"\n    println(\"Halo, \$nama!\")\n}\n"
        )
        File(root, "Hello.java").writeText(
            "public class Hello {\n    public static void main(String[] args) {\n        System.out.println(\"Hello\");\n    }\n}\n"
        )
        File(root, "README.md").writeText("# Workspace\n\nKetik `fun` lalu pilih snippet di bar saran.\n")
    }
}
