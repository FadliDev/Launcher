package com.sally.ide.ui

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sally.ide.IdeViewModel
import com.sally.ide.data.ProjectInfo
import com.sally.ide.data.ProjectKind
import com.sally.ide.data.Template
import com.sally.ide.data.Templates

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: IdeViewModel, snackbar: SnackbarHostState) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val zip = rememberZipActions(vm)
    var showNew by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<ProjectInfo?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Sally IDE") },
                actions = {
                    IconButton(onClick = zip.onImport) { Icon(Ic.Upload, "Impor zip") }
                    IconButton(onClick = { showSettings = true }) { Icon(Ic.Settings, "Setelan") }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showNew = true }, icon = { Icon(Ic.Add, null) }, text = { Text("Proyek baru") })
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            when {
                !vm.loaded -> Unit
                vm.projects.isEmpty() -> EmptyHome(onNew = { showNew = true }, onImport = zip.onImport)
                else -> LazyColumn(Modifier.widthIn(max = 640.dp).fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
                    items(vm.projects, key = { it.dir.path }) { p ->
                        ProjectRow(p, onOpen = { vm.openProject(p.dir) }, onExport = { zip.onExport(p.dir) }, onDelete = { deleting = p })
                    }
                }
            }
        }
    }

    if (showNew) NewProjectDialog(onCreate = { n, t -> showNew = false; vm.createProject(n, t) }, onDismiss = { showNew = false })
    if (showSettings) {
        SettingsSheet(prefs, vm::setThemeMode, vm::setDynamic, vm::setFontSize, vm::setWordWrap, vm::setLineNumbers, vm::setTabSize) { showSettings = false }
    }
    deleting?.let { p ->
        ConfirmDialog(
            "Hapus proyek?", "\"${p.dir.name}\" dan semua isinya akan dihapus permanen. Ekspor zip dulu kalau perlu cadangan.", "Hapus",
            onConfirm = { vm.deleteProject(p.dir); deleting = null }, onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun EmptyHome(onNew: () -> Unit, onImport: () -> Unit) {
    Column(Modifier.padding(32.dp).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
        Icon(Ic.Code, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("Belum ada proyek", style = MaterialTheme.typography.titleLarge)
        Text(
            "Buat dari template atau impor proyek dari file zip.",
            Modifier.padding(top = 4.dp, bottom = 20.dp), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FilledTonalButton(onClick = onNew) { Icon(Ic.Add, null, Modifier.size(18.dp)); Spacer(Modifier.size(8.dp)); Text("Proyek baru") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onImport) { Icon(Ic.Upload, null, Modifier.size(18.dp)); Spacer(Modifier.size(8.dp)); Text("Impor zip") }
    }
}

@Composable
private fun ProjectRow(p: ProjectInfo, onOpen: () -> Unit, onExport: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val whenText = DateUtils.getRelativeTimeSpanString(p.modified, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
    ListItem(
        headlineContent = { Text(p.dir.name, maxLines = 1) },
        supportingContent = { Text(p.kind.label + " · " + whenText) },
        leadingContent = {
            Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(
                    when (p.kind) { ProjectKind.ANDROID -> Ic.Android; ProjectKind.MOD -> Ic.Extension; ProjectKind.PLAIN -> Ic.Folder },
                    null, tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        },
        trailingContent = {
            Box {
                IconButton(onClick = { menu = true }) { Icon(Ic.More, "Menu proyek") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Ekspor zip") }, leadingIcon = { Icon(Ic.Download, null) }, onClick = { menu = false; onExport() })
                    DropdownMenuItem(text = { Text("Hapus") }, leadingIcon = { Icon(Ic.Delete, null) }, onClick = { menu = false; onDelete() })
                }
            }
        },
        modifier = Modifier.clickable(onClick = onOpen),
    )
}

@Composable
private fun NewProjectDialog(onCreate: (String, Template) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var tpl by remember { mutableStateOf(Templates.all.first()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Proyek baru") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, label = { Text("Nama proyek") })
                Spacer(Modifier.height(12.dp))
                Templates.all.forEach { t ->
                    Row(Modifier.fillMaxWidth().clickable { tpl = t }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = tpl == t, onClick = { tpl = t })
                        Column {
                            Text(t.label, style = MaterialTheme.typography.bodyLarge)
                            Text(t.desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onCreate(name, tpl) }, enabled = name.isNotBlank()) { Text("Buat") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}
