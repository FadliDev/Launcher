package com.sally.ember.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sally.ember.data.*
import com.sally.ember.game.GameActivity
import com.sally.ember.game.Renderers

@Composable
fun HomeScreen(vm: AppViewModel, ctx: Context) {
    val accounts by vm.accounts.collectAsState()
    val selAcc by vm.selAcc.collectAsState()
    val selVer by vm.selVer.collectAsState()
    val dl by vm.dl.collectAsState()
    val tick by vm.installedTick.collectAsState()
    val acc = accounts.firstOrNull { it.uuid == selAcc }
    val ready = selVer != null && Mojang.isInstalled(selVer!!) && tick >= 0

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Ember", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        InfoCard(Icons.Default.Person, "Akun", acc?.name ?: "Belum ada akun (buka tab Akun)")
        InfoCard(Icons.Default.Layers, "Versi", selVer ?: "Belum ada versi (buka tab Versi)")
        Spacer(Modifier.weight(1f))
        when (val d = dl) {
            is DlState.Running -> {
                Text("Mengunduh ${d.label}: ${d.done}/${d.total}")
                LinearProgressIndicator(progress = { if (d.total == 0) 0f else d.done / d.total.toFloat() }, modifier = Modifier.fillMaxWidth())
            }
            is DlState.Failed -> Text(d.msg, color = MaterialTheme.colorScheme.error)
            else -> {}
        }
        Button(
            onClick = {
                ctx.startActivity(Intent(ctx, GameActivity::class.java)
                    .putExtra("version", selVer).putExtra("account", acc!!.uuid))
            },
            enabled = acc != null && ready && dl !is DlState.Running,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text("MAIN") }
    }
}

@Composable private fun InfoCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column { Text(title, style = MaterialTheme.typography.labelMedium); Text(value, style = MaterialTheme.typography.titleMedium) }
        }
    }
}

@Composable
fun VersionsScreen(vm: AppViewModel) {
    val all by vm.versions.collectAsState()
    val err by vm.loadError.collectAsState()
    val sel by vm.selVer.collectAsState()
    val dl by vm.dl.collectAsState()
    val tick by vm.installedTick.collectAsState()
    var type by remember { mutableStateOf("release") }
    val list = remember(all, type) { all.filter { it.type == type } }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("release" to "Release", "snapshot" to "Snapshot", "old_beta" to "Beta", "old_alpha" to "Alpha").forEach { (k, l) ->
                FilterChip(selected = type == k, onClick = { type = k }, label = { Text(l) })
            }
        }
        if (err != null) Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(err!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
            TextButton(onClick = { vm.refreshVersions() }) { Text("Coba lagi") }
        }
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(list, key = { it.id }) { v ->
                val installed = Mojang.isInstalled(v.id) && tick >= 0
                val busy = (dl as? DlState.Running)?.label == v.id
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (sel == v.id) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().clickable(enabled = installed) { vm.select(v.id) }
                ) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(v.id, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        when {
                            busy -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                            installed -> Icon(Icons.Default.CheckCircle, "Terpasang", tint = MaterialTheme.colorScheme.primary)
                            else -> IconButton(onClick = { vm.install(v) }, enabled = dl !is DlState.Running) { Icon(Icons.Default.Download, "Unduh") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccountsScreen(vm: AppViewModel) {
    val accounts by vm.accounts.collectAsState()
    val sel by vm.selAcc.collectAsState()
    var name by remember { mutableStateOf("") }
    val valid = name.trim().length in 3..16 && name.trim().all { it.isLetterOrDigit() || it == '_' }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Nama offline (3-16)") }, singleLine = true, modifier = Modifier.weight(1f))
            Button(onClick = { vm.addAccount(name); name = "" }, enabled = valid) { Text("Tambah") }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(accounts, key = { it.uuid }) { a ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(start = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = sel == a.uuid, onClick = { vm.selectAccount(a.uuid) })
                        Column(Modifier.weight(1f)) { Text(a.name, style = MaterialTheme.typography.titleMedium); Text(a.uuid, style = MaterialTheme.typography.labelSmall) }
                        IconButton(onClick = { vm.removeAccount(a.uuid) }) { Icon(Icons.Default.Delete, "Hapus") }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(vm: AppViewModel, ctx: Context) {
    var ram by remember { mutableFloatStateOf(vm.prefs.ramMb.toFloat()) }
    var jvm by remember { mutableStateOf(vm.prefs.extraJvm) }
    val nativeDir = remember { java.io.File(ctx.applicationInfo.nativeLibraryDir) }
    var rid by remember { mutableStateOf(vm.prefs.renderer ?: Renderers.DEFAULT.id) }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Renderer", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Renderers.all.forEach { r ->
                val ok = Renderers.isAvailable(r, nativeDir)
                FilterChip(
                    selected = rid == r.id, enabled = ok,
                    onClick = { rid = r.id; vm.prefs.renderer = r.id },
                    label = { Text(if (r.id == Renderers.DEFAULT.id) r.name + " (default)" else r.name) }
                )
            }
        }
        if (!Renderers.isAvailable(Renderers.KRYPTON, nativeDir))
            Text("libng_gl4es.so tidak ada di APK ini, jadi dipakai GL4ES.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
        Text("RAM untuk game: ${ram.toInt()} MB", style = MaterialTheme.typography.titleMedium)
        Slider(ram, { ram = it }, valueRange = 512f..4096f, steps = 13, onValueChangeFinished = { vm.prefs.ramMb = ram.toInt() })
        OutlinedTextField(jvm, { jvm = it; vm.prefs.extraJvm = it }, label = { Text("Argumen JVM tambahan") }, modifier = Modifier.fillMaxWidth())
    }
}
