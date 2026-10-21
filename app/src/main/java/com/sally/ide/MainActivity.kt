package com.sally.ide

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.graphics.Typeface
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SnippetFolder
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.edit
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.widget.CodeEditor
import com.sally.ide.data.EditorDocument
import com.sally.ide.data.SnippetManager
import com.sally.ide.data.SnippetManager.Snippet
import com.sally.ide.editor.TextMateSupport
import com.sally.ide.ui.theme.SallyTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TextMateSupport.initialize(applicationContext)
        setContent { SallyIDEApp() }
    }
}

private enum class AppScreen { HOME, EDITOR, SETTINGS }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun SallyIDEApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("sally_preferences", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var darkTheme by remember { mutableStateOf(prefs.getBoolean("dark", true)) }
    var amoled by remember { mutableStateOf(prefs.getBoolean("amoled", false)) }
    var dynamicColor by remember { mutableStateOf(prefs.getBoolean("dynamic", true)) }
    var screen by rememberSaveable { mutableStateOf(AppScreen.HOME) }
    var screenBeforeSettings by remember { mutableStateOf(AppScreen.HOME) }
    val documents = remember { mutableStateListOf<EditorDocument>() }
    var activeId by rememberSaveable { mutableStateOf<String?>(null) }
    val activeDoc = documents.firstOrNull { it.id == activeId }
    var editorRef by remember { mutableStateOf<CodeEditor?>(null) }
    var showNewFile by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("Main.kt") }
    var showFindReplace by remember { mutableStateOf(false) }
    var showSnippets by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }
    var findText by remember { mutableStateOf("") }
    var replaceText by remember { mutableStateOf("") }
    var pendingSaveId by remember { mutableStateOf<String?>(null) }
    var snippets by remember { mutableStateOf<List<Snippet>>(emptyList()) }
    var snippetStatus by remember { mutableStateOf("Menyiapkan snippet…") }
    var prefix by remember { mutableStateOf("") }
    var lineNumber by remember { mutableIntStateOf(1) }
    var columnNumber by remember { mutableIntStateOf(1) }
    val completionSuggestions = remember(prefix, snippets, activeDoc?.name, activeDoc?.content) {
        buildCompletionSuggestions(prefix, activeDoc?.name.orEmpty(), activeDoc?.content.orEmpty(), snippets)
    }

    SideEffect { TextMateSupport.setTheme(darkTheme) }

    fun updateDocument(id: String, transform: (EditorDocument) -> EditorDocument) {
        val index = documents.indexOfFirst { it.id == id }
        if (index >= 0) documents[index] = transform(documents[index])
    }

    fun createNewDocument(rawName: String, content: String = "") {
        val safeName = rawName.trim().ifBlank { "Untitled.txt" }.replace("/", "_").replace("\\", "_")
        val workspace = File(context.filesDir, "workspace").apply { mkdirs() }
        var target = File(workspace, safeName)
        var suffix = 1
        while (target.exists()) {
            val base = safeName.substringBeforeLast('.', safeName)
            val extension = safeName.substringAfterLast('.', "")
            target = File(workspace, if (extension.isBlank()) "$base-$suffix" else "$base-$suffix.$extension")
            suffix++
        }
        runCatching { target.writeText(content) }
        val doc = EditorDocument(name = target.name, content = content, savedContent = content, localPath = target.absolutePath)
        documents.add(doc)
        activeId = doc.id
        screen = AppScreen.EDITOR
        showNewFile = false
        newFileName = "Main.kt"
    }

    fun addOpenedDocument(uri: Uri, content: String) {
        val existing = documents.indexOfFirst { it.uri == uri }
        val name = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "Untitled.txt"
        if (existing >= 0) {
            updateDocument(documents[existing].id) { it.copy(content = content, savedContent = content) }
            activeId = documents[existing].id
        } else {
            val doc = EditorDocument(id = uri.toString(), name = name, content = content, savedContent = content, uri = uri)
            documents.add(doc)
            activeId = doc.id
        }
        screen = AppScreen.EDITOR
    }

    val openFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            scope.launch {
                val loaded = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
                    }.getOrDefault("")
                }
                addOpenedDocument(uri, loaded)
            }
        }
    }

    val createFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        val id = pendingSaveId
        pendingSaveId = null
        val doc = documents.firstOrNull { it.id == id }
        if (uri != null && doc != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        val output = context.contentResolver.openOutputStream(uri, "wt")
                            ?: error("Provider tidak membuka output stream")
                        output.bufferedWriter().use { it.write(doc.content) }
                        true
                    }.getOrDefault(false)
                }
                if (ok) {
                    updateDocument(doc.id) { it.copy(uri = uri, savedContent = it.content) }
                    snackbar.showSnackbar("File disimpan")
                } else snackbar.showSnackbar("Gagal menyimpan file")
            }
        }
    }

    fun saveDocument(doc: EditorDocument, asCopy: Boolean = false) {
        if (!asCopy && doc.uri != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        val output = context.contentResolver.openOutputStream(doc.uri, "wt")
                            ?: error("Provider tidak membuka output stream")
                        output.bufferedWriter().use { it.write(doc.content) }
                        true
                    }.getOrDefault(false)
                }
                if (ok) {
                    updateDocument(doc.id) { it.copy(savedContent = it.content) }
                    snackbar.showSnackbar("${doc.name} disimpan")
                } else snackbar.showSnackbar("Gagal menyimpan ${doc.name}")
            }
        } else if (!asCopy && doc.localPath != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching { File(doc.localPath).apply { parentFile?.mkdirs() }.writeText(doc.content); true }.getOrDefault(false)
                }
                if (ok) {
                    updateDocument(doc.id) { it.copy(savedContent = it.content) }
                    snackbar.showSnackbar("${doc.name} disimpan")
                } else snackbar.showSnackbar("Gagal menyimpan ${doc.name}")
            }
        } else {
            pendingSaveId = doc.id
            createFileLauncher.launch(doc.name)
        }
    }

    LaunchedEffect(activeDoc?.name) {
        val doc = activeDoc
        if (doc == null) {
            snippets = emptyList()
            snippetStatus = "Buka file untuk memuat snippet."
        } else {
            snippetStatus = "Mencari snippet untuk ${TextMateSupport.languageLabel(doc.name)}…"
            val result = SnippetManager.load(context, doc.name)
            snippets = result.first
            snippetStatus = result.second
        }
    }

    SallyTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, amoled = amoled) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                TopAppBar(
                    title = {
                        when (screen) {
                            AppScreen.HOME -> Row(verticalAlignment = Alignment.CenterVertically) {
                                AppMark(Modifier.size(36.dp))
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("Sally IDE", fontWeight = FontWeight.Bold)
                                    Text("Code, langsung dari HP", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            AppScreen.EDITOR -> Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(activeDoc?.name ?: "Editor", maxLines = 1, fontWeight = FontWeight.SemiBold)
                                    if (activeDoc?.isDirty == true) {
                                        Box(Modifier.size(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                                    }
                                }
                                Text(activeDoc?.let { TextMateSupport.languageLabel(it.name) } ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            AppScreen.SETTINGS -> Text("Pengaturan", fontWeight = FontWeight.SemiBold)
                        }
                    },
                    navigationIcon = {
                        if (screen == AppScreen.HOME) {
                            IconButton(onClick = { showNewFile = true }) { Icon(Icons.Default.Add, "File baru") }
                        } else {
                            IconButton(onClick = {
                                if (screen == AppScreen.SETTINGS) screen = screenBeforeSettings else screen = AppScreen.HOME
                            }) { Icon(Icons.Default.ArrowBack, "Kembali") }
                        }
                    },
                    actions = {
                        if (screen == AppScreen.EDITOR && activeDoc != null) {
                            IconButton(onClick = { saveDocument(activeDoc) }) {
                                Icon(Icons.Default.Save, "Simpan")
                            }
                            IconButton(onClick = { editorRef?.undo() }, enabled = editorRef?.canUndo() == true) {
                                Icon(Icons.Default.Undo, "Undo")
                            }
                            IconButton(onClick = { editorRef?.redo() }, enabled = editorRef?.canRedo() == true) {
                                Icon(Icons.Default.Redo, "Redo")
                            }
                            Box {
                                IconButton(onClick = { showMore = true }) { Icon(Icons.Default.MoreVert, "Menu editor") }
                                DropdownMenu(expanded = showMore, onDismissRequest = { showMore = false }) {
                                    DropdownMenuItem(text = { Text("Cari & ganti") }, leadingIcon = { Icon(Icons.Default.Search, null) }, onClick = { showMore = false; showFindReplace = true })
                                    DropdownMenuItem(text = { Text("Snippet") }, leadingIcon = { Icon(Icons.Default.SnippetFolder, null) }, onClick = { showMore = false; showSnippets = true })
                                    DropdownMenuItem(text = { Text("Simpan sebagai…") }, leadingIcon = { Icon(Icons.Default.Save, null) }, onClick = { showMore = false; saveDocument(activeDoc, asCopy = true) })
                                    DropdownMenuItem(text = { Text("Pengaturan") }, leadingIcon = { Icon(Icons.Default.Settings, null) }, onClick = { showMore = false; screenBeforeSettings = screen; screen = AppScreen.SETTINGS })
                                }
                            }
                        } else if (screen == AppScreen.HOME) {
                            IconButton(onClick = { screenBeforeSettings = screen; screen = AppScreen.SETTINGS }) { Icon(Icons.Default.Settings, "Pengaturan") }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            },
            bottomBar = {
                if (screen == AppScreen.EDITOR) {
                    Column {
                        AnimatedVisibility(visible = prefix.length >= 2 && completionSuggestions.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 3.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                completionSuggestions.forEach { suggestion ->
                                    AssistChip(
                                        onClick = {
                                            val editor = editorRef
                                            if (editor != null) {
                                                val line = editor.cursor.leftLine
                                                val col = editor.cursor.leftColumn
                                                val from = (col - prefix.length).coerceAtLeast(0)
                                                editor.setSelection(line, from, true)
                                                val insertion = suggestion.snippet?.let { SnippetManager.renderBody(it.body) } ?: suggestion.label
                                                editor.insertText(insertion, insertion.length)
                                            }
                                            prefix = ""
                                        },
                                        label = { Text(suggestion.label, maxLines = 1) },
                                        leadingIcon = { Icon(if (suggestion.snippet != null) Icons.Default.SnippetFolder else Icons.Default.Code, null, Modifier.size(16.dp)) }
                                    )
                                }
                            }
                        }
                        ExtraKeyRow(onInsert = { value ->
                            val editor = editorRef
                            if (editor != null) {
                                if (value == "TAB") editor.indentOrCommitTab()
                                else if (value == "←") editor.setSelection(editor.cursor.leftLine, (editor.cursor.leftColumn - 1).coerceAtLeast(0), true)
                                else if (value == "→") editor.setSelection(editor.cursor.leftLine, (editor.cursor.leftColumn + 1).coerceAtMost(editor.text.getColumnCount(editor.cursor.leftLine)), true)
                                else editor.insertText(value, value.length)
                                editor.requestFocus()
                            }
                        })
                        Row(
                            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(horizontal = 14.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(activeDoc?.let { TextMateSupport.languageLabel(it.name) } ?: "Plain Text", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Ln $lineNumber, Col $columnNumber", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiary))
                                Text("Local", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        EditorNavigationBar(
                            onFiles = { screen = AppScreen.HOME },
                            onSearch = { showFindReplace = true },
                            onSnippets = { showSnippets = true },
                            onBuild = { scope.launch { snackbar.showSnackbar("Build Gradle di HP masuk fase toolchain berikutnya.") } },
                            onSettings = { screenBeforeSettings = screen; screen = AppScreen.SETTINGS }
                        )
                    }
                } else {
                    EditorNavigationBar(
                        onFiles = { screen = AppScreen.HOME },
                        onSearch = { if (activeDoc != null) { screen = AppScreen.EDITOR; showFindReplace = true } },
                        onSnippets = { if (activeDoc != null) { screen = AppScreen.EDITOR; showSnippets = true } },
                        onBuild = { scope.launch { snackbar.showSnackbar("Build Gradle di HP akan ditambahkan pada fase toolchain.") } },
                        onSettings = { screenBeforeSettings = screen; screen = AppScreen.SETTINGS }
                    )
                }
            },
            contentWindowInsets = WindowInsets.navigationBars
        ) { padding ->
            when (screen) {
                AppScreen.HOME -> HomeScreen(
                    modifier = Modifier.padding(padding),
                    documents = documents,
                    onNewFile = { newFileName = it; showNewFile = true },
                    onOpen = { openFileLauncher.launch(arrayOf("*/*")) },
                    onOpenDocument = { doc -> activeId = doc.id; screen = AppScreen.EDITOR },
                    onTemplate = { name, content -> createNewDocument(name, content) }
                )
                AppScreen.EDITOR -> {
                    if (activeDoc == null) {
                        EmptyEditorScreen(Modifier.padding(padding), onNew = { showNewFile = true }, onOpen = { openFileLauncher.launch(arrayOf("*/*")) })
                    } else {
                        EditorScreen(
                            modifier = Modifier.padding(padding),
                            documents = documents,
                            activeDoc = activeDoc,
                            darkTheme = darkTheme,
                            editorHolder = { editorRef = it },
                            onSelectTab = { activeId = it },
                            onCloseTab = { id ->
                                val index = documents.indexOfFirst { it.id == id }
                                if (index >= 0) documents.removeAt(index)
                                if (activeId == id) activeId = documents.lastOrNull()?.id
                                if (documents.isEmpty()) screen = AppScreen.HOME
                            },
                            onContent = { id, value, word, line, col ->
                                updateDocument(id) { it.copy(content = value) }
                                prefix = word
                                lineNumber = line + 1
                                columnNumber = col + 1
                            },
                            onOpen = { openFileLauncher.launch(arrayOf("*/*")) },
                            onSave = { saveDocument(activeDoc) },
                            onFind = { showFindReplace = true }
                        )
                    }
                }
                AppScreen.SETTINGS -> SettingsScreen(
                    modifier = Modifier.padding(padding),
                    darkTheme = darkTheme,
                    amoled = amoled,
                    dynamicColor = dynamicColor,
                    onDarkChanged = { darkTheme = it; prefs.edit { putBoolean("dark", it) } },
                    onAmoledChanged = { amoled = it; prefs.edit { putBoolean("amoled", it) } },
                    onDynamicChanged = { dynamicColor = it; prefs.edit { putBoolean("dynamic", it) } },
                    onBack = { screen = screenBeforeSettings }
                )
            }
        }
    }

    if (showNewFile) {
        AlertDialog(
            onDismissRequest = { showNewFile = false },
            title = { Text("Buat file baru") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Pilih nama dan ekstensi file.", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(value = newFileName, onValueChange = { newFileName = it }, singleLine = true, label = { Text("Nama file") }, leadingIcon = { Icon(Icons.Default.InsertDriveFile, null) })
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(".kt", ".java", ".html", ".py", ".cpp").forEach { ext ->
                            AssistChip(onClick = { newFileName = "Main$ext" }, label = { Text(ext) })
                        }
                    }
                }
            },
            confirmButton = { Button(onClick = { createNewDocument(newFileName) }) { Text("Buat") } },
            dismissButton = { TextButton(onClick = { showNewFile = false }) { Text("Batal") } }
        )
    }

    if (showFindReplace) {
        AlertDialog(
            onDismissRequest = { showFindReplace = false },
            title = { Text("Cari & ganti") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = findText, onValueChange = { findText = it }, label = { Text("Cari teks") }, singleLine = true)
                    OutlinedTextField(value = replaceText, onValueChange = { replaceText = it }, label = { Text("Ganti dengan") }, singleLine = true)
                    Text("Pencarian teks biasa untuk file aktif.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val doc = activeDoc
                    if (doc != null && findText.isNotEmpty()) {
                        val updated = doc.content.replace(findText, replaceText)
                        updateDocument(doc.id) { it.copy(content = updated) }
                        editorRef?.setText(updated)
                        scope.launch { snackbar.showSnackbar("Teks diganti") }
                    }
                    showFindReplace = false
                }) { Text("Ganti semua") }
            },
            dismissButton = { TextButton(onClick = { showFindReplace = false }) { Text("Tutup") } }
        )
    }

    if (showSnippets) {
        SnippetsDialog(
            snippets = snippets,
            status = snippetStatus,
            language = activeDoc?.let { TextMateSupport.languageLabel(it.name) } ?: "Bahasa",
            onDismiss = { showSnippets = false },
            onInsert = { snippet ->
                val editor = editorRef
                if (editor != null) {
                    val line = editor.cursor.leftLine
                    val col = editor.cursor.leftColumn
                    val replaceLength = prefix.length.coerceAtMost(col)
                    if (replaceLength > 0) editor.setSelection(line, col - replaceLength, true)
                    val body = SnippetManager.renderBody(snippet.body)
                    editor.insertText(body, body.length)
                    prefix = ""
                }
                showSnippets = false
            }
        )
    }
}

@Composable
private fun HomeScreen(
    modifier: Modifier,
    documents: List<EditorDocument>,
    onNewFile: (String) -> Unit,
    onOpen: () -> Unit,
    onOpenDocument: (EditorDocument) -> Unit,
    onTemplate: (String, String) -> Unit
) {
    Column(modifier.fillMaxSize().padding(horizontal = 18.dp).verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(8.dp))
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Code, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(28.dp)) }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Edit kode tanpa ribet.", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Workspace coding di saku lo.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Text("Syntax highlight · snippet · multi-tab · file lokal", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { onNewFile("Main.kt") }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("File baru")
                    }
                    OutlinedButton(onClick = onOpen, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.FolderOpen, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Buka file")
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Mulai dari template", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("3 contoh", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(10.dp))
        TemplateCard("Kotlin", "Main.kt", "fun main() {\n    println(\"Hello, Sally!\")\n}", "K", Modifier.fillMaxWidth(), onTemplate)
        TemplateCard("HTML", "index.html", "<!doctype html>\n<html lang=\"id\">\n  <head><title>Halo Sally</title></head>\n  <body><h1>Halo dunia!</h1></body>\n</html>", "<>", Modifier.fillMaxWidth(), onTemplate)
        TemplateCard("Python", "main.py", "def main():\n    print(\"Hello, Sally!\")\n\nif __name__ == \"__main__\":\n    main()", "Py", Modifier.fillMaxWidth(), onTemplate)
        Spacer(Modifier.height(24.dp))
        Text("Terakhir dibuka", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        if (documents.isEmpty()) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Belum ada file", fontWeight = FontWeight.Medium)
                        Text("Buat file baru atau buka file dari penyimpanan.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            documents.takeLast(8).reversed().forEach { doc ->
                RecentFileRow(doc = doc, onClick = { onOpenDocument(doc) })
            }
        }
        Spacer(Modifier.height(22.dp))
    }
}

