package com.sally.freshlauncher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(vm: LauncherViewModel, onNavigate: (Int) -> Unit) {
    var showLog by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenHeader(
            title = "Halo, ${vm.accountName}!",
            subtitle = "Siap bermain hari ini?",
            icon = Icons.Filled.Home
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Layers, null, tint = MaterialTheme.colorScheme.onPrimary)
                    Text(
                        "Versi terpilih",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Text(
                    vm.selected ?: "Belum ada versi",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Button(
                    onClick = { vm.play() },
                    enabled = !vm.busy && vm.selected != null,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Filled.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Main", style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        if (vm.busy) LinearProgressIndicator(Modifier.fillMaxWidth())

        if (!vm.jreReady) {
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.tertiary)
                    Text(
                        "Java (JRE) belum terpasang, jadi game belum bisa dijalankan.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Text("Aksi cepat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickAction(Icons.Filled.Layers, "Versi", Modifier.weight(1f)) { onNavigate(1) }
            QuickAction(Icons.Filled.Person, "Akun", Modifier.weight(1f)) { onNavigate(2) }
            QuickAction(Icons.Filled.Settings, "Pengaturan", Modifier.weight(1f)) { onNavigate(3) }
        }

        TextButton(onClick = { showLog = !showLog }) {
            Icon(Icons.Filled.Terminal, null)
            Spacer(Modifier.width(8.dp))
            Text(if (showLog) "Sembunyikan log" else "Lihat log")
        }
        if (showLog) {
            SectionCard {
                if (vm.log.isEmpty()) {
                    Text("Belum ada aktivitas.", style = MaterialTheme.typography.bodyMedium)
                }
                vm.log.takeLast(50).reversed().forEach { line ->
                    Text(line, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
