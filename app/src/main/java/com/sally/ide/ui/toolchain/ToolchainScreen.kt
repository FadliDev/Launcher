package com.sally.ide.ui.toolchain

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sally.ide.data.toolchain.CustomPackage
import com.sally.ide.data.toolchain.GradleDist
import com.sally.ide.data.toolchain.StorageInfo
import com.sally.ide.data.toolchain.TaskPhase
import com.sally.ide.data.toolchain.TaskState
import com.sally.ide.data.toolchain.ToolCategory
import com.sally.ide.data.toolchain.ToolchainManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun formatBytes(bytes: Long): String = when {
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024 -> "%.1f KB".format(bytes / 1024.0)
    bytes < 1024L * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    else -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
}

@Composable
fun ToolchainScreen(modifier: Modifier, onMessage: (String) -> Unit) {
    val context = LocalContext.current
    val revision = ToolchainManager.revision
    val tasks = ToolchainManager.tasks

    var apis by remember { mutableStateOf<List<Int>>(emptyList()) }
    var gradleList by remember { mutableStateOf<List<GradleDist>>(emptyList()) }
    var listError by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var reloadTick by remember { mutableIntStateOf(0) }
    var showAllApis by remember { mutableStateOf(false) }
    var showAllGradle by remember { mutableStateOf(false) }
    var storage by remember { mutableStateOf<StorageInfo?>(null) }

    LaunchedEffect(reloadTick) {
        loading = true
        listError = null
        apis = ToolchainManager.loadPlatformApis(context)
        gradleList = try {
            ToolchainManager.loadGradleVersions(context)
        } catch (error: Exception) {
            listError = error.message
            emptyList()
        }
        loading = false
    }
    LaunchedEffect(revision, tasks.isEmpty()) {
        storage = withContext(Dispatchers.IO) { ToolchainManager.storage(context) }
    }

    val installedApis = remember(revision) { ToolchainManager.installedPlatforms(context) }
    val installedGradle = remember(revision) { ToolchainManager.installedGradle(context) }
    val installedCustom = remember(revision) { ToolchainManager.installedCustom(context) }

    val apiRows = (apis + installedApis).distinct().sortedDescending()
    val shownApis = if (showAllApis) apiRows else apiRows.take(8)
    val gradleRows = gradleList + installedGradle.filter { v -> gradleList.none { it.version == v } }.map { GradleDist(it, "", "") }
    val shownGradle = if (showAllGradle) gradleRows else gradleRows.take(8)
    val customTasks = tasks.entries.filter { it.key.startsWith("custom:") }.map { it.key to it.value }

    var category by remember { mutableStateOf(ToolCategory.JDK) }
    var customName by rememberSaveable { mutableStateOf("") }
    var customUrl by rememberSaveable { mutableStateOf("") }
    var customSha by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Toolchain", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        if (loading) "Mengambil daftar…" else "Unduhan bisa dilanjutkan (resume) kalau terputus.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { reloadTick++ }) { Icon(Icons.Default.Refresh, "Muat ulang daftar") }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Storage, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Penyimpanan toolchain: ${storage?.let { formatBytes(it.total) } ?: "…"}", fontWeight = FontWeight.SemiBold)
                        storage?.let {
                            Text(
                                "SDK ${formatBytes(it.sdk)} · Gradle ${formatBytes(it.gradle)} · Kustom ${formatBytes(it.custom)} · Cache ${formatBytes(it.cache)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    TextButton(onClick = {
                        ToolchainManager.clearCache(context)
                        onMessage("Cache unduhan dibersihkan (tool terpasang tidak dihapus).")
                    }) { Text("Bersihkan cache") }
                }
            }
        }
        if (listError != null) {
            item {
                Text(
                    "Daftar versi Gradle belum bisa diambil: $listError",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        item { SectionTitle("Android SDK Platform (android.jar)") }
        item {
            Text(
                "Sumber: Sable/android-platforms. Isinya implementasi framework asli (bukan stub resmi Google). " +
                    "Struktur platform lengkap yang dibutuhkan AGP (build.prop, data/, framework.aidl) divalidasi di Fase 4.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(shownApis, key = { "api-$it" }) { api ->
            val installed = api in installedApis
            ToolRow(
                title = "API $api",
                subtitle = if (installed) "Terpasang · ${formatBytes(ToolchainManager.folderSize(ToolchainManager.platformDir(context, api)))}" else "android-$api/android.jar",
                installed = installed,
                task = tasks["platform:$api"],
                canInstall = true,
                onInstall = { ToolchainManager.installPlatform(context, api) },
                onRemove = { ToolchainManager.removePlatform(context, api) },
                onCancel = { ToolchainManager.cancel("platform:$api") }
            )
        }
        if (apiRows.size > 8) {
            item { TextButton(onClick = { showAllApis = !showAllApis }) { Text(if (showAllApis) "Tampilkan lebih sedikit" else "Tampilkan semua (${apiRows.size})") } }
        }

        item { SectionTitle("Gradle") }
        items(shownGradle, key = { "gradle-${it.version}" }) { dist ->
            val installed = dist.version in installedGradle
            ToolRow(
                title = "Gradle ${dist.version}",
                subtitle = if (installed) "Terpasang · ${formatBytes(ToolchainManager.folderSize(ToolchainManager.gradleHome(context, dist.version)))}" else "Distribusi -bin, SHA-256 diverifikasi",
                installed = installed,
                task = tasks["gradle:${dist.version}"],
                canInstall = dist.downloadUrl.isNotEmpty(),
                onInstall = { ToolchainManager.installGradle(context, dist) },
                onRemove = { ToolchainManager.removeGradle(context, dist.version) },
                onCancel = { ToolchainManager.cancel("gradle:${dist.version}") }
            )
        }
        if (gradleRows.size > 8) {
            item { TextButton(onClick = { showAllGradle = !showAllGradle }) { Text(if (showAllGradle) "Tampilkan lebih sedikit" else "Tampilkan semua (${gradleRows.size})") } }
        }

        item { SectionTitle("JDK, build-tools, NDK (paket kustom)") }
        item {
            Text(
                "Biner arm64 untuk JDK/aapt2/NDK belum punya sumber resmi tunggal, jadi tempel URL HTTPS sendiri. " +
                    "Saat ini format .zip yang didukung (tar.gz/xz menyusul). SHA-256 opsional tapi disarankan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ToolCategory.values().forEach { option ->
                            FilterChip(selected = category == option, onClick = { category = option }, label = { Text(option.label) })
                        }
                    }
                    OutlinedTextField(customName, { customName = it }, Modifier.fillMaxWidth(), label = { Text("Nama (mis. jdk-21)") }, singleLine = true)
                    OutlinedTextField(
                        customUrl, { customUrl = it }, Modifier.fillMaxWidth(), label = { Text("URL https://… (.zip)") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                    )
                    OutlinedTextField(customSha, { customSha = it }, Modifier.fillMaxWidth(), label = { Text("SHA-256 (opsional)") }, singleLine = true)
                    OutlinedButton(onClick = {
                        val url = customUrl.trim()
                        when {
                            customName.isBlank() -> onMessage("Isi nama paket dulu.")
                            !url.startsWith("https://") -> onMessage("URL harus diawali https://")
                            else -> {
                                ToolchainManager.installCustom(context, CustomPackage(category, customName.trim(), url, customSha.trim()))
                                customName = ""
                                customUrl = ""
                                customSha = ""
                            }
                        }
                    }) {
                        Icon(Icons.Default.Download, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Unduh & pasang")
                    }
                }
            }
        }
        items(customTasks, key = { "ctask-${it.first}" }) { (id, task) ->
            ToolRow(
                title = task.label,
                subtitle = "Paket kustom",
                installed = false,
                task = task,
                canInstall = false,
                onInstall = {},
                onRemove = {},
                onCancel = { ToolchainManager.cancel(id) }
            )
        }
        items(installedCustom, key = { "custom-${it.category.name}-${it.name}" }) { pkg ->
            ToolRow(
                title = pkg.name,
                subtitle = "${pkg.category.label} · ${formatBytes(ToolchainManager.folderSize(java.io.File(ToolchainManager.root(context), "custom/${pkg.category.name}/${pkg.name}")))}",
                installed = true,
                task = null,
                canInstall = false,
                onInstall = {},
                onRemove = { ToolchainManager.removeCustom(context, pkg) },
                onCancel = {}
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
}

@Composable
private fun ToolRow(
    title: String,
    subtitle: String,
    installed: Boolean,
    task: TaskState?,
    canInstall: Boolean,
    onInstall: () -> Unit,
    onRemove: () -> Unit,
    onCancel: () -> Unit
) {
    val running = task != null && task.phase != TaskPhase.FAILED
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
                if (running) {
                    IconButton(onClick = onCancel) { Icon(Icons.Default.Close, "Batalkan") }
                } else if (installed) {
                    Icon(Icons.Default.CheckCircle, "Terpasang", tint = MaterialTheme.colorScheme.primary)
                    IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, "Hapus") }
                } else if (canInstall) {
                    FilledTonalButton(onClick = onInstall) {
                        Icon(Icons.Default.Download, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Unduh")
                    }
                }
            }
            if (task != null) {
                Spacer(Modifier.size(6.dp))
                when (task.phase) {
                    TaskPhase.DOWNLOADING -> {
                        if (task.total > 0) {
                            LinearProgressIndicator(progress = { (task.done.toFloat() / task.total).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        Text(
                            "${formatBytes(task.done)} / ${if (task.total > 0) formatBytes(task.total) else "?"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TaskPhase.VERIFYING -> {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text("Memeriksa file…", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TaskPhase.EXTRACTING -> {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text("Mengekstrak…", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TaskPhase.FAILED -> {
                        Text(task.error ?: "Gagal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = onCancel) { Text("Tutup pesan") }
                    }
                }
            }
        }
    }
}
