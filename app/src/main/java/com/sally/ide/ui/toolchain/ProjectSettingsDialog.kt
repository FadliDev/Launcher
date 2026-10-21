package com.sally.ide.ui.toolchain

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sally.ide.data.toolchain.ProjectConfig
import com.sally.ide.data.toolchain.ProjectConfigStore
import com.sally.ide.data.toolchain.ToolchainManager
import java.io.File

/** Edits `<project>/.sallyide/project.json` (Gradle, JDK, compileSdk, NDK, JVM args). */
@Composable
fun ProjectSettingsDialog(projectDir: File, onDismiss: () -> Unit, onSaved: (String) -> Unit) {
    val context = LocalContext.current
    val initial = remember(projectDir) { ProjectConfigStore.load(projectDir) }
    val installedGradle = remember { ToolchainManager.installedGradle(context) }
    val installedApis = remember { ToolchainManager.installedPlatforms(context) }

    var gradle by remember { mutableStateOf(initial.gradleVersion) }
    var jdk by remember { mutableStateOf(initial.jdkVersion) }
    var compileSdk by remember { mutableStateOf(if (initial.compileSdk > 0) initial.compileSdk.toString() else "") }
    var ndk by remember { mutableStateOf(initial.ndkVersion) }
    var jvmArgs by remember { mutableStateOf(initial.jvmArgs) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Pengaturan proyek", fontWeight = FontWeight.Bold)
                Text(projectDir.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(gradle, { gradle = it.trim() }, Modifier.fillMaxWidth(), label = { Text("Versi Gradle") }, singleLine = true)
                if (installedGradle.isNotEmpty()) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        installedGradle.forEach { version -> AssistChip(onClick = { gradle = version }, label = { Text(version) }) }
                    }
                } else {
                    Text("Belum ada Gradle terpasang. Unduh dari layar Toolchain.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Text("JDK", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("8", "17", "21").forEach { v -> FilterChip(selected = jdk == v, onClick = { jdk = if (jdk == v) "" else v }, label = { Text(v) }) }
                }

                OutlinedTextField(
                    compileSdk, { compileSdk = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("compileSdk") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                if (installedApis.isNotEmpty()) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        installedApis.forEach { api -> AssistChip(onClick = { compileSdk = api.toString() }, label = { Text("API $api") }) }
                    }
                }
                OutlinedTextField(ndk, { ndk = it.trim() }, Modifier.fillMaxWidth(), label = { Text("Versi NDK (opsional)") }, singleLine = true)
                OutlinedTextField(jvmArgs, { jvmArgs = it }, Modifier.fillMaxWidth(), label = { Text("JVM args") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                runCatching {
                    ProjectConfigStore.save(
                        projectDir,
                        ProjectConfig(gradle, jdk, compileSdk.toIntOrNull() ?: 0, ndk, jvmArgs.trim())
                    )
                }.onSuccess { onSaved("Tersimpan di .sallyide/project.json") }
                    .onFailure { onSaved("Gagal menyimpan: ${it.message}") }
                onDismiss()
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
