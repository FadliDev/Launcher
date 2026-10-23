package com.sally.freshlauncher.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(vm: LauncherViewModel) {
    val ramOptions = listOf(1024 to "1 GB", 2048 to "2 GB", 3072 to "3 GB", 4096 to "4 GB")
    val pickJre = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) vm.installJre(uris)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenHeader("Pengaturan", "Atur launcher sesuai perangkatmu", Icons.Filled.Settings)

        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Filled.Storage, null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text("Java (JRE)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (vm.jreReady) "Terpasang" else "Belum terpasang",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (vm.jreReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            Text(
                "Pilih arsip bin-arm64.tar.xz dan universal.tar.xz (folder components/jre-17 di ZalithLauncher). " +
                    "Keduanya diekstrak ke folder JRE.",
                style = MaterialTheme.typography.bodySmall
            )
            Button(
                onClick = { pickJre.launch(arrayOf("*/*")) },
                enabled = !vm.busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Download, null)
                Spacer(Modifier.width(8.dp))
                Text("Pasang dari file")
            }
        }

        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Filled.Memory, null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text("Memori untuk game", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Lebih besar biasanya lebih lancar, tapi perlu RAM perangkat yang cukup.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                ramOptions.forEach { (mb, label) ->
                    FilterChip(
                        selected = vm.ramMb == mb,
                        onClick = { vm.saveRam(mb) },
                        label = { Text(label) }
                    )
                }
            }
        }

        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text("Tentang", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Fresh Launcher v0.1.0", style = MaterialTheme.typography.bodySmall)
                    Text("Lisensi GPL-3.0 · lihat NOTICE.md", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
