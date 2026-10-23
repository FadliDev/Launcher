package com.sally.modforge.ui.projects

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sally.modforge.model.Severity
import com.sally.modforge.model.ValidationIssue

@Composable
fun NewProjectWizard(vm: ProjectsViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    BackHandler { if (ui.step > 0) vm.back() else vm.closeWizard() }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (ui.step > 0) vm.back() else vm.closeWizard() }) {
                Icon(Icons.Filled.Close, contentDescription = "Tutup")
            }
            Text("Project baru", style = MaterialTheme.typography.titleLarge)
        }
        Text(
            "Langkah ${ui.step + 1} dari 3",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        when (ui.step) {
            0 -> BasicStep(ui, vm)
            1 -> VersionStep(ui, vm)
            else -> TemplateStep(ui)
        }

        IssueList(ui.issues, onFix = vm::applyFix)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(onClick = vm::back, enabled = ui.step > 0) { Text("Kembali") }
            if (ui.step < 2) {
                Button(onClick = vm::next) { Text("Lanjut") }
            } else {
                Button(onClick = vm::create) { Text("Buat project") }
            }
        }
    }
}

@Composable
private fun BasicStep(ui: ProjectsUiState, vm: ProjectsViewModel) {
    OutlinedTextField(
        value = ui.name,
        onValueChange = vm::onName,
        label = { Text("Nama mod") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = ui.modId,
        onValueChange = vm::onModId,
        label = { Text("Mod ID") },
        supportingText = { Text("Huruf kecil, angka, underscore. Otomatis dari nama.") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = ui.packageName,
        onValueChange = vm::onPackage,
        label = { Text("Package") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun VersionStep(ui: ProjectsUiState, vm: ProjectsViewModel) {
    val versions = vm.matrix.versions
    Text("Minecraft", style = MaterialTheme.typography.titleSmall)
    versions.forEach { v ->
        val label = if (v.stability == "experimental") "${v.minecraft} · eksperimental" else v.minecraft
        RadioRow(label, selected = v.minecraft == ui.minecraft, onClick = { vm.onMinecraft(v.minecraft) })
    }

    val entry = versions.firstOrNull { it.minecraft == ui.minecraft } ?: return
    if (entry.stability == "experimental") {
        Text(
            "Versi ini eksperimental; build belum tentu lulus.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Text("Java", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
    (entry.javaMin..entry.javaMax).forEach { j ->
        val label = if (j == entry.javaRecommended) "Java $j (disarankan)" else "Java $j"
        RadioRow(label, selected = ui.java == j, onClick = { vm.onJava(j) })
    }

    Text(
        "Gradle ${entry.gradle} · Loom ${entry.loom} · Fabric Loader ${entry.fabricLoader}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun TemplateStep(ui: ProjectsUiState) {
    Text("Template", style = MaterialTheme.typography.titleSmall)
    RadioRow("Empty", selected = ui.template == "empty", onClick = {})
    listOf("Item", "Block", "Item + Block", "Event").forEach { name ->
        RadioRow("$name · segera hadir", selected = false, onClick = {}, enabled = false)
    }

    Text("Ringkasan", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
    Text("${ui.name} (${ui.modId})")
    Text(
        "Minecraft ${ui.minecraft} · Java ${ui.java} · ${ui.packageName}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun RadioRow(text: String, selected: Boolean, onClick: () -> Unit, enabled: Boolean = true) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Text(
            text,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun IssueList(issues: List<ValidationIssue>, onFix: (ValidationIssue) -> Unit) {
    issues.forEach { issue ->
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    issue.message,
                    color = if (issue.severity == Severity.ERROR) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                if (issue.fixedJava != null) {
                    TextButton(onClick = { onFix(issue) }) { Text("Perbaiki otomatis") }
                }
            }
        }
    }
}
