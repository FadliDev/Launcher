package com.sally.ide

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sally.ide.data.Fs
import com.sally.ide.data.Hit
import com.sally.ide.data.Keys
import com.sally.ide.data.Prefs
import com.sally.ide.data.ProjectInfo
import com.sally.ide.data.ProjectSearch
import com.sally.ide.data.ProjectStore
import com.sally.ide.data.SettingsRepo
import com.sally.ide.data.Template
import com.sally.ide.data.Toolchain
import com.sally.ide.data.ToolchainModel
import com.sally.ide.data.ThemeMode
import com.sally.ide.editor.Lang
import com.sally.ide.editor.Snippet
import com.sally.ide.editor.SnippetEngine
import io.github.rosemoe.sora.text.Content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Satu tab editor. [content] dipegang per tab supaya undo/redo tidak hilang saat pindah tab. */
class EditorTab(file: File, val content: Content) {
    var file by mutableStateOf(file)
    var dirty by mutableStateOf(false)
    var cursorLine = 0
    var cursorCol = 0
    val lang: Lang get() = Lang.forFile(file.name)
}

data class TreeRow(val file: File, val depth: Int, val expanded: Boolean, val isDir: Boolean)

class IdeViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = SettingsRepo(app)
    val prefs = repo.flow.stateIn(viewModelScope, SharingStarted.Eagerly, Prefs())
    private val store = ProjectStore(File(app.getExternalFilesDir(null) ?: app.filesDir, "workspace"))

    // ---------- toolchain (Fase 3) ----------
    val toolchain = ToolchainModel(Toolchain(File(app.filesDir, "toolchain")), viewModelScope) { message = it }
    var showToolchain by mutableStateOf(false)

    // ---------- beranda ----------
    var loaded by mutableStateOf(false)
        private set
    var projects by mutableStateOf<List<ProjectInfo>>(emptyList())
        private set

    // ---------- proyek terbuka ----------
    var project by mutableStateOf<File?>(null)
        private set
    val tabs = mutableStateListOf<EditorTab>()
    var activeIndex by mutableIntStateOf(-1)
        private set
    val active: EditorTab? get() = tabs.getOrNull(activeIndex)

    var rows by mutableStateOf<List<TreeRow>>(emptyList())
        private set
    /** Folder tujuan untuk "file baru" dari header explorer. */
    var targetDir by mutableStateOf<File?>(null)
        private set
    private val expanded = HashSet<String>()

    var message by mutableStateOf<String?>(null)
    var pendingClose by mutableIntStateOf(-1)
    /** Nomor baris (1-based) yang harus dilompati setelah file terbuka; dikonsumsi UI. */
    var pendingGoto by mutableStateOf<Int?>(null)

    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { store.migrateLegacy() }
            reloadProjects()
            loaded = true
        }
    }

    // =====================================================================
    // Proyek
    // =====================================================================
    fun reloadProjects() {
        viewModelScope.launch { projects = withContext(Dispatchers.IO) { store.list() } }
    }

    fun createProject(name: String, template: Template) {
        val n = name.trim()
        viewModelScope.launch {
            val dir = withContext(Dispatchers.IO) { store.create(n, template) }
            if (dir == null) { message = "Nama tidak valid atau sudah dipakai"; return@launch }
            reloadProjects()
            openProject(dir)
        }
    }

    fun deleteProject(dir: File) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { store.delete(dir) }
            if (!ok) message = "Gagal menghapus ${dir.name}"
            reloadProjects()
        }
    }

    fun importZip(uri: Uri) {
        viewModelScope.launch {
            val r = withContext(Dispatchers.IO) { runCatching { store.importZip(getApplication(), uri) } }
            r.onSuccess { message = "Diimpor: ${it.name}"; reloadProjects() }
                .onFailure { message = "Gagal impor: ${it.message ?: it.javaClass.simpleName}" }
        }
    }

    fun exportProject(dir: File, uri: Uri) {
        viewModelScope.launch {
            val r = withContext(Dispatchers.IO) { runCatching { store.exportZip(getApplication(), dir, uri) } }
            r.onSuccess { message = "${dir.name}.zip diekspor" }
                .onFailure { message = "Gagal ekspor: ${it.message ?: it.javaClass.simpleName}" }
        }
    }

    fun openProject(dir: File) {
        if (project == dir) return
        viewModelScope.launch {
            saveAll()
            persistSession()
            tabs.clear(); activeIndex = -1; expanded.clear(); rows = emptyList()
            project = dir
            targetDir = dir
            val session = repo.loadSession(dir.path)
            val restored = withContext(Dispatchers.IO) {
                dir.setLastModified(System.currentTimeMillis())
                session.paths.take(MAX_RESTORE).mapNotNull { rel ->
                    val f = File(dir, rel)
                    readText(f)?.let { f to it }
                }
            }
            restored.forEach { (f, text) -> tabs += EditorTab(f, Content(text)) }
            activeIndex = if (tabs.isEmpty()) -1 else session.active.coerceIn(0, tabs.lastIndex)
            refreshTree()
        }
    }

    fun closeProject() {
        saveAll()
        persistSession()
        project = null
        targetDir = null
        tabs.clear(); activeIndex = -1; expanded.clear(); rows = emptyList()
        reloadProjects()
    }

    /** Simpan daftar tab terbuka. Snapshot diambil sekarang (main thread), penulisan di coroutine. */
    private fun persistSession() {
        val p = project ?: return
        val rels = tabs.mapNotNull { t -> runCatching { t.file.relativeTo(p).path }.getOrNull() }
        val act = activeIndex
        viewModelScope.launch { repo.saveSession(p.path, rels, act) }
    }

    fun onAppStop() {
        saveAll()
        persistSession()
    }

    // =====================================================================
    // Tree
    // =====================================================================
    fun refreshTree() {
        val root = project ?: return
        val open = expanded.toSet()
        viewModelScope.launch { rows = withContext(Dispatchers.IO) { buildRows(root, open) } }
    }

    private fun buildRows(root: File, open: Set<String>): List<TreeRow> {
        val out = ArrayList<TreeRow>()
        fun walk(dir: File, depth: Int) {
            val kids = dir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: return
            for (f in kids) {
                val isDir = f.isDirectory
                val isOpen = isDir && f.path in open
                out += TreeRow(f, depth, isOpen, isDir)
                if (isOpen) walk(f, depth + 1)
            }
        }
        walk(root, 0)
        return out
    }

    fun onRowClick(row: TreeRow) {
        if (row.isDir) {
            targetDir = row.file
            if (!expanded.add(row.file.path)) expanded.remove(row.file.path)
            refreshTree()
        } else {
            targetDir = row.file.parentFile
            openFile(row.file)
        }
    }

    fun collapseAll() {
        expanded.clear()
        targetDir = project
        refreshTree()
    }

    // =====================================================================
    // Operasi file
    // =====================================================================
    fun createFile(parent: File, name: String) {
        val n = name.trim()
        if (!Fs.validName(n)) { message = "Nama tidak valid"; return }
        val f = File(parent, n)
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { runCatching { !f.exists() && f.createNewFile() }.getOrDefault(false) }
            if (!ok) { message = "Sudah ada atau gagal dibuat"; return@launch }
            expanded += parent.path
            targetDir = parent
            refreshTree()
            openFile(f)
        }
    }

    fun createFolder(parent: File, name: String) {
        val n = name.trim()
        if (!Fs.validName(n)) { message = "Nama tidak valid"; return }
        val f = File(parent, n)
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { runCatching { !f.exists() && f.mkdir() }.getOrDefault(false) }
            if (!ok) { message = "Sudah ada atau gagal dibuat"; return@launch }
            expanded += parent.path
            expanded += f.path
            targetDir = f
            refreshTree()
        }
    }

    fun rename(f: File, newName: String) {
        val n = newName.trim()
        if (!Fs.validName(n)) { message = "Nama tidak valid"; return }
        val dest = File(f.parentFile, n)
        if (dest.path == f.path) return
        if (dest.exists()) { message = "Sudah ada: $n"; return }
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { f.renameTo(dest) }
            if (!ok) { message = "Gagal mengganti nama"; return@launch }
            val old = f.path
            tabs.forEach { t ->
                val p = t.file.path
                if (p == old || p.startsWith(old + File.separator)) t.file = File(dest.path + p.removePrefix(old))
            }
            val remapped = expanded.map { if (it == old || it.startsWith(old + File.separator)) dest.path + it.removePrefix(old) else it }
            expanded.clear(); expanded.addAll(remapped)
            if (targetDir?.path == old) targetDir = dest
            refreshTree()
        }
    }

    fun delete(f: File) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { f.deleteRecursively() }
            if (!ok) message = "Sebagian file gagal dihapus"
            val old = f.path
            tabs.indices.filter { i ->
                val p = tabs[i].file.path
                p == old || p.startsWith(old + File.separator)
            }.reversed().forEach { closeNow(it) }
            if (targetDir?.exists() != true) targetDir = project
            refreshTree()
        }
    }

    fun duplicate(f: File) {
        viewModelScope.launch {
            val dst = withContext(Dispatchers.IO) {
                runCatching { Fs.uniqueCopy(f).takeIf { d -> f.copyRecursively(d) } }.getOrNull()
            }
            if (dst == null) message = "Gagal menduplikat" else refreshTree()
        }
    }

    // =====================================================================
    // Tab
    // =====================================================================
    fun openFile(file: File, line: Int? = null) {
        val existing = tabs.indexOfFirst { it.file == file }
        if (existing >= 0) {
            activeIndex = existing
            if (line != null) pendingGoto = line
            return
        }
        viewModelScope.launch {
            val text = withContext(Dispatchers.IO) { readText(file) }
            if (text == null) { message = "Tidak bisa membuka ${file.name} (bukan teks / terlalu besar)"; return@launch }
            val again = tabs.indexOfFirst { it.file == file }
            if (again >= 0) activeIndex = again else {
                tabs += EditorTab(file, Content(text))
                activeIndex = tabs.lastIndex
            }
            if (line != null) pendingGoto = line
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
        val target = t.file
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { runCatching { target.writeText(text) }.isSuccess }
            if (ok) { t.dirty = false; after() } else message = "Gagal menyimpan ${t.file.name}"
        }
    }

    fun saveAll() {
        tabs.filter { it.dirty }.forEach { t ->
            val text = t.content.toString()
            runCatching { t.file.writeText(text); t.dirty = false }
        }
    }

    // =====================================================================
    // Pencarian
    // =====================================================================
    suspend fun findFiles(q: String): List<File> {
        val root = project ?: return emptyList()
        return withContext(Dispatchers.IO) { ProjectSearch.files(root, q) { ensureActive() } }
    }

    suspend fun findText(q: String, ignoreCase: Boolean): List<Hit> {
        val root = project ?: return emptyList()
        return withContext(Dispatchers.IO) { ProjectSearch.text(root, q, ignoreCase) { ensureActive() } }
    }

    // =====================================================================
    // Snippet & setelan
    // =====================================================================
    fun snippetsFor(lang: Lang): List<Snippet> = SnippetEngine.load(getApplication(), lang)

    private fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        viewModelScope.launch { repo.edit(block) }
    }
    fun setThemeMode(m: ThemeMode) = edit { it[Keys.THEME] = m.name }
    fun setFontSize(v: Float) = edit { it[Keys.FONT] = v }
    fun setWordWrap(v: Boolean) = edit { it[Keys.WRAP] = v }
    fun setLineNumbers(v: Boolean) = edit { it[Keys.LINES] = v }
    fun setTabSize(v: Int) = edit { it[Keys.TAB] = v }

    companion object {
        const val MAX_BYTES = 5L * 1024 * 1024
        const val MAX_RESTORE = 12
    }
}
