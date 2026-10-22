package com.sally.ide.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sally.ide.IdeViewModel
import com.sally.ide.data.ThemeMode
import com.sally.ide.editor.Completion
import com.sally.ide.editor.Kind
import com.sally.ide.editor.Lang
import com.sally.ide.editor.Suggestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdeScreen(vm: IdeViewModel) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val handle = remember { EditorHandle() }

    val dark = isDark(prefs.themeMode)
    val editorTheme = when { prefs.themeMode == ThemeMode.AMOLED -> "amoled"; dark -> "dark"; else -> "light" }

    var showFind by remember { mutableStateOf(false) }
    var showSnippets by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showGoto by remember { mutableStateOf(false) }
    var showNewFile by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    var tick by remember { mutableIntStateOf(0) }
    var suggestions by remember { mutableStateOf<List<Suggestion>>(emptyList()) }
    var prefixLen by remember { mutableIntStateOf(0) }
    var cursor by remember { mutableStateOf(1 to 1) }

    val tab = vm.active
    val lang = tab?.lang ?: Lang.PLAIN

    vm.message?.let { msg -> LaunchedEffect(msg) { snackbar.showSnackbar(msg); vm.message = null } }

    // Saran completion: debounce, hitung di background; editor hanya disentuh di main thread.
    LaunchedEffect(tick, tab) {
        delay(120)
        cursor = handle.cursor()
        val p = handle.prefix()
        prefixLen = p.length
        if (tab == null || p.isEmpty()) { suggestions = emptyList(); return@LaunchedEffect }
        val text = handle.textForIndex(Completion.MAX_INDEXED_CHARS)
        val snippets = vm.snippetsFor(lang)
        suggestions = withContext(Dispatchers.Default) { Completion.suggest(p, lang, snippets, text) }
    }

    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            ModalDrawerSheet {
                Explorer(
                    rows = vm.rows, selectedDir = vm.selectedDir, activeFile = tab?.file,
                    onRow = { r -> vm.onRowClick(r); if (!r.file.isDirectory) scope.launch { drawer.close() } },
                    onNewFile = { showNewFile = true }, onRefresh = { vm.refreshTree() },
                )
            }
        },
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize().imePadding(),
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                TopAppBar(
                    title = { Text(tab?.file?.name ?: "Sally IDE", maxLines = 1) },
                    navigationIcon = { IconButton(onClick = { scope.launch { drawer.open() } }) { Icon(Icons.Filled.Menu, "Buka explorer") } },
                    actions = {
                        IconButton(onClick = { handle.undo() }) { Icon(Icons.AutoMirrored.Filled.Undo, "Undo") }
                        IconButton(onClick = { handle.redo() }) { Icon(Icons.AutoMirrored.Filled.Redo, "Redo") }
                        IconButton(onClick = { vm.save() }, enabled = tab?.dirty == true) { Icon(Icons.Filled.Save, "Simpan") }
                        IconButton(onClick = { vm.message = "Build Gradle tersedia di Fase 4 (lihat PROGRESS.md)" }) {
                            Icon(Icons.Filled.PlayArrow, "Build", tint = MaterialTheme.colorScheme.primary)
                        }
                        Box {
                            IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, "Menu lain") }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(text = { Text("Ke baris…") }, enabled = tab != null,
                                    onClick = { menuOpen = false; showGoto = true })
                                DropdownMenuItem(text = { Text("File baru…") }, onClick = { menuOpen = false; showNewFile = true })
                                DropdownMenuItem(text = { Text("Tutup tab") }, enabled = tab != null,
                                    onClick = { menuOpen = false; vm.requestClose(vm.activeIndex) })
                            }
                        }
                    },
                )
            },
            bottomBar = {
                Column {
                    if (tab != null) {
                        SuggestionBar(suggestions) { s ->
                            if (s.snippet != null) handle.insertSnippet(s.snippet, prefixLen, prefs.tabSize)
                            else handle.replacePrefix(prefixLen, s.label)
                            suggestions = emptyList()
                        }
                        ExtraKeyRow(handle, prefs.tabSize)
                        StatusBar(lang.label, cursor.first, cursor.second, prefs.tabSize)
                    }
                    if (!imeVisible) {
                        NavigationBar {
                            NavigationBarItem(false, { scope.launch { drawer.open() } }, { Icon(Icons.Outlined.Folder, null) }, label = { Text("File") })
                            NavigationBarItem(showFind, { if (tab != null) showFind = !showFind }, { Icon(Icons.Outlined.Search, null) }, label = { Text("Cari") })
                            NavigationBarItem(false, { vm.message = "Build tersedia di Fase 4" }, { Icon(Icons.Outlined.Build, null) }, label = { Text("Build") })
                            NavigationBarItem(false, { if (tab != null) showSnippets = true }, { Icon(Icons.Outlined.Extension, null) }, label = { Text("Snippet") })
                            NavigationBarItem(false, { showSettings = true }, { Icon(Icons.Outlined.Settings, null) }, label = { Text("Setelan") })
                        }
                    }
                }
            },
        ) { pad ->
            Column(Modifier.padding(pad).fillMaxSize()) {
                TabBar(vm.tabs, vm.activeIndex, vm::select, vm::requestClose)
                if (showFind && tab != null) FindBar(handle) { showFind = false }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    if (tab != null) {
                        CodeEditorView(tab, prefs, editorTheme, handle, onChanged = { tick++ }, modifier = Modifier.fillMaxSize())
                    } else {
                        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Belum ada file terbuka", style = MaterialTheme.typography.titleMedium)
                            Text("Buka explorer (☰) lalu pilih file", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    if (showSnippets && tab != null) {
        SnippetSheet(vm.snippetsFor(lang), lang.label, onPick = { s ->
            handle.insertSnippet(s, 0, prefs.tabSize); showSnippets = false
        }, onDismiss = { showSnippets = false })
    }
    if (showSettings) {
        SettingsSheet(prefs, vm::setThemeMode, vm::setDynamic, vm::setFontSize, vm::setWordWrap, vm::setLineNumbers, vm::setTabSize) { showSettings = false }
    }
    if (showGoto) {
        TextInputDialog("Ke baris", "Nomor baris", numeric = true, onConfirm = { s ->
            s.trim().toIntOrNull()?.let { handle.gotoLine(it) }; showGoto = false
        }, onDismiss = { showGoto = false })
    }
    if (showNewFile) {
        TextInputDialog("File baru di ${vm.selectedDir.name}", "Nama file (mis. Util.kt)", onConfirm = { vm.createFile(it); showNewFile = false },
            onDismiss = { showNewFile = false })
    }
    val closing = vm.pendingClose
    if (closing >= 0) {
        UnsavedDialog(
            name = vm.tabs.getOrNull(closing)?.file?.name ?: "",
            onSave = { vm.saveAndClose(closing) }, onDiscard = { vm.closeNow(closing) }, onCancel = { vm.pendingClose = -1 },
        )
    }
}
