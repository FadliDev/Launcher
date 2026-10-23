package com.sally.freshlauncher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.sally.freshlauncher.core.VersionEntry

@Composable
fun VersionsScreen(vm: LauncherViewModel) {
    var filter by rememberSaveable { mutableStateOf("release") }

    LaunchedEffect(Unit) {
        if (vm.versions.isEmpty()) vm.refreshVersions()
    }

    val shown = vm.versions.filter { filter == "all" || it.type == filter }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScreenHeader("Versi Minecraft", "Pilih versi yang ingin kamu mainkan", Icons.Filled.Layers)

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filter == "release",
                    onClick = { filter = "release" },
                    label = { Text("Rilis") }
                )
                FilterChip(
                    selected = filter == "snapshot",
                    onClick = { filter = "snapshot" },
                    label = { Text("Snapshot") }
                )
                FilterChip(
                    selected = filter == "all",
                    onClick = { filter = "all" },
                    label = { Text("Semua") }
                )
            }
            IconButton(onClick = { vm.refreshVersions() }, enabled = !vm.busy) {
                Icon(Icons.Filled.Refresh, "Muat ulang")
            }
        }

        if (vm.busy) LinearProgressIndicator(Modifier.fillMaxWidth())

        if (shown.isEmpty()) {
            EmptyState(
                Icons.Filled.Refresh,
                "Belum ada versi",
                "Tekan tombol muat ulang untuk mengambil daftar versi dari Mojang."
            )
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(shown, key = { it.id }) { v ->
                    VersionCard(
                        v = v,
                        installed = v.id in vm.installed,
                        selected = vm.selected == v.id,
                        busy = vm.busy,
                        onInstall = { vm.install(v) },
                        onSelect = { vm.select(v.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun VersionCard(
    v: VersionEntry,
    installed: Boolean,
    selected: Boolean,
    busy: Boolean,
    onInstall: () -> Unit,
    onSelect: () -> Unit
) {
    val container = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = container)
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(typeIcon(v.type), null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(v.id, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    if (installed) "${typeLabel(v.type)} • Terpasang" else typeLabel(v.type),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            when {
                selected -> Icon(Icons.Filled.CheckCircle, "Dipilih", tint = MaterialTheme.colorScheme.primary)
                installed -> FilledTonalButton(onClick = onSelect) { Text("Pilih") }
                else -> FilledTonalButton(onClick = onInstall, enabled = !busy) {
                    Icon(Icons.Filled.Download, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Pasang")
                }
            }
        }
    }
}

private fun typeIcon(type: String): ImageVector = when (type) {
    "release" -> Icons.Filled.CheckCircle
    "snapshot" -> Icons.Filled.Build
    "old_beta", "old_alpha" -> Icons.Filled.History
    else -> Icons.Filled.Info
}

private fun typeLabel(type: String): String = when (type) {
    "release" -> "Rilis"
    "snapshot" -> "Snapshot"
    "old_beta" -> "Beta lama"
    "old_alpha" -> "Alpha lama"
    else -> type
}
