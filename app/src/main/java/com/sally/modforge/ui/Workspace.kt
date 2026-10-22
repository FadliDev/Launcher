package com.sally.modforge.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sally.modforge.model.Project
import com.sally.modforge.script.NodeCatalog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(vm: AppVm, id: String) {
    val p = vm.projects.collectAsState().value.firstOrNull { it.id == id } ?: return
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Code", "Build", "Visual", "Models")
    Scaffold(topBar = { TopAppBar(title = { Text(p.name) }) }) { pad ->
        Column(Modifier.padding(pad)) {
            TabRow(tab) { tabs.forEachIndexed { i, t -> Tab(tab == i, { tab = i }, text = { Text(t) }) } }
            when (tab) {
                0 -> CodeTab(vm, p)
                1 -> BuildTab(vm, p)
                2 -> VisualTab()
                else -> Text("3D Item/Block editor: Fase 3 di ROADMAP.", Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
fun CodeTab(vm: AppVm, p: Project) {
    val files = remember(p.id) { vm.store.files(p) }
    var open by remember(p.id) { mutableStateOf<String?>(null) }
    var text by remember(p.id) { mutableStateOf("") }
    var dirty by remember(p.id) { mutableStateOf(false) }
    val path = open
    if (path == null) {
        LazyColumn {
            items(files) { f ->
                ListItem(
                    headlineContent = { Text(f, fontFamily = FontFamily.Monospace, fontSize = 13.sp) },
                    trailingContent = { if (f in p.toolchain.customFiles) Text("Custom") },
                    modifier = Modifier.clickable { open = f; text = vm.store.read(p, f); dirty = false },
                )
            }
        }
    } else {
        Column(Modifier.fillMaxSize().padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton({ open = null }) { Text("← Files") }
                Spacer(Modifier.weight(1f))
                Text(if (dirty) "Belum disimpan" else "Tersimpan (lokal) ✓", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.width(8.dp))
                Button(enabled = dirty, onClick = { vm.saveFile(p, path, text); dirty = false }) { Text("Save") }
            }
            Text(path, style = MaterialTheme.typography.labelSmall)
            OutlinedTextField(
                text, { text = it; dirty = true }, Modifier.fillMaxSize(),
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
            )
        }
    }
}

@Composable
fun BuildTab(vm: AppVm, p: Project) {
    val st by vm.builder.state.collectAsState()
    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Java ${p.toolchain.javaVersion} • Gradle ${p.toolchain.gradleVersion}")
        Text(if (p.repoFullName.isBlank()) "Repo dibuat otomatis saat Build pertama" else "Repo: ${p.repoFullName}")
        Button(onClick = { vm.build(p) }, enabled = st !is BuildState.Working, modifier = Modifier.fillMaxWidth()) { Text("Build") }
        when (val s = st) {
            is BuildState.Working -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(20.dp)); Spacer(Modifier.width(12.dp)); Text(s.step)
            }
            is BuildState.Done -> {
                Text("Selesai ✓  ${s.runUrl}")
                s.jars.forEach { f ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("${f.name} (${f.length() / 1024} KB)")
                            TextButton({ vm.exportJar(f) }) { Text("Simpan ke Download") }
                        }
                    }
                }
            }
            is BuildState.Failed -> {
                Text(s.message, color = MaterialTheme.colorScheme.error)
                if (s.log.isNotBlank()) Text(s.log, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
            BuildState.Idle -> Unit
        }
    }
}

@Composable
fun VisualTab() {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Katalog node (editor kanvas = Fase 4):", style = MaterialTheme.typography.titleSmall) }
        items(NodeCatalog.all) { n ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("${n.category.label} • ${n.title}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "in: ${n.inputs.joinToString { it.name }}  out: ${n.outputs.joinToString { it.name }}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