@Composable
private fun TemplateCard(title: String, fileName: String, body: String, badge: String, modifier: Modifier, onTemplate: (String, String) -> Unit) {
    Card(
        modifier = modifier.padding(bottom = 8.dp).clickable { onTemplate(fileName, body) },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(13.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) { Text(badge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(fileName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RecentFileRow(doc: EditorDocument, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.size(40.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.InsertDriveFile, null, tint = MaterialTheme.colorScheme.onTertiaryContainer) }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(doc.name, fontWeight = FontWeight.Medium)
                if (doc.isDirty) { Spacer(Modifier.width(7.dp)); Box(Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)) }
            }
            Text(TextMateSupport.languageLabel(doc.name), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun EmptyEditorScreen(modifier: Modifier, onNew: () -> Unit, onOpen: () -> Unit) {
    Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Code, null, modifier = Modifier.size(54.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text("Workspace kosong", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Buka file atau buat yang baru untuk mulai coding.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onNew) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("File baru") }
        TextButton(onClick = onOpen) { Text("Buka file") }
    }
}

@Composable
private fun EditorScreen(
    modifier: Modifier,
    documents: List<EditorDocument>,
    activeDoc: EditorDocument,
    darkTheme: Boolean,
    editorHolder: (CodeEditor?) -> Unit,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onContent: (String, String, String, Int, Int) -> Unit,
    onOpen: () -> Unit,
    onSave: () -> Unit,
    onFind: () -> Unit
) {
    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow).horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            documents.forEach { doc ->
                val selected = doc.id == activeDoc.id
                Surface(
                    color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                    modifier = Modifier.padding(start = 4.dp, top = 5.dp).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)).clickable { onSelectTab(doc.id) }
                ) {
                    Row(Modifier.padding(start = 12.dp, end = 5.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        FileKindIcon(doc.name, Modifier.size(16.dp))
                        Text(doc.name, maxLines = 1, style = MaterialTheme.typography.labelMedium, color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                        if (doc.isDirty) Box(Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                        IconButton(onClick = { onCloseTab(doc.id) }, modifier = Modifier.size(22.dp)) { Icon(Icons.Default.Close, "Tutup ${doc.name}", modifier = Modifier.size(14.dp)) }
                    }
                }
            }
            IconButton(onClick = onOpen, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Add, "Buka file lain") }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        key(activeDoc.id, darkTheme) {
            AndroidView(
                modifier = Modifier.weight(1f).fillMaxWidth().background(MaterialTheme.colorScheme.surface),
                factory = { ctx ->
                    CodeEditor(ctx).apply {
                        setTextSize(15f)
                        typefaceText = Typeface.MONOSPACE
                        setLineNumberEnabled(true)
                        setHighlightCurrentLine(true)
                        setHighlightBracketPair(true)
                        setWordwrap(false)
                        setTabWidth(4)
                        colorScheme = TextMateSupport.colorScheme()
                        setEditorLanguage(TextMateSupport.languageFor(activeDoc.name))
                        setText(activeDoc.content)
                        editorHolder(this)
                        subscribeAlways<ContentChangeEvent> { _ ->
                            val current = text.toString()
                            val row = cursor.leftLine
                            val col = cursor.leftColumn
                            val word = wordAtCursor(current, row, col)
                            onContent(activeDoc.id, current, word, row, col)
                        }
                        subscribeAlways<SelectionChangeEvent> { _ ->
                            val current = text.toString()
                            val row = cursor.leftLine
                            val col = cursor.leftColumn
                            onContent(activeDoc.id, current, wordAtCursor(current, row, col), row, col)
                        }
                    }
                },
                onRelease = { view -> view.release() }
            )
        }
    }
}

private data class CompletionSuggestion(
    val label: String,
    val snippet: Snippet? = null
)

private fun buildCompletionSuggestions(
    prefix: String,
    fileName: String,
    content: String,
    snippets: List<Snippet>
): List<CompletionSuggestion> {
    if (prefix.length < 2) return emptyList()
    val suggestions = linkedMapOf<String, CompletionSuggestion>()
    snippets.asSequence()
        .filter { it.prefix.startsWith(prefix, ignoreCase = true) }
        .take(3)
        .forEach { suggestions.putIfAbsent(it.prefix.lowercase(), CompletionSuggestion(it.prefix, it)) }

    languageKeywords(fileName).asSequence()
        .filter { it.startsWith(prefix, ignoreCase = true) }
        .take(3)
        .forEach { suggestions.putIfAbsent(it.lowercase(), CompletionSuggestion(it)) }

    Regex("[A-Za-z_$][A-Za-z0-9_$]{1,}").findAll(content).map { it.value }
        .distinct().filter { it.startsWith(prefix, ignoreCase = true) }
        .take(3).forEach { suggestions.putIfAbsent(it.lowercase(), CompletionSuggestion(it)) }
    return suggestions.values.take(7)
}

private fun languageKeywords(fileName: String): List<String> {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    val common = "if else when switch case break continue return class interface enum object fun function val var const let static final public private protected import package try catch finally throw new this super true false null void int string boolean for while do async await yield type interface extends implements public export default from as return def lambda self None True False and or not print include define using namespace template typename auto constexpr struct union sizeof nullptr fn impl pub mod use mut match loop crate self Some Ok Err let mut fn pub async await SELECT FROM WHERE INSERT UPDATE DELETE CREATE TABLE ALTER DROP JOIN ON AND OR NOT NULL PRIMARY KEY REFERENCES BEGIN END".split(Regex("\\s+"))
    val specific = when (ext) {
        "kt", "kts" -> "fun val var data object companion suspend sealed override lateinit by where is in when as typealias reified inline crossinline noinline internal actual expect".split(' ')
        "java" -> "void byte short int long float double char boolean String package import class interface enum record extends implements throws throw new instanceof public private protected static final abstract synchronized".split(' ')
        "js", "jsx", "mjs", "cjs", "ts", "tsx" -> "function const let var async await export import default class extends implements interface type return console Promise undefined typeof instanceof".split(' ')
        "py" -> "def class self elif pass raise with as from import lambda yield nonlocal global True False None print range len".split(' ')
        "html", "htm", "xml", "svg" -> "html head body div span main section article header footer script style class id href src title meta link input button form label ul li img p h1 h2".split(' ')
        "css", "scss", "less" -> "color background display position flex grid margin padding border width height font-size align-items justify-content gap".split(' ')
        "c", "h" -> "int char float double void struct typedef enum static const include define sizeof NULL".split(' ')
        "cpp", "cc", "cxx", "hpp", "hh", "hxx" -> "int char float double void class struct template typename namespace using public private protected virtual override constexpr nullptr auto std vector string cout cin include define".split(' ')
        "json", "jsonc" -> "true false null".split(' ')
        "sh", "bash", "zsh" -> "if then elif else fi for in do done case esac function export local echo printf read".split(' ')
        else -> emptyList()
    }
    return (common + specific).distinct()
}

private fun wordAtCursor(text: String, line: Int, column: Int): String {
    val currentLine = text.split('\n').getOrNull(line).orEmpty()
    val safeColumn = column.coerceIn(0, currentLine.length)
    return currentLine.take(safeColumn).takeLastWhile { it.isLetterOrDigit() || it == '_' || it == '$' }
}

@Composable
private fun ExtraKeyRow(onInsert: (String) -> Unit) {
    val items = listOf("TAB", "(", ")", "{", "}", "[", "]", "<", ">", ";", "=", "\"", "'", "/", "\\", "←", "↑", "↓", "→")
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow).horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        items.forEach { value ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(9.dp),
                modifier = Modifier.height(36.dp).width(if (value == "TAB") 48.dp else 34.dp).clickable { onInsert(value) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(value, style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp), fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun EditorNavigationBar(
    onFiles: () -> Unit,
    onSearch: () -> Unit,
    onSnippets: () -> Unit,
    onBuild: () -> Unit,
    onSettings: () -> Unit
) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, windowInsets = WindowInsets.navigationBars) {
        NavigationBarItem(selected = false, onClick = onFiles, icon = { Icon(Icons.Default.FolderOpen, "File") }, label = { Text("File") })
        NavigationBarItem(selected = false, onClick = onSearch, icon = { Icon(Icons.Default.Search, "Cari") }, label = { Text("Cari") })
        NavigationBarItem(selected = false, onClick = onBuild, icon = { Icon(Icons.Default.Build, "Build") }, label = { Text("Build") })
        NavigationBarItem(selected = false, onClick = onSnippets, icon = { Icon(Icons.Default.SnippetFolder, "Snippet") }, label = { Text("Snippet") })
        NavigationBarItem(selected = false, onClick = onSettings, icon = { Icon(Icons.Default.Settings, "Pengaturan") }, label = { Text("Setelan") })
    }
}

@Composable
private fun SettingsScreen(
    modifier: Modifier,
    darkTheme: Boolean,
    amoled: Boolean,
    dynamicColor: Boolean,
    onDarkChanged: (Boolean) -> Unit,
    onAmoledChanged: (Boolean) -> Unit,
    onDynamicChanged: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Tampilan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        SettingSwitchRow(Icons.Default.DarkMode, "Tema gelap", "Nyaman untuk coding malam hari", darkTheme, onDarkChanged)
        SettingSwitchRow(Icons.Default.WbSunny, "Hitam AMOLED", "Latar hitam untuk layar OLED", amoled, onAmoledChanged, enabled = darkTheme)
        SettingSwitchRow(Icons.Default.WbSunny, "Dynamic color", "Ikuti warna sistem Android 12+", dynamicColor, onDynamicChanged)
        HorizontalDivider()
        Text("Editor", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        SettingInfoRow(Icons.Default.Code, "Mesin editor", "Sora Editor · TextMate grammar")
        SettingInfoRow(Icons.Default.SnippetFolder, "Snippet", "Pack lokal + unduhan per bahasa")
        SettingInfoRow(Icons.Default.Terminal, "Build lokal", "Toolchain dan build Gradle berada di fase selanjutnya")
        HorizontalDivider()
        Text("Tentang Sally IDE", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text("Versi 0.1.0 · dibuat oleh sally", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("MVP editor kode Android. Syntax highlight, tab, pencarian, file SAF, dan snippet tersedia; build APK dari dalam HP belum diimplementasikan pada tahap ini.", style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Selesai") }
    }
}

@Composable
private fun SettingSwitchRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, summary: String, checked: Boolean, onChecked: (Boolean) -> Unit, enabled: Boolean = true) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChecked, enabled = enabled)
    }
}

