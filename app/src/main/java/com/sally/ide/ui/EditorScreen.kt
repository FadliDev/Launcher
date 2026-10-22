package com.sally.ide.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sally.ide.IdeViewModel
import com.sally.ide.data.ThemeMode
import com.sally.ide.editor.Completion
import com.sally.ide.editor.Lang
import com.sally.ide.editor.Suggestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(vm: IdeViewModel, project: File, snackbar: SnackbarHostState) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val handle = remember { EditorHandle() }
    val clip = LocalClipboardManager.current
    val zip = rememberZipActions(vm)
    val wide = LocalConfiguration.current.screenWidthDp >= 720

    val dark = isDark(prefs.themeMode)
    val editorTheme = when { prefs.themeMode == ThemeMode.AMOLED -> "amoled"; dark -> "dark"; else -> "light" }

    var showFind by remember { mutableStateOf(false) }
    var showSnippets by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showGoto by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var newFileIn by remember { mutableStateOf<File?>(null) }
    var newFolderIn by remember { mutableStateOf<File?>(null) }
    var renaming by remember { mutableStateOf<File?>(null) }
    var deleting by remember { mutableStateOf<File?>(null) }

    var tick by remember { mutableIntStateOf(0) }
    var suggestions by remember { mutableStateOf<List<Suggestion>>(emptyList()) }
    var prefixLen by remember { mutableIntStateOf(0) }
    var cursor by remember { mutableStateOf(1 to 1) }

    val tab = vm.active
    val lang = tab?.lang ?: Lang.PLAIN

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

    // Lompat ke baris (hasil pencarian). Beri waktu editor memasang teks tab baru.
    val goto = vm.pendingGoto
    LaunchedEffect(goto, tab) {
        if (goto != null && tab != null) {
            delay(150)
            handle.gotoLine(goto)
            vm.pendingGoto = null
        }
    }

    BackHandler(enabled = !showSearch && drawer.isClosed) { vm.closeProject() }
    BackHandler(enabled = showFind && !showSearch) { handle.stopSearch(); showFind = false }
    BackHandler(enabled = showSearch) { showSearch = false }

    val actions = ExplorerActions(
        onOpen = { r -> vm.onRowClick(r); if (!r.isDir && !wide) scope.launch { drawer.close() } },
        onNewFile = { newFileIn = it },
        onNewFolder = { newFolderIn = it },
        onRename = { renaming = it },
        onDuplicate = { vm.duplicate(it) },
        onDelete = { deleting = it },
        onCopyPath = { f -> clip.setText(AnnotatedString(runCatching { f.relativeTo(project).path }.getOrDefault(f.path))) },
        onRefresh = { vm.refreshTree() },
        onCollapse = { vm.collapseAll() },
    )

    val explorer: @Composable () -> Unit = {
        Explorer(project.name, vm.rows, tab?.file, vm.targetDir ?: project, actions)
    }

    val main: @Composable () -> Unit = {
        Scaffold(
            modifier = Modifier.fillMaxSize().imePadding(),
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(tab?.file?.name ?: project.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                            if (tab != null) {
                                Text(
                                    lang.label + " · Ln " + cursor.first + ", Col " + cursor.second + (if (tab.dirty) " · belum disimpan" else ""),
                                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        if (wide) IconButton(onClick = { vm.closeProject() }) { Icon(Ic.Back, "Daftar proyek") }
                        else IconButton(onClick = { scope.launch { drawer.open() } }) { Icon(Ic.Menu, "Buka explorer") }
                    },
                    actions = {
                        if (tab?.dirty == true) IconButton(onClick = { vm.save() }) { Icon(Ic.Save, "Simpan") }
                        IconButton(onClick = { if (tab != null) showFind = !showFind }, enabled = tab != null) { Icon(Ic.Search, "Cari di file") }
                        Box {
                            IconButton(onClick = { menuOpen = true }) { Icon(Ic.More, "Menu lain") }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(text = { Text("Cari di proyek") }, leadingIcon = { Icon(Ic.Search, null) },
                                    onClick = { menuOpen = false; showSearch = true })
                                DropdownMenuItem(text = { Text("Ke baris…") }, leadingIcon = { Icon(Ic.Code, null) }, enabled = tab != null,
                                    onClick = { menuOpen = false; showGoto = true })
                                DropdownMenuItem(text = { Text("Snippet") }, leadingIcon = { Icon(Ic.Extension, null) }, enabled = tab != null,
                                    onClick = { menuOpen = false; showSnippets = true })
                                DropdownMenuItem(text = { Text("Simpan") }, leadingIcon = { Icon(Ic.Save, null) }, enabled = tab?.dirty == true,
                                    onClick = { menuOpen = false; vm.save() })
                                DropdownMenuItem(text = { Text("Tutup tab") }, leadingIcon = { Icon(Ic.Close, null) }, enabled = tab != null,
                                    onClick = { menuOpen = false; vm.requestClose(vm.activeIndex) })
                                HorizontalDivider()
                                DropdownMenuItem(text = { Text("Ekspor zip") }, leadingIcon = { Icon(Ic.Download, null) },
                                    onClick = { menuOpen = false; vm.saveAll(); zip.onExport(project) })
                                DropdownMenuItem(text = { Text("Setelan") }, leadingIcon = { Icon(Ic.Settings, null) },
                                    onClick = { menuOpen = false; showSettings = true })
                                DropdownMenuItem(text = { Text("Tutup proyek") }, leadingIcon = { Icon(Ic.Home, null) },
                                    onClick = { menuOpen = false; vm.closeProject() })
                            }
                        }
                    },
                )
            },
            bottomBar = {
                if (tab != null) {
                    Column(Modifier.navigationBarsPadding()) {
                        SuggestionBar(suggestions) { s ->
                            if (s.snippet != null) handle.insertSnippet(s.snippet, prefixLen, prefs.tabSize)
                            else handle.replacePrefix(prefixLen, s.label)
                            suggestions = emptyList()
                        }
                        ExtraKeyRow(handle, prefs.tabSize)
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
                        EmptyEditor(onOpenExplorer = if (wide) null else ({ scope.launch { drawer.open() } }), onSearch = { showSearch = true })
                    }
                }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (wide) {
            Row(Modifier.fillMaxSize()) {
                Surface(Modifier.width(300.dp).fillMaxHeight(), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Column(Modifier.statusBarsPadding()) { explorer() }
                }
                VerticalDivider()
                Box(Modifier.weight(1f).fillMaxHeight()) { main() }
            }
        } else {
            ModalNavigationDrawer(drawerState = drawer, drawerContent = { ModalDrawerSheet { explorer() } }) { main() }
        }
        if (showSearch) {
            ProjectSearchScreen(
                vm, project,
                onOpen = { f, line -> vm.openFile(f, line); showSearch = false },
                onClose = { showSearch = false },
            )
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
    newFileIn?.let { dir ->
        TextInputDialog("File baru di ${dir.name}", "Nama file (mis. Util.kt)",
            onConfirm = { vm.createFile(dir, it); newFileIn = null }, onDismiss = { newFileIn = null })
    }
    newFolderIn?.let { dir ->
        TextInputDialog("Folder baru di ${dir.name}", "Nama folder",
            onConfirm = { vm.createFolder(dir, it); newFolderIn = null }, onDismiss = { newFolderIn = null })
    }
    renaming?.let { f ->
        TextInputDialog("Ganti nama", "Nama baru", initial = f.name,
            onConfirm = { vm.rename(f, it); renaming = null }, onDismiss = { renaming = null })
    }
    deleting?.let { f ->
        ConfirmDialog(
            "Hapus ${if (f.isDirectory) "folder" else "file"}?",
            "\"${f.name}\"" + (if (f.isDirectory) " dan semua isinya" else "") + " akan dihapus permanen.",
            "Hapus", onConfirm = { vm.delete(f); deleting = null }, onDismiss = { deleting = null },
        )
    }
    val closing = vm.pendingClose
    if (closing >= 0) {
        UnsavedDialog(
            name = vm.tabs.getOrNull(closing)?.file?.name ?: "",
            onSave = { vm.saveAndClose(closing) }, onDiscard = { vm.closeNow(closing) }, onCancel = { vm.pendingClose = -1 },
        )
    }
}

@Composable
private fun EmptyEditor(onOpenExplorer: (() -> Unit)?, onSearch: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
        Icon(Ic.Code, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text("Belum ada file terbuka", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        if (onOpenExplorer != null) {
            FilledTonalButton(onClick = onOpenExplorer) { Icon(Ic.Folder, null, Modifier.size(18.dp)); Spacer(Modifier.size(8.dp)); Text("Buka explorer") }
            Spacer(Modifier.height(8.dp))
        }
        FilledTonalButton(onClick = onSearch) { Icon(Ic.Search, null, Modifier.size(18.dp)); Spacer(Modifier.size(8.dp)); Text("Cari file") }
    }
}
