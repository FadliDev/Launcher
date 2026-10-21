package com.sally.ide.ui.explorer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sally.ide.data.ProjectManager
import java.io.File

/**
 * Pembungkus layout: drawer modal di HP, sidebar tetap di layar lebar (tablet/landscape).
 * Bila [enabled] false (belum ada proyek / bukan layar editor) isinya ditampilkan apa adanya.
 */
@Composable
fun ExplorerHost(
    wide: Boolean,
    enabled: Boolean,
    drawerState: DrawerState,
    drawerContent: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    if (!enabled) {
        content()
    } else if (wide) {
        Row(Modifier.fillMaxSize()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.width(300.dp).fillMaxHeight()
            ) { drawerContent() }
            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Box(Modifier.weight(1f).fillMaxHeight()) { content() }
        }
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            // Geser-buka dimatikan agar tidak bentrok dengan scroll horizontal editor; buka lewat tombol.
            gesturesEnabled = drawerState.isOpen,
            drawerContent = { ModalDrawerSheet { drawerContent() } },
            content = content
        )
    }
}

private data class TreeRow(val file: File, val depth: Int)

private sealed interface ExplorerDialog {
    data class NewFile(val parent: File) : ExplorerDialog
    data class NewFolder(val parent: File) : ExplorerDialog
    data class Rename(val file: File) : ExplorerDialog
    data class Delete(val file: File) : ExplorerDialog
}

private fun flatten(dir: File, expanded: Set<String>, depth: Int, out: MutableList<TreeRow>) {
    for (child in ProjectManager.children(dir)) {
        out.add(TreeRow(child, depth))
        if (child.isDirectory && child.absolutePath in expanded) flatten(child, expanded, depth + 1, out)
    }
}

