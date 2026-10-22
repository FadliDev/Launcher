package com.sally.modforge.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sally.modforge.model.Loader

sealed interface Screen {
    data object Projects : Screen
    data object Create : Screen
    data object GitHub : Screen
    data class Workspace(val id: String) : Screen
}

@Composable
fun App(vm: AppVm = viewModel()) {
    var screen by remember { mutableStateOf<Screen>(Screen.Projects) }
    val msg by vm.message.collectAsState()
    BackHandler(enabled = screen != Screen.Projects) { screen = Screen.Projects }
    when (val s = screen) {
        Screen.Projects -> ProjectsScreen(vm, { screen = Screen.Create }, { screen = Screen.Workspace(it) }, { screen = Screen.GitHub })
        Screen.Create -> CreateScreen(vm) { screen = Screen.Projects }
        Screen.GitHub -> GitHubScreen(vm)
        is Screen.Workspace -> WorkspaceScreen(vm, s.id)
    }
    msg?.let {
        AlertDialog(
            onDismissRequest = { vm.message.value = null },
            confirmButton = { TextButton({ vm.message.value = null }) { Text("OK") } },
            text = { Text(it) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(vm: AppVm, onNew: () -> Unit, onOpen: (String) -> Unit, onGitHub: () -> Unit) {
    val projects by vm.projects.collectAsState()
    val login by vm.login.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("ModForge Studio") }, actions = {
                TextButton(onGitHub) { Text(login?.let { "@$it" } ?: "Connect GitHub") }
            })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onNew, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Project baru") })
        },
    ) { pad ->
        if (projects.isEmpty()) {
            Box(Modifier.padding(pad).fillMaxSize(), Alignment.Center) {
                Text("Belum ada project.\nBuat mod pertamamu!", textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(projects, key = { it.id }) { p ->
                    Card(Modifier.fillMaxWidth().clickable { onOpen(p.id) }) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(p.name, style = MaterialTheme.typography.titleMedium)
                                Text("${p.loader} • MC ${p.mcVersion} • Java ${p.toolchain.javaVersion}", style = MaterialTheme.typography.bodySmall)
                                Text(p.repoFullName.ifBlank { "Belum di-push" }, style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton({ vm.delete(p) }) { Icon(Icons.Default.Delete, "Hapus") }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Picker(label: String, value: String, options: List<String>, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(open, { open = it }) {
        OutlinedTextField(
            value, {}, readOnly = true, label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(open, { open = false }) {
            options.forEach { o -> DropdownMenuItem(text = { Text(o) }, onClick = { onPick(o); open = false }) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen(vm: AppVm, done: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var loader by remember { mutableStateOf(Loader.FABRIC) }
    val versions = vm.profiles.filter { it.loader == loader }.map { it.mc }
    var mc by remember(loader) { mutableStateOf(versions.firstOrNull() ?: "") }
    var priv by remember { mutableStateOf(true) }
    val pr = vm.profiles.firstOrNull { it.loader == loader && it.mc == mc }
    Scaffold(topBar = { TopAppBar(title = { Text("Project baru") }) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Nama mod") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(author, { author = it }, label = { Text("Author") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Picker("Loader", loader.name, Loader.entries.filter { l -> vm.profiles.any { it.loader == l } }.map { it.name }) { loader = Loader.valueOf(it) }
            Picker("Versi Minecraft", mc, versions) { mc = it }
            pr?.let { Text("Otomatis: Java ${it.java} • Gradle ${it.gradle} • loader ${it.loaderVersion}", style = MaterialTheme.typography.bodySmall) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(priv, { priv = it }); Spacer(Modifier.width(8.dp)); Text(if (priv) "Repo private" else "Repo public")
            }
            Button(
                enabled = name.isNotBlank() && author.isNotBlank() && mc.isNotBlank(),
                onClick = { vm.create(name.trim(), author.trim(), loader, mc, priv); done() },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Buat") }
        }
    }
}
