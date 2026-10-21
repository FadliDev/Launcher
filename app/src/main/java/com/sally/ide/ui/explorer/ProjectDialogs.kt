package com.sally.ide.ui.explorer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sally.ide.data.ProjectManager
import com.sally.ide.data.ProjectTemplate
import com.sally.ide.data.TextMatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun NewProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (ProjectTemplate, String, String) -> Unit
) {
    var template by remember { mutableStateOf(ProjectTemplate.ANDROID_KOTLIN) }
    var name by remember { mutableStateOf("MyApp") }
    var pkg by remember { mutableStateOf("com.sally.myapp") }
    var packageEdited by remember { mutableStateOf(false) }
    val needsPackage = template == ProjectTemplate.ANDROID_KOTLIN ||
        template == ProjectTemplate.ANDROID_JAVA || template == ProjectTemplate.FABRIC_MOD

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Proyek baru") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ProjectTemplate.entries.forEach { item ->
                    val selected = item == template
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth().clickable { template = item }
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(38.dp)) {
                                Box(contentAlignment = Alignment.Center) { Text(item.badge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer) }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(item.title, fontWeight = FontWeight.SemiBold)
                                Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (!packageEdited) {
                            val tail = it.lowercase().filter { c -> c.isLetterOrDigit() && c.code < 128 }
                            pkg = "com.sally." + tail.ifBlank { "app" }
                        }
                    },
                    singleLine = true,
                    label = { Text("Nama proyek") }
                )
                if (needsPackage) {
                    OutlinedTextField(
                        value = pkg,
                        onValueChange = { pkg = it; packageEdited = true },
                        singleLine = true,
                        label = { Text("Package") }
                    )
                }
            }
        },
        confirmButton = { Button(onClick = { onCreate(template, name, pkg) }, enabled = name.isNotBlank()) { Text("Buat") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
fun ProjectSearchDialog(
    projectDir: File,
    onDismiss: () -> Unit,
    onOpen: (File, Int?) -> Unit
) {
    var textMode by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    var matches by remember { mutableStateOf<List<TextMatch>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }

    LaunchedEffect(query, textMode) {
        if (query.isBlank()) {
            files = emptyList(); matches = emptyList(); searching = false
            return@LaunchedEffect
        }
        searching = true
        delay(250)
        if (textMode) {
            matches = withContext(Dispatchers.IO) { ProjectManager.searchText(projectDir, query) }
            files = emptyList()
        } else {
            files = withContext(Dispatchers.IO) { ProjectManager.fuzzyFiles(projectDir, query) }
            matches = emptyList()
        }
        searching = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cari di proyek") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !textMode, onClick = { textMode = false }, label = { Text("Nama file") })
                    FilterChip(selected = textMode, onClick = { textMode = true }, label = { Text("Isi teks") })
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    label = { Text(if (textMode) "Teks yang dicari" else "Nama file (fuzzy)") },
                    modifier = Modifier.fillMaxWidth()
                )
                val empty = if (textMode) matches.isEmpty() else files.isEmpty()
                if (query.isNotBlank() && empty && !searching) {
                    Text("Tidak ada hasil.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    if (textMode) {
                        items(matches) { m ->
                            Column(Modifier.fillMaxWidth().clickable { onOpen(m.file, m.line) }.padding(vertical = 8.dp)) {
                                Text(
                                    m.file.relativeTo(projectDir).invariantSeparatorsPath + ":" + (m.line + 1),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(m.preview, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    } else {
                        items(files) { f ->
                            Column(Modifier.fillMaxWidth().clickable { onOpen(f, null) }.padding(vertical = 8.dp)) {
                                Text(f.name, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    f.relativeTo(projectDir).invariantSeparatorsPath,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup") } }
    )
}