private fun fileTint(name: String, fallback: Color): Color = when (name.substringAfterLast('.', "").lowercase()) {
    "kt", "kts" -> Color(0xFF9C6ADE)
    "java" -> Color(0xFFE99B48)
    "html", "htm" -> Color(0xFFE66B42)
    "css", "scss" -> Color(0xFF4A91E2)
    "js", "jsx", "ts", "tsx" -> Color(0xFFD6B546)
    "json", "xml", "yaml", "yml", "toml" -> Color(0xFF3CA88B)
    "py" -> Color(0xFF4A96C6)
    "cpp", "c", "h", "hpp" -> Color(0xFF598DE0)
    "md", "mdx" -> Color(0xFF7794AA)
    "gradle" -> Color(0xFF5FAE6E)
    else -> fallback
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExplorerPanel(
    projectDir: File,
    refreshTick: Int,
    activePath: String?,
    dirtyPaths: Set<String>,
    modifier: Modifier = Modifier,
    onOpenFile: (File) -> Unit,
    onRenamed: (File, File) -> Unit,
    onDeleted: (File) -> Unit,
    onChanged: () -> Unit,
    onMessage: (String) -> Unit,
    onSearch: () -> Unit,
    onExport: () -> Unit,
    onCloseProject: () -> Unit
) {
    var expanded by remember(projectDir) { mutableStateOf(setOf(projectDir.absolutePath)) }
    var localTick by remember { mutableIntStateOf(0) }
    var dialog by remember { mutableStateOf<ExplorerDialog?>(null) }
    var menuFor by remember { mutableStateOf<String?>(null) }
    var headerMenu by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }

    val rows = remember(projectDir, expanded, refreshTick, localTick) {
        val out = ArrayList<TreeRow>()
        flatten(projectDir, expanded, 0, out)
        out as List<TreeRow>
    }

    fun changed() { localTick++; onChanged() }

    fun parentFor(file: File): File = if (file.isDirectory) file else file.parentFile ?: projectDir

    fun expand(dir: File) { expanded = expanded + dir.absolutePath }

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("EXPLORER", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(projectDir.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = { nameInput = ""; dialog = ExplorerDialog.NewFile(projectDir) }) { Icon(Icons.Default.NoteAdd, "File baru") }
            IconButton(onClick = { nameInput = ""; dialog = ExplorerDialog.NewFolder(projectDir) }) { Icon(Icons.Default.CreateNewFolder, "Folder baru") }
            Box {
                IconButton(onClick = { headerMenu = true }) { Icon(Icons.Default.MoreVert, "Menu explorer") }
                DropdownMenu(expanded = headerMenu, onDismissRequest = { headerMenu = false }) {
                    DropdownMenuItem(text = { Text("Cari di proyek") }, leadingIcon = { Icon(Icons.Default.Search, null) }, onClick = { headerMenu = false; onSearch() })
                    DropdownMenuItem(text = { Text("Muat ulang") }, leadingIcon = { Icon(Icons.Default.Refresh, null) }, onClick = { headerMenu = false; changed() })
                    DropdownMenuItem(text = { Text("Ciutkan semua") }, leadingIcon = { Icon(Icons.Default.UnfoldLess, null) }, onClick = { headerMenu = false; expanded = setOf(projectDir.absolutePath) })
                    DropdownMenuItem(text = { Text("Ekspor ZIP") }, leadingIcon = { Icon(Icons.Default.FileDownload, null) }, onClick = { headerMenu = false; onExport() })
                    DropdownMenuItem(text = { Text("Tutup proyek") }, leadingIcon = { Icon(Icons.Default.FolderOpen, null) }, onClick = { headerMenu = false; onCloseProject() })
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        if (rows.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Folder kosong", fontWeight = FontWeight.Medium)
                Text("Buat file atau folder dengan tombol di atas.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(rows, key = { it.file.absolutePath }) { row ->
                    val file = row.file
                    val isDir = file.isDirectory
                    val isOpen = isDir && file.absolutePath in expanded
                    val selected = file.absolutePath == activePath
                    val dim = file.name == "build" || file.name == ".gradle" || file.name == ".git"
                    Box {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                                .combinedClickable(
                                    onClick = {
                                        if (isDir) {
                                            expanded = if (isOpen) expanded - file.absolutePath else expanded + file.absolutePath
                                        } else onOpenFile(file)
                                    },
                                    onLongClick = { menuFor = file.absolutePath }
                                )
                                .alpha(if (dim) 0.55f else 1f)
                                .padding(start = (10 + row.depth * 16).dp, end = 8.dp, top = 9.dp, bottom = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isDir) {
                                Icon(if (isOpen) Icons.Default.ExpandMore else Icons.Default.ChevronRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(2.dp))
                                Icon(if (isOpen) Icons.Default.FolderOpen else Icons.Default.Folder, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            } else {
                                Spacer(Modifier.width(20.dp))
                                Icon(Icons.Default.InsertDriveFile, null, Modifier.size(20.dp), tint = fileTint(file.name, MaterialTheme.colorScheme.onSurfaceVariant))
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(file.name, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                            if (file.absolutePath in dirtyPaths) {
                                Box(Modifier.size(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                            }
                        }
                        DropdownMenu(expanded = menuFor == file.absolutePath, onDismissRequest = { menuFor = null }) {
                            DropdownMenuItem(text = { Text("File baru") }, leadingIcon = { Icon(Icons.Default.NoteAdd, null) }, onClick = {
                                menuFor = null; nameInput = ""; dialog = ExplorerDialog.NewFile(parentFor(file))
                            })
                            DropdownMenuItem(text = { Text("Folder baru") }, leadingIcon = { Icon(Icons.Default.CreateNewFolder, null) }, onClick = {
                                menuFor = null; nameInput = ""; dialog = ExplorerDialog.NewFolder(parentFor(file))
                            })
                            DropdownMenuItem(text = { Text("Ganti nama") }, leadingIcon = { Icon(Icons.Default.Edit, null) }, onClick = {
                                menuFor = null; nameInput = file.name; dialog = ExplorerDialog.Rename(file)
                            })
                            DropdownMenuItem(text = { Text("Duplikat") }, leadingIcon = { Icon(Icons.Default.ContentCopy, null) }, onClick = {
                                menuFor = null
                                ProjectManager.duplicate(file)
                                    .onSuccess { changed(); onMessage("${file.name} diduplikat") }
                                    .onFailure { onMessage(it.message ?: "Gagal menduplikasi") }
                            })
                            DropdownMenuItem(text = { Text("Hapus") }, leadingIcon = { Icon(Icons.Default.Delete, null) }, onClick = {
                                menuFor = null; dialog = ExplorerDialog.Delete(file)
                            })
                        }
                    }
                }
            }
        }
    }

    when (val d = dialog) {
        is ExplorerDialog.NewFile, is ExplorerDialog.NewFolder -> {
            val isFolder = d is ExplorerDialog.NewFolder
            val parent = if (d is ExplorerDialog.NewFile) d.parent else (d as ExplorerDialog.NewFolder).parent
            AlertDialog(
                onDismissRequest = { dialog = null },
                title = { Text(if (isFolder) "Folder baru" else "File baru") },
                text = {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        singleLine = true,
                        label = { Text("Nama") },
                        supportingText = { Text("di ${parent.relativeToOrSelf(projectDir.parentFile ?: projectDir).path}") }
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        val name = nameInput.trim()
                        val result = if (isFolder) ProjectManager.createFolder(parent, name) else ProjectManager.createFile(parent, name)
                        result.onSuccess { created ->
                            dialog = null
                            expand(parent)
                            changed()
                            if (!isFolder) onOpenFile(created)
                        }.onFailure { onMessage(it.message ?: "Gagal membuat") }
                    }) { Text("Buat") }
                },
                dismissButton = { TextButton(onClick = { dialog = null }) { Text("Batal") } }
            )
        }
        is ExplorerDialog.Rename -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Ganti nama") },
            text = { OutlinedTextField(value = nameInput, onValueChange = { nameInput = it }, singleLine = true, label = { Text("Nama baru") }) },
            confirmButton = {
                Button(onClick = {
                    val old = d.file
                    ProjectManager.rename(old, nameInput.trim())
                        .onSuccess { renamed ->
                            dialog = null
                            if (old.isDirectory && old.absolutePath in expanded) {
                                expanded = expanded - old.absolutePath + renamed.absolutePath
                            }
                            onRenamed(old, renamed)
                            changed()
                        }
                        .onFailure { onMessage(it.message ?: "Gagal mengganti nama") }
                }) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("Batal") } }
        )
        is ExplorerDialog.Delete -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Hapus ${d.file.name}?") },
            text = { Text(if (d.file.isDirectory) "Folder dan seluruh isinya akan dihapus permanen." else "File akan dihapus permanen.") },
            confirmButton = {
                Button(onClick = {
                    val target = d.file
                    ProjectManager.delete(target)
                        .onSuccess { dialog = null; onDeleted(target); changed() }
                        .onFailure { onMessage(it.message ?: "Gagal menghapus") }
                }) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("Batal") } }
        )
        null -> Unit
    }
}