@Composable
private fun SettingInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(40.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer) }
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.Medium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SnippetsDialog(snippets: List<Snippet>, status: String, language: String, onDismiss: () -> Unit, onInsert: (Snippet) -> Unit) {
    var search by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Snippet $language", fontWeight = FontWeight.Bold)
                Text(status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(Modifier.fillMaxWidth()) {
                OutlinedTextField(value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Cari snippet") }, singleLine = true, leadingIcon = { Icon(Icons.Default.Search, null) })
                Spacer(Modifier.height(8.dp))
                if (snippets.isEmpty()) {
                    Text("Belum ada snippet untuk bahasa ini. Sambungkan internet saat membuka file yang belum punya pack agar Sally mencoba mengunduhnya.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    LazyColumn(Modifier.height(330.dp)) {
                        items(snippets.filter { it.prefix.contains(search, true) || it.name.contains(search, true) }.take(80), key = { "${it.name}-${it.prefix}" }) { snippet ->
                            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onInsert(snippet) }.padding(vertical = 10.dp, horizontal = 6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(snippet.prefix, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                    Icon(Icons.Default.ArrowForward, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(snippet.description.ifBlank { snippet.name }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup") } },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.94f)
    )
}

@Composable
private fun FileKindIcon(fileName: String, modifier: Modifier = Modifier) {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    val tint = when (ext) {
        "kt", "kts" -> Color(0xFF9C6ADE)
        "java" -> Color(0xFFE99B48)
        "html", "htm" -> Color(0xFFE66B42)
        "css", "scss" -> Color(0xFF4A91E2)
        "js", "jsx", "ts", "tsx" -> Color(0xFFD6B546)
        "json", "xml", "yaml", "yml", "toml" -> Color(0xFF3CA88B)
        "py" -> Color(0xFF4A96C6)
        "cpp", "c", "h", "hpp" -> Color(0xFF598DE0)
        "md", "mdx" -> Color(0xFF7794AA)
        else -> MaterialTheme.colorScheme.primary
    }
    Icon(Icons.Default.InsertDriveFile, contentDescription = "$ext file", modifier = modifier, tint = tint)
}

@Composable
private fun AppMark(modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary, shape = RoundedCornerShape(12.dp), modifier = modifier) {
        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Code, "Sally IDE", modifier = Modifier.size(24.dp)) }
    }
}
