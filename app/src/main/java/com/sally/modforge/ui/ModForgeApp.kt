@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.sally.modforge.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.sally.modforge.data.ModLoader
import com.sally.modforge.data.ModProject
import com.sally.modforge.data.ModTemplate
import com.sally.modforge.data.ProjectRepository
import java.io.File
import kotlinx.coroutines.launch

private enum class Destination(val label: String) {
    PROJECTS("Projects"), GITHUB("GitHub & Build"), TEMPLATES("Templates"), SETTINGS("Settings")
}

private val workspaceTabs = listOf("Overview", "Code", "Visual", "Models", "Build")

@Composable
fun ModForgeApp() {
    var darkTheme by rememberSaveable { mutableStateOf(false) }
    ModForgeTheme(darkTheme = darkTheme) {
        ModForgeContent(darkTheme = darkTheme, onDarkThemeChange = { darkTheme = it })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModForgeContent(darkTheme: Boolean, onDarkThemeChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) { ProjectRepository(context.applicationContext) }
    var projects by remember { mutableStateOf(repository.listProjects()) }
    var destination by rememberSaveable { mutableStateOf(Destination.PROJECTS) }
    var selectedProject by remember { mutableStateOf<ModProject?>(null) }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var templateForNewProject by remember { mutableStateOf(ModTemplate.EMPTY) }
    var workspaceTab by rememberSaveable { mutableStateOf(0) }
    var selectedFile by remember { mutableStateOf<String?>(null) }
    var editorText by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<ModProject?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun refresh() { projects = repository.listProjects() }

    LaunchedEffect(selectedProject?.id, selectedFile) {
        val project = selectedProject
        val path = selectedFile
        if (project != null && path != null) editorText = repository.readFile(project.id, path)
    }

    BackHandler(enabled = selectedProject != null) {
        selectedProject = null
        selectedFile = null
        workspaceTab = 0
        refresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(selectedProject?.name ?: "ModForge Studio", fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (selectedProject == null) Text(destination.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    if (selectedProject != null) {
                        IconButton(onClick = {
                            selectedProject = null
                            selectedFile = null
                            workspaceTab = 0
                            refresh()
                        }) { Icon(Icons.Outlined.FolderOpen, contentDescription = "Kembali ke projects") }
                    }
                },
                actions = {
                    if (selectedProject != null && workspaceTab == 1) {
                        TextButton(onClick = {
                            selectedProject?.let { project ->
                                selectedFile?.let { path -> repository.writeFile(project.id, path, editorText) }
                                message = "Tersimpan lokal ✓"
                                refresh()
                                scope.launch { snackbarHostState.showSnackbar("File disimpan di perangkat") }
                            }
                        }) { Text("Save") }
                    } else if (selectedProject != null && workspaceTab == 4) {
                        IconButton(onClick = { message = "Build cloud belum terhubung di milestone ini." }) {
                            Icon(Icons.Outlined.Build, contentDescription = "Build")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (selectedProject == null) {
                NavigationBar {
                    Destination.entries.forEach { item ->
                        NavigationBarItem(
                            selected = destination == item,
                            onClick = { destination = item },
                            icon = {
                                Icon(
                                    when (item) {
                                        Destination.PROJECTS -> Icons.Outlined.FolderOpen
                                        Destination.GITHUB -> Icons.Outlined.Cloud
                                        Destination.TEMPLATES -> Icons.Outlined.Extension
                                        Destination.SETTINGS -> Icons.Outlined.Settings
                                    },
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label, maxLines = 1) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (selectedProject == null && destination == Destination.PROJECTS) {
                FloatingActionButton(onClick = {
                    templateForNewProject = ModTemplate.EMPTY
                    showCreateDialog = true
                }) {
                    Icon(Icons.Outlined.Add, contentDescription = "Project baru")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (selectedProject != null) {
                WorkspaceScreen(
                    project = selectedProject!!,
                    repository = repository,
                    tabIndex = workspaceTab,
                    onTabChange = { workspaceTab = it },
                    selectedFile = selectedFile,
                    onSelectFile = { selectedFile = it },
                    editorText = editorText,
                    onEditorTextChange = { editorText = it },
                    message = message,
                    onMessage = { message = it },
                    onExport = { project -> shareProjectZip(context, repository, project) },
                    onRefresh = { refresh() }
                )
            } else {
                when (destination) {
                    Destination.PROJECTS -> ProjectsScreen(
                        projects = projects,
                        onOpen = { project ->
                            selectedProject = project
                            workspaceTab = 0
                            selectedFile = null
                            message = null
                        },
                        onDelete = { project -> deleteTarget = project },
                        onCreate = { showCreateDialog = true }
                    )
                    Destination.GITHUB -> GitHubBuildScreen()
                    Destination.TEMPLATES -> TemplatesScreen(onUseTemplate = { template ->
                        templateForNewProject = template
                        showCreateDialog = true
                    })
                    Destination.SETTINGS -> SettingsScreen(darkTheme, onDarkThemeChange)
                }
            }
        }
    }

    if (deleteTarget != null) {
        val target = deleteTarget!!
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Hapus ${target.name}?") },
            text = { Text("Source lokal project ini akan dihapus permanen dari aplikasi. Ekspor ZIP terlebih dahulu bila ingin menyimpannya.") },
            confirmButton = {
                Button(onClick = {
                    repository.deleteProject(target.id)
                    deleteTarget = null
                    refresh()
                    scope.launch { snackbarHostState.showSnackbar("Project ${target.name} dihapus") }
                }) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Batal") } }
        )
    }

    if (showCreateDialog) {
        CreateProjectDialog(
            initialTemplate = templateForNewProject,
            onDismiss = { showCreateDialog = false },
            onCreate = { draft ->
                val created = repository.createProject(draft)
                showCreateDialog = false
                destination = Destination.PROJECTS
                selectedProject = created
                workspaceTab = 0
                selectedFile = null
                message = "Project dibuat · file tersimpan lokal"
                refresh()
            }
        )
    }
}

@Composable
private fun ProjectsScreen(
    projects: List<ModProject>,
    onOpen: (ModProject) -> Unit,
    onDelete: (ModProject) -> Unit,
    onCreate: () -> Unit
) {
    if (projects.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(shape = RoundedCornerShape(32.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Box(Modifier.padding(26.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Extension, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("Buat mod pertamamu", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Mulai dengan template, edit file mod, lalu ekspor project untuk dibuild.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))
            Button(onClick = onCreate) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text("Project Baru") }
            Spacer(Modifier.height(12.dp))
            Text("Save selalu lokal. Integrasi GitHub cloud build ditandai terpisah.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Projects", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("${projects.size} project tersimpan di perangkat", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledTonalButton(onClick = onCreate) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(6.dp)); Text("Baru") }
            }
            Spacer(Modifier.height(16.dp))
            projects.forEach { project ->
                ProjectCard(project, onOpen = { onOpen(project) }, onDelete = { onDelete(project) })
                Spacer(Modifier.height(12.dp))
            }
            Spacer(Modifier.height(64.dp))
        }
    }
}

@Composable
private fun ProjectCard(project: ModProject, onOpen: () -> Unit, onDelete: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    ElevatedCard(onClick = onOpen, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Card(shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Icon(Icons.Outlined.Extension, contentDescription = null, modifier = Modifier.padding(12.dp).size(26.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(project.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${project.modId} · ${project.minecraftVersion}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Outlined.MoreVert, contentDescription = "Menu project") }
                    androidx.compose.material3.DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Hapus project") },
                            leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null) },
                            onClick = { menuOpen = false; onDelete() }
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                LoaderBadge(project.loader)
                Tag(text = project.template.label)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Outlined.Description, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(project.buildStatus, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun LoaderBadge(loader: ModLoader) {
    val color = when (loader) {
        ModLoader.FABRIC -> MaterialTheme.colorScheme.primaryContainer
        ModLoader.FORGE -> MaterialTheme.colorScheme.secondaryContainer
        ModLoader.NEOFORGE -> MaterialTheme.colorScheme.tertiaryContainer
    }
    Card(shape = RoundedCornerShape(50), colors = CardDefaults.cardColors(containerColor = color)) {
        Text(loader.label, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun Tag(text: String) {
    Card(shape = RoundedCornerShape(50), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Text(text, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun WorkspaceScreen(
    project: ModProject,
    repository: ProjectRepository,
    tabIndex: Int,
    onTabChange: (Int) -> Unit,
    selectedFile: String?,
    onSelectFile: (String) -> Unit,
    editorText: String,
    onEditorTextChange: (String) -> Unit,
    message: String?,
    onMessage: (String?) -> Unit,
    onExport: (ModProject) -> Unit,
    onRefresh: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = tabIndex, edgePadding = 12.dp) {
            workspaceTabs.forEachIndexed { index, title ->
                Tab(selected = tabIndex == index, onClick = { onTabChange(index) }, text = { Text(title) })
            }
        }
        when (tabIndex) {
            0 -> OverviewScreen(project, repository, onExport)
            1 -> CodeScreen(project, repository, selectedFile, onSelectFile, editorText, onEditorTextChange, message, onMessage, onRefresh)
            2 -> RoadmapFeatureScreen(
                icon = Icons.Outlined.Code,
                title = "Visual Scripting",
                description = "Canvas node untuk Event → Kondisi → Aksi akan ditambahkan pada fase berikutnya. Project file lokal tetap bisa diedit dari tab Code.",
                bullets = listOf("Node graph dan port bertipe", "Generator kode Java per loader", "Validasi graph sebelum build")
            )
            3 -> RoadmapFeatureScreen(
                icon = Icons.Outlined.ViewInAr,
                title = "3D Item / Block Editor",
                description = "Editor model visual masih menjadi roadmap. Untuk sekarang kamu bisa memasukkan JSON model dan PNG texture melalui file project.",
                bullets = listOf("Viewport 3D dan gizmo", "Edit elemen cube dan UV", "Preview inventory / tangan / tanah")
            )
            else -> BuildScreen(project, onExport, onMessage)
        }
    }
}

@Composable
private fun OverviewScreen(project: ModProject, repository: ProjectRepository, onExport: (ModProject) -> Unit) {
    val files = remember(project.id) { repository.listFiles(project.id) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Text(project.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.height(6.dp))
                Text(project.description, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LoaderBadge(project.loader)
                    Tag("MC ${project.minecraftVersion}")
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("Ringkasan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard("Files", files.size.toString(), Modifier.weight(1f))
            MetricCard("Template", project.template.label, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard("Versi mod", project.modVersion, Modifier.weight(1f))
            MetricCard("Build", "Belum berjalan", Modifier.weight(1f))
        }
        Spacer(Modifier.height(20.dp))
        Text("Langkah selanjutnya", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        InfoRow("1", "Buka Code untuk melihat dan mengedit file.")
        InfoRow("2", "Ekspor ZIP project atau unggah ke repository GitHub.")
        InfoRow("3", "Jalankan workflow build di tab Actions repository.")
        Spacer(Modifier.height(16.dp))
        Button(onClick = { onExport(project) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.FolderOpen, null); Spacer(Modifier.width(8.dp)); Text("Ekspor project ZIP")
        }
        Spacer(Modifier.height(6.dp))
        Text("Build cloud otomatis dari aplikasi belum aktif di versi starter ini.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun InfoRow(number: String, text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Card(shape = RoundedCornerShape(50), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Text(number, modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp), fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CodeScreen(
    project: ModProject,
    repository: ProjectRepository,
    selectedFile: String?,
    onSelectFile: (String) -> Unit,
    editorText: String,
    onEditorTextChange: (String) -> Unit,
    message: String?,
    onMessage: (String?) -> Unit,
    onRefresh: () -> Unit
) {
    val files = repository.listFiles(project.id)
    var showNewFileDialog by remember { mutableStateOf(false) }
    var newFilePath by remember { mutableStateOf("src/main/java/${project.packageName.replace('.', '/')}/NewFeature.java") }
    var newFileError by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("File explorer", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = { showNewFileDialog = true }) { Icon(Icons.Outlined.Add, contentDescription = "File baru") }
        }
        Spacer(Modifier.height(6.dp))
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.fillMaxWidth().height(156.dp).verticalScroll(rememberScrollState()).padding(8.dp)) {
                files.forEach { path ->
                    val active = path == selectedFile
                    TextButton(onClick = { onSelectFile(path); onMessage(null) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(if (path.endsWith(".java")) Icons.Outlined.Code else Icons.Outlined.Description, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(path, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = FontFamily.Monospace, color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(selectedFile ?: "Pilih file", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            FilledTonalButton(onClick = {
                if (selectedFile != null) {
                    repository.writeFile(project.id, selectedFile, editorText)
                    onMessage("Tersimpan lokal ✓")
                    onRefresh()
                }
            }, enabled = selectedFile != null) { Text("Save") }
        }
        if (message != null) Text(message, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(6.dp))
        if (selectedFile == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Pilih file dari daftar untuk mulai mengedit.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            OutlinedTextField(
                value = editorText,
                onValueChange = onEditorTextChange,
                modifier = Modifier.fillMaxWidth().weight(1f),
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 19.sp),
                shape = RoundedCornerShape(16.dp),
                placeholder = { Text("Isi file…") },
                supportingText = { Text("Editor dasar · simpan manual · belum ada syntax highlighting") }
            )
        }
    }
    if (showNewFileDialog) {
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false; newFileError = null },
            title = { Text("Buat file baru") },
            text = {
                Column {
                    Text("Masukkan path relatif dari root project.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newFilePath,
                        onValueChange = { newFilePath = it; newFileError = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Path file") },
                        singleLine = true
                    )
                    if (newFileError != null) Text(newFileError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val path = newFilePath.trim().replace('\\', '/')
                    if (path.isBlank() || path.startsWith("/") || path.split('/').any { it == ".." } || path.endsWith('/')) {
                        newFileError = "Path tidak valid. Gunakan path relatif, tanpa segmen '..'."
                    } else {
                        repository.writeFile(project.id, path, "")
                        showNewFileDialog = false
                        newFileError = null
                        onSelectFile(path)
                        onMessage("File baru dibuat · belum disimpan ke remote")
                        onRefresh()
                    }
                }) { Text("Buat") }
            },
            dismissButton = { TextButton(onClick = { showNewFileDialog = false; newFileError = null }) { Text("Batal") } }
        )
    }
}

@Composable
private fun BuildScreen(project: ModProject, onExport: (ModProject) -> Unit, onMessage: (String?) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Build, contentDescription = null, modifier = Modifier.size(30.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Build status", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Belum ada run", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text("Di versi ini aplikasi sudah membuat file workflow untuk project. Hubungkan repository secara manual dan jalankan Actions; kontrol OAuth, push, polling log, dan unduh artifact otomatis belum diimplementasikan.")
                Spacer(Modifier.height(16.dp))
                Button(onClick = { onExport(project) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.FolderOpen, null); Spacer(Modifier.width(8.dp)); Text("Ekspor ZIP untuk GitHub")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { onMessage("Workflow ada di .github/workflows/build.yml pada ZIP project.") }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Description, null); Spacer(Modifier.width(8.dp)); Text("Info workflow")
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Pipeline yang disiapkan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        InfoRow("1", "Checkout source code dari repository.")
        InfoRow("2", "Pasang Java dan Gradle yang dipilih oleh template.")
        InfoRow("3", "Jalankan Gradle build dan upload JAR sebagai artifact.")
    }
}

@Composable
private fun GitHubBuildScreen() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(20.dp)) {
                Icon(Icons.Outlined.Cloud, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.height(12.dp))
                Text("GitHub & Build", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.height(6.dp))
                Text("Cloud build adalah bagian inti roadmap, tetapi belum dikoneksikan pada starter ini. Aplikasi tidak meminta token atau menyimpan credential dulu.", color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Status fitur", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        FeatureStatusRow("Export project ZIP", true)
        FeatureStatusRow("Workflow GitHub Actions di project", true)
        FeatureStatusRow("GitHub Device Flow / OAuth", false)
        FeatureStatusRow("Buat repo dan push commit dari app", false)
        FeatureStatusRow("Pantau run, log, dan download JAR otomatis", false)
        Spacer(Modifier.height(14.dp))
        Text("Keamanan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text("Integrasi token akan memakai penyimpanan terenkripsi Android dan tidak akan menaruh token di file project, workflow, maupun log build.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FeatureStatusRow(label: String, done: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (done) Icons.Outlined.CheckCircle else Icons.Outlined.Cloud, contentDescription = null, tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(label, modifier = Modifier.weight(1f))
        Tag(if (done) "Siap" else "Roadmap")
    }
}

@Composable
private fun TemplatesScreen(onUseTemplate: (ModTemplate) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Templates", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Pilih contoh untuk memulai project baru.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        ModTemplate.entries.forEach { template ->
            ElevatedCard(onClick = { onUseTemplate(template) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Icon(if (template == ModTemplate.EMPTY) Icons.Outlined.Extension else Icons.Outlined.Code, null, modifier = Modifier.padding(12.dp).size(24.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(template.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(template.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Outlined.Add, contentDescription = "Gunakan template")
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        Card(shape = RoundedCornerShape(18.dp)) {
            Text("Catatan: contoh kode Item, Block, dan Command saat ini dibuat khusus untuk Fabric 1.21.1. Profile Forge/NeoForge menghasilkan starter class dan konfigurasi build.", Modifier.padding(14.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsScreen(darkTheme: Boolean, onDarkThemeChange: (Boolean) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        Card(shape = RoundedCornerShape(22.dp)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Dark theme", style = MaterialTheme.typography.titleMedium)
                    Text("Gunakan tema gelap untuk UI editor.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = darkTheme, onCheckedChange = onDarkThemeChange)
            }
        }
        Spacer(Modifier.height(10.dp))
        Card(shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Penyimpanan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Project disimpan di storage privat aplikasi. Gunakan Export ZIP untuk memindahkan source ke GitHub atau komputer.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(10.dp))
        Card(shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Tentang", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("ModForge Studio · 0.1.0 · dibuat oleh sally", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun RoadmapFeatureScreen(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String, bullets: List<String>) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(28.dp))
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Icon(icon, contentDescription = null, modifier = Modifier.padding(24.dp).size(56.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(22.dp))
        Card(shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Rencana implementasi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                bullets.forEachIndexed { index, bullet -> InfoRow((index + 1).toString(), bullet) }
            }
        }
    }
}

@Composable
private fun CreateProjectDialog(initialTemplate: ModTemplate, onDismiss: () -> Unit, onCreate: (ModProject) -> Unit) {
    var step by rememberSaveable { mutableStateOf(0) }
    var loader by rememberSaveable { mutableStateOf(ModLoader.FABRIC) }
    var minecraftVersion by rememberSaveable { mutableStateOf("1.21.1") }
    var name by rememberSaveable { mutableStateOf("") }
    var modId by rememberSaveable { mutableStateOf("") }
    var packageName by rememberSaveable { mutableStateOf("com.example.examplemod") }
    var modVersion by rememberSaveable { mutableStateOf("1.0.0") }
    var author by rememberSaveable { mutableStateOf("sally") }
    var description by rememberSaveable { mutableStateOf("A Minecraft Java mod") }
    var license by rememberSaveable { mutableStateOf("MIT") }
    var template by rememberSaveable { mutableStateOf(initialTemplate) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Project baru", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Langkah ${step + 1} dari 4", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(Modifier.fillMaxWidth().height(420.dp).verticalScroll(rememberScrollState())) {
                when (step) {
                    0 -> {
                        Text("Loader & versi Minecraft", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ModLoader.entries.forEach { option ->
                                FilterChip(selected = loader == option, onClick = {
                                    loader = option
                                    minecraftVersion = when (option) { ModLoader.FABRIC -> "1.21.1"; ModLoader.FORGE -> "1.20.1"; ModLoader.NEOFORGE -> "1.21.1" }
                                }, label = { Text(option.label) })
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Text("Contoh versi yang diprofilkan", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val versions = when (loader) {
                                ModLoader.FABRIC -> listOf("1.21.1", "1.20.1", "1.19.2")
                                ModLoader.FORGE -> listOf("1.20.1", "1.19.2", "1.18.2")
                                ModLoader.NEOFORGE -> listOf("1.21.1", "1.21.4", "1.20.4")
                            }
                            versions.forEach { version -> FilterChip(selected = minecraftVersion == version, onClick = { minecraftVersion = version }, label = { Text(version) }) }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = minecraftVersion, onValueChange = { minecraftVersion = it.trim() }, modifier = Modifier.fillMaxWidth(), label = { Text("Versi Minecraft") }, singleLine = true)
                        Spacer(Modifier.height(8.dp))
                        Text("Java: ${recommendedJava(minecraftVersion)} (rekomendasi awal; wajib diverifikasi untuk kombinasi loader/versi ini).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    1 -> {
                        Text("Info mod", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(name, { value -> name = value; val generated = normalizeModId(value); modId = generated; packageName = "com.${normalizeModId(author.ifBlank { "example" })}.$generated" }, label = { Text("Nama mod") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(modId, { modId = normalizeModId(it) }, label = { Text("Mod ID") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(packageName, { packageName = normalizePackage(it) }, label = { Text("Java package") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(modVersion, { modVersion = it }, label = { Text("Versi mod") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(author, { author = it }, label = { Text("Author") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(description, { description = it }, label = { Text("Deskripsi") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                        OutlinedTextField(license, { license = it }, label = { Text("Lisensi") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    }
                    2 -> {
                        Text("Build configuration", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Text("Java ${recommendedJava(minecraftVersion)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Gradle akan ditentukan oleh template loader. Saat ini tidak ada JDK atau Gradle yang dijalankan di HP; file workflow disiapkan untuk GitHub Actions.")
                        Spacer(Modifier.height(12.dp))
                        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                            Column(Modifier.padding(14.dp)) {
                                Text("Belum terhubung ke GitHub", fontWeight = FontWeight.SemiBold)
                                Text("Project akan dibuat secara lokal. Setelah diekspor, unggah ke GitHub lalu jalankan workflow build.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    else -> {
                        Text("Pilih template", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        ModTemplate.entries.forEach { option ->
                            Card(
                                onClick = { template = option },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = if (template == option) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(if (template == option) Icons.Outlined.CheckCircle else Icons.Outlined.Extension, null)
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(option.label, fontWeight = FontWeight.SemiBold)
                                        Text(option.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("Ringkasan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("$name · ${loader.label} · Minecraft $minecraftVersion · ${template.label}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            if (step < 3) {
                Button(onClick = {
                    error = when (step) {
                        0 -> if (minecraftVersion.matches(Regex("\\d+\\.\\d+(\\.\\d+)?"))) null else "Format versi tidak valid. Contoh: 1.21.1"
                        1 -> if (name.isBlank() || modId.isBlank() || packageName.isBlank()) "Isi nama mod, ID, dan package terlebih dahulu." else null
                        else -> null
                    }
                    if (error == null) step++
                }) { Text("Lanjut") }
            } else {
                Button(onClick = {
                    if (name.isBlank() || modId.isBlank() || packageName.isBlank()) {
                        error = "Lengkapi nama, mod ID, dan package."
                    } else {
                        onCreate(ModProject(name = name.trim(), modId = modId, packageName = packageName, modVersion = modVersion.ifBlank { "1.0.0" }, author = author.trim(), description = description.trim(), license = license.ifBlank { "MIT" }, loader = loader, minecraftVersion = minecraftVersion, template = template))
                    }
                }) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(5.dp)); Text("Buat") }
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { if (step > 0) { step--; error = null } else onDismiss() }) { Text(if (step > 0) "Kembali" else "Batal") }
            }
        }
    )
}

private fun normalizeModId(input: String): String = input.lowercase()
    .replace(Regex("[^a-z0-9_]+"), "_")
    .trim('_')
    .ifBlank { "example_mod" }

private fun normalizePackage(input: String): String = input.lowercase()
    .replace(Regex("[^a-z0-9_.]"), "")
    .split('.')
    .filter { it.isNotBlank() }
    .joinToString(".") { part -> if (part.first().isDigit()) "_${part}" else part }
    .ifBlank { "com.example.examplemod" }

private fun recommendedJava(version: String): Int = when {
    version.startsWith("1.16") -> 8
    version.startsWith("1.17") -> 16
    version.startsWith("1.18") || version.startsWith("1.19") || version.startsWith("1.20.1") || version.startsWith("1.20.2") || version.startsWith("1.20.3") || version.startsWith("1.20.4") -> 17
    else -> 21
}

private fun shareProjectZip(context: Context, repository: ProjectRepository, project: ModProject) {
    runCatching {
        val exports = File(context.cacheDir, "exports").apply { mkdirs() }
        val zip = File(exports, "${project.modId}-${project.minecraftVersion}.zip")
        repository.exportZip(project.id, zip)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", zip)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "${project.name} source ZIP")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Ekspor project"))
    }.onFailure { error ->
        Toast.makeText(context, "Ekspor gagal: ${error.message ?: "kesalahan tidak diketahui"}", Toast.LENGTH_LONG).show()
    }
}
