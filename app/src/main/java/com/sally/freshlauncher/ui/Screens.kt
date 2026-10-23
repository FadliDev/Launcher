package com.sally.freshlauncher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sally.freshlauncher.core.OfflineAccount

@Composable
fun HomeScreen(vm: LauncherViewModel) {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Fresh Launcher", style = MaterialTheme.typography.headlineMedium)
        Text("Akun: ${vm.accountName}")
        Text("Versi terpilih: ${vm.selected ?: "belum dipilih"}")
        Button(
            onClick = { vm.play() },
            enabled = !vm.busy && vm.selected != null,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Main") }
        if (vm.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        Text("Log", style = MaterialTheme.typography.titleSmall)
        LazyColumn(Modifier.weight(1f)) {
            items(vm.log.reversed()) { line ->
                Text(line, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp))
            }
        }
    }
}

@Composable
fun VersionsScreen(vm: LauncherViewModel) {
    var onlyRelease by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        if (vm.versions.isEmpty()) vm.refreshVersions()
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { vm.refreshVersions() }, enabled = !vm.busy) { Text("Muat ulang") }
            Spacer(Modifier.width(12.dp))
            Text("Hanya release")
            Spacer(Modifier.width(8.dp))
            Switch(checked = onlyRelease, onCheckedChange = { onlyRelease = it })
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(vm.versions.filter { !onlyRelease || it.type == "release" }, key = { it.id }) { v ->
                val isInstalled = v.id in vm.installed
                val isSelected = vm.selected == v.id
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(v.id)
                            Text(v.type, style = MaterialTheme.typography.bodySmall)
                        }
                        if (!isInstalled) {
                            OutlinedButton(onClick = { vm.install(v) }, enabled = !vm.busy) { Text("Pasang") }
                            Spacer(Modifier.width(8.dp))
                        }
                        Button(
                            onClick = { vm.select(v.id) },
                            enabled = isInstalled && !isSelected
                        ) { Text(if (isSelected) "Dipilih" else "Pilih") }
                    }
                }
            }
        }
    }
}

@Composable
fun AccountScreen(vm: LauncherViewModel) {
    var name by rememberSaveable { mutableStateOf(vm.accountName) }
    val valid = OfflineAccount.isValid(name)

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Akun offline", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nama pemain") },
            singleLine = true,
            isError = !valid,
            supportingText = {
                Text(if (valid) "UUID: ${OfflineAccount(name).uuid}" else "3–16 karakter: huruf, angka, atau _")
            },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { vm.saveAccount(name) },
            enabled = valid,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Simpan") }
    }
}
